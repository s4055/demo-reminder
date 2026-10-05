package demo.ai.reminder.service;

import demo.ai.reminder.common.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.Tag;
import demo.ai.reminder.repository.ReminderRepository;
import demo.ai.reminder.repository.TagRepository;
import demo.ai.reminder.repository.TagSummary;
import demo.ai.reminder.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;
    private final ReminderRepository reminderRepository;
    private final CurrentUser currentUser;

    public List<TagSummary> getTags() {
        return tagRepository.findAllInUseWithReminderCount(currentUser.id());
    }

    /**
     * 태그 이름 목록을 태그 엔티티로 바꾼다. 현재 사용자의 태그에서 찾고, 앞뒤 공백은 제거하고 중복은 하나로 합치며, 없는 태그는 새로 만든다.
     * null이면 빈 목록으로 본다.
     */
    @Transactional
    public List<Tag> resolveTags(List<String> tagNames) {
        if (tagNames == null) {
            return List.of();
        }
        Set<String> names = tagNames.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Tag> existing = tagRepository.findByUserIdAndNameIn(currentUser.id(), names).stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity()));
        return names.stream()
                .map(name -> existing.computeIfAbsent(name,
                        newName -> tagRepository.save(new Tag(currentUser.reference(), newName))))
                .toList();
    }

    // 태그를 삭제하면 붙어 있던 리마인더에서 떼어내기만 하고 리마인더 자체는 유지한다.
    @Transactional
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Tag not found: " + id));
        reminderRepository.findByTagsId(id).forEach(reminder -> reminder.removeTag(tag));
        tagRepository.delete(tag);
    }
}
