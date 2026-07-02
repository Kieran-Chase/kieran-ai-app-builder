package pers.kieran.study.kieranaiappbuilder.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import pers.kieran.study.kieranaiappbuilder.ai.model.HtmlCodeResult;
import pers.kieran.study.kieranaiappbuilder.ai.model.MultiFileCodeResult;


/**
 * @author Kieran_Chase
 * @project kieran-ai-app-builder
 * @date 2026/7/1
 */
@SpringBootTest
class AiCodeGeneratorServiceTest {
    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generatorHtmlCode() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("生成一个程序员 Kieran 的博客网站，不超过20行");
        Assertions.assertNotNull(result);
    }

    @Test
    void generatorMultiFileCode() {
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode("生成一个程序员 Kieran 的留言板，不超过50行");
        Assertions.assertNotNull(result);
    }
}