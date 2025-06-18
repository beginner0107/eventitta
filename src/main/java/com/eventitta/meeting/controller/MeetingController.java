package com.eventitta.meeting.controller;

import com.eventitta.auth.annotation.CurrentUser;
import com.eventitta.meeting.dto.MeetingCreateRequest;
import com.eventitta.meeting.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    public ResponseEntity<Void> createMeeting(@CurrentUser Long userId,
                                              @RequestBody @Valid MeetingCreateRequest request) {
        Long meetingId = meetingService.createMeeting(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/meetings/" + meetingId)).build();
    }
}
