package com.eventitta.user.service.dto;

public record ChangePasswordCommand(
    String currentPassword,
    String newPassword
) {
}
