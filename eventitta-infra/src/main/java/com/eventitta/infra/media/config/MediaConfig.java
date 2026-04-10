package com.eventitta.infra.media.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MediaProperties.class)
public class MediaConfig {
}
