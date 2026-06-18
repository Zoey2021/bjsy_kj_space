<template>
  <div class="editor-page">
    <!-- 左侧：课程与活动 -->
    <aside class="sidebar">
      <div class="sidebar-head">
        <h3 class="lesson-title">{{ lesson?.title || '请选择课时' }}</h3>
        <p class="grade-hint" v-if="gradeName">{{ gradeName }}</p>
      </div>

      <div class="sidebar-filters">
        <el-select v-model="gradeId" placeholder="切换册次" filterable @change="onGradeChange" class="full">
          <el-option v-for="g in textbooks" :key="g.id" :label="g.name" :value="g.id" />
        </el-select>
        <el-select v-model="classId" placeholder="选择班级" class="full" style="margin-top:8px">
          <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-button
          v-if="lesson?.id && classId"
          type="success"
          class="full"
          style="margin-top:8px"
          @click="openLessonStats"
        >
          查看本课学情
        </el-button>
      </div>

      <el-button type="primary" class="btn-create" round @click="openCreateDialog">
        + 创建课时
      </el-button>

      <LessonQuickNav
        class="teacher-quick-nav"
        :active-key="sideTab === 'challenge' ? '' : sideTab"
        :items="teacherNavItems"
        @select="sideTab = $event"
      />
      <button type="button" class="chip-add" @click="openAddResource">+ 新增资源</button>

      <div class="editor-panel" v-show="sideTab === 'intro'">
        <el-input
          v-model="introDraft"
          type="textarea"
          :rows="8"
          placeholder="填写本课课程介绍（学生进入课时后可见）"
        />
        <el-button type="primary" size="small" class="save-btn" @click="saveIntro" :loading="saving">
          保存介绍
        </el-button>
      </div>

      <div class="editor-panel" v-show="sideTab === 'resource'">
        <div v-for="res in resources" :key="res.id" class="res-row">
          <span>{{ res.title }}</span>
          <el-button link type="danger" size="small" @click="removeResource(res.id)">删</el-button>
        </div>
        <p v-if="!resources.length" class="mini-tip">暂无资源，点「+ 新增」添加</p>
      </div>

      <div class="lesson-tree">
        <p class="tree-label">课时列表</p>
        <template v-for="unit in outline?.units || []" :key="unit.id">
          <p class="unit-name">{{ unit.name }}</p>
          <div
            v-for="les in unit.lessons || []"
            :key="les.id"
            class="activity-item"
            :class="{ active: lesson?.id === les.id }"
            @click="selectLesson(les.id)"
          >
            {{ les.title }}
          </div>
        </template>
      </div>

      <div class="activity-list" v-if="tasks.length">
        <p class="tree-label">本课活动</p>
        <div
          v-for="t in tasks"
          :key="t.id"
          class="activity-item"
          :class="{ active: activeTaskId === t.id && sideTab === 'challenge' }"
          @click="selectTask(t.id)"
        >
          {{ t.title }}
        </div>
      </div>
    </aside>

    <!-- 右侧：学生预览 -->
    <main class="preview-pane">
      <div class="preview-header">
        <span>学生预览</span>
        <el-tag size="small" type="info">右侧为学生端看到的效果</el-tag>
      </div>
      <StudentLessonPreview
        :lesson="lesson"
        :resources="resources"
        :tasks="tasks"
        :active-task-id="activeTaskId"
        :panel="previewPanel"
      />
    </main>

    <!-- 创建课时 -->
    <el-dialog v-model="createDialogVisible" title="创建课时" width="480px">
      <el-form label-width="88px">
        <el-form-item label="所属册次">
          <el-select v-model="createForm.gradeId" @change="loadUnitsForCreate" class="full">
            <el-option v-for="g in textbooks" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属单元">
          <el-select v-model="createForm.unitId" placeholder="选择单元" class="full">
            <el-option
              v-for="u in createUnits"
              :key="u.id"
              :label="u.name"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="课时标题">
          <el-input v-model="createForm.title" placeholder="如：第1课 身边的系统" />
        </el-form-item>
        <el-form-item label="课程介绍">
          <el-input v-model="createForm.content" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreateLesson">创建</el-button>
      </template>
    </el-dialog>

    <!-- 新增资源 -->
    <el-dialog v-model="resourceDialogVisible" title="新增课程资源" width="520px">
      <el-form label-width="88px">
        <el-form-item label="标题">
          <el-input v-model="resourceForm.title" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="resourceForm.resType">
            <el-option label="图文" value="TEXT" />
            <el-option label="链接" value="LINK" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" v-if="resourceForm.resType === 'TEXT'">
          <el-input v-model="resourceForm.contentText" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="链接" v-else>
          <el-input v-model="resourceForm.contentUrl" placeholder="https://" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resourceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResource">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import StudentLessonPreview from '../../components/teacher/StudentLessonPreview.vue'
import LessonQuickNav from '../../components/lesson/LessonQuickNav.vue'
import {
  getClasses,
  teacherGetTextbooks,
  teacherGetOutline,
  teacherGetLesson,
  teacherCreateLesson,
  teacherUpdateLesson,
  teacherCreateResource,
  teacherDeleteResource
} from '../../api'

const router = useRouter()
const textbooks = ref([])
const gradeId = ref(null)
const gradeName = ref('')
const outline = ref(null)
const classId = ref(null)
const classes = ref([])

const lesson = ref(null)
const resources = ref([])
const tasks = ref([])
const introDraft = ref('')
const sideTab = ref('intro')

const teacherNavItems = [
  { key: 'intro', label: '课程介绍', icon: '📖', tone: 'purple' },
  { key: 'resource', label: '课程资源', icon: '📁', tone: 'green' }
]
const activeTaskId = ref(null)
const saving = ref(false)

const createDialogVisible = ref(false)
const createForm = ref({ gradeId: null, unitId: null, title: '', content: '' })
const createUnits = ref([])

const resourceDialogVisible = ref(false)
const resourceForm = ref({ title: '', resType: 'TEXT', contentText: '', contentUrl: '' })

const previewPanel = computed(() => {
  if (sideTab.value === 'intro') return 'intro'
  if (sideTab.value === 'resource') return 'resource'
  return 'challenge'
})

const loadTextbooks = async () => {
  const [main, school] = await Promise.all([
    teacherGetTextbooks('MAIN'),
    teacherGetTextbooks('SCHOOL')
  ])
  textbooks.value = [...(main.data || []), ...(school.data || [])]
  if (!gradeId.value && textbooks.value.length) {
    gradeId.value = textbooks.value[0].id
    gradeName.value = textbooks.value[0].name
  }
}

const loadOutline = async () => {
  if (!gradeId.value) return
  const res = await teacherGetOutline(gradeId.value)
  outline.value = res.data
  gradeName.value = res.data?.name || ''
  const first = res.data?.units?.[0]?.lessons?.[0]
  if (first && !lesson.value) {
    await selectLesson(first.id)
  }
}

const selectLesson = async (id) => {
  const res = await teacherGetLesson(id)
  lesson.value = res.data.lesson
  resources.value = res.data.resources || []
  tasks.value = res.data.tasks || []
  introDraft.value = lesson.value?.content || ''
  activeTaskId.value = tasks.value[0]?.id || null
  sideTab.value = 'intro'
}

const selectTask = (id) => {
  activeTaskId.value = id
  sideTab.value = 'challenge'
}

const openLessonStats = () => {
  if (!lesson.value?.id || !classId.value) return
  router.push({
    path: `/teacher/dashboard`,
    query: { tab: 'overview', lessonId: lesson.value.id, classId: classId.value }
  })
}

const onGradeChange = async () => {
  lesson.value = null
  outline.value = null
  const g = textbooks.value.find(x => x.id === gradeId.value)
  gradeName.value = g?.name || ''
  await loadOutline()
}

const saveIntro = async () => {
  if (!lesson.value?.id) return
  saving.value = true
  try {
    await teacherUpdateLesson(lesson.value.id, { content: introDraft.value })
    lesson.value.content = introDraft.value
    ElMessage.success('课程介绍已保存')
  } finally {
    saving.value = false
  }
}

const openCreateDialog = () => {
  createForm.value = {
    gradeId: gradeId.value,
    unitId: null,
    title: '',
    content: ''
  }
  createDialogVisible.value = true
  loadUnitsForCreate()
}

const loadUnitsForCreate = async () => {
  createForm.value.unitId = null
  if (!createForm.value.gradeId) {
    createUnits.value = []
    return
  }
  const res = await teacherGetOutline(createForm.value.gradeId)
  createUnits.value = res.data?.units || []
}

const submitCreateLesson = async () => {
  if (!createForm.value.unitId || !createForm.value.title?.trim()) {
    ElMessage.warning('请选择单元并填写标题')
    return
  }
  const res = await teacherCreateLesson({
    unitId: createForm.value.unitId,
    title: createForm.value.title.trim(),
    content: createForm.value.content
  })
  ElMessage.success('课时已创建')
  createDialogVisible.value = false
  gradeId.value = createForm.value.gradeId
  await loadOutline()
  await selectLesson(res.data.id)
}

const openAddResource = () => {
  if (!lesson.value?.id) {
    ElMessage.warning('请先选择课时')
    return
  }
  resourceForm.value = { title: '', resType: 'TEXT', contentText: '', contentUrl: '' }
  resourceDialogVisible.value = true
}

const submitResource = async () => {
  if (!lesson.value?.id || !resourceForm.value.title?.trim()) {
    ElMessage.warning('请填写资源标题')
    return
  }
  await teacherCreateResource({
    lessonId: lesson.value.id,
    title: resourceForm.value.title.trim(),
    resType: resourceForm.value.resType,
    contentText: resourceForm.value.contentText,
    contentUrl: resourceForm.value.contentUrl
  })
  ElMessage.success('资源已添加')
  resourceDialogVisible.value = false
  await selectLesson(lesson.value.id)
  sideTab.value = 'resource'
}

const removeResource = async (id) => {
  await teacherDeleteResource(id)
  ElMessage.success('已删除')
  await selectLesson(lesson.value.id)
}

onMounted(async () => {
  const cls = await getClasses()
  classes.value = cls.data?.length ? cls.data : [{ id: 1, name: '七年级1班' }]
  classId.value = classes.value[0]?.id
  await loadTextbooks()
  await loadOutline()
  const qLesson = router.currentRoute.value.query.lessonId
  if (qLesson) {
    await selectLesson(Number(qLesson))
    sideTab.value = 'challenge'
  }
})

watch(sideTab, (tab) => {
  if (tab === 'challenge' && tasks.value.length && !activeTaskId.value) {
    activeTaskId.value = tasks.value[0].id
  }
})
</script>

<style scoped>
.editor-page {
  display: flex;
  gap: 16px;
  height: calc(100vh - 60px - 28px);
  max-width: 1400px;
  margin: 0 auto;
  box-sizing: border-box;
}
.sidebar {
  width: 320px;
  flex-shrink: 0;
  background: linear-gradient(180deg, #5b4fc7 0%, #7c6cf0 48%, #9d8df8 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  padding: 20px 18px 18px;
  overflow: hidden;
  border-radius: 16px;
  box-shadow: 0 8px 28px rgba(91, 79, 199, 0.22);
  box-sizing: border-box;
}
.sidebar-head {
  margin-bottom: 14px;
  padding: 0 2px;
}
.lesson-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.35;
}
.grade-hint {
  margin: 6px 0 0;
  font-size: 12px;
  opacity: 0.85;
}
.sidebar-filters {
  padding: 0 2px;
  margin-bottom: 4px;
}
.full { width: 100%; }
.btn-create {
  width: 100%;
  margin: 14px 2px;
  background: linear-gradient(135deg, #f472b6, #ec4899);
  border: none;
  font-weight: 600;
}
.teacher-quick-nav {
  margin: 0 2px 8px;
}
.chip-add {
  width: calc(100% - 4px);
  margin: 0 2px 12px;
  border: none;
  border-radius: 10px;
  padding: 8px 10px;
  font-size: 12px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.35);
  color: #fff;
  font-weight: 600;
}
.chip-add:hover {
  background: rgba(255, 255, 255, 0.5);
}
.editor-panel {
  background: rgba(255, 255, 255, 0.14);
  border-radius: 12px;
  padding: 12px;
  margin: 0 2px 12px;
}
.save-btn { margin-top: 8px; }
.res-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  padding: 4px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
}
.mini-tip { font-size: 12px; opacity: 0.8; margin: 0; }
.lesson-tree, .activity-list {
  flex: 1;
  overflow-y: auto;
  margin-top: 4px;
  padding: 0 2px 4px;
  scrollbar-width: thin;
}
.lesson-tree::-webkit-scrollbar,
.activity-list::-webkit-scrollbar {
  width: 6px;
}
.lesson-tree::-webkit-scrollbar-thumb,
.activity-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.35);
  border-radius: 3px;
}
.tree-label {
  font-size: 11px;
  text-transform: uppercase;
  opacity: 0.75;
  margin: 8px 0 4px;
}
.unit-name {
  font-size: 12px;
  opacity: 0.9;
  margin: 8px 0 4px;
  font-weight: 600;
}
.activity-item {
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  margin-bottom: 4px;
  background: rgba(255, 255, 255, 0.08);
  line-height: 1.3;
}
.activity-item:hover {
  background: rgba(255, 255, 255, 0.18);
}
.activity-item.active {
  background: #fff;
  color: #5b4fc7;
  font-weight: 600;
}
.preview-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 18px 20px 20px;
  min-width: 0;
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e8e6f5;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.06);
  box-sizing: border-box;
}
.preview-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  font-weight: 600;
  color: #4338ca;
}
.preview-pane :deep(.preview-root) {
  flex: 1;
  min-height: 0;
}

/* 侧栏内下拉框：白底圆角，避免贴边、难辨认 */
.sidebar :deep(.el-select .el-input__wrapper) {
  border-radius: 10px;
  box-shadow: none;
}
.sidebar :deep(.el-input__inner) {
  font-size: 13px;
}

@media (max-width: 1100px) {
  .editor-page {
    flex-direction: column;
    height: auto;
    min-height: calc(100vh - 60px - 28px);
  }
  .sidebar {
    width: 100%;
    max-height: 42vh;
  }
  .preview-pane {
    min-height: 50vh;
  }
}
</style>
