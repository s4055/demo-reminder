package demo.ai.reminder.repository;

import demo.ai.reminder.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByNameIn(Collection<String> names);

    // 어떤 리마인더에도 붙어 있지 않은 태그는 제외한다.
    @Query("""
            select new demo.ai.reminder.repository.TagSummary(
                t,
                (select count(r) from Reminder r join r.tags rt where rt = t and r.completed = false)
            )
            from Tag t
            where exists (select 1 from Reminder r join r.tags rt where rt = t)
            order by t.name
            """)
    List<TagSummary> findAllInUseWithReminderCount();
}
