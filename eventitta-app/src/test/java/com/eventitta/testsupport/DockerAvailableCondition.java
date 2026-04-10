package com.eventitta.testsupport;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

public class DockerAvailableCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED =
        ConditionEvaluationResult.enabled("Docker is available");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        try {
            boolean available = DockerClientFactory.instance().isDockerAvailable();
            if (available) {
                return ENABLED;
            }
            return ConditionEvaluationResult.disabled("Docker is not available (Testcontainers)");
        } catch (Throwable t) {
            return ConditionEvaluationResult.disabled("Docker availability check failed: " + t.getMessage());
        }
    }
}
