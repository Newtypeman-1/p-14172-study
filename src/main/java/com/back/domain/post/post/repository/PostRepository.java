package com.back.domain.post.post.repository;

import com.back.domain.post.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    // 메서드 이름만으로 쿼리가 만들어진다 : SELECT * FROM post ORDER BY id DESC
    List<Post> findAllByOrderByIdDesc();
}
