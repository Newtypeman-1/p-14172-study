package com.back.domain.post.postComment.controller;

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.service.PostService;
import com.back.domain.post.postComment.dto.PostCommentDto;
import com.back.domain.post.postComment.entity.PostComment;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
@RequiredArgsConstructor
public class ApiV1PostCommentController {
    // 댓글은 항상 글을 통해서 다루므로 PostService 만 사용한다.
    private final PostService postService;

    @GetMapping
    @Transactional(readOnly = true)
    public List<PostCommentDto> getItems(@PathVariable long postId) {
        Post post = postService.findById(postId).get();

        return post.getComments()
                .stream()
                .sorted(Comparator.comparing(PostComment::getId).reversed()) // 최신 댓글 먼저
                .map(PostCommentDto::new)
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public PostCommentDto getItem(
            @PathVariable long postId,
            @PathVariable long id
    ) {
        Post post = postService.findById(postId).get();          // 글이 없으면 404
        PostComment postComment = post.findCommentById(id).get(); // 이 글의 댓글이 아니면 404

        return new PostCommentDto(postComment);
    }

    record PostCommentWriteReqBody(
            @NotBlank
            @Size(min = 2, max = 100)
            String content
    ) {
    }

    @PostMapping
    @Transactional
    public RsData<PostCommentDto> write(
            @PathVariable long postId,
            @Valid @RequestBody PostCommentWriteReqBody reqBody
    ) {
        Post post = postService.findById(postId).get();

        PostComment postComment = postService.writeComment(post, reqBody.content());

        // 응답을 만들기 전에 INSERT 를 먼저 실행시켜서 id, createDate, modifyDate 를 채운다.
        postService.flush();

        return new RsData<>(
                "201-1",
                "%d번 댓글이 작성되었습니다.".formatted(postComment.getId()),
                new PostCommentDto(postComment)
        );
    }

    record PostCommentModifyReqBody(
            @NotBlank
            @Size(min = 2, max = 100)
            String content
    ) {
    }

    @PutMapping("/{id}")
    @Transactional
    public RsData<Void> modify(
            @PathVariable long postId,
            @PathVariable long id,
            @Valid @RequestBody PostCommentModifyReqBody reqBody
    ) {
        Post post = postService.findById(postId).get();
        PostComment postComment = post.findCommentById(id).get();

        postService.modifyComment(postComment, reqBody.content());

        return new RsData<>(
                "200-1",
                "%d번 댓글이 수정되었습니다.".formatted(id)
        );
    }

    @DeleteMapping("/{id}")
    @Transactional
    public RsData<Void> delete(
            @PathVariable long postId,
            @PathVariable long id
    ) {
        Post post = postService.findById(postId).get();
        PostComment postComment = post.findCommentById(id).get(); // 이 글의 댓글이 아니면 404

        postService.deleteComment(post, postComment);

        return new RsData<>(
                "200-1",
                "%d번 댓글이 삭제되었습니다.".formatted(id)
        );
    }
}
