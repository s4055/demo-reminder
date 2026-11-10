package demo.ai.reminder.service;

import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.exception.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;

final class UniqueSaves {

    private UniqueSaves() {
    }

    static <T> T saveOrConflict(JpaRepository<T, ?> repository, T entity, String conflictMessage) {
        try {
            return repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ResultCode.CONFLICT, conflictMessage);
        }
    }
}
