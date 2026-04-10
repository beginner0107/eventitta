import http from 'k6/http';
import { check, group } from 'k6';
import { Trend } from 'k6/metrics';

import {
  BASE_URL,
  authParams,
  basename,
  buildSummary,
  commonOptions,
  contentTypeFromPath,
  intEnv,
  pathFromPublicUrl,
  randomItem,
  parseCsvStrings,
  safeJson,
} from './lib/config.js';
import { registerAndLoginUsers } from './lib/auth.js';
import { buildMultipartBody } from './lib/multipart.js';

const fixturePaths = parseCsvStrings(__ENV.UPLOAD_FIXTURE_PATHS, ['./fixtures/sample-upload.png']);
const uploadFixtures = fixturePaths.map((path) => ({
  path,
  filename: basename(path),
  contentType: contentTypeFromPath(path),
  bytes: open(path, 'b'),
}));

const mediaUploadDuration = new Trend('media_upload_duration');
const mediaDownloadDuration = new Trend('media_download_duration');

export const options = commonOptions(
  { scenario: 'media-upload' },
  {
    media_upload_duration: ['p(95)<1500'],
    media_download_duration: ['p(95)<800'],
  },
  [
    { duration: '30s', target: 5 },
    { duration: '1m', target: 10 },
    { duration: '2m', target: 20 },
    { duration: '30s', target: 0 },
  ]
);

export function setup() {
  const users = registerAndLoginUsers(intEnv('MEDIA_UPLOAD_AUTH_USERS', 12), 'mup');
  return { users };
}

export default function (data) {
  const authUser = data.users[(__VU - 1) % data.users.length];
  const fileCount = Math.max(1, Math.min(intEnv('MEDIA_UPLOAD_FILE_COUNT', 2), uploadFixtures.length));
  const selectedFixtures = [];
  for (let index = 0; index < fileCount; index += 1) {
    selectedFixtures.push(randomItem(uploadFixtures));
  }

  const payload = buildMultipartBody(selectedFixtures);

  group('media upload', () => {
    const response = http.post(
      `${BASE_URL}/api/v1/uploads?category=POST_IMAGE`,
      payload.body,
      authParams(authUser, {
        tags: { name: 'mediaUpload' },
        headers: {
          'Content-Type': `multipart/form-data; boundary=${payload.boundary}`,
        },
      })
    );

    mediaUploadDuration.add(response.timings.duration);
    check(response, {
      'media upload status is 200': (res) => res.status === 200,
    });

    if (response.status !== 200) {
      return;
    }

    const uploadedFiles = safeJson(response);
    if (!Array.isArray(uploadedFiles) || uploadedFiles.length === 0) {
      return;
    }

    const publicUrl = uploadedFiles[0].publicUrl;
    const publicPath = pathFromPublicUrl(publicUrl);
    if (!publicPath) {
      return;
    }

    const downloadResponse = http.get(
      `${BASE_URL}${publicPath}`,
      { tags: { name: 'mediaDownload' } }
    );

    mediaDownloadDuration.add(downloadResponse.timings.duration);
    check(downloadResponse, {
      'media download status is 200': (res) => res.status === 200,
    });
  });
}

export function handleSummary(data) {
  return buildSummary('Media Upload Load Test', data, [
    'media_upload_duration',
    'media_download_duration',
  ]);
}
