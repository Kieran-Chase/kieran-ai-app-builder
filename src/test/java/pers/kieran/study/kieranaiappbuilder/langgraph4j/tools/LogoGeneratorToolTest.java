package pers.kieran.study.kieranaiappbuilder.langgraph4j.tools;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import pers.kieran.study.kieranaiappbuilder.langgraph4j.model.ImageResource;
import pers.kieran.study.kieranaiappbuilder.langgraph4j.model.enums.ImageCategoryEnum;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Kieran_Chase
 * @project kieran-ai-app-builder
 * @date 2026/7/22
 */
@SpringBootTest
class LogoGeneratorToolTest {

    @Resource
    private LogoGeneratorTool logoGeneratorTool;

    @Test
    void testGenerateLogos() {
        // 测试生成Logo
        List<ImageResource> logos = logoGeneratorTool.generateLogos("技术公司现代简约风格Logo");
        assertNotNull(logos);
        ImageResource firstLogo = logos.getFirst();
        assertEquals(ImageCategoryEnum.LOGO, firstLogo.getCategory());
        assertNotNull(firstLogo.getDescription());
        assertNotNull(firstLogo.getUrl());
        logos.forEach(logo ->
                System.out.println("Logo: " + logo.getDescription() + " - " + logo.getUrl())
        );
    }
}
