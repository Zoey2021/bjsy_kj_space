/** 配套教材册次（三至六年级，上下册共 8 本） */
export const MAIN_GRADE_ORDER = ['三年级', '四年级', '五年级', '六年级']

export const MAIN_GRADE_BOOKS = [
  '三年级上册', '三年级下册',
  '四年级上册', '四年级下册',
  '五年级上册', '五年级下册',
  '六年级上册', '六年级下册'
]

/** 从 API 列表中筛出配套教材并排序 */
export function sortMainGrades(grades) {
  const list = grades || []
  return MAIN_GRADE_BOOKS.map((name) => list.find((g) => g.name === name)).filter(Boolean)
}
