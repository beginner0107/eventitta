package com.eventitta.event.schedule;

import com.eventitta.event.service.NationalFestivalImportService;
import com.eventitta.event.service.SeoulFestivalImportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FestivalBackfillJobTest {

    @Mock
    NationalFestivalImportService nationalService;
    @Mock
    SeoulFestivalImportService seoulService;

    @InjectMocks
    FestivalBackfillJob job;

    @Test
    @DisplayName("애플리케이션 첫 실행 시 필요한 축제 데이터를 모두 수집해서 데이터베이스에 저장한다")
    void runBackfillOnce_invokesServices() {
        YearMonth endYm = YearMonth.now();
        YearMonth startYm = endYm.minusYears(1);
        int months = (int) ChronoUnit.MONTHS.between(startYm, endYm) + 1;

        job.runBackfillOnce();

        verify(nationalService).importAll();
        verify(seoulService, times(months)).importByYearMonth(anyString());
    }
}
