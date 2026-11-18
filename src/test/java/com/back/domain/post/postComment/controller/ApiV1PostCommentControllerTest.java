package com.back.domain.post.postComment.controller;

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.service.PostService;
import com.back.domain.post.postComment.entity.PostComment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
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
class ApiV1PostCommentControllerTest {
    private static final String DATE_TIME_PATTERN = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PostService postService;

    // 테스트용 도우미 : postId 번 글의 댓글 중에서 id 번 댓글을 꺼낸다.
    private PostComment findComment(long postId, long id) {
        Post post = postService.findById(postId).get();

        return post.getComments().stream()
                .filter(comment -> comment.getId() == id)
                .findFirst()
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
}
