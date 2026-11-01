/** 课件与教材 PDF 放在华北2（北京）的 OSS，不再占服务器磁盘与带宽。 */
export const OSS_ORIGIN = 'https://learn-space-static.oss-cn-beijing.aliyuncs.com'

export function toOssUrl(path) {
  const value = String(path || '').trim()
  if (!value) return ''
  if (/^https?:\/\//i.test(value)) return value
  if (value.startsWith('/school/') || value.startsWith('/textbooks/')) {
    return OSS_ORIGIN + value
  }
  return value
}
