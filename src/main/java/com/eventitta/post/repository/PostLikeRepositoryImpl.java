package com.eventitta.post.repository;

import com.eventitta.post.dto.response.PostSummaryResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;
import java.util.Optional;

import static com.eventitta.post.domain.QPost.post;
import static com.eventitta.post.domain.QPostLike.postLike;
import static com.eventitta.region.domain.QRegion.region;
import static com.eventitta.user.domain.QUser.user;

@RequiredArgsConstructor
public class PostLikeRepositoryImpl implements PostLikeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<PostSummaryResponse> findLikedSummaries(Long userId, Pageable pageable) {
        List<PostSummaryResponse> content = queryFactory
            .select(Projections.constructor(
                PostSummaryResponse.class,
                post.id,
                post.title,
                user.nickname,
                region.code,
                post.likeCount,
                post.createdAt
            ))
            .from(postLike)
            .join(postLike.post, post)
            .join(post.user, user)
            .join(post.region, region)
            .where(
                postLike.user.id.eq(userId),
                post.deleted.isFalse()
            )
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(post.createdAt.desc())
            .fetch();

        return PageableExecutionUtils.getPage(content, pageable,
            () -> Optional.ofNullable(
                queryFactory.select(post.count())
                    .from(postLike)
                    .join(postLike.post, post)
                    .where(
                        postLike.user.id.eq(userId),
                        post.deleted.isFalse()
                    )
                    .fetchOne()
            ).orElse(0L)
        );
    }
}

