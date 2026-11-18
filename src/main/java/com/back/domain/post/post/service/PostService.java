package com.back.domain.post.post.service;

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.repository.PostRepository;
import com.back.domain.post.postComment.entity.PostComment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
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

    public void modify(Post post, String title, String content) {
        post.modify(title, content);
        // save 를 호출하지 않아도 된다.
        // 트랜잭션이 끝날 때 JPA 가 바뀐 필드를 감지(더티체킹)해서 UPDATE 를 실행한다.
    }

    public void delete(Post post) {
        postRepository.delete(post);
        // Post.comments 의 cascade = REMOVE 덕분에 댓글들이 먼저 삭제되고, 그 다음 글이 삭제된다.
    }

    public PostComment writeComment(Post post, String content) {
        return post.addComment(content); // cascade PERSIST 로 flush 시점에 INSERT 된다.
    }

    public void modifyComment(PostComment postComment, String content) {
        postComment.modify(content); // 더티체킹으로 트랜잭션이 끝날 때 UPDATE 된다.
    }

    public Optional<Post> findById(long id) {
        return postRepository.findById(id);
    }

    public List<Post> findAll() {
        return postRepository.findAll();
    }

    public List<Post> findAllByOrderByIdDesc() {
        return postRepository.findAllByOrderByIdDesc();
    }

    // 지금까지 모아둔 변경사항(INSERT/UPDATE/DELETE)을 DB 에 바로 보낸다.
    // 커밋은 아니다. 트랜잭션은 계속 유지되고, 나중에 롤백도 가능하다.
    public void flush() {
        postRepository.flush();
    }
}
