package demo.ai.reminder.controller;

import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.repository.ReminderListRepository;
import demo.ai.reminder.repository.UserRepository;
import demo.ai.reminder.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
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
class ReminderListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReminderListRepository reminderListRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;

    @BeforeEach
    void signIn() {
        owner = TestAuth.signIn(userRepository, "owner@example.com");
    }

    @Test
    @DisplayName("리스트 목록 조회 응답은 resultCode, resultMsg와 data 배열을 포함한다")
    void getLists_wrapsListInApiResponse() throws Exception {
        reminderListRepository.save(new ReminderList(owner, "장보기", null));

        mockMvc.perform(get("/api/lists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.resultMsg").value("성공"))
                .andExpect(jsonPath("$.data[*].name").value(hasItem("장보기")));
    }

    @Test
    @DisplayName("리스트 생성 응답은 201과 함께 생성된 리스트를 data에 담는다")
    void createList_returnsCreatedListInData() throws Exception {
        mockMvc.perform(post("/api/lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"장보기\",\"color\":\"#FF9500\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("장보기"))
                .andExpect(jsonPath("$.data.reminderCount").value(0));
    }

    @Test
    @DisplayName("존재하지 않는 리스트를 수정하면 404와 NOT_FOUND 응답을 반환한다")
    void updateList_returnsNotFound_whenListDoesNotExist() throws Exception {
        mockMvc.perform(put("/api/lists/{id}", -1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"업무\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.resultMsg").value("List not found: -1"));
    }

    @Test
    @DisplayName("리스트 삭제는 200과 함께 data가 null인 성공 응답을 반환한다")
    void deleteList_returnsSuccessWithNullData() throws Exception {
        ReminderList saved = reminderListRepository.save(new ReminderList(owner, "장보기", null));

        mockMvc.perform(delete("/api/lists/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("리스트 순서 변경은 200 성공 응답을 반환하고, 리스트 목록이 바뀐 순서와 sortOrder로 조회된다")
    void reorderLists_returnsSuccess_andIsReflected() throws Exception {
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null));
        ReminderList work = reminderListRepository.save(new ReminderList(owner, "업무", null));
        List<Long> ids = new ArrayList<>(List.of(work.getId(), shopping.getId()));
        reminderListRepository.findAll().stream()
                .map(ReminderList::getId)
                .filter(id -> !ids.contains(id))
                .forEach(ids::add);

        mockMvc.perform(patch("/api/lists/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":" + ids + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"));

        mockMvc.perform(get("/api/lists"))
                .andExpect(jsonPath("$.data[0].name").value("업무"))
                .andExpect(jsonPath("$.data[0].sortOrder").value(0))
                .andExpect(jsonPath("$.data[1].name").value("장보기"))
                .andExpect(jsonPath("$.data[1].sortOrder").value(1));
    }

    @Test
    @DisplayName("리스트 순서 변경 ids가 전체 리스트와 일치하지 않으면 400과 BAD_REQUEST 응답을 반환한다")
    void reorderLists_returnsBadRequest_whenIdsDoNotMatch() throws Exception {
        reminderListRepository.save(new ReminderList(owner, "장보기", null));

        mockMvc.perform(patch("/api/lists/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[-1]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드로 요청하면 405와 METHOD_NOT_ALLOWED 응답을 반환한다")
    void unsupportedMethod_returnsMethodNotAllowed() throws Exception {
        mockMvc.perform(put("/api/lists"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.resultCode").value("METHOD_NOT_ALLOWED"));
    }
}
