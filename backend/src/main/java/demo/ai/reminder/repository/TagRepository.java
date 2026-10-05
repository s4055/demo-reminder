package demo.ai.reminder.repository;

import demo.ai.reminder.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// 태그 이름은 사용자별로 고유하므로 모든 조회에 소유자(userId) 조건을 붙인다.
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByIdAndUserId(Long id, Long userId);

    List<Tag> findByUserIdAndNameIn(Long userId, Collection<String> names);

    // 어떤 리마인더에도 붙어 있지 않은 태그는 제외한다.
    @Query("""
            select new demo.ai.reminder.repository.TagSummary(
                t,
                (select count(r) from Reminder r join r.tags rt where rt = t and r.completed = false)
            )
            from Tag t
            where t.user.id = :userId
              and exists (select 1 from Reminder r join r.tags rt where rt = t)
            order by t.name
            """)
    List<TagSummary> findAllInUseWithReminderCount(Long userId);
}
