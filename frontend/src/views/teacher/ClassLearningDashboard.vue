<template>
  <div class="cld-page" v-loading="loading">
    <header class="cld-header">
      <div class="header-left">
        <h1 class="header-title">
          <span class="title-icon">📊</span>
          全班学习情况看板
        </h1>
      </div>
      <div class="header-pills">
        <div class="pill pill-select">
          <span class="pill-label">年级</span>
          <el-select
            v-model="gradeId"
            size="small"
            class="filter-select grade-select"
            placeholder="选择年级"
            filterable
            @change="onGradeChange"
          >
            <el-option v-for="g in grades" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </div>
        <div class="pill pill-select">
          <span class="pill-label">课程</span>
          <el-select
            v-model="lessonId"
            size="small"
            class="filter-select lesson-select"
            placeholder="选择课时"
            filterable
            :disabled="!lessons.length"
            @change="onLessonChange"
          >
            <el-option v-for="les in lessons" :key="les.id" :label="les.title" :value="les.id" />
          </el-select>
        </div>
        <div class="pill pill-select">
          <span class="pill-label">班级</span>
          <el-select
            v-model="classId"
            size="small"
            class="filter-select class-select"
            placeholder="选择班级"
            @change="onClassChange"
          >
            <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </div>
        <div class="pill pill-code" v-if="classId">
          <span class="pill-label">班级码</span>
          <span class="pill-value code-val">{{ classLoginCode || (codeLoading ? '…' : '—') }}</span>
          <button type="button" class="code-copy" title="复制班级码" :disabled="!classLoginCode" @click="copyLoginCode">⎘</button>
          <button type="button" class="code-refresh" title="刷新班级码（随机6位，8小时有效）" @click="refreshLoginCode">↻</button>
        </div>
        <div class="pill">
          <span class="pill-label">人数</span>
          <span class="pill-value">{{ data.totalStudents || 0 }} 人</span>
        </div>
        <div class="pill">
          <span class="pill-label">活动</span>
          <span class="pill-value">{{ data.activityCount || 0 }} 个</span>
        </div>
      </div>
      <div class="header-actions">
        <button
          v-if="classId && lessonId"
          type="button"
          class="action-btn indigo"
          @click="markCurrentLesson"
        >📍 设为当前课时</button>
        <button
          v-if="classId"
          type="button"
          class="action-btn teal"
          @click="broadcastCountdown"
        >⏱ 倒计时提醒</button>
        <button type="button" class="action-btn orange" @click="$router.push('/teacher/points')">🏆 班级积分榜单</button>
        <button type="button" class="action-btn pink" @click="$router.push('/teacher/ai-evaluation')">🤖 AI 课程评价</button>
        <button type="button" class="icon-btn" title="刷新" @click="loadData">↻</button>
      </div>
    </header>

    <section v-if="classId" class="class-code-banner">
      <div class="banner-text">
        <strong>班级登录码</strong>
        <span>学生：登录页 → 班级码登录 → 输入 6 位码 → 选择姓名进入课程</span>
      </div>
      <div class="banner-code-wrap">
        <span class="banner-code">{{ classLoginCode || (codeLoading ? '生成中…' : '—') }}</span>
        <button type="button" class="banner-btn" :disabled="!classLoginCode" @click="copyLoginCode">复制</button>
        <button type="button" class="banner-btn" @click="refreshLoginCode">刷新</button>
      </div>
      <p v-if="codeExpiresHint" class="banner-expires">{{ codeExpiresHint }}</p>
    </section>

    <section v-if="classId" class="park-section">
      <div class="section-head">
        <h3>
          <span class="sec-icon">🎮</span>
          游学乐园授权
          <el-tag v-if="parkPendingCount" size="small" type="warning" style="margin-left:8px">
            {{ parkPendingCount }} 待审
          </el-tag>
        </h3>
        <button type="button" class="act-btn sm" @click="loadParkApps">刷新申请</button>
      </div>
      <p class="park-tip">学生完成当前课时任务并提交申请后，可在此授权开启贪吃蛇、打字等娱乐活动。</p>
      <el-table :data="parkApps" stripe size="small" max-height="280" v-loading="parkLoading">
        <el-table-column prop="realName" label="学生" width="100" />
        <el-table-column prop="username" label="学号" width="110" />
        <el-table-column prop="lessonTitle" label="关联课时" min-width="160">
          <template #default="{ row }">{{ row.lessonTitle || '—' }}</template>
        </el-table-column>
        <el-table-column label="任务" width="90" align="center">
          <template #default="{ row }">
            <span :class="row.taskCompleted ? 'done' : 'undone'">
              {{ row.taskCompleted ? '已完成' : '未完成' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="parkStatusType(row.status)">{{ parkStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="160">
          <template #default="{ row }">{{ formatTime(row.appliedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING' || row.status === 'REJECTED' || row.status === 'REVOKED'"
              link
              type="success"
              size="small"
              :disabled="!row.taskCompleted"
              @click="reviewPark(row, 'approve')"
            >授权</el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              link
              type="warning"
              size="small"
              @click="reviewPark(row, 'reject')"
            >拒绝</el-button>
            <el-button
              v-if="row.status === 'APPROVED'"
              link
              type="danger"
              size="small"
              @click="reviewPark(row, 'revoke')"
            >关闭</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p v-if="!parkApps.length && !parkLoading" class="empty-hint">暂无游学乐园申请</p>
    </section>

    <template v-if="lessonId">
      <!-- 中部三卡片 -->
      <section class="cards-row">
        <div class="card chart-card">
          <div class="card-head">
            <h3>💻 提交情况与提交率</h3>
            <div class="legend">
              <span><i class="dot green" />已提交</span>
              <span><i class="dot red" />未提交</span>
              <span><i class="dot blue" />提交率</span>
            </div>
          </div>
          <div class="chart-wrap">
            <canvas ref="chartRef" />
          </div>
          <p v-if="!data.activities?.length" class="empty-hint">暂无活动数据</p>
        </div>

        <div class="card summary-card radar-card">
          <h3>✨ 课程小测 概况</h3>
          <p class="radar-meta">
            参与 {{ quiz.participantCount || 0 }} 人
            <span v-if="quiz.avgTotalScore != null"> · 均分 {{ quiz.avgTotalScore }}/{{ quiz.totalMaxScore || 6 }}</span>
          </p>
          <div class="radar-wrap">
            <canvas ref="quizRadarRef" />
          </div>
          <p v-if="!(quiz.dimensions || []).length" class="empty-hint">暂无小测数据</p>
        </div>

        <div class="card summary-card radar-card">
          <h3>📝 学习评价 概况</h3>
          <p class="radar-meta">参与 {{ evaluation.participantCount || 0 }} 人</p>
          <div class="radar-wrap">
            <canvas ref="evalRadarRef" />
          </div>
          <p v-if="!(evaluation.dimensions || []).length" class="empty-hint">暂无评价数据</p>
        </div>
      </section>

      <!-- 活动提交进度 -->
      <section class="progress-section">
        <div class="section-head">
          <h3><span class="sec-icon">📚</span> 活动提交进度概览</h3>
          <div class="view-toggle">
            <button type="button" :class="{ active: viewMode === 'grid' }" @click="viewMode = 'grid'">网格视图</button>
            <button type="button" :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'">列表视图</button>
          </div>
        </div>

        <div v-if="viewMode === 'list'" class="activity-list">
          <div v-for="act in data.activities || []" :key="act.index" class="activity-row">
            <div class="act-index">#{{ act.index }}</div>
            <div class="act-info">
              <div class="act-title">
                {{ activityLabel(act) }}
                <el-tag v-if="!act.unlocked" size="small" type="info">需解锁</el-tag>
              </div>
              <div class="act-meta">{{ act.submittedCount || 0 }} / {{ data.totalStudents || 0 }} 人已提交</div>
            </div>
            <div class="act-progress">
              <el-progress
                :percentage="act.submitRate || 0"
                :stroke-width="10"
                :color="progressColor(act.submitRate)"
              />
            </div>
            <div class="act-rate">{{ act.submitRate || 0 }}%</div>
            <div class="act-actions">
              <button type="button" class="act-btn" @click="openDetail(act, 'submitted')">提交详情</button>
              <button type="button" class="act-btn outline" @click="openDetail(act, 'unsubmitted')">未交名单</button>
            </div>
          </div>
          <p v-if="!(data.activities || []).length" class="empty-hint">该课时暂无活动数据</p>
        </div>

        <div v-else class="activity-grid">
          <div v-for="act in data.activities || []" :key="'g-' + act.index" class="grid-card">
            <div class="grid-head">
              <span class="grid-num">#{{ act.index }}</span>
              <el-tag v-if="!act.unlocked" size="small" type="info">需解锁</el-tag>
            </div>
            <div class="grid-title">{{ activityLabel(act) }}</div>
            <div class="grid-count">{{ act.submittedCount || 0 }} / {{ data.totalStudents || 0 }}</div>
            <el-progress
              :percentage="act.submitRate || 0"
              :stroke-width="8"
              :color="progressColor(act.submitRate)"
            />
            <div class="grid-actions">
              <button type="button" class="act-btn sm" @click="openDetail(act, 'submitted')">提交详情</button>
            </div>
          </div>
          <p v-if="!(data.activities || []).length" class="empty-hint">该课时暂无活动数据</p>
        </div>
      </section>

      <!-- 全班任务完成矩阵 -->
      <section class="matrix-section">
        <div class="section-head">
          <h3><span class="sec-icon">📋</span> 全班任务完成矩阵</h3>
        </div>
        <div class="matrix-wrap">
          <el-table :data="matrixRows" border stripe size="small" max-height="480">
            <el-table-column prop="realName" label="学生" fixed width="100" />
            <el-table-column prop="username" label="学号" fixed width="110" />
            <el-table-column
              v-for="col in matrixColumns"
              :key="col.index"
              :label="col.label"
              min-width="88"
              align="center"
            >
              <template #default="{ row }">
                <span :class="cellClass(row.cells[col.index])">
                  {{ cellText(row.cells[col.index]) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <p class="matrix-legend">
          <span class="done">✓ 已完成</span>
          <span class="undone">✗ 未完成</span>
        </p>
      </section>
    </template>

    <div v-else class="empty-page">
      <el-empty description="请先选择年级与课程" :image-size="88" />
    </div>

    <el-dialog v-model="detailVisible" :title="detailTitle" width="520px">
      <el-table :data="detailRows" stripe max-height="360" size="small">
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="username" label="学号" width="100" />
        <el-table-column v-if="detailMode === 'submitted'" label="得分" width="70">
          <template #default="{ row }">{{ row.score ?? '—' }}</template>
        </el-table-column>
        <el-table-column v-if="detailMode === 'submitted'" label="提交时间" min-width="140">
          <template #default="{ row }">{{ formatTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="detailMode === 'unsubmitted'"
              link
              type="warning"
              size="small"
              @click="remindStudent(row)"
            >提醒完成</el-button>
            <el-button
              v-else-if="isLowScore(row)"
              link
              type="primary"
              size="small"
              @click="guideStudent(row)"
            >学习建议</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p v-if="!detailRows.length" class="empty-hint">暂无记录</p>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Chart,
  BarController,
  BarElement,
  LineController,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale,
  RadialLinearScale,
  RadarController,
  Filler,
  Tooltip,
  Legend
} from 'chart.js'
import {
  getClasses,
  getLessonActivityDashboard,
  teacherGetTextbooks,
  teacherGetOutline,
  getClassLoginCode,
  refreshClassLoginCode,
  teacherIntervene,
  setClassCurrentLesson,
  getParkApplications,
  reviewParkAccess
} from '../../api'

Chart.register(
  BarController, BarElement, LineController, LineElement, PointElement,
  CategoryScale, LinearScale, RadialLinearScale, RadarController, Filler,
  Tooltip, Legend
)

const QUIZ_RADAR_LABELS = ['知识掌握', '能力提升', '素养变化']
const EVAL_RADAR_LABELS = ['学习兴趣', '学习难度', '学习信心']

const route = useRoute()
const router = useRouter()

const grades = ref([])
const gradeId = ref(null)
const lessons = ref([])
const lessonId = ref(null)
const classes = ref([])
const classId = ref(null)
const classLoginCode = ref('')
const codeExpiresAt = ref('')
const codeLoading = ref(false)
const data = ref({})
const loading = ref(false)
const viewMode = ref('list')
const chartRef = ref(null)
const quizRadarRef = ref(null)
const evalRadarRef = ref(null)
const sseConnected = ref(false)
let chart = null
let quizRadar = null
let evalRadar = null
let eventSource = null

const detailVisible = ref(false)
const detailMode = ref('submitted')
const detailAct = ref(null)

const parkApps = ref([])
const parkLoading = ref(false)
const parkPendingCount = computed(() => parkApps.value.filter((r) => r.status === 'PENDING').length)

const quiz = computed(() => data.value.quizSummary || {})
const evaluation = computed(() => data.value.evaluationSummary || {})

const codeExpiresHint = computed(() => {
  if (!codeExpiresAt.value) return ''
  const dt = new Date(codeExpiresAt.value)
  if (Number.isNaN(dt.getTime())) return ''
  return `有效期至 ${dt.toLocaleString('zh-CN', { hour12: false })}（8 小时内有效）`
})

const detailTitle = computed(() => {
  if (!detailAct.value) return '提交详情'
  const label = activityLabel(detailAct.value)
  return detailMode.value === 'submitted' ? `${label} · 已交名单` : `${label} · 未交名单`
})

const detailRows = computed(() => {
  if (!detailAct.value) return []
  return detailMode.value === 'submitted'
    ? (detailAct.value.submittedStudents || [])
    : (detailAct.value.unsubmittedStudents || [])
})

const matrixColumns = computed(() =>
  (data.value.activities || []).map((act) => ({
    index: act.index,
    label: act.type === 'QUIZ' ? '小测' : act.type === 'EVALUATION' ? '评价' : `#${act.index}`
  }))
)

const matrixRows = computed(() => {
  const acts = data.value.activities || []
  if (!acts.length) return []

  const studentMap = new Map()
  for (const act of acts) {
    const submittedIds = new Set((act.submittedStudents || []).map((s) => s.studentId))
    const allStudents = [...(act.submittedStudents || []), ...(act.unsubmittedStudents || [])]
    for (const s of allStudents) {
      if (!studentMap.has(s.studentId)) {
        studentMap.set(s.studentId, {
          studentId: s.studentId,
          realName: s.realName,
          username: s.username,
          cells: {}
        })
      }
      const row = studentMap.get(s.studentId)
      const submitted = act.submittedStudents?.find((x) => x.studentId === s.studentId)
      row.cells[act.index] = {
        done: submittedIds.has(s.studentId),
        score: submitted?.score ?? null
      }
    }
  }
  return Array.from(studentMap.values()).sort((a, b) =>
    String(a.realName).localeCompare(String(b.realName), 'zh-CN')
  )
})

const chineseNum = (n) => ({ 1: '一', 2: '二', 3: '三', 4: '四', 5: '五', 6: '六' }[n] || n)

const activityLabel = (act) => {
  if (act.type === 'EVALUATION') return '学习评价'
  if (act.type === 'QUIZ') return '课堂小测'
  return `探究活动${chineseNum(act.index)}：${act.title}`
}

const buildQuizRadarValues = () => {
  const map = {}
  ;(quiz.value.dimensions || []).forEach((d) => {
    map[d.name] = Number(d.correctRate ?? 0)
  })
  return QUIZ_RADAR_LABELS.map((label) => map[label] ?? 0)
}

const buildEvalRadarValues = () => {
  const map = {}
  ;(evaluation.value.dimensions || []).forEach((d) => {
    const label = d.name === '难度感知' ? '学习难度' : d.name
    map[label] = Number(d.positiveRate ?? 0)
  })
  return EVAL_RADAR_LABELS.map((label) => map[label] ?? 0)
}

const renderRadarChart = (canvas, labels, values, color) => {
  if (!canvas) return null
  return new Chart(canvas, {
    type: 'radar',
    data: {
      labels,
      datasets: [{
        label: '班级概况',
        data: values,
        backgroundColor: `${color}33`,
        borderColor: color,
        borderWidth: 2,
        pointBackgroundColor: color,
        pointRadius: 3
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        r: {
          beginAtZero: true,
          max: 100,
          ticks: { stepSize: 25, backdropColor: 'transparent', font: { size: 10 } },
          pointLabels: { font: { size: 12, weight: '600' }, color: '#475569' },
          grid: { color: 'rgba(148, 163, 184, 0.35)' }
        }
      }
    }
  })
}

const renderRadarCharts = () => {
  if (quizRadar) {
    quizRadar.destroy()
    quizRadar = null
  }
  if (evalRadar) {
    evalRadar.destroy()
    evalRadar = null
  }
  if (quizRadarRef.value) {
    quizRadar = renderRadarChart(quizRadarRef.value, QUIZ_RADAR_LABELS, buildQuizRadarValues(), '#6366f1')
  }
  if (evalRadarRef.value) {
    evalRadar = renderRadarChart(evalRadarRef.value, EVAL_RADAR_LABELS, buildEvalRadarValues(), '#8b5cf6')
  }
}

const progressColor = (rate) => {
  if (rate >= 80) return '#22c55e'
  if (rate >= 40) return '#6366f1'
  return '#f87171'
}

const formatTime = (t) => {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

const cellClass = (cell) => (cell?.done ? 'done' : 'undone')
const cellText = (cell) => {
  if (!cell?.done) return '✗'
  return cell.score != null ? `✓${cell.score}` : '✓'
}

const openDetail = (act, mode) => {
  detailAct.value = act
  detailMode.value = mode
  detailVisible.value = true
}

const parkStatusText = (s) => ({
  PENDING: '待审批',
  APPROVED: '已授权',
  REJECTED: '已拒绝',
  REVOKED: '已关闭'
}[s] || s)

const parkStatusType = (s) => ({
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'info',
  REVOKED: 'danger'
}[s] || 'info')

const loadParkApps = async () => {
  if (!classId.value) {
    parkApps.value = []
    return
  }
  parkLoading.value = true
  try {
    const res = await getParkApplications(classId.value)
    parkApps.value = res.data || []
  } catch {
    parkApps.value = []
  } finally {
    parkLoading.value = false
  }
}

const reviewPark = async (row, action) => {
  try {
    await reviewParkAccess({ studentId: row.studentId, action })
    ElMessage.success(action === 'approve' ? '已授权开启游学乐园' : action === 'reject' ? '已拒绝申请' : '已关闭访问')
    await loadParkApps()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const isLowScore = (row) => {
  if (row.score == null) return false
  const act = detailAct.value
  if (act?.type === 'QUIZ') return row.score < 4
  return row.score < 60
}

const remindStudent = async (row) => {
  if (!classId.value || !lessonId.value || !detailAct.value) return
  const act = detailAct.value
  const actIndex = act.type === 'QUIZ' || act.type === 'EVALUATION' ? act.index : act.index
  const label = activityLabel(act)
  try {
    await teacherIntervene({
      type: 'remind',
      classId: classId.value,
      targetStudentId: row.studentId,
      lessonId: lessonId.value,
      activityIndex: actIndex,
      message: `请尽快完成${label}`
    })
    ElMessage.success(`已向 ${row.realName} 发送提醒`)
  } catch {
    ElMessage.error('发送失败')
  }
}

const guideStudent = async (row) => {
  if (!classId.value) return
  try {
    await teacherIntervene({
      type: 'guide',
      classId: classId.value,
      targetStudentId: row.studentId,
      lessonId: lessonId.value,
      content: '建议回顾本课内容与教材，巩固薄弱知识点后再继续练习'
    })
    ElMessage.success(`已向 ${row.realName} 发送学习建议`)
  } catch {
    ElMessage.error('发送失败')
  }
}

const broadcastCountdown = async () => {
  if (!classId.value) return
  try {
    await teacherIntervene({
      type: 'broadcast',
      classId: classId.value,
      lessonId: lessonId.value || undefined,
      message: '还有5分钟，请抓紧提交'
    })
    ElMessage.success('全班倒计时提醒已发送')
  } catch {
    ElMessage.error('发送失败')
  }
}

const markCurrentLesson = async () => {
  if (!classId.value || !lessonId.value) return
  try {
    await setClassCurrentLesson(classId.value, lessonId.value)
    ElMessage.success('已设为班级当前课时，学生登录后将自动进入')
  } catch {
    ElMessage.error('设置失败')
  }
}

const syncQuery = () => {
  router.replace({
    query: {
      gradeId: gradeId.value || undefined,
      lessonId: lessonId.value || undefined,
      classId: classId.value || undefined
    }
  })
}

const flattenLessons = (outline) => {
  const list = []
  for (const unit of outline?.units || []) {
    for (const lesson of unit.lessons || []) {
      list.push({ id: lesson.id, title: lesson.title, unitName: unit.name })
    }
  }
  return list
}

const loadLessonsForGrade = async (keepLessonId) => {
  if (!gradeId.value) {
    lessons.value = []
    lessonId.value = null
    return
  }
  const outline = await teacherGetOutline(gradeId.value)
  lessons.value = flattenLessons(outline.data)
  if (keepLessonId && lessons.value.some((l) => l.id === keepLessonId)) {
    lessonId.value = keepLessonId
  } else if (lessons.value.length) {
    lessonId.value = lessons.value[0].id
  } else {
    lessonId.value = null
  }
}

const renderChart = () => {
  if (!chartRef.value) return
  if (chart) chart.destroy()
  const acts = data.value.activities || []
  if (!acts.length) return

  const labels = acts.map((a) => {
    if (a.type === 'EVALUATION') return '学习评价'
    if (a.type === 'QUIZ') return '课堂小测'
    return `探究活动${chineseNum(a.index)}`
  })
  const submitted = acts.map((a) => a.submittedCount || 0)
  const unsubmitted = acts.map((a) => a.unsubmittedCount || 0)
  const rates = acts.map((a) => a.submitRate || 0)

  chart = new Chart(chartRef.value, {
    data: {
      labels,
      datasets: [
        {
          type: 'bar',
          label: '已提交',
          data: submitted,
          backgroundColor: '#22c55e',
          borderRadius: 4,
          yAxisID: 'y'
        },
        {
          type: 'bar',
          label: '未提交',
          data: unsubmitted,
          backgroundColor: '#f87171',
          borderRadius: 4,
          yAxisID: 'y'
        },
        {
          type: 'line',
          label: '提交率',
          data: rates,
          borderColor: '#6366f1',
          backgroundColor: '#6366f1',
          tension: 0.3,
          yAxisID: 'y1'
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: 'index', intersect: false },
      plugins: { legend: { display: false } },
      scales: {
        y: {
          type: 'linear',
          position: 'left',
          beginAtZero: true,
          max: Math.max(data.value.totalStudents || 10, 10),
          title: { display: true, text: '人数' }
        },
        y1: {
          type: 'linear',
          position: 'right',
          beginAtZero: true,
          max: 100,
          grid: { drawOnChartArea: false },
          title: { display: true, text: '提交率%' }
        }
      }
    }
  })
}

const loadClassLoginCode = async () => {
  if (!classId.value) {
    classLoginCode.value = ''
    codeExpiresAt.value = ''
    return
  }
  codeLoading.value = true
  try {
    const res = await getClassLoginCode(classId.value)
    classLoginCode.value = res.data?.loginCode || ''
    codeExpiresAt.value = res.data?.expiresAt || ''
  } catch {
    classLoginCode.value = ''
    codeExpiresAt.value = ''
    ElMessage.error('班级码获取失败，请确认已登录教师账号')
  } finally {
    codeLoading.value = false
  }
}

const refreshLoginCode = async () => {
  if (!classId.value) return
  codeLoading.value = true
  try {
    const res = await refreshClassLoginCode(classId.value)
    classLoginCode.value = res.data?.loginCode || ''
    codeExpiresAt.value = res.data?.expiresAt || ''
    ElMessage.success(`班级码已更新：${classLoginCode.value}`)
  } catch {
    ElMessage.error('刷新班级码失败')
  } finally {
    codeLoading.value = false
  }
}

const copyLoginCode = async () => {
  if (!classLoginCode.value) return
  try {
    await navigator.clipboard.writeText(classLoginCode.value)
    ElMessage.success('班级码已复制')
  } catch {
    ElMessage.info(`班级码：${classLoginCode.value}`)
  }
}

const loadData = async () => {
  if (!classId.value || !lessonId.value) {
    data.value = {}
    return
  }
  loading.value = true
  try {
    const res = await getLessonActivityDashboard(lessonId.value, classId.value)
    data.value = res.data || {}
    await nextTick()
    renderChart()
    renderRadarCharts()
  } catch {
    ElMessage.error('加载学情数据失败')
    data.value = {}
  } finally {
    loading.value = false
  }
}

const connectSse = () => {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
  sseConnected.value = false
  if (!classId.value) return
  const token = localStorage.getItem('token')
  eventSource = new EventSource(`/api/dashboard/sse/${classId.value}?token=${token}`)
  eventSource.addEventListener('connected', () => { sseConnected.value = true })
  eventSource.addEventListener('dashboard', () => { loadData() })
  eventSource.onerror = () => { sseConnected.value = false }
}

const onGradeChange = async () => {
  await loadLessonsForGrade()
  syncQuery()
  await loadData()
}

const onLessonChange = async () => {
  syncQuery()
  await loadData()
}

const onClassChange = async () => {
  syncQuery()
  connectSse()
  await loadClassLoginCode()
  await Promise.all([loadData(), loadParkApps()])
}

onMounted(async () => {
  const [gradeRes, clsRes] = await Promise.all([
    teacherGetTextbooks('MAIN'),
    getClasses()
  ])
  grades.value = gradeRes.data || []
  classes.value = clsRes.data || []

  const qGrade = route.query.gradeId ? Number(route.query.gradeId) : null
  const qLesson = route.query.lessonId ? Number(route.query.lessonId) : null
  const qClass = route.query.classId ? Number(route.query.classId) : null

  if (qGrade && grades.value.some((g) => g.id === qGrade)) {
    gradeId.value = qGrade
  } else if (grades.value.length) {
    gradeId.value = grades.value[0].id
  }

  await loadLessonsForGrade(qLesson)

  if (qClass && classes.value.some((c) => c.id === qClass)) {
    classId.value = qClass
  } else if (classes.value.length) {
    classId.value = classes.value[0].id
  } else {
    classId.value = 1
  }

  syncQuery()
  connectSse()
  await loadClassLoginCode()
  await Promise.all([loadData(), loadParkApps()])
})

onUnmounted(() => {
  if (eventSource) eventSource.close()
  if (chart) chart.destroy()
  if (quizRadar) quizRadar.destroy()
  if (evalRadar) evalRadar.destroy()
})

watch(() => data.value.activities, () => nextTick(() => {
  renderChart()
  renderRadarCharts()
}))
watch(() => [quiz.value.dimensions, evaluation.value.dimensions], () => nextTick(renderRadarCharts), { deep: true })
</script>

<style scoped>
.cld-page {
  min-height: calc(100vh - 72px);
  margin: -16px -20px -24px;
  background: #eef0f6;
}
.class-code-banner {
  margin: 0;
  padding: 14px 20px;
  background: linear-gradient(90deg, #fef3c7 0%, #fff7ed 100%);
  border-bottom: 1px solid #fcd34d;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 20px;
}
.banner-text {
  flex: 1;
  min-width: 220px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #92400e;
}
.banner-text strong {
  font-size: 15px;
  color: #78350f;
}
.banner-code-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}
.banner-code {
  font-family: ui-monospace, monospace;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: 0.2em;
  color: #b45309;
  min-width: 8.5em;
  text-align: center;
}
.banner-btn {
  border: 1px solid #f59e0b;
  background: #fff;
  color: #b45309;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.banner-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.banner-expires {
  width: 100%;
  margin: 0;
  font-size: 12px;
  color: #a16207;
}
.cld-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 16px;
  padding: 14px 20px;
  background: linear-gradient(135deg, #5b4fc7 0%, #7c3aed 45%, #9333ea 100%);
  color: #fff;
  box-shadow: 0 4px 20px rgba(91, 79, 199, 0.35);
}
.header-left { flex: 0 0 auto; }
.header-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.title-icon { font-size: 20px; }
.live-tag { margin-left: 4px; }
.sse-tag { margin-left: 0; }
.header-pills {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}
.pill {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  font-size: 12px;
  backdrop-filter: blur(4px);
}
.pill-label { opacity: 0.85; white-space: nowrap; }
.pill-value { font-weight: 600; white-space: nowrap; }
.pill-select { padding-right: 4px; }
.filter-select { width: 140px; }
.grade-select { width: 130px; }
.lesson-select { width: 200px; }
.class-select { width: 120px; }
.pill-code {
  padding-right: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.code-val {
  font-family: ui-monospace, monospace;
  font-size: 15px;
  letter-spacing: 0.12em;
}
.code-copy,
.code-refresh {
  border: none;
  background: rgba(255,255,255,0.2);
  color: #fff;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
}
.filter-select :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: none;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.action-btn {
  padding: 8px 14px;
  border: none;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
}
.action-btn.orange { background: linear-gradient(135deg, #f97316, #fb923c); }
.action-btn.pink { background: linear-gradient(135deg, #ec4899, #f472b6); }
.action-btn.indigo { background: linear-gradient(135deg, #4338ca, #6366f1); }
.action-btn.teal { background: linear-gradient(135deg, #0d9488, #14b8a6); }
.icon-btn {
  width: 36px; height: 36px;
  border: 1px solid rgba(255,255,255,0.4);
  border-radius: 8px;
  background: rgba(255,255,255,0.1);
  color: #fff;
  font-size: 18px;
  cursor: pointer;
}

.cards-row {
  display: grid;
  grid-template-columns: 1.4fr 0.8fr 0.8fr;
  gap: 14px;
  padding: 16px 20px 0;
}
@media (max-width: 1200px) {
  .cards-row { grid-template-columns: 1fr; }
}
.card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.06);
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.card h3 {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 700;
  color: #334155;
}
.legend { display: flex; gap: 12px; font-size: 11px; color: #64748b; }
.dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 4px; }
.dot.green { background: #22c55e; }
.dot.red { background: #f87171; }
.dot.blue { background: #6366f1; }
.chart-wrap { height: 220px; position: relative; }
.radar-card h3 { margin-bottom: 6px; }
.radar-meta {
  margin: 0 0 8px;
  font-size: 12px;
  color: #64748b;
}
.radar-wrap {
  height: 220px;
  position: relative;
}
.eval-charts-placeholder {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.placeholder-box {
  height: 72px;
  border: 1px dashed #e2e8f0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #94a3b8;
}

.progress-section,
.matrix-section,
.park-section {
  margin: 16px 20px 0;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.06);
}
.matrix-section { margin-bottom: 20px; }
.park-tip {
  margin: -4px 0 12px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px 12px;
  margin-bottom: 14px;
}
.section-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #334155;
  display: flex;
  align-items: center;
  gap: 6px;
}
.view-toggle {
  display: flex;
  gap: 4px;
  background: #f1f5f9;
  padding: 3px;
  border-radius: 8px;
}
.view-toggle button {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  background: transparent;
  font-size: 12px;
  color: #64748b;
  cursor: pointer;
}
.view-toggle button.active {
  background: #fff;
  color: #5b4fc7;
  font-weight: 600;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.activity-list { display: flex; flex-direction: column; gap: 10px; }
.activity-row {
  display: grid;
  grid-template-columns: 36px 1fr 180px 52px auto;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  background: #fafbfc;
}
@media (max-width: 900px) {
  .activity-row { grid-template-columns: 1fr; }
}
.act-index {
  width: 32px; height: 32px;
  border-radius: 8px;
  background: #ede9fe;
  color: #5b4fc7;
  font-weight: 700;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.act-title { font-weight: 600; font-size: 14px; color: #334155; display: flex; align-items: center; gap: 6px; }
.act-meta { font-size: 12px; color: #94a3b8; margin-top: 2px; }
.act-rate { font-weight: 700; color: #5b4fc7; font-size: 14px; text-align: right; }
.act-actions { display: flex; gap: 6px; flex-wrap: wrap; }
.act-btn {
  padding: 6px 10px;
  border: none;
  border-radius: 6px;
  background: #5b4fc7;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
}
.act-btn.outline { background: #fff; color: #5b4fc7; border: 1px solid #c4b5fd; }
.act-btn.sm { padding: 4px 8px; font-size: 11px; }

.activity-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}
.grid-card {
  padding: 14px;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  background: #fafbfc;
}
.grid-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.grid-num { font-weight: 700; color: #5b4fc7; }
.grid-title { font-size: 13px; font-weight: 600; color: #334155; margin-bottom: 8px; min-height: 36px; }
.grid-count { font-size: 12px; color: #64748b; margin-bottom: 6px; }
.grid-actions { margin-top: 8px; }

.matrix-wrap { overflow-x: auto; }
.matrix-legend { margin: 12px 0 0; font-size: 13px; }
.matrix-legend .done { color: #67c23a; font-weight: bold; margin-right: 20px; }
.matrix-legend .undone { color: #f56c6c; }
.done { color: #67c23a; font-weight: bold; }
.undone { color: #f56c6c; }

.empty-page { padding: 48px 20px; }
.empty-hint { text-align: center; color: #94a3b8; font-size: 13px; padding: 12px; grid-column: 1 / -1; }
</style>
