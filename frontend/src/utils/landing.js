export const IDENTITY_INFO_TEACHER = 'INFO_TEACHER'

export function identityCode() {
  return localStorage.getItem('identityCode') || ''
}

export function isInfoTeacher() {
  return identityCode() === IDENTITY_INFO_TEACHER
}

/** 登录后按角色进入工作台，不再经过教、学、评选择页。 */
export function landingPath() {
  const role = String(localStorage.getItem('role') || '').toUpperCase()
  if (role === 'STUDENT') return '/student/map'
  if (role === 'TEACHER' || role === 'ADMIN') return '/teacher/course-map'
  return '/login'
}
