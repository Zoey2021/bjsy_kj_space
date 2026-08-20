import request from './request'

export const login = (data) => request.post('/auth/login', data)
export const getClassCodeStudents = (code) => request.get(`/auth/class-code/${code}/students`)
export const classCodeLogin = (data) => request.post('/auth/class-code/login', data)
export const getClassLoginCode = (classId) => request.get(`/dashboard/class/${classId}/login-code`)
export const refreshClassLoginCode = (classId) => request.post(`/dashboard/class/${classId}/login-code/refresh`)
export const getMe = () => request.get('/auth/me')
export const getCourseMap = () => request.get('/course/map')
export const getTextbooks = (type = 'MAIN') => request.get('/course/textbooks', { params: { type } })
export const getGradeOutline = (gradeId) => request.get(`/course/grade/${gradeId}/outline`)
export const getLesson = (id) => request.get(`/course/lesson/${id}`)
export const submitTask = (data) => request.post('/learn/submit', data)
export const recordVisit = (data) => request.post('/learn/visit', data)
export const getMyRecords = () => request.get('/learn/records')
export const getLessonRecords = (lessonId) => request.get(`/learn/records/lesson/${lessonId}`)
export const getCurrentLesson = () => request.get('/learn/current-lesson')
export const getNotifications = (since) =>
  request.get('/learn/notifications', { params: since ? { since } : {} })
export const getParkStatus = () => request.get('/learn/park/status')
export const applyParkAccess = () => request.post('/learn/park/apply')
export const getParkApplications = (classId) =>
  request.get('/teacher/park/applications', { params: { classId } })
export const reviewParkAccess = (data) => request.post('/teacher/park/review', data)
export const teacherIntervene = (data) => request.post('/teacher/intervene', data)
export const setClassCurrentLesson = (classId, lessonId) =>
  request.put(`/teacher/class/${classId}/current-lesson`, { lessonId })
export const getClasses = () => request.get('/dashboard/classes')
export const getLessonActivityDashboard = (lessonId, classId) =>
  request.get(`/dashboard/lesson/${lessonId}/class/${classId}`)
export const getDashboard = (classId) => request.get(`/dashboard/data/${classId}`)
export const getMatrix = (classId) => request.get(`/dashboard/matrix/${classId}`)
export const getRanking = (classId) => request.get(`/dashboard/ranking/${classId}`)
export const getUsers = () => request.get('/admin/users')
export const createUser = (data) => request.post('/admin/users', data)
export const toggleUser = (id) => request.put(`/admin/users/${id}/toggle`)

/** 教师端课程编辑 */
export const teacherGetTextbooks = (type = 'MAIN') => request.get('/teacher/course/textbooks', { params: { type } })
export const teacherGetOutline = (gradeId) => request.get(`/teacher/course/grade/${gradeId}/outline`)
export const teacherGetLesson = (lessonId) => request.get(`/teacher/course/lesson/${lessonId}`)
export const teacherCreateLesson = (data) => request.post('/teacher/course/lesson', data)
export const teacherUpdateLesson = (lessonId, data) => request.put(`/teacher/course/lesson/${lessonId}`, data)
export const teacherCreateResource = (data) => request.post('/teacher/course/resource', data)
export const teacherUpdateResource = (id, data) => request.put(`/teacher/course/resource/${id}`, data)
export const teacherDeleteResource = (id) => request.delete(`/teacher/course/resource/${id}`)
export const teacherLessonStats = (lessonId, classId) =>
  request.get(`/teacher/course/lesson/${lessonId}/stats`, { params: { classId } })

/** AI 活动推荐（Dify，工作流可能需 30～120 秒） */
export const aiRecommendActivities = (lessonId) =>
  request.post(`/ai/recommend/${lessonId}`, null, { timeout: 180000 })
export const aiAdoptActivities = (lessonId, tasks) =>
  request.post(`/ai/recommend/${lessonId}/adopt`, { tasks })

/** 按勾选活动生成互动页草稿（Dify 可能较慢，最多 3 分钟） */
export const teacherGenerateActivities = (lessonId, data) =>
  request.post(`/teacher/course/lesson/${lessonId}/generate-activities`, data, { timeout: 180000 })

/** 同步到班级学生端 */
export const teacherPublishActivity = (lessonId, data) =>
  request.post(`/teacher/course/lesson/${lessonId}/publish-activity`, data, { timeout: 180000 })

/** 上传飞象老师 HTML 到指定活动槽 */
export const teacherUploadFeixiang = (lessonId, file, slotIndex) => {
  const form = new FormData()
  form.append('file', file)
  form.append('slotIndex', String(slotIndex))
  return request.post(`/teacher/course/lesson/${lessonId}/upload-feixiang`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}
