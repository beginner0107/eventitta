package com.eventitta.meeting.repository;

import com.eventitta.meeting.domain.MeetingStatus;
import com.eventitta.meeting.domain.QMeeting;
import com.eventitta.meeting.dto.request.MeetingFilter;
import com.eventitta.meeting.dto.response.MeetingSummaryResponse;
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

        // 1) 공통 where 절
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

        // 거리 필터 유무 체크
        boolean hasDistanceFilter = filter.distance() != null
            && filter.latitude() != null
            && filter.longitude() != null;

        // 2) content Projection 쿼리 분기 작성
        JPAQuery<MeetingSummaryResponse> contentQ;
        if (hasDistanceFilter) {
            NumberExpression<Double> distance = calculateDistance(
                filter.latitude(), filter.longitude(),
                m.latitude, m.longitude
            );
            // 거리 조건 추가
            where.and(m.latitude.isNotNull()
                .and(m.longitude.isNotNull())
                .and(distance.loe(filter.distance())));

            contentQ = qf.select(Projections.constructor(
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
                .orderBy(distance.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());
        } else {
            // 거리 필터 없을 때, 거리 필드는 항상 null
            contentQ = qf.select(Projections.constructor(
                    MeetingSummaryResponse.class,
                    m.id, m.title, m.description,
                    m.startTime, m.endTime,
                    m.maxMembers, m.currentMembers,
                    m.address, m.latitude, m.longitude,
                    m.status,
                    m.leader.id, m.leader.nickname,
                    Expressions.nullExpression(Double.class)
                ))
                .from(m)
                .join(m.leader, u)
                .where(where)
                .orderBy(m.startTime.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());
        }

        List<MeetingSummaryResponse> content = contentQ.fetch();

        // 3) count 쿼리
        JPAQuery<Long> countQ = qf.select(m.count())
            .from(m)
            .where(where);

        // 4) Page 생성
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
            "2 * ASIN(SQRT({0}))", a
        );

        return c.multiply(R);
    }
}
