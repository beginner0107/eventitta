package com.eventitta.meeting;

import com.eventitta.meeting.domain.Meeting;
import com.eventitta.meeting.domain.MeetingStatus;
import com.eventitta.meeting.repository.MeetingParticipantRepository;
import com.eventitta.meeting.repository.MeetingRepository;
import com.eventitta.meeting.service.MeetingService;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class MeetingConcurrencyTest {

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MeetingParticipantRepository participantRepository;

    @Autowired
    private UserRepository userRepository;

    private Meeting meeting;
    private List<User> users;

    @BeforeEach
    void setUp() {
        participantRepository.deleteAll();
        meetingRepository.deleteAll();
        userRepository.deleteAll();

        users = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            User user = User.builder()
                .email("user" + i + "@test.com")
                .nickname("사용자" + i)
                .password("password")
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .points(0)
                .build();
            users.add(userRepository.save(user));
        }

        // 정원 10명인 모임 생성
        User leader = users.get(0);
        meeting = Meeting.builder()
            .title("인기 모임 - 선착순 10명")
            .description("동시 접속 테스트용 모임")
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
            .maxMembers(10)
            .address("서울시 강남구")
            .status(MeetingStatus.RECRUITING)
            .leader(leader)
            .build();
        meeting = meetingRepository.save(meeting);
    }

    @Test
    @DisplayName("20명이 동시에 참가 신청하여 정원 10명 초과 발생")
    void testRaceCondition_WithoutLock() throws InterruptedException {
        // Given
        int participantCount = 19;
        int maxMembers = meeting.getMaxMembers();

        ExecutorService executorService = Executors.newFixedThreadPool(participantCount);
        CountDownLatch latch = new CountDownLatch(participantCount);

        // When
        for (int i = 1; i < users.size(); i++) {
            final Long userId = users.get(i).getId();
            final Long meetingId = meeting.getId();

            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    // 참가 신청
                    meetingService.joinMeeting(userId, meetingId);
                } catch (Exception ignored) {
                }
            });
        }

        executorService.shutdown();
        boolean finished = executorService.awaitTermination(10, SECONDS);
        assertThat(finished).isTrue();

        // Then
        int actualParticipants = participantRepository.countByMeetingId(meeting.getId());

        assertThat(actualParticipants)
            .isGreaterThan(maxMembers);
    }
}

