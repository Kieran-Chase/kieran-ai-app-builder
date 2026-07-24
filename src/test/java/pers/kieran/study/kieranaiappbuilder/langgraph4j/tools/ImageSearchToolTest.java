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
class ImageSearchToolTest {

    @Resource
    private ImageSearchTool imageSearchTool;

    @Test
    void testSearchContentImages() {
        // 测试正常搜索
        List<ImageResource> images = imageSearchTool.searchContentImages("technology");
        assertNotNull(images);
        assertFalse(images.isEmpty());
        // 验证返回的图片资源
        ImageResource firstImage = images.get(0);
        assertEquals(ImageCategoryEnum.CONTENT, firstImage.getCategory());
        assertNotNull(firstImage.getDescription());
        assertNotNull(firstImage.getUrl());
        assertTrue(firstImage.getUrl().startsWith("http"));
        System.out.println("搜索到 " + images.size() + " 张图片");
        images.forEach(image ->
                System.out.println("图片: " + image.getDescription() + " - " + image.getUrl())
        );
    }
}
