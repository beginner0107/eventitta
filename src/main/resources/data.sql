INSERT INTO region(code, name, parent_code, level)
VALUES ('1100000000', '서울특별시', NULL, 1),
       ('1100100000', '서울특별시 종로구', '1100000000', 2),
       ('1100110100', '서울특별시 종로구 청운효자동', '1100100000', 3),
       ('1100200000', '서울특별시 중구', '1100000000', 2),
       ('1100210300', '서울특별시 중구 회현동', '1100200000', 3),

       ('2600000000', '부산광역시', NULL, 1),
       ('2601000000', '부산광역시 중구', '2600000000', 2),
       ('2601010100', '부산광역시 중구 중앙동', '2601000000', 3),
       ('2602000000', '부산광역시 서구', '2600000000', 2),
       ('2602010100', '부산광역시 서구 남부민1동', '2602000000', 3),

       ('4100000000', '경기도', NULL, 1),
       ('4101000000', '경기도 수원시', '4100000000', 2),
       ('4101025000', '경기도 수원시 권선구', '4101000000', 3),
       ('4101100000', '경기도 용인시 기흥구', '4100000000', 2),
       ('4101107300', '경기도 용인시 기흥구 보정동', '4101100000', 3),
       ('4101107400', '경기도 용인시 기흥구 상갈동', '4101100000', 3),

       ('2800000000', '대구광역시', NULL, 1),
       ('2801000000', '대구광역시 중구', '2800000000', 2),
       ('2801010100', '대구광역시 중구 동성로1가', '2801000000', 3),
       ('2900000000', '인천광역시', NULL, 1);

INSERT INTO activity_types (id, code, name, default_point)
VALUES (1, 'CREATE_POST', '게시글 작성', 10),
       (2, 'CREATE_COMMENT', '댓글 작성', 5),
       (3, 'LIKE_POST', '게시글 좋아요', 1),
       (4, 'JOIN_MEETING', '모임 참여', 20)
ON DUPLICATE KEY UPDATE code          = VALUES(code),
                        name          = VALUES(name),
                        default_point = VALUES(default_point);

-- 배지 삽입
INSERT INTO badges (id, name, description, icon_url)
VALUES (1, '첫 게시글', '첫 번째 게시글을 작성하여 커뮤니티 활동을 시작했습니다.', 'https://eventitta.com/icons/first_post.png'),
       (2, '열혈 댓글러', '댓글을 10개 이상 작성하여 활발하게 소통했습니다.', 'https://eventitta.com/icons/commenter.png'),
       (3, '첫 모임 참가', '첫 번째 모임에 참가하여 새로운 인연을 만들었습니다.', 'https://eventitta.com/icons/first_meeting.png'),
       (4, '프로 좋아요꾼', '다른 사람의 게시글에 좋아요를 50회 이상 눌렀습니다.', 'https://eventitta.com/icons/pro_liker.png')
ON DUPLICATE KEY UPDATE name        = VALUES(name),
                        description = VALUES(description),
                        icon_url    = VALUES(icon_url);

-- 배지 규칙 삽입
INSERT INTO badge_rules (id, badge_id, activity_type_id, threshold, enabled)
VALUES (1, 1, 1, 1, true),  -- CREATE_POST
       (2, 2, 2, 10, true), -- CREATE_COMMENT
       (3, 3, 4, 1, true),  -- JOIN_MEETING
       (4, 4, 3, 50, true)  -- LIKE_POST
ON DUPLICATE KEY UPDATE badge_id         = VALUES(badge_id),
                        activity_type_id = VALUES(activity_type_id),
                        threshold        = VALUES(threshold),
                        enabled          = VALUES(enabled);
