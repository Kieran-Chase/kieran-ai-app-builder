/// <reference types="vite/client" />

interface ImportMetaEnv {
  // 后端服务地址
  readonly VITE_API_BASE_URL: string
  // 应用部署后的访问域名
  readonly VITE_CODE_DEPLOY_HOST: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
