export const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
export const DEFAULT_PASSWORD = __ENV.K6_DEFAULT_PASSWORD || 'Pass123!';
export const DEFAULT_REGION_CODES = parseCsvStrings(
  __ENV.POST_REGION_CODES,
  ['1100000000', '1168000000', '2611000000', '2711000000', '2811000000']
);
export const DEFAULT_KEYWORDS = parseCsvStrings(
  __ENV.POST_SEARCH_KEYWORDS,
  ['맛집', '러닝', '공연', '동네', '추천']
);
export const HOT_POST_IDS = parseCsvNumbers(
  __ENV.HOT_POST_IDS,
  [2000000, 2000001, 2000002, 2000003, 2000004]
);

export function intEnv(name, fallback) {
  const raw = __ENV[name];
  if (!raw) {
    return fallback;
  }
  const parsed = Number.parseInt(raw, 10);
  return Number.isNaN(parsed) ? fallback : parsed;
}

export function parseCsvStrings(raw, fallback = []) {
  if (!raw) {
    return fallback;
  }
  const values = raw.split(',').map((value) => value.trim()).filter(Boolean);
  return values.length > 0 ? values : fallback;
}

export function parseCsvNumbers(raw, fallback = []) {
  return parseCsvStrings(raw).map((value) => Number.parseInt(value, 10)).filter((value) => !Number.isNaN(value)).length > 0
    ? parseCsvStrings(raw).map((value) => Number.parseInt(value, 10)).filter((value) => !Number.isNaN(value))
    : fallback;
}

function parseStages(defaultStages) {
  if (!__ENV.STAGES_JSON) {
    return defaultStages;
  }
  return JSON.parse(__ENV.STAGES_JSON);
}

export function commonOptions(tags = {}, thresholds = {}, defaultStages = null) {
  const stages = parseStages(defaultStages || [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 30 },
    { duration: '2m', target: 30 },
    { duration: '30s', target: 0 },
  ]);

  return {
    stages,
    thresholds: {
      http_req_duration: ['p(95)<500'],
      http_req_failed: ['rate<0.01'],
      ...thresholds,
    },
    tags,
  };
}

export function safeJson(response) {
  try {
    return JSON.parse(response.body);
  } catch (error) {
    return null;
  }
}

export function cookieHeader(auth) {
  return `access_token=${auth.accessToken}; refresh_token=${auth.refreshToken}`;
}

export function authParams(auth, extra = {}) {
  return {
    ...extra,
    headers: {
      ...(extra.headers || {}),
      Cookie: cookieHeader(auth),
    },
  };
}

export function jsonAuthParams(auth, extra = {}) {
  return authParams(auth, {
    ...extra,
    headers: {
      'Content-Type': 'application/json',
      ...(extra.headers || {}),
    },
  });
}

export function randomItem(items) {
  return items[Math.floor(Math.random() * items.length)];
}

export function basename(path) {
  const normalized = path.replace(/\\/g, '/');
  return normalized.substring(normalized.lastIndexOf('/') + 1);
}

export function pathFromPublicUrl(publicUrl) {
  if (!publicUrl) {
    return null;
  }
  if (publicUrl.startsWith('/')) {
    return publicUrl;
  }
  const normalized = publicUrl.replace(/^https?:\/\/[^/]+/i, '');
  return normalized.startsWith('/') ? normalized : `/${normalized}`;
}

export function contentTypeFromPath(path) {
  const lowered = path.toLowerCase();
  if (lowered.endsWith('.jpg') || lowered.endsWith('.jpeg')) {
    return 'image/jpeg';
  }
  if (lowered.endsWith('.gif')) {
    return 'image/gif';
  }
  if (lowered.endsWith('.webp')) {
    return 'image/webp';
  }
  return 'image/png';
}

export function buildSummary(name, data, metricNames = []) {
  const lines = [];
  lines.push('');
  lines.push('========================================');
  lines.push(` ${name}`);
  lines.push('========================================');
  lines.push('');
  lines.push(`[BASE_URL] ${BASE_URL}`);
  lines.push(`[Requests] ${data.metrics.http_reqs?.values?.count ?? 0}`);
  lines.push(`[Errors] ${(((data.metrics.http_req_failed?.values?.rate) || 0) * 100).toFixed(2)}%`);
  lines.push(`[Avg] ${((data.metrics.http_req_duration?.values?.avg) || 0).toFixed(2)}ms`);
  lines.push(`[p95] ${((data.metrics.http_req_duration?.values?.['p(95)']) || 0).toFixed(2)}ms`);
  metricNames.forEach((metricName) => {
    const metric = data.metrics[metricName];
    if (!metric || !metric.values) {
      return;
    }
    lines.push(`[${metricName}] avg=${(metric.values.avg || 0).toFixed(2)}ms p95=${(metric.values['p(95)'] || 0).toFixed(2)}ms`);
  });
  lines.push('');

  return {
    stdout: `${lines.join('\n')}\n`,
    [`${name.replace(/\s+/g, '_').toLowerCase()}_summary.json`]: JSON.stringify(data, null, 2),
  };
}
