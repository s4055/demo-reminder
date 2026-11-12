package demo.ai.reminder.controller;

import demo.ai.reminder.domain.list.ReminderList;
import demo.ai.reminder.domain.reminder.Priority;
import demo.ai.reminder.domain.reminder.Recurrence;
import demo.ai.reminder.domain.reminder.Reminder;
import demo.ai.reminder.domain.reminder.RepeatRule;
import demo.ai.reminder.domain.user.User;
import demo.ai.reminder.repository.list.ReminderListRepository;
import demo.ai.reminder.repository.reminder.ReminderRepository;
import demo.ai.reminder.repository.user.UserRepository;
import demo.ai.reminder.support.TestAuth;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static demo.ai.reminder.support.TestTimes.SEOUL;
import static demo.ai.reminder.support.TestTimes.seoul;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
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

    private static final String UTC_ISO_INSTANT = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private ReminderListRepository reminderListRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;

    @BeforeEach
    void signIn() {
        owner = TestAuth.signIn(userRepository, "owner@example.com");
    }

    @Test
    @DisplayName("리마인더 목록 조회 응답은 resultCode, resultMsg와 data 배열을 포함한다")
    void getReminders_wrapsListInApiResponse() throws Exception {
        reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, null, null));

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
    @DisplayName("리마인더 수정은 200과 data가 null인 성공 응답을 반환하고, 변경된 priority는 목록에서 조회된다")
    void updateReminder_returnsNoData_andPriorityIsListed() throws Exception {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, null, null));

        mockMvc.perform(put("/api/reminders/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"우유 사기\",\"flagged\":false,\"priority\":\"LOW\",\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/reminders"))
                .andExpect(jsonPath("$.data[0].priority").value("LOW"));
    }

    @Test
    @DisplayName("리마인더 수정은 version이 필수이며, 생략하면 400이고 지난 버전이면 409와 VERSION_CONFLICT다")
    void updateReminder_requiresCurrentVersion() throws Exception {
        Reminder saved = reminderRepository.saveAndFlush(new Reminder(owner, "우유 사기", null, null, null, null, null, null));

        mockMvc.perform(put("/api/reminders/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"저지방 우유\",\"flagged\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
        mockMvc.perform(put("/api/reminders/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"저지방 우유\",\"flagged\":false,\"version\":0}"))
                .andExpect(status().isOk());
        entityManager.flush();
        mockMvc.perform(put("/api/reminders/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"두유\",\"flagged\":false,\"version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("VERSION_CONFLICT"));
        mockMvc.perform(get("/api/reminders"))
                .andExpect(jsonPath("$.data[0].title").value("저지방 우유"))
                .andExpect(jsonPath("$.data[0].version").value(1));
    }

    @Test
    @DisplayName("리마인더 목록 조회 응답의 각 항목은 priority를 포함한다")
    void getReminders_includesPriority() throws Exception {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, Priority.MEDIUM, null));

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
    @DisplayName("완료 토글은 200과 data가 null인 성공 응답을 반환하고, 완료 상태와 완료일시는 목록에서 조회된다")
    void toggleComplete_returnsNoData_andCompletionIsListed() throws Exception {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, null, null));

        mockMvc.perform(patch("/api/reminders/{id}/complete", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/reminders"))
                .andExpect(jsonPath("$.data[0].completed").value(true))
                .andExpect(jsonPath("$.data[0].completedAt").isString());
    }

    @Test
    @DisplayName("플래그 토글은 200과 data가 null인 성공 응답을 반환하고, 플래그는 목록에서 조회된다")
    void toggleFlag_returnsNoData_andFlagIsListed() throws Exception {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, null, null));

        mockMvc.perform(patch("/api/reminders/{id}/flag", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/reminders"))
                .andExpect(jsonPath("$.data[0].flagged").value(true));
    }

    @Test
    @DisplayName("반복을 지정해 생성하면 응답에 repeatRule이 담기고, 생략하면 NONE이다")
    void createReminder_returnsRepeatRule() throws Exception {
        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"분리수거\",\"dueAt\":\"2026-10-04T20:00:00+09:00\",\"timeZone\":\"Asia/Seoul\",\"repeatRule\":\"WEEKLY\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.repeatRule").value("WEEKLY"));

        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"우유 사기\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.repeatRule").value("NONE"));
    }

    @Test
    @DisplayName("마감일시 없이 반복을 지정해 생성하면 400을 반환한다")
    void createReminder_withRepeatButNoDueAt_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"분리수거\",\"repeatRule\":\"DAILY\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("간격과 요일을 지정해 생성하면 응답에 repeatInterval과 월요일부터 정렬된 repeatDaysOfWeek가 담긴다")
    void createReminder_returnsRepeatIntervalAndDaysOfWeek() throws Exception {
        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"운동","dueAt":"2026-10-05T07:00:00+09:00","timeZone":"Asia/Seoul","repeatRule":"WEEKLY",
                                 "repeatInterval":2,"repeatDaysOfWeek":["WEDNESDAY","MONDAY"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.repeatInterval").value(2))
                .andExpect(jsonPath("$.data.repeatDaysOfWeek[0]").value("MONDAY"))
                .andExpect(jsonPath("$.data.repeatDaysOfWeek[1]").value("WEDNESDAY"));

        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"우유 사기\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.repeatInterval").value(1))
                .andExpect(jsonPath("$.data.repeatDaysOfWeek", hasSize(0)));
    }

    @Test
    @DisplayName("간격이 1~99를 벗어나거나, WEEKLY가 아닌데 요일을 지정하거나, 잘못된 요일 값이면 400을 반환한다")
    void createReminder_withInvalidRecurrence_returnsBadRequest() throws Exception {
        String[] bodies = {
                "{\"title\":\"운동\",\"dueAt\":\"2026-10-05T07:00:00+09:00\",\"timeZone\":\"Asia/Seoul\",\"repeatRule\":\"DAILY\",\"repeatInterval\":0}",
                "{\"title\":\"운동\",\"dueAt\":\"2026-10-05T07:00:00+09:00\",\"timeZone\":\"Asia/Seoul\",\"repeatRule\":\"DAILY\",\"repeatInterval\":100}",
                "{\"title\":\"운동\",\"dueAt\":\"2026-10-05T07:00:00+09:00\",\"timeZone\":\"Asia/Seoul\",\"repeatRule\":\"DAILY\",\"repeatDaysOfWeek\":[\"MONDAY\"]}",
                "{\"title\":\"운동\",\"dueAt\":\"2026-10-05T07:00:00+09:00\",\"timeZone\":\"Asia/Seoul\",\"repeatRule\":\"WEEKLY\",\"repeatDaysOfWeek\":[\"MON\"]}",
        };
        for (String body : bodies) {
            mockMvc.perform(post("/api/reminders").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
        }
    }

    @Test
    @DisplayName("수정할 때도 간격이 1~99를 벗어나거나 WEEKLY가 아닌데 요일을 지정하면 400을 반환하고 바뀌지 않는다")
    void updateReminder_withInvalidRecurrence_returnsBadRequest() throws Exception {
        Reminder saved = reminderRepository.save(
                new Reminder(owner, "운동", null, null, seoul(2026, 10, 5, 7, 0), SEOUL, null, null));
        String[] bodies = {
                "{\"title\":\"운동\",\"dueAt\":\"2026-10-05T07:00:00+09:00\",\"timeZone\":\"Asia/Seoul\",\"repeatRule\":\"DAILY\",\"repeatInterval\":0,\"version\":0}",
                "{\"title\":\"운동\",\"dueAt\":\"2026-10-05T07:00:00+09:00\",\"timeZone\":\"Asia/Seoul\",\"repeatRule\":\"DAILY\",\"repeatInterval\":100,\"version\":0}",
                "{\"title\":\"운동\",\"dueAt\":\"2026-10-05T07:00:00+09:00\",\"timeZone\":\"Asia/Seoul\",\"repeatRule\":\"DAILY\",\"repeatDaysOfWeek\":[\"MONDAY\"],\"version\":0}",
        };
        for (String body : bodies) {
            mockMvc.perform(put("/api/reminders/{id}", saved.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
        }
        assertThat(reminderRepository.findById(saved.getId()).orElseThrow().getRecurrence().isRepeating()).isFalse();
    }

    @Test
    @DisplayName("지원하지 않는 repeatRule 값으로 생성하면 400을 반환한다")
    void createReminder_withUnknownRepeatRule_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"분리수거\",\"dueAt\":\"2026-10-04T20:00:00+09:00\",\"timeZone\":\"Asia/Seoul\",\"repeatRule\":\"HOURLY\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("반복 리마인더를 완료하면 data 없이 응답하고, 다음 회차가 조회된다")
    void toggleComplete_whenRepeating_returnsNoData_andNextOccurrenceIsListed() throws Exception {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        Reminder saved = reminderRepository.save(new Reminder(owner, "분리수거", null, home,
                seoul(2026, 10, 4, 20, 0), SEOUL, null, new Recurrence(RepeatRule.WEEKLY, 1, null)));

        mockMvc.perform(patch("/api/reminders/{id}/complete", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/reminders").param("listId", String.valueOf(home.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[1].completed").value(false))
                .andExpect(jsonPath("$.data[1].dueAt").value("2026-10-11T11:00:00Z"))
                .andExpect(jsonPath("$.data[1].repeatRule").value("WEEKLY"));
    }

    @Test
    @DisplayName("다가오는 리마인더 조회는 기간 안에 마감되는 미완료 리마인더를 data에 담는다")
    void getUpcomingReminders_returnsRemindersDueInRange() throws Exception {
        reminderRepository.save(new Reminder(owner, "회의", null, null, seoul(2026, 10, 4, 21, 1), SEOUL, null, null));
        reminderRepository.save(new Reminder(owner, "내일 회의", null, null, seoul(2026, 10, 5, 21, 1), SEOUL, null, null));

        mockMvc.perform(get("/api/reminders/upcoming")
                        .param("from", "2026-10-04T12:00:00Z")
                        .param("to", "2026-10-04T12:02:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("회의"))
                .andExpect(jsonPath("$.data[0].dueAt").value("2026-10-04T12:01:00Z"));
    }

    @Test
    @DisplayName("다가오는 리마인더 조회에서 from/to가 없거나 형식이 잘못되면 400을 반환한다")
    void getUpcomingReminders_withMissingOrInvalidRange_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reminders/upcoming").param("from", "2026-10-04T12:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/reminders/upcoming")
                        .param("from", "not-a-date")
                        .param("to", "2026-10-04T12:02:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("다가오는 리마인더 조회에서 from이 to보다 늦으면 400을 반환한다")
    void getUpcomingReminders_withFromAfterTo_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reminders/upcoming")
                        .param("from", "2026-10-04T12:02:00Z")
                        .param("to", "2026-10-04T12:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("리마인더 삭제는 200과 함께 data가 null인 성공 응답을 반환한다")
    void deleteReminder_returnsSuccessWithNullData() throws Exception {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, null, null));

        mockMvc.perform(delete("/api/reminders/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("리마인더 검색은 200과 함께 일치하는 리마인더 목록을 반환한다")
    void searchReminders_returnsMatchingReminders() throws Exception {
        reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, null, null));
        reminderRepository.save(new Reminder(owner, "보고서", null, null, null, null, null, null));

        mockMvc.perform(get("/api/reminders/search").param("q", "우유"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("우유 사기"));
    }

    @Test
    @DisplayName("리마인더 검색어와 태그 조회의 tag는 앞뒤 공백을 지우고 찾는다")
    void searchAndTagQuery_stripParameters() throws Exception {
        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"우유 사기\",\"tagNames\":[\"장보기\"]}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reminders/search").param("q", "  우유 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].title").value(contains("우유 사기")));
        mockMvc.perform(get("/api/reminders").param("tag", " 장보기 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].title").value(contains("우유 사기")));
    }

    @Test
    @DisplayName("리마인더를 만들거나 수정할 때 제목과 태그 이름의 앞뒤 공백은 지운다")
    void createAndUpdateReminder_stripTitleAndTagNames() throws Exception {
        Reminder reminder = reminderRepository.saveAndFlush(new Reminder(owner, "빨래", null, null, null, null, null, null));

        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  우유 사기  \",\"tagNames\":[\" 장보기 \"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("우유 사기"))
                .andExpect(jsonPath("$.data.tags").value(contains("장보기")));

        mockMvc.perform(put("/api/reminders/" + reminder.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" 청소 \",\"flagged\":false,\"tagNames\":[\" 집안일 \"],\"version\":"
                                + reminder.getVersion() + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reminders"))
                .andExpect(jsonPath("$.data[?(@.id == " + reminder.getId() + ")].title").value(contains("청소")))
                .andExpect(jsonPath("$.data[?(@.id == " + reminder.getId() + ")].tags[*]").value(contains("집안일")));
    }

    @Test
    @DisplayName("리마인더 검색에서 q가 없거나 비어 있으면 400을 반환한다")
    void searchReminders_withMissingOrBlankQuery_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reminders/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/reminders/search").param("q", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.resultMsg").value(startsWith("q: ")));
    }

    @Test
    @DisplayName("리마인더 검색어는 100자까지 허용하고, 넘으면 400을 반환한다")
    void searchReminders_withQueryOverMaxLength_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reminders/search").param("q", "가".repeat(100)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/reminders/search").param("q", "가".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.resultMsg").value(startsWith("q: ")));
    }

    @Test
    @DisplayName("완료 항목 일괄 삭제는 200과 함께 삭제 개수를 반환한다")
    void deleteCompletedReminders_returnsDeletedCount() throws Exception {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        Reminder done = reminderRepository.save(new Reminder(owner, "빨래", null, home, null, null, null, null));
        reminderRepository.save(new Reminder(owner, "청소", null, home, null, null, null, null));
        mockMvc.perform(patch("/api/reminders/{id}/complete", done.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/reminders/completed").param("listId", String.valueOf(home.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data.deletedCount").value(1));

        mockMvc.perform(get("/api/reminders").param("listId", String.valueOf(home.getId())))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("청소"));
    }

    @Test
    @DisplayName("완료 항목 일괄 삭제에서 listId가 없으면 400을 반환한다")
    void deleteCompletedReminders_withoutListId_returnsBadRequest() throws Exception {
        mockMvc.perform(delete("/api/reminders/completed"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("리마인더 이동은 200과 data가 null인 성공 응답을 반환하고, 대상 리스트에서 조회된다")
    void moveReminder_returnsNoData_andReminderIsListedInTarget() throws Exception {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        ReminderList work = reminderListRepository.save(new ReminderList(owner, "업무", null, null, 0));
        Reminder saved = reminderRepository.save(new Reminder(owner, "보고서", null, home, null, null, null, null));

        mockMvc.perform(patch("/api/reminders/{id}/list", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"listId\":" + work.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/reminders").param("listId", String.valueOf(work.getId())))
                .andExpect(jsonPath("$.data[0].id").value(saved.getId()))
                .andExpect(jsonPath("$.data[0].listId").value(work.getId()));
    }

    @Test
    @DisplayName("리마인더 이동에서 listId가 없으면 400을 반환한다")
    void moveReminder_withoutListId_returnsBadRequest() throws Exception {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        Reminder saved = reminderRepository.save(new Reminder(owner, "보고서", null, home, null, null, null, null));

        mockMvc.perform(patch("/api/reminders/{id}/list", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("리마인더 단건 조회 응답은 data에 하위 작업을 포함한 리마인더를 담는다")
    void getReminder_returnsReminderWithSubtasks() throws Exception {
        Reminder parent = new Reminder(owner, "우유 사기", null, null, null, null, null, null);
        parent.addSubtask(new Reminder(owner, "저지방", null, null, null, null, null, null));
        reminderRepository.save(parent);

        mockMvc.perform(get("/api/reminders/{id}", parent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(parent.getId()))
                .andExpect(jsonPath("$.data.title").value("우유 사기"))
                .andExpect(jsonPath("$.data.version").isNumber())
                .andExpect(jsonPath("$.data.subtasks[*].title").value(contains("저지방")));
    }

    @Test
    @DisplayName("다른 사용자의 리마인더를 단건 조회하면 404와 NOT_FOUND 응답을 반환한다")
    void getReminder_returnsNotFound_forOtherUsersReminder() throws Exception {
        User other = userRepository.save(new User("other@example.com", "{noop}password", "다른 사람"));
        Reminder othersReminder = reminderRepository.save(new Reminder(other, "비밀", null, null, null, null, null, null));

        mockMvc.perform(get("/api/reminders/{id}", othersReminder.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("숫자가 아닌 id로 단건 조회하면 400과 BAD_REQUEST 응답을 반환한다")
    void getReminder_returnsBadRequest_forNonNumericId() throws Exception {
        mockMvc.perform(get("/api/reminders/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
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
    @DisplayName("스마트 뷰 이름은 대소문자를 구분하지 않는다")
    void getSmartReminders_isCaseInsensitive() throws Exception {
        reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, null, null));

        for (String view : new String[]{"all", "ALL", "All"}) {
            mockMvc.perform(get("/api/reminders/smart/{view}", view))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].title").value("우유 사기"));
        }
    }

    @Test
    @DisplayName("지원하지 않는 스마트 뷰를 조회하면 400과 BAD_REQUEST 응답을 반환한다")
    void getSmartReminders_returnsBadRequest_whenViewIsUnknown() throws Exception {
        mockMvc.perform(get("/api/reminders/smart/{view}", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("오프셋이 있는 마감 시각과 시간대로 생성하면 응답 시각은 Z가 붙은 UTC ISO-8601이고 시간대가 담긴다")
    void createReminder_withOffsetDueAt_returnsUtcInstantAndTimeZone() throws Exception {
        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"회의","dueAt":"2026-10-05T07:00:00+09:00","timeZone":"Asia/Seoul"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dueAt").value("2026-10-04T22:00:00Z"))
                .andExpect(jsonPath("$.data.timeZone").value("Asia/Seoul"))
                .andExpect(jsonPath("$.data.createdAt").value(matchesPattern(UTC_ISO_INSTANT)))
                .andExpect(jsonPath("$.data.updatedAt").value(matchesPattern(UTC_ISO_INSTANT)));
    }

    @Test
    @DisplayName("마감 시각에 오프셋이 없거나, 시간대 ID가 잘못되었거나, 마감 시각만 있고 시간대가 없으면 400을 반환한다")
    void createReminder_withInvalidDueAtOrTimeZone_returnsBadRequest() throws Exception {
        String[] bodies = {
                "{\"title\":\"회의\",\"dueAt\":\"2026-10-05T07:00:00\",\"timeZone\":\"Asia/Seoul\"}",
                "{\"title\":\"회의\",\"dueAt\":\"2026-10-05T07:00:00+09:00\",\"timeZone\":\"Asia/Nowhere\"}",
                "{\"title\":\"회의\",\"dueAt\":\"2026-10-05T07:00:00+09:00\",\"timeZone\":\"not a zone\"}",
                "{\"title\":\"회의\",\"dueAt\":\"2026-10-05T07:00:00+09:00\"}",
        };
        for (String body : bodies) {
            mockMvc.perform(post("/api/reminders").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
        }
        assertThat(reminderRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("수정할 때 시간대 ID가 잘못되면 400을 반환하고 바뀌지 않는다")
    void updateReminder_withInvalidTimeZone_returnsBadRequest() throws Exception {
        Reminder saved = reminderRepository.save(
                new Reminder(owner, "회의", null, null, seoul(2026, 10, 5, 7, 0), SEOUL, null, null));

        mockMvc.perform(put("/api/reminders/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"회의","dueAt":"2026-10-05T07:00:00Z","timeZone":"Mars/Base","version":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));

        assertThat(reminderRepository.findById(saved.getId()).orElseThrow().getTimeZone()).isEqualTo(SEOUL);
    }

    @Test
    @DisplayName("today 뷰는 zone 파라미터로 받은 시간대의 오늘을 조회한다")
    void getSmartReminders_today_usesZoneParameter() throws Exception {
        reminderRepository.save(new Reminder(owner, "오늘", null, null, Instant.now(), SEOUL, null, null));

        mockMvc.perform(get("/api/reminders/smart/{view}", "TODAY").param("zone", "Asia/Seoul"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].dueAt").value(matchesPattern(UTC_ISO_INSTANT)));
    }

    @Test
    @DisplayName("today 뷰에 zone이 없거나 잘못된 시간대면 400을 반환한다")
    void getSmartReminders_today_withMissingOrInvalidZone_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reminders/smart/{view}", "TODAY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/reminders/smart/{view}", "TODAY").param("zone", "Asia/Nowhere"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("리마인더 순서 변경은 200 성공 응답을 반환하고, 리스트별 조회에 반영된다")
    void reorderReminders_returnsSuccess_andIsReflected() throws Exception {
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null, null, 0));
        Reminder milk = reminderRepository.save(new Reminder(owner, "우유", null, shopping, null, null, null, null));
        Reminder eggs = reminderRepository.save(new Reminder(owner, "계란", null, shopping, null, null, null, null));

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
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null, null, 0));
        Reminder milk = reminderRepository.save(new Reminder(owner, "우유", null, shopping, null, null, null, null));
        reminderRepository.save(new Reminder(owner, "계란", null, shopping, null, null, null, null));

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
    @DisplayName("리마인더를 수정하면 교체된 tags가 목록에서 조회된다")
    void updateReminder_replacesTagsInList() throws Exception {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, null, null));

        mockMvc.perform(put("/api/reminders/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"우유 사기\",\"flagged\":false,\"tagNames\":[\"급함\"],\"version\":0}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reminders"))
                .andExpect(jsonPath("$.data[0].tags", contains("급함")));
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
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        Reminder parent = reminderRepository.save(new Reminder(owner, "이사 준비", null, home, null, null, null, null));

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
        Reminder parent = new Reminder(owner, "이사 준비", null, null, null, null, null, null);
        Reminder subtask = new Reminder(owner, "박스 구하기", null, null, null, null, null, null);
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
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        Reminder parent = new Reminder(owner, "이사 준비", null, home, null, null, null, null);
        parent.addSubtask(new Reminder(owner, "박스 구하기", null, null, null, null, null, null));
        parent.addSubtask(new Reminder(owner, "이삿짐센터 예약", null, null, null, null, null, null));
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

    @Test
    @DisplayName("리마인더 제목은 200자, 메모는 255자까지 허용하고, 넘으면 400과 필드 오류를 반환한다")
    void createReminder_withTitleOrMemoOverMaxLength_returnsBadRequest() throws Exception {
        String maxTitle = "가".repeat(Reminder.TITLE_MAX_LENGTH);
        String maxMemo = "나".repeat(Reminder.MEMO_MAX_LENGTH);

        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + maxTitle + "\",\"memo\":\"" + maxMemo + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + maxTitle + "가\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.resultMsg").value(startsWith("title: ")));
        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"우유\",\"memo\":\"" + maxMemo + "나\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.resultMsg").value(startsWith("memo: ")));
    }

    @Test
    @DisplayName("리마인더를 수정할 때도 메모가 255자를 넘으면 400과 memo 필드 오류를 반환한다")
    void updateReminder_withMemoOverMaxLength_returnsBadRequest() throws Exception {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, null, null));
        String longMemo = "나".repeat(Reminder.MEMO_MAX_LENGTH + 1);

        mockMvc.perform(put("/api/reminders/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"우유 사기\",\"memo\":\"" + longMemo + "\",\"flagged\":false,\"version\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.resultMsg").value(startsWith("memo: ")));
    }
}
