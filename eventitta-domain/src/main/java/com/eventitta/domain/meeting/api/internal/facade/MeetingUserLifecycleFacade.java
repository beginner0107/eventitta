package com.eventitta.domain.meeting.api.internal.facade;

public interface MeetingUserLifecycleFacade {

    void removeUserFromActiveMeetings(Long userId);
}
