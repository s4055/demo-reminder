package demo.ai.reminder.service;

import demo.ai.reminder.common.BusinessException;
import demo.ai.reminder.common.ResultCode;

import java.util.Arrays;

public enum SmartView {
    TODAY,
    SCHEDULED,
    ALL,
    FLAGGED,
    COMPLETED;

    public static SmartView from(String value) {
        return Arrays.stream(values())
                .filter(view -> view.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "Unknown smart view: " + value));
    }
}
