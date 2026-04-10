package com.eventitta.domain.gamification.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserActivityStatsId implements Serializable {

    private Long userId;
    private RewardActionType actionType;
}
