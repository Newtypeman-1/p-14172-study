package com.back.global.jpa.entity;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;

// 모든 엔티티가 공통으로 가지는 필드 : id, 생성일, 수정일
// @MappedSuperclass : 이 클래스 자체는 테이블이 아니고, 자식 엔티티 테이블에 칼럼만 물려준다.
@MappedSuperclass
@Getter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY) // DB 의 AUTO_INCREMENT 로 id 생성
    private long id;

    @CreatedDate // 처음 저장될 때 자동 기록
    private LocalDateTime createDate;

    @LastModifiedDate // 수정될 때마다 자동 기록
    private LocalDateTime modifyDate;
}
