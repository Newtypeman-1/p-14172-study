package com.back.domain.post.post.controller;

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.matchesPattern;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional // 각 테스트가 끝나면 롤백 -> 테스트끼리 서로 영향을 주지 않는다.
class ApiV1PostControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private PostService postService;

    @Test
    @DisplayName("글 작성")
    void t1() throws Exception {
        // 1. 요청을 날린다.
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "새 제목",
                                            "content": "새 내용"
                                        }
                                        """)
                )
                .andDo(print());

        // 2. 결과를 검증한다.
        Post post = postService.findLatest().get();

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글이 작성되었습니다.".formatted(post.getId())))
                .andExpect(jsonPath("$.data.id").value(post.getId()))
                .andExpect(jsonPath("$.data.createDate").value(matchesPattern("^\\d{4}-\\d{2}-\\d{2}T.+")))
                .andExpect(jsonPath("$.data.modifyDate").value(matchesPattern("^\\d{4}-\\d{2}-\\d{2}T.+")))
                .andExpect(jsonPath("$.data.title").value("새 제목"))
                .andExpect(jsonPath("$.data.content").value("새 내용"));
    }

    @Test
    @DisplayName("글 작성, 제목 없이 요청하면 400")
    void t2() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "",
                                            "content": "새 내용"
                                        }
                                        """)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value("""
                        title-NotBlank-must not be blank
                        title-Size-size must be between 2 and 100
                        """.stripIndent().trim()));
    }

    @Test
    @DisplayName("글 작성, 잘못된 JSON 형식으로 요청하면 400")
    void t3() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "새 제목",
                                            "content": "새 내용"
                                        """)
                )
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-2"))
                .andExpect(jsonPath("$.msg").value("요청 본문이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("글 삭제")
    void t4() throws Exception {
        long id = 1;

        ResultActions resultActions = mvc
                .perform(delete("/api/v1/posts/%d".formatted(id)))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글이 삭제되었습니다.".formatted(id)))
                .andExpect(jsonPath("$.data").doesNotExist());

        // 정말 삭제되었는지 확인 (선택사항)
        // 테스트는 끝나면 롤백되므로 flush 로 DELETE 쿼리를 실제로 실행시켜 본다.
        // 1번 글에는 댓글이 있으므로, cascade 로 댓글이 먼저 지워지지 않으면 여기서 FK 오류가 난다.
        postService.flush();
        assertThat(postService.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("글 삭제, 존재하지 않는 글이면 404")
    void t5() throws Exception {
        long id = Integer.MAX_VALUE;

        ResultActions resultActions = mvc
                .perform(delete("/api/v1/posts/%d".formatted(id)))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."));
    }
}
