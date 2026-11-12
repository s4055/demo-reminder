package demo.ai.reminder.controller;

import demo.ai.reminder.service.OffsetCursor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class OffsetCursorConverter implements Converter<String, OffsetCursor> {

    @Override
    public OffsetCursor convert(@NonNull String value) {
        return OffsetCursor.decode(value);
    }
}
