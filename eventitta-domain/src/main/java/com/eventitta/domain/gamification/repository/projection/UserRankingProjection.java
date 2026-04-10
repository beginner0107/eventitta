package com.eventitta.domain.gamification.repository.projection;

public interface UserRankingProjection {

    Long getUserId();

    String getNickname();

    String getProfilePictureUrl();

    int getScore();
}
