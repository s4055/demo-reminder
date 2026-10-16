package demo.ai.reminder.repository;

import demo.ai.reminder.domain.ListGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ListGroupRepository extends JpaRepository<ListGroup, Long> {

    List<ListGroup> findByUserIdOrderBySortOrderAscIdAsc(Long userId);

    List<ListGroup> findByUserId(Long userId);

    // 다른 사용자의 그룹은 없는 것처럼 다룬다 (404).
    Optional<ListGroup> findByIdAndUserId(Long id, Long userId);

    // 새 그룹이 받을 마지막 순서. 그룹이 없으면 첫 순서(0)다.
    @Query("select coalesce(max(g.sortOrder), -1) + 1 from ListGroup g where g.user.id = :userId")
    int findNextSortOrder(Long userId);
}
