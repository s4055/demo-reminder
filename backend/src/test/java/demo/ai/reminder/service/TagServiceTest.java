package demo.ai.reminder.service;

import demo.ai.reminder.common.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.Reminder;
import demo.ai.reminder.domain.Tag;
import demo.ai.reminder.repository.ReminderRepository;
import demo.ai.reminder.repository.TagRepository;
import demo.ai.reminder.repository.TagSummary;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TagServiceTest {

    @Autowired
    private TagService tagService;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("태그 목록은 이름순이고, 태그별 미완료 리마인더 개수를 포함하며, 사용되지 않는 태그는 제외한다")
    void getTags_countsIncompleteReminders_andExcludesUnusedTags() {
        Tag home = tagRepository.save(new Tag("집"));
        Tag work = tagRepository.save(new Tag("회사"));
        tagRepository.save(new Tag("안 쓰는 태그"));
        reminder("우유 사기", home);
        reminder("빨래", home);
        Reminder done = reminder("청소", home);
        done.toggleComplete(LocalDateTime.now());
        Reminder doneAtWork = reminder("보고서", work);
        doneAtWork.toggleComplete(LocalDateTime.now());

        List<TagSummary> result = tagService.getTags();

        assertThat(result).extracting(summary -> summary.tag().getName()).containsExactly("집", "회사");
        assertThat(result).extracting(TagSummary::reminderCount).containsExactly(2L, 0L);
    }

    @Test
    @DisplayName("없는 태그는 새로 만들고, 있는 태그는 재사용한다")
    void resolveTags_createsMissingTags_andReusesExisting() {
        Tag home = tagRepository.save(new Tag("집"));

        List<Tag> result = tagService.resolveTags(List.of("집", "심부름"));

        assertThat(result).extracting(Tag::getName).containsExactly("집", "심부름");
        assertThat(result.getFirst().getId()).isEqualTo(home.getId());
        assertThat(result.get(1).getId()).isNotNull();
        assertThat(tagRepository.findByNameIn(List.of("집", "심부름"))).hasSize(2);
    }

    @Test
    @DisplayName("태그 이름의 앞뒤 공백은 제거하고 중복은 하나로 합치며 빈 이름은 버린다")
    void resolveTags_trimsAndDeduplicatesNames() {
        List<Tag> result = tagService.resolveTags(Arrays.asList(" 집 ", "집", "", null, "심부름"));

        assertThat(result).extracting(Tag::getName).containsExactly("집", "심부름");
    }

    @Test
    @DisplayName("태그 이름이 null이면 빈 태그 목록으로 본다")
    void resolveTags_returnsEmpty_whenNamesAreNull() {
        assertThat(tagService.resolveTags(null)).isEmpty();
    }

    @Test
    @DisplayName("태그를 삭제하면 리마인더에서 떨어지고 리마인더 자체는 유지된다")
    void deleteTag_detachesFromReminders_andKeepsReminders() {
        Tag home = tagRepository.save(new Tag("집"));
        Tag errand = tagRepository.save(new Tag("심부름"));
        Reminder milk = reminder("우유 사기", home, errand);
        entityManager.flush();
        entityManager.clear();

        tagService.deleteTag(home.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(tagRepository.findById(home.getId())).isEmpty();
        Reminder result = reminderRepository.findById(milk.getId()).orElseThrow();
        assertThat(result.getTags()).extracting(Tag::getName).containsExactly("심부름");
    }

    @Test
    @DisplayName("존재하지 않는 태그를 삭제하면 404 예외가 발생한다")
    void deleteTag_throwsNotFound_whenTagDoesNotExist() {
        assertThatThrownBy(() -> tagService.deleteTag(-1L))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
    }

    private Reminder reminder(String title, Tag... tags) {
        Reminder reminder = new Reminder(title, null, null, null);
        reminder.replaceTags(List.of(tags));
        return reminderRepository.save(reminder);
    }
}
