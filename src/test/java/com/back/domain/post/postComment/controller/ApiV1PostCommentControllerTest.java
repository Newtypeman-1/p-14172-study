package com.back.domain.post.postComment.controller;

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.service.PostService;
import com.back.domain.post.postComment.entity.PostComment;
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
import static org.hamcrest.Matchers.greaterThan;
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
class ApiV1PostCommentControllerTest {
    private static final String DATE_TIME_PATTERN = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PostService postService;

    @Autowired
    private EntityManager em;

    // 테스트용 도우미 : postId 번 글의 댓글 중에서 id 번 댓글을 꺼낸다.
    private PostComment findComment(long postId, long id) {
        return postService.findById(postId).get()
                .findCommentById(id)
                .get();
    }

    @Test
    @DisplayName("댓글 단건조회")
    void t1() throws Exception {
        long postId = 1;
        long id = 1;

        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/%d/comments/%d".formatted(postId, id)))
                .andDo(print());

        PostComment postComment = findComment(postId, id);

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postComment.getId()))
                .andExpect(jsonPath("$.createDate").value(matchesPattern(DATE_TIME_PATTERN)))
                .andExpect(jsonPath("$.modifyDate").value(matchesPattern(DATE_TIME_PATTERN)))
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.content").value(postComment.getContent()));
    }

    @Test
    @DisplayName("댓글 단건조회, 존재하지 않는 댓글이면 404")
    void t2() throws Exception {
        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/1/comments/%d".formatted(Integer.MAX_VALUE)))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."));
    }

    @Test
    @DisplayName("댓글 단건조회, 다른 글의 댓글 번호로 요청하면 404")
    void t3() throws Exception {
        // 1번 댓글은 1번 글의 댓글이다. 2번 글의 경로로 요청하면 찾을 수 없어야 한다.
        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/2/comments/1"))
                .andDo(print());

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("댓글 다건조회, 해당 글의 댓글만 최신순으로")
    void t4() throws Exception {
        long postId = 1;

        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/%d/comments".formatted(postId)))
                .andDo(print());

        List<PostComment> postComments = postService.findById(postId).get()
                .getComments().stream()
                .sorted(Comparator.comparing(PostComment::getId).reversed())
                .toList();

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(postComments.size())));

        for (int i = 0; i < postComments.size(); i++) {
            PostComment postComment = postComments.get(i);

            resultActions
                    .andExpect(jsonPath("$[%d].id".formatted(i)).value(postComment.getId()))
                    .andExpect(jsonPath("$[%d].createDate".formatted(i)).value(matchesPattern(DATE_TIME_PATTERN)))
                    .andExpect(jsonPath("$[%d].modifyDate".formatted(i)).value(matchesPattern(DATE_TIME_PATTERN)))
                    .andExpect(jsonPath("$[%d].postId".formatted(i)).value(postId))
                    .andExpect(jsonPath("$[%d].content".formatted(i)).value(postComment.getContent()));
        }
    }

    @Test
    @DisplayName("댓글 작성")
    void t5() throws Exception {
        long postId = 1;

        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/posts/%d/comments".formatted(postId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "새 댓글"
                                        }
                                        """)
                )
                .andDo(print());

        PostComment postComment = postService.findById(postId).get().getComments().getLast(); // 방금 작성된 댓글

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("%d번 댓글이 작성되었습니다.".formatted(postComment.getId())))
                // 기대값(postComment.getId())도 0 이면 "0 == 0" 으로 통과해 버리므로, 0 보다 큰지 따로 확인한다.
                .andExpect(jsonPath("$.data.id").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.id").value(postComment.getId()))
                .andExpect(jsonPath("$.data.createDate").value(matchesPattern(DATE_TIME_PATTERN)))
                .andExpect(jsonPath("$.data.modifyDate").value(matchesPattern(DATE_TIME_PATTERN)))
                .andExpect(jsonPath("$.data.postId").value(postId))
                .andExpect(jsonPath("$.data.content").value("새 댓글"));
    }

    @Test
    @DisplayName("댓글 작성, 내용 없이 요청하면 400")
    void t6() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/posts/1/comments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": ""
                                        }
                                        """)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value("""
                        content-NotBlank-must not be blank
                        content-Size-size must be between 2 and 100
                        """.stripIndent().trim()));
    }

    @Test
    @DisplayName("댓글 수정")
    void t7() throws Exception {
        long postId = 1;
        long id = 1;

        ResultActions resultActions = mvc
                .perform(
                        put("/api/v1/posts/%d/comments/%d".formatted(postId, id))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "댓글 수정"
                                        }
                                        """)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 댓글이 수정되었습니다.".formatted(id)))
                .andExpect(jsonPath("$.data").doesNotExist());

        // 정말 수정되었는지 확인 (선택사항)
        assertThat(findComment(postId, id).getContent()).isEqualTo("댓글 수정");
    }

    @Test
    @DisplayName("댓글 삭제")
    void t8() throws Exception {
        long postId = 1;
        long id = 1;

        ResultActions resultActions = mvc
                .perform(delete("/api/v1/posts/%d/comments/%d".formatted(postId, id)))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 댓글이 삭제되었습니다.".formatted(id)))
                .andExpect(jsonPath("$.data").doesNotExist());

        // DELETE 쿼리를 실제로 실행시키고, 1차 캐시를 비운 뒤 DB 에서 다시 읽어서 확인 (선택사항)
        em.flush();
        em.clear();

        Post post = postService.findById(postId).get();
        assertThat(post.getComments()).hasSize(2);
        assertThat(post.getComments()).noneMatch(comment -> comment.getId() == id);
    }

    @Test
    @DisplayName("댓글 삭제, 다른 글의 댓글 번호로 요청하면 404, 댓글은 그대로")
    void t9() throws Exception {
        // 1번 댓글은 1번 글의 댓글이다. 2번 글의 경로로는 지울 수 없어야 한다.
        ResultActions resultActions = mvc
                .perform(delete("/api/v1/posts/2/comments/1"))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."));

        assertThat(findComment(1, 1)).isNotNull(); // 지워지지 않고 남아 있다.
    }
}
