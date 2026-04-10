-- Eventitta 초기 데이터 삽입

-- 활동 타입 데이터 삽입
INSERT INTO activity_types (id, code, name, default_point)
VALUES (1, 'CREATE_POST', '게시글 작성', 10),
       (2, 'CREATE_COMMENT', '댓글 작성', 5),
       (3, 'LIKE_POST', '게시글 좋아요', 1),
       (4, 'JOIN_MEETING', '모임 참여', 20);

-- 기본 배지 데이터 삽입
INSERT INTO badges (id, name, description, icon_url)
VALUES (1, '첫 게시글', '첫 번째 게시글을 작성하여 커뮤니티 활동을 시작했습니다.', 'https://eventitta.com/icons/first_post.png'),
       (2, '열혈 댓글러', '댓글을 10개 이상 작성하여 활발하게 소통했습니다.', 'https://eventitta.com/icons/commenter.png'),
       (3, '첫 모임 참가', '첫 번째 모임에 참가하여 새로운 인연을 만들었습니다.', 'https://eventitta.com/icons/first_meeting.png'),
       (4, '프로 좋아요꾼', '다른 사람의 게시글에 좋아요를 50회 이상 눌렀습니다.', 'https://eventitta.com/icons/pro_liker.png');

-- 배지 규칙 데이터 삽입
INSERT INTO badge_rules (id, badge_id, activity_type_id, threshold, enabled)
VALUES (1, 1, 1, 1, true),  -- 첫 게시글: CREATE_POST 1회
       (2, 2, 2, 10, true), -- 열혈 댓글러: CREATE_COMMENT 10회
       (3, 3, 4, 1, true),  -- 첫 모임 참가: JOIN_MEETING 1회
       (4, 4, 3, 50, true); -- 프로 좋아요꾼: LIKE_POST 50회
