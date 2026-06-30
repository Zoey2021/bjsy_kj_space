<template>
  <div class="outline-page" v-if="tree">
    <header class="hub-header">
      <el-page-header @back="$router.push('/student/map')" :content="tree.name" />
      <el-tag :type="tree.textbookType === 'SCHOOL' ? 'success' : 'primary'" size="small">
        {{ tree.textbookType === 'SCHOOL' ? '校本教材' : '配套教材' }}
      </el-tag>
    </header>

    <el-tabs v-model="activeTab" class="hub-tabs">
      <el-tab-pane label="目录导航" name="outline">
        <TextbookDirectory :tree="tree" mode="student" @lesson-click="goLesson" />
      </el-tab-pane>

      <el-tab-pane label="电子教材" name="pdf" lazy>
        <div class="pdf-pane">
          <PdfViewer :src="pdfUrl" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PdfViewer from '../../components/PdfViewer.vue'
import TextbookDirectory from '../../components/textbook/TextbookDirectory.vue'
import { getGradeOutline, getMe } from '../../api'
import { resolveTextbookPdfUrl } from '../../utils/textbookPdf'
import {
  detectEnrollmentYear,
  getGradeAccess,
  parseGradeKeyFromBookName,
  GRADE_ACCESS
} from '../../utils/cohortGradeAccess'

const route = useRoute()
const router = useRouter()
const tree = ref(null)
const activeTab = ref('outline')
const pdfUrl = ref('')

const load = async () => {
  const id = route.params.gradeId
  const res = await getGradeOutline(id)
  tree.value = res.data
  pdfUrl.value = resolveTextbookPdfUrl(res.data)
  await guardGradeAccess()
}

const guardGradeAccess = async () => {
  if (!tree.value || tree.value.textbookType === 'SCHOOL') return
  const gradeKey = parseGradeKeyFromBookName(tree.value.name)
  if (!gradeKey) return
  const meRes = await getMe().catch(() => ({ data: {} }))
  const profile = meRes.data || {}
  if (profile.className) localStorage.setItem('className', profile.className)
  const year = detectEnrollmentYear(profile)
  const access = getGradeAccess(year, gradeKey)
  if (access === GRADE_ACCESS.NOT_STARTED) {
    ElMessage.warning('该年级课程尚未开始')
    router.replace('/student/map')
  } else if (access === GRADE_ACCESS.COMPLETED) {
    ElMessage.info('该年级课程已完成，请在课程地图点击「已完成」查看成绩')
    router.replace('/student/map')
  }
}

onMounted(load)
watch(() => route.params.gradeId, load)

const goLesson = (lesson) => router.push(`/student/lesson/${lesson.id}`)
</script>

<style scoped>
.outline-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 8px 16px 40px;
  min-height: calc(100vh - 64px);
}
.hub-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}
.hub-tabs :deep(.el-tabs__content) {
  padding-top: 8px;
}
.pdf-pane {
  height: calc(100vh - 200px);
  min-height: 480px;
}
</style>
