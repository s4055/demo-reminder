package demo.ai.reminder.service;

import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.Tag;
import demo.ai.reminder.dto.response.TagResponse;
import demo.ai.reminder.repository.ReminderRepository;
import demo.ai.reminder.repository.TagRepository;
import demo.ai.reminder.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    public List<TagResponse> getTags() {
        return tagRepository.findAllInUseWithReminderCount(currentUser.id()).stream()
                .map(TagResponse::from)
                .toList();
    }

    @Transactional
    public List<Tag> resolveTags(List<String> tagNames) {
        if (tagNames == null) {
            return List.of();
        }
        Set<String> names = new LinkedHashSet<>(tagNames);
        Map<String, Tag> existing = tagRepository.findByUserIdAndNameIn(currentUser.id(), names).stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity()));
        return names.stream()
                .map(name -> existing.computeIfAbsent(name,
                        newName -> tagRepository.save(new Tag(currentUser.reference(), newName))))
                .toList();
    }

    @Transactional
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Tag not found: " + id));
        reminderRepository.findByReminderTagsTagId(id).forEach(reminder -> reminder.removeTag(tag));
        reminderRepository.flush();
        tagRepository.delete(tag);
    }
}
