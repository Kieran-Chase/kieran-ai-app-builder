package pers.kieran.study.kieranaiappbuilder.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pers.kieran.study.kieranaiappbuilder.common.BaseResponse;
import pers.kieran.study.kieranaiappbuilder.common.ResultUtils;

/**
 * @author Kieran_Chase
 * @project kieran-ai-app-builder
 * @date 2026/6/20
 */

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping("/")
    public BaseResponse healthCheck(){
        return ResultUtils.success("ok");
    }

}
