# 📝 p-14172-study

> 글과 댓글을 다루는 간단한 게시판 REST API 서버

![Java](https://img.shields.io/badge/Java-25-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-6DB33F?logo=springboot&logoColor=white)
![JPA](https://img.shields.io/badge/Spring%20Data%20JPA-H2-59666C?logo=hibernate&logoColor=white)
![Tests](https://img.shields.io/badge/tests-21%20passed-brightgreen)

Spring Boot로 글/댓글 CRUD API를 만들고, 모든 기능을 **TDD(테스트 먼저 → 구현)** 로 개발한 학습 프로젝트입니다.
커밋 메시지에 `001`, `002` … 번호를 달아 두어서, 개발 과정을 순서대로 따라가 볼 수 있습니다.

## ✨ 주요 기능

- **글** 목록 / 조회 / 작성 / 수정 / 삭제
- **댓글** 목록 / 조회 / 작성 / 수정 / 삭제
- 글을 삭제하면 그 글의 댓글도 함께 삭제
- 입력값 검증 (빈 제목, 길이 제한 등)
- 성공/실패 응답을 `resultCode` + `msg` 공통 형식으로 통일
- 앱 시작 시 샘플 데이터(글 3건, 댓글 5건) 자동 생성

## 🚀 빠른 시작

```bash
git clone https://github.com/Newtypeman-1/p-14172-study.git
cd p-14172-study
./gradlew bootRun
```

`http://localhost:8080/api/v1/posts` 를 열면 샘플 글 목록을 볼 수 있습니다.

```bash
./gradlew test   # 테스트 실행 (메모리 DB 사용)
```

> 🗄️ H2 콘솔: `http://localhost:8080/h2-console` · JDBC URL `jdbc:h2:./db_dev;MODE=MySQL` · 사용자 `sa` (비밀번호 없음)

## 📡 API 한눈에 보기

| 메서드 | URL | 설명 |
|------|------|------|
| `GET` | `/api/v1/posts` | 글 목록 (최신순) |
| `GET` | `/api/v1/posts/{id}` | 글 조회 |
| `POST` | `/api/v1/posts` | 글 작성 |
| `PUT` | `/api/v1/posts/{id}` | 글 수정 |
| `DELETE` | `/api/v1/posts/{id}` | 글 삭제 |
| `GET` | `/api/v1/posts/{postId}/comments` | 댓글 목록 (최신순) |
| `GET` | `/api/v1/posts/{postId}/comments/{id}` | 댓글 조회 |
| `POST` | `/api/v1/posts/{postId}/comments` | 댓글 작성 |
| `PUT` | `/api/v1/posts/{postId}/comments/{id}` | 댓글 수정 |
| `DELETE` | `/api/v1/posts/{postId}/comments/{id}` | 댓글 삭제 |

조회가 아닌 요청은 아래 형식으로 응답합니다.

```json
{
  "resultCode": "201-1",
  "msg": "4번 글이 작성되었습니다.",
  "data": { "id": 4, "title": "새 제목", "content": "새 내용", "...": "..." }
}
```

`resultCode` 앞자리는 HTTP 응답코드와 같습니다. (`200-1` 성공, `201-1` 작성됨, `400-1` 입력값 오류, `404-1` 데이터 없음)

## 🛠️ 기술 스택

| 분류 | 사용 기술 |
|------|------|
| Language | Java 25 |
| Framework | Spring Boot 4.1.2 (Web MVC, Validation, AOP) |
| Data | Spring Data JPA, H2 |
| Test | JUnit 5, MockMvc |
| Build | Gradle (Kotlin DSL) |
| Etc | Lombok |

## 📂 프로젝트 구조

```
src/main/java/com/back
├── domain/post
│   ├── post          # 글 (controller, dto, entity, repository, service)
│   └── postComment   # 댓글 (controller, dto, entity)
└── global            # 공통 응답(RsData), 예외 처리, AOP, 샘플 데이터, BaseEntity
```

## 🌿 브랜치

| 브랜치 | 내용 |
|------|------|
| `main` | 현재 버전 · 글/댓글 CRUD (TDD) |
| `v1` | 이전 버전 · 글/댓글 작성·삭제 |

---

📖 **어떻게 만들었는지 궁금하다면?** → [작업일지 (STORY.md)](STORY.md)
