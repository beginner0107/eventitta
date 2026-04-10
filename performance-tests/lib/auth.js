import http from 'k6/http';
import { check } from 'k6';

import { BASE_URL, DEFAULT_PASSWORD, cookieHeader } from './config.js';

function userIdentity(purpose, runToken, index) {
  const safePurpose = purpose.replace(/[^a-z0-9]/gi, '').toLowerCase().slice(0, 4) || 'perf';
  const suffix = `${runToken}${index}`.toLowerCase();
  const nickname = `${safePurpose}${suffix}`.replace(/[^a-z0-9]/g, '').slice(0, 20);
  return {
    email: `${safePurpose}${suffix}@example.com`,
    nickname: nickname.length >= 2 ? nickname : `${safePurpose}${index}`,
    password: DEFAULT_PASSWORD,
  };
}

function extractAuth(loginResponse) {
  const accessToken = loginResponse.cookies.access_token?.[0]?.value;
  const refreshToken = loginResponse.cookies.refresh_token?.[0]?.value;

  if (!accessToken || !refreshToken) {
    throw new Error(`missing auth cookies for login status=${loginResponse.status}`);
  }

  return {
    accessToken,
    refreshToken,
  };
}

function loginSeededUsers(count) {
  const users = [];
  const seededOffset = Number.parseInt(__ENV.K6_SEEDED_USER_OFFSET || '0', 10);

  for (let index = 0; index < count; index += 1) {
    const seededIndex = seededOffset + index;
    const loginResponse = http.post(
      `${BASE_URL}/api/v1/auth/login`,
      JSON.stringify({
        email: `perfseed${seededIndex}@example.com`,
        password: DEFAULT_PASSWORD,
      }),
      {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'authLogin', purpose: 'seeded' },
      }
    );

    check(loginResponse, {
      'seeded login succeeded': (response) => response.status === 200,
    });

    if (loginResponse.status !== 200) {
      throw new Error(
        `seeded login failed status=${loginResponse.status} email=perfseed${seededIndex}@example.com. ` +
        'Run ./scripts/perf/load_seed_data.sh to provision verified perf users.'
      );
    }

    users.push({
      email: `perfseed${seededIndex}@example.com`,
      nickname: `perfuser${seededIndex}`,
      password: DEFAULT_PASSWORD,
      ...extractAuth(loginResponse),
    });
  }

  return users;
}

export function registerAndLoginUsers(count, purpose = 'perf') {
  const authMode = (__ENV.K6_AUTH_MODE || 'seeded').toLowerCase();
  if (authMode === 'seeded') {
    return loginSeededUsers(count);
  }

  const runToken = (__ENV.K6_RUN_TOKEN || Date.now().toString(36)).replace(/[^a-z0-9]/gi, '').slice(-6) || 'seeded';
  const users = [];

  for (let index = 0; index < count; index += 1) {
    const identity = userIdentity(purpose, runToken, index);
    const signupResponse = http.post(
      `${BASE_URL}/api/v1/auth/signup`,
      JSON.stringify({
        email: identity.email,
        password: identity.password,
        nickname: identity.nickname,
      }),
      {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'authSignup', purpose },
      }
    );

    if (![200, 409].includes(signupResponse.status)) {
      throw new Error(`signup failed status=${signupResponse.status} body=${signupResponse.body}`);
    }

    const loginResponse = http.post(
      `${BASE_URL}/api/v1/auth/login`,
      JSON.stringify({
        email: identity.email,
        password: identity.password,
      }),
      {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'authLogin', purpose },
      }
    );

    check(loginResponse, {
      'login succeeded': (response) => response.status === 200,
    });

    users.push({
      ...identity,
      ...extractAuth(loginResponse),
    });
  }

  return users;
}

export function authCookieHeader(auth) {
  return { Cookie: cookieHeader(auth) };
}
