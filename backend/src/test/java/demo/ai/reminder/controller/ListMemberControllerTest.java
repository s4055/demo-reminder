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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ListMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReminderListRepository reminderListRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User bob;
    private ReminderList list;

    @BeforeEach
    void setUp() {
        bob = userRepository.save(new User("bob@example.com", "{noop}password", "밥"));
        owner = TestAuth.signIn(userRepository, "owner@example.com");
        list = reminderListRepository.save(new ReminderList(owner, "장보기", null));
    }

    @Test
    @DisplayName("초대하면 201과 멤버 정보를 반환하고, 멤버 목록은 소유자부터 반환한다")
    void invite_returnsCreatedMember_andMembersAreListed() throws Exception {
        invite("bob@example.com")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data.userId").value(bob.getId()))
                .andExpect(jsonPath("$.data.email").value("bob@example.com"))
                .andExpect(jsonPath("$.data.name").value("밥"))
                .andExpect(jsonPath("$.data.role").value("EDITOR"))
                .andExpect(jsonPath("$.data.joinedAt").isNotEmpty());

        mockMvc.perform(get("/api/lists/{listId}/members", list.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].userId").value(owner.getId()))
                .andExpect(jsonPath("$.data[0].role").value("OWNER"))
                .andExpect(jsonPath("$.data[1].userId").value(bob.getId()))
                .andExpect(jsonPath("$.data[1].role").value("EDITOR"));
    }

    @Test
    @DisplayName("공유하면 리스트 목록 응답의 memberCount가 늘고, 초대받은 사용자에게는 role이 EDITOR로 나온다")
    void listsResponse_includesRoleAndMemberCount() throws Exception {
        invite("bob@example.com").andExpect(status().isCreated());

        mockMvc.perform(get("/api/lists"))
                .andExpect(jsonPath("$.data[0].role").value("OWNER"))
                .andExpect(jsonPath("$.data[0].memberCount").value(2));

        TestAuth.signIn(bob);
        mockMvc.perform(get("/api/lists"))
                .andExpect(jsonPath("$.data[0].id").value(list.getId()))
                .andExpect(jsonPath("$.data[0].role").value("EDITOR"))
                .andExpect(jsonPath("$.data[0].memberCount").value(2));
    }

    @Test
    @DisplayName("이메일 형식이 아니면 400, 없는 사용자면 404, 이미 멤버면 400을 반환한다")
    void invite_withInvalidTarget_returnsError() throws Exception {
        invite("not-an-email")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
        invite("nobody@example.com")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("NOT_FOUND"));
        invite("owner@example.com")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("편집자가 초대하거나 리스트를 수정/삭제하면 403을 반환한다")
    void editor_isForbiddenFromOwnerActions() throws Exception {
        invite("bob@example.com").andExpect(status().isCreated());
        TestAuth.signIn(bob);

        invite("owner@example.com")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("FORBIDDEN"));
        mockMvc.perform(put("/api/lists/{id}", list.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"바꿈\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("FORBIDDEN"));
        mockMvc.perform(delete("/api/lists/{id}", list.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("멤버 제거와 나가기는 200을 반환하고, 소유자가 나가려 하면 400을 반환한다")
    void removeMember_andLeave() throws Exception {
        invite("bob@example.com").andExpect(status().isCreated());

        mockMvc.perform(delete("/api/lists/{listId}/members/{userId}", list.getId(), owner.getId()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/lists/{listId}/members/{userId}", list.getId(), bob.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"));

        invite("bob@example.com").andExpect(status().isCreated());
        TestAuth.signIn(bob);
        mockMvc.perform(delete("/api/lists/{listId}/members/{userId}", list.getId(), bob.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/lists/{listId}/members", list.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("멤버가 아닌 사용자는 멤버 목록 조회도 404다")
    void getMembers_byNonMember_returnsNotFound() throws Exception {
        TestAuth.signIn(bob);

        mockMvc.perform(get("/api/lists/{listId}/members", list.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("NOT_FOUND"));
    }

    private ResultActions invite(String email) throws Exception {
        return mockMvc.perform(post("/api/lists/{listId}/members", list.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"));
    }
}
