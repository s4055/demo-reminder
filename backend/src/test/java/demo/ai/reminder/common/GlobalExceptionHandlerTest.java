package demo.ai.reminder.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 테스트 전용 컨트롤러로 각종 예외를 발생시켜 GlobalExceptionHandler의 변환 결과를 검증한다.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("ResponseStatusException(404)은 404와 NOT_FOUND, 예외 사유를 담은 응답으로 변환된다")
    void responseStatusException_notFound_isConvertedToNotFound() throws Exception {
        mockMvc.perform(get("/test/status/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.resultMsg").value("reason-404"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("ResponseStatusException(400)은 400과 BAD_REQUEST 응답으로 변환된다")
    void responseStatusException_badRequest_isConvertedToBadRequest() throws Exception {
        mockMvc.perform(get("/test/status/400"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.resultMsg").value("reason-400"));
    }

    @Test
    @DisplayName("별도 매핑이 없는 4xx 상태는 원래 상태 코드를 유지하고 BAD_REQUEST로 변환된다")
    void responseStatusException_otherClientError_fallsBackToBadRequest() throws Exception {
        mockMvc.perform(get("/test/status/409"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.resultMsg").value("reason-409"));
    }

    @Test
    @DisplayName("사유 없는 ResponseStatusException은 ResultCode의 기본 메시지를 사용한다")
    void responseStatusException_withoutReason_usesDefaultMessage() throws Exception {
        mockMvc.perform(get("/test/status-without-reason"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.resultMsg").value("대상을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("5xx ResponseStatusException은 원래 상태 코드를 유지하고 INTERNAL_SERVER_ERROR로 변환된다")
    void responseStatusException_serverError_isConvertedToInternalServerError() throws Exception {
        mockMvc.perform(get("/test/status/503"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.resultCode").value("INTERNAL_SERVER_ERROR"));
    }

    @Test
    @DisplayName("@Valid 검증 실패는 400과 BAD_REQUEST, 실패한 모든 필드의 메시지로 변환된다")
    void methodArgumentNotValid_isConvertedToBadRequestWithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"memo\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.resultMsg").value(allOf(containsString("title: "), containsString("memo: "))));
    }

    @Test
    @DisplayName("JSON 본문을 읽을 수 없으면 400과 BAD_REQUEST 응답으로 변환된다")
    void httpMessageNotReadable_isConvertedToBadRequest() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.resultMsg").isString());
    }

    @Test
    @DisplayName("경로 변수 타입이 맞지 않으면 400과 BAD_REQUEST 응답으로 변환된다")
    void typeMismatch_isConvertedToBadRequest() throws Exception {
        mockMvc.perform(get("/test/number/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("필수 요청 파라미터가 없으면 400과 BAD_REQUEST 응답으로 변환된다")
    void missingRequestParameter_isConvertedToBadRequest() throws Exception {
        mockMvc.perform(get("/test/required-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드는 405와 METHOD_NOT_ALLOWED로 변환되고 Allow 헤더를 유지한다")
    void methodNotSupported_isConvertedToMethodNotAllowed() throws Exception {
        mockMvc.perform(delete("/test/validate"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("POST")))
                .andExpect(jsonPath("$.resultCode").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type은 415를 유지하고 BAD_REQUEST로 변환된다")
    void mediaTypeNotSupported_isConvertedToBadRequest() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("title"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("매핑되지 않은 경로는 404와 NOT_FOUND 응답으로 변환된다")
    void noHandlerFound_isConvertedToNotFound() throws Exception {
        mockMvc.perform(get("/test/unknown-path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("예상하지 못한 예외는 500과 INTERNAL_SERVER_ERROR로 변환되고 내부 메시지를 노출하지 않는다")
    void unexpectedException_isConvertedToInternalServerError_withoutLeakingMessage() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.resultCode").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.resultMsg").value("서버 오류가 발생했습니다."))
                .andExpect(jsonPath("$.resultMsg").value(not(containsString("secret"))))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/status/{code}")
        void throwResponseStatus(@PathVariable int code) {
            throw new ResponseStatusException(HttpStatus.valueOf(code), "reason-" + code);
        }

        @GetMapping("/test/status-without-reason")
        void throwResponseStatusWithoutReason() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        @PostMapping("/test/validate")
        void validate(@Valid @RequestBody ValidatedRequest request) {
        }

        @GetMapping("/test/number/{id}")
        void number(@PathVariable Long id) {
        }

        @GetMapping("/test/required-param")
        void requiredParam(@RequestParam String name) {
        }

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("secret internal detail");
        }
    }

    record ValidatedRequest(@NotBlank String title, @NotBlank String memo) {
    }
}
