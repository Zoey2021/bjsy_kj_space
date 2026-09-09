/** 按入学年份筛选班级（名称或年级含 2021级 / 2023级） */
export function filterClassesByCohort(classes, year) {
  const y = String(year)
  return (classes || []).filter((c) => {
    const text = `${c.name || ''} ${c.gradeName || ''}`
    const m = text.match(/(20\d{2})/)
    return m && m[1] === y
  })
}
