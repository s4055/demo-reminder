package demo.ai.reminder.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import demo.ai.reminder.domain.list.ListRole;
import demo.ai.reminder.domain.list.QListMember;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static demo.ai.reminder.domain.list.QListMember.listMember;
import static demo.ai.reminder.domain.list.QReminderList.reminderList;
import static demo.ai.reminder.domain.reminder.QReminder.reminder;

@RequiredArgsConstructor
class ReminderListRepositoryCustomImpl implements ReminderListRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ReminderListSummary> findAllWithReminderCount(Long userId) {
        BooleanExpression isOwner = listMember.role.eq(ListRole.OWNER);
        return summaries(userId)
                .orderBy(
                        new CaseBuilder().when(isOwner).then(Expressions.ZERO).otherwise(Expressions.ONE).asc(),
                        new CaseBuilder().when(isOwner).then(reminderList.sortOrder).otherwise(Expressions.ZERO).asc(),
                        listMember.id.asc())
                .fetch();
    }

    @Override
    public Optional<ReminderListSummary> findSummary(Long userId, Long listId) {
        return Optional.ofNullable(summaries(userId).where(reminderList.id.eq(listId)).fetchOne());
    }

    private JPAQuery<ReminderListSummary> summaries(Long userId) {
        QListMember otherMember = new QListMember("otherMember");
        return queryFactory
                .select(Projections.constructor(ReminderListSummary.class,
                        reminderList,
                        JPAExpressions.select(reminder.count())
                                .from(reminder)
                                .where(reminder.list.eq(reminderList), reminder.parent.isNull(), reminder.completed.isFalse()),
                        listMember.role,
                        JPAExpressions.select(otherMember.count())
                                .from(otherMember)
                                .where(otherMember.list.eq(reminderList))))
                .from(listMember)
                .join(listMember.list, reminderList)
                .where(listMember.user.id.eq(userId));
    }
}
