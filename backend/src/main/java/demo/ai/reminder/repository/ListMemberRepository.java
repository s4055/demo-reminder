package demo.ai.reminder.repository;

import demo.ai.reminder.domain.ListMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ListMemberRepository extends JpaRepository<ListMember, Long> {

    Optional<ListMember> findByListIdAndUserId(Long listId, Long userId);
}
