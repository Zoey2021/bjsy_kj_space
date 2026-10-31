<template>
  <div class="outline-page" :class="{ 'is-school': isSchool }" v-if="tree">
    <header class="hub-header">
      <el-page-header @back="$router.push('/student/map')" :content="tree.name" />
      <el-tag :type="tree.textbookType === 'SCHOOL' ? 'success' : 'primary'" size="small">
        {{ tree.textbookType === 'SCHOOL' ? '校本教材' : '配套教材' }}
      </el-tag>
    </header>

    <SchoolCourseCatalog
      v-if="contentReady && isSchool"
      :tree="tree"
      @lesson-click="goSchoolLesson"
    />

    <el-tabs v-else-if="contentReady" v-model="activeTab" class="hub-tabs">
      <el-tab-pane label="目录导航" name="outline">
        <TextbookDirectory :tree="tree" mode="student" @lesson-click="goLesson" />
      </el-tab-pane>

      <el-tab-pane v-if="pdfUrl" label="电子教材" name="pdf" lazy>
        <div class="pdf-pane">
          <PdfViewer :src="pdfUrl" />
        </div>
      </el-tab-pane>
    </el-tabs>
    <el-empty v-else description="资源正在建设中" :image-size="96" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PdfViewer from '../../components/PdfViewer.vue'
import TextbookDirectory from '../../components/textbook/TextbookDirectory.vue'
import SchoolCourseCatalog from '../../components/school/SchoolCourseCatalog.vue'
import { getGradeOutline, getMe } from '../../api'
import { resolveTextbookPdfUrl } from '../../utils/textbookPdf'
import { openSchoolPpt, resolveSchoolPptUrl } from '../../utils/schoolPpt'
import {
  detectEnrollmentYear,
  getGradeAccess,
  parseGradeKeyFromBookName,
  parseVolumeFromBookName,
  isDownVolumeLocked,
  GRADE_ACCESS
} from '../../utils/cohortGradeAccess'
const route = useRoute()
const router = useRouter()
const tree = ref(null)
const activeTab = ref('outline')
const pdfUrl = ref('')

const isSchool = computed(() => tree.value?.textbookType === 'SCHOOL')

const contentReady = computed(() => {
  if (!tree.value) return false
  if (isSchool.value) {
    return (tree.value.units || []).length > 0 || !!pdfUrl.value
  }
  return true
})

const load = async () => {
  const id = route.params.gradeId
  const res = await getGradeOutline(id)
  const data = res.data
  tree.value = data
  pdfUrl.value = resolveTextbookPdfUrl(data)
  await guardGradeAccess()
}

const guardGradeAccess = async () => {
  if (!tree.value) return
  if (tree.value.textbookType === 'SCHOOL') return
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
    return
  }
  if (access === GRADE_ACCESS.COMPLETED) {
    ElMessage.info('该年级课程已完成，请在课程地图点击「已完成」查看成绩')
    router.replace('/student/map')
    return
  }
  const volume = parseVolumeFromBookName(tree.value.name)
  if (isDownVolumeLocked(gradeKey, volume)) {
    ElMessage.info('下册内容暂未开放')
    router.replace('/student/map')
  }
}

onMounted(load)
watch(() => route.params.gradeId, load)

const goLesson = (lesson) => router.push(`/student/lesson/${lesson.id}`)
const goSchoolLesson = (lesson) => {
  const ppt = resolveSchoolPptUrl(lesson)
  openSchoolPpt(ppt)
  router.push({
    path: `/student/school-ppt/${lesson.id}`,
    query: {
      gradeId: String(tree.value?.id || route.params.gradeId),
      ppt,
      title: lesson.title || ''
    }
  })
}
</script>

<style scoped>
.outline-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 8px 16px 40px;
  min-height: calc(100vh - 64px);
}
.outline-page.is-school {
  width: 100%;
  height: 100%;
  max-width: none;
  min-height: 0;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  align-self: stretch;
  box-sizing: border-box;
}
.outline-page.is-school .hub-header {
  flex-shrink: 0;
  margin-bottom: 8px;
}
.outline-page.is-school :deep(.catalog) {
  flex: 1;
  min-height: 0;
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
