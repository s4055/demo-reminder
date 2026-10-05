package demo.ai.reminder.repository;

import demo.ai.reminder.domain.ListMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ListMemberRepository extends JpaRepository<ListMember, Long> {

    // 사용자가 리스트의 멤버인지(그리고 어떤 역할인지) 확인할 때 쓴다. 멤버가 아니면 비어 있다.
    Optional<ListMember> findByListIdAndUserId(Long listId, Long userId);

    boolean existsByListIdAndUserId(Long listId, Long userId);
}
