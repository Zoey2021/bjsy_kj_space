<template>
  <div class="textbook-hub" :class="{ 'is-school': textbookType === 'SCHOOL' }" v-if="ready">
    <header class="hub-header">
      <el-page-header @back="goMap" :content="gradeName" />
      <el-tag :type="textbookType === 'SCHOOL' ? 'success' : 'primary'" size="small">
        {{ textbookType === 'SCHOOL' ? '校本教材' : '配套教材' }}
      </el-tag>
    </header>

    <SchoolCourseCatalog
      v-if="textbookType === 'SCHOOL'"
      :tree="tree || {}"
      @lesson-click="goSchoolLesson"
    />

    <el-tabs v-else v-model="activeTab" class="hub-tabs">
      <el-tab-pane label="目录导航" name="outline">
        <TextbookDirectory v-if="tree" :tree="tree" mode="teacher" @lesson-click="goLesson" />
      </el-tab-pane>

      <el-tab-pane v-if="pdfUrl" label="电子教材" name="pdf" lazy>
        <div class="pdf-pane">
          <PdfViewer :src="pdfUrl" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PdfViewer from '../../components/PdfViewer.vue'
import TextbookDirectory from '../../components/textbook/TextbookDirectory.vue'
import SchoolCourseCatalog from '../../components/school/SchoolCourseCatalog.vue'
import { teacherGetOutline } from '../../api'
import { resolveTextbookPdfUrl } from '../../utils/textbookPdf'
import { openSchoolPpt, resolveSchoolPptUrl } from '../../utils/schoolPpt'
import { isKechuangBook, accountScope, SCOPE_ALL, SCOPE_KECHUANG } from '../../utils/schoolCourseAccess'

const route = useRoute()
const router = useRouter()
const gradeId = Number(route.params.gradeId)

const ready = ref(false)
const tree = ref(null)
const gradeName = ref('')
const textbookType = ref('MAIN')
const pdfUrl = ref('')
const activeTab = ref('outline')

const goMap = () => router.push('/teacher/course-map')

const goLesson = (lesson) => {
  router.push({ path: `/teacher/lesson/${lesson.id}`, query: { gradeId: String(gradeId) } })
}

const goSchoolLesson = (lesson) => {
  const ppt = resolveSchoolPptUrl(lesson)
  openSchoolPpt(ppt)
  router.push({
    path: `/teacher/school-ppt/${lesson.id}`,
    query: { gradeId: String(gradeId), ppt, title: lesson.title || '' }
  })
}

onMounted(async () => {
  if (route.query.tab === 'manage') {
    router.replace(`/teacher/activity-editor/grade/${gradeId}`)
    return
  }
  const res = await teacherGetOutline(gradeId)
  const data = res.data || {}
  if ((data.textbookType || '') === 'SCHOOL' && isKechuangBook(data) && accountScope() !== SCOPE_ALL && accountScope() !== SCOPE_KECHUANG) {
    ElMessage.info('科创启航暂未开放，开通后将点亮入口')
    router.replace('/teacher/course-map')
    return
  }
  if (accountScope() === SCOPE_KECHUANG && !isKechuangBook(data)) {
    ElMessage.info('当前账号仅可查看科创启航')
    router.replace('/teacher/course-map')
    return
  }
  tree.value = data
  gradeName.value = data.name || '教材'
  textbookType.value = data.textbookType || 'MAIN'
  pdfUrl.value = resolveTextbookPdfUrl(data)
  ready.value = true
  if (route.query.tab === 'pdf') {
    activeTab.value = 'pdf'
  }
})
</script>

<style scoped>
.textbook-hub {
  height: calc(100vh - 56px);
  display: flex;
  flex-direction: column;
  padding: 12px 16px 16px;
  max-width: 1400px;
  margin: 0 auto;
}
.hub-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  flex-shrink: 0;
}
.hub-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.hub-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  padding-top: 8px;
  overflow-y: auto;
}
.hub-tabs :deep(.el-tab-pane) {
  height: 100%;
}
.textbook-hub.is-school {
  flex: 1;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 0;
  max-width: none;
  margin: 0;
  align-self: stretch;
  box-sizing: border-box;
}
.textbook-hub.is-school .hub-header {
  margin-bottom: 8px;
}
.textbook-hub.is-school :deep(.catalog) {
  flex: 1;
  min-height: 0;
}
.pdf-pane {
  height: calc(100vh - 200px);
  min-height: 480px;
}
</style>
