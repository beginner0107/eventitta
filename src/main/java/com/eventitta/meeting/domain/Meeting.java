package com.eventitta.meeting.domain;

import com.eventitta.meeting.dto.request.MeetingUpdateRequest;
import com.eventitta.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Meeting {

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

    private boolean deleted = false;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MeetingParticipant> participants = new ArrayList<>();

    @Builder
    public Meeting(String title, String description, LocalDateTime startTime, LocalDateTime endTime,
                   int maxMembers, String address, Double latitude, Double longitude,
                   MeetingStatus status, User leader) {
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxMembers = maxMembers;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.leader = leader;
    }

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
