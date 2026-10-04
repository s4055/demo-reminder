package demo.ai.reminder.controller;

import demo.ai.reminder.domain.Priority;
import demo.ai.reminder.domain.Reminder;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.repository.ReminderListRepository;
import demo.ai.reminder.repository.ReminderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
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

    @Autowired
    private ReminderListRepository reminderListRepository;

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
    @DisplayName("리마인더 순서 변경은 200 성공 응답을 반환하고, 리스트별 조회에 반영된다")
    void reorderReminders_returnsSuccess_andIsReflected() throws Exception {
        ReminderList shopping = reminderListRepository.save(new ReminderList("장보기", null));
        Reminder milk = reminderRepository.save(new Reminder("우유", null, shopping, null));
        Reminder eggs = reminderRepository.save(new Reminder("계란", null, shopping, null));

        mockMvc.perform(patch("/api/reminders/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"listId\":%d,\"ids\":[%d,%d]}".formatted(shopping.getId(), eggs.getId(), milk.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/reminders").param("listId", shopping.getId().toString()))
                .andExpect(jsonPath("$.data[*].title", contains("계란", "우유")))
                .andExpect(jsonPath("$.data[*].sortOrder", contains(0, 1)));
    }

    @Test
    @DisplayName("리마인더 순서 변경 ids가 리스트 항목과 일치하지 않으면 400과 BAD_REQUEST 응답을 반환한다")
    void reorderReminders_returnsBadRequest_whenIdsDoNotMatch() throws Exception {
        ReminderList shopping = reminderListRepository.save(new ReminderList("장보기", null));
        Reminder milk = reminderRepository.save(new Reminder("우유", null, shopping, null));
        reminderRepository.save(new Reminder("계란", null, shopping, null));

        mockMvc.perform(patch("/api/reminders/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"listId\":%d,\"ids\":[%d]}".formatted(shopping.getId(), milk.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("리마인더 순서 변경에 listId가 없으면 400과 BAD_REQUEST 응답을 반환한다")
    void reorderReminders_returnsBadRequest_whenListIdIsMissing() throws Exception {
        mockMvc.perform(patch("/api/reminders/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultMsg").value(startsWith("listId: ")));
    }

    @Test
    @DisplayName("태그 이름을 담아 생성하면 응답의 tags에 태그 이름이 이름순으로 담긴다")
    void createReminder_returnsTagNames() throws Exception {
        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"우유 사기\",\"tagNames\":[\"집\",\"심부름\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tags", contains("심부름", "집")));
    }

    @Test
    @DisplayName("태그 없이 생성하면 응답의 tags는 빈 배열이다")
    void createReminder_returnsEmptyTags_whenTagNamesAreOmitted() throws Exception {
        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"우유 사기\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tags", hasSize(0)));
    }

    @Test
    @DisplayName("빈 태그 이름으로 생성하면 400과 BAD_REQUEST 응답을 반환한다")
    void createReminder_returnsBadRequest_whenTagNameIsBlank() throws Exception {
        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"우유 사기\",\"tagNames\":[\" \"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("리마인더 수정 응답은 교체된 tags를 담는다")
    void updateReminder_returnsReplacedTags() throws Exception {
        Reminder saved = reminderRepository.save(new Reminder("우유 사기", null, null, null));

        mockMvc.perform(put("/api/reminders/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"우유 사기\",\"flagged\":false,\"tagNames\":[\"급함\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tags", contains("급함")));
    }

    @Test
    @DisplayName("tag 파라미터로 조회하면 해당 태그가 붙은 리마인더만 반환한다")
    void getReminders_byTag_returnsTaggedReminders() throws Exception {
        mockMvc.perform(post("/api/reminders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"우유 사기\",\"tagNames\":[\"집\"]}"));
        mockMvc.perform(post("/api/reminders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"보고서\",\"tagNames\":[\"회사\"]}"));

        mockMvc.perform(get("/api/reminders").param("tag", "집"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].title", contains("우유 사기")));
    }

    @Test
    @DisplayName("parentId를 담아 생성하면 응답의 parentId에 부모 id가 담기고 listId는 부모의 리스트를 따른다")
    void createReminder_withParentId_returnsSubtask() throws Exception {
        ReminderList home = reminderListRepository.save(new ReminderList("집", null));
        Reminder parent = reminderRepository.save(new Reminder("이사 준비", null, home, null));

        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"박스 구하기\",\"parentId\":" + parent.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.parentId").value(parent.getId()))
                .andExpect(jsonPath("$.data.listId").value(home.getId()))
                .andExpect(jsonPath("$.data.subtasks", hasSize(0)));
    }

    @Test
    @DisplayName("최상위 리마인더의 응답은 parentId가 null이고 subtasks는 빈 배열이다")
    void createReminder_returnsNullParentId_andEmptySubtasks() throws Exception {
        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"이사 준비\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.parentId").isEmpty())
                .andExpect(jsonPath("$.data.subtasks", hasSize(0)));
    }

    @Test
    @DisplayName("존재하지 않는 부모로 하위 작업을 생성하면 404와 NOT_FOUND 응답을 반환한다")
    void createReminder_returnsNotFound_whenParentDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"박스 구하기\",\"parentId\":-1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("하위 작업 아래에 하위 작업을 생성하면 400과 BAD_REQUEST 응답을 반환한다")
    void createReminder_returnsBadRequest_whenParentIsSubtask() throws Exception {
        Reminder parent = new Reminder("이사 준비", null, null, null);
        Reminder subtask = new Reminder("박스 구하기", null, null, null);
        parent.addSubtask(subtask);
        reminderRepository.save(parent);

        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"테이프 사기\",\"parentId\":" + subtask.getId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("리스트별 조회 응답은 최상위 리마인더만 담고, 하위 작업은 각 항목의 subtasks에 담는다")
    void getReminders_byList_nestsSubtasks() throws Exception {
        ReminderList home = reminderListRepository.save(new ReminderList("집", null));
        Reminder parent = new Reminder("이사 준비", null, home, null);
        parent.addSubtask(new Reminder("박스 구하기", null, null, null));
        parent.addSubtask(new Reminder("이삿짐센터 예약", null, null, null));
        reminderRepository.save(parent);

        mockMvc.perform(get("/api/reminders").param("listId", home.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].title", contains("이사 준비")))
                .andExpect(jsonPath("$.data[0].subtasks[*].title", contains("박스 구하기", "이삿짐센터 예약")))
                .andExpect(jsonPath("$.data[0].subtasks[0].parentId").value(parent.getId()));
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
