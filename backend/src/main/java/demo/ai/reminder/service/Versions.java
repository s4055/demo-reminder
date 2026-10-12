package demo.ai.reminder.service;

import demo.ai.reminder.common.BusinessException;
import demo.ai.reminder.common.ResultCode;

// 수정 요청의 버전 검증 (동시 수정 충돌 감지)
final class Versions {

    private Versions() {
    }

    /**
     * 요청한 버전이 현재 버전과 다르면, 편집을 시작한 뒤 다른 사용자가 먼저 수정한 것이므로 409(VERSION_CONFLICT)다.
     */
    static void verify(Long currentVersion, Long requestedVersion, String target) {
        if (!currentVersion.equals(requestedVersion)) {
            throw new BusinessException(ResultCode.VERSION_CONFLICT,
                    target + " was modified by someone else: requested version " + requestedVersion
                            + ", current version " + currentVersion);
        }
    }
}
