package com.back.global.initData;

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.repository.PostRepository;
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
    private PostRepository postRepository;

    @Test
    @DisplayName("앱이 시작되면 글 3건, 댓글 5건이 자동으로 생성된다.")
    void t1() {
        assertThat(postRepository.count()).isEqualTo(3);

        int totalComments = postRepository.findAll().stream()
                .mapToInt(post -> post.getComments().size())
                .sum();

        assertThat(totalComments).isEqualTo(5);
    }

    @Test
    @DisplayName("1번 글에는 댓글 3건, 2번 글에는 댓글 2건이 있다.")
    void t2() {
        Post post1 = postRepository.findById(1L).get();
        Post post2 = postRepository.findById(2L).get();

        assertThat(post1.getComments()).hasSize(3);
        assertThat(post2.getComments()).hasSize(2);
    }
}
