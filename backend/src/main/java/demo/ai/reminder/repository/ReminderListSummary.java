package demo.ai.reminder.repository;

import demo.ai.reminder.domain.ListRole;
import demo.ai.reminder.domain.ReminderList;

public record ReminderListSummary(ReminderList list, long reminderCount, ListRole role, long memberCount) {
}
