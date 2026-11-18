package com.back.domain.post.post.entity;

import com.back.domain.post.postComment.entity.PostComment;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;

@Entity
@Getter
@NoArgsConstructor
public class Post extends BaseEntity {
    private String title;
    private String content;

    // mappedBy = "post" : 관계의 주인은 PostComment.post (FK 는 post_comment 테이블에 있다)
    // PERSIST : 글을 저장/flush 할 때 리스트에 새로 들어온 댓글도 저장
    // REMOVE : 글을 삭제하면 댓글도 함께 삭제
    // orphanRemoval : 리스트에서 빠진 댓글은 DB 에서도 삭제
    @OneToMany(mappedBy = "post", cascade = {PERSIST, REMOVE}, orphanRemoval = true)
    private List<PostComment> comments = new ArrayList<>();

    public Post(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public PostComment addComment(String content) {
        PostComment postComment = new PostComment(this, content);
        comments.add(postComment); // save 를 직접 하지 않아도 cascade PERSIST 로 저장된다.

        return postComment;
    }
}
