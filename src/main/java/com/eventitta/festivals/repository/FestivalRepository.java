package com.eventitta.festivals.repository;

import com.eventitta.festivals.domain.Festival;
import com.eventitta.festivals.domain.DataSource;
import com.eventitta.festivals.dto.projection.FestivalProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface FestivalRepository extends JpaRepository<Festival, Long> {
    Optional<Festival> findByExternalIdAndDataSource(String externalId, DataSource dataSource);

    @Query(
        value = """
                SELECT
                    f.id            AS id,
                    f.title         AS title,
                    f.venue         AS place,
                    f.start_date    AS startTime,
                    f.end_date      AS endTime,
                    f.category      AS category,
                    f.is_free       AS isFree,
                    f.homepage_url  AS homepageUrl,
                    (
                      6371 * acos(
                        cos(radians(:latitude)) * cos(radians(f.latitude)) *
                        cos(radians(f.longitude) - radians(:longitude)) +
                        sin(radians(:latitude)) * sin(radians(f.latitude))
                      )
                    ) AS distance
                FROM festivals f
                WHERE
                  f.start_date >= DATE(:startDateTime)
                  AND (:endDateTime IS NULL OR f.start_date <= DATE(:endDateTime))
                HAVING distance <= :distanceKm
                ORDER BY distance ASC
            """,
        countQuery = """
                SELECT COUNT(*)
                FROM festivals f
                WHERE
                  f.start_date >= DATE(:startDateTime)
                  AND (:endDateTime IS NULL OR f.start_date <= DATE(:endDateTime))
                  AND (
                    6371 * acos(
                      cos(radians(:latitude)) * cos(radians(f.latitude)) *
                      cos(radians(f.longitude) - radians(:longitude)) +
                      sin(radians(:latitude)) * sin(radians(f.latitude))
                    )
                  ) <= :distanceKm
            """,
        nativeQuery = true
    )
    Page<FestivalProjection> findFestivalsWithinDistanceAndDateBetween(
        @Param("latitude") double latitude,
        @Param("longitude") double longitude,
        @Param("distanceKm") double distanceKm,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime,
        Pageable pageable
    );
}
