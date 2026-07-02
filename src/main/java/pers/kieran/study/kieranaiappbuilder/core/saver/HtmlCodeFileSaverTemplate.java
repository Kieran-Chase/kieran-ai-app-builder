package pers.kieran.study.kieranaiappbuilder.core.saver;

import cn.hutool.core.util.StrUtil;
import pers.kieran.study.kieranaiappbuilder.ai.model.HtmlCodeResult;
import pers.kieran.study.kieranaiappbuilder.exception.BusinessException;
import pers.kieran.study.kieranaiappbuilder.exception.ErrorCode;
import pers.kieran.study.kieranaiappbuilder.model.enums.CodeGenTypeEnum;

/**
 * HTML代码文件保存器
 *
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        // 保存 HTML 文件
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    }

    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
        // HTML 代码不能为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码内容不能为空");
        }
    }
}
