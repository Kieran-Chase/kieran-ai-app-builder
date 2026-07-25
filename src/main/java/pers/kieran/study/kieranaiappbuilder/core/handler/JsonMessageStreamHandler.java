package pers.kieran.study.kieranaiappbuilder.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pers.kieran.study.kieranaiappbuilder.ai.model.message.AiResponseMessage;
import pers.kieran.study.kieranaiappbuilder.ai.model.message.StreamMessage;
import pers.kieran.study.kieranaiappbuilder.ai.model.message.StreamMessageTypeEnum;
import pers.kieran.study.kieranaiappbuilder.ai.model.message.ToolExecutedMessage;
import pers.kieran.study.kieranaiappbuilder.ai.model.message.ToolRequestMessage;
import pers.kieran.study.kieranaiappbuilder.ai.tools.BaseTool;
import pers.kieran.study.kieranaiappbuilder.ai.tools.ToolManager;
import pers.kieran.study.kieranaiappbuilder.core.builder.VueProjectBuilder;
import pers.kieran.study.kieranaiappbuilder.model.entity.User;
import pers.kieran.study.kieranaiappbuilder.model.enums.ChatHistoryMessageTypeEnum;
import pers.kieran.study.kieranaiappbuilder.service.ChatHistoryService;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class JsonMessageStreamHandler {

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ToolManager toolManager;

    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId,
                               User loginUser) {
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        Set<String> seenToolIds = new HashSet<>();
        return originFlux
                // 解析每个 JSON 消息块
                .map(chunk -> handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds))
                .filter(StrUtil::isNotEmpty)
                .doOnComplete(() -> {
                    // 流式响应完成后，添加 AI 消息到对话历史
                    String aiResponse = chatHistoryStringBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());

                })
                .doOnError(error -> {
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                })
                .onErrorResume(error -> {
                    String errorMessage = "\n\nAI 生成失败：模型工具调用参数格式错误或生成中断，请重试或更换模型。\n\n";
                    return Flux.just(errorMessage);
                });
    }

    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, Set<String> seenToolIds) {
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        if (typeEnum == null) {
            log.error("不支持的消息类型: {}", streamMessage.getType());
            return "";
        }
        switch (typeEnum) {
            case AI_RESPONSE -> {
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData();
                chatHistoryStringBuilder.append(data);
                return data;
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                String toolName = toolRequestMessage.getName();
                // 检查是否是第一次看到这个工具 ID
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    // 第一次调用这个工具，记录 ID 并完整返回工具信息
                    seenToolIds.add(toolId);
                    // 根据工具名称获取工具示例
                    BaseTool tool = toolManager.getTool(toolName);
                    // 返回格式化的工具调用信息
                    return tool.generateToolRequestResponse();
                }else{
                    // 不是第一次调用这个工具，直接返回空
                    return "";
                }
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                // 根据工具名称获取工具实例
                String toolName = toolExecutedMessage.getName();
                BaseTool tool = toolManager.getTool(toolName);
                String result = tool.generateToolExecutedResult(jsonObject);
                // 输出前端和要持久化的内容
                String output = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(output);
                return output;
            }
            default -> {
                log.error("不支持的消息类型: {}", typeEnum);
                return "";
            }
        }
    }
}
