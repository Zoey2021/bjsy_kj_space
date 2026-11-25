/** 课程地图/目录中识别「单元小测」课时 */
export function isUnitQuizLesson(lesson) {
  if (!lesson) return false
  const title = String(lesson.title || '')
  if (title.includes('单元小测')) return true
  const cfg = lesson.configJson || lesson.workspaceLayout || ''
  return String(cfg).includes('unit_quiz')
}

export function studentUnitQuizPath(lessonId) {
  return `/student/unit-quiz/${lessonId}`
}

export function teacherUnitQuizPath(lessonId, gradeId) {
  const q = gradeId != null ? `?gradeId=${gradeId}` : ''
  return `/teacher/unit-quiz/${lessonId}${q}`
}
