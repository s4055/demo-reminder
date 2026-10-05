package demo.ai.reminder.controller;

import demo.ai.reminder.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 실제 보안 필터를 거쳐 세션(쿠키) 기반 인증을 검증한다. 로그인 상태는 응답으로 받은 세션으로 이어 간다.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("회원가입하면 201과 사용자 정보를 반환하고 바로 로그인된 세션이 생긴다")
    void signup_returnsUser_andStartsSession() throws Exception {
        MockHttpSession session = signup("Alice@Example.com", "password1", "앨리스");

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.name").value("앨리스"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("이미 가입한 이메일(대소문자 무시)로 가입하면 409를 반환한다")
    void signup_withDuplicateEmail_returnsConflict() throws Exception {
        signup("alice@example.com", "password1", "앨리스");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("ALICE@example.com", "password2", "다른 앨리스")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("CONFLICT"));
    }

    @Test
    @DisplayName("이메일 형식이 틀리거나 비밀번호가 8자 미만이면 400을 반환한다")
    void signup_withInvalidFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("not-an-email", "short", "앨리스")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
        assertThat(userRepository.count()).isZero();
    }

    @Test
    @DisplayName("올바른 이메일/비밀번호로 로그인하면 사용자 정보를 반환하고 세션으로 API를 호출할 수 있다")
    void login_withValidCredentials_startsSession() throws Exception {
        signup("alice@example.com", "password1", "앨리스");

        MockHttpSession session = login("ALICE@example.com", "password1");

        mockMvc.perform(get("/api/lists").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"));
    }

    @Test
    @DisplayName("비밀번호가 틀리거나 없는 이메일이면 같은 메시지로 401을 반환하고 세션을 만들지 않는다")
    void login_withInvalidCredentials_returnsUnauthorized() throws Exception {
        signup("alice@example.com", "password1", "앨리스");

        for (String body : new String[]{
                "{\"email\":\"alice@example.com\",\"password\":\"wrong-password\"}",
                "{\"email\":\"nobody@example.com\",\"password\":\"password1\"}"}) {
            var result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.resultCode").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.resultMsg").value("Invalid email or password"))
                    .andReturn();
            assertThat(result.getRequest().getSession(false)).isNull();
        }
    }

    @Test
    @DisplayName("로그아웃하면 세션이 무효화되어 이후 요청은 401이다")
    void logout_invalidatesSession() throws Exception {
        MockHttpSession session = signup("alice@example.com", "password1", "앨리스");

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"));

        assertThat(session.isInvalid()).isTrue();
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그인하지 않고 API를 호출하면 ApiResponse 형식의 401을 반환한다")
    void api_withoutLogin_returnsUnauthorizedApiResponse() throws Exception {
        mockMvc.perform(get("/api/reminders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.resultMsg").value("로그인이 필요합니다."))
                .andExpect(jsonPath("$.data").isEmpty());
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("두 사용자는 서로의 리스트를 볼 수 없고, 다른 사용자의 리스트를 수정하면 404다")
    void users_cannotSeeOrModifyEachOthersLists() throws Exception {
        MockHttpSession alice = signup("alice@example.com", "password1", "앨리스");
        MockHttpSession bob = signup("bob@example.com", "password1", "밥");
        String location = mockMvc.perform(post("/api/lists").session(alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"앨리스 장보기\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long aliceListId = Long.parseLong(location.replaceAll(".*\"data\":\\{\"id\":(\\d+).*", "$1"));

        mockMvc.perform(get("/api/lists").session(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
        mockMvc.perform(put("/api/lists/{id}", aliceListId).session(bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"가로채기\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/lists").session(alice))
                .andExpect(jsonPath("$.data[0].name").value("앨리스 장보기"));
    }

    @Test
    @DisplayName("프론트엔드 출처의 preflight에는 쿠키를 보낼 수 있도록 Allow-Credentials를 응답한다")
    void corsPreflight_fromFrontend_allowsCredentials() throws Exception {
        mockMvc.perform(options("/api/reminders")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        mockMvc.perform(options("/api/reminders")
                        .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession signup(String email, String password, String name) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(email, password, name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(email.toLowerCase()))
                .andReturn().getRequest().getSession(false);
    }

    private MockHttpSession login(String email, String password) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email.toLowerCase()))
                .andReturn().getRequest().getSession(false);
    }

    private static String signupJson(String email, String password, String name) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"name\":\"" + name + "\"}";
    }
}
