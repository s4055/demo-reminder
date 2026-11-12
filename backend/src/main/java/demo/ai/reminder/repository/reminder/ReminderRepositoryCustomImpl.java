package demo.ai.reminder.repository.reminder;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import demo.ai.reminder.domain.reminder.Reminder;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static demo.ai.reminder.domain.list.QListMember.listMember;
import static demo.ai.reminder.domain.reminder.QReminder.reminder;
import static demo.ai.reminder.domain.reminder.QReminderTag.reminderTag;

@RequiredArgsConstructor
class ReminderRepositoryCustomImpl implements ReminderRepositoryCustom {

    private static final char LIKE_ESCAPE = '\\';

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Reminder> findAccessibleById(Long id, Long userId) {
        return Optional.ofNullable(accessible(userId).where(reminder.id.eq(id)).fetchOne());
    }

    @Override
    public List<Reminder> findAccessible(Long userId) {
        return accessible(userId).orderBy(reminder.id.asc()).fetch();
    }

    @Override
    public List<Reminder> findAccessibleByTagName(Long userId, String tagName) {
        return accessible(userId)
                .join(reminder.reminderTags, reminderTag)
                .where(reminderTag.tag.name.eq(tagName))
                .orderBy(reminder.id.asc())
                .fetch();
    }

    @Override
    public List<Reminder> findAccessibleIncomplete(Long userId) {
        return accessible(userId).where(reminder.completed.isFalse()).orderBy(reminder.id.asc()).fetch();
    }

    @Override
    public List<Reminder> findAccessibleIncompleteDueBetween(Long userId, Instant from, Instant to) {
        return accessible(userId)
                .where(reminder.completed.isFalse(), reminder.dueAt.goe(from), reminder.dueAt.lt(to))
                .orderBy(reminder.dueAt.asc(), reminder.id.asc())
                .fetch();
    }

    @Override
    public List<Reminder> findAccessibleIncompleteWithDueAt(Long userId) {
        return accessible(userId)
                .where(reminder.completed.isFalse(), reminder.dueAt.isNotNull())
                .orderBy(reminder.dueAt.asc(), reminder.id.asc())
                .fetch();
    }

    @Override
    public List<Reminder> findAccessibleIncompleteFlagged(Long userId) {
        return accessible(userId)
                .where(reminder.completed.isFalse(), reminder.flagged.isTrue())
                .orderBy(reminder.id.asc())
                .fetch();
    }

    @Override
    public List<Reminder> findAccessibleCompleted(Long userId, int limit) {
        return completedNewestFirst(userId).limit(limit).fetch();
    }

    @Override
    public List<Reminder> findAccessibleCompletedBefore(Long userId, Instant completedAt, Long id, int limit) {
        return completedNewestFirst(userId)
                .where(reminder.completedAt.lt(completedAt)
                        .or(reminder.completedAt.eq(completedAt).and(reminder.id.lt(id))))
                .limit(limit)
                .fetch();
    }

    @Override
    public List<Reminder> searchAccessible(Long userId, String pattern, long offset, int limit) {
        return accessible(userId)
                .where(reminder.title.lower().like(pattern, LIKE_ESCAPE)
                        .or(reminder.memo.lower().like(pattern, LIKE_ESCAPE))
                        .or(hasTagNameLike(pattern)))
                .orderBy(
                        reminder.completed.asc(),
                        dueAtIfIncomplete().asc().nullsLast(),
                        completedAtIfCompleted().desc().nullsLast(),
                        reminder.id.asc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    private JPAQuery<Reminder> completedNewestFirst(Long userId) {
        return accessible(userId)
                .where(reminder.completed.isTrue())
                .orderBy(reminder.completedAt.desc(), reminder.id.desc());
    }

    private DateTimeExpression<Instant> dueAtIfIncomplete() {
        return new CaseBuilder()
                .when(reminder.completed.isFalse()).then(reminder.dueAt)
                .otherwise(Expressions.nullExpression(Instant.class));
    }

    private DateTimeExpression<Instant> completedAtIfCompleted() {
        return new CaseBuilder()
                .when(reminder.completed.isTrue()).then(reminder.completedAt)
                .otherwise(Expressions.nullExpression(Instant.class));
    }

    private JPAQuery<Reminder> accessible(Long userId) {
        return queryFactory.selectFrom(reminder).where(accessibleBy(userId));
    }

    private BooleanExpression accessibleBy(Long userId) {
        return reminder.list.isNull().and(reminder.user.id.eq(userId))
                .or(JPAExpressions.selectOne()
                        .from(listMember)
                        .where(listMember.list.eq(reminder.list), listMember.user.id.eq(userId))
                        .exists());
    }

    private BooleanExpression hasTagNameLike(String pattern) {
        return JPAExpressions.selectOne()
                .from(reminderTag)
                .where(reminderTag.reminder.eq(reminder), reminderTag.tag.name.lower().like(pattern, LIKE_ESCAPE))
                .exists();
    }
}
