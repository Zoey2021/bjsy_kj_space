/**
 * 已过期课时：学生端一律显示「已过期」，不可再进入互动学习。
 * 用于腾出服务器磁盘、收回早期课互动资源。
 *
 * match 规则（满足 gradeName 包含 + title 命中任一）：
 * - titleIncludes: 标题包含
 * - titleStartsWith: 标题以…开头
 * - sortOrders: 课时序号（课程大纲 sort_order）
 */

export const EXPIRED_LESSON_RULES = [
  {
    id: 'g6-up-1-4',
    gradeNameIncludes: '六年级上',
    titleStartsWith: ['第1课', '第2课', '第3课', '第4课'],
    // 与 init.sql 六年级上第一单元 sort_order 对齐，双保险
    sortOrders: [1, 2, 3, 4],
    label: '已过期',
    tip: '本课学习资源已收回，腾出空间给后续新课。完成记录仍保留，无需再学。'
  }
]

export function isExpiredLesson(lesson, gradeName = '') {
  if (!lesson) return false
  const title = String(lesson.title || '')
  const grade = String(gradeName || lesson.gradeName || '')
  const sortOrder = Number(lesson.sortOrder ?? lesson.sort_order)

  return EXPIRED_LESSON_RULES.some((rule) => {
    if (rule.gradeNameIncludes && !grade.includes(rule.gradeNameIncludes)) {
      return false
    }
    const byPrefix = (rule.titleStartsWith || []).some((p) => title.startsWith(p))
    const bySort = Array.isArray(rule.sortOrders)
      && Number.isFinite(sortOrder)
      && rule.sortOrders.includes(sortOrder)
    return byPrefix || bySort
  })
}

export function expiredLessonMeta(lesson, gradeName = '') {
  if (!isExpiredLesson(lesson, gradeName)) return null
  const rule = EXPIRED_LESSON_RULES.find((r) => {
    const title = String(lesson.title || '')
    const grade = String(gradeName || lesson.gradeName || '')
    if (r.gradeNameIncludes && !grade.includes(r.gradeNameIncludes)) return false
    return (r.titleStartsWith || []).some((p) => title.startsWith(p))
      || (Array.isArray(r.sortOrders) && r.sortOrders.includes(Number(lesson.sortOrder ?? lesson.sort_order)))
  })
  return {
    label: rule?.label || '已过期',
    tip: rule?.tip || '本课资源已收回。'
  }
}
