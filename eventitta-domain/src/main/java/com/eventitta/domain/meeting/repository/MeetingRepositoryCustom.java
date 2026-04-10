package com.eventitta.domain.meeting.repository;

import com.eventitta.domain.meeting.dto.request.MeetingFilter;
import com.eventitta.domain.meeting.dto.response.MeetingSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MeetingRepositoryCustom {

    Page<MeetingSummaryResponse> findMeetingsByFilter(MeetingFilter filter, Pageable pageable);
}
