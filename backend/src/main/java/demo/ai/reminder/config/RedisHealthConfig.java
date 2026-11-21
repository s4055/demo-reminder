package demo.ai.reminder.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.health.DataRedisHealthIndicator;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@ConditionalOnProperty(prefix = "app.auth.login-attempt", name = "store", havingValue = "redis")
public class RedisHealthConfig {

    @Bean
    public HealthContributor redisHealthContributor(RedisConnectionFactory connectionFactory) {
        return new DataRedisHealthIndicator(connectionFactory);
    }
}
