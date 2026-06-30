/** 配套课程年级卡状态 */
export const GRADE_ACCESS = {
  ACTIVE: 'active',
  COMPLETED: 'completed',
  NOT_STARTED: 'not_started',
  OPEN: 'open'
}

/** 入学年份 → 各年级配套课程状态 */
const COHORT_GRADE_RULES = {
  '2021': {
    三年级: GRADE_ACCESS.COMPLETED,
    四年级: GRADE_ACCESS.COMPLETED,
    五年级: GRADE_ACCESS.COMPLETED,
    六年级: GRADE_ACCESS.ACTIVE
  },
  '2023': {
    三年级: GRADE_ACCESS.COMPLETED,
    四年级: GRADE_ACCESS.ACTIVE,
    五年级: GRADE_ACCESS.NOT_STARTED,
    六年级: GRADE_ACCESS.NOT_STARTED
  }
}

/** 从用户信息解析入学年份（如 2021级9班 → 2021） */
export function detectEnrollmentYear(profile) {
  const className = profile?.className || localStorage.getItem('className') || ''

  // 1. 班级名最可靠（后端返回，如 2023级4班）
  const fromClass = className.match(/(20\d{2})级/)
  if (fromClass) return fromClass[1]

  // 2. 学号用户名（如 wangziming20230432 → 2023）
  const username = profile?.username || localStorage.getItem('username') || ''
  const fromUserSuffix = username.match(/(20[12]\d)\d{4,}$/)
  if (fromUserSuffix) return fromUserSuffix[1]
  const fromUser = username.match(/(20\d{2})/)
  if (fromUser) return fromUser[1]

  // 3. 仅信任后端 profile 字段，不用 localStorage 缓存（避免切换账号后残留旧届别）
  if (profile?.enrollmentYear != null && String(profile.enrollmentYear).trim()) {
    return String(profile.enrollmentYear).trim()
  }

  return null
}

/** 从册次名解析年级前缀，如「六年级上册」→「六年级」 */
export function parseGradeKeyFromBookName(bookName) {
  const m = String(bookName || '').match(/^(.+?)(上|下)册$/)
  return m ? m[1] : null
}

/** 获取某入学届在某年级的配套课程状态；未配置届别则全部开放 */
export function getGradeAccess(enrollmentYear, gradeKey) {
  if (!enrollmentYear || !gradeKey) return GRADE_ACCESS.OPEN
  const rules = COHORT_GRADE_RULES[String(enrollmentYear)]
  if (!rules) return GRADE_ACCESS.OPEN
  return rules[gradeKey] || GRADE_ACCESS.OPEN
}

export function canEnterGrade(enrollmentYear, gradeKey) {
  return getGradeAccess(enrollmentYear, gradeKey) === GRADE_ACCESS.ACTIVE
    || getGradeAccess(enrollmentYear, gradeKey) === GRADE_ACCESS.OPEN
}

export function getGradeBadge(enrollmentYear, gradeKey) {
  const access = getGradeAccess(enrollmentYear, gradeKey)
  if (access === GRADE_ACCESS.COMPLETED) return { type: 'completed', label: '✅已完成' }
  if (access === GRADE_ACCESS.NOT_STARTED) return { type: 'not_started', label: '未开始' }
  return null
}

/** 已完成年级的默认展示成绩（后续可接后端） */
export function getDefaultGradeScores(gradeKey) {
  return { up: '优秀', down: '优秀', gradeKey }
}
