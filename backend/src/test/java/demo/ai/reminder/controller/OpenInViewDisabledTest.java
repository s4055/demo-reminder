package demo.ai.reminder.controller;

import com.jayway.jsonpath.JsonPath;
import demo.ai.reminder.repository.user.RefreshTokenRepository;
import demo.ai.reminder.repository.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenInViewDisabledTest {

    private static final String EMAIL = "osiv-test@example.com";

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private Long listId;

    private String accessToken;

    @AfterEach
    void deleteData() throws Exception {
        if (listId != null) {
            List<Integer> tagIds = JsonPath.read(send("GET", "/api/tags", null).body(), "$.data[*].id");
            for (Integer tagId : tagIds) {
                send("DELETE", "/api/tags/" + tagId, null);
            }
            send("DELETE", "/api/lists/" + listId, null);
        }
        refreshTokenRepository.deleteAll();
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("open-in-view가 꺼져 있어 요청 전체에 걸쳐 영속성 컨텍스트를 열어 두지 않는다")
    void openInView_isDisabled() {
        assertThat(applicationContext.getBeanNamesForType(OpenEntityManagerInViewInterceptor.class)).isEmpty();
    }

    @Test
    @DisplayName("트랜잭션 밖에서도 리마인더의 태그/하위 작업과 멤버의 사용자 정보가 응답에 담긴다")
    void responses_includeLazyAssociations_withoutOpenInView() throws Exception {
        signup();
        listId = idOf(send("POST", "/api/lists", "{\"name\":\"장보기\"}"));
        Long parentId = idOf(send("POST", "/api/reminders",
                "{\"title\":\"이사 준비\",\"listId\":" + listId + ",\"tagNames\":[\"집\"]}"));
        send("POST", "/api/reminders", "{\"title\":\"박스 구하기\",\"parentId\":" + parentId + "}");

        HttpResponse<String> reminders = send("GET", "/api/reminders?listId=" + listId, null);
        HttpResponse<String> toggled = send("PATCH", "/api/reminders/" + parentId + "/flag", null);
        HttpResponse<String> members = send("GET", "/api/lists/" + listId + "/members", null);

        assertThat(reminders.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<List<String>>read(reminders.body(), "$.data[0].tags")).containsExactly("집");
        assertThat(JsonPath.<List<String>>read(reminders.body(), "$.data[0].subtasks[*].title"))
                .containsExactly("박스 구하기");
        assertThat(toggled.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Object>read(toggled.body(), "$.data")).isNull();
        assertThat(members.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(members.body(), "$.data[0].email")).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("수정 요청이 끝나면 변경 내용과 올라간 버전이 커밋되어 다음 조회에 보인다")
    void mutation_isCommittedWithIncrementedVersion_whenRequestEnds() throws Exception {
        signup();
        listId = idOf(send("POST", "/api/lists", "{\"name\":\"장보기\"}"));
        Long reminderId = idOf(send("POST", "/api/reminders", "{\"title\":\"우유\",\"listId\":" + listId + "}"));

        send("PATCH", "/api/reminders/" + reminderId + "/flag", null);
        HttpResponse<String> reminders = send("GET", "/api/reminders?listId=" + listId, null);

        assertThat(JsonPath.<Boolean>read(reminders.body(), "$.data[0].flagged")).isTrue();
        assertThat(JsonPath.<Integer>read(reminders.body(), "$.data[0].version")).isEqualTo(1);
    }

    private static Long idOf(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return JsonPath.<Number>read(response.body(), "$.data.id").longValue();
    }

    private void signup() throws Exception {
        HttpResponse<String> response = send("POST", "/api/auth/signup",
                "{\"email\":\"" + EMAIL + "\",\"password\":\"password1\",\"name\":\"오시브\"}");
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        accessToken = JsonPath.read(response.body(), "$.data.accessToken");
    }

    private HttpResponse<String> send(String method, String path, String json) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
