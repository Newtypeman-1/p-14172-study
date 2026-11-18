# [Spring Boot] 글/댓글 REST API를 테스트와 함께 만들어보기 (작성 · 삭제)

> 📅 2026-11-17
> 🏷️ `Spring Boot 4` `JPA` `REST API` `MockMvc` `TDD`

강의 노트([slog.gg/p/14172](https://www.slog.gg/p/14172))를 보면서 글/댓글 API 중 **작성**과 **삭제** 기능을 테스트케이스와 함께 구현했다.
이번 글은 그 과정을 커밋 단위로 정리한 작업일지다.

---

## 📌 이번에 한 일

| 항목 | 내용 |
|------|------|
| 샘플 데이터 | 앱 시작 시 글 3건, 댓글 5건 자동 생성 |
| 글 작성 | `POST /api/v1/posts` |
| 글 삭제 | `DELETE /api/v1/posts/{id}` |
| 댓글 작성 | `POST /api/v1/posts/{postId}/comments` |
| 댓글 삭제 | `DELETE /api/v1/posts/{postId}/comments/{id}` |
| 테스트 | 총 14개, 전부 통과 ✅ |

### 개발 환경
- Java 25, Spring Boot 4.1.2, Gradle
- Spring Data JPA + H2 (dev: 파일 DB, test: 메모리 DB)
- Lombok, Validation

---

## 🗂️ 패키지 구조

```
com.back
├── BackApplication.java
├── domain/post
│   ├── post
│   │   ├── controller/ApiV1PostController.java
│   │   ├── dto/PostDto.java
│   │   ├── entity/Post.java
│   │   ├── repository/PostRepository.java
│   │   └── service/PostService.java
│   └── postComment
│       ├── controller/ApiV1PostCommentController.java
│       ├── dto/PostCommentDto.java
│       └── entity/PostComment.java
└── global
    ├── aspect/ResponseAspect.java
    ├── globalExceptionHandler/GlobalExceptionHandler.java
    ├── initData/BaseInitData.java
    ├── jpa/entity/BaseEntity.java
    └── rsData/RsData.java
```

> 💡 `PostComment` 에는 리포지터리가 없다. 댓글은 항상 **글(Post)을 통해서** 추가/삭제하기 때문이다.

---

## 1️⃣ 프로젝트 초기 설정

> 커밋: `chore: 프로젝트 초기 설정`

처음 빌드를 돌리자마자 컴파일 에러가 났다. 파일 이름은 `BackApplication.java` 인데 클래스 이름이 `P14172StudyApplication` 이었다.
**자바에서 public 클래스 이름은 파일 이름과 같아야 한다**는 걸 다시 한번 확인. 클래스 이름을 `BackApplication` 으로 맞췄다.

그리고 프로필별로 DB를 나눴다.

```yaml
# application-dev.yaml : 개발용, 파일 DB (재시작해도 데이터 유지)
spring:
  datasource:
    url: jdbc:h2:./db_dev;MODE=MySQL

# application-test.yaml : 테스트용, 메모리 DB (테스트 끝나면 사라짐)
spring:
  datasource:
    url: jdbc:h2:mem:db_test;MODE=MySQL
```

- 테스트 클래스에 `@ActiveProfiles("test")` 를 붙이면 `application-test.yaml` 이 적용된다.
- `@EnableJpaAuditing` 을 켜서 `createDate`, `modifyDate` 가 자동으로 채워지게 했다.

---

## 2️⃣ 엔티티 + 샘플 데이터 (글 3건, 댓글 5건)

> 커밋: `feat: 글/댓글 엔티티와 샘플 데이터(글 3건, 댓글 5건) 자동 생성`

### Post ↔ PostComment 관계

```java
@OneToMany(mappedBy = "post", cascade = {PERSIST, REMOVE}, orphanRemoval = true)
private List<PostComment> comments = new ArrayList<>();
```

| 옵션 | 의미 | 이번 작업에서 쓰인 곳 |
|------|------|------|
| `PERSIST` | 글을 저장하면 리스트 안의 새 댓글도 저장 | 댓글 작성 |
| `REMOVE` | 글을 삭제하면 댓글도 같이 삭제 | 글 삭제 |
| `orphanRemoval` | 리스트에서 빠진 댓글은 DB 에서도 삭제 | 댓글 삭제 |

### 샘플 데이터

`ApplicationRunner` 로 앱이 뜰 때 데이터를 넣는다. 이미 데이터가 있으면 건너뛴다.

```java
@Transactional
public void work1() {
    if (postService.count() > 0) return;

    Post post1 = postService.write("제목 1", "내용 1");
    Post post2 = postService.write("제목 2", "내용 2");
    postService.write("제목 3", "내용 3");

    post1.addComment("댓글 1-1"); // 1번 글 : 댓글 3건
    post1.addComment("댓글 1-2");
    post1.addComment("댓글 1-3");
    post2.addComment("댓글 2-1"); // 2번 글 : 댓글 2건
    post2.addComment("댓글 2-2");
}
```

#### 🤔 `self.work1()` 은 왜 필요할까?
`@Transactional` 은 스프링이 만든 **프록시 객체**를 거쳐서 호출될 때만 동작한다.
같은 클래스 안에서 `this.work1()` 로 부르면 프록시를 안 거치기 때문에 트랜잭션이 없다.
그래서 `@Lazy` 로 자기 자신(프록시)을 주입받아서 `self.work1()` 로 호출했다.

트랜잭션이 있어야 `addComment()` 로 리스트에만 넣은 댓글이 **더티체킹 + cascade** 로 DB 에 저장된다.

---

## 3️⃣ 공통 응답 형식 RsData + 전역 예외 처리

> 커밋: `feat: 공통 응답(RsData)과 전역 예외 처리 추가`

조회를 제외한 API 는 모두 이 형식으로 응답한다.

```json
{
  "resultCode": "201-1",
  "msg": "4번 글이 작성되었습니다.",
  "data": { ... }
}
```

- `resultCode` = **HTTP 응답코드 - 세부코드**
- 그런데 `resultCode` 를 `"201-1"` 로 써도 HTTP 상태코드가 자동으로 201 이 되지는 않는다!
- 그래서 `ResponseAspect`(AOP)로 컨트롤러가 `RsData` 를 리턴하면 앞자리를 읽어서 `response.setStatus(201)` 을 해준다.

예외는 `@RestControllerAdvice` 한 곳에서 처리한다.

| 예외 | 상황 | 응답 |
|------|------|------|
| `NoSuchElementException` | 없는 글/댓글 (`Optional.get()` 실패) | 404 / `404-1` |
| `MethodArgumentNotValidException` | `@Valid` 검증 실패 | 400 / `400-1` |
| `HttpMessageNotReadableException` | JSON 형식이 깨짐 | 400 / `400-2` |

덕분에 컨트롤러 코드는 `postService.findById(id).get()` 처럼 **성공 흐름만** 쓰면 된다.

---

## 4️⃣ 글 작성

> 커밋: `feat: 글 작성 API(POST /api/v1/posts)와 테스트`

```http
POST /api/v1/posts
Content-Type: application/json

{ "title": "새 제목", "content": "새 내용" }
```

```java
record PostWriteReqBody(
        @NotBlank @Size(min = 2, max = 100) String title,
        @NotBlank @Size(min = 2, max = 5000) String content
) {}
```

- 요청 본문을 받는 DTO 는 `XxxReqBody` 로 이름 지었다. (Form 보다 의미가 정확함)
- 응답에는 엔티티 대신 `PostDto` 를 담는다. 엔티티를 그대로 노출하면 필드 이름 하나 바꿀 때도 API 가 같이 깨지기 때문.

### 테스트

컨트롤러 테스트는 언제나 **2단계**다.
1. 요청을 날린다.
2. 결과를 검증한다.

```java
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
```

- **응답 JSON 의 모든 필드를 검사**했다. 프론트 개발자와의 약속(API 계약)이 깨지면 바로 알 수 있다.
- 실패 케이스: 제목 없음 → `400-1`, 잘못된 JSON → `400-2`

> ⚠️ Spring Boot 4 에서는 `@AutoConfigureMockMvc` 패키지가
> `org.springframework.boot.webmvc.test.autoconfigure` 로 바뀌었다. 예전 import 로는 안 된다.

---

## 5️⃣ 글 삭제

> 커밋: `feat: 글 삭제 API(DELETE /api/v1/posts/{id})와 테스트`

```java
@DeleteMapping("/{id}")
@Transactional
public RsData<Void> delete(@PathVariable long id) {
    Post post = postService.findById(id).get(); // 없으면 404
    postService.delete(post);

    return new RsData<>("200-1", "%d번 글이 삭제되었습니다.".formatted(id));
}
```

- 돌려줄 데이터가 없으니 `RsData<Void>` 로 명시했다.

### 🐛 테스트가 "너무 쉽게" 통과하는 문제

처음엔 이렇게 테스트했다.

```java
mvc.perform(delete("/api/v1/posts/1"));
assertThat(postService.findById(1)).isEmpty(); // 통과!
```

그런데 생각해보니 테스트는 `@Transactional` 이라서 **끝나면 롤백**된다.
DELETE 쿼리는 커밋(또는 flush) 시점에 나가는데, 롤백되면 쿼리가 **아예 안 나갈 수도** 있다.
`findById` 가 비어있는 건 영속성 컨텍스트(1차 캐시)에서 지워졌기 때문일 뿐, DB 에서 진짜 지워졌는지는 모르는 것!

1번 글에는 댓글이 3개 있으니, cascade 가 제대로 안 되면 **FK 제약조건 오류**가 나야 정상이다.
그래서 `flush()` 를 넣어서 DELETE 쿼리를 실제로 실행시켜 봤다.

```java
postService.flush(); // 여기서 DELETE 쿼리 실행 (커밋은 아님 → 테스트 후 롤백됨)
assertThat(postService.findById(id)).isEmpty();
```

- 실패 케이스: 없는 글 삭제 → `404-1`

---

## 6️⃣ 댓글 작성

> 커밋: `feat: 댓글 작성 API(POST /api/v1/posts/{postId}/comments)와 테스트`

```java
Post post = postService.findById(postId).get();
PostComment postComment = postService.writeComment(post, reqBody.content());

postService.flush(); // ← 이게 없으면 댓글 id 가 0

return new RsData<>(
        "201-1",
        "%d번 댓글이 작성되었습니다.".formatted(postComment.getId()),
        new PostCommentDto(postComment)
);
```

### 🤔 왜 댓글 id 가 0 일까?
- 댓글은 `post.addComment()` 로 **리스트에 넣기만** 했다. `save()` 를 직접 부르지 않았다.
- 실제 INSERT 는 트랜잭션이 끝날 때(더티체킹 + cascade PERSIST) 실행된다.
- id 는 DB 의 AUTO_INCREMENT 로 만들어지니, INSERT 전에는 기본값 `0` 이다.
- 그래서 응답을 만들기 전에 `flush()` 로 INSERT 를 **미리** 실행시켰다.

> `flush()` ≠ 커밋. 쿼리만 먼저 보내고 트랜잭션은 계속 유지된다. 이후 롤백도 가능.

- 실패 케이스: 내용 없음 → `400-1`, 없는 글에 댓글 → `404-1`

---

## 7️⃣ 댓글 삭제

> 커밋: `feat: 댓글 삭제 API(DELETE /api/v1/posts/{postId}/comments/{id})와 테스트`

```java
Post post = postService.findById(postId).get();
PostComment postComment = post.findCommentById(id).get(); // 이 글의 댓글 중에서만 찾는다
postService.deleteComment(post, postComment);             // 리스트에서 제거 → orphanRemoval 로 DB 삭제
```

### 🤔 왜 PostCommentRepository 로 바로 지우지 않았을까?
`/api/v1/posts/2/comments/1` 처럼 **다른 글의 댓글 번호**로 요청이 올 수 있다.
댓글 리포지터리로 id 만 보고 지우면 2번 글 경로로 1번 글의 댓글이 지워진다.
`post.findCommentById(id)` 로 **해당 글의 댓글 목록 안에서만** 찾으면 이 문제가 자연스럽게 막힌다.
이 케이스도 테스트로 만들어 두었다. (`댓글 삭제, 다른 글의 댓글 번호로 요청하면 404`)

---

## ✅ 테스트 결과

```bash
./gradlew test
```

| 테스트 클래스 | 개수 | 결과 |
|------|------|------|
| `BackApplicationTests` | 1 | ✅ |
| `BaseInitDataTest` (글 3건/댓글 5건) | 2 | ✅ |
| `ApiV1PostControllerTest` (글 작성/삭제) | 5 | ✅ |
| `ApiV1PostCommentControllerTest` (댓글 작성/삭제) | 6 | ✅ |
| **합계** | **14** | **✅ 전부 통과** |

---

## 📝 회고

- **`@Transactional` 이 실제로 무슨 일을 하는지** 가 이번 작업의 핵심이었다.
  - 샘플 데이터: 트랜잭션이 있어야 더티체킹 + cascade 로 댓글이 저장된다.
  - 댓글 작성: INSERT 가 트랜잭션 끝에 나가서 id 가 0 → `flush()` 로 해결.
  - 테스트: 롤백 덕분에 테스트끼리 영향을 안 주지만, 반대로 쿼리가 안 나가서 "가짜 통과"가 될 수 있다.
- 실패 케이스는 전부 다 테스트하기보다 **대표 케이스 1~2개**만 했다. (가성비)
- 다음에 해볼 것: 글/댓글 **조회**, **수정** API 와 테스트, Swagger(SpringDoc) 문서화

---

## 🔗 API 요약

| 메서드 | URL | 성공 | 실패 |
|------|------|------|------|
| `POST` | `/api/v1/posts` | 201 `201-1` | 400 `400-1`, `400-2` |
| `DELETE` | `/api/v1/posts/{id}` | 200 `200-1` | 404 `404-1` |
| `POST` | `/api/v1/posts/{postId}/comments` | 201 `201-1` | 400 `400-1`, 404 `404-1` |
| `DELETE` | `/api/v1/posts/{postId}/comments/{id}` | 200 `200-1` | 404 `404-1` |
