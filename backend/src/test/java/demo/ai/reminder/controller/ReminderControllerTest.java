package demo.ai.reminder.controller;

import demo.ai.reminder.domain.Priority;
import demo.ai.reminder.domain.Reminder;
import demo.ai.reminder.repository.ReminderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReminderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReminderRepository reminderRepository;

    @Test
    @DisplayName("리마인더 목록 조회 응답은 resultCode, resultMsg와 data 배열을 포함한다")
    void getReminders_wrapsListInApiResponse() throws Exception {
        reminderRepository.save(new Reminder("우유 사기", null, null, null));

        mockMvc.perform(get("/api/reminders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.resultMsg").value("성공"))
                .andExpect(jsonPath("$.data[*].title").value(hasItem("우유 사기")));
    }

    @Test
    @DisplayName("리마인더 생성 응답은 201과 함께 생성된 리마인더를 data에 담는다")
    void createReminder_returnsCreatedReminderInData() throws Exception {
        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"우유 사기\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.title").value("우유 사기"));
    }

    @Test
    @DisplayName("우선순위를 생략하고 생성하면 응답의 priority는 NONE이다")
    void createReminder_returnsNonePriority_whenPriorityIsOmitted() throws Exception {
        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"우유 사기\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.priority").value("NONE"));
    }

    @Test
    @DisplayName("우선순위를 지정해 생성하면 응답에 해당 priority를 담는다")
    void createReminder_returnsGivenPriority() throws Exception {
        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"우유 사기\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.priority").value("HIGH"));
    }

    @Test
    @DisplayName("리마인더 수정 응답은 변경된 priority를 담는다")
    void updateReminder_returnsChangedPriority() throws Exception {
        Reminder saved = reminderRepository.save(new Reminder("우유 사기", null, null, null));

        mockMvc.perform(put("/api/reminders/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"우유 사기\",\"flagged\":false,\"priority\":\"LOW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data.priority").value("LOW"));
    }

    @Test
    @DisplayName("리마인더 목록 조회 응답의 각 항목은 priority를 포함한다")
    void getReminders_includesPriority() throws Exception {
        Reminder saved = reminderRepository.save(new Reminder("우유 사기", null, null, null, Priority.MEDIUM));

        mockMvc.perform(get("/api/reminders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + saved.getId() + ")].priority").value(hasItem("MEDIUM")));
    }

    @Test
    @DisplayName("지원하지 않는 우선순위 값으로 생성하면 400과 BAD_REQUEST 응답을 반환한다")
    void createReminder_returnsBadRequest_whenPriorityIsUnknown() throws Exception {
        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"우유 사기\",\"priority\":\"URGENT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("완료 토글 응답은 토글된 리마인더와 완료일시를 data에 담는다")
    void toggleComplete_returnsToggledReminderInData() throws Exception {
        Reminder saved = reminderRepository.save(new Reminder("우유 사기", null, null, null));

        mockMvc.perform(patch("/api/reminders/{id}/complete", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.completedAt").isString());
    }

    @Test
    @DisplayName("리마인더 삭제는 200과 함께 data가 null인 성공 응답을 반환한다")
    void deleteReminder_returnsSuccessWithNullData() throws Exception {
        Reminder saved = reminderRepository.save(new Reminder("우유 사기", null, null, null));

        mockMvc.perform(delete("/api/reminders/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("존재하지 않는 리마인더를 삭제하면 404와 NOT_FOUND 응답을 반환한다")
    void deleteReminder_returnsNotFound_whenReminderDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/reminders/{id}", -1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.resultMsg").value("Reminder not found: -1"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("제목 없이 생성하면 400과 BAD_REQUEST 응답에 필드 오류를 담는다")
    void createReminder_returnsBadRequest_whenTitleIsBlank() throws Exception {
        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.resultMsg").value(startsWith("title: ")));
    }

    @Test
    @DisplayName("지원하지 않는 스마트 뷰를 조회하면 400과 BAD_REQUEST 응답을 반환한다")
    void getSmartReminders_returnsBadRequest_whenViewIsUnknown() throws Exception {
        mockMvc.perform(get("/api/reminders/smart/{view}", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.resultMsg").value("Unknown smart view: unknown"));
    }

    @Test
    @DisplayName("본문을 읽을 수 없는 요청은 400과 BAD_REQUEST 응답을 반환한다")
    void createReminder_returnsBadRequest_whenBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.resultMsg").isString());
    }
}
