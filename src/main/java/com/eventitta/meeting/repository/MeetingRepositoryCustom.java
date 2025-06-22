package com.eventitta.meeting.repository;

import com.eventitta.meeting.dto.request.MeetingFilter;
import com.eventitta.meeting.dto.response.MeetingSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MeetingRepositoryCustom {

    Page<MeetingSummaryResponse> findMeetingsByFilter(MeetingFilter filter, Pageable pageable);
}
