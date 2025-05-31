package com.eventitta.post.repository;

import com.eventitta.post.domain.Post;
import com.eventitta.post.dto.PostFilter;
import com.eventitta.post.dto.response.PostSummaryDto;
import com.querydsl.core.BooleanBuilder;
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
import static com.eventitta.region.domain.QRegion.region;
import static com.eventitta.user.domain.QUser.user;


@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Post> findAllByFilter(PostFilter filter, Pageable pageable) {
        BooleanBuilder predicate = buildFilter(filter);

        com.eventitta.user.domain.QUser postUser = new com.eventitta.user.domain.QUser("postUser");
        com.eventitta.region.domain.QRegion postRegion = new com.eventitta.region.domain.QRegion("postRegion");

        List<Post> content = queryFactory
            .selectFrom(post)
            .join(post.user, postUser).fetchJoin()
            .join(post.region, postRegion).fetchJoin()
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
    public Page<PostSummaryDto> findSummaries(PostFilter filter, Pageable pageable) {
        BooleanBuilder predicate = buildFilter(filter);
        com.eventitta.user.domain.QUser postUser = new com.eventitta.user.domain.QUser("postUser");

        List<PostSummaryDto> content = queryFactory
            .select(Projections.constructor(
                PostSummaryDto.class,
                post.id,
                post.title,
                post.user.nickname,
                post.region.code,
                post.likeCount,
                post.createdAt
            ))
            .from(post)
            .join(post.user, user)
            .join(post.region, region)
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
            b.and(post.region.code.eq(filter.regionCode()));
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
