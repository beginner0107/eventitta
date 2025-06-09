package com.eventitta.event.repository;

import com.eventitta.event.domain.Event;
import com.eventitta.event.dto.EventDistanceDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query(
        value = """
                SELECT
                    e.id              AS id,
                    e.title           AS title,
                    e.place           AS place,
                    e.start_time      AS startTime,
                    e.end_time        AS endTime,
                    e.category        AS category,
                    e.is_free         AS isFree,
                    e.homepage_url    AS homepageUrl,
                    (
                      6371 * acos(
                        cos(radians(:lat)) * cos(radians(e.latitude)) *
                        cos(radians(e.longitude) - radians(:lng)) +
                        sin(radians(:lat)) * sin(radians(e.latitude))
                      )
                    ) AS distance
                FROM events e
                HAVING distance <= :distanceKm
                ORDER BY distance ASC
            """,
        countQuery = """
                SELECT COUNT(*)
                FROM events e
                WHERE (
                  6371 * acos(
                    cos(radians(:lat)) * cos(radians(e.latitude)) *
                    cos(radians(e.longitude) - radians(:lng)) +
                    sin(radians(:lat)) * sin(radians(e.latitude))
                  )
                ) <= :distanceKm
            """,
        nativeQuery = true
    )
    Page<EventDistanceDto> findEventsWithinDistance(
        @Param("lat") double lat,
        @Param("lng") double lng,
        @Param("distanceKm") double distanceKm,
        Pageable pageable
    );
}
