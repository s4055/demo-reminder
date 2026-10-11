package demo.ai.reminder.controller;

import demo.ai.reminder.domain.ListGroup;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.repository.ListGroupRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
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
class ListGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ListGroupRepository listGroupRepository;

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
    @DisplayName("그룹 생성은 201, 목록/이름 변경/순서 변경/삭제는 200과 ApiResponse로 응답한다")
    void groupCrud_returnsApiResponses() throws Exception {
        mockMvc.perform(post("/api/list-groups").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"생활\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("생활"))
                .andExpect(jsonPath("$.data.sortOrder").value(0));
        Long id = listGroupRepository.findByUserId(owner.getId()).getFirst().getId();

        mockMvc.perform(get("/api/list-groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(id));
        mockMvc.perform(put("/api/list-groups/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"집안일\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("집안일"));
        mockMvc.perform(patch("/api/list-groups/order").contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[" + id + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"));
        mockMvc.perform(delete("/api/list-groups/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("이름 없이 그룹을 만들거나 순서 변경 ids가 맞지 않으면 400이다")
    void invalidRequests_returnBadRequest() throws Exception {
        mockMvc.perform(post("/api/list-groups").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
        mockMvc.perform(patch("/api/list-groups/order").contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[-1]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("리스트 그룹 지정은 groupId를 담아 응답하고, groupId가 null이면 그룹에서 뺀다")
    void changeGroup_assignsAndClears() throws Exception {
        ListGroup group = listGroupRepository.save(new ListGroup(owner, "생활", 0));
        ReminderList list = reminderListRepository.save(new ReminderList(owner, "장보기", null));

        mockMvc.perform(patch("/api/lists/{id}/group", list.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"groupId\":" + group.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupId").value(group.getId()));
        mockMvc.perform(patch("/api/lists/{id}/group", list.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"groupId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupId").value(nullValue()));
    }

    @Test
    @DisplayName("없는 그룹을 지정하면 404다")
    void changeGroup_withMissingGroup_returnsNotFound() throws Exception {
        ReminderList list = reminderListRepository.save(new ReminderList(owner, "장보기", null));

        mockMvc.perform(patch("/api/lists/{id}/group", list.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"groupId\":-1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("NOT_FOUND"));
    }
}
