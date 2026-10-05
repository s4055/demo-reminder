package demo.ai.reminder.service;

import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.repository.ReminderListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 공유 기능 이전에 저장된 리스트에는 멤버가 없어 소유자도 접근할 수 없으므로, 기동할 때 소유자를 OWNER 멤버로 채운다.
 * H2 파일 DB에 남아 있는 기존 데이터를 위한 것이며, 채울 리스트가 없으면 아무것도 하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ListOwnerBackfill implements ApplicationRunner {

    private final ReminderListRepository reminderListRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int restored = restoreOwnerMembers();
        if (restored > 0) {
            log.info("Restored owner members for {} lists created before sharing", restored);
        }
    }

    @Transactional
    public int restoreOwnerMembers() {
        List<ReminderList> lists = reminderListRepository.findWithoutMembers();
        lists.forEach(ReminderList::restoreOwnerMember);
        return lists.size();
    }
}
