package demo.ai.reminder.repository.tag;

import demo.ai.reminder.domain.tag.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long>, TagRepositoryCustom {

    Optional<Tag> findByIdAndUserId(Long id, Long userId);

    List<Tag> findByUserIdAndNameIn(Long userId, Collection<String> names);
}
