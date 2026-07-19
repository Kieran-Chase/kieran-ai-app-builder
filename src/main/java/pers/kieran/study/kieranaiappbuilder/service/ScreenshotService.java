package pers.kieran.study.kieranaiappbuilder.service;

/**
 * @author Kieran_Chase
 * @project kieran-ai-app-builder
 * @date 2026/7/18
 */

/**
 * 截图服务
 */
public interface ScreenshotService {

    /**
     * 通用的截图服务，可以得到访问地址
     *
     * @param webUrl 网址
     * @return
     */
    String generateAndUploadScreenshot(String webUrl);


}
