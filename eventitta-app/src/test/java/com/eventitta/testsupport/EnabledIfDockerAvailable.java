package com.eventitta.testsupport;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Testcontainers(Docker)가 사용 가능한 환경에서만 테스트를 실행한다.
 * - 로컬에서 Docker Desktop 미실행/미설치 시: 테스트는 실패 대신 SKIPPED 처리
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DockerAvailableCondition.class)
public @interface EnabledIfDockerAvailable {
}
