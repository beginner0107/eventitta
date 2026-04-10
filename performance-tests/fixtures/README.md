# Upload Fixtures

- 기본 fixture는 `sample-upload.png` 하나만 포함합니다.
- 실제 메모리/GC 압박을 보려면 3MB~8MB 이미지 여러 장을 별도로 준비한 뒤 `UPLOAD_FIXTURE_PATHS=/abs/a.png,/abs/b.png` 형태로 전달하세요.
- k6는 스크립트 기준 상대 경로도 읽을 수 있으므로, 필요하면 이 디렉터리에 큰 샘플 이미지를 추가해도 됩니다.
