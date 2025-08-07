package com.eventitta.festivals.scheduler;

import com.eventitta.festivals.service.FestivalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("축제 자동 업데이트 스케줄러 테스트")
class FestivalSchedulerTest {

    @Mock
    private FestivalService festivalService;

    @InjectMocks
    private FestivalScheduler festivalScheduler;

    @Test
    @DisplayName("전국 축제 데이터 초기 설정 - 수동으로 전국 축제 데이터를 불러온다")
    void givenManualTrigger_whenLoadInitialNationalFestivalData_thenServiceIsExecuted() {
        // given
        // 수동 호출

        // when
        festivalScheduler.loadInitialNationalFestivalData();

        // then
        then(festivalService).should(times(1)).loadInitialNationalFestivalData();
    }

    @Test
    @DisplayName("서울시 축제 데이터 초기 설정 - 수동으로 서울시 축제 데이터를 불러온다")
    void givenManualTrigger_whenLoadInitialSeoulFestivalData_thenServiceIsExecuted() {
        // given
        // 수동 호출

        // when
        festivalScheduler.loadInitialSeoulFestivalData();

        // then
        then(festivalService).should(times(1)).loadInitialSeoulFestivalData();
    }

    @Test
    @DisplayName("전국 축제 데이터 정기 업데이트 - 정해진 시간에 자동으로 전국 축제 데이터를 업데이트한다")
    void givenScheduledTime_whenSyncNationalFestivalData_thenServiceIsExecuted() {
        // given
        // 스케줄러 시간 도달

        // when
        festivalScheduler.syncNationalFestivalData();

        // then
        then(festivalService).should(times(1)).loadInitialNationalFestivalData();
    }

    @Test
    @DisplayName("전국 축제 데이터 업데이트 오류 처리 - 업데이트 중 문제가 발생해도 시스템이 멈추지 않는다")
    void givenServiceException_whenSyncNationalFestivalData_thenExceptionIsHandled() {
        // given
        willThrow(new RuntimeException("데이터 로드 실패"))
            .given(festivalService).loadInitialNationalFestivalData();

        // when
        festivalScheduler.syncNationalFestivalData();

        // then
        then(festivalService).should(times(1)).loadInitialNationalFestivalData();
        // 예외가 발생해도 메서드가 정상 완료되어야 함
    }

    @Test
    @DisplayName("서울시 축제 데이터 일별 업데이트 - 매일 정해진 시간에 서울시 축제 데이터를 업데이트한다")
    void givenScheduledTime_whenSyncSeoulFestivalData_thenServiceIsExecuted() {
        // given
        // 스케줄러 시간 도달

        // when
        festivalScheduler.syncSeoulFestivalData();

        // then
        then(festivalService).should(times(1)).syncDailySeoulFestivalData();
    }

    @Test
    @DisplayName("서울시 축제 데이터 업데이트 오류 처리 - 업데이트 중 문제가 발생해도 시스템이 멈추지 않는다")
    void givenServiceException_whenSyncSeoulFestivalData_thenExceptionIsHandled() {
        // given
        willThrow(new RuntimeException("일별 동기화 실패"))
            .given(festivalService).syncDailySeoulFestivalData();

        // when
        festivalScheduler.syncSeoulFestivalData();

        // then
        then(festivalService).should(times(1)).syncDailySeoulFestivalData();
        // 예외가 발생해도 메서드가 정상 완료되어야 함
    }
}
