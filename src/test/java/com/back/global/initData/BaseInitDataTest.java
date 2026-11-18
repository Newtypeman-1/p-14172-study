package com.back.global.initData;

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BaseInitDataTest {
    @Autowired
    private PostService postService;

    @Test
    @DisplayName("앱이 시작되면 글 3건, 댓글 5건이 자동으로 생성된다.")
    void t1() {
        assertThat(postService.count()).isEqualTo(3);

        int totalComments = postService.findAll().stream()
                .mapToInt(post -> post.getComments().size())
                .sum();

        assertThat(totalComments).isEqualTo(5);
    }

    @Test
    @DisplayName("1번 글에는 댓글 3건, 2번 글에는 댓글 2건, 3번 글에는 댓글이 없다.")
    void t2() {
        Post post1 = postService.findById(1).get();
        Post post2 = postService.findById(2).get();
        Post post3 = postService.findById(3).get();

        assertThat(post1.getComments()).hasSize(3);
        assertThat(post2.getComments()).hasSize(2);
        assertThat(post3.getComments()).isEmpty();
    }
}
