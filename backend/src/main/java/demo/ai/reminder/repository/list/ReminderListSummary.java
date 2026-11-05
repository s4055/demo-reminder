package demo.ai.reminder.repository.list;

import demo.ai.reminder.domain.list.ListRole;
import demo.ai.reminder.domain.list.ReminderList;

public record ReminderListSummary(ReminderList list, long reminderCount, ListRole role, long memberCount) {
}
