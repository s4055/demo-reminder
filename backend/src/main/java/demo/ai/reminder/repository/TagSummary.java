package demo.ai.reminder.repository;

import demo.ai.reminder.domain.Tag;

public record TagSummary(Tag tag, long reminderCount) {
}
