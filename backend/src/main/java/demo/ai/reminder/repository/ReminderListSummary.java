package demo.ai.reminder.repository;

import demo.ai.reminder.domain.ReminderList;

/**
 * 리스트와 해당 리스트의 미완료 리마인더 개수를 함께 담는 조회 전용 프로젝션.
 */
public record ReminderListSummary(ReminderList list, long reminderCount) {
}
