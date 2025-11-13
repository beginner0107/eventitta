# generate_region_sql.py
#
# 역할: 국토교통부 법정동 코드 CSV → Flyway INSERT SQL 생성
#
# 관심사 분리 원칙:
# ✅ Flyway (스키마 관리):
#    - CREATE TABLE regions (V1__Create_schema.sql)
#    - ALTER TABLE regions (V*__*.sql)
#    - 스키마 = 단일 진실의 원천 (Single Source of Truth)
#
# ✅ Python (데이터 관리):
#    - CSV 읽기 → INSERT SQL 생성
#    - 레벨 계산, 인코딩 감지, 배치 처리
#    - 스키마 정의 안 함 (CREATE TABLE LIKE regions 활용)
#
# 왜 이렇게 설계했나?
# - regions 테이블에 컬럼 추가/삭제 → Python 수정 불필요!
# - CREATE TABLE LIKE regions가 자동으로 최신 스키마 복사
# - 신규 개발자는 Flyway만 보면 됨
#
import csv
import sys

def detect_encoding(file_path):
    """파일 인코딩 자동 감지"""
    encodings = ['utf-8-sig', 'utf-8', 'cp949', 'euc-kr', 'latin1']

    for encoding in encodings:
        try:
            with open(file_path, 'r', encoding=encoding) as f:
                f.read(1024)
                return encoding
        except (UnicodeDecodeError, LookupError):
            continue

    return 'utf-8'

def get_level(code):
    """법정동 코드로부터 레벨 계산"""
    code = str(code).strip()
    if len(code) != 10:
        return None

    # 레벨 1: 시/도 (뒤 8자리가 모두 0)
    if code[2:10] == '00000000':
        return 1

    # 레벨 2: 시/군/구 (뒤 5자리가 모두 0, 뒤 8자리는 아님)
    # 예: 1111000000 (종로구), 1121500000 (광진구), 5211000000 (전주시)
    elif code[5:10] == '00000':
        return 2

    # 레벨 3: 읍/면/동 (뒤 3자리가 모두 0, 뒤 5자리는 아님)
    # 예: 1111010100 (청운동), 5211100000 (완산구)
    elif code[7:10] == '000':
        return 3

    # 레벨 4: 리
    else:
        return 4

def get_parent_code(code, level):
    """레벨에 맞는 parent_code 계산"""
    code = str(code).strip()

    if level == 1:
        return None
    elif level == 2:
        # 시/도 코드 (앞 2자리 + 00000000)
        return code[:2] + '00000000'
    elif level == 3:
        # 시/군/구 코드 (앞 5자리 + 00000)
        return code[:5] + '00000'
    elif level == 4:
        # 읍/면/동 코드 (앞 7자리 + 000)
        return code[:7] + '000'
    return None

def is_active(status):
    """폐지여부가 '존재'인지 확인"""
    status_clean = status.strip() if status else ''
    return status_clean in ['존재', '存在', '']  # 빈 값도 존재로 간주

def escape_sql_string(s):
    """SQL 문자열 이스케이프"""
    if s is None:
        return None
    return s.replace("'", "''").replace("\\", "\\\\")

def generate_insert_sql(csv_file, output_file, max_level=3, use_rename_strategy=True):
    """CSV를 읽어서 올바른 INSERT SQL 생성

    Args:
        csv_file: 입력 CSV 파일 경로
        output_file: 출력 SQL 파일 경로
        max_level: 포함할 최대 레벨 (기본: 3)
        use_rename_strategy: RENAME 전략 사용 여부 (기본: True, 무중단 배포)
    """

    print(f"📖 CSV 파일 읽는 중: {csv_file}")

    # 인코딩 자동 감지
    encoding = detect_encoding(csv_file)
    print(f"🔍 감지된 인코딩: {encoding}")

    regions = []
    skipped = 0
    skipped_reasons = {'폐지': 0, '레벨제한': 0, '기타': 0}

    try:
        with open(csv_file, 'r', encoding=encoding) as f:
            # 구분자 감지 (탭 우선)
            sample = f.read(200)
            f.seek(0)

            if '\t' in sample:
                delimiter = '\t'
                print(f"📋 구분자: 탭 (\\t)")
            elif ',' in sample:
                delimiter = ','
                print(f"📋 구분자: 콤마 (,)")
            else:
                delimiter = '\t'  # 기본값
                print(f"📋 구분자: 탭 (기본값)")

            reader = csv.DictReader(f, delimiter=delimiter)

            # CSV 필드명 정규화
            fieldnames = reader.fieldnames
            if not fieldnames:
                print("❌ CSV 필드명을 읽을 수 없습니다!")
                sys.exit(1)

            print(f"📌 필드명: {fieldnames}")

            # 필드명 매핑
            code_field = None
            name_field = None
            status_field = None

            for field in fieldnames:
                field_clean = field.strip()
                if '법정동코드' in field_clean or 'code' in field_clean.lower():
                    code_field = field
                elif '법정동명' in field_clean or 'name' in field_clean.lower():
                    name_field = field
                elif '폐지' in field_clean or 'status' in field_clean.lower():
                    status_field = field

            if not code_field or not name_field:
                print(f"❌ 필수 필드를 찾을 수 없습니다! code: {code_field}, name: {name_field}")
                sys.exit(1)

            print(f"✅ 필드 매핑: code={code_field}, name={name_field}, status={status_field}")

            # 데이터 처리
            for row_num, row in enumerate(reader, start=2):
                code = row.get(code_field, '').strip()
                name = row.get(name_field, '').strip()
                status = row.get(status_field, '').strip() if status_field else '존재'

                if not code or not name:
                    skipped += 1
                    skipped_reasons['기타'] += 1
                    continue

                # 폐지된 지역 제외
                if not is_active(status):
                    skipped += 1
                    skipped_reasons['폐지'] += 1
                    continue

                # 레벨 계산
                level = get_level(code)

                if level is None:
                    skipped += 1
                    skipped_reasons['기타'] += 1
                    continue

                # max_level 제한
                if level > max_level:
                    skipped += 1
                    skipped_reasons['레벨제한'] += 1
                    continue

                # parent_code 계산
                parent_code = get_parent_code(code, level)

                regions.append({
                    'code': code,
                    'name': escape_sql_string(name),
                    'parent_code': parent_code,
                    'level': level
                })

                # 진행 상황 표시
                if len(regions) % 5000 == 0:
                    print(f"  처리 중... {len(regions):,}개")

        print(f"\n✅ 총 {len(regions):,}개 레코드 처리 완료")
        print(f"⏭️  {skipped:,}개 레코드 스킵:")
        print(f"   - 폐지: {skipped_reasons['폐지']:,}개")
        print(f"   - 레벨 제한 (>{max_level}): {skipped_reasons['레벨제한']:,}개")
        print(f"   - 기타: {skipped_reasons['기타']:,}개")

    except Exception as e:
        print(f"❌ CSV 읽기 오류: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

    # 레벨별, 코드순 정렬
    regions.sort(key=lambda x: (x['level'], x['code']))

    # SQL 파일 생성
    print(f"\n📝 SQL 파일 생성 중: {output_file}")

    try:
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write("-- ==========================================\n")
            f.write("-- Region 데이터 INSERT (자동 생성)\n")
            f.write("-- 국토교통부 법정동 코드 기준\n")
            f.write(f"-- 총 {len(regions):,}개 레코드\n")
            f.write(f"-- 최대 레벨: {max_level}\n")

            if use_rename_strategy:
                f.write("-- 전략: 임시 테이블 + RENAME (무중단 배포)\n")
            else:
                f.write("-- 전략: 직접 교체 (DELETE + INSERT)\n")

            f.write("-- ==========================================\n\n")

            # RENAME 전략 (무중단)
            if use_rename_strategy:
                f.write("-- ==========================================\n")
                f.write("-- Step 1: 임시 테이블 생성\n")
                f.write("-- ==========================================\n\n")
                f.write("-- ✅ 스키마 관리: Flyway가 책임 (단일 진실의 원천)\n")
                f.write("-- ✅ 데이터 관리: Python 스크립트가 책임 (관심사 분리)\n")
                f.write("-- ✅ 동기화: CREATE TABLE LIKE로 자동 (스키마 변경 시 안전)\n\n")
                f.write("-- 기존 regions_new가 있으면 삭제\n")
                f.write("DROP TABLE IF EXISTS regions_new;\n\n")
                f.write("-- 현재 regions 테이블과 동일한 구조로 생성 (스키마 자동 복사)\n")
                f.write("-- ⚠️ 주의: 외래 키는 복사되지 않음 (하지만 regions는 외래 키 없음)\n")
                f.write("CREATE TABLE regions_new LIKE regions;\n\n")
                f.write("-- ==========================================\n")
                f.write("-- Step 2: 새 데이터를 regions_new에 INSERT\n")
                f.write("-- ==========================================\n\n")
                target_table = "regions_new"
            else:
                # 기존 DELETE 방식
                f.write("-- ==========================================\n")
                f.write("-- 기존 데이터 삭제\n")
                f.write("-- ⚠️  주의: 이 작업은 되돌릴 수 없습니다!\n")
                f.write("-- ⚠️  다운타임 발생 가능\n")
                f.write("-- ==========================================\n\n")
                f.write("-- 외래 키 체크 임시 비활성화 (MySQL)\n")
                f.write("SET FOREIGN_KEY_CHECKS = 0;\n\n")
                f.write("-- regions 테이블 데이터 전체 삭제\n")
                f.write("DELETE FROM regions WHERE 1=1;\n\n")
                f.write("-- 외래 키 체크 다시 활성화\n")
                f.write("SET FOREIGN_KEY_CHECKS = 1;\n\n")
                f.write("-- ==========================================\n")
                f.write("-- 새 데이터 INSERT\n")
                f.write("-- ==========================================\n\n")
                target_table = "regions"

            # 배치 크기
            batch_size = 500
            total_batches = (len(regions) + batch_size - 1) // batch_size

            for batch_num in range(total_batches):
                start_idx = batch_num * batch_size
                end_idx = min(start_idx + batch_size, len(regions))
                batch = regions[start_idx:end_idx]

                f.write(f"-- Batch {batch_num + 1}/{total_batches}\n")
                f.write(f"INSERT INTO {target_table} (code, name, parent_code, level)\nVALUES\n")

                for i, region in enumerate(batch):
                    if region['parent_code']:
                        line = f"  ('{region['code']}', '{region['name']}', '{region['parent_code']}', {region['level']})"
                    else:
                        line = f"  ('{region['code']}', '{region['name']}', NULL, {region['level']})"

                    if i < len(batch) - 1:
                        line += ","
                    else:
                        line += ";"

                    f.write(line + "\n")

                f.write("\n")

            # RENAME 전략: 원자적 테이블 교체
            if use_rename_strategy:
                f.write("-- ==========================================\n")
                f.write("-- Step 3: 원자적 테이블 교체 (무중단)\n")
                f.write("-- ⚠️  이 작업은 1ms 이하로 완료됩니다\n")
                f.write("-- ==========================================\n\n")
                f.write("-- 백업 테이블이 이미 있으면 삭제 (선택적)\n")
                f.write("-- DROP TABLE IF EXISTS regions_old;\n\n")
                f.write("-- 원자적 교체: regions → regions_old, regions_new → regions\n")
                f.write("RENAME TABLE \n")
                f.write("    regions TO regions_old,\n")
                f.write("    regions_new TO regions;\n\n")
                f.write("-- ==========================================\n")
                f.write("-- Step 4: 옛날 테이블 정리 (나중에 수동 실행)\n")
                f.write("-- ==========================================\n\n")
                f.write("-- ⚠️  주의: 검증 후 실행하세요!\n")
                f.write("-- ⚠️  롤백이 필요하면 아래 명령 실행:\n")
                f.write("--   RENAME TABLE regions TO regions_new, regions_old TO regions;\n\n")
                f.write("-- 검증이 완료되면 옛날 백업 삭제 (선택)\n")
                f.write("-- DROP TABLE IF EXISTS regions_old;\n\n")

        print(f"✅ SQL 파일 생성 완료!")

        # 통계 출력
        level_counts = {}
        for region in regions:
            level = region['level']
            level_counts[level] = level_counts.get(level, 0) + 1

        print("\n📊 레벨별 통계:")
        level_names = {1: '시/도', 2: '시/군/구', 3: '읍/면/동', 4: '리'}
        for level in sorted(level_counts.keys()):
            print(f"  Level {level} ({level_names.get(level, '기타')}): {level_counts[level]:,}개")

        # 샘플 데이터 표시
        print("\n📋 샘플 데이터 (처음 10개):")
        for i, region in enumerate(regions[:10]):
            parent_str = f" → {region['parent_code']}" if region['parent_code'] else ""
            print(f"  {region['code']} {region['name']} (L{region['level']}){parent_str}")

    except Exception as e:
        print(f"❌ SQL 파일 생성 오류: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    import os

    # 프로젝트 루트 디렉토리 계산
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.abspath(os.path.join(script_dir, "../.."))

    csv_file = os.path.join(script_dir, "regions_source_20250805.csv")
    output_file = os.path.join(project_root, "src/main/resources/db/migration/V9__Fix_region_hierarchy.sql")
    max_level = 3  # 리(4) 제외
    use_rename_strategy = True  # 무중단 배포 (RENAME 전략)

    print("=" * 70)
    print("🏢 법정동 코드 SQL 생성기 v5 (RENAME 전략)")
    print("=" * 70)
    print(f"📂 프로젝트 루트: {project_root}")
    print(f"📖 입력 CSV: {csv_file}")
    print(f"📝 출력 SQL: {output_file}")
    print()

    if use_rename_strategy:
        print("✅ 배포 전략: 임시 테이블 + RENAME (무중단)")
        print("   - 다운타임: 0초")
        print("   - 외래 키: 안전")
        print("   - 롤백: 가능 (regions_old 보존)")
    else:
        print("⚠️  배포 전략: DELETE + INSERT (다운타임 발생)")
        print("⚠️  외래 키 참조가 있는 경우 문제가 발생할 수 있습니다.")
    print()

    generate_insert_sql(csv_file, output_file, max_level, use_rename_strategy)

    print("\n" + "=" * 70)
    print("✨ 다음 단계:")
    print("=" * 70)

    if use_rename_strategy:
        print("1. 애플리케이션 재시작 (Flyway가 자동 실행)")
        print("   ./gradlew bootRun")
        print()
        print("2. 데이터 검증")
        print("   curl http://localhost:8080/api/v1/regions/1111010100/hierarchy | jq .")
        print()
        print("3. 검증 완료 후 regions_old 삭제 (선택)")
        print("   mysql -u root -p eventitta -e \"DROP TABLE IF EXISTS regions_old;\"")
        print()
        print("4. 롤백이 필요한 경우")
        print("   mysql -u root -p eventitta << 'EOF'")
        print("   RENAME TABLE regions TO regions_failed, regions_old TO regions;")
        print("   EOF")
    else:
        print("mysql -u root -p eventitta << 'EOF'")
        print("SET FOREIGN_KEY_CHECKS=0;")
        print("DELETE FROM regions;")
        print("SET FOREIGN_KEY_CHECKS=1;")
        print("EOF")
        print()
        print("./gradlew clean bootRun")

    print("=" * 70)
