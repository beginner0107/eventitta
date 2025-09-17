package com.eventitta.comment.repository;

import com.eventitta.comment.domain.QComment;
import com.eventitta.comment.dto.query.CommentFlatDto;
import com.eventitta.user.domain.QUser;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.querydsl.core.types.Projections.*;
import static com.querydsl.core.types.dsl.Expressions.cases;
import static com.querydsl.core.types.dsl.Expressions.nullExpression;

@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CommentFlatDto> findFlatByPost(Long postId) {
        QComment comment = QComment.comment;
        QUser commentUser = new QUser("commentUser");

        String deletedMessage = "[삭제된 댓글입니다]";
        return queryFactory
            .select(constructor(CommentFlatDto.class,
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
            .join(comment.user, commentUser)
            .where(comment.post.id.eq(postId))
            .orderBy(comment.createdAt.asc())
            .fetch();
    }
}
