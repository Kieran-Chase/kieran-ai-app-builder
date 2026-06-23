package pers.kieran.study.kieranaiappbuilder.common;

import lombok.Data;

import java.io.Serializable;
/**
 * @author Kieran_Chase
 * @project kieran-ai-app-builder
 * @date 2026/6/23
 */

/**
 * 删除请求包装类
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
