package com.eventitta.infra.comment.repository;

import com.eventitta.domain.comment.domain.QComment;
import com.eventitta.domain.comment.dto.projection.CommentFlatProjection;
import com.eventitta.domain.comment.repository.CommentRepositoryCustom;
import com.eventitta.domain.user.domain.QUser;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.querydsl.core.types.Projections.*;
import static com.querydsl.core.types.dsl.Expressions.cases;
import static com.querydsl.core.types.dsl.Expressions.nullExpression;

@RequiredArgsConstructor
public class JpaCommentRepositoryImpl implements CommentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CommentFlatProjection> findFlatByPost(Long postId) {
        QComment comment = QComment.comment;
        QUser commentUser = new QUser("commentUser");

        String deletedMessage = "[삭제된 댓글입니다]";
        return queryFactory
            .select(constructor(CommentFlatProjection.class,
                comment.id,
                cases()
                    .when(comment.deleted.isTrue()).then(deletedMessage)
                    .otherwise(comment.content),
                cases()
                    .when(comment.deleted.isTrue()).then(nullExpression(String.class))
                    .otherwise(commentUser.nickname),
                comment.deleted,
                comment.createdAt,
                comment.parent.id
            ))
            .from(comment)
            .leftJoin(commentUser).on(comment.userId.eq(commentUser.id))
            .where(comment.postId.eq(postId))
            .orderBy(comment.createdAt.asc())
            .fetch();
    }
}
