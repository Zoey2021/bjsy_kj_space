<template>
  <div class="ppt-page">
    <header class="ppt-head">
      <el-page-header @back="goBack" :content="title" />
      <a v-if="pptHref" class="dl-btn" :href="pptHref" download>下载课件</a>
    </header>

    <div v-if="loading" class="ppt-card">正在打开课件…</div>

    <div v-else class="ppt-card">
      <div class="empty-badge">PPT</div>
      <h2>{{ title }}</h2>
      <p v-if="sizeLabel">本课课件约 {{ sizeLabel }}，请用 WPS 或 PowerPoint 打开后放映。</p>
      <p v-else-if="pptHref">点击下面按钮打开本课课件，用 WPS 或 PowerPoint 放映即可。</p>
      <p v-else>本课已经在目录里。课件准备好后，就可以在这里打开。</p>
      <a v-if="pptHref" class="open-btn" :href="pptHref" target="_blank" rel="noopener">打开课件</a>
      <p v-if="pptHref && isKechuangPptUrl(pptHref)" class="hint">第1课约 200MB，天文课更大，第一次打开会比较慢。建议上课前先点一次。</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getLesson } from '../../api'
import { resolveSchoolPptUrl } from '../../utils/schoolPpt'
import { toOssUrl } from '../../utils/ossStatic'
import {
  isKechuangPptUrl,
  canTeacherOpenKechuang
} from '../../utils/schoolCourseAccess'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const title = ref(route.query.title || '教学课件')
const pptUrl = ref(toOssUrl(route.query.ppt || ''))
const fileSize = ref(0)
const gradeId = ref(route.query.gradeId || '')

const isTeacher = computed(() => localStorage.getItem('role') === 'TEACHER' || localStorage.getItem('role') === 'ADMIN')
const pptHref = computed(() => pptUrl.value || '')

const sizeLabel = computed(() => {
  const n = fileSize.value
  if (!n) return ''
  if (n >= 1024 * 1024 * 1024) return `${(n / 1024 / 1024 / 1024).toFixed(1)} GB`
  return `${Math.max(1, Math.round(n / 1024 / 1024))} MB`
})

const goBack = () => {
  if (gradeId.value) {
    router.push(isTeacher.value
      ? `/teacher/course-map/grade/${gradeId.value}`
      : `/student/textbook/${gradeId.value}`)
    return
  }
  router.push(isTeacher.value ? '/teacher/course-map' : '/student/map')
}

onMounted(async () => {
  if (isTeacher.value && isKechuangPptUrl(route.query.ppt) && !canTeacherOpenKechuang()) {
    ElMessage.info('科创启航暂未开放，开通后将点亮入口')
    router.replace('/teacher/course-map')
    return
  }
  try {
    const id = route.params.id
    const res = await getLesson(id)
    const data = res.data || {}
    const lesson = data.lesson || data
    title.value = lesson.title || title.value || '教学课件'
    if (data.gradeId) gradeId.value = String(data.gradeId)
    const resources = data.resources || []
    const ppt = resources.find((r) => ['PPT', 'PPTX', 'SLIDE'].includes(String(r.resType || r.res_type || '').toUpperCase()))
    const fromApi = ppt?.contentUrl || ppt?.content_url || resolveSchoolPptUrl(lesson)
    pptUrl.value = toOssUrl(fromApi || pptUrl.value)
    if (isTeacher.value && isKechuangPptUrl(pptUrl.value) && !canTeacherOpenKechuang()) {
      ElMessage.info('科创启航暂未开放，开通后将点亮入口')
      router.replace('/teacher/course-map')
      return
    }
    if (pptUrl.value) {
      try {
        const head = await fetch(pptUrl.value, { method: 'HEAD' })
        if (head.ok) fileSize.value = Number(head.headers.get('content-length') || 0)
      } catch { /* 打开按钮不依赖 HEAD */ }
    }
  } catch {
    if (!pptUrl.value) title.value = '课件暂不可用'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.ppt-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.ppt-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  flex-shrink: 0;
}
.dl-btn,
.open-btn {
  text-decoration: none;
  background: #4f46e5;
  color: #fff;
  border-radius: 999px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 700;
}
.open-btn {
  display: inline-block;
  margin-top: 18px;
  padding: 12px 28px;
  font-size: 16px;
}
.ppt-card {
  flex: 1;
  min-height: 0;
  background: #fff;
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 40px 24px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}
.empty-badge {
  width: 72px;
  height: 72px;
  border-radius: 22px;
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 800;
  margin-bottom: 16px;
}
.ppt-card h2 {
  margin: 0 0 8px;
  color: #1e293b;
}
.ppt-card p,
.hint {
  margin: 0;
  color: #64748b;
  max-width: 480px;
  line-height: 1.7;
}
.hint {
  margin-top: 14px;
  font-size: 13px;
}
</style>
