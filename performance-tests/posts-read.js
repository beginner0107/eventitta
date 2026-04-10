import http from 'k6/http';
import { check, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

import {
  BASE_URL,
  DEFAULT_KEYWORDS,
  DEFAULT_REGION_CODES,
  HOT_POST_IDS,
  authParams,
  buildSummary,
  commonOptions,
  intEnv,
  randomItem,
  safeJson,
} from './lib/config.js';
import { registerAndLoginUsers } from './lib/auth.js';

const postsListDuration = new Trend('posts_list_duration');
const postsSearchDuration = new Trend('posts_search_duration');
const postDetailDuration = new Trend('post_detail_duration');
const authenticatedReadErrors = new Rate('authenticated_read_errors');

export const options = commonOptions(
  { scenario: 'posts-read' },
  {
    posts_list_duration: ['p(95)<500'],
    posts_search_duration: ['p(95)<600'],
    post_detail_duration: ['p(95)<350'],
  },
  [
    { duration: '30s', target: 20 },
    { duration: '1m', target: 50 },
    { duration: '2m', target: 100 },
    { duration: '30s', target: 0 },
  ]
);

export function setup() {
  const authUsers = registerAndLoginUsers(intEnv('POSTS_READ_AUTH_USERS', 24), 'pread');
  return { authUsers };
}

export default function (data) {
  const authUser = data.authUsers[(__VU - 1) % data.authUsers.length];
  const page = Math.floor(Math.random() * Math.max(intEnv('POSTS_READ_MAX_PAGE', 5), 1));
  const size = intEnv('POSTS_READ_PAGE_SIZE', 20);

  group('anonymous post list', () => {
    const response = http.get(`${BASE_URL}/api/v1/posts?page=${page}&size=${size}`, {
      tags: { name: 'postsListAnonymous' },
    });
    const payload = safeJson(response);

    postsListDuration.add(response.timings.duration);
    check(response, {
      'anonymous list status is 200': (res) => res.status === 200,
      'anonymous list contains content': () => Array.isArray(payload?.content),
    });
  });

  group('authenticated post list', () => {
    const response = http.get(
      `${BASE_URL}/api/v1/posts?page=${page}&size=${size}&regionCode=${randomItem(DEFAULT_REGION_CODES)}`,
      authParams(authUser, { tags: { name: 'postsListAuthenticated' } })
    );
    const payload = safeJson(response);

    postsListDuration.add(response.timings.duration);
    const isSuccessful = check(response, {
      'authenticated list status is 200': (res) => res.status === 200,
      'authenticated list contains content': () => Array.isArray(payload?.content),
    });

    if (!isSuccessful) {
      authenticatedReadErrors.add(1);
    }
  });

  if (Math.random() < 0.5) {
    group('search list', () => {
      const response = http.get(
        `${BASE_URL}/api/v1/posts?page=0&size=${size}&searchType=TITLE_CONTENT&keyword=${encodeURIComponent(randomItem(DEFAULT_KEYWORDS))}`,
        authParams(authUser, { tags: { name: 'postsSearch' } })
      );
      const payload = safeJson(response);

      postsSearchDuration.add(response.timings.duration);
      check(response, {
        'search list status is 200': (res) => res.status === 200,
        'search list contains content': () => Array.isArray(payload?.content),
      });
    });
  }

  if (Math.random() < 0.4) {
    group('post detail', () => {
      const postId = randomItem(HOT_POST_IDS);
      const response = http.get(
        `${BASE_URL}/api/v1/posts/${postId}`,
        authParams(authUser, { tags: { name: 'postDetail' } })
      );

      postDetailDuration.add(response.timings.duration);
      check(response, {
        'detail status is 200': (res) => res.status === 200,
      });
    });
  }
}

export function handleSummary(data) {
  return buildSummary('Posts Read Load Test', data, [
    'posts_list_duration',
    'posts_search_duration',
    'post_detail_duration',
  ]);
}
