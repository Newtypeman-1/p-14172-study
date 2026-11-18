package com.back.domain.post.post.dto;

import com.back.domain.post.post.entity.Post;

import java.time.LocalDateTime;

// 엔티티를 그대로 응답하지 않고, 응답에 필요한 필드만 골라 담는다.
// - 엔티티 필드 이름을 바꿔도 API 응답이 깨지지 않는다.
// - comments 같은 양방향 관계 때문에 JSON 변환이 무한 반복되는 문제도 피할 수 있다.
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
