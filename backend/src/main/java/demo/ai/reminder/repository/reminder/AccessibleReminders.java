package demo.ai.reminder.repository.reminder;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import demo.ai.reminder.domain.reminder.QReminder;

import static demo.ai.reminder.domain.list.QListMember.listMember;

public final class AccessibleReminders {

    private AccessibleReminders() {
    }

    public static BooleanExpression accessibleBy(QReminder reminder, Long userId) {
        return reminder.list.isNull().and(reminder.user.id.eq(userId))
                .or(JPAExpressions.selectOne()
                        .from(listMember)
                        .where(listMember.list.eq(reminder.list), listMember.user.id.eq(userId))
                        .exists());
    }
}
