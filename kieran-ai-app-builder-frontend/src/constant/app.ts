/**
 * 应用相关常量
 */

// 精选应用的优先级（与后端 AppConstant.GOOD_APP_PRIORITY 保持一致）
export const GOOD_APP_PRIORITY = 99

// 默认应用优先级
export const DEFAULT_APP_PRIORITY = 0

/**
 * 代码生成类型枚举（与后端 CodeGenTypeEnum 保持一致）
 */
export enum CodeGenTypeEnum {
  HTML = 'html',
  MULTI_FILE = 'multi_file',
  VUE_PROJECT = 'vue_project',
}

/**
 * 代码生成类型配置
 */
export const CODE_GEN_TYPE_CONFIG = {
  [CodeGenTypeEnum.HTML]: {
    label: '原生 HTML 模式',
    value: CodeGenTypeEnum.HTML,
  },
  [CodeGenTypeEnum.MULTI_FILE]: {
    label: '原生多文件模式',
    value: CodeGenTypeEnum.MULTI_FILE,
  },
  [CodeGenTypeEnum.VUE_PROJECT]: {
    label: 'Vue 项目模式',
    value: CodeGenTypeEnum.VUE_PROJECT,
  },
}

// 兼容旧常量命名
export const CODE_GEN_TYPE_ENUM = CodeGenTypeEnum

// 代码生成类型的中文映射
export const CODE_GEN_TYPE_MAP: Record<string, string> = {
  [CodeGenTypeEnum.HTML]: CODE_GEN_TYPE_CONFIG[CodeGenTypeEnum.HTML].label,
  [CodeGenTypeEnum.MULTI_FILE]: CODE_GEN_TYPE_CONFIG[CodeGenTypeEnum.MULTI_FILE].label,
  [CodeGenTypeEnum.VUE_PROJECT]: CODE_GEN_TYPE_CONFIG[CodeGenTypeEnum.VUE_PROJECT].label,
}
