package demo.ai.reminder.repository.list;

import demo.ai.reminder.domain.list.ListMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ListMemberRepository extends JpaRepository<ListMember, Long> {

    Optional<ListMember> findByListIdAndUserId(Long listId, Long userId);

    List<ListMember> findByListIdOrderByIdAsc(Long listId);
}
