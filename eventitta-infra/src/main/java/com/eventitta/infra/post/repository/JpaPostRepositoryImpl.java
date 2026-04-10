package com.eventitta.infra.post.repository;

import com.eventitta.domain.post.domain.Post;
import com.eventitta.domain.post.dto.PostFilter;
import com.eventitta.domain.post.dto.response.PostSummaryResponse;
import com.eventitta.domain.post.domain.QPostLike;
import com.eventitta.domain.post.repository.PostRepositoryCustom;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Expression;
import com.eventitta.domain.region.domain.QRegion;
import com.eventitta.domain.user.domain.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

import static com.eventitta.domain.post.domain.QPost.post;
import static com.eventitta.domain.region.domain.QRegion.region;
import static com.eventitta.domain.user.domain.QUser.user;


@RequiredArgsConstructor
public class JpaPostRepositoryImpl implements PostRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Post> findAllByFilter(PostFilter filter, Pageable pageable) {
        BooleanBuilder predicate = buildFilter(filter);

        List<Post> content = queryFactory
            .selectFrom(post)
            .where(predicate)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(post.createdAt.desc())
            .fetch();

        return PageableExecutionUtils.getPage(content, pageable,
            () -> Optional.ofNullable(
                queryFactory.select(post.count())
                    .from(post)
                    .where(predicate)
                    .fetchOne()
            ).orElse(0L)
        );
    }

    @Override
    public Page<PostSummaryResponse> findSummaries(PostFilter filter, Pageable pageable, Long currentUserId) {
        BooleanBuilder predicate = buildFilter(filter);

        List<PostSummaryResponse> content = queryFactory
            .select(createPostSummaryProjection(currentUserId))
            .from(post)
            .leftJoin(user).on(post.authorUserId.eq(user.id))
            .leftJoin(region).on(post.regionCode.eq(region.code))
            .where(predicate)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(post.createdAt.desc())
            .fetch();

        return PageableExecutionUtils.getPage(content, pageable,
            () -> Optional.ofNullable(
                queryFactory.select(post.count())
                    .from(post)
                    .where(predicate)
                    .fetchOne()
            ).orElse(0L)
        );
    }

    private ConstructorExpression<PostSummaryResponse> createPostSummaryProjection(Long currentUserId) {
        QPostLike likeCountPostLike = new QPostLike("likeCountPostLike");
        QPostLike likedByUserPostLike = new QPostLike("likedByUserPostLike");

        return Projections.constructor(
            PostSummaryResponse.class,
            post.id,
            post.title,
            user.nickname,
            post.regionCode,
            JPAExpressions.select(likeCountPostLike.id.count().intValue())
                .from(likeCountPostLike)
                .where(likeCountPostLike.post.id.eq(post.id)),
            likedByMeExpression(currentUserId, likedByUserPostLike),
            post.createdAt
        );
    }

    private Expression<Boolean> likedByMeExpression(Long currentUserId, QPostLike likedByUserPostLike) {
        if (currentUserId == null) {
            return Expressions.constant(false);
        }

        return JPAExpressions.selectOne()
            .from(likedByUserPostLike)
            .where(
                likedByUserPostLike.post.id.eq(post.id)
                    .and(likedByUserPostLike.userId.eq(currentUserId))
            )
            .exists();
    }

    private BooleanBuilder buildFilter(PostFilter filter) {
        BooleanBuilder b = new BooleanBuilder(post.deleted.isFalse());

        if (filter.searchType() != null
            && StringUtils.hasText(filter.keyword())) {

            String kw = filter.keyword().toLowerCase();
            BooleanExpression titleCond = post.title.lower().like("%" + kw + "%");
            BooleanExpression contentCond = lowerClobContains(post.content, kw);

            switch (filter.searchType()) {
                case TITLE -> b.and(titleCond);
                case CONTENT -> b.and(contentCond);
                case TITLE_CONTENT -> b.and(titleCond.or(contentCond));
            }
        }
        if (filter.regionCode() != null) {
            b.and(post.regionCode.eq(filter.regionCode()));
        }
        return b;
    }

    private BooleanExpression lowerClobContains(Path<String> clob, String kw) {
        StringExpression lowered = Expressions.stringTemplate(
            "lower(cast({0} as string))", clob
        );
        return lowered.like("%" + kw + "%");
    }
}
