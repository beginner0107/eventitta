package com.eventitta.post.repository;

import com.eventitta.post.dto.PostFilter;
import com.eventitta.post.dto.SearchType;
import com.eventitta.post.dto.response.PostSummaryResponse;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

import static com.eventitta.post.domain.QPost.post;
import static com.eventitta.post.domain.QPostLike.postLike;
import static com.eventitta.region.domain.QRegion.region;
import static com.eventitta.user.domain.QUser.user;

@RequiredArgsConstructor
public class PostLikeRepositoryImpl implements PostLikeRepositoryCustom {

    private static final String WILDCARD_FORMAT = "%%%s%%";
    private static final String LOWER_CAST_TEMPLATE = "lower(cast({0} as string))";

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<PostSummaryResponse> findLikedSummaries(Long userId, PostFilter filter, Pageable pageable) {
        BooleanBuilder whereClause = buildWhereClause(userId, filter);

        List<PostSummaryResponse> content = fetchLikedPostSummaries(whereClause, pageable);
        Long totalCount = countLikedPosts(whereClause);

        return PageableExecutionUtils.getPage(content, pageable, () -> totalCount);
    }

    private BooleanBuilder buildWhereClause(Long userId, PostFilter filter) {
        BooleanBuilder predicate = new BooleanBuilder()
            .and(post.deleted.isFalse())
            .and(postLike.user.id.eq(userId));

        if (filter == null) {
            return predicate;
        }

        applySearchFilter(predicate, filter);
        applyRegionFilter(predicate, filter);

        return predicate;
    }

    private void applySearchFilter(BooleanBuilder predicate, PostFilter filter) {
        if (filter.searchType() == null || !StringUtils.hasText(filter.keyword())) {
            return;
        }

        String lowercaseKeyword = filter.keyword().toLowerCase();
        BooleanExpression searchCondition = createSearchCondition(filter.searchType(), lowercaseKeyword);
        predicate.and(searchCondition);
    }

    private BooleanExpression createSearchCondition(SearchType searchType, String keyword) {
        BooleanExpression titleCondition = createTitleCondition(keyword);
        BooleanExpression contentCondition = createContentCondition(keyword);

        return switch (searchType) {
            case TITLE -> titleCondition;
            case CONTENT -> contentCondition;
            case TITLE_CONTENT -> titleCondition.or(contentCondition);
        };
    }

    private BooleanExpression createTitleCondition(String keyword) {
        return post.title.lower().like(formatLikePattern(keyword));
    }

    private BooleanExpression createContentCondition(String keyword) {
        StringExpression loweredContent = Expressions.stringTemplate(
            LOWER_CAST_TEMPLATE,
            post.content
        );
        return loweredContent.like(formatLikePattern(keyword));
    }

    private String formatLikePattern(String keyword) {
        return String.format(WILDCARD_FORMAT, keyword);
    }

    private void applyRegionFilter(BooleanBuilder predicate, PostFilter filter) {
        if (filter.regionCode() != null) {
            predicate.and(post.region.code.eq(filter.regionCode()));
        }
    }

    private List<PostSummaryResponse> fetchLikedPostSummaries(BooleanBuilder whereClause, Pageable pageable) {
        return queryFactory
            .select(createPostSummaryProjection())
            .from(postLike)
            .join(postLike.post, post)
            .join(post.user, user)
            .join(post.region, region)
            .where(whereClause)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(post.createdAt.desc())
            .fetch();
    }

    private ConstructorExpression<PostSummaryResponse> createPostSummaryProjection() {
        return Projections.constructor(
            PostSummaryResponse.class,
            post.id,
            post.title,
            user.nickname,
            region.code,
            post.likeCount,
            post.createdAt
        );
    }

    private Long countLikedPosts(BooleanBuilder whereClause) {
        return Optional.ofNullable(
            queryFactory
                .select(post.count())
                .from(postLike)
                .join(postLike.post, post)
                .where(whereClause)
                .fetchOne()
        ).orElse(0L);
    }
}
