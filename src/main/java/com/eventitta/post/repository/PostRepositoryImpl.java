package com.eventitta.post.repository;

import com.eventitta.post.domain.Post;
import com.eventitta.post.domain.QPost;
import com.eventitta.post.dto.PostFilter;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;
import java.util.Objects;

import static com.querydsl.core.types.dsl.Expressions.stringTemplate;

@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final QPost post = QPost.post;

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
            () -> Objects.requireNonNullElse(
                queryFactory
                    .select(post.count())
                    .from(post)
                    .where(predicate)
                    .fetchOne(),
                0L
            )
        );
    }

    private BooleanBuilder buildFilter(PostFilter filter) {
        BooleanBuilder b = new BooleanBuilder(post.deleted.isFalse());

        if (filter.keyword() != null) {
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
        return stringTemplate("lower(cast({0} as char))", clob).like("%" + kw + "%");
    }
}
