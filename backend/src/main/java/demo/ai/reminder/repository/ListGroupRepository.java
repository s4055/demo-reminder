package demo.ai.reminder.repository;

import demo.ai.reminder.domain.ListGroup;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ListGroupRepository extends JpaRepository<ListGroup, Long> {

    List<ListGroup> findByUserId(Long userId, Sort sort);

    List<ListGroup> findByUserId(Long userId);

    Optional<ListGroup> findByIdAndUserId(Long id, Long userId);

    @Query("select coalesce(max(g.sortOrder), -1) + 1 from ListGroup g where g.user.id = :userId")
    int findNextSortOrder(Long userId);
}
