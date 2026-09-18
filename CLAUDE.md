# CLAUDE.md

이 저장소에서 작업할 때 지켜야 할 코딩 관례입니다.

## 프로젝트 구조

- `spec.md` — 요구사항(PRD), `plan.md` — Phase별 개발 계획, `tasks.md` — Phase별 체크리스트.
- 기능 범위나 다음 작업을 특정하지 않고 요청받으면, 임의로 범위를 정하지 말고 `tasks.md`에서 현재 Phase의 다음 미완료 항목을 확인한다. 작업 완료 후에는 `tasks.md`의 체크박스를 갱신한다.

## backend 하위 - 백엔드(Spring Boot) 관례

### 엔티티

- 필드는 생성자 또는 의도가 드러나는 도메인 메서드를 통해서만 변경한다.
- JPA용 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 제한해 외부에서 빈 객체를 생성하지 못하게 한다.
- `createdAt`/`updatedAt`은 자바 코드(`LocalDateTime.now()`)가 아니라 DB가 계산하도록 위임한다. Hibernate의 `@CreationTimestamp(source = SourceType.DB)`, `@UpdateTimestamp(source = SourceType.DB)`를 사용하고, `createdAt`은 `@Column(updatable = false)`로 수정 불가 처리한다.

### 요청/응답 DTO

- 컨트롤러의 요청/응답 객체는 순수 DTO여야 한다. Lombok getter/setter 클래스가 아니라 불변 Java `record`로 작성한다.

### API 명세

- API를 추가하거나 변경할 때는 `backend/openapi.yml`도 같은 작업 안에서 함께 갱신한다.

### 테스트

- 기능을 새로 추가하거나 기존 동작을 수정할 때는 이를 검증하는 테스트도 같은 작업 안에서 함께 작성한다.
- 테스트 메서드명은 영어(camelCase)로 작성하고, 사람이 읽을 설명은 `@DisplayName`(한글)으로 표현한다.
- Service= 테스트는 @SpringBootTest + @Transactional를 이용한 통합 테스트로 작성한다.
