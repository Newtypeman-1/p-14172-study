package com.back.domain.post.post.dto;

import com.back.domain.post.post.entity.Post;

import java.time.LocalDateTime;

// 엔티티를 그대로 노출하지 않고, 응답에 필요한 필드만 담는다.
public record PostDto(
        long id,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        String title,
        String content
) {
    public PostDto(Post post) {
        this(
                post.getId(),
                post.getCreateDate(),
                post.getModifyDate(),
                post.getTitle(),
                post.getContent()
        );
    }
}
