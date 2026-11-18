package com.back.global.initData;

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
public class BaseInitData {
    // this.work1() 처럼 같은 클래스 안에서 호출하면 @Transactional 이 적용되지 않는다.
    // 스프링이 만든 프록시(self)를 통해서 호출해야 트랜잭션이 걸린다.
    @Autowired
    @Lazy
    private BaseInitData self;

    private final PostService postService;

    @Bean
    public ApplicationRunner baseInitDataApplicationRunner() {
        return args -> self.work1();
    }

    @Transactional
    public void work1() {
        if (postService.count() > 0) return; // 이미 데이터가 있으면 다시 만들지 않는다.

        Post post1 = postService.write("제목 1", "내용 1");
        Post post2 = postService.write("제목 2", "내용 2");
        postService.write("제목 3", "내용 3");

        post1.addComment("댓글 1-1");
        post1.addComment("댓글 1-2");
        post1.addComment("댓글 1-3");

        post2.addComment("댓글 2-1");
        post2.addComment("댓글 2-2");
    }
}
