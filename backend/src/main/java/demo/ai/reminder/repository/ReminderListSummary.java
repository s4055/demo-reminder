package demo.ai.reminder.repository;

import demo.ai.reminder.domain.ListRole;
import demo.ai.reminder.domain.ReminderList;

/**
 * 리스트와 해당 리스트의 미완료 리마인더 개수, 조회한 사용자의 역할, 멤버 수(소유자 포함)를 함께 담는 조회 전용 프로젝션.
 * 멤버 수가 2 이상이면 공유된 리스트다.
 */
public record ReminderListSummary(ReminderList list, long reminderCount, ListRole role, long memberCount) {
}
