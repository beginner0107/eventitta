package com.eventitta.scheduler;

import com.eventitta.auth.repository.RefreshTokenRepository;
import com.eventitta.auth.schedule.RefreshTokenCleanupTask;
import com.eventitta.festivals.scheduler.FestivalScheduler;
import com.eventitta.festivals.service.FestivalService;
import com.eventitta.meeting.domain.MeetingStatus;
import com.eventitta.meeting.repository.MeetingRepository;
import com.eventitta.meeting.scheduler.MeetingStatusScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("스케줄러 기본 동작 테스트 - 각 스케줄러가 올바르게 작동하는지 확인")
class SchedulerUnitTest {

    @Mock
    private FestivalService festivalService;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private FestivalScheduler festivalScheduler;

    @InjectMocks
    private MeetingStatusScheduler meetingStatusScheduler;

    @InjectMocks
    private RefreshTokenCleanupTask refreshTokenCleanupTask;

    @Test
    @DisplayName("전국 축제 데이터 가져오기가 성공적으로 완료되는지 테스트")
    void syncNationalFestivalData_Success() {
        // given
        doNothing().when(festivalService).loadInitialNationalFestivalData();

        // when
        festivalScheduler.syncNationalFestivalData();

        // then
        verify(festivalService, times(1)).loadInitialNationalFestivalData();
    }

    @Test
    @DisplayName("전국 축제 데이터 가져오기에서 에러가 발생해도 시스템이 멈추지 않는지 테스트")
    void syncNationalFestivalData_ExceptionHandling() {
        // given
        doThrow(new RuntimeException("API 호출 실패"))
            .when(festivalService).loadInitialNationalFestivalData();

        // when & then - 예외가 발생해도 스케줄러는 중단되지 않음
        festivalScheduler.syncNationalFestivalData();

        verify(festivalService, times(1)).loadInitialNationalFestivalData();
    }

    @Test
    @DisplayName("서울 축제 데이터 가져오기가 성공적으로 완료되는지 테스트")
    void syncSeoulFestivalData_Success() {
        // given
        doNothing().when(festivalService).syncDailySeoulFestivalData();

        // when
        festivalScheduler.syncSeoulFestivalData();

        // then
        verify(festivalService, times(1)).syncDailySeoulFestivalData();
    }

    @Test
    @DisplayName("서울 축제 데이터 가져오기에서 에러가 발생해도 시스템이 멈추지 않는지 테스트")
    void syncSeoulFestivalData_ExceptionHandling() {
        // given
        doThrow(new RuntimeException("API 호출 실패"))
            .when(festivalService).syncDailySeoulFestivalData();

        // when & then - 예외가 발생해도 스케줄러는 중단되지 않음
        festivalScheduler.syncSeoulFestivalData();

        verify(festivalService, times(1)).syncDailySeoulFestivalData();
    }

    @Test
    @DisplayName("모임이 끝났을 때 상태를 '완료'로 바꾸는 기능이 올바르게 작동하는지 테스트")
    void markFinishedMeetings_Success() {
        // given
        int expectedUpdatedCount = 5;
        when(meetingRepository.updateStatusToFinished(eq(MeetingStatus.FINISHED), any(LocalDateTime.class)))
            .thenReturn(expectedUpdatedCount);

        // when
        meetingStatusScheduler.markFinishedMeetings();

        // then
        verify(meetingRepository, times(1))
            .updateStatusToFinished(eq(MeetingStatus.FINISHED), any(LocalDateTime.class));
        // 실제 반환값이 사용되는지도 검증할 수 있다면 추가하는 것이 좋습니다
    }

    @Test
    @DisplayName("모임 상태 업데이트에서 에러가 발생해도 시스템이 멈추지 않는지 테스트")
    void markFinishedMeetings_ExceptionHandling() {
        // given
        when(meetingRepository.updateStatusToFinished(eq(MeetingStatus.FINISHED), any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("데이터베이스 연결 실패"));

        // when & then - 예외가 발생해도 스케줄러는 중단되지 않음
        meetingStatusScheduler.markFinishedMeetings();

        verify(meetingRepository, times(1))
            .updateStatusToFinished(eq(MeetingStatus.FINISHED), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("만료된 로그인 토큰을 자동으로 삭제하는 기능이 올바르게 작동하는지 테스트")
    void removeExpiredRefreshTokens_Success() {
        // given
        long expectedDeletedCount = 10L;
        when(refreshTokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class)))
            .thenReturn(expectedDeletedCount);

        // when
        refreshTokenCleanupTask.removeExpiredRefreshTokens();

        // then
        verify(refreshTokenRepository, times(1))
            .deleteByExpiresAtBefore(any(LocalDateTime.class));
        // 실제 반환값이 사용되는지도 검증할 수 있다면 추가하는 것이 좋습니다
    }

    @Test
    @DisplayName("만료된 토큰 삭제에서 에러가 발생해도 시스템이 멈추지 않는지 테스트")
    void removeExpiredRefreshTokens_ExceptionHandling() {
        // given
        when(refreshTokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("데이터베이스 연결 실패"));

        // when & then - 예외가 발생해도 스케줄러는 중단되지 않음
        refreshTokenCleanupTask.removeExpiredRefreshTokens();

        verify(refreshTokenRepository, times(1))
            .deleteByExpiresAtBefore(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("앱 시작할 때 전국 축제 데이터를 처음 불러오는 기능이 올바르게 작동하는지 테스트")
    void loadInitialNationalFestivalData_Success() {
        // given
        doNothing().when(festivalService).loadInitialNationalFestivalData();

        // when
        festivalScheduler.loadInitialNationalFestivalData();

        // then
        verify(festivalService, times(1)).loadInitialNationalFestivalData();
    }

    @Test
    @DisplayName("앱 시작할 때 서울 축제 데이터를 처음 불러오는 기능이 올바르게 작동하는지 테스트")
    void loadInitialSeoulFestivalData_Success() {
        // given
        doNothing().when(festivalService).loadInitialSeoulFestivalData();

        // when
        festivalScheduler.loadInitialSeoulFestivalData();

        // then
        verify(festivalService, times(1)).loadInitialSeoulFestivalData();
    }
}
