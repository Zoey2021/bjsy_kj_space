<template>
  <!-- 学生工作台：左侧课程信息 + 右侧内容（六年级上第1课等） -->
  <div v-if="workspacePlan" class="lesson-studio">
    <aside class="studio-left">
      <div class="lesson-title-bar">{{ workspacePlan.lessonTitle || lesson?.title }}</div>

      <LessonQuickNav :active-key="quickNavKey" :items="quickNavItems" @select="onQuickNav" />

      <div class="menu-list">
        <button
          v-if="hasTextbook"
          type="button"
          class="menu-item"
          :class="{ active: activePanel === 'reading', locked: !isTeacherPreview && !readingUnlocked }"
          @click="openReading"
        >
          <span class="menu-label">阅读教材</span>
          <span v-if="readingCompleted" class="done-tag">✓</span>
        </button>
        <button
          type="button"
          class="menu-item"
          :class="{ active: activePanel === 'intro', locked: !introUnlocked }"
          @click="openIntro"
        >
          <span class="menu-label">课程介绍</span>
          <span v-if="!introUnlocked" class="lock-tag">需解锁</span>
        </button>
        <button
          v-for="act in workspacePlan.activities || []"
          :key="'act-' + act.index"
          type="button"
          class="menu-item"
          :class="{ active: activePanel === 'act-' + act.index, locked: !isActivityUnlocked(act) }"
          @click="selectActivity(act)"
        >
          <span class="menu-label">{{ activityMenuLabel(act) }}</span>
          <span v-if="!isActivityUnlocked(act)" class="lock-tag">需解锁</span>
          <span v-else-if="completedActivities.has(act.index)" class="done-tag">✓</span>
        </button>
        <button
          type="button"
          class="menu-item"
          :class="{ active: activePanel === 'evaluation' }"
          @click="openEvaluation"
        >
          <span class="menu-label">学习评价</span>
          <span v-if="evaluationSubmitted" class="done-tag">已完成</span>
        </button>
        <button
          type="button"
          class="menu-item"
          :class="{ active: activePanel === 'quiz', locked: !quizUnlocked }"
          @click="openQuiz"
        >
          <span class="menu-label">课堂小测</span>
          <span v-if="!quizUnlocked" class="lock-tag">需解锁</span>
          <span v-else-if="quizSubmitted" class="done-tag">已完成</span>
        </button>
      </div>

      <div v-if="!isTeacherPreview" class="feature-row">
        <button type="button" class="feature-btn" @click="ElMessage.info('班级学习大屏即将开放')">班级学习大屏</button>
        <button type="button" class="feature-btn" @click="router.push('/student/ai')">AI评价中心</button>
      </div>

      <template v-if="!isTeacherPreview">
        <div class="points-row">
          <span>排名积分 <strong>{{ totalPoints }}</strong></span>
          <span>可兑奖 <strong>{{ totalPoints }}</strong></span>
        </div>
        <button type="button" class="signin-btn" :class="{ done: signedToday }" @click="toggleSignIn">
          {{ signedToday ? '今日已签到' : '今日签到' }}
        </button>
      </template>

      <div v-if="isTeacherPreview" class="teacher-preview-bar">
        <el-tag type="warning" effect="plain" size="small">教师预览</el-tag>
        <p>与学生端相同的课时内容与探究活动，提交与积分仅对学生生效。</p>
        <el-button type="primary" link @click="goBack">← 返回教材目录</el-button>
      </div>

      <div v-if="!isTeacherPreview" class="student-footer">
        <div>{{ className || '我的班级' }}</div>
        <div>学号 {{ studentNo }} · {{ studentName }}</div>
      </div>
    </aside>

    <main class="studio-right">
      <section v-show="activePanel === 'reading'" class="content-panel reading-panel">
        <header class="activity-panel-head reading-head">
          <h2>阅读教材</h2>
          <p>请先阅读与本课对应的电子课本，了解核心概念后再进入后续学习环节。</p>
        </header>
        <div class="reading-body">
          <PdfViewer v-if="textbookPdfUrl" :src="textbookPdfUrl" />
          <el-empty v-else description="暂无关联教材" :image-size="88" />
        </div>
        <footer v-if="!isTeacherPreview" class="reading-footer">
          <el-button
            type="primary"
            size="large"
            :loading="submitting === externalTask?.id"
            @click="completeReading"
          >
            已阅读，继续
          </el-button>
        </footer>
      </section>
      <section v-show="activePanel === 'intro'" class="content-panel intro-panel">
        <LessonIntroMindMap v-if="introMindMap" :map="introMindMap" />
      </section>
      <section
        v-show="activePanel.startsWith('act-')"
        class="content-panel iframe-panel"
      >
        <header v-if="currentActivity" class="activity-panel-head">
          <h2>{{ activityMenuLabel(currentActivity) }}</h2>
        </header>
        <iframe
          v-if="currentIframeSrc"
          :key="currentIframeSrc"
          :src="currentIframeSrc"
          class="content-frame"
          :title="currentActivityTitle"
        />
      </section>
      <section v-show="activePanel === 'records'" class="content-panel records-panel-wrap">
        <el-empty v-if="isTeacherPreview" description="学习记录仅学生端可见" :image-size="88" />
        <LessonRecordsPanel
          v-else
          ref="recordsPanelRef"
          :lesson-id="Number(route.params.id)"
          @go-activity="goActivityFromRecords"
        />
      </section>
      <section v-show="activePanel === 'profile'" class="content-panel profile-panel">
        <el-empty description="正在开发" :image-size="96" />
      </section>
      <section v-show="activePanel === 'evaluation'" class="content-panel eval-panel">
        <LessonSelfEvaluation
          v-if="workspacePlan.evaluationForm"
          :evaluation="workspacePlan.evaluationForm"
          :submitting="submitting === externalTask?.id"
          @submit="submitEvaluation"
        />
        <div v-else class="intro-panel">
          <h2>学习评价</h2>
          <p class="objectives">{{ workspacePlan.evaluationHint || '请按教师要求完成自评。' }}</p>
        </div>
      </section>
      <section v-show="activePanel === 'quiz'" class="content-panel quiz-panel">
        <LessonClassQuiz
          v-if="workspacePlan.quiz"
          :quiz="workspacePlan.quiz"
          :submitting="submitting === externalTask?.id"
          @submit="submitQuiz"
        />
      </section>
      <section v-show="isLockedPanel" class="content-panel empty-panel">
        <el-empty description="该内容尚未解锁，请先完成前置活动" :image-size="88" />
      </section>
    </main>

    <el-dialog v-model="showRanking" title="积分榜（本班）" width="420px">
      <p class="rank-hint">当前积分：<strong>{{ totalPoints }}</strong> 分</p>
      <el-button type="primary" link @click="router.push('/student/records')">查看学习记录 →</el-button>
    </el-dialog>
  </div>

  <!-- 同步发布：左侧导航 + 右侧内容（旧版五板块） -->
  <div v-else-if="sidebarPlan" class="lesson-workspace">
    <header class="ws-bar">
      <el-button link type="primary" @click="goBack">← 返回课程</el-button>
      <span class="ws-title">{{ sidebarPlan.lessonTitle || lesson?.title }}</span>
    </header>
    <div class="ws-body">
      <nav class="ws-nav">
        <button type="button" class="nav-item" :class="{ active: activePanel === 'objectives' }" @click="activePanel = 'objectives'">学习目标</button>
        <button v-for="act in sidebarPlan.activities || []" :key="act.index" type="button" class="nav-item" :class="{ active: activePanel === 'act-' + act.index }" @click="activePanel = 'act-' + act.index">{{ activityMenuLabel(act) }}</button>
        <button type="button" class="nav-item" :class="{ active: activePanel === 'evaluation' }" @click="activePanel = 'evaluation'">学习评价</button>
      </nav>
      <main class="ws-main">
        <section v-show="activePanel === 'objectives'" class="text-panel">
          <h2>学习目标</h2>
          <div class="text-body">{{ sidebarPlan.objectives }}</div>
        </section>
        <section v-for="act in sidebarPlan.activities || []" :key="'panel-' + act.index" v-show="activePanel === 'act-' + act.index" class="act-panel">
          <iframe :src="activityIframeSrc(act)" class="act-frame" :title="activityMenuLabel(act)" />
        </section>
        <section v-show="activePanel === 'evaluation'" class="text-panel">
          <h2>学习评价</h2>
          <div class="text-body">{{ sidebarPlan.evaluation }}</div>
        </section>
      </main>
    </div>
  </div>

  <div v-else-if="immersiveUrl" class="immersive-wrap">
    <header class="immersive-bar">
      <el-button link type="primary" @click="goBack">← 返回课程</el-button>
      <span class="immersive-title">{{ lesson?.title }}</span>
      <el-tag v-if="taskSubmitted" type="success" size="small">已提交</el-tag>
    </header>
    <iframe :src="immersiveUrl" class="immersive-frame" title="课程探究单" />
  </div>

  <div class="lesson-page" v-else-if="lesson">
    <el-page-header @back="goBack" :content="lesson.title" />
    <el-card class="section"><div v-html="lesson.content"></div></el-card>
    <el-card class="section" v-if="resources.length">
      <template #header>学习资料</template>
      <div v-for="res in resources" :key="res.id" class="resource-item">
        <h4>{{ res.title }}</h4>
        <div v-if="res.contentText" v-html="res.contentText"></div>
        <el-link v-if="res.contentUrl" :href="res.contentUrl" target="_blank">打开链接</el-link>
      </div>
    </el-card>
    <el-card class="section" v-if="visibleTasks.length">
      <template #header>互动学习任务</template>
      <div v-for="task in visibleTasks" :key="task.id" class="task-item">
        <h4>{{ task.title }}</h4>
        <p>{{ task.description }}</p>
        <el-button type="primary" @click="submit(task)" :loading="submitting === task.id">提交任务</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getLesson, submitTask, recordVisit, getMyRecords, getMe } from '../../api'
import LessonClassQuiz from '../../components/student/LessonClassQuiz.vue'
import LessonSelfEvaluation from '../../components/student/LessonSelfEvaluation.vue'
import LessonQuickNav from '../../components/lesson/LessonQuickNav.vue'
import LessonRecordsPanel from '../../components/student/LessonRecordsPanel.vue'
import LessonIntroMindMap from '../../components/student/LessonIntroMindMap.vue'
import PdfViewer from '../../components/PdfViewer.vue'
import { buildLessonIntroMap } from '../../utils/buildLessonIntroMap'

const route = useRoute()
const router = useRouter()
const isTeacherPreview = computed(() =>
  route.meta.teacherPreview === true || /^\/teacher\/lesson\/\d+$/.test(route.path)
)
const quickNavItems = computed(() => {
  if (!isTeacherPreview.value) return undefined
  return [{ key: 'intro', label: '课程介绍', icon: '📖', tone: 'purple' }]
})
const lesson = ref(null)
const resources = ref([])
const tasks = ref([])
const textbookPdfUrl = ref('')
const readingCompleted = ref(false)
const answers = reactive({})
const submitting = ref(null)
const taskSubmitted = ref(false)
const activePanel = ref('intro')
const totalPoints = ref(0)
const studentNo = ref('')
const studentName = ref('')
const className = ref('')
const signedToday = ref(false)
const showRanking = ref(false)
const recordsPanelRef = ref(null)
const completedActivities = ref(new Set())
const quizSubmitted = ref(false)
const evaluationSubmitted = ref(false)
let startTime = Date.now()
let visitTimer = null

const MSG_TYPE = 'LEARN_SPACE_SUBMIT'

const parseExternalPath = (task) => {
  try {
    const cfg = JSON.parse(task.configJson || '{}')
    return cfg.path || cfg.url || null
  } catch {
    return null
  }
}

const externalTask = computed(() => tasks.value.find((t) => t.taskType === 'EXTERNAL'))

const hasTextbook = computed(() => !!textbookPdfUrl.value)

const readingUnlocked = computed(() => isTeacherPreview.value || hasTextbook.value)

const introUnlocked = computed(() =>
  isTeacherPreview.value || !hasTextbook.value || readingCompleted.value
)

const workspacePlan = computed(() => {
  const task = externalTask.value
  if (!task) return null
  try {
    const cfg = JSON.parse(task.configJson || '{}')
    if (cfg.layout === 'student_workspace') return cfg
  } catch { /* ignore */ }
  return null
})

const sidebarPlan = computed(() => {
  const task = externalTask.value
  if (!task || workspacePlan.value) return null
  try {
    const cfg = JSON.parse(task.configJson || '{}')
    if (cfg.layout === 'sidebar' && cfg.activities?.length) return cfg
  } catch { /* ignore */ }
  return null
})

const introMindMap = computed(() => {
  if (!workspacePlan.value) return null
  return buildLessonIntroMap({
    lessonTitle: workspacePlan.value.lessonTitle || lesson.value?.title,
    contentHtml: lesson.value?.content,
    objectives: workspacePlan.value.objectives,
    activities: workspacePlan.value.activities,
    introMap: workspacePlan.value.introMap,
    hasQuiz: !!workspacePlan.value.quiz
  })
})

const unlockedActivities = computed(() =>
  (workspacePlan.value?.activities || []).filter((a) => isActivityUnlocked(a))
)

const quizUnlocked = computed(() => {
  const q = workspacePlan.value?.quiz
  if (!q) return false
  if (isTeacherPreview.value) return (q.questions?.length || 0) > 0
  if (q.unlocked === false) {
    const need = q.unlockAfterActivity || 3
    return completedActivities.value.has(need) || [...completedActivities.value].some((i) => i >= need)
  }
  return q.unlocked !== false && (q.questions?.length > 0)
})

const currentActivity = computed(() => {
  if (!activePanel.value.startsWith('act-')) return null
  const idx = parseInt(activePanel.value.replace('act-', ''), 10)
  return (workspacePlan.value?.activities || []).find((a) => a.index === idx) || null
})

const currentActivityTitle = computed(() => {
  const act = currentActivity.value
  if (!act) return '探究'
  return activityMenuLabel(act)
})

const currentIframeSrc = computed(() => {
  const act = currentActivity.value
  if (!act?.path) return ''
  const step = act.step || act.index
  const sep = act.path.includes('?') ? '&' : '?'
  return `${act.path}${sep}step=${step}&activityIndex=${act.index}&embedded=1`
})

const isActivityUnlocked = (act) => {
  if (!act) return false
  if (isTeacherPreview.value) return !!act.path
  if (hasTextbook.value && !readingCompleted.value) return false
  if (act.unlocked === false) {
    const prev = act.index - 1
    return prev < 1 || completedActivities.value.has(prev)
  }
  return act.unlocked !== false && !!act.path
}

const isLockedPanel = computed(() => {
  if (!workspacePlan.value) return false
  const p = activePanel.value
  if (!p.startsWith('act-')) return false
  const idx = parseInt(p.replace('act-', ''), 10)
  const act = (workspacePlan.value.activities || []).find((a) => a.index === idx)
  return act && !isActivityUnlocked(act)
})

const immersiveUrl = computed(() => {
  if (sidebarPlan.value || workspacePlan.value) return null
  const web = resources.value.find((r) => (r.resType || r.res_type) === 'WEB' && (r.contentUrl || r.content_url))
  if (web) return web.contentUrl || web.content_url
  const ext = externalTask.value
  if (ext) return parseExternalPath(ext)
  if (lesson.value?.title === '第8课 体验控制系统') return '/lessons/g5-lesson8/index.html'
  return null
})

const visibleTasks = computed(() => tasks.value.filter((t) => t.taskType !== 'EXTERNAL'))

const chineseNum = (n) => ({ 1: '一', 2: '二', 3: '三', 4: '四' }[n] || n)

const inquiryActivityLabel = (index) => `探究${chineseNum(index)}`

const activityMenuLabel = (act) => `${inquiryActivityLabel(act.index)}：${act.title}`

const activityIframeSrc = (act) => {
  const path = act.path || ''
  const step = act.step || act.index
  const sep = path.includes('?') ? '&' : '?'
  return `${path}${sep}step=${step}&activityIndex=${act.index}&embedded=1`
}

const selectActivity = (act) => {
  if (hasTextbook.value && !readingCompleted.value && !isTeacherPreview.value) {
    ElMessage.info('请先完成教材阅读')
    activePanel.value = 'reading'
    return
  }
  if (!isActivityUnlocked(act)) {
    ElMessage.info('请先完成前面的活动')
    return
  }
  activePanel.value = 'act-' + act.index
}

const openReading = () => {
  if (!readingUnlocked.value) return
  activePanel.value = 'reading'
}

const openIntro = () => {
  if (!introUnlocked.value) {
    ElMessage.info('请先完成教材阅读')
    activePanel.value = 'reading'
    return
  }
  activePanel.value = 'intro'
}

const completeReading = async () => {
  if (isTeacherPreview.value) {
    readingCompleted.value = true
    activePanel.value = 'intro'
    return
  }
  if (readingCompleted.value) {
    activePanel.value = 'intro'
    return
  }
  await submitExternal({ type: 'reading', stepCompleted: true, readingCompleted: true })
  readingCompleted.value = true
  activePanel.value = 'intro'
}

const quickNavKey = computed(() => {
  if (activePanel.value === 'reading') return 'reading'
  if (activePanel.value === 'intro') return 'intro'
  if (activePanel.value === 'records') return 'records'
  if (activePanel.value === 'profile') return 'profile'
  if (showRanking.value) return 'ranking'
  return ''
})

const onQuickNav = (key) => {
  showRanking.value = false
  if (key === 'intro') activePanel.value = 'intro'
  else if (key === 'records') {
    if (isTeacherPreview.value) {
      ElMessage.info('学习记录仅学生端可见')
      return
    }
    activePanel.value = 'records'
    recordsPanelRef.value?.reload?.()
  } else if (key === 'profile') activePanel.value = 'profile'
  else if (key === 'ranking') {
    if (isTeacherPreview.value) {
      ElMessage.info('积分榜仅学生端可见')
      return
    }
    showRanking.value = true
  }
}

const goActivityFromRecords = (act) => {
  if (act.type === 'READING') {
    openReading()
    return
  }
  if (act.type === 'QUIZ') {
    openQuiz()
    return
  }
  if (act.type === 'EVALUATION') {
    activePanel.value = 'evaluation'
    return
  }
  const found = (workspacePlan.value?.activities || []).find((a) => a.index === act.index)
  if (found) selectActivity(found)
}

const refreshPoints = async () => {
  const recordsRes = await getMyRecords().catch(() => ({ data: { totalPoints: 0 } }))
  totalPoints.value = recordsRes.data?.totalPoints || 0
  recordsPanelRef.value?.reload?.()
}

const openEvaluation = () => {
  if (hasTextbook.value && !readingCompleted.value && !isTeacherPreview.value) {
    ElMessage.info('请先完成教材阅读')
    activePanel.value = 'reading'
    return
  }
  activePanel.value = 'evaluation'
}

const openQuiz = () => {
  if (hasTextbook.value && !readingCompleted.value && !isTeacherPreview.value) {
    ElMessage.info('请先完成教材阅读')
    activePanel.value = 'reading'
    return
  }
  if (!quizUnlocked.value) {
    ElMessage.info('课堂小测尚未开放')
    return
  }
  activePanel.value = 'quiz'
}

const applySubmissionState = (contentJson) => {
  if (!contentJson) return
  try {
    const data = JSON.parse(contentJson)
    if (data.type === 'reading' || data.readingCompleted) {
      readingCompleted.value = true
    }
    if (data.type === 'quiz' || data.quizScore != null) {
      quizSubmitted.value = true
    }
    if (data.type === 'evaluation' || data.evaluationAnswers) {
      evaluationSubmitted.value = true
    }
    if (data.completedActivities && Array.isArray(data.completedActivities)) {
      data.completedActivities.forEach((i) => completedActivities.value.add(i))
    }
    if (data.activityIndex && data.type !== 'quiz' && data.type !== 'evaluation' && data.activityIndex < 10) {
      completedActivities.value.add(data.activityIndex)
    }
  } catch { /* ignore */ }
}

const parseExistingSubmission = () => {
  const raw = externalTask.value?.studentSubmission?.contentJson
  if (!raw) return {}
  try {
    return JSON.parse(raw)
  } catch {
    return {}
  }
}

const signinKey = () => {
  const u = studentNo.value || 'student'
  const d = new Date().toISOString().slice(0, 10)
  return `learn_signin_${u}_${d}`
}

const toggleSignIn = () => {
  if (signedToday.value) return
  localStorage.setItem(signinKey(), '1')
  signedToday.value = true
  totalPoints.value += 2
  ElMessage.success('签到成功，+2 积分')
}

const goBack = () => {
  if (isTeacherPreview.value) {
    const gradeId = route.query.gradeId
    if (gradeId) router.push(`/teacher/course-map/grade/${gradeId}`)
    else router.push('/teacher/course-map')
    return
  }
  router.back()
}

const previewCompleteActivity = (payload) => {
  if (payload.activityIndex && payload.type !== 'quiz' && payload.type !== 'evaluation' && payload.activityIndex < 10) {
    completedActivities.value.add(payload.activityIndex)
  }
  if (payload.type === 'quiz') {
    quizSubmitted.value = true
    ElMessage.success(`教师预览：小测作答完成（得分 ${payload.quizScore ?? '-'}，未提交）`)
    return
  }
  if (payload.type === 'evaluation') {
    evaluationSubmitted.value = true
    ElMessage.success('教师预览：学习评价已填写（未提交）')
    return
  }
  if (payload.stepCompleted && payload.activityIndex) {
    ElMessage.success(`${inquiryActivityLabel(payload.activityIndex)}完成（教师预览，未提交）`)
    return
  }
  ElMessage.success('教师预览：活动进度已记录（未提交）')
}

const submitExternal = async (payload) => {
  if (isTeacherPreview.value) {
    previewCompleteActivity(payload)
    return
  }
  const task = externalTask.value
  if (!task) return
  if (submitting.value === task.id) return
  submitting.value = task.id
  try {
    const existing = parseExistingSubmission()
    const merged = { ...existing, ...payload }
    if (merged.activityIndex && merged.type !== 'quiz' && merged.type !== 'evaluation' && merged.activityIndex < 10) {
      completedActivities.value.add(merged.activityIndex)
    }
    if (merged.type === 'reading') {
      readingCompleted.value = true
      merged.readingCompleted = true
    }
    if (merged.type === 'quiz') {
      quizSubmitted.value = true
    }
    if (merged.type === 'evaluation') {
      evaluationSubmitted.value = true
      merged.evaluationAnswers = payload.answers
      delete merged.activityIndex
    }
    merged.completedActivities = [...completedActivities.value]
    const studySeconds = Math.floor((Date.now() - startTime) / 1000)
    await submitTask({
      taskId: task.id,
      contentJson: JSON.stringify({ ...merged, submittedAt: new Date().toISOString() }),
      studySeconds
    })
    taskSubmitted.value = true
    if (merged.type === 'quiz') {
      ElMessage.success(`小测已提交，得分 ${merged.quizScore} / 6 分`)
    } else if (merged.type === 'reading') {
      ElMessage.success('教材阅读已记录')
    } else if (merged.stepCompleted && merged.activityIndex) {
      const label = inquiryActivityLabel(merged.activityIndex)
      ElMessage.success(`${label}完成，+2 积分`)
    } else if (merged.type !== 'evaluation') {
      ElMessage.success('活动进度已保存')
    }
    await refreshPoints()
  } finally {
    submitting.value = null
  }
}

const submitQuiz = (payload) => submitExternal(payload)
const submitEvaluation = (payload) => submitExternal(payload)

const onLearnMessage = (event) => {
  if (!event?.data || event.data.type !== MSG_TYPE) return
  const payload = { ...(event.data.payload || {}) }
  if (!payload.activityIndex && activePanel.value.startsWith('act-')) {
    payload.activityIndex = parseInt(activePanel.value.replace('act-', ''), 10)
  }
  if (isTeacherPreview.value) {
    previewCompleteActivity(payload)
    return
  }
  submitExternal(payload)
}

const submit = async (task) => {
  if (isTeacherPreview.value) {
    ElMessage.info('教师预览模式下无需提交任务')
    return
  }
  submitting.value = task.id
  try {
    const studySeconds = Math.floor((Date.now() - startTime) / 1000)
    await submitTask({ taskId: task.id, contentJson: JSON.stringify(answers[task.id]), studySeconds })
    ElMessage.success('提交成功')
  } finally {
    submitting.value = null
  }
}

onMounted(async () => {
  window.addEventListener('message', onLearnMessage)

  const lessonRes = await getLesson(route.params.id)
  lesson.value = lessonRes.data.lesson
  resources.value = lessonRes.data.resources || []
  tasks.value = lessonRes.data.tasks || []
  textbookPdfUrl.value = lessonRes.data.textbookPdfUrl || ''
  tasks.value.forEach((t) => { answers[t.id] = {} })

  if (!isTeacherPreview.value) {
    const [meRes, recordsRes] = await Promise.all([
      getMe().catch(() => ({ data: {} })),
      getMyRecords().catch(() => ({ data: { totalPoints: 0 } }))
    ])
    studentNo.value = meRes.data?.username || localStorage.getItem('username') || ''
    studentName.value = meRes.data?.realName || localStorage.getItem('realName') || ''
    className.value = meRes.data?.className || ''
    totalPoints.value = recordsRes.data?.totalPoints || 0
    signedToday.value = localStorage.getItem(signinKey()) === '1'
  } else {
    studentName.value = localStorage.getItem('realName') || '教师'
  }

  if (workspacePlan.value) {
    const task = externalTask.value
    const sub = task?.studentSubmission
    if (sub?.contentJson && !isTeacherPreview.value) {
      applySubmissionState(sub.contentJson)
    }
    const stepQuery = route.query.step
    if (stepQuery != null && stepQuery !== '') {
      const idx = parseInt(String(stepQuery), 10)
      if (!Number.isNaN(idx) && idx >= 1) {
        activePanel.value = 'act-' + idx
      } else if (String(stepQuery) === 'intro') {
        activePanel.value = introUnlocked.value ? 'intro' : 'reading'
      } else if (String(stepQuery) === 'reading') {
        activePanel.value = 'reading'
      }
    } else if (hasTextbook.value && !readingCompleted.value && !isTeacherPreview.value) {
      activePanel.value = 'reading'
    } else {
      activePanel.value = 'intro'
    }
  } else if (sidebarPlan.value) {
    activePanel.value = 'objectives'
  }

  if (!isTeacherPreview.value) {
    recordVisit({ lessonId: Number(route.params.id), pageUrl: route.path, durationSec: 0 })
    visitTimer = setInterval(() => {
      recordVisit({ lessonId: Number(route.params.id), pageUrl: route.path, durationSec: 30 })
    }, 30000)
  }
})

onUnmounted(() => {
  window.removeEventListener('message', onLearnMessage)
  if (visitTimer) clearInterval(visitTimer)
  if (!isTeacherPreview.value) {
    const sec = Math.floor((Date.now() - startTime) / 1000)
    recordVisit({ lessonId: Number(route.params.id), pageUrl: route.path, durationSec: sec })
  }
})
</script>

<style scoped>
.lesson-studio {
  height: 100%;
  min-height: 0;
  display: flex;
  background: linear-gradient(180deg, #e8ecf4 0%, #eef1f8 100%);
}
.studio-left {
  width: 280px;
  flex-shrink: 0;
  min-height: 0;
  background: linear-gradient(180deg, #fafbfd 0%, #f1f5f9 100%);
  border-right: 1px solid rgba(148, 163, 184, 0.35);
  box-shadow: 4px 0 24px rgba(15, 23, 42, 0.04);
  display: flex;
  flex-direction: column;
  padding: 0 0 12px;
  overflow-y: auto;
}
.lesson-title-bar {
  background: linear-gradient(135deg, #3730a3 0%, #4f46e5 55%, #6366f1 100%);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  padding: 14px 14px;
  line-height: 1.45;
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.25);
}
.records-panel-wrap { overflow: auto; }
.menu-list {
  padding: 0 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  text-align: left;
  padding: 10px 12px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 10px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  font-size: 13px;
  color: #334155;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
  transition: border-color 0.2s, box-shadow 0.2s;
}
.menu-item:hover:not(.locked) {
  border-color: rgba(99, 102, 241, 0.35);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.1);
}
.menu-item.active {
  border-color: #6366f1;
  background: linear-gradient(135deg, #eef2ff 0%, #f5f3ff 100%);
  color: #4338ca;
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.12);
}
.menu-item.locked {
  opacity: 0.72;
  cursor: not-allowed;
}
.lock-tag {
  font-size: 11px;
  color: #94a3b8;
  white-space: nowrap;
}
.done-tag {
  font-size: 11px;
  color: #10b981;
  font-weight: 700;
}
.feature-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 12px 10px 0;
}
.feature-btn {
  padding: 8px 6px;
  border: 1px dashed #c7d2e3;
  border-radius: 8px;
  background: #fff;
  font-size: 12px;
  color: #64748b;
  cursor: pointer;
}
.feature-btn:hover { border-color: #7c6fe0; color: #5b4fc7; }
.points-row {
  display: flex;
  justify-content: space-between;
  padding: 12px 14px 8px;
  font-size: 13px;
  color: #64748b;
}
.points-row strong { color: #5b4fc7; font-size: 16px; }
.signin-btn {
  margin: 0 10px;
  padding: 10px;
  border: none;
  border-radius: 8px;
  background: #5b4fc7;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}
.signin-btn.done { background: #10b981; }
.student-footer {
  margin-top: auto;
  padding: 12px 14px 4px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.6;
  border-top: 1px solid #e8edf4;
}
.teacher-preview-bar {
  margin: 12px 10px 0;
  padding: 12px;
  border-radius: 10px;
  background: #fffbeb;
  border: 1px dashed #fcd34d;
}
.teacher-preview-bar p {
  margin: 8px 0;
  font-size: 12px;
  line-height: 1.55;
  color: #92400e;
}
.studio-right {
  flex: 1;
  min-width: 0;
  min-height: 0;
  background: #fff;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: inset 1px 0 0 rgba(255, 255, 255, 0.6);
}
.reading-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.reading-head p {
  margin: 6px 0 0;
  font-size: 13px;
  font-weight: 400;
  color: #64748b;
}
.reading-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.reading-footer {
  flex-shrink: 0;
  padding: 14px 20px;
  border-top: 1px solid #e2e8f0;
  background: linear-gradient(180deg, #fafbfd 0%, #f8fafc 100%);
  text-align: center;
}
.content-panel {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
.intro-panel { padding: 20px 24px; }
.iframe-panel {
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.activity-panel-head {
  flex-shrink: 0;
  padding: 14px 20px;
  background: linear-gradient(90deg, #f8fafc 0%, #fff 100%);
  border-bottom: 1px solid #e2e8f0;
}
.activity-panel-head h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #1e3a8a;
}
.content-frame {
  flex: 1;
  width: 100%;
  min-height: 0;
  border: none;
  display: block;
}
.quiz-panel { overflow: auto; }
.eval-panel { overflow: auto; }
.empty-panel { display: flex; align-items: center; justify-content: center; height: 100%; }

.lesson-workspace {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #f1f5f9;
}
.ws-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 16px; background: #fff; border-bottom: 1px solid #e2e8f0;
}
.ws-title { flex: 1; font-weight: 600; font-size: 15px; }
.ws-body { flex: 1; display: flex; min-height: 0; }
.ws-nav {
  width: 168px; background: #fff; border-right: 1px solid #e2e8f0;
  padding: 12px 8px; display: flex; flex-direction: column; gap: 6px;
}
.nav-item {
  text-align: left; padding: 10px 12px; border: none; border-radius: 8px;
  background: transparent; color: #475569; font-size: 14px; cursor: pointer;
}
.nav-item.active { background: #eff6ff; color: #2563eb; font-weight: 600; }
.ws-main { flex: 1; min-width: 0; background: #fff; overflow: hidden; }
.text-panel { padding: 24px 28px; height: 100%; overflow-y: auto; }
.text-body { white-space: pre-wrap; line-height: 1.75; }
.act-panel { height: 100%; }
.act-frame { width: 100%; height: 100%; border: none; }
.immersive-wrap {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #f5f6fa;
}
.immersive-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8;
}
.immersive-title { flex: 1; font-weight: 600; font-size: 15px; }
.immersive-frame { flex: 1; width: 100%; border: none; }
.lesson-page { max-width: 900px; margin: 0 auto; }
.section { margin-top: 20px; }
.resource-item, .task-item { margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #eee; }
</style>
