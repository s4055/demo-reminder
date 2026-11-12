package demo.ai.reminder.controller;

import demo.ai.reminder.service.CompletedCursor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CompletedCursorConverter implements Converter<String, CompletedCursor> {

    @Override
    public CompletedCursor convert(@NonNull String value) {
        return CompletedCursor.decode(value);
    }
}
