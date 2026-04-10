function escapeHeaderValue(value) {
  return String(value).replace(/"/g, '%22');
}

function encodeAscii(value) {
  const normalized = String(value);
  const bytes = new Uint8Array(normalized.length);
  for (let index = 0; index < normalized.length; index += 1) {
    bytes[index] = normalized.charCodeAt(index) & 0xff;
  }
  return bytes;
}

function toUint8Array(value) {
  if (value instanceof Uint8Array) {
    return value;
  }
  if (value instanceof ArrayBuffer) {
    return new Uint8Array(value);
  }
  if (ArrayBuffer.isView(value)) {
    return new Uint8Array(value.buffer, value.byteOffset, value.byteLength);
  }
  return encodeAscii(value);
}

function safeFilename(filename, index) {
  const raw = String(filename || `upload-${index}.bin`);
  const extensionIndex = raw.lastIndexOf('.');
  const extension = extensionIndex >= 0 ? raw.slice(extensionIndex) : '';
  return `upload-${index}${extension}`.replace(/[^a-zA-Z0-9._-]/g, '_');
}

export function buildMultipartBody(files, fieldName = 'files') {
  const boundary = `----eventitta-k6-${Date.now().toString(16)}-${Math.random().toString(16).slice(2)}`;
  const chunks = [];

  files.forEach((file, index) => {
    chunks.push(
      encodeAscii(
        `--${boundary}\r\n`
        + `Content-Disposition: form-data; name="${escapeHeaderValue(fieldName)}"; `
        + `filename="${escapeHeaderValue(safeFilename(file.filename, index))}"\r\n`
        + `Content-Type: ${file.contentType}\r\n\r\n`
      )
    );
    chunks.push(toUint8Array(file.bytes));
    chunks.push(encodeAscii('\r\n'));
  });

  chunks.push(encodeAscii(`--${boundary}--\r\n`));

  const totalLength = chunks.reduce((sum, chunk) => sum + chunk.byteLength, 0);
  const body = new Uint8Array(totalLength);
  let offset = 0;
  chunks.forEach((chunk) => {
    body.set(chunk, offset);
    offset += chunk.byteLength;
  });

  return {
    body: body.buffer,
    boundary,
  };
}
