/**
 * 全局环境配置
 */

// 后端服务地址（读取 .env 中的 VITE_API_BASE_URL，request.ts 中的 baseURL 也引用此常量）
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL

// 应用部署后的访问域名（读取 .env 中的 VITE_CODE_DEPLOY_HOST，与后端 AppConstant.CODE_DEPLOY_HOST 保持一致）
export const CODE_DEPLOY_HOST = import.meta.env.VITE_CODE_DEPLOY_HOST

/**
 * 应用部署后的完整访问地址
 * 完整地址格式：{CODE_DEPLOY_HOST}/{deployKey}/
 */
export const getDeployUrl = (deployKey: string) => {
  return `${CODE_DEPLOY_HOST}/${deployKey}/`
}

/**
 * 应用生成后的网页访问地址前缀
 * 完整地址格式：{API_BASE_URL}/static/{codeGenType}_{appId}/
 */
export const getStaticPreviewUrl = (codeGenType: string, appId: number | string) => {
  return `${API_BASE_URL}/static/${codeGenType}_${appId}/`
}
