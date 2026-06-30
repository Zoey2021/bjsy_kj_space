<template>
  <!-- 学生工作台：左侧课程信息 + 右侧内容（六年级上第1课等） -->
  <div v-if="workspacePlan" class="lesson-studio">
    <aside class="studio-left">
      <div class="lesson-title-bar">{{ workspacePlan.lessonTitle || lesson?.title }}</div>

      <LessonQuickNav :active-key="quickNavKey" @select="onQuickNav" />

      <div class="menu-list">
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
          @click="activePanel = 'evaluation'"
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

      <div class="feature-row">
        <button type="button" class="feature-btn" @click="ElMessage.info('班级学习大屏即将开放')">班级学习大屏</button>
        <button type="button" class="feature-btn" @click="router.push('/student/ai')">AI评价中心</button>
      </div>

      <div class="points-row">
        <span>排名积分 <strong>{{ totalPoints }}</strong></span>
        <span>可兑奖 <strong>{{ totalPoints }}</strong></span>
      </div>
      <button type="button" class="signin-btn" :class="{ done: signedToday }" @click="toggleSignIn">
        {{ signedToday ? '今日已签到' : '今日签到' }}
      </button>

      <div class="student-footer">
        <div>{{ className || '我的班级' }}</div>
        <div>学号 {{ studentNo }} · {{ studentName }}</div>
      </div>
    </aside>

    <main class="studio-right">
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
        <LessonRecordsPanel
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
import { buildLessonIntroMap } from '../../utils/buildLessonIntroMap'

const route = useRoute()
const router = useRouter()
const lesson = ref(null)
const resources = ref([])
const tasks = ref([])
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
  if (!isActivityUnlocked(act)) {
    ElMessage.info('请先完成前面的活动')
    return
  }
  activePanel.value = 'act-' + act.index
}

const quickNavKey = computed(() => {
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
    activePanel.value = 'records'
    recordsPanelRef.value?.reload?.()
  } else if (key === 'profile') activePanel.value = 'profile'
  else if (key === 'ranking') showRanking.value = true
}

const goActivityFromRecords = (act) => {
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

const openQuiz = () => {
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

const goBack = () => router.back()

const submitExternal = async (payload) => {
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
  submitExternal(payload)
}

const submit = async (task) => {
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

  const [lessonRes, meRes, recordsRes] = await Promise.all([
    getLesson(route.params.id),
    getMe().catch(() => ({ data: {} })),
    getMyRecords().catch(() => ({ data: { totalPoints: 0 } }))
  ])

  lesson.value = lessonRes.data.lesson
  resources.value = lessonRes.data.resources || []
  tasks.value = lessonRes.data.tasks || []
  tasks.value.forEach((t) => { answers[t.id] = {} })

  studentNo.value = meRes.data?.username || localStorage.getItem('username') || ''
  studentName.value = meRes.data?.realName || localStorage.getItem('realName') || ''
  className.value = meRes.data?.className || ''
  totalPoints.value = recordsRes.data?.totalPoints || 0
  signedToday.value = localStorage.getItem(signinKey()) === '1'

  if (workspacePlan.value) {
    const task = externalTask.value
    const sub = task?.studentSubmission
    if (sub?.contentJson) {
      applySubmissionState(sub.contentJson)
    }
    activePanel.value = 'intro'
  } else if (sidebarPlan.value) {
    activePanel.value = 'objectives'
  }

  recordVisit({ lessonId: Number(route.params.id), pageUrl: route.path, durationSec: 0 })
  visitTimer = setInterval(() => {
    recordVisit({ lessonId: Number(route.params.id), pageUrl: route.path, durationSec: 30 })
  }, 30000)
})

onUnmounted(() => {
  window.removeEventListener('message', onLearnMessage)
  if (visitTimer) clearInterval(visitTimer)
  const sec = Math.floor((Date.now() - startTime) / 1000)
  recordVisit({ lessonId: Number(route.params.id), pageUrl: route.path, durationSec: sec })
})
</script>

<style scoped>
.lesson-studio {
  height: 100%;
  min-height: 0;
  display: flex;
  background: #eef1f6;
}
.studio-left {
  width: 280px;
  flex-shrink: 0;
  min-height: 0;
  background: #f8fafc;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  padding: 0 0 12px;
  overflow-y: auto;
}
.lesson-title-bar {
  background: linear-gradient(135deg, #5b4fc7, #7c6fe0);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  padding: 14px 14px;
  line-height: 1.45;
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
  border: 1px solid #e8edf4;
  border-radius: 8px;
  background: #fff;
  font-size: 13px;
  color: #334155;
  cursor: pointer;
}
.menu-item.active {
  border-color: #7c6fe0;
  background: #f5f3ff;
  color: #5b4fc7;
  font-weight: 600;
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
.studio-right {
  flex: 1;
  min-width: 0;
  min-height: 0;
  background: #fff;
  overflow: hidden;
  display: flex;
  flex-direction: column;
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
