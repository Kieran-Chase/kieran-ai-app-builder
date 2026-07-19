package pers.kieran.study.kieranaiappbuilder.service;

import jakarta.servlet.http.HttpServletResponse;
import pers.kieran.study.kieranaiappbuilder.model.entity.User;

/**
 * @author Kieran_Chase
 * @project kieran-ai-app-builder
 * @date 2026/7/19
 */
public interface ProjectDownloadService {

    /**
     * 下载项目为压缩包
     * @param projectPath
     * @param downloadFileName
     * @param response
     */
    void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}
