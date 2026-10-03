package demo.ai.reminder.controller;

import demo.ai.reminder.domain.Reminder;
import demo.ai.reminder.domain.Tag;
import demo.ai.reminder.repository.ReminderRepository;
import demo.ai.reminder.repository.TagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Test
    @DisplayName("태그 목록 조회 응답은 태그 이름과 미완료 리마인더 개수를 data에 담는다")
    void getTags_returnsTagsWithReminderCount() throws Exception {
        Tag home = tagRepository.save(new Tag("집"));
        Reminder reminder = new Reminder("우유 사기", null, null, null);
        reminder.replaceTags(List.of(home));
        reminderRepository.save(reminder);

        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].id").value(home.getId()))
                .andExpect(jsonPath("$.data[0].name").value("집"))
                .andExpect(jsonPath("$.data[0].reminderCount").value(1));
    }

    @Test
    @DisplayName("태그 삭제는 200과 함께 data가 null인 성공 응답을 반환한다")
    void deleteTag_returnsSuccessWithNullData() throws Exception {
        Tag home = tagRepository.save(new Tag("집"));

        mockMvc.perform(delete("/api/tags/{id}", home.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("존재하지 않는 태그를 삭제하면 404와 NOT_FOUND 응답을 반환한다")
    void deleteTag_returnsNotFound_whenTagDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/tags/{id}", -1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.resultMsg").value("Tag not found: -1"));
    }
}
