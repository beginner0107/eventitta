package com.eventitta.meeting;

import com.eventitta.common.config.redis.MockRedisConfig;
import com.eventitta.meeting.domain.Meeting;
import com.eventitta.meeting.domain.MeetingParticipant;
import com.eventitta.meeting.domain.MeetingStatus;
import com.eventitta.meeting.domain.ParticipantStatus;
import com.eventitta.meeting.repository.MeetingParticipantRepository;
import com.eventitta.meeting.repository.MeetingRepository;
import com.eventitta.meeting.service.MeetingService;
import com.eventitta.gamification.event.ActivityEventPublisher;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(MockRedisConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MeetingConcurrencyTest {

    private static final String TEST_PASSWORD = "TestPass123!";

    @Autowired
    private MeetingService meetingService;
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private MeetingParticipantRepository participantRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private ActivityEventPublisher activityEventPublisher;

    @BeforeEach
    void setUp() {
        participantRepository.deleteAll();
        meetingRepository.deleteAll();
        userRepository.deleteAll();
        entityManager.clear();
    }

    @ParameterizedTest(name = "concurrent approvals maxMembers={0} currentApproved={1} pendingUsers={2}")
    @DisplayName("동시에 승인해도 maxMembers를 절대 초과하면 안 된다")
    @CsvSource({
        "10,5,20",
        "100,10,200"
    })
    void givenMultiplePendingParticipants_whenConcurrentApproval_thenShouldNotExceedMaxMembers(int maxMembers, int currentApproved, int pendingUsers) throws Exception {
        MeetingSetup setup = prepareMeeting(maxMembers, currentApproved, pendingUsers);

        ExecutorService executor = Executors.newFixedThreadPool(pendingUsers);
        CountDownLatch ready = new CountDownLatch(pendingUsers);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(pendingUsers);
        TestResult result = new TestResult();

        for (Long participantId : setup.pendingParticipantIds()) {
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    meetingService.approveParticipant(setup.leaderId(), setup.meetingId(), participantId);
                    result.successCount.incrementAndGet();
                } catch (Exception e) {
                    result.failCount.incrementAndGet();
                    result.failures.add(e);
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await();
        executor.shutdown();

        entityManager.clear();

        Meeting refreshed = meetingRepository.findById(setup.meetingId()).orElseThrow();
        int approvedCount = participantRepository.countByMeetingIdAndStatus(setup.meetingId(), ParticipantStatus.APPROVED);
        int capacityRemaining = Math.max(0, maxMembers - currentApproved);

        assertAll(
            () -> assertEquals(capacityRemaining, result.successCount.get(), "성공 승인 수는 남은 정원(" + capacityRemaining + ")과 같아야 함"),
            () -> assertEquals(pendingUsers, result.successCount.get() + result.failCount.get(), "성공+실패 합은 시도한 pending 수와 같아야 함"),
            () -> assertEquals(Math.min(maxMembers, currentApproved + result.successCount.get()), approvedCount, "최종 APPROVED 수 검증"),
            () -> assertEquals(approvedCount, refreshed.getCurrentMembers(), "currentMembers 는 APPROVED 수와 동일해야 함"),
            () -> assertEquals(maxMembers, refreshed.getMaxMembers(), "maxMembers 변경되면 안 됨"),
            () -> assertTrue(approvedCount <= maxMembers, "승인 수가 정원(maxMembers) 초과하지 않아야 함")
        );
    }

    private static class TestResult {
        final AtomicInteger successCount = new AtomicInteger();
        final AtomicInteger failCount = new AtomicInteger();
        final List<Exception> failures = Collections.synchronizedList(new ArrayList<>());
    }

    private record MeetingSetup(Long meetingId, Long leaderId, List<Long> pendingParticipantIds,
                                int initialApproved, int maxMembers) {
    }

    private MeetingSetup prepareMeeting(int maxMembers, int currentApproved, int pendingUsers) {
        User leader = User.builder()
            .email("leader@test.com")
            .nickname("leader")
            .password("testpassword1234!@#F")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .points(0)
            .build();
        leader = userRepository.save(leader);

        Meeting meeting = Meeting.builder()
            .title("동시성 테스트")
            .description("approve race")
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
            .maxMembers(maxMembers)
            .currentMembers(currentApproved)  // 리더 포함한 현재 승인된 인원 수 설정
            .address("서울")
            .status(MeetingStatus.RECRUITING)
            .leader(leader)
            .build();
        meeting = meetingRepository.save(meeting);

        MeetingParticipant leaderParticipant = MeetingParticipant.builder()
            .meeting(meeting)
            .user(leader)
            .status(ParticipantStatus.APPROVED)
            .build();
        participantRepository.save(leaderParticipant);

        for (int i = 0; i < currentApproved - 1; i++) {
            User u = User.builder()
                .email("approved" + i + "@test.com")
                .nickname("appr" + i)
                .password(TEST_PASSWORD)
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .points(0)
                .build();
            u = userRepository.save(u);
            MeetingParticipant approvedP = MeetingParticipant.builder()
                .meeting(meeting)
                .user(u)
                .status(ParticipantStatus.APPROVED)
                .build();
            participantRepository.save(approvedP);
        }

        List<Long> pendingIds = new ArrayList<>();
        for (int i = 0; i < pendingUsers; i++) {
            User u = User.builder()
                .email("pending" + i + "@test.com")
                .nickname("pend" + i)
                .password(TEST_PASSWORD)
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .points(0)
                .build();
            u = userRepository.save(u);
            MeetingParticipant pending = MeetingParticipant.builder()
                .meeting(meeting)
                .user(u)
                .status(ParticipantStatus.PENDING)
                .build();
            pendingIds.add(participantRepository.save(pending).getId());
        }

        entityManager.clear();
        return new MeetingSetup(meeting.getId(), leader.getId(), pendingIds, currentApproved, maxMembers);
    }

}
