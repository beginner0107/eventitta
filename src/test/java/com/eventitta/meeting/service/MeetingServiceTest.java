package com.eventitta.meeting.service;

import com.eventitta.gamification.event.ActivityEventPublisher;
import com.eventitta.meeting.domain.Meeting;
import com.eventitta.meeting.domain.MeetingParticipant;
import com.eventitta.meeting.domain.MeetingStatus;
import com.eventitta.meeting.domain.ParticipantStatus;
import com.eventitta.meeting.dto.request.MeetingCreateRequest;
import com.eventitta.meeting.dto.request.MeetingUpdateRequest;
import com.eventitta.meeting.dto.response.ParticipantResponse;
import com.eventitta.meeting.exception.MeetingErrorCode;
import com.eventitta.meeting.exception.MeetingException;
import com.eventitta.meeting.mapper.MeetingMapper;
import com.eventitta.meeting.repository.MeetingParticipantRepository;
import com.eventitta.meeting.repository.MeetingRepository;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.eventitta.gamification.domain.ActivityType.JOIN_MEETING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    MeetingRepository meetingRepository;
    @Mock
    MeetingParticipantRepository participantRepository;
    @Mock
    MeetingMapper meetingMapper;
    @Mock
    UserRepository userRepository;
    @Mock
    ActivityEventPublisher activityEventPublisher;

    @InjectMocks
    MeetingService meetingService;


    private User createUser(Long id) {
        return User.builder()
            .id(id)
            .email("u" + id + "@test.com")
            .password("pw")
            .nickname("user" + id)
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build();
    }

    @Test
    @DisplayName("모임 생성 시 ID가 반환되고 리더가 참여자로 추가된다")
    void createMeeting_returnsIdAndAddsLeader() {
        // given
        Long leaderId = 1L;
        User leader = createUser(leaderId);
        given(userRepository.findById(leaderId)).willReturn(Optional.of(leader));

        MeetingCreateRequest req = new MeetingCreateRequest(
            "title", null,
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(2),
            10, null, null, null
        );
        Meeting meetingEntity = Meeting.builder()
            .title("title")
            .startTime(req.startTime())
            .endTime(req.endTime())
            .maxMembers(10)
            .status(MeetingStatus.RECRUITING)
            .leader(leader)
            .build();
        Meeting savedMeeting = Meeting.builder()
            .id(100L)
            .title("title")
            .startTime(req.startTime())
            .endTime(req.endTime())
            .maxMembers(10)
            .status(MeetingStatus.RECRUITING)
            .leader(leader)
            .build();

        given(meetingMapper.toEntity(any(MeetingCreateRequest.class), eq(leader)))
            .willReturn(meetingEntity);
        given(meetingRepository.save(any(Meeting.class))).willReturn(savedMeeting);

        // when
        Long id = meetingService.createMeeting(leaderId, req);

        // then
        assertThat(id).isEqualTo(100L);
        verify(participantRepository).save(any(MeetingParticipant.class));
    }

    @Test
    @DisplayName("모임 리더가 수정하면 정보가 변경된다")
    void updateMeeting_updatesFields() {
        // given
        Long leaderId = 1L;
        User leader = createUser(leaderId);
        Meeting meeting = Meeting.builder()
            .id(200L)
            .title("old")
            .description("old")
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(2))
            .maxMembers(10)
            .status(MeetingStatus.RECRUITING)
            .leader(leader)
            .build();

        given(userRepository.findById(leaderId)).willReturn(Optional.of(leader));
        given(meetingRepository.findById(anyLong())).willReturn(Optional.of(meeting));

        MeetingUpdateRequest req = new MeetingUpdateRequest(
            "new", "desc",
            LocalDateTime.now().plusDays(2),
            LocalDateTime.now().plusDays(3),
            20, null, null, null,
            MeetingStatus.RECRUITING
        );

        // when
        meetingService.updateMeeting(leaderId, meeting.getId(), req);

        // then
        assertThat(meeting.getTitle()).isEqualTo("new");
        assertThat(meeting.getMaxMembers()).isEqualTo(20);
    }

    @Test
    @DisplayName("모임 참가 신청 시 PENDING 상태의 참가자가 생성된다")
    void joinMeeting_returnsPendingParticipant() {
        // given
        Long userId = 2L;
        Long meetingId = 300L;
        User user = createUser(userId);
        User leader = createUser(1L);
        Meeting meeting = Meeting.builder()
            .id(meetingId)
            .title("title")
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(2))
            .maxMembers(10)
            .currentMembers(1)
            .status(MeetingStatus.RECRUITING)
            .leader(leader)
            .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(meetingRepository.findById(meetingId)).willReturn(Optional.of(meeting));
        given(participantRepository.findByMeetingIdAndUser_Id(meetingId, userId))
            .willReturn(Optional.empty());
        MeetingParticipant saved = MeetingParticipant.builder()
            .meeting(meeting)
            .user(user)
            .status(ParticipantStatus.PENDING)
            .build();
        org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", 1L);
        given(participantRepository.save(any(MeetingParticipant.class))).willReturn(saved);

        // when
        var resp = meetingService.joinMeeting(userId, meetingId);

        // then
        assertThat(resp.participantId()).isEqualTo(1L);
        assertThat(resp.status()).isEqualTo(ParticipantStatus.PENDING);
    }

    @Test
    @DisplayName("모임 리더가 참가자를 승인하면 상태가 변경된다")
    void approveParticipant_changesStatus() {
        // given
        Long leaderId = 1L;
        Long userId = 2L;
        Long meetingId = 400L;
        Long participantId = 10L;
        User leader = createUser(leaderId);
        User user = createUser(userId);
        Meeting meeting = Meeting.builder()
            .id(meetingId)
            .title("t")
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(2))
            .maxMembers(10)
            .currentMembers(1)
            .status(MeetingStatus.RECRUITING)
            .leader(leader)
            .build();
        MeetingParticipant participant = MeetingParticipant.builder()
            .meeting(meeting)
            .user(user)
            .status(ParticipantStatus.PENDING)
            .build();
        org.springframework.test.util.ReflectionTestUtils.setField(participant, "id", participantId);

        given(userRepository.findById(leaderId)).willReturn(Optional.of(leader));
        given(meetingRepository.findById(meetingId)).willReturn(Optional.of(meeting));
        given(participantRepository.findByIdWithMeeting(participantId)).willReturn(Optional.of(participant));
        given(meetingMapper.toParticipantResponse(any(), any())).willReturn(
            new ParticipantResponse(participantId, userId, "nick", null, ParticipantStatus.APPROVED)
        );

        // when
        ParticipantResponse response = meetingService.approveParticipant(leaderId, meetingId, participantId);

        // then
        assertThat(participant.getStatus()).isEqualTo(ParticipantStatus.APPROVED);
        assertThat(meeting.getCurrentMembers()).isEqualTo(2);

        // 이벤트 발행 검증
        verify(activityEventPublisher).publish(JOIN_MEETING, userId, meetingId);
    }

    @Test
    @DisplayName("모임 리더가 참가자를 거절하면 상태가 변경된다")
    void rejectParticipant_changesStatus() {
        // given
        Long leaderId = 1L;
        Long userId = 2L;
        Long meetingId = 500L;
        Long participantId = 11L;
        User leader = createUser(leaderId);
        User user = createUser(userId);
        Meeting meeting = Meeting.builder()
            .id(meetingId)
            .title("t")
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(2))
            .maxMembers(10)
            .status(MeetingStatus.RECRUITING)
            .leader(leader)
            .build();
        MeetingParticipant participant = MeetingParticipant.builder()
            .meeting(meeting)
            .user(user)
            .status(ParticipantStatus.PENDING)
            .build();
        org.springframework.test.util.ReflectionTestUtils.setField(participant, "id", participantId);

        given(userRepository.findById(leaderId)).willReturn(Optional.of(leader));
        given(meetingRepository.findById(meetingId)).willReturn(Optional.of(meeting));
        given(participantRepository.findByIdWithMeeting(participantId)).willReturn(Optional.of(participant));

        // when
        meetingService.rejectParticipant(leaderId, meetingId, participantId);

        // then
        assertThat(participant.getStatus()).isEqualTo(ParticipantStatus.REJECTED);
    }

    @Test
    @DisplayName("cancelJoin 호출 시 APPROVED 상태 참여자 수가 감소한다")
    void cancelJoin_approvedParticipant_decrementsCount() {
        // given
        Long userId = 3L;
        Long meetingId = 600L;
        User leader = createUser(1L);
        User user = createUser(userId);
        Meeting meeting = Meeting.builder()
            .id(meetingId)
            .title("t")
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(2))
            .maxMembers(10)
            .currentMembers(2)
            .status(MeetingStatus.RECRUITING)
            .leader(leader)
            .build();
        MeetingParticipant participant = MeetingParticipant.builder()
            .meeting(meeting)
            .user(user)
            .status(ParticipantStatus.APPROVED)
            .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(meetingRepository.findById(meetingId)).willReturn(Optional.of(meeting));
        given(participantRepository.findByMeetingIdAndUser_Id(meetingId, userId))
            .willReturn(Optional.of(participant));

        // when
        meetingService.cancelJoin(userId, meetingId);

        // then
        assertThat(meeting.getCurrentMembers()).isEqualTo(1);
        verify(participantRepository).delete(participant);

        // 활동 취소 이벤트는 현재 구현하지 않았으므로 검증하지 않음
        // 필요시 별도 이벤트 타입으로 구현 가능
    }

    @Test
    @DisplayName("현재 인원이 가득 찼을 때 approveParticipant는 예외를 던진다")
    void approveParticipant_whenFull_throwsException() {
        // given
        Long leaderId = 1L;
        Long userId = 4L;
        Long meetingId = 700L;
        Long participantId = 20L;
        User leader = createUser(leaderId);
        User user = createUser(userId);
        Meeting meeting = Meeting.builder()
            .id(meetingId)
            .title("full")
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(2))
            .maxMembers(2)
            .currentMembers(2)
            .status(MeetingStatus.RECRUITING)
            .leader(leader)
            .build();
        MeetingParticipant participant = MeetingParticipant.builder()
            .meeting(meeting)
            .user(user)
            .status(ParticipantStatus.PENDING)
            .build();
        org.springframework.test.util.ReflectionTestUtils.setField(participant, "id", participantId);

        given(userRepository.findById(leaderId)).willReturn(Optional.of(leader));
        given(meetingRepository.findById(meetingId)).willReturn(Optional.of(meeting));
        given(participantRepository.findByIdWithMeeting(participantId)).willReturn(Optional.of(participant));

        // when & then
        assertThatThrownBy(() -> meetingService.approveParticipant(leaderId, meetingId, participantId))
            .isInstanceOf(MeetingException.class)
            .extracting("errorCode")
            .isEqualTo(MeetingErrorCode.MEETING_MAX_MEMBERS_REACHED);
    }

    @Test
    @DisplayName("이미 참가한 모임에 다시 참여하려 하면 예외가 발생한다")
    void joinMeeting_whenAlreadyJoined_throwsException() {
        // given
        Long userId = 5L;
        Long meetingId = 800L;
        User user = createUser(userId);
        User leader = createUser(1L);
        Meeting meeting = Meeting.builder()
            .id(meetingId)
            .title("t")
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(2))
            .maxMembers(10)
            .currentMembers(1)
            .status(MeetingStatus.RECRUITING)
            .leader(leader)
            .build();
        MeetingParticipant participant = MeetingParticipant.builder()
            .meeting(meeting)
            .user(user)
            .status(ParticipantStatus.PENDING)
            .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(meetingRepository.findById(meetingId)).willReturn(Optional.of(meeting));
        given(participantRepository.findByMeetingIdAndUser_Id(meetingId, userId))
            .willReturn(Optional.of(participant));

        // when & then
        assertThatThrownBy(() -> meetingService.joinMeeting(userId, meetingId))
            .isInstanceOf(MeetingException.class)
            .extracting("errorCode")
            .isEqualTo(MeetingErrorCode.ALREADY_JOINED_MEETING);
    }

    @Test
    @DisplayName("모집 중이 아닌 모임 참가 신청 시 예외가 발생한다")
    void joinMeeting_whenNotRecruiting_throwsException() {
        // given
        Long userId = 6L;
        Long meetingId = 801L;
        User user = createUser(userId);
        User leader = createUser(1L);
        Meeting meeting = Meeting.builder()
            .id(meetingId)
            .title("t")
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(2))
            .maxMembers(10)
            .currentMembers(1)
            .status(MeetingStatus.CLOSED)
            .leader(leader)
            .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(meetingRepository.findById(meetingId)).willReturn(Optional.of(meeting));

        // when & then
        assertThatThrownBy(() -> meetingService.joinMeeting(userId, meetingId))
            .isInstanceOf(MeetingException.class)
            .extracting("errorCode")
            .isEqualTo(MeetingErrorCode.MEETING_NOT_RECRUITING);
    }

    @Test
    @DisplayName("삭제된 모임에 대한 수정 요청 시 예외가 발생한다")
    void updateMeeting_onDeletedMeeting_throwsException() {
        // given
        Long leaderId = 1L;
        User leader = createUser(leaderId);
        Meeting meeting = Meeting.builder()
            .id(900L)
            .title("t")
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(2))
            .maxMembers(10)
            .status(MeetingStatus.RECRUITING)
            .leader(leader)
            .build();
        meeting.delete();

        given(userRepository.findById(leaderId)).willReturn(Optional.of(leader));
        given(meetingRepository.findById(meeting.getId())).willReturn(Optional.of(meeting));

        MeetingUpdateRequest req = new MeetingUpdateRequest(
            "n", null,
            LocalDateTime.now().plusDays(2),
            LocalDateTime.now().plusDays(3),
            10, null, null, null,
            MeetingStatus.RECRUITING
        );

        // when & then
        assertThatThrownBy(() -> meetingService.updateMeeting(leaderId, meeting.getId(), req))
            .isInstanceOf(MeetingException.class)
            .extracting("errorCode")
            .isEqualTo(MeetingErrorCode.ALREADY_DELETED_MEETING);
    }

    @Test
    @DisplayName("삭제된 모임 삭제 시 예외가 발생한다")
    void deleteMeeting_onDeletedMeeting_throwsException() {
        // given
        Long leaderId = 1L;
        User leader = createUser(leaderId);
        Meeting meeting = Meeting.builder()
            .id(901L)
            .title("t")
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(2))
            .maxMembers(10)
            .status(MeetingStatus.RECRUITING)
            .leader(leader)
            .build();
        meeting.delete();

        given(userRepository.findById(leaderId)).willReturn(Optional.of(leader));
        given(meetingRepository.findById(meeting.getId())).willReturn(Optional.of(meeting));

        // when & then
        assertThatThrownBy(() -> meetingService.deleteMeeting(leaderId, meeting.getId()))
            .isInstanceOf(MeetingException.class)
            .extracting("errorCode")
            .isEqualTo(MeetingErrorCode.ALREADY_DELETED_MEETING);
    }

    @Test
    @DisplayName("삭제된 모임 참가 신청 시 예외가 발생한다")
    void joinMeeting_onDeletedMeeting_throwsException() {
        // given
        Long userId = 7L;
        Long meetingId = 902L;
        User user = createUser(userId);
        User leader = createUser(1L);
        Meeting meeting = Meeting.builder()
            .id(meetingId)
            .title("t")
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(2))
            .maxMembers(10)
            .currentMembers(1)
            .status(MeetingStatus.RECRUITING)
            .leader(leader)
            .build();
        meeting.delete();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(meetingRepository.findById(meetingId)).willReturn(Optional.of(meeting));

        // when & then
        assertThatThrownBy(() -> meetingService.joinMeeting(userId, meetingId))
            .isInstanceOf(MeetingException.class)
            .extracting("errorCode")
            .isEqualTo(MeetingErrorCode.ALREADY_DELETED_MEETING);
    }

    @Test
    @DisplayName("승인된 참가자가 취소될 때 활동 취소 이벤트가 발행된다")
    void cancelJoin_approvedParticipant_publishesRevoke() {
        // given
        Long userId = 8L;
        Long meetingId = 903L;
        User leader = createUser(1L);
        User user = createUser(userId);
        Meeting meeting = Meeting.builder()
            .id(meetingId)
            .title("t")
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(2))
            .maxMembers(10)
            .currentMembers(2)
            .status(MeetingStatus.RECRUITING)
            .leader(leader)
            .build();
        MeetingParticipant participant = MeetingParticipant.builder()
            .meeting(meeting)
            .user(user)
            .status(ParticipantStatus.APPROVED)
            .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(meetingRepository.findById(meetingId)).willReturn(Optional.of(meeting));
        given(participantRepository.findByMeetingIdAndUser_Id(meetingId, userId))
            .willReturn(Optional.of(participant));

        // when
        meetingService.cancelJoin(userId, meetingId);

        // then
        verify(activityEventPublisher).publishRevoke(JOIN_MEETING, userId, meetingId);
    }

    @Test
    @DisplayName("거절된 참가자가 취소를 시도하면 예외가 발생한다")
    void cancelJoin_rejectedParticipant_throwsException() {
        // given
        Long userId = 9L;
        Long meetingId = 904L;
        User leader = createUser(1L);
        User user = createUser(userId);
        Meeting meeting = Meeting.builder()
            .id(meetingId)
            .title("t")
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(2))
            .maxMembers(10)
            .status(MeetingStatus.RECRUITING)
            .leader(leader)
            .build();
        MeetingParticipant participant = MeetingParticipant.builder()
            .meeting(meeting)
            .user(user)
            .status(ParticipantStatus.REJECTED)
            .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(meetingRepository.findById(meetingId)).willReturn(Optional.of(meeting));
        given(participantRepository.findByMeetingIdAndUser_Id(meetingId, userId))
            .willReturn(Optional.of(participant));

        // when & then
        assertThatThrownBy(() -> meetingService.cancelJoin(userId, meetingId))
            .isInstanceOf(MeetingException.class)
            .extracting("errorCode")
            .isEqualTo(MeetingErrorCode.INVALID_PARTICIPANT_STATUS);
    }
}
