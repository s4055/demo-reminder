package demo.ai.reminder.controller;

import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.repository.ReminderListRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    @DisplayName("리스트 목록 조회 응답은 resultCode, resultMsg와 data 배열을 포함한다")
    void getLists_wrapsListInApiResponse() throws Exception {
        reminderListRepository.save(new ReminderList("장보기", null));

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
        ReminderList saved = reminderListRepository.save(new ReminderList("장보기", null));

        mockMvc.perform(delete("/api/lists/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드로 요청하면 405와 METHOD_NOT_ALLOWED 응답을 반환한다")
    void unsupportedMethod_returnsMethodNotAllowed() throws Exception {
        mockMvc.perform(put("/api/lists"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.resultCode").value("METHOD_NOT_ALLOWED"));
    }
}
