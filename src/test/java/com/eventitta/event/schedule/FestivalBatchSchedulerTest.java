package com.eventitta.event.schedule;

import com.eventitta.event.service.NationalFestivalImportService;
import com.eventitta.event.service.SeoulFestivalImportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FestivalBatchSchedulerTest {

    @Mock
    NationalFestivalImportService nationalService;
    @Mock
    SeoulFestivalImportService seoulService;

    @InjectMocks
    FestivalBatchScheduler scheduler;

    @Test
    @DisplayName("3개월마다 전국 축제 정보를 자동으로 업데이트한다")
    void runNationalQuarterlyImport_invokesService() {
        scheduler.runNationalQuarterlyImport();

        verify(nationalService).importAll();
    }

    @Test
    @DisplayName("매일 서울 축제 정보를 자동으로 업데이트한다")
    void runSeoulDailyImport_invokesService() {
        scheduler.runSeoulDailyImport();

        verify(seoulService).importCurrentMonth();
    }
}
