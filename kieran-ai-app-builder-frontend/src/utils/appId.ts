/**
 * 应用 ID 处理工具
 *
 * ⚠️ 重要：应用 ID 是后端雪花算法生成的 18 位大整数（如 431765052603629568），
 * 远超 JavaScript 安全整数范围（Number.MAX_SAFE_INTEGER ≈ 9×10¹⁵）。
 * 后端已将其序列化为字符串返回，前端必须全程以字符串传递，
 * 绝对不能使用 Number(appId) / parseInt(appId) 转换，否则会丢失精度导致 ID 对不上。
 *
 * 由于 OpenAPI 自动生成的接口类型将 id 声明为 number，此处用类型断言桥接：
 * 运行时字符串原样传递（不丢精度），仅在编译期满足类型约束。
 */
export const toAppId = (id: string): number => {
  return id as unknown as number
}
