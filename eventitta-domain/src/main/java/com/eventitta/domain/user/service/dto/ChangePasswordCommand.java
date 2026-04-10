package com.eventitta.domain.user.service.dto;

public record ChangePasswordCommand(
    String currentPassword,
    String newPassword
) {
}
