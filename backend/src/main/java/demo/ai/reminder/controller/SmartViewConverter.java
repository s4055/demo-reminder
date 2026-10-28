package demo.ai.reminder.controller;

import demo.ai.reminder.service.SmartView;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class SmartViewConverter implements Converter<String, SmartView> {

    @Override
    public SmartView convert(String value) {
        return Arrays.stream(SmartView.values())
                .filter(view -> view.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown smart view: " + value));
    }
}
