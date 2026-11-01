import request from './request'

export const login = (data) => request.post('/auth/login', data)
export const getDingTalkLogin = () => request.get('/auth/dingtalk/url')
export const getClassCodeStudents = (code) => request.get(`/auth/class-code/${code}/students`)
export const classCodeLogin = (data) => request.post('/auth/class-code/login', data)
export const getClassLoginCode = (classId) => request.get(`/dashboard/class/${classId}/login-code`)
export const refreshClassLoginCode = (classId) => request.post(`/dashboard/class/${classId}/login-code/refresh`)
export const getMe = () => request.get('/auth/me')
export const changePassword = (data) => request.post('/auth/change-password', data)
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
export const getClassDismiss = () => request.get('/learn/class-dismiss')
export const applyParkAccess = () => request.post('/learn/park/apply')
export const submitPretestG4 = (data) => request.post('/learn/pretest/g4', data)
export const getMyPretestG4 = () => request.get('/learn/pretest/g4/mine')
export const teacherGetPretestG4 = (classId) =>
  request.get('/teacher/pretest/g4', { params: { classId } })
export const submitPretestG6 = (data) => request.post('/learn/pretest/g6', data)
export const getMyPretestG6 = () => request.get('/learn/pretest/g6/mine')
export const teacherGetPretestG6 = (classId) =>
  request.get('/teacher/pretest/g6', { params: { classId } })
export const teacherClearPretestG6 = (classId) =>
  request.delete('/teacher/pretest/g6', { params: { classId } })
export const getParkApplications = (classId, lessonId) =>
  request.get('/teacher/park/applications', { params: { classId, lessonId } })
export const reviewParkAccess = (data) => request.post('/teacher/park/review', data)
export const reviewParkClass = (data) => request.post('/teacher/park/review-class', data)
export const getParkPlays = (classId, lessonId) =>
  request.get('/teacher/park/plays', { params: { classId, lessonId } })
export const getParkSettings = () => request.get('/teacher/park/settings')
export const saveParkSettings = (data) => request.put('/teacher/park/settings', data)
export const recordParkPlay = (data) => request.post('/learn/park/play', data)

/** 教师端后台管理：教师 / 班级 / 学生 */
export const manageListTeachers = () => request.get('/teacher/manage/teachers')
export const manageCreateTeacher = (data) => request.post('/teacher/manage/teachers', data)
export const manageUpdateTeacher = (id, data) => request.put(`/teacher/manage/teachers/${id}`, data)
export const manageDeleteTeacher = (id) => request.delete(`/teacher/manage/teachers/${id}`)
export const manageResetTeacherPassword = (id) => request.put(`/teacher/manage/teachers/${id}/reset-password`)
export const manageSaveTeacherClasses = (id, classIds) => request.put(`/teacher/manage/teachers/${id}/classes`, { classIds })

export const manageListClasses = () => request.get('/teacher/manage/classes')
export const manageCreateClass = (data) => request.post('/teacher/manage/classes', data)
export const manageUpdateClass = (id, data) => request.put(`/teacher/manage/classes/${id}`, data)
export const manageDeleteClass = (id) => request.delete(`/teacher/manage/classes/${id}`)

export const manageListStudents = (classId, q) =>
  request.get('/teacher/manage/students', { params: { classId: classId || undefined, q: q || undefined } })
export const manageCreateStudent = (data) => request.post('/teacher/manage/students', data)
export const manageBatchCreateStudents = (data) => request.post('/teacher/manage/students/batch', data)
export const manageUpdateStudent = (id, data) => request.put(`/teacher/manage/students/${id}`, data)
export const manageDeleteStudent = (id) => request.delete(`/teacher/manage/students/${id}`)
export const manageResetStudentPassword = (id) => request.put(`/teacher/manage/students/${id}/reset-password`)
export const manageNormalizeClassUsernames = (classId) =>
  request.post(`/teacher/manage/classes/${classId}/normalize-usernames`)

export const teacherIntervene = (data) => request.post('/teacher/intervene', data)
export const setClassCurrentLesson = (classId, lessonId) =>
  request.put(`/teacher/class/${classId}/current-lesson`, { lessonId })
export const getClasses = () => request.get('/dashboard/classes')
export const getLessonActivityDashboard = (lessonId, classId) =>
  request.get(`/dashboard/lesson/${lessonId}/class/${classId}`)
export const clearLessonClassRecords = (lessonId, classId) =>
  request.delete(`/dashboard/lesson/${lessonId}/class/${classId}/records`)
export const getDashboard = (classId) => request.get(`/dashboard/data/${classId}`)
export const getMatrix = (classId) => request.get(`/dashboard/matrix/${classId}`)
export const getRanking = (classId) => request.get(`/dashboard/ranking/${classId}`)
export const getClassScreen = (lessonId) =>
  request.get('/learn/class-screen', { params: { lessonId } })
export const getStudentAvatar = () => request.get('/learn/avatar')
export const chooseAvatarFigure = (figure) => request.post('/learn/avatar/figure', { figure })
export const equipAvatarItem = (itemId) => request.post('/learn/avatar/equip', { itemId })
export const getStudentMall = () => request.get('/learn/mall')
export const redeemMallItem = (itemId) => request.post('/learn/mall/redeem', { itemId })
export const getTeacherPointsRules = () => request.get('/teacher/points/rules')
export const saveTeacherPointsRules = (data) => request.put('/teacher/points/rules', data)
export const resetClassPoints = (classId) =>
  request.post('/teacher/points/reset', null, { params: { classId } })
export const getTeacherMall = (classId) =>
  request.get('/teacher/mall', { params: classId ? { classId } : {} })
export const setMallApplyOpen = (data) => request.put('/teacher/mall/open', data)
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

/** AI 实验室（学生端四馆） */
export const getAiLabConfig = () => request.get('/ai-lab/config')
export const teacherGetAiLabConfig = () => request.get('/teacher/ai-lab/config')
export const teacherSaveAiLabConfig = (data) => request.put('/teacher/ai-lab/config', data || {})
export const teacherUploadAiLabHtml = (hallId, file, title) => {
  const form = new FormData()
  form.append('file', file)
  if (title) form.append('title', title)
  return request.post(`/teacher/ai-lab/${hallId}/upload-html`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

/** 课时配置：读取当前已发布探究 */
export const teacherGetLessonHtmlConfig = (lessonId) =>
  request.get(`/teacher/course/lesson/${lessonId}/html-config`)

/** 课时配置：修改某一探究标题 */
export const teacherUpdateActivityTitle = (lessonId, seq, title) =>
  request.put(`/teacher/course/lesson/${lessonId}/activity/${seq}/title`, { title })

/** 课时配置：保存学习目标 */
export const teacherUpdateLessonHtmlMeta = (lessonId, data) =>
  request.put(`/teacher/course/lesson/${lessonId}/html-meta`, data || {})

/** 课时配置：单独替换某一探究 HTML */
export const teacherReplaceActivityHtml = (lessonId, seq, file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post(`/teacher/course/lesson/${lessonId}/activity/${seq}/replace-html`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

/** 课时配置：上传整课 HTML 并拆成 2～3 个探究草稿 */
export const teacherUploadSplitHtml = (lessonId, file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post(`/teacher/course/lesson/${lessonId}/upload-split-html`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

/** 课时配置：确认预览后发布到学生课程地图 */
export const teacherPublishHtmlPack = (lessonId, data) =>
  request.post(`/teacher/course/lesson/${lessonId}/publish-html-pack`, data || {}, { timeout: 60000 })

/** 分层脚手架工作台 */
export const teacherGetScaffoldTemplates = () => request.get('/teacher/scaffold/templates')
export const teacherSaveScaffoldTemplate = (data) => request.post('/teacher/scaffold/templates', data)
export const teacherGetLessonScaffold = (lessonId) => request.get(`/teacher/scaffold/${lessonId}`)
export const teacherSaveLessonScaffold = (lessonId, data) =>
  request.post(`/teacher/scaffold/${lessonId}`, data)
export const teacherPublishLessonScaffold = (lessonId, data) =>
  request.post(`/teacher/scaffold/${lessonId}/publish`, data)
export const teacherGenerateScaffold = (data, config = {}) =>
  request.post('/teacher/scaffold/generate', data, { timeout: 360000, ...config })
export const teacherGetTiers = (classId) => request.get('/teacher/tiers', { params: { classId } })
export const teacherOverrideTier = (data) => request.post('/teacher/tiers/override', data)

/** 学生端按档课时内容（含 effective 档位 tier：教师指定 > 前测 > B） */
export const getLessonContent = (lessonId) =>
  request.get('/learn/lesson-content', { params: { lessonId } })
export const reportHintUsed = (data) => request.post('/learn/hint-used', data)
