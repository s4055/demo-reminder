package demo.ai.reminder.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("app.logging.http")
public record HttpLoggingProperties(List<String> excludedPaths) {

    public HttpLoggingProperties {
        excludedPaths = excludedPaths == null ? List.of() : List.copyOf(excludedPaths);
    }
}
