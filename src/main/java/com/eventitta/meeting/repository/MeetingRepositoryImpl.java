package com.eventitta.meeting.repository;

import com.eventitta.meeting.domain.MeetingStatus;
import com.eventitta.meeting.domain.QMeeting;
import com.eventitta.meeting.dto.MeetingFilter;
import com.eventitta.meeting.dto.MeetingSummaryResponse;
import com.eventitta.user.domain.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.List;

@Repository
public class MeetingRepositoryImpl implements MeetingRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<MeetingSummaryResponse> findMeetingsByFilter(MeetingFilter filter, Pageable pageable) {
        JPAQueryFactory qf = new JPAQueryFactory(em);
        QMeeting m = QMeeting.meeting;
        QUser u = QUser.user;

        // 1) 공통 where
        BooleanBuilder where = new BooleanBuilder(m.deleted.eq(false));
        if (StringUtils.hasText(filter.keyword())) {
            where.and(m.title.containsIgnoreCase(filter.keyword())
                .or(m.description.containsIgnoreCase(filter.keyword())));
        }
        if (StringUtils.hasText(filter.region())) {
            where.and(m.address.containsIgnoreCase(filter.region()));
        }
        if (filter.startDateFrom() != null) {
            where.and(m.startTime.goe(filter.startDateFrom().atStartOfDay()));
        }
        if (filter.startDateTo() != null) {
            where.and(m.startTime.loe(filter.startDateTo().atTime(LocalTime.MAX)));
        }
        if (filter.status() != null) {
            where.and(m.status.eq(filter.status()));
        } else {
            where.and(m.status.eq(MeetingStatus.RECRUITING));
        }

        // 2) 거리식 정의
        NumberExpression<Double> distance = null;
        if (filter.distance() != null
            && filter.latitude() != null
            && filter.longitude() != null) {

            distance = calculateDistance(
                filter.latitude(), filter.longitude(),
                m.latitude, m.longitude
            );
            where.and(m.latitude.isNotNull()
                .and(m.longitude.isNotNull())
                .and(distance.loe(filter.distance())));
        }

        // 3) content 쿼리
        JPAQuery<MeetingSummaryResponse> contentQ = qf
            .select(Projections.constructor(
                MeetingSummaryResponse.class,
                m.id, m.title, m.description,
                m.startTime, m.endTime,
                m.maxMembers, m.currentMembers,
                m.address, m.latitude, m.longitude,
                m.status,
                m.leader.id, m.leader.nickname,
                distance
            ))
            .from(m)
            .join(m.leader, u)
            .where(where)
            .orderBy(distance != null ? distance.asc() : m.startTime.asc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize());

        List<MeetingSummaryResponse> content = contentQ.fetch();

        // 4) count 쿼리
        JPAQuery<Long> countQ = qf
            .select(m.count())
            .from(m)
            .where(where);

        // 5) Page 생성
        return PageableExecutionUtils.getPage(content, pageable, countQ::fetchOne);
    }

    private NumberExpression<Double> calculateDistance(Double lat1, Double lon1,
                                                       NumberExpression<Double> lat2,
                                                       NumberExpression<Double> lon2) {
        final double R = 6371.0;
        double radLat1 = Math.toRadians(lat1);
        NumberExpression<Double> radLat2 = lat2.multiply(Math.PI / 180.0);
        NumberExpression<Double> dLat = lat2.subtract(lat1).multiply(Math.PI / 180.0);
        NumberExpression<Double> dLon = lon2.subtract(lon1).multiply(Math.PI / 180.0);

        NumberExpression<Double> a = Expressions.numberTemplate(Double.class,
            "SIN({0} / 2) * SIN({0} / 2) + COS({1}) * COS({2}) * SIN({3} / 2) * SIN({3} / 2)",
            dLat, Expressions.constant(radLat1), radLat2, dLon
        );
        NumberExpression<Double> c = Expressions.numberTemplate(Double.class,
            "2 * ASIN(SQRT({0}))", a);

        return c.multiply(R);
    }
}
