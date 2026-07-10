import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

/**
 * 格式化时间为相对时间，例如「5 小时前」「1 天前」
 *
 * @param time 时间字符串
 */
export const formatRelativeTime = (time?: string) => {
  if (!time) {
    return '-'
  }
  return dayjs(time).fromNow()
}

/**
 * 格式化时间为标准格式，例如「2026-07-05 12:00:00」
 *
 * @param time 时间字符串
 */
export const formatTime = (time?: string) => {
  if (!time) {
    return '-'
  }
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}
