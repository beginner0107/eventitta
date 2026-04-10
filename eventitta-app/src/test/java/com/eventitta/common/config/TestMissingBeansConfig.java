package com.eventitta.infra.common.config;

import com.eventitta.domain.meeting.mapper.MeetingMapper;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestMissingBeansConfig {

    @Bean
    @Primary
    public MeetingMapper meetingMapper() {
        return Mockito.mock(MeetingMapper.class);
    }
}
