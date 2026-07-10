/**
 * 应用相关常量
 */

// 精选应用的优先级（与后端 AppConstant.GOOD_APP_PRIORITY 保持一致）
export const GOOD_APP_PRIORITY = 99

// 默认应用优先级
export const DEFAULT_APP_PRIORITY = 0

// 代码生成类型枚举（与后端 CodeGenTypeEnum 保持一致）
export const CODE_GEN_TYPE_ENUM = {
  HTML: 'html',
  MULTI_FILE: 'multi_file',
} as const

// 代码生成类型的中文映射
export const CODE_GEN_TYPE_MAP: Record<string, string> = {
  html: '原生 HTML 模式',
  multi_file: '原生多文件模式',
}
