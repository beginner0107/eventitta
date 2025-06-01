package com.eventitta.comment.dto.response;

import com.eventitta.comment.dto.query.CommentFlatDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "댓글 목록")
public record CommentWithChildrenDto(
    @Schema(description = "댓글 ID", example = "1")
    Long id,
    @Schema(description = "댓글 내용", example = "이 글 정말 좋아요!")
    String content,
    @Schema(description = "댓글 작성자 닉네임", example = "개구리왕눈이")
    String nickname,
    @Schema(description = "댓글 삭제여부", example = "false")
    boolean deleted,
    @Schema(description = "댓글 작성일", example = "2023-01-01T00:00:00")
    LocalDateTime createdAt,
    @Schema(description = "대댓글 목록")
    List<CommentChildDto> children
) {
    public static CommentWithChildrenDto from(CommentFlatDto parent, List<CommentFlatDto> childDtos) {
        List<CommentChildDto> children = childDtos == null ? List.of() :
            childDtos.stream()
                .map(child -> new CommentChildDto(
                    child.id(),
                    child.content(),
                    child.nickname(),
                    child.deleted(),
                    child.createdAt()
                ))
                .toList();

        return new CommentWithChildrenDto(
            parent.id(),
            parent.content(),
            parent.nickname(),
            parent.deleted(),
            parent.createdAt(),
            children
        );
    }
}
