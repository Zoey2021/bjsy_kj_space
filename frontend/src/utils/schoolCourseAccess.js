/** 账号可见范围。身份以姓名和登录账号为准，不信任浏览器里手改的标记。 */

export const SCOPE_ALL = 'ALL'
export const SCOPE_KECHUANG = 'KECHUANG'
export const SCOPE_DEFAULT = 'DEFAULT'

const FULL_NAMES = ['张老师', '贾斌', '倪婕妤']
const FULL_USERNAMES = ['teacher1', '18857121982', 'admin']
const KECHUANG_NAMES = ['郑超煌', '郭伟杰']
const KECHUANG_USERNAMES = ['15396254154', '13588326564']

export const KECHUANG_SERIES = '科创启航'

export function isKechuangBook(book) {
  const blob = [book?.name, book?.description, book?.title, book?.key, book?.cardTitle]
    .filter(Boolean)
    .join(' ')
  return blob.includes(KECHUANG_SERIES)
}

export function isKechuangPptUrl(url) {
  return String(url || '').includes('/school/kechuang-up/')
}

export function currentAccountProfile() {
  return {
    username: localStorage.getItem('username') || '',
    realName: localStorage.getItem('realName') || '',
    role: String(localStorage.getItem('role') || '').toUpperCase()
  }
}

export function accountScope(profile = currentAccountProfile()) {
  const role = String(profile?.role || localStorage.getItem('role') || '').toUpperCase()
  const name = String(profile?.realName || '').trim()
  const username = String(profile?.username || '').trim()
  if (role === 'ADMIN' || FULL_NAMES.includes(name) || FULL_USERNAMES.includes(username)) return SCOPE_ALL
  if (KECHUANG_NAMES.includes(name) || KECHUANG_USERNAMES.includes(username)) return SCOPE_KECHUANG
  return SCOPE_DEFAULT
}

export function canStudentOpenKechuang() {
  return true
}

export function canTeacherOpenKechuang(profile = currentAccountProfile()) {
  const scope = accountScope(profile)
  return scope === SCOPE_ALL || scope === SCOPE_KECHUANG
}

/** 2021 届只看六年级上册，2023 届只看四年级上册 */
export function studentOnlyBookName(profile) {
  const year = String(
    profile?.enrollmentYear
    || localStorage.getItem('enrollmentYear')
    || ''
  )
  if (year === '2021') return '六年级上册'
  if (year === '2023') return '四年级上册'
  const className = profile?.className || localStorage.getItem('className') || ''
  const fromClass = String(className).match(/(20\d{2})级/)
  if (fromClass && fromClass[1] === '2021') return '六年级上册'
  if (fromClass && fromClass[1] === '2023') return '四年级上册'
  return ''
}

export const KECHUANG_LOCK_TEXT = '暂未开放'
