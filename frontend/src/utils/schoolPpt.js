import { toOssUrl } from './ossStatic'

const KECHUANG_LESSON_NAMES = [
  '神奇的3D打印（1）',
  '神奇的3D打印（2）',
  '3D软件早知道',
  '3D打印零距离',
  '机器人的历史发展',
  '医用机器人与我们的生活',
  '工业机器人与我们的生活',
  '人型机器人与未来发展',
  '和天文学交个朋友吧',
  '谁是太阳系的老二',
  '日历是怎么来的',
  '太阳系里还有谁',
  '什么是人工智能',
  '计算机视觉',
  '语音识别'
]

export function resolveSchoolPptUrl(lesson) {
  if (lesson?.pptUrl) return toOssUrl(lesson.pptUrl)
  const title = String(lesson?.title || '')
  const name = title.replace(/^第\d+课\s*/, '').trim()
  if (!KECHUANG_LESSON_NAMES.includes(name)) return ''
  const m = title.match(/第(\d+)课/)
  if (!m) return ''
  return toOssUrl(`/school/kechuang-up/lesson-${String(m[1]).padStart(2, '0')}.pptx`)
}

export function openSchoolPpt(url) {
  if (!url) return
  window.open(url, '_blank', 'noopener')
}
