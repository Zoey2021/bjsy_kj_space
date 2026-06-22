/** 从校本教材 description 解析系列名（如「科创启航 · …」→ 科创启航） */
export function parseSchoolSeries(book) {
  const desc = book?.description || ''
  const m = desc.match(/^(.+?)\s*[·•]/)
  if (m) return m[1].trim()
  return '校本课程'
}

/** 将校本册次按系列分组，每组内按 sortOrder 排序 */
export function groupSchoolTextbooks(books) {
  const map = new Map()
  const sorted = [...(books || [])].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
  for (const book of sorted) {
    const key = parseSchoolSeries(book)
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(book)
  }
  return Array.from(map.entries()).map(([key, seriesBooks]) => ({
    key,
    cardTitle: key,
    books: seriesBooks
  }))
}
