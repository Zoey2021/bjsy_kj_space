<template>
  <!-- 学生工作台：左侧课程信息 + 右侧内容（六年级上第1课等） -->
  <div v-if="workspacePlan" class="lesson-studio" :class="{ 'studio-play-fs': playFullscreen }">
    <aside class="studio-left">
      <div class="lesson-title-bar">{{ workspacePlan.lessonTitle || lesson?.title }}</div>

      <LessonQuickNav :active-key="quickNavKey" @select="onQuickNav" />

      <div class="menu-list">
        <button
          type="button"
          class="menu-item"
          :class="{ active: activePanel === 'intro' }"
          @click="openIntro"
        >
          <span class="menu-label">课程介绍</span>
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
          :class="{ active: activePanel === 'quiz', locked: !quizUnlocked }"
          @click="openQuiz"
        >
          <span class="menu-label">课堂小测</span>
          <span v-if="!quizUnlocked" class="lock-tag">需解锁</span>
          <span v-else-if="quizSubmitted" class="done-tag">已完成</span>
        </button>
        <button
          v-if="workspacePlan.evaluationForm"
          type="button"
          class="menu-item"
          :class="{ active: activePanel === 'evaluation' }"
          @click="openEvaluation"
        >
          <span class="menu-label">学习评价</span>
          <span v-if="evaluationSubmitted" class="done-tag">已完成</span>
        </button>
      </div>

      <div class="submit-actions">
        <button type="button" class="save-progress-btn" :disabled="savingProgress" @click="saveProgress">
          {{ savingProgress ? '正在保存…' : '保存进度' }}
        </button>
        <button
          type="button"
          class="hand-in-btn"
          :class="{ ready: canHandIn }"
          :disabled="!canHandIn || handingIn"
          @click="confirmHandIn"
        >
          {{ handedIn ? '已确认上交' : '确认上交' }}
        </button>
        <p class="hand-in-hint">{{ handInHint }}</p>
      </div>

      <div class="feature-row">
        <button type="button" class="feature-btn" @click="showClassScreen = true">班级学习大屏</button>
        <button type="button" class="feature-btn" @click="openMall">兑换商城</button>
        <button v-if="isTeacherPreview" type="button" class="feature-btn" @click="router.push('/teacher/student-view/park')">游学乐园</button>
      </div>

      <template v-if="!isTeacherPreview">
        <div class="points-row">
          <span>排名积分 <strong :class="{ 'points-bump': pointsBumping }">{{ earnedPoints }}</strong></span>
          <span>可兑奖 <strong :class="{ 'points-bump': pointsBumping }">{{ redeemablePoints }}</strong></span>
        </div>
        <button type="button" class="signin-btn" :class="{ done: signedToday }" @click="toggleSignIn">
          {{ signedToday ? '今日已签到' : '今日签到' }}
        </button>
      </template>

      <div v-if="isTeacherPreview" class="teacher-preview-bar">
        <el-tag type="warning" effect="plain" size="small">教师预览</el-tag>
        <p>六年级课时的探究页面上方有「预览支架」，可切换 C、B、A，查看各档页面。学生仍然只看到自己的那一档。提交和兑换不会写进学生数据。</p>
        <el-button type="primary" link @click="goBack">← 返回教材目录</el-button>
      </div>

      <div v-if="!isTeacherPreview" class="student-footer">
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
          <h2 v-if="showActivityHeadTitle">{{ activityMenuLabel(currentActivity) }}</h2>
          <span v-else class="activity-head-spacer"></span>
          <div class="head-actions">
            <div v-if="showTeacherTierSwitch" class="tier-switch" role="group" aria-label="查看支架档位">
              <span>支架</span>
              <button
                v-for="code in ['A', 'B', 'C']"
                :key="code"
                type="button"
                :class="{ on: teacherScaffold === code }"
                @click="teacherScaffold = code"
              >
                {{ code }}档
              </button>
            </div>
            <el-button
              v-if="currentIframeSrc"
              size="small"
              @click="refreshActivity(currentActivity.index)"
            >
              刷新本栏
            </el-button>
            <el-button
              v-if="currentIframeSrc"
              type="primary"
              plain
              size="small"
              @click="togglePlayFullscreen"
            >
              {{ playFullscreen ? '退出全屏' : '全屏操作' }}
            </el-button>
          </div>
        </header>
        <ActivityScaffoldPanel
          v-if="currentScaffoldContent && !playFullscreen && !hideInlineScaffold"
          :lesson-id="visitLessonId"
          :activity-index="currentActivity.index"
          :content="currentScaffoldContent"
          :preview="isTeacherPreview"
        />
        <div ref="playStageRef" class="play-stage" :class="{ 'is-fs': playFullscreen }">
          <div v-if="playFullscreen" class="play-fs-bar">
            <span>{{ currentActivityTitle }}</span>
            <div class="head-actions">
              <div v-if="showTeacherTierSwitch" class="tier-switch" role="group" aria-label="查看支架档位">
                <span>支架</span>
                <button
                  v-for="code in ['A', 'B', 'C']"
                  :key="'fs-' + code"
                  type="button"
                  :class="{ on: teacherScaffold === code }"
                  @click="teacherScaffold = code"
                >
                  {{ code }}档
                </button>
              </div>
              <el-button size="small" @click="refreshActivity(currentActivity.index)">刷新本栏</el-button>
              <el-button type="primary" size="small" @click="togglePlayFullscreen">退出全屏</el-button>
            </div>
          </div>
          <div v-if="currentRestorePending" class="restore-bar">
            <span>这一栏有你刚才填写的内容。要恢复吗？只影响这一栏。</span>
            <el-button type="primary" size="small" @click="acceptActivityRestore">恢复刚才的操作</el-button>
            <el-button size="small" @click="declineActivityRestore">重新开始这一栏</el-button>
          </div>
          <iframe
            v-for="act in iframeActivities"
            v-show="activePanel === 'act-' + act.index"
            :key="'frame-' + act.index + '-' + (frameNonce[act.index] || 0)"
            :ref="(el) => setActivityFrame(act.index, el)"
            :src="activityIframeSrc(act)"
            class="content-frame"
            :title="activityMenuLabel(act)"
            @load="onActivityFrameLoad(act)"
          />
        </div>
      </section>
      <section v-show="activePanel === 'records'" class="content-panel records-panel-wrap">
        <LessonRecordsPanel
          ref="recordsPanelRef"
          :lesson-id="Number(route.params.id)"
          @go-activity="goActivityFromRecords"
        />
      </section>
      <section v-show="activePanel === 'profile'" class="content-panel profile-panel">
        <StudentProfile
          :class-name="className"
          :student-no="studentNo"
          :student-name="studentName"
          @points="onProfilePoints"
        />
      </section>
      <section v-show="activePanel === 'evaluation'" class="content-panel eval-panel">
        <div v-if="workspacePlan.evaluationForm && !isTeacherPreview" class="section-tools">
          <el-button size="small" @click="refreshEvaluation">刷新本栏</el-button>
        </div>
        <div v-if="evalRestorePending" class="restore-bar">
          <span>学习评价有你刚才选过的内容。要恢复吗？</span>
          <el-button type="primary" size="small" @click="acceptEvalRestore">恢复刚才的操作</el-button>
          <el-button size="small" @click="declineEvalRestore">重新开始这一栏</el-button>
        </div>
        <LessonSelfEvaluation
          v-if="workspacePlan.evaluationForm"
          :key="'eval-' + evalMountKey"
          :evaluation="workspacePlan.evaluationForm"
          :seed="evalSeed"
          :submitting="submitting === externalTask?.id"
          @draft="onEvalDraft"
          @submit="submitEvaluation"
        />
        <div v-else class="intro-panel">
          <h2>学习评价</h2>
          <p class="objectives">{{ workspacePlan.evaluationHint || '请按教师要求完成自评。' }}</p>
        </div>
      </section>
      <section v-show="activePanel === 'quiz'" class="content-panel quiz-panel">
        <div v-if="workspacePlan.quiz && !isTeacherPreview" class="section-tools">
          <el-button size="small" @click="refreshQuiz">刷新本栏</el-button>
        </div>
        <div v-if="quizRestorePending" class="restore-bar">
          <span>课堂小测有你刚才选过的答案。要恢复吗？</span>
          <el-button type="primary" size="small" @click="acceptQuizRestore">恢复刚才的操作</el-button>
          <el-button size="small" @click="declineQuizRestore">重新开始这一栏</el-button>
        </div>
        <LessonClassQuiz
          v-if="workspacePlan.quiz"
          :key="'quiz-' + quizMountKey"
          :quiz="workspacePlan.quiz"
          :seed="quizSeed"
          :submitting="submitting === externalTask?.id"
          @draft="onQuizDraft"
          @submit="submitQuiz"
        />
      </section>
      <section v-show="isLockedPanel" class="content-panel empty-panel">
        <el-empty description="该内容尚未解锁，请先完成前置活动" :image-size="88" />
      </section>
    </main>

    <ClassLearningScreen v-model:visible="showClassScreen" :lesson-id="visitLessonId" />
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
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getLesson, submitTask, recordVisit, getMyRecords, getMe, getMyPretestG6, getLessonContent } from '../../api'
import LessonClassQuiz from '../../components/student/LessonClassQuiz.vue'
import LessonSelfEvaluation from '../../components/student/LessonSelfEvaluation.vue'
import LessonQuickNav from '../../components/lesson/LessonQuickNav.vue'
import LessonRecordsPanel from '../../components/student/LessonRecordsPanel.vue'
import StudentProfile from '../../components/student/StudentProfile.vue'
import LessonIntroMindMap from '../../components/student/LessonIntroMindMap.vue'
import ClassLearningScreen from '../../components/student/ClassLearningScreen.vue'
import ActivityScaffoldPanel from '../../components/student/ActivityScaffoldPanel.vue'
import { buildLessonIntroMap } from '../../utils/buildLessonIntroMap'

const route = useRoute()
const router = useRouter()
const isTeacherPreview = computed(() =>
  route.meta.teacherPreview === true || /^\/teacher\/lesson\/\d+$/.test(route.path)
)
const lesson = ref(null)
const resources = ref([])
const tasks = ref([])
const textbookPdfUrl = ref('')
const savingProgress = ref(false)
const handingIn = ref(false)
const handedIn = ref(false)
const answers = reactive({})
const submitting = ref(null)
const taskSubmitted = ref(false)
const activePanel = ref('intro')
const totalPoints = ref(0)
const earnedPoints = ref(0)
const redeemablePoints = ref(0)
const studentNo = ref('')
const studentName = ref('')
const className = ref('')
const signedToday = ref(false)
const showClassScreen = ref(false)
const recordsPanelRef = ref(null)
const completedActivities = ref(new Set())
const quizSubmitted = ref(false)
const evaluationSubmitted = ref(false)
const pretestLevel = ref('')
const lessonContent = ref({ published: false, activities: [] })
const teacherScaffold = ref('B')
const showTeacherTierSwitch = computed(() =>
  isTeacherPreview.value && lessonContent.value?.teacherPreview === true
)
const playFullscreen = ref(false)
const playStageRef = ref(null)
const activityDrafts = ref({})
const frameNonce = reactive({})
const restorePending = reactive({})
const frameRefs = {}
const frameListeners = {}
const sessionChoice = {}
const quizSeed = ref(null)
const evalSeed = ref(null)
const quizRestorePending = ref(false)
const evalRestorePending = ref(false)
const quizMountKey = ref(0)
const evalMountKey = ref(0)
const savedQuizPayload = ref(null)
const savedEvalPayload = ref(null)
let draftTimer = null
let startTime = Date.now()
let visitTimer = null
const visitLessonId = Number(route.params.id)
const sendVisit = (durationSec) => {
  if (!Number.isFinite(visitLessonId) || visitLessonId <= 0) return
  recordVisit({
    lessonId: visitLessonId,
    pageUrl: `/student/lesson/${visitLessonId}`,
    durationSec
  }).catch(() => {})
}

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

const quizUnlocked = computed(() => (workspacePlan.value?.quiz?.questions?.length || 0) > 0)

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

const scaffoldLevel = computed(() => {
  if (showTeacherTierSwitch.value) {
    const picked = String(teacherScaffold.value || 'B').toUpperCase()
    if (picked === 'A' || picked === 'B' || picked === 'C') return picked
  }
  const q = String(route.query.scaffold || '').toUpperCase()
  if (q === 'A' || q === 'B' || q === 'C') return q
  const fromContent = String(lessonContent.value?.tier || '').toUpperCase()
  if (fromContent === 'A' || fromContent === 'B' || fromContent === 'C') return fromContent
  const lv = String(pretestLevel.value || '').toUpperCase()
  if (lv === 'A' || lv === 'B' || lv === 'C') return lv
  return 'B'
})

const currentScaffoldContent = computed(() => {
  if (!lessonContent.value?.published || !currentActivity.value) return null
  const idx = currentActivity.value.index
  const tierMap = lessonContent.value.tierActivities
  const tierList = tierMap && tierMap[scaffoldLevel.value]
  const list = Array.isArray(tierList) ? tierList : (lessonContent.value.activities || [])
  const item = list.find((a) => Number(a.activityIndex) === Number(idx))
  if (!item) return null
  const hints = Array.isArray(item.hints) ? item.hints.filter(Boolean) : []
  const traces = Array.isArray(item.traceQuestions) ? item.traceQuestions : []
  const hasText = String(item.task || '').trim() || String(item.extraText || '').trim()
  if (!hasText && !hints.length && !traces.length) return null
  return item
})

const appendActivityParams = (path, act) => {
  if (!path) return ''
  const step = act.step || act.index
  const sep = path.includes('?') ? '&' : '?'
  const preview = isTeacherPreview.value ? '&preview=1' : ''
  const cache = path.includes('/lessons/g6-up-lesson1/')
    ? '&v=20261023b'
      : path.includes('/lessons/g6-up-lesson2/')
      ? '&v=20261013c'
      : path.includes('/lessons/g6-up-lesson3/')
        ? '&v=20261023b'
      : path.includes('/lessons/g6-up-lesson4/')
        ? '&v=20261024b'
      : path.includes('/lessons/g4-up-lesson5/')
        ? '&v=20261021g'
      : path.includes('/lessons/g4-up-lesson4/')
        ? '&v=20261009b'
        : path.includes('/lessons/g4-up-lesson3/')
          ? '&v=20261009a'
          : (path.includes('/lessons/g4-up-lesson2/') ? '&v=20260920b' : '')
  return `${path}${sep}step=${step}&activityIndex=${act.index}&embedded=1&scaffold=${scaffoldLevel.value}${preview}${cache}`
}

const fsElement = () => document.fullscreenElement || document.webkitFullscreenElement
const exitNativeFs = () => {
  const exit = document.exitFullscreen || document.webkitExitFullscreen
  if (fsElement() && exit) return exit.call(document)
  return Promise.resolve()
}
const togglePlayFullscreen = async () => {
  if (playFullscreen.value) {
    playFullscreen.value = false
    try { await exitNativeFs() } catch { /* ignore */ }
    return
  }
  playFullscreen.value = true
  const el = playStageRef.value
  const req = el && (el.requestFullscreen || el.webkitRequestFullscreen)
  if (req) {
    try { await req.call(el) } catch { /* 浏览器拒绝时仍用页面内全屏 */ }
  }
}
const onFsChange = () => {
  if (!fsElement()) playFullscreen.value = false
}

watch(activePanel, () => {
  if (!playFullscreen.value) return
  playFullscreen.value = false
  exitNativeFs().catch(() => {})
})

const currentIframeSrc = computed(() => {
  const act = currentActivity.value
  if (!act?.path) return ''
  return appendActivityParams(act.path, act)
})

const hideInlineScaffold = computed(() =>
  String(currentIframeSrc.value || '').includes('/lessons/g6-up-lesson1/')
  && Number(currentActivity.value?.index) === 2
)

const showActivityHeadTitle = computed(() =>
  !String(currentActivity.value?.path || '').includes('/lessons/g6-up-lesson1/')
)

const iframeActivities = computed(() =>
  (workspacePlan.value?.activities || []).filter((act) => act?.path)
)

const currentRestorePending = computed(() => {
  const act = currentActivity.value
  if (!act || isTeacherPreview.value) return false
  return !!restorePending[act.index]
})

const setActivityFrame = (index, el) => {
  if (el) frameRefs[index] = el
  else delete frameRefs[index]
}

const cssAttr = (value) => {
  const text = String(value ?? '')
  if (window.CSS && CSS.escape) return CSS.escape(text)
  return text.replace(/["\\]/g, '\\$&')
}

const collectFrameFields = (frame) => {
  let doc = null
  try { doc = frame?.contentDocument } catch { return [] }
  if (!doc) return []
  const fields = []
  const seenRadio = new Set()
  doc.querySelectorAll('input, textarea, select').forEach((el) => {
    if (el.type === 'hidden' || el.type === 'button' || el.type === 'submit' || el.readOnly) return
    if (el.type === 'radio') {
      if (!el.checked || !el.name || seenRadio.has(el.name)) return
      seenRadio.add(el.name)
      fields.push({ kind: 'radio', name: el.name, value: el.value })
      return
    }
    if (el.type === 'checkbox') {
      fields.push({ kind: 'check', id: el.id || '', name: el.name || '', checked: !!el.checked })
      return
    }
    if (!el.id && !el.name && !el.getAttribute('data-table') && !el.getAttribute('data-h')) return
    fields.push({
      kind: 'value',
      id: el.id || '',
      name: el.name || '',
      table: el.getAttribute('data-table') || '',
      h: el.getAttribute('data-h') || '',
      value: el.value || ''
    })
  })
  return fields
}

const fieldHasContent = (fields) => (fields || []).some((f) => {
  if (f.kind === 'radio') return true
  if (f.kind === 'check') return !!f.checked
  return String(f.value || '').trim() !== ''
})

const extraHasProgress = (extra) => !!(
  extra && (extra.tablePassed || extra.modelPassed || extra.sum1 || extra.sum2 || extra.sum3
    || extra.progress || extra.qAll || extra.tableHit
    || (Array.isArray(extra.filled) && extra.filled.length)
    || (Array.isArray(extra.notified) && extra.notified.length))
)

const readFrameExtra = (frame) => {
  try {
    const fn = frame?.contentWindow?.__learnDraftExtra
    return typeof fn === 'function' ? fn() : null
  } catch {
    return null
  }
}

const applyFrameFields = (frame, fields) => {
  let doc = null
  try { doc = frame?.contentDocument } catch { return }
  if (!doc || !Array.isArray(fields)) return
  fields.forEach((f) => {
    if (f.kind === 'radio') {
      const el = doc.querySelector(`input[type="radio"][name="${cssAttr(f.name)}"][value="${cssAttr(f.value)}"]`)
      if (el) el.checked = true
      return
    }
    if (f.kind === 'check') {
      const el = (f.id && doc.getElementById(f.id))
        || (f.name && doc.querySelector(`input[type="checkbox"][name="${cssAttr(f.name)}"]`))
      if (el) el.checked = !!f.checked
      return
    }
    let el = f.id ? doc.getElementById(f.id) : null
    if (!el && f.table) el = doc.querySelector(`[data-table="${cssAttr(f.table)}"]`)
    if (!el && f.h) el = doc.querySelector(`[data-h="${cssAttr(f.h)}"]`)
    if (!el && f.name) el = doc.querySelector(`[name="${cssAttr(f.name)}"]`)
    if (!el) return
    el.value = f.value || ''
  })
}

const activityHasRestore = (index) => {
  const draft = activityDrafts.value[String(index)]
  if (draft && (fieldHasContent(draft.fields) || extraHasProgress(draft.extra))) return true
  return Number(index) === 2 && !!flowchartRestorePayload()
}

const clearFrameListeners = (index) => {
  const prev = frameListeners[index]
  if (prev) prev()
  delete frameListeners[index]
}

const snapshotActivity = (index) => {
  if (isTeacherPreview.value || sessionChoice[index] === 'pending') return
  const frame = frameRefs[index]
  if (!frame) return
  const fields = collectFrameFields(frame)
  const extra = readFrameExtra(frame)
  if (!fieldHasContent(fields) && !extraHasProgress(extra)) return
  activityDrafts.value = {
    ...activityDrafts.value,
    [String(index)]: { fields, extra, savedAt: Date.now() }
  }
  scheduleDraftFlush()
}

const armActivityDraft = (index) => {
  clearFrameListeners(index)
  const frame = frameRefs[index]
  let doc = null
  try { doc = frame?.contentDocument } catch { return }
  if (!doc) return
  const onChange = () => snapshotActivity(index)
  doc.addEventListener('input', onChange, true)
  doc.addEventListener('change', onChange, true)
  doc.addEventListener('click', onChange, false)
  frameListeners[index] = () => {
    doc.removeEventListener('input', onChange, true)
    doc.removeEventListener('change', onChange, true)
    doc.removeEventListener('click', onChange, false)
  }
}

let draftChain = Promise.resolve()

const flushDrafts = () => {
  if (isTeacherPreview.value) return Promise.resolve()
  const task = externalTask.value
  if (!task) return Promise.resolve()
  const job = async () => {
    try {
      const existing = parseExistingSubmission()
      delete existing.draftSave
      delete existing.progressSave
      if (existing.type === 'progress') delete existing.type
      existing.completedActivities = keptActivityIndexes()
      if (quizSubmitted.value) existing.quizSubmitted = true
      if (evaluationSubmitted.value) existing.evaluationSubmitted = true
      if (handedIn.value) existing.handedIn = true
      existing.activityDrafts = JSON.parse(JSON.stringify(activityDrafts.value || {}))
      const stored = JSON.stringify(existing)
      await submitTask({
        taskId: task.id,
        contentJson: JSON.stringify({ ...existing, draftSave: true }),
        studySeconds: 0
      })
      task.studentSubmission = { ...(task.studentSubmission || {}), contentJson: stored }
    } catch {
      /* 草稿保存失败时，本栏仍留在页面上 */
    }
  }
  // 草稿单独排队，避免堵住正式提交和「打开下一环」
  draftChain = draftChain.then(job, job)
  return draftChain
}

const scheduleDraftFlush = () => {
  if (draftTimer) clearTimeout(draftTimer)
  draftTimer = setTimeout(() => {
    draftTimer = null
    flushDrafts()
  }, 600)
}

const syncRestorePrompts = () => {
  if (isTeacherPreview.value) return
  ;(workspacePlan.value?.activities || []).forEach((act) => {
    const index = act?.index
    if (!index || !frameRefs[index] || sessionChoice[index]) return
    if (!activityHasRestore(index)) return
    clearFrameListeners(index)
    sessionChoice[index] = 'pending'
    restorePending[index] = true
  })
}

const onActivityFrameLoad = (act) => {
  const index = act?.index
  if (!index) return
  if (isTeacherPreview.value) return
  if (activityHasRestore(index) && !sessionChoice[index]) {
    sessionChoice[index] = 'pending'
    restorePending[index] = true
    return
  }
  if (sessionChoice[index] === 'restore') {
    acceptActivityRestore(index)
    return
  }
  armActivityDraft(index)
}

const acceptActivityRestore = (index) => {
  const idx = index || currentActivity.value?.index
  if (!idx) return
  const frame = frameRefs[idx]
  const draft = activityDrafts.value[String(idx)]
  if (frame && draft?.fields) applyFrameFields(frame, draft.fields)
  if (draft?.extra && frame?.contentWindow?.__learnApplyDraftExtra) {
    try { frame.contentWindow.__learnApplyDraftExtra(draft.extra) } catch { /* 跨页时忽略 */ }
  }
  if (Number(idx) === 2) restoreFlowchartToFrame(frame)
  restorePending[idx] = false
  sessionChoice[idx] = 'restore'
  armActivityDraft(idx)
}

const declineActivityRestore = () => {
  const idx = currentActivity.value?.index
  if (!idx) return
  restorePending[idx] = false
  sessionChoice[idx] = 'fresh'
  armActivityDraft(idx)
}

const flushDraftNow = () => {
  if (draftTimer) {
    clearTimeout(draftTimer)
    draftTimer = null
  }
  return flushDrafts()
}

const refreshActivity = (index) => {
  if (!index) return
  if (sessionChoice[index] && sessionChoice[index] !== 'pending') snapshotActivity(index)
  flushDraftNow()
  clearFrameListeners(index)
  delete sessionChoice[index]
  restorePending[index] = false
  frameNonce[index] = (frameNonce[index] || 0) + 1
}

const answerListHasContent = (list) => Array.isArray(list) && list.some((a) => a && a.selected != null)

const onQuizDraft = (answers) => {
  if (isTeacherPreview.value || quizSubmitted.value || quizRestorePending.value) return
  if (!answerListHasContent(answers)) return
  activityDrafts.value = {
    ...activityDrafts.value,
    quiz: { answers, savedAt: Date.now() }
  }
  scheduleDraftFlush()
}

const onEvalDraft = (answers) => {
  if (isTeacherPreview.value || evaluationSubmitted.value || evalRestorePending.value) return
  if (!answerListHasContent(answers)) return
  activityDrafts.value = {
    ...activityDrafts.value,
    evaluation: { answers, savedAt: Date.now() }
  }
  scheduleDraftFlush()
}

const acceptQuizRestore = () => {
  const draft = activityDrafts.value.quiz
  quizSeed.value = { mode: 'draft', answers: draft?.answers || [], token: Date.now() }
  quizRestorePending.value = false
}

const declineQuizRestore = () => {
  quizSeed.value = { mode: 'empty', answers: [], token: Date.now() }
  quizRestorePending.value = false
}

const refreshQuiz = () => {
  flushDraftNow()
  quizMountKey.value += 1
  if (quizSubmitted.value && savedQuizPayload.value) {
    quizSeed.value = { ...savedQuizPayload.value, token: Date.now() }
    quizRestorePending.value = false
    return
  }
  quizSeed.value = { mode: 'empty', answers: [], token: Date.now() }
  quizRestorePending.value = answerListHasContent(activityDrafts.value.quiz?.answers)
}

const acceptEvalRestore = () => {
  const draft = activityDrafts.value.evaluation
  evalSeed.value = { mode: 'draft', answers: draft?.answers || [], token: Date.now() }
  evalRestorePending.value = false
}

const declineEvalRestore = () => {
  evalSeed.value = { mode: 'empty', answers: [], token: Date.now() }
  evalRestorePending.value = false
}

const refreshEvaluation = () => {
  flushDraftNow()
  evalMountKey.value += 1
  if (evaluationSubmitted.value && savedEvalPayload.value) {
    evalSeed.value = { ...savedEvalPayload.value, token: Date.now() }
    evalRestorePending.value = false
    return
  }
  evalSeed.value = { mode: 'empty', answers: [], token: Date.now() }
  evalRestorePending.value = answerListHasContent(activityDrafts.value.evaluation?.answers)
}

const flowchartRestorePayload = () => {
  const data = parseExistingSubmission()
  if (!data?.flowchart) return null
  return {
    flowchart: data.flowchart,
    flowchartVersion: data.flowchartVersion,
    flowchartScore: data.flowchartScore ?? data.userScore,
    flowchartPassed: data.flowchartPassed,
    flowchartChecks: data.flowchartChecks
  }
}

const restoreFlowchartToFrame = (frame) => {
  const target = frame || frameRefs[2]
  const payload = flowchartRestorePayload()
  if (!target?.contentWindow || !payload) return
  target.contentWindow.postMessage({
    type: 'LEARN_SPACE_RESTORE_FLOWCHART',
    payload
  }, '*')
}

const isActivityUnlocked = (act) => !!act?.path

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

const activityDone = (index) => completedActivities.value.has(index) || completedActivities.value.has(Number(index))

const keptActivityIndexes = () => {
  const indexes = new Set()
  completedActivities.value.forEach((n) => {
    const i = Number(n)
    if ((i > 0 && i < 10) || i === 98 || i === 99) indexes.add(i)
  })
  ;(workspacePlan.value?.activities || []).forEach((a) => {
    const i = Number(a.index)
    if (i > 0 && i < 10 && activityDone(i)) indexes.add(i)
  })
  if (quizSubmitted.value) indexes.add(99)
  if (evaluationSubmitted.value) indexes.add(98)
  return [...indexes]
}

const canHandIn = computed(() => {
  const acts = (workspacePlan.value?.activities || []).filter((a) => Number(a.index) > 0)
  if (!acts.length) return false
  const inquiryDone = acts.every((a) => activityDone(a.index))
  const needQuiz = (workspacePlan.value?.quiz?.questions || []).length > 0
  const needEval = !!workspacePlan.value?.evaluationForm
  return inquiryDone && (!needQuiz || quizSubmitted.value) && (!needEval || evaluationSubmitted.value)
})

const handInHint = computed(() => {
  if (canHandIn.value) return '探究、课堂小测和学习评价都已完成'
  const parts = (workspacePlan.value?.activities || [])
    .filter((a) => Number(a.index) > 0)
    .map((a) => inquiryActivityLabel(a.index))
  if ((workspacePlan.value?.quiz?.questions || []).length) parts.push('课堂小测')
  if (workspacePlan.value?.evaluationForm) parts.push('学习评价')
  return parts.length ? `完成${parts.join('、')}后才能上交` : '完成本课任务后才能上交'
})

const activityIframeSrc = (act) => appendActivityParams(act.path || '', act)

const selectActivity = (act) => {
  if (!act?.path) return
  activePanel.value = 'act-' + act.index
}

const lastInquiryIndex = () => {
  const acts = workspacePlan.value?.activities || []
  if (!acts.length) return 0
  return Math.max(...acts.map((a) => Number(a.index) || 0))
}

const guideAfterActivity = async (activityIndex, options = {}) => {
  if (options.deferNext) return
  const idx = Number(activityIndex)
  if (!idx) return
  const acts = workspacePlan.value?.activities || []
  const next = acts.find((a) => Number(a.index) === idx + 1)
  if (next) {
    ElMessage.success(`${inquiryActivityLabel(idx)}完成，已为你打开${inquiryActivityLabel(next.index)}`)
    activePanel.value = 'act-' + next.index
    return
  }
  if (idx !== lastInquiryIndex()) return
  const hasEval = !!workspacePlan.value?.evaluationForm
  const hasQuiz = (workspacePlan.value?.quiz?.questions || []).length > 0
  if (!hasEval && !hasQuiz) return
  const parts = []
  if (hasQuiz) parts.push('课堂小测')
  if (hasEval) parts.push('学习评价')
  try {
    await ElMessageBox.alert(
      `请接着完成${parts.join('和')}。`,
      '探究环节已完成',
      { confirmButtonText: '去完成', type: 'success' }
    )
  } catch {
    /* 关闭也进入下一环节 */
  }
  activePanel.value = hasQuiz ? 'quiz' : 'evaluation'
}

const openIntro = () => {
  activePanel.value = 'intro'
}

const quickNavKey = computed(() => {
  if (activePanel.value === 'intro') return 'intro'
  if (activePanel.value === 'records') return 'records'
  if (activePanel.value === 'profile') return 'profile'
  if (showClassScreen.value) return 'ranking'
  return ''
})

const onQuickNav = (key) => {
  showClassScreen.value = false
  if (key === 'intro') activePanel.value = 'intro'
  else if (key === 'records') {
    activePanel.value = 'records'
    recordsPanelRef.value?.reload?.()
  } else if (key === 'profile') activePanel.value = 'profile'
  else if (key === 'ranking') {
    showClassScreen.value = true
  }
}

const openMall = () => {
  router.push(isTeacherPreview.value ? '/teacher/student-view/mall' : '/student/mall')
}

const goActivityFromRecords = (act) => {
  if (act.type === 'READING') {
    openIntro()
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

const pointsBumping = ref(false)
let pointsBumpTimer = null
const flashPoints = () => {
  pointsBumping.value = true
  if (pointsBumpTimer) clearTimeout(pointsBumpTimer)
  pointsBumpTimer = setTimeout(() => { pointsBumping.value = false }, 700)
}

const showPointTotals = (earned, redeemable, bump = false) => {
  if (earned == null && redeemable == null) return
  earnedPoints.value = earned ?? redeemable ?? earnedPoints.value
  redeemablePoints.value = redeemable ?? earned ?? redeemablePoints.value
  totalPoints.value = redeemablePoints.value
  if (bump) flashPoints()
}

const refreshPoints = async () => {
  try {
    const recordsRes = await getMyRecords()
    const d = recordsRes.data || {}
    if (d.earnedPoints != null || d.redeemablePoints != null || d.totalPoints != null) {
      showPointTotals(d.earnedPoints ?? d.totalPoints, d.redeemablePoints ?? d.totalPoints)
    }
  } catch {
    /* 提交结果里的分数保持不变 */
  }
  recordsPanelRef.value?.reload?.()
}

const openEvaluation = () => {
  activePanel.value = 'evaluation'
}

const openQuiz = () => {
  if (!quizUnlocked.value) return
  activePanel.value = 'quiz'
}

const applySubmissionState = (contentJson) => {
  if (!contentJson) return
  try {
    const data = JSON.parse(contentJson)
    if (data.activityDrafts && typeof data.activityDrafts === 'object') {
      activityDrafts.value = { ...data.activityDrafts }
    }
    if (data.handedIn) handedIn.value = true
    if (data.type === 'quiz' || data.quizScore != null || data.quizSubmitted) {
      quizSubmitted.value = true
      savedQuizPayload.value = {
        mode: 'result',
        quizScore: data.quizScore,
        correctCount: data.correctCount,
        answers: data.quizAnswers
          || (Array.isArray(data.answers) && data.answers.some((a) => a && (a.isCorrect != null || a.correct != null))
            ? data.answers
            : [])
      }
      quizSeed.value = savedQuizPayload.value
    }
    if (data.type === 'evaluation' || data.evaluationSubmitted || (data.evaluationAnswers && data.evaluationAnswers.length)) {
      evaluationSubmitted.value = true
      savedEvalPayload.value = {
        mode: 'result',
        answers: data.evaluationAnswers || data.answers || []
      }
      evalSeed.value = savedEvalPayload.value
    }
    if (!quizSubmitted.value && answerListHasContent(data.activityDrafts?.quiz?.answers)) {
      quizRestorePending.value = true
    }
    if (!evaluationSubmitted.value && answerListHasContent(data.activityDrafts?.evaluation?.answers)) {
      evalRestorePending.value = true
    }
    if (data.completedActivities && Array.isArray(data.completedActivities)) {
      data.completedActivities.forEach((i) => {
        const n = Number(i)
        if ((n > 0 && n < 10) || n === 98 || n === 99) completedActivities.value.add(n)
        if (n === 99) {
          quizSubmitted.value = true
          if (!savedQuizPayload.value) {
            savedQuizPayload.value = {
              mode: 'result',
              quizScore: data.quizScore || 0,
              correctCount: data.correctCount || 0,
              answers: data.quizAnswers || []
            }
            quizSeed.value = savedQuizPayload.value
          }
        }
        if (n === 98) {
          evaluationSubmitted.value = true
          if (!savedEvalPayload.value) {
            savedEvalPayload.value = { mode: 'result', answers: data.evaluationAnswers || [] }
            evalSeed.value = savedEvalPayload.value
          }
        }
      })
    }
    if (data.stepCompleted && data.activityIndex && data.type !== 'quiz' && data.type !== 'evaluation'
      && data.type !== 'progress' && data.type !== 'handin' && data.activityIndex < 10) {
      completedActivities.value.add(Number(data.activityIndex))
    }
    completedActivities.value = new Set(completedActivities.value)
    syncRestorePrompts()
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
  showPointTotals(earnedPoints.value + 2, redeemablePoints.value + 2, true)
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
    const score = payload.flowchartScore ?? payload.userScore
    const scoreTxt = score != null ? `，得分 ${score}` : ''
    ElMessage.success(`${inquiryActivityLabel(payload.activityIndex)}完成（教师预览，未提交${scoreTxt}）`)
    guideAfterActivity(payload.activityIndex, { deferNext: payload.deferNext })
    return
  }
  ElMessage.success('教师预览：活动进度已记录（未提交）')
}

let submitChain = Promise.resolve()

/** 探究完成时先切下一环，不把跳转绑在接口返回上（长停留后接口常变慢） */
const advanceUiAfterComplete = (merged) => {
  if (merged.type === 'quiz') {
    if (workspacePlan.value?.evaluationForm && !evaluationSubmitted.value) {
      activePanel.value = 'evaluation'
      ElMessage.info('请继续完成学习评价')
    }
    return true
  }
  if (merged.type === 'evaluation') {
    if ((workspacePlan.value?.quiz?.questions || []).length && !quizSubmitted.value) {
      activePanel.value = 'quiz'
      ElMessage.info('请继续完成课堂小测')
    }
    return true
  }
  if (merged.stepCompleted && merged.activityIndex
    && merged.type !== 'progress' && merged.type !== 'handin') {
    guideAfterActivity(merged.activityIndex, { deferNext: merged.deferNext })
    return true
  }
  return false
}

const submitExternal = (payload) => {
  if (isTeacherPreview.value) {
    previewCompleteActivity(payload)
    return Promise.resolve()
  }
  const task = externalTask.value
  if (!task) return Promise.resolve()

  // 本地先标完成并立刻跳转，避免排队/超时导致「提交后卡住」
  let uiAdvanced = false
  const early = { ...payload }
  if (early.stepCompleted && early.activityIndex
    && early.type !== 'quiz' && early.type !== 'evaluation'
    && early.type !== 'progress' && early.type !== 'handin'
    && Number(early.activityIndex) > 0 && Number(early.activityIndex) < 10) {
    completedActivities.value = new Set([...completedActivities.value, Number(early.activityIndex)])
    const label = inquiryActivityLabel(early.activityIndex)
    const score = early.flowchartScore ?? early.userScore
    if (early.flowchartRevision) {
      ElMessage.success(`${label}已保存修改${score != null ? `（${score}分）` : ''}`)
    } else {
      ElMessage.success(`${label}完成${score != null ? `，本次 ${score} 分` : ''}`)
    }
    uiAdvanced = advanceUiAfterComplete(early)
  } else if (early.type === 'quiz') {
    quizSubmitted.value = true
    savedQuizPayload.value = {
      mode: 'result',
      quizScore: early.quizScore,
      correctCount: early.correctCount,
      answers: early.answers || []
    }
    quizSeed.value = savedQuizPayload.value
    quizRestorePending.value = false
    uiAdvanced = advanceUiAfterComplete(early)
  } else if (early.type === 'evaluation') {
    evaluationSubmitted.value = true
    savedEvalPayload.value = { mode: 'result', answers: early.answers || [] }
    evalSeed.value = savedEvalPayload.value
    evalRestorePending.value = false
    uiAdvanced = advanceUiAfterComplete(early)
  }

  const job = async () => {
    submitting.value = task.id
    try {
      const existing = parseExistingSubmission()
      delete existing.draftSave
      delete existing.progressSave
      if (existing.type === 'progress') delete existing.type
      const merged = { ...existing, ...payload }
      delete merged.draftSave
      const keepProgress = merged.type === 'progress' || merged.type === 'handin' || merged.progressSave
      const inquiryFirst = !keepProgress && !!(merged.stepCompleted && merged.activityIndex
        && merged.type !== 'quiz' && merged.type !== 'evaluation'
        && merged.activityIndex < 10
        && !completedActivities.value.has(merged.activityIndex)
        && !completedActivities.value.has(Number(merged.activityIndex)))
      const quizFirst = merged.type === 'quiz' && !quizSubmitted.value
      const evalFirst = merged.type === 'evaluation' && !evaluationSubmitted.value
      // 若上面已提前标完成，这里不再重复加乐观积分
      const optimistic = (!uiAdvanced && (inquiryFirst || quizFirst || evalFirst)) ? 2 : 0
      if (optimistic) showPointTotals(earnedPoints.value + optimistic, redeemablePoints.value + optimistic, true)
      if (!keepProgress && merged.activityIndex && merged.type !== 'quiz' && merged.type !== 'evaluation' && merged.activityIndex < 10) {
        completedActivities.value = new Set([...completedActivities.value, Number(merged.activityIndex)])
      }
      if (merged.handedIn) handedIn.value = true
      if (merged.type === 'quiz') {
        quizSubmitted.value = true
        merged.quizAnswers = payload.answers
        savedQuizPayload.value = {
          mode: 'result',
          quizScore: payload.quizScore,
          correctCount: payload.correctCount,
          answers: payload.answers || []
        }
        quizSeed.value = savedQuizPayload.value
        quizRestorePending.value = false
      }
      if (merged.type === 'evaluation') {
        evaluationSubmitted.value = true
        merged.evaluationAnswers = payload.answers
        savedEvalPayload.value = { mode: 'result', answers: payload.answers || [] }
        evalSeed.value = savedEvalPayload.value
        evalRestorePending.value = false
        delete merged.activityIndex
      }
      merged.completedActivities = keptActivityIndexes()
      if (quizSubmitted.value) merged.quizSubmitted = true
      if (evaluationSubmitted.value) merged.evaluationSubmitted = true
      if (handedIn.value) merged.handedIn = true
      const studySeconds = Math.floor((Date.now() - startTime) / 1000)
      const toStore = { ...merged, submittedAt: new Date().toISOString() }
      delete toStore.progressSave
      delete toStore.draftSave
      if (toStore.type === 'progress') delete toStore.type
      const contentJson = JSON.stringify(merged.type === 'progress'
        ? { ...toStore, progressSave: true }
        : toStore)
      let awarded = optimistic
      try {
        const res = await submitTask({
          taskId: task.id,
          contentJson,
          studySeconds
        })
        const body = res?.data || {}
        if (body.pointsAwarded != null) awarded = Number(body.pointsAwarded) || 0
        if (body.earnedPoints != null || body.redeemablePoints != null) {
          showPointTotals(body.earnedPoints, body.redeemablePoints, awarded > 0 && !optimistic)
        } else if (optimistic && awarded > 0) {
          /* 已乐观加过 */
        } else if (uiAdvanced && awarded > 0) {
          showPointTotals(earnedPoints.value + awarded, redeemablePoints.value + awarded, true)
        }
      } catch (err) {
        if (optimistic) showPointTotals(earnedPoints.value - optimistic, redeemablePoints.value - optimistic)
        if (uiAdvanced) {
          ElMessage.warning('下一环已打开；进度稍后再自动保存，也可点「保存进度」')
          return
        }
        throw err
      }
      task.studentSubmission = { ...(task.studentSubmission || {}), contentJson }
      taskSubmitted.value = true
      const plus = awarded > 0 ? `，+${awarded} 积分` : ''
      if (merged.type === 'progress') {
        ElMessage.success('进度已保存，下次登录仍会保留已完成的环节')
      } else if (merged.type === 'handin') {
        ElMessage.success('已确认上交')
      } else if (merged.type === 'quiz') {
        ElMessage.success(`小测已提交${plus}`)
        if (!uiAdvanced) advanceUiAfterComplete(merged)
      } else if (merged.type === 'evaluation') {
        ElMessage.success(`学习评价已提交${plus}`)
        if (!uiAdvanced) advanceUiAfterComplete(merged)
      } else if (merged.stepCompleted && merged.activityIndex) {
        if (!uiAdvanced) {
          const label = inquiryActivityLabel(merged.activityIndex)
          const score = merged.flowchartScore ?? merged.userScore
          if (merged.flowchartRevision) {
            ElMessage.success(`${label}已保存修改${score != null ? `（${score}分）` : ''}`)
          } else {
            ElMessage.success(`${label}完成${plus}${score != null ? `，本次 ${score} 分` : ''}`)
          }
          advanceUiAfterComplete(merged)
        } else if (plus) {
          ElMessage.success(`已计入积分${plus}`)
        }
      } else if (merged.type !== 'evaluation') {
        ElMessage.success(awarded > 0 ? `活动进度已保存${plus}` : '活动进度已保存')
      }
      refreshPoints()
    } finally {
      submitting.value = null
    }
  }
  submitChain = submitChain.then(job, job)
  return submitChain
}

const submitQuiz = (payload) => submitExternal(payload)
const submitEvaluation = (payload) => submitExternal(payload)

const saveProgress = async () => {
  if (isTeacherPreview.value) {
    ElMessage.info('教师预览不会写入学生进度')
    return
  }
  ;(workspacePlan.value?.activities || []).forEach((act) => {
    if (frameRefs[act.index]) snapshotActivity(act.index)
  })
  savingProgress.value = true
  try {
    await flushDraftNow()
    await submitExternal({ type: 'progress', progressSave: true })
  } catch {
    ElMessage.error('进度保存失败，请再试一次')
  } finally {
    savingProgress.value = false
  }
}

const confirmHandIn = async () => {
  if (!canHandIn.value) return
  if (isTeacherPreview.value) {
    handedIn.value = true
    ElMessage.success('教师预览：已确认上交（未写入学生数据）')
    return
  }
  handingIn.value = true
  try {
    await submitExternal({ type: 'handin', handedIn: true })
  } catch {
    ElMessage.error('上交失败，请再试一次')
  } finally {
    handingIn.value = false
  }
}

const onLearnMessage = (event) => {
  if (!event?.data) return
  if (event.data.type === 'LEARN_SPACE_REQUEST_RESTORE') {
    if (sessionChoice[2] === 'restore') restoreFlowchartToFrame(frameRefs[2])
    return
  }
  if (event.data.type !== MSG_TYPE) return
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
  document.addEventListener('fullscreenchange', onFsChange)
  document.addEventListener('webkitfullscreenchange', onFsChange)

  const lessonRes = await getLesson(route.params.id)
  lesson.value = lessonRes.data.lesson
  resources.value = lessonRes.data.resources || []
  tasks.value = lessonRes.data.tasks || []
  textbookPdfUrl.value = lessonRes.data.textbookPdfUrl || ''
  tasks.value.forEach((t) => { answers[t.id] = {} })

  if (!isTeacherPreview.value) {
    const [meRes, recordsRes, pretestRes, contentRes] = await Promise.all([
      getMe().catch(() => ({ data: {} })),
      getMyRecords().catch(() => ({ data: { totalPoints: 0 } })),
      getMyPretestG6().catch(() => ({ data: null })),
      getLessonContent(visitLessonId).catch(() => ({ data: { published: false } }))
    ])
    const lv = String(pretestRes?.data?.level || '').toUpperCase()
    pretestLevel.value = (lv === 'A' || lv === 'B' || lv === 'C') ? lv : ''
    lessonContent.value = contentRes?.data || { published: false, activities: [] }
    studentNo.value = meRes.data?.username || localStorage.getItem('username') || ''
    studentName.value = meRes.data?.realName || localStorage.getItem('realName') || ''
    className.value = meRes.data?.className || ''
    const d = recordsRes.data || {}
    earnedPoints.value = d.earnedPoints ?? d.totalPoints ?? 0
    redeemablePoints.value = d.redeemablePoints ?? d.totalPoints ?? 0
    totalPoints.value = redeemablePoints.value
    signedToday.value = localStorage.getItem(signinKey()) === '1'
  } else {
    studentName.value = localStorage.getItem('realName') || '教师'
    const contentRes = await getLessonContent(visitLessonId).catch(() => ({ data: { published: false } }))
    lessonContent.value = contentRes?.data || { published: false, activities: [] }
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
        activePanel.value = 'intro'
      } else if (String(stepQuery) === 'reading') {
        activePanel.value = 'intro'
      }
    } else {
      activePanel.value = 'intro'
    }
  } else if (sidebarPlan.value) {
    activePanel.value = 'objectives'
  }

  if (!isTeacherPreview.value) {
    sendVisit(0)
    visitTimer = setInterval(() => sendVisit(30), 30000)
  }
})

const onProfilePoints = (n) => {
  redeemablePoints.value = n
  totalPoints.value = n
}

onUnmounted(() => {
  window.removeEventListener('message', onLearnMessage)
  document.removeEventListener('fullscreenchange', onFsChange)
  document.removeEventListener('webkitfullscreenchange', onFsChange)
  if (playFullscreen.value) exitNativeFs().catch(() => {})
  if (visitTimer) clearInterval(visitTimer)
  if (draftTimer) {
    clearTimeout(draftTimer)
    draftTimer = null
    flushDrafts()
  }
  if (!isTeacherPreview.value) {
    const sec = Math.floor((Date.now() - startTime) / 1000)
    sendVisit(sec)
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
.submit-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 10px 0;
}
.save-progress-btn,
.hand-in-btn {
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
}
.save-progress-btn {
  border: 1px solid #1677ff;
  background: #fff;
  color: #1677ff;
  cursor: pointer;
}
.save-progress-btn:disabled {
  opacity: 0.65;
  cursor: wait;
}
.hand-in-btn {
  border: none;
  background: #cbd5e1;
  color: #64748b;
  cursor: not-allowed;
}
.hand-in-btn.ready {
  background: #1677ff;
  color: #fff;
  cursor: pointer;
}
.hand-in-btn.ready:hover {
  background: #0f62d6;
}
.hand-in-hint {
  margin: 0;
  font-size: 12px;
  line-height: 1.45;
  color: #64748b;
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
.points-row strong { color: #5b4fc7; font-size: 16px; display: inline-block; }
.points-row strong.points-bump { color: #b45309; animation: points-pop 0.45s ease; }
@keyframes points-pop {
  0% { transform: scale(1); }
  40% { transform: scale(1.35); }
  100% { transform: scale(1); }
}
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
.reading-head {
  display: block;
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
.iframe-panel :deep(.scaffold-panel) {
  flex-shrink: 0;
  max-height: 28%;
  overflow: auto;
}
.activity-panel-head {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 14px;
  background: linear-gradient(90deg, #f8fafc 0%, #fff 100%);
  border-bottom: 1px solid #e2e8f0;
}
.activity-panel-head h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #1e3a8a;
}
.activity-head-spacer { flex: 1; }
.head-actions { display: flex; align-items: center; gap: 8px; }
.tier-switch {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-right: 4px;
}
.tier-switch span {
  font-size: 12px;
  color: #64748b;
}
.tier-switch button {
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #334155;
  border-radius: 999px;
  padding: 2px 10px;
  font-size: 12px;
  line-height: 20px;
  cursor: pointer;
}
.tier-switch button.on {
  background: #1d4ed8;
  border-color: #1d4ed8;
  color: #fff;
}
.section-tools {
  display: flex;
  justify-content: flex-end;
  padding: 8px 14px 0;
}
.restore-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin: 8px 12px 0;
  padding: 10px 12px;
  border: 1px solid #fcd34d;
  border-radius: 10px;
  background: #fffbeb;
  color: #92400e;
  font-size: 14px;
  line-height: 1.5;
}
.play-stage {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
}
.play-stage.is-fs {
  background: #0f172a;
}
.play-fs-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 14px;
  color: #fff;
  background: #1e293b;
  font-size: 14px;
  font-weight: 600;
}
.studio-play-fs .studio-left {
  display: none;
}
.studio-play-fs .activity-panel-head {
  display: none;
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
