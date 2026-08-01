package pers.kieran.study.kieranaiappbuilder.monitor;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
@Slf4j
public class AiModelMonitorListener implements ChatModelListener {

    private static final String REQUEST_START_TIME_KEY = "request_start_time";

    private static final String MONITOR_CONTEXT_KEY = "monitor_context";

    @Resource
    private AiModelMetricsCollector aiModelMetricsCollector;

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        Map<Object, Object> attributes = requestContext.attributes();
        attributes.put(REQUEST_START_TIME_KEY, Instant.now());

        MonitorContext context = MonitorContextHolder.getContext();
        if (context != null) {
            attributes.put(MONITOR_CONTEXT_KEY, context);
        }

        String modelName = requestContext.chatRequest().modelName();
        aiModelMetricsCollector.recordRequest(userId(context), appId(context), modelName, "started");
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        Map<Object, Object> attributes = responseContext.attributes();
        MonitorContext context = getMonitorContext(attributes);
        String modelName = responseContext.chatResponse() == null ? responseContext.chatRequest().modelName()
                : responseContext.chatResponse().modelName();

        aiModelMetricsCollector.recordRequest(userId(context), appId(context), modelName, "success");
        recordResponseTime(attributes, context, modelName);
        recordTokenUsage(responseContext, context, modelName);
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        Map<Object, Object> attributes = errorContext.attributes();
        MonitorContext context = getMonitorContext(attributes);
        String modelName = errorContext.chatRequest().modelName();
        String errorMessage = errorContext.error() == null ? "unknown" : errorContext.error().getMessage();

        aiModelMetricsCollector.recordRequest(userId(context), appId(context), modelName, "error");
        aiModelMetricsCollector.recordError(userId(context), appId(context), modelName, errorMessage);
        recordResponseTime(attributes, context, modelName);
    }

    private void recordResponseTime(Map<Object, Object> attributes, MonitorContext context, String modelName) {
        Object startTime = attributes.get(REQUEST_START_TIME_KEY);
        if (!(startTime instanceof Instant instant)) {
            return;
        }
        aiModelMetricsCollector.recordResponseTime(
                userId(context),
                appId(context),
                modelName,
                Duration.between(instant, Instant.now())
        );
    }

    private void recordTokenUsage(ChatModelResponseContext responseContext, MonitorContext context, String modelName) {
        if (responseContext.chatResponse() == null || responseContext.chatResponse().metadata() == null) {
            return;
        }
        TokenUsage tokenUsage = responseContext.chatResponse().metadata().tokenUsage();
        if (tokenUsage == null) {
            return;
        }
        aiModelMetricsCollector.recordTokenUsage(userId(context), appId(context), modelName, "input", tokenUsage.inputTokenCount());
        aiModelMetricsCollector.recordTokenUsage(userId(context), appId(context), modelName, "output", tokenUsage.outputTokenCount());
        aiModelMetricsCollector.recordTokenUsage(userId(context), appId(context), modelName, "total", tokenUsage.totalTokenCount());
    }

    private MonitorContext getMonitorContext(Map<Object, Object> attributes) {
        Object context = attributes.get(MONITOR_CONTEXT_KEY);
        if (context instanceof MonitorContext monitorContext) {
            return monitorContext;
        }
        return MonitorContextHolder.getContext();
    }

    private String userId(MonitorContext context) {
        return context == null ? null : context.getUserId();
    }

    private String appId(MonitorContext context) {
        return context == null ? null : context.getAppId();
    }
}
