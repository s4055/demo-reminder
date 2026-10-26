package demo.ai.reminder.service;

import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.common.ResultCode;

final class Versions {

    private Versions() {
    }

    static void verify(Long currentVersion, Long requestedVersion, String target) {
        if (!currentVersion.equals(requestedVersion)) {
            throw new BusinessException(ResultCode.VERSION_CONFLICT,
                    target + " was modified by someone else: requested version " + requestedVersion
                            + ", current version " + currentVersion);
        }
    }
}
