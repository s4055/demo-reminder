package demo.ai.reminder.repository;

import demo.ai.reminder.domain.Tag;

/**
 * 태그와 해당 태그가 붙은 미완료 리마인더 개수를 함께 담는 조회 전용 프로젝션.
 */
public record TagSummary(Tag tag, long reminderCount) {
}
