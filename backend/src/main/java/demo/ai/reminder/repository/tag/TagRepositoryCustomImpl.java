package demo.ai.reminder.repository.tag;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static demo.ai.reminder.domain.reminder.QReminderTag.reminderTag;
import static demo.ai.reminder.domain.tag.QTag.tag;

@RequiredArgsConstructor
class TagRepositoryCustomImpl implements TagRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<TagSummary> findAllInUseWithReminderCount(Long userId) {
        return queryFactory
                .select(Projections.constructor(TagSummary.class,
                        tag,
                        JPAExpressions.select(reminderTag.count())
                                .from(reminderTag)
                                .where(reminderTag.tag.eq(tag), reminderTag.reminder.completed.isFalse())))
                .from(tag)
                .where(tag.user.id.eq(userId),
                        JPAExpressions.selectOne()
                                .from(reminderTag)
                                .where(reminderTag.tag.eq(tag))
                                .exists())
                .orderBy(tag.name.asc())
                .fetch();
    }
}
