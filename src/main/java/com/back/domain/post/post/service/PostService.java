package com.back.domain.post.post.service;

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.repository.PostRepository;
import com.back.domain.post.postComment.entity.PostComment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;

    public long count() {
        return postRepository.count();
    }

    public Post write(String title, String content) {
        Post post = new Post(title, content);

        return postRepository.save(post);
    }

    public Optional<Post> findById(long id) {
        return postRepository.findById(id);
    }

    public void delete(Post post) {
        postRepository.delete(post); // 댓글은 cascade REMOVE 로 함께 삭제된다.
    }

    public PostComment writeComment(Post post, String content) {
        return post.addComment(content); // cascade PERSIST 로 저장된다.
    }

    public boolean deleteComment(Post post, PostComment postComment) {
        return post.deleteComment(postComment); // orphanRemoval 로 DB 에서도 삭제된다.
    }

    public Optional<Post> findLatest() {
        return postRepository.findFirstByOrderByIdDesc();
    }

    // 지금까지의 변경사항(INSERT/DELETE 등)을 DB 에 즉시 반영한다. (커밋은 아님)
    public void flush() {
        postRepository.flush();
    }
}
