import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

import {
  BASE_URL,
  DEFAULT_REGION_CODES,
  HOT_POST_IDS,
  authParams,
  buildSummary,
  commonOptions,
  intEnv,
  jsonAuthParams,
  randomItem,
  safeJson,
} from './lib/config.js';
import { registerAndLoginUsers } from './lib/auth.js';

const gamificationActionDuration = new Trend('gamification_action_duration');
const rankingQueryDuration = new Trend('ranking_query_duration');
const projectionLagRate = new Rate('projection_lag_rate');

export const options = commonOptions(
  { scenario: 'gamification-projection' },
  {
    gamification_action_duration: ['p(95)<700'],
    ranking_query_duration: ['p(95)<500'],
  },
  [
    { duration: '30s', target: 5 },
    { duration: '1m', target: 10 },
    { duration: '2m', target: 15 },
    { duration: '30s', target: 0 },
  ]
);

export function setup() {
  const users = registerAndLoginUsers(intEnv('GAMIFICATION_AUTH_USERS', 12), 'gami');
  return { users };
}

export default function (data) {
  const authUser = data.users[(__VU - 1) % data.users.length];
  const createPost = Math.random() < 0.45;

  if (createPost) {
    group('gamification post create', () => {
      const response = http.post(
        `${BASE_URL}/api/v1/posts`,
        JSON.stringify({
          title: `perf post vu=${__VU} iter=${__ITER}`,
          content: `gamification pressure post ${__ITER}`,
          regionCode: randomItem(DEFAULT_REGION_CODES),
          imageMediaIds: [],
        }),
        jsonAuthParams(authUser, { tags: { name: 'gamificationPostCreate' } })
      );

      gamificationActionDuration.add(response.timings.duration);
      check(response, {
        'post create status is 201': (res) => res.status === 201,
      });
    });
  } else {
    group('gamification comment create', () => {
      const response = http.post(
        `${BASE_URL}/api/v1/posts/${randomItem(HOT_POST_IDS)}/comments`,
        JSON.stringify({
          content: `gamification comment vu=${__VU} iter=${__ITER}`,
          parentCommentId: null,
        }),
        jsonAuthParams(authUser, { tags: { name: 'gamificationCommentCreate' } })
      );

      gamificationActionDuration.add(response.timings.duration);
      check(response, {
        'comment create status is 201': (res) => res.status === 201,
      });
    });
  }

  sleep(Number.parseFloat(__ENV.GAMIFICATION_SETTLE_SECONDS || '0.2'));

  group('ranking verification', () => {
    const myRankResponse = http.get(
      `${BASE_URL}/api/v1/rankings/me?type=POINTS`,
      authParams(authUser, { tags: { name: 'rankingMe' } })
    );
    rankingQueryDuration.add(myRankResponse.timings.duration);

    const statsResponse = http.get(
      `${BASE_URL}/api/v1/rankings/stats`,
      authParams(authUser, { tags: { name: 'rankingStats' } })
    );
    rankingQueryDuration.add(statsResponse.timings.duration);

    const rankOk = check(myRankResponse, {
      'ranking me status is 200': (res) => res.status === 200,
    });
    const statsOk = check(statsResponse, {
      'ranking stats status is 200': (res) => res.status === 200,
    });

    if (!rankOk || !statsOk) {
      projectionLagRate.add(1);
      return;
    }

    const rankPayload = safeJson(myRankResponse);
    projectionLagRate.add((rankPayload.score || 0) <= 0 ? 1 : 0);
  });
}

export function handleSummary(data) {
  return buildSummary('Gamification Projection Burst Test', data, [
    'gamification_action_duration',
    'ranking_query_duration',
  ]);
}
