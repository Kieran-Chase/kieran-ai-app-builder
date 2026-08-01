package pers.kieran.study.kieranaiappbuilder.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Slf4j
public class AiModelMetricsCollector {

    @Resource
    private MeterRegistry meterRegistry;

    private final ConcurrentMap<String, Counter> requestCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> errorCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> tokenCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> responseTimersCache = new ConcurrentHashMap<>();

    public void recordRequest(String userId, String appId, String modelName, String status) {
        Counter counter = requestCountersCache.computeIfAbsent(
                buildKey(userId, appId, modelName, status),
                key -> Counter.builder("ai_model_requests_total")
                        .description("AI model request count")
                        .tag("user_id", safeTag(userId))
                        .tag("app_id", safeTag(appId))
                        .tag("model_name", safeTag(modelName))
                        .tag("status", safeTag(status))
                        .register(meterRegistry)
        );
        counter.increment();
    }

    public void recordError(String userId, String appId, String modelName, String errorMessage) {
        Counter counter = errorCountersCache.computeIfAbsent(
                buildKey(userId, appId, modelName, errorMessage),
                key -> Counter.builder("ai_model_errors_total")
                        .description("AI model error count")
                        .tag("user_id", safeTag(userId))
                        .tag("app_id", safeTag(appId))
                        .tag("model_name", safeTag(modelName))
                        .tag("error_message", safeTag(errorMessage))
                        .register(meterRegistry)
        );
        counter.increment();
    }

    public void recordTokenUsage(String userId, String appId, String modelName, String tokenType, Integer tokenCount) {
        if (tokenCount == null || tokenCount <= 0) {
            return;
        }
        Counter counter = tokenCountersCache.computeIfAbsent(
                buildKey(userId, appId, modelName, tokenType),
                key -> Counter.builder("ai_model_tokens_total")
                        .description("AI model token usage")
                        .tag("user_id", safeTag(userId))
                        .tag("app_id", safeTag(appId))
                        .tag("model_name", safeTag(modelName))
                        .tag("token_type", safeTag(tokenType))
                        .register(meterRegistry)
        );
        counter.increment(tokenCount);
    }

    public void recordResponseTime(String userId, String appId, String modelName, Duration duration) {
        if (duration == null || duration.isNegative()) {
            return;
        }
        Timer timer = responseTimersCache.computeIfAbsent(
                buildKey(userId, appId, modelName),
                key -> Timer.builder("ai_model_response_duration_seconds")
                        .description("AI model response duration")
                        .tag("user_id", safeTag(userId))
                        .tag("app_id", safeTag(appId))
                        .tag("model_name", safeTag(modelName))
                        .register(meterRegistry)
        );
        timer.record(duration);
    }

    private String buildKey(String... values) {
        return String.join("|", java.util.Arrays.stream(values).map(this::safeTag).toList());
    }

    private String safeTag(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.length() > 200 ? value.substring(0, 200) : value;
    }
}
