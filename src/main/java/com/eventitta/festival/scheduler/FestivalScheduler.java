package com.eventitta.festival.scheduler;

import com.eventitta.festival.service.FestivalService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FestivalScheduler {

    private final FestivalService festivalService;

    //    @PostConstruct
    public void loadInitialNationalFestivalData() {
        festivalService.loadInitialNationalFestivalData();
        ;
    }

    //    @PostConstruct
    public void loadInitialSeoulFestivalData() {
        festivalService.loadInitialSeoulFestivalData();
    }
}
