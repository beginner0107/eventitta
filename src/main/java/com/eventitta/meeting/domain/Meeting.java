package com.eventitta.meeting.domain;

import com.eventitta.common.domain.BaseEntity;
import com.eventitta.meeting.dto.request.MeetingUpdateRequest;
import com.eventitta.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "meetings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Meeting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int maxMembers;

    @Builder.Default
    private int currentMembers = 1;

    private String address;
    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    private MeetingStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id")
    private User leader;

    @Builder.Default
    private boolean deleted = false;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MeetingParticipant> participants = new ArrayList<>();

    public boolean isLeader(Long userId) {
        return leader != null && Objects.equals(leader.getId(), userId);
    }

    public void update(MeetingUpdateRequest req) {
        this.title = req.title();
        this.description = req.description();
        this.startTime = req.startTime();
        this.endTime = req.endTime();
        this.maxMembers = req.maxMembers();
        this.address = req.address();
        this.latitude = req.latitude();
        this.longitude = req.longitude();
        this.status = req.status();
    }

    public void delete() {
        this.deleted = true;
    }

    public void incrementCurrentMembers() {
        this.currentMembers++;
    }

    public void decrementCurrentMembers() {
        if (this.currentMembers > 0) {
            this.currentMembers--;
        }
    }
}
