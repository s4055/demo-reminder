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

    // 그룹이 없으면 -1을 돌려주므로 +1 하면 첫 순서(0)가 된다.
    @Query("select coalesce(max(g.sortOrder), -1) from ListGroup g where g.user.id = :userId")
    int findMaxSortOrder(Long userId);
}
