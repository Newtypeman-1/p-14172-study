# [Spring Boot] 글/댓글 CRUD API를 TDD로 처음부터 다시 만들기

> 📅 2026-11-18
> 🏷️ `Spring Boot 4` `JPA` `REST API` `TDD` `MockMvc`

지난번에는 글/댓글 **작성·삭제**를 만들었다. 그런데 돌아보니 테스트와 구현을 **같이** 써서, 테스트가 처음부터 통과했다.
"테스트가 있는 코드"였지 **TDD**는 아니었던 것. (이전 작업일지 → [README_old.md](README_old.md), 코드 → `v1` 브랜치)

그래서 이번에는 프로젝트를 **제로에서 다시 시작**해서, 글/댓글 **CRUD 전체**를 TDD로 만들었다.
강의 노트([slog.gg/p/14172](https://www.slog.gg/p/14172))의 2부(REST API by TDD) 흐름을 따랐다.

---

## 📌 이번에 한 일

| 항목 | 내용 |
|------|------|
| 브랜치 | 이전 작업은 `v1` 으로 보존, 새로 시작한 작업이 `main` |
| 샘플 데이터 | 앱 시작 시 글 3건, 댓글 5건 자동 생성 |
| 글 CRUD | 다건조회 / 단건조회 / 작성 / 수정 / 삭제 |
| 댓글 CRUD | 다건조회 / 단건조회 / 작성 / 수정 / 삭제 |
| 방식 | 기능마다 🔴 Red 커밋 → 🟢 Green 커밋 |
| 커밋 | `001` ~ `030`, 번호만 보고 순서대로 따라갈 수 있게 |
| 테스트 | 20개 전부 통과 ✅ |

### 개발 환경
- Java 25, Spring Boot 4.1.2, Gradle(Kotlin DSL)
- Spring Data JPA + H2 (dev: 파일 DB, test: 메모리 DB)
- Lombok, Validation

---

## 🔁 TDD를 커밋으로 남기는 규칙

```
🔴 Red      테스트 먼저 작성 → 실행해서 "의도한 이유로" 실패하는지 확인 → 커밋
🟢 Green    테스트를 통과시키는 최소한의 구현 → 전체 테스트 통과 확인 → 커밋
🔵 Refactor 초록 상태를 유지하면서 정리 → 커밋
```

- Red 커밋 메시지에는 **실제로 어떤 메시지로 실패했는지**를 적었다.
- Red 커밋에서 테스트가 컴파일조차 안 되면 실패 이유를 볼 수 없다. 그래서 테스트가 참조하는 클래스가 없을 때는 **빈 컨트롤러 클래스만** 같이 넣었다. (006, 016)

> 💡 Red 커밋으로 돌아가서 `./gradlew test` 를 돌리면 실패하는 걸 직접 볼 수 있다.
> `git log --oneline` 에서 `010` 커밋의 해시를 찾아 `git checkout <해시>`, 다 보면 `git checkout main`.

---

## 🌿 브랜치 정리: 제로에서 다시 시작하기

```bash
git branch v1 main              # 1. 이전 작업을 v1 으로 보존
git checkout --orphan new-main  # 2. 커밋이 하나도 없는 새 브랜치
# ... 001 ~ 030 커밋 ...
git branch -M new-main main     # 3. 새 브랜치를 main 으로
git push origin v1
git push --force-with-lease origin main
```

- `--orphan` 은 부모 커밋이 없는 브랜치를 만든다. 그래서 `main` 의 히스토리가 `001` 부터 시작한다.
- 원격 `main` 과 히스토리가 완전히 달라지므로 **force push** 가 필요하다.
  `--force-with-lease` 는 "내가 마지막으로 본 원격 상태와 같을 때만" 덮어써서, 남이 올린 커밋을 실수로 날리는 걸 막는다.

---

## 🗂️ 최종 패키지 구조

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
│       └── entity/PostComment.java          ← 리포지터리 없음! 댓글은 항상 Post 를 통해서
└── global
    ├── aspect/ResponseAspect.java
    ├── globalExceptionHandler/GlobalExceptionHandler.java
    ├── initData/BaseInitData.java
    ├── jpa/entity/BaseEntity.java
    └── rsData/RsData.java
```

---

## 1️⃣ 기반 작업 (001 ~ 005)

| 커밋 | 내용 |
|------|------|
| `001` | Spring Boot 프로젝트 생성 |
| `002` | 프로필별 DB (dev: `jdbc:h2:./db_dev`, test: `jdbc:h2:mem:db_test`) |
| `003` | `BaseEntity`, `Post`, `PostComment`, `PostRepository`, `PostService` 골격 |
| `004` 🔴 | 샘플 데이터 테스트 |
| `005` 🟢 | `BaseInitData` 로 글 3건, 댓글 5건 생성 |

샘플 데이터부터 TDD로 했다. `004` 에서는 글이 0개라서 실패했고, `005` 에서 통과했다.

```java
@Test
@DisplayName("1번 글에는 댓글 3건, 2번 글에는 댓글 2건, 3번 글에는 댓글이 없다.")
void t2() {
    assertThat(postService.findById(1).get().getComments()).hasSize(3);
    assertThat(postService.findById(2).get().getComments()).hasSize(2);
    assertThat(postService.findById(3).get().getComments()).isEmpty();
}
```

`BaseInitData` 에서 `this.work1()` 이 아니라 `self.work1()` 로 호출한다. `@Transactional` 은 스프링이 만든 **프록시**를 거쳐야 동작하기 때문이다.
트랜잭션이 없으면 `post.addComment()` 로 리스트에만 넣은 댓글이 저장되지 않는다.

---

## 2️⃣ 글 CRUD (006 ~ 015)

| 커밋 | 기능 | Red 에서 실패한 이유 |
|------|------|------|
| `006` 🔴 / `007` 🟢 | 단건조회 `GET /posts/{id}` | 처리할 메서드가 없음 → 정적 리소스 핸들러가 받음 |
| `008` 🔴 / `009` 🟢 | 다건조회 `GET /posts` | 위와 같음 |
| `010` 🔴 / `011` 🟢 | 작성 `POST /posts` | 같은 URL 에 GET 만 있어서 **405** |
| `012` 🔴 / `013` 🟢 | 수정 `PUT /posts/{id}` | 405 |
| `014` 🔴 / `015` 🟢 | 삭제 `DELETE /posts/{id}` | 405 |

### 컨트롤러 테스트의 기본 모양

모든 테스트가 **1. 요청을 날린다, 2. 결과를 검증한다** 의 2단계를 따른다.

```java
ResultActions resultActions = mvc
        .perform(get("/api/v1/posts/%d".formatted(id)))
        .andDo(print());

resultActions
        .andExpect(handler().handlerType(ApiV1PostController.class)) // 어떤 컨트롤러가
        .andExpect(handler().methodName("getItem"))                  // 어떤 메서드로 처리했는지
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(post.getId()))             // 응답 JSON 의 "모든" 필드
        .andExpect(jsonPath("$.createDate").value(matchesPattern(DATE_TIME_PATTERN)))
        .andExpect(jsonPath("$.modifyDate").value(matchesPattern(DATE_TIME_PATTERN)))
        .andExpect(jsonPath("$.title").value(post.getTitle()))
        .andExpect(jsonPath("$.content").value(post.getContent()));
```

- `handler()` 검사 덕분에 Red 단계에서 실패 이유가 분명하게 보였다. 메서드가 없으면 `ResourceHttpRequestHandler` 가 대신 받는다.
- 날짜는 정확한 값을 맞추기 어려워서 **형식만** 확인했다.

### Green 단계에서 하나씩 생긴 것들

처음부터 공통 클래스를 다 만들어 두지 않았다. **테스트가 요구할 때** 만들었다.

| 언제 | 무엇이 | 왜 |
|------|------|------|
| `007` 단건조회 | `RsData`, `GlobalExceptionHandler`(404) | 없는 글 테스트가 `{"resultCode":"404-1"}` 을 요구 |
| `011` 작성 | `ResponseAspect`, 400 처리 | 테스트가 `201 Created` 와 검증 실패 `400-1` 을 요구 |
| `013` 수정 | `Post.modify()` | 더티체킹으로 UPDATE |
| `015` 삭제 | `PostService.delete()` | cascade REMOVE 로 댓글 먼저 삭제 |

#### `ResponseAspect`: resultCode 와 HTTP 응답코드 맞추기
`new RsData<>("201-1", ...)` 을 리턴해도 HTTP 응답코드는 **200 그대로**다.
AOP 로 `RsData` 를 리턴하는 컨트롤러 메서드에 끼어들어 `response.setStatus(201)` 을 해 준다.

```java
@Around("""
        execution(public com.back.global.rsData.RsData *(..)) &&
        within(@org.springframework.web.bind.annotation.RestController *)
        """)
public Object handleResponse(ProceedingJoinPoint joinPoint) throws Throwable {
    Object proceed = joinPoint.proceed();
    response.setStatus(((RsData<?>) proceed).statusCode()); // "201-1" -> 201
    return proceed;
}
```

### 🐛 글 삭제 테스트의 "가짜 통과" 막기

테스트 클래스는 `@Transactional` 이라 끝나면 **롤백**된다. DELETE 쿼리는 flush/커밋 때 나가므로, 그냥 두면 **쿼리가 DB 에 가지 않은 채** 통과할 수 있다.

```java
em.flush();  // DELETE 를 실제로 실행 (1번 글엔 댓글 3개 → cascade 가 없으면 FK 오류)
em.clear();  // 1차 캐시를 비워서 아래 조회가 DB 에서 읽도록
assertThat(postService.findById(id)).isEmpty();
```

테스트 로그로 실행 순서도 확인했다.
```
delete from post_comment where id=?   ← 댓글 3개 먼저
delete from post_comment where id=?
delete from post_comment where id=?
delete from post where id=?           ← 그다음 글
```

---

## 3️⃣ 댓글 CRUD (016 ~ 028)

| 커밋 | 기능 |
|------|------|
| `016` 🔴 / `017` 🟢 | 단건조회 `GET /posts/{postId}/comments/{id}` |
| `018` 🔴 / `019` 🟢 | 다건조회 `GET /posts/{postId}/comments` |
| `020` 🔴 / `021` 🔴 / `022` 🔴 / `023` 🟢 | 작성 `POST /posts/{postId}/comments` ← **여기가 이번 작업의 하이라이트** |
| `024` 🔴 / `025` 🟢 | 수정 `PUT /posts/{postId}/comments/{id}` |
| `026` 🔴 / `027` 🟢 | 삭제 `DELETE /posts/{postId}/comments/{id}` |
| `028` 🔵 | 테스트 도우미 리팩터링 |

### URL 의 postId 를 무시하지 않기

```java
Post post = postService.findById(postId).get();          // 글이 없으면 404
PostComment postComment = post.findCommentById(id).get(); // "이 글의" 댓글이 아니면 404
```

댓글 리포지터리로 `id` 만 보고 찾으면 `/posts/2/comments/1` 요청에도 1번 글의 댓글이 조회되고 **삭제까지** 된다.
그래서 조회(`016`)와 삭제(`026`)에 "다른 글의 댓글 번호로 요청하면 404" 테스트를 넣었다.

### 🔥 댓글 작성: 테스트가 버그를 놓칠 뻔한 이야기 (020 → 023)

**`021` 1차 구현:** 댓글을 리스트에 넣고 바로 응답을 만들었다.

```java
PostComment postComment = postService.writeComment(post, reqBody.content()); // post.addComment()
return new RsData<>("201-1", "%d번 댓글이 작성되었습니다.".formatted(postComment.getId()), new PostCommentDto(postComment));
```

결과는 실패였다.
```
JSON path "$.data.createDate"
Expected: a string matching the pattern '^\d{4}-...'
     but: was null
```

`addComment()` 는 리스트에 넣기만 하고, 실제 INSERT 는 **트랜잭션이 끝날 때** cascade PERSIST 로 실행된다.
그런데 응답 DTO 는 그 전에 만들어지니까, 이 시점의 댓글은 `id = 0`, `createDate = null` 이다.

**그런데 이상한 점이 있었다.** 응답의 `id` 도 0 이었을 텐데, `$.data.id` 검증은 **통과**했다.

```java
PostComment postComment = post.getComments().getLast(); // 기대값도 아직 INSERT 전 → id 0
.andExpect(jsonPath("$.data.id").value(postComment.getId())) // 0 == 0 → 통과!
```

기대값을 **실제 코드가 만든 값**에서 가져왔기 때문에, 코드가 틀리면 기대값도 똑같이 틀린 것이다.
`createDate` 검사가 없었다면 이 버그는 그대로 지나갔을 것이다.

**`022` 테스트 보강:** 실제 코드와 상관없는 규칙으로 한 번 더 검사한다.
```java
.andExpect(jsonPath("$.data.id").value(greaterThan(0)))
```
```
JSON path "$.data.id"
Expected: a value greater than <0>
     but: <0> was equal to <0>      ← 이제 id 문제를 정확히 잡는다
```

**`023` 해결:** 응답을 만들기 전에 `flush()` 를 호출한다.
```java
PostComment postComment = postService.writeComment(post, reqBody.content());
postService.flush(); // INSERT 실행 → id, createDate, modifyDate 채워짐
```

#### 🤔 글 작성에는 왜 flush 가 필요 없을까?
| | 글 작성 | 댓글 작성 |
|------|------|------|
| 저장 방법 | `postRepository.save(post)` → `em.persist()` | `post.getComments().add(...)` |
| INSERT 시점 | **즉시** (IDENTITY 는 id 를 받아야 해서 persist 때 바로 INSERT) | flush/커밋 때 (cascade PERSIST) |
| 응답 만들 때 id | 있음 | 0 → `flush()` 필요 |

> `flush()` 는 커밋이 아니다. 쿼리만 먼저 보내고 트랜잭션은 계속된다. 이후 예외가 나면 함께 롤백된다.

### 댓글 삭제와 orphanRemoval

```java
post.getComments().remove(postComment); // 리스트에서 빼기만 하면
// orphanRemoval = true → 부모와 관계가 끊긴 댓글을 JPA 가 DELETE
```

### 🔵 `028` 리팩터링
`016`(Red) 시점에는 `Post.findCommentById()` 가 없어서, 테스트 도우미가 `stream().filter()` 로 직접 찾았다.
`017`(Green) 에서 같은 기능이 생겼으니 그걸 쓰도록 정리했다. 테스트 20개가 그대로 초록인 걸 확인한 뒤 커밋했다.

---

## ✅ 테스트 결과

```bash
./gradlew test
```

| 테스트 클래스 | 개수 | 결과 |
|------|------|------|
| `BackApplicationTests` | 1 | ✅ |
| `BaseInitDataTest` | 2 | ✅ |
| `ApiV1PostControllerTest` | 8 | ✅ |
| `ApiV1PostCommentControllerTest` | 9 | ✅ |
| **합계** | **20** | **✅ 전부 통과** |

---

## 🔗 API 요약

조회 성공은 데이터를 그대로 응답하고, 나머지는 `{ resultCode, msg, data }`(RsData) 로 응답한다.

| 메서드 | URL | 성공 | 실패 |
|------|------|------|------|
| `GET` | `/api/v1/posts` | 200 | |
| `GET` | `/api/v1/posts/{id}` | 200 | 404 `404-1` |
| `POST` | `/api/v1/posts` | 201 `201-1` | 400 `400-1` |
| `PUT` | `/api/v1/posts/{id}` | 200 `200-1` | 404 `404-1` |
| `DELETE` | `/api/v1/posts/{id}` | 200 `200-1` | 404 `404-1` |
| `GET` | `/api/v1/posts/{postId}/comments` | 200 | 404 `404-1` |
| `GET` | `/api/v1/posts/{postId}/comments/{id}` | 200 | 404 `404-1` |
| `POST` | `/api/v1/posts/{postId}/comments` | 201 `201-1` | 400 `400-1`, 404 `404-1` |
| `PUT` | `/api/v1/posts/{postId}/comments/{id}` | 200 `200-1` | 404 `404-1` |
| `DELETE` | `/api/v1/posts/{postId}/comments/{id}` | 200 `200-1` | 404 `404-1` |

---

## 📝 회고

- **Red 를 직접 보는 게 핵심이었다.** 지난번처럼 처음부터 통과하는 테스트는, 테스트가 정말 무언가를 검사하는지 알 수 없다.
  이번에는 매번 "의도한 이유로 실패하는지"를 확인했다. 405 와 "No handler" 의 차이 같은 것도 그 과정에서 알게 됐다.
- **기대값을 실제 코드에서 가져오면 함정이 있다.** 댓글 id 가 `0 == 0` 으로 통과한 게 이번 작업에서 가장 큰 배움이었다.
  "응답 JSON 의 모든 필드를 검사하라"는 말이 왜 중요한지 직접 겪었다. `createDate` 하나가 버그를 잡아 줬다.
- **롤백되는 테스트는 쿼리가 안 나갈 수 있다.** 삭제처럼 DB 제약조건이 중요한 테스트는 `em.flush()` 로 쿼리를 실제로 보내 봐야 한다.
- **공통 코드는 테스트가 요구할 때 만든다.** `RsData`, `ResponseAspect`, 예외 처리를 미리 만들지 않고 필요해진 커밋에서 추가했더니, 각각 왜 필요한지가 커밋에 그대로 남았다.

### 다음에 해볼 것
- 회원(Member) 기능, 그리고 "내가 쓴 글/댓글만 수정·삭제"하는 권한 검사
- 댓글이 많아질 때의 정렬과 조회 성능 (`@OrderBy`, 페이징)
- Swagger(SpringDoc)로 API 문서 자동 생성
