const CN_NUM = { 1: '一', 2: '二', 3: '三', 4: '四', 5: '五', 6: '六' }

/** 从「第1课 算法与问题解决」提取课题名 */
export function extractLessonTopic(lessonTitle) {
  const m = String(lessonTitle || '').match(/第\s*\d+\s*课\s*(.+)/)
  return m ? m[1].trim() : (String(lessonTitle || '').trim() || '本课')
}

function stripHtml(html) {
  return String(html || '')
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/p>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function asNodes(items) {
  return (items || []).map((text) => ({ text: String(text) }))
}

function normalizeBranch(branch) {
  if (Array.isArray(branch.nodes) && branch.nodes.length) return branch
  if (Array.isArray(branch.items) && branch.items.length) {
    return { ...branch, nodes: asNodes(branch.items) }
  }
  return { ...branch, nodes: [] }
}

function extractCoreNodes(text) {
  const nodes = []
  if (/人工/.test(text) && /计算机/.test(text)) {
    nodes.push({ text: '两种解决途径（人工实施、计算机实现）' })
  }
  if (/抽象/.test(text) || /建模/.test(text) || /算法/.test(text)) {
    nodes.push({
      text: '完整探究流程（问题分析→抽象与建模→设计算法→验证算法）'
    })
  }
  if (!nodes.length) {
    const sentences = text.split(/[。！？\n]/).map((s) => s.trim()).filter(Boolean)
    return asNodes(sentences.slice(0, 2).length ? sentences.slice(0, 2) : ['阅读教材，了解本课核心概念'])
  }
  return nodes
}

function buildOrderNodes(activities, hasQuiz) {
  const nodes = []
  if (activities?.length) {
    const actPart = activities
      .map((a, i) => `活动${CN_NUM[a.index || i + 1] || (i + 1)}`)
      .join('、')
    const inqPart = activities
      .map((a, i) => `探究${CN_NUM[a.index || i + 1] || (i + 1)}`)
      .join('、')
    nodes.push({ text: `左侧依次完成${actPart}` })
    nodes.push({ text: `（对应网页${inqPart}）` })
  } else {
    nodes.push({ text: '请从左侧菜单依次完成各探究活动' })
  }
  if (hasQuiz) {
    nodes.push({ text: '全部活动完成后作答课堂小测' })
  }
  return nodes
}

function buildObjectiveNodes(objectives) {
  if (!objectives?.trim()) {
    return [{ text: '按要求完成本课学习活动' }]
  }
  const text = objectives.trim().replace(/^我能/, '')
  const parts = text
    .split(/[，,；;]|并能|并且/)
    .map((s) => s.trim().replace(/[。．.]+$/g, ''))
    .filter(Boolean)

  let wayPart = null
  let flowPart = null
  let verifyPart = null
  for (const p of parts) {
    if (/流程图|绘制/.test(p)) flowPart = p
    else if (/验证|拓展/.test(p)) verifyPart = p
    else if (/人工|计算机|途径/.test(p)) wayPart = p
  }

  const nodes = []
  if (wayPart || verifyPart) {
    const node = { text: '区分人工、计算机两种问题解决途径' }
    if (verifyPart) {
      node.children = [{ text: '验证、拓展投票统计程序' }]
    }
    nodes.push(node)
  }
  if (flowPart) {
    nodes.push({ text: '使用流程图绘制简单算法' })
  }
  if (!nodes.length) {
    return parts.map((t) => ({ text: t }))
  }
  return nodes
}

/**
 * 构建课程介绍思维导图数据。
 * workspacePlan.introMap 可覆盖自动生成（{ title, branches: [{ label, nodes }] }）
 */
export function buildLessonIntroMap({
  lessonTitle,
  contentHtml,
  objectives,
  activities,
  introMap,
  hasQuiz = true
}) {
  if (introMap?.title && Array.isArray(introMap.branches) && introMap.branches.length) {
    return {
      title: introMap.title,
      branches: introMap.branches.map(normalizeBranch)
    }
  }
  const plain = stripHtml(contentHtml)
  return {
    title: extractLessonTopic(lessonTitle),
    branches: [
      { label: '3 本课学习目标', nodes: buildObjectiveNodes(objectives) },
      { label: '1. 核心学习内容', nodes: extractCoreNodes(plain) },
      { label: '2. 课堂完成顺序', nodes: buildOrderNodes(activities, hasQuiz) }
    ]
  }
}
