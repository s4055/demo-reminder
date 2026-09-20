package demo.ai.demoreminder.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown smart view: " + value));
    }
}
