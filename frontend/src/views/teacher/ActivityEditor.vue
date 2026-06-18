<template>
  <div class="activity-page">
    <header class="top-bar">
      <div class="top-left">
        <h2>活动编辑</h2>
        <p>三至六年级配套教材 · AI 生成草案 → 教师确认 → 生成活动 → 同步给学生</p>
      </div>
      <div class="top-actions">
        <el-select
          v-model="gradeId"
          placeholder="选择教材"
          class="grade-select"
          @change="onGradeChange"
        >
          <el-option v-for="g in gradeOptions" :key="g.id" :label="g.name" :value="g.id" />
        </el-select>
        <el-button
          type="primary"
          :loading="recommending"
          :disabled="!lesson?.id"
          @click="fetchRecommendations"
        >
          {{ recommending ? `AI 生成中 ${recommendElapsed}s…` : 'AI 生成草案' }}
        </el-button>
      </div>
    </header>

    <div v-if="recommending" class="ai-waiting">
      <el-progress :percentage="recommendProgress" :stroke-width="8" striped striped-flow />
      <p>正在生成「学习目标 / 学习活动 / 学习评价」草案，通常 30 秒～2 分钟，请稍候…</p>
    </div>

    <div class="main-layout">
      <aside class="lesson-panel">
        <div class="panel-head">
          <h3>{{ gradeName || '请选择教材' }}</h3>
          <p v-if="lesson">{{ lesson.title }}</p>
          <p v-else class="muted">请在下方选择课时</p>
        </div>
        <div class="lesson-tree">
          <template v-for="unit in outline?.units || []" :key="unit.id">
            <p class="unit-name">{{ unit.name }}</p>
            <div
              v-for="les in unit.lessons || []"
              :key="les.id"
              class="lesson-item"
              :class="{ active: lesson?.id === les.id }"
              @click="selectLesson(les.id)"
            >
              {{ les.title }}
            </div>
          </template>
        </div>
      </aside>

      <section class="editor-panel">
        <el-empty
          v-if="!hasContent && !recommending"
          description="选择课时后点击「AI 生成草案」"
        />

        <template v-else-if="hasContent && !recommending">
          <el-alert
            v-if="aiDraftReady"
            type="info"
            :closable="false"
            show-icon
            class="draft-alert"
            title="AI 已生成草案"
            description="请核对并修改「学习目标」「学习活动」「学习评价」后，勾选活动并点击「生成活动」，确认无误再「同步给学生」。"
          />
          <div class="editor-toolbar">
            <el-tabs v-model="activeTab" class="section-tabs">
              <el-tab-pane label="学习目标" name="objectives" />
              <el-tab-pane label="学习活动" name="activities" />
              <el-tab-pane label="学习评价" name="evaluation" />
            </el-tabs>
            <div class="publish-bar">
              <el-button
                type="warning"
                :loading="generating"
                :disabled="!lesson?.id || !enabledSlots.length"
                @click="generateActivities"
              >
                生成活动
              </el-button>
              <el-select v-model="classId" placeholder="选择班级" class="class-select">
                <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
              <el-button
                type="success"
                :loading="publishing"
                :disabled="!canSync"
                @click="syncToStudents"
              >
                同步给学生
              </el-button>
            </div>
          </div>

          <div class="edit-form">
            <div v-show="activeTab === 'objectives'" class="tab-panel">
              <p class="tab-hint">由 AI 根据教材生成，请修改确认。学生端将看到本段「学习目标」。</p>
              <el-input v-model="sections.objectives" type="textarea" :rows="12" placeholder="学习目标（AI 生成后可编辑）…" />
            </div>

            <div v-show="activeTab === 'activities'" class="tab-panel">
              <p class="tab-hint">由 AI 生成活动描述，请修改确认；勾选要上的活动（1～3 个）后点「生成活动」生成互动页。</p>
              <div
                v-for="(slot, idx) in sections.activitySlots"
                :key="idx"
                class="slot-card"
                :class="{ 'slot-off': !slot.enabled }"
              >
                <div class="slot-head">
                  <el-checkbox v-model="slot.enabled">活动 {{ idx + 1 }}</el-checkbox>
                  <el-input
                    v-model="slot.title"
                    placeholder="活动标题"
                    class="slot-title-input"
                    :disabled="!slot.enabled"
                  />
                  <el-tag v-if="slot.generated" type="success" size="small">已生成</el-tag>
                </div>
                <template v-if="slot.enabled">
                  <el-radio-group v-model="slot.source" size="small" class="source-group">
                    <el-radio-button value="dify">AI 生成</el-radio-button>
                    <el-radio-button value="platform">平台模板</el-radio-button>
                    <el-radio-button value="feixiang">飞象网页</el-radio-button>
                  </el-radio-group>
                  <template v-if="slot.source === 'feixiang'">
                    <div class="upload-row">
                      <input
                        type="file"
                        accept=".html,text/html"
                        :ref="(el) => setFileRef(el, idx)"
                        class="file-input"
                        @change="(e) => onFeixiangFile(idx, e)"
                      />
                      <el-button size="small" @click="triggerUpload(idx)">上传 HTML</el-button>
                      <span v-if="slot.uploadedPath" class="upload-ok">已上传</span>
                    </div>
                  </template>
                  <el-input
                    v-else
                    v-model="slot.content"
                    type="textarea"
                    :rows="5"
                    placeholder="活动描述（供生成互动页）"
                  />
                  <el-button
                    v-if="slot.previewPath"
                    link
                    type="primary"
                    @click="openSlotPreview(slot.previewPath)"
                  >
                    预览本活动
                  </el-button>
                </template>
              </div>
            </div>

            <div v-show="activeTab === 'evaluation'" class="tab-panel">
              <p class="tab-hint">由 AI 生成，请修改确认。学生端将看到本段「学习评价」。</p>
              <el-input v-model="sections.evaluation" type="textarea" :rows="12" placeholder="学习评价（AI 生成后可编辑）…" />
            </div>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  teacherGetTextbooks,
  teacherGetOutline,
  teacherGetLesson,
  aiRecommendActivities,
  teacherGenerateActivities,
  teacherPublishActivity,
  teacherUploadFeixiang,
  getClasses
} from '../../api'
import { parseAiSections } from '../../utils/parseAiSections'
import { sortMainGrades } from '../../constants/grades'

const route = useRoute()

const gradeOptions = ref([])
const gradeId = ref(null)
const gradeName = ref('')
const outline = ref(null)
const lesson = ref(null)
const recommending = ref(false)
const generating = ref(false)
const publishing = ref(false)
const activeTab = ref('objectives')
const recommendElapsed = ref(0)
const recommendProgress = ref(0)
const activityTitle = ref('')
const fileRefs = ref([])
let recommendTimer = null
let recommendProgressTimer = null

const defaultSlots = () => [1, 2, 3].map((n) => ({
  title: `活动${n}`,
  source: 'dify',
  content: '',
  uploadedPath: '',
  enabled: n === 1,
  generated: false,
  previewPath: ''
}))

const sections = reactive({
  objectives: '',
  evaluation: '',
  activities: '',
  activitySlots: defaultSlots()
})

const classes = ref([])
const classId = ref(null)
const aiDraftReady = ref(false)

const enabledSlots = computed(() =>
  sections.activitySlots.filter((s) => s.enabled)
)

const hasContent = computed(() =>
  !!(sections.objectives || sections.evaluation
    || sections.activitySlots.some((s) => s.enabled && (s.content || s.uploadedPath)))
)

const allEnabledGenerated = computed(() =>
  enabledSlots.value.length > 0 && enabledSlots.value.every((s) => s.generated)
)

const canSync = computed(() =>
  lesson.value?.id && classId.value && sections.objectives?.trim()
    && enabledSlots.value.length > 0 && allEnabledGenerated.value
)

const buildPayloadSlots = () =>
  sections.activitySlots
    .filter((s) => s.enabled)
    .map((s) => ({
      title: s.title,
      source: s.source,
      content: s.content,
      uploadedPath: s.uploadedPath,
      enabled: true
    }))

const setFileRef = (el, idx) => {
  if (el) fileRefs.value[idx] = el
}

const triggerUpload = (idx) => {
  fileRefs.value[idx]?.click()
}

const startRecommendTimers = () => {
  recommendElapsed.value = 0
  recommendProgress.value = 5
  recommendTimer = setInterval(() => {
    recommendElapsed.value += 1
  }, 1000)
  recommendProgressTimer = setInterval(() => {
    if (recommendProgress.value < 92) {
      recommendProgress.value += Math.random() * 4
    }
  }, 2000)
}

const stopRecommendTimers = () => {
  if (recommendTimer) clearInterval(recommendTimer)
  if (recommendProgressTimer) clearInterval(recommendProgressTimer)
  recommendTimer = null
  recommendProgressTimer = null
  recommendProgress.value = 100
}

const loadGrades = async () => {
  const res = await teacherGetTextbooks('MAIN')
  gradeOptions.value = sortMainGrades(res.data)
  if (!gradeOptions.value.length) {
    ElMessage.warning('未加载到三至六年级教材，请检查课程数据是否已初始化')
  }
}

const loadClasses = async () => {
  const res = await getClasses()
  classes.value = res.data || []
  if (!classId.value && classes.value.length) {
    classId.value = classes.value[0].id
  }
}

const loadOutline = async () => {
  if (!gradeId.value) return
  const res = await teacherGetOutline(gradeId.value)
  outline.value = res.data
  gradeName.value = res.data?.name || ''
  const queryLessonId = route.query.lessonId ? Number(route.query.lessonId) : null
  if (queryLessonId) {
    await selectLesson(queryLessonId)
    return
  }
  const first = res.data?.units?.[0]?.lessons?.[0]
  if (first && !lesson.value) {
    await selectLesson(first.id)
  }
}

const resetSections = () => {
  aiDraftReady.value = false
  Object.assign(sections, {
    objectives: '',
    evaluation: '',
    activities: '',
    activitySlots: defaultSlots()
  })
}

const selectLesson = async (id) => {
  const res = await teacherGetLesson(id)
  lesson.value = res.data.lesson
  activityTitle.value = ''
  resetSections()
}

const onGradeChange = async () => {
  const g = gradeOptions.value.find((x) => x.id === gradeId.value)
  gradeName.value = g?.name || ''
  lesson.value = null
  outline.value = null
  await loadOutline()
}

const fetchRecommendations = async () => {
  if (!lesson.value?.id) {
    ElMessage.warning('请先选择课时')
    return
  }
  recommending.value = true
  startRecommendTimers()
  try {
    const res = await aiRecommendActivities(lesson.value.id)
    const raw = res.data?.rawContent || ''
    if (!raw) {
      ElMessage.warning('AI 未返回内容')
      return
    }
    const parsed = parseAiSections(raw)
    Object.assign(sections, parsed)
    activityTitle.value = lesson.value.title + ' · 课堂学习'
    activeTab.value = 'objectives'
    aiDraftReady.value = true
    if (!sections.objectives?.trim()) {
      ElMessage.warning('AI 未解析到「学习目标」，请检查 Dify 是否按三板块格式输出')
    } else if (!sections.evaluation?.trim()) {
      ElMessage.warning('AI 未解析到「学习评价」，可在 Dify 中调整 Prompt 后重试')
    } else {
      ElMessage.success('AI 草案已填入三板块，请修改确认')
    }
  } finally {
    stopRecommendTimers()
    recommending.value = false
  }
}

const onFeixiangFile = async (idx, e) => {
  const file = e.target.files?.[0]
  if (!file || !lesson.value?.id) return
  try {
    const res = await teacherUploadFeixiang(lesson.value.id, file, idx + 1)
    const slot = sections.activitySlots[idx]
    slot.uploadedPath = res.data?.uploadedPath || ''
    slot.source = 'feixiang'
    slot.generated = false
    slot.previewPath = ''
    ElMessage.success(`活动${idx + 1} 已上传`)
  } finally {
    e.target.value = ''
  }
}

const generateActivities = async () => {
  if (!lesson.value?.id) return
  if (!sections.objectives?.trim()) {
    ElMessage.warning('请先填写学习目标')
    activeTab.value = 'objectives'
    return
  }
  if (!enabledSlots.value.length) {
    ElMessage.warning('请至少勾选一个活动')
    activeTab.value = 'activities'
    return
  }
  generating.value = true
  try {
    const res = await teacherGenerateActivities(lesson.value.id, {
      studentObjectives: sections.objectives,
      activitySlots: buildPayloadSlots()
    })
    const list = res.data?.activities || []
    let seq = 0
    for (const slot of sections.activitySlots) {
      if (!slot.enabled) continue
      const item = list[seq]
      if (item) {
        slot.generated = true
        slot.previewPath = item.previewPath || ''
      }
      seq++
    }
    ElMessage.success(`已生成 ${list.length} 个活动互动页，确认后点「同步给学生」`)
    activeTab.value = 'activities'
  } finally {
    generating.value = false
  }
}

const syncToStudents = async () => {
  if (!canSync.value) {
    if (!allEnabledGenerated.value) {
      ElMessage.warning('请先为所有勾选的活动点击「生成活动」')
    }
    return
  }
  publishing.value = true
  try {
    const res = await teacherPublishActivity(lesson.value.id, {
      classId: classId.value,
      activityTitle: activityTitle.value || lesson.value.title,
      studentObjectives: sections.objectives,
      evaluation: sections.evaluation,
      activitySlots: buildPayloadSlots()
    })
    const data = res.data || {}
    ElMessage.success(
      `已同步到「${data.className}」，${data.studentCount || 0} 名学生可见 ${data.activityCount || enabledSlots.value.length} 个活动`
    )
  } finally {
    publishing.value = false
  }
}

const openSlotPreview = (path) => {
  if (path) window.open(path, '_blank')
}

onMounted(async () => {
  await loadGrades()
  await loadClasses()
  if (gradeOptions.value.length) {
    gradeId.value = gradeOptions.value[0].id
    gradeName.value = gradeOptions.value[0].name
  }
  await loadOutline()
})

onUnmounted(() => {
  stopRecommendTimers()
})
</script>

<style scoped>
.activity-page {
  max-width: 1280px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding-bottom: 12px;
}
.top-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
  flex-shrink: 0;
}
.top-left h2 { margin: 0 0 4px; font-size: 20px; color: #1e293b; }
.top-left p { margin: 0; font-size: 13px; color: #64748b; }
.top-actions { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.grade-select { width: 180px; }
.ai-waiting {
  margin-bottom: 12px;
  padding: 12px 16px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 10px;
}
.ai-waiting p { margin: 8px 0 0; font-size: 13px; color: #3b82f6; }
.draft-alert { margin: 0 16px 8px; flex-shrink: 0; }
.main-layout {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 14px;
}
.lesson-panel,
.editor-panel {
  background: #fff;
  border: 1px solid #e8e6f5;
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.05);
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
.lesson-panel {
  background: linear-gradient(180deg, #5b4fc7 0%, #7c6cf0 100%);
  color: #fff;
  border: none;
}
.panel-head { padding: 14px 16px 10px; border-bottom: 1px solid rgba(255, 255, 255, 0.12); }
.panel-head h3 { margin: 0 0 4px; font-size: 15px; font-weight: 700; }
.panel-head p { margin: 0; font-size: 12px; }
.muted { opacity: 0.75; }
.lesson-tree { flex: 1; overflow-y: auto; padding: 8px 12px 12px; }
.unit-name { font-size: 11px; font-weight: 600; opacity: 0.85; margin: 10px 4px 4px; }
.lesson-item {
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  margin-bottom: 4px;
  background: rgba(255, 255, 255, 0.08);
  line-height: 1.35;
}
.lesson-item:hover { background: rgba(255, 255, 255, 0.16); }
.lesson-item.active { background: #fff; color: #5b4fc7; font-weight: 600; }
.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 16px 0;
  border-bottom: 1px solid #eef2ff;
  flex-shrink: 0;
  flex-wrap: wrap;
}
.section-tabs { flex: 1; min-width: 200px; }
.section-tabs :deep(.el-tabs__header) { margin-bottom: 0; }
.publish-bar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; padding-bottom: 8px; }
.class-select { width: 140px; }
.edit-form {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  min-height: 0;
}
.tab-panel { max-width: 720px; }
.tab-hint { font-size: 13px; color: #64748b; margin: 0 0 12px; }
.slot-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 12px;
  background: #fafbff;
  transition: opacity 0.2s;
}
.slot-card.slot-off { opacity: 0.55; background: #f8fafc; }
.slot-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.slot-title-input { flex: 1; min-width: 160px; }
.source-group { margin-bottom: 10px; }
.upload-row { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.file-input { display: none; }
.upload-ok { font-size: 12px; color: #059669; }

@media (max-width: 900px) {
  .main-layout { grid-template-columns: 1fr; }
  .lesson-panel { max-height: 220px; }
}
</style>
