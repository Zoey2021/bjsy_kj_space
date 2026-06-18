/** 将 Dify【活动推荐】Markdown 拆分为：学习目标、学习活动槽、学习评价 */
export function parseAiSections(markdown) {
  const text = (markdown || '').trim()
  const sections = {
    objectives: '',
    evaluation: '',
    activities: '',
    activitySlots: defaultSlots()
  }
  if (!text) return sections

  // 固定三板块标题（与 Dify Prompt 一致）
  const blocks = extractSections(text, [
    { key: 'objectives', titles: ['学习目标', '学生学习目标'] },
    { key: 'activities', titles: ['学习活动', '课堂学习活动', '学习活动设计'] },
    { key: 'evaluation', titles: ['学习评价', '课堂评价', '评价方式'] }
  ])

  sections.objectives = blocks.objectives || ''
  sections.evaluation = blocks.evaluation || ''
  sections.activities = blocks.activities || ''

  // 兼容旧版 Dify 输出（教学目标、核心素养等）
  if (!sections.objectives) {
    sections.objectives = extractLegacyObjectives(text)
  }
  if (!sections.evaluation) {
    sections.evaluation = extractLegacyEvaluation(text)
  }
  if (!sections.activities) {
    sections.activities = extractLegacyActivities(text)
  }

  sections.activitySlots = splitActivitiesIntoSlots(sections.activities)
  return sections
}

/** 按 ## 标题切块 */
function extractSections(text, defs) {
  const result = {}
  const headings = []
  const re = /^#{2,3}\s*(.+?)\s*$/gm
  let m
  while ((m = re.exec(text)) !== null) {
    headings.push({ title: m[1].trim(), index: m.index, len: m[0].length })
  }
  for (const def of defs) {
    const hit = headings.find((h) =>
      def.titles.some((t) => h.title === t || h.title.startsWith(t))
    )
    if (!hit) continue
    const start = hit.index + hit.len
    const next = headings.find((h) => h.index > hit.index)
    const end = next ? next.index : text.length
    result[def.key] = text.slice(start, end).trim()
  }
  return result
}

function extractLegacyObjectives(text) {
  const re = /^#{1,3}\s*(学习目标|学生学习目标|教学目标|教师学习目标|教学重点)\s*$/m
  const match = re.exec(text)
  if (!match) return ''
  const start = match.index + match[0].length
  const next = /^#{1,3}\s/m.exec(text.slice(start))
  const end = next ? start + next.index : text.length
  return text.slice(start, end).trim()
}

function extractLegacyEvaluation(text) {
  const parts = []
  const progress = sliceByHeading(text, ['学习后的进步', '学习后进步'])
  const literacy = sliceByHeading(text, ['核心素养精准指向', '核心素养'])
  const evalBody = sliceByHeading(text, ['学习评价', '课堂评价'])
  if (evalBody) parts.push(evalBody)
  if (progress) parts.push('**学习后的进步**\n\n' + progress)
  if (literacy) parts.push('**核心素养**\n\n' + literacy)
  return parts.join('\n\n').trim()
}

function extractLegacyActivities(text) {
  return sliceByHeading(text, ['课堂学习活动', '学习活动', '3个学习活动', '3 个学习活动'])
    || ''
}

function sliceByHeading(text, labels) {
  for (const label of labels) {
    const re = new RegExp(`^#{1,3}\\s*${escapeReg(label)}\\s*$`, 'm')
    const match = re.exec(text)
    if (match) {
      const start = match.index + match[0].length
      const next = /^#{1,3}\s/m.exec(text.slice(start))
      const end = next ? start + next.index : text.length
      return text.slice(start, end).trim()
    }
  }
  return ''
}

function defaultSlots() {
  return [1, 2, 3].map((n) => ({
    title: `活动${n}`,
    source: 'dify',
    content: '',
    uploadedPath: '',
    enabled: false,
    generated: false,
    previewPath: ''
  }))
}

function splitActivitiesIntoSlots(activitiesText) {
  const slots = defaultSlots()
  const text = (activitiesText || '').trim()
  if (!text) return slots

  const re = /^#{1,3}\s*(?:活动\s*)?([一二三四123])\s*[：:.、]?\s*(.*)$/gm
  const hits = []
  let m
  while ((m = re.exec(text)) !== null) {
    const num = chineseNumToInt(m[1])
    if (num >= 1 && num <= 3) {
      hits.push({ num, index: m.index, len: m[0].length, title: m[2]?.trim() || `活动${num}` })
    }
  }

  if (!hits.length) {
    slots[0].content = text
    slots[0].title = '活动1'
    slots[0].enabled = true
    return slots
  }

  hits.sort((a, b) => a.index - b.index)
  for (let i = 0; i < hits.length; i++) {
    const h = hits[i]
    const start = h.index + h.len
    const end = i + 1 < hits.length ? hits[i + 1].index : text.length
    const idx = h.num - 1
    if (idx < 3) {
      slots[idx].title = h.title || `活动${h.num}`
      slots[idx].content = text.slice(start, end).trim()
      slots[idx].enabled = !!slots[idx].content
    }
  }
  return slots
}

function chineseNumToInt(s) {
  const map = { 一: 1, 二: 2, 三: 3, 四: 4, 五: 5 }
  if (/^\d+$/.test(s)) return parseInt(s, 10)
  return map[s] || 1
}

function escapeReg(s) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}
