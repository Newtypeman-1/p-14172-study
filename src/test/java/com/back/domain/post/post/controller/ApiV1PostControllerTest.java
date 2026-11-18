package com.back.domain.post.post.controller;

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.service.PostService;
import jakarta.persistence.EntityManager;
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

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Autowired
    private EntityManager em;

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

    @Test
    @DisplayName("글 다건조회, 최신글부터 정렬")
    void t3() throws Exception {
        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts"))
                .andDo(print());

        // 기대값 : 모든 글을 id 내림차순(최신글 먼저)으로
        List<Post> posts = postService.findAll().stream()
                .sorted(Comparator.comparing(Post::getId).reversed())
                .toList();

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(posts.size())));

        for (int i = 0; i < posts.size(); i++) {
            Post post = posts.get(i);

            resultActions
                    .andExpect(jsonPath("$[%d].id".formatted(i)).value(post.getId()))
                    .andExpect(jsonPath("$[%d].createDate".formatted(i)).value(matchesPattern(DATE_TIME_PATTERN)))
                    .andExpect(jsonPath("$[%d].modifyDate".formatted(i)).value(matchesPattern(DATE_TIME_PATTERN)))
                    .andExpect(jsonPath("$[%d].title".formatted(i)).value(post.getTitle()))
                    .andExpect(jsonPath("$[%d].content".formatted(i)).value(post.getContent()));
        }
    }

    @Test
    @DisplayName("글 작성")
    void t4() throws Exception {
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

        Post post = postService.findAllByOrderByIdDesc().getFirst(); // 방금 작성된 글

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글이 작성되었습니다.".formatted(post.getId())))
                .andExpect(jsonPath("$.data.id").value(post.getId()))
                .andExpect(jsonPath("$.data.createDate").value(matchesPattern(DATE_TIME_PATTERN)))
                .andExpect(jsonPath("$.data.modifyDate").value(matchesPattern(DATE_TIME_PATTERN)))
                .andExpect(jsonPath("$.data.title").value("새 제목"))
                .andExpect(jsonPath("$.data.content").value("새 내용"));
    }

    @Test
    @DisplayName("글 작성, 제목 없이 요청하면 400")
    void t5() throws Exception {
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
    @DisplayName("글 수정")
    void t6() throws Exception {
        long id = 1;

        ResultActions resultActions = mvc
                .perform(
                        put("/api/v1/posts/%d".formatted(id))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "제목 수정",
                                            "content": "내용 수정"
                                        }
                                        """)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글이 수정되었습니다.".formatted(id)))
                .andExpect(jsonPath("$.data").doesNotExist());

        // 정말 수정되었는지 확인 (선택사항)
        Post post = postService.findById(id).get();
        assertThat(post.getTitle()).isEqualTo("제목 수정");
        assertThat(post.getContent()).isEqualTo("내용 수정");
    }

    @Test
    @DisplayName("글 삭제, 댓글이 달린 글도 삭제된다")
    void t7() throws Exception {
        long id = 1; // 1번 글에는 댓글이 3개 있다.

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

        // 테스트는 끝나면 롤백되므로, 그냥 두면 DELETE 쿼리가 DB 로 가지 않을 수도 있다.
        // flush 로 DELETE 를 실제로 실행시킨다. 댓글이 먼저 지워지지 않으면 여기서 FK 오류가 난다.
        em.flush();
        em.clear(); // 1차 캐시를 비워서 아래 조회가 DB 에서 직접 읽도록

        assertThat(postService.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("글 삭제, 존재하지 않는 글이면 404")
    void t8() throws Exception {
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
