package com.back.domain.post.post.controller;

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ApiV1PostControllerTest {
    // 날짜 필드는 값을 정확히 맞추기 어려우니 "yyyy-MM-ddT..." 형식인지만 확인한다.
    private static final String DATE_TIME_PATTERN = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PostService postService;

    @Test
    @DisplayName("글 단건조회")
    void t1() throws Exception {
        long id = 1;

        // 1. 요청을 날린다.
        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/%d".formatted(id)))
                .andDo(print());

        // 2. 결과를 검증한다.
        Post post = postService.findById(id).get();

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(post.getId()))
                .andExpect(jsonPath("$.createDate").value(matchesPattern(DATE_TIME_PATTERN)))
                .andExpect(jsonPath("$.modifyDate").value(matchesPattern(DATE_TIME_PATTERN)))
                .andExpect(jsonPath("$.title").value(post.getTitle()))
                .andExpect(jsonPath("$.content").value(post.getContent()));
    }

    @Test
    @DisplayName("글 단건조회, 존재하지 않는 글이면 404")
    void t2() throws Exception {
        long id = Integer.MAX_VALUE;

        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/%d".formatted(id)))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."));
    }
}
