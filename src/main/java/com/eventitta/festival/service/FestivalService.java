package com.eventitta.festival.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FestivalService {

    private final SeoulFestivalInitializer seoulFestivalInitializer;
    private final NationalFestivalInitializer nationalFestivalInitializer;

    public void loadInitialNationalFestivalData() {
        nationalFestivalInitializer.loadInitialData();
    }

    public void loadInitialSeoulFestivalData() {
        seoulFestivalInitializer.loadInitialData();
    }
}
