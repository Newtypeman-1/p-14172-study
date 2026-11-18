package com.back.domain.post.postComment.dto;

import com.back.domain.post.postComment.entity.PostComment;

import java.time.LocalDateTime;

// 댓글 응답 전용 DTO
// post 엔티티 전체 대신 postId 만 담는다.
public record PostCommentDto(
        long id,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        long postId,
        String content
) {
    public PostCommentDto(PostComment postComment) {
        this(
                postComment.getId(),
                postComment.getCreateDate(),
                postComment.getModifyDate(),
                postComment.getPost().getId(),
                postComment.getContent()
        );
    }
}
