package demo.ai.reminder.repository;

import demo.ai.reminder.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByIdAndUserId(Long id, Long userId);

    List<Tag> findByUserIdAndNameIn(Long userId, Collection<String> names);

    @Query("""
            select new demo.ai.reminder.repository.TagSummary(
                t,
                (select count(rt) from ReminderTag rt where rt.tag = t and rt.reminder.completed = false)
            )
            from Tag t
            where t.user.id = :userId
              and exists (select 1 from ReminderTag rt where rt.tag = t)
            order by t.name
            """)
    List<TagSummary> findAllInUseWithReminderCount(Long userId);
}
