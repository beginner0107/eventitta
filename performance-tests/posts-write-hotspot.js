import http from 'k6/http';
import { check, group } from 'k6';
import { Trend } from 'k6/metrics';

import {
  BASE_URL,
  HOT_POST_IDS,
  authParams,
  buildSummary,
  commonOptions,
  intEnv,
  jsonAuthParams,
  randomItem,
} from './lib/config.js';
import { registerAndLoginUsers } from './lib/auth.js';

const hotPostReadDuration = new Trend('hot_post_read_duration');
const postLikeDuration = new Trend('post_like_duration');
const commentCreateDuration = new Trend('comment_create_duration');

export const options = commonOptions(
  { scenario: 'posts-write-hotspot' },
  {
    hot_post_read_duration: ['p(95)<400'],
    post_like_duration: ['p(95)<500'],
    comment_create_duration: ['p(95)<500'],
  },
  [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 20 },
    { duration: '2m', target: 30 },
    { duration: '30s', target: 0 },
  ]
);

export function setup() {
  const users = registerAndLoginUsers(intEnv('POSTS_WRITE_AUTH_USERS', 20), 'pwrite');
  return { users };
}

export default function (data) {
  const authUser = data.users[(__VU - 1) % data.users.length];
  const postId = randomItem(HOT_POST_IDS);
  const roll = Math.random();

  if (roll < 0.30) {
    group('hot post detail', () => {
      const response = http.get(
        `${BASE_URL}/api/v1/posts/${postId}`,
        authParams(authUser, { tags: { name: 'hotPostDetail' } })
      );

      hotPostReadDuration.add(response.timings.duration);
      check(response, {
        'hot post detail status is 200': (res) => res.status === 200,
      });
    });
    return;
  }

  if (roll < 0.60) {
    group('hot post like', () => {
      const response = http.put(
        `${BASE_URL}/api/v1/posts/${postId}/like`,
        null,
        authParams(authUser, { tags: { name: 'hotPostLike' } })
      );

      postLikeDuration.add(response.timings.duration);
      check(response, {
        'hot post like status is 200': (res) => res.status === 200,
      });
    });
    return;
  }

  if (roll < 0.80) {
    group('hot post unlike', () => {
      const response = http.del(
        `${BASE_URL}/api/v1/posts/${postId}/like`,
        null,
        authParams(authUser, { tags: { name: 'hotPostUnlike' } })
      );

      postLikeDuration.add(response.timings.duration);
      check(response, {
        'hot post unlike status is 200': (res) => res.status === 200,
      });
    });
    return;
  }

  group('hot post comment', () => {
    const response = http.post(
      `${BASE_URL}/api/v1/posts/${postId}/comments`,
      JSON.stringify({
        content: `perf comment vu=${__VU} iter=${__ITER}`,
        parentCommentId: null,
      }),
      jsonAuthParams(authUser, { tags: { name: 'hotPostComment' } })
    );

    commentCreateDuration.add(response.timings.duration);
    check(response, {
      'hot post comment status is 201': (res) => res.status === 201,
    });
  });
}

export function handleSummary(data) {
  return buildSummary('Posts Write Hotspot Test', data, [
    'hot_post_read_duration',
    'post_like_duration',
    'comment_create_duration',
  ]);
}
