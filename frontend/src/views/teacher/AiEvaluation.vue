<template>
  <div class="eval-page" v-loading="loading">
    <header class="eval-header">
      <div class="header-main">
        <h1><span class="title-icon">✨</span> 智能评价</h1>
        <p>查看任课班级学生的学习过程档案与综合等级。过程分看完成、轨迹与态度，结果分看课堂小测，二者合成综合等级。</p>
      </div>
      <div class="header-filters">
        <el-select v-model="classId" placeholder="选择班级" filterable style="width:200px" @change="onClassChange">
          <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="gradeId" placeholder="教材册次" filterable style="width:200px" @change="onGradeChange">
          <el-option v-for="g in grades" :key="g.id" :label="g.name" :value="g.id" />
        </el-select>
        <button type="button" class="action-btn indigo" :disabled="!classId || !gradeId" @click="reload">刷新</button>
      </div>
    </header>

    <div class="eval-body">
      <el-alert
        v-if="classesReady && !classes.length"
        type="info"
        show-icon
        :closable="false"
        title="暂无任课班级。请先在学校管理中匹配班级。"
      />

      <!-- 花名册 -->
      <template v-if="view === 'roster'">
        <div v-if="roster" class="summary-bar">
          <div class="sum-item"><span>学生</span><b>{{ roster.studentCount || 0 }}</b></div>
          <div class="sum-item"><span>课时</span><b>{{ roster.lessonCount || 0 }}</b></div>
          <div class="sum-item lv-excellent"><span>优秀</span><b>{{ roster.levelCounts?.excellent || 0 }}</b></div>
          <div class="sum-item lv-good"><span>良好</span><b>{{ roster.levelCounts?.good || 0 }}</b></div>
          <div class="sum-item lv-pass"><span>合格</span><b>{{ roster.levelCounts?.pass || 0 }}</b></div>
          <div class="sum-item lv-weak"><span>待提升</span><b>{{ roster.levelCounts?.weak || 0 }}</b></div>
          <div class="sum-item"><span>暂无数据</span><b>{{ roster.levelCounts?.noData || 0 }}</b></div>
        </div>

        <div class="panel">
          <div class="panel-head">
            <h2>{{ roster?.className || '班级' }} · 学生评价花名册</h2>
            <p class="hint">点击学生姓名查看各课学习档案</p>
          </div>
          <el-table :data="rosterStudents" stripe empty-text="请先选择班级与教材册次" class="soft-table">
            <el-table-column label="学生" min-width="140">
              <template #default="{ row }">
                <button type="button" class="name-link" @click="openStudent(row)">{{ row.realName }}</button>
                <div class="sub">{{ row.username }}</div>
              </template>
            </el-table-column>
            <el-table-column label="综合等级" width="110">
              <template #default="{ row }">
                <span class="level-tag" :class="levelClass(row.levelCode)">{{ row.levelLabel }}</span>
              </template>
            </el-table-column>
            <el-table-column label="综合分" width="90">
              <template #default="{ row }">{{ dash(row.compositeScore) }}</template>
            </el-table-column>
            <el-table-column label="过程分" width="90">
              <template #default="{ row }">{{ dash(row.processScore) }}</template>
            </el-table-column>
            <el-table-column label="结果分" width="90">
              <template #default="{ row }">{{ dash(row.resultScore) }}</template>
            </el-table-column>
            <el-table-column label="完成课时" width="110">
              <template #default="{ row }">{{ row.completedLessons || 0 }}/{{ row.lessonCount || 0 }}</template>
            </el-table-column>
            <el-table-column label="小测均分" width="100">
              <template #default="{ row }">{{ dash(row.avgQuizPercent) }}</template>
            </el-table-column>
            <el-table-column label="提示次数" width="90" prop="hintTotal" />
            <el-table-column label="学习时长" width="110">
              <template #default="{ row }">{{ formatSeconds(row.studySeconds) }}</template>
            </el-table-column>
          </el-table>
          <p v-if="roster?.rubric" class="rubric">
            {{ roster.rubric.process }}；{{ roster.rubric.result }}；{{ roster.rubric.composite }}；{{ roster.rubric.levels }}
          </p>
        </div>
      </template>

      <!-- 学生档案 -->
      <template v-else-if="view === 'student' && profile">
        <div class="crumb">
          <button type="button" class="link-btn" @click="backToRoster">← 返回花名册</button>
          <span>{{ profile.className }} / {{ profile.student?.realName }}</span>
        </div>

        <div class="profile-hero panel">
          <div class="hero-left">
            <h2>{{ profile.student?.realName }}</h2>
            <p>{{ profile.student?.username }} · {{ profile.gradeName }}</p>
            <div class="hero-meta" v-if="baselineText">
              <span>{{ baselineText }}</span>
            </div>
          </div>
          <div class="hero-right">
            <span class="level-tag big" :class="levelClass(profile.summary?.levelCode)">
              {{ profile.summary?.levelLabel || '暂无数据' }}
            </span>
            <div class="score-grid">
              <div><span>综合分</span><b>{{ dash(profile.summary?.compositeScore) }}</b></div>
              <div><span>过程分</span><b>{{ dash(profile.summary?.processScore) }}</b></div>
              <div><span>结果分</span><b>{{ dash(profile.summary?.resultScore) }}</b></div>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-head">
            <h2>各课学习状态</h2>
            <p class="hint">点击某一课查看小测三维、自评与过程轨迹</p>
          </div>
          <div class="lesson-grid">
            <button
              v-for="les in profile.lessons || []"
              :key="les.lessonId"
              type="button"
              class="lesson-card"
              :class="{ muted: !les.hasData }"
              @click="openLesson(les)"
            >
              <div class="lc-top">
                <strong>{{ les.lessonTitle }}</strong>
                <span class="level-tag sm" :class="levelClass(les.levelCode)">{{ les.levelLabel }}</span>
              </div>
              <div class="lc-unit">{{ les.unitName }}</div>
              <div class="lc-stats">
                <span>进度 {{ les.progressPercent || 0 }}%</span>
                <span>探究 {{ les.inquiryDone || 0 }}</span>
                <span>小测 {{ les.quizSubmitted ? (dash(les.quizPercent) + '分') : '未交' }}</span>
                <span>自评 {{ les.evaluationSubmitted ? '已填' : '未填' }}</span>
              </div>
            </button>
          </div>
        </div>
      </template>

      <!-- 单课详情 -->
      <template v-else-if="view === 'lesson' && lessonDetail">
        <div class="crumb">
          <button type="button" class="link-btn" @click="backToStudent">← 返回学生档案</button>
          <span>{{ lessonDetail.student?.realName }} / {{ lessonDetail.lesson?.lessonTitle }}</span>
        </div>

        <div class="profile-hero panel">
          <div class="hero-left">
            <h2>{{ lessonDetail.lesson?.lessonTitle }}</h2>
            <p>{{ lessonDetail.unitName }} · {{ lessonDetail.gradeName }}</p>
          </div>
          <div class="hero-right">
            <span class="level-tag big" :class="levelClass(lessonDetail.grade?.levelCode)">
              {{ lessonDetail.grade?.levelLabel || '暂无数据' }}
            </span>
            <div class="score-grid">
              <div><span>综合分</span><b>{{ dash(lessonDetail.grade?.compositeScore) }}</b></div>
              <div><span>过程分</span><b>{{ dash(lessonDetail.grade?.processScore) }}</b></div>
              <div><span>结果分</span><b>{{ dash(lessonDetail.grade?.resultScore) }}</b></div>
            </div>
          </div>
        </div>

        <div class="detail-grid">
          <section class="panel">
            <h3>学习进度</h3>
            <ul class="kv">
              <li><span>状态</span><b>{{ statusLabel(lessonDetail.progress?.status) }}</b></li>
              <li><span>进度</span><b>{{ lessonDetail.progress?.progressPercent || 0 }}%</b></li>
              <li><span>学习时长</span><b>{{ formatSeconds(lessonDetail.progress?.studySeconds) }}</b></li>
              <li><span>最近访问</span><b>{{ formatTime(lessonDetail.progress?.lastVisitAt) }}</b></li>
              <li><span>提示次数</span><b>{{ lessonDetail.hints?.total || 0 }}</b></li>
            </ul>
          </section>

          <section class="panel">
            <h3>探究环节</h3>
            <div v-if="!(lessonDetail.activities || []).length" class="empty">暂无环节数据</div>
            <ul v-else class="act-list">
              <li v-for="act in lessonDetail.activities" :key="act.index + '-' + act.type">
                <span class="dot" :class="{ on: act.done }" />
                <div>
                  <strong>{{ act.title }}</strong>
                  <div class="sub">{{ act.done ? '已完成' : '未完成' }}{{ act.firstAt ? ' · ' + formatTime(act.firstAt) : '' }}</div>
                </div>
              </li>
            </ul>
          </section>

          <section class="panel">
            <h3>课堂小测 · 三维</h3>
            <div v-if="!lessonDetail.quiz?.submitted" class="empty">本课尚未提交小测</div>
            <template v-else>
              <p class="quiz-total">得分 {{ dash(lessonDetail.quiz.percent) }} 分
                <span v-if="lessonDetail.quiz.correctCount >= 0">
                  （对 {{ lessonDetail.quiz.correctCount }}/{{ lessonDetail.quiz.totalQuestions }} 题）
                </span>
              </p>
              <div class="dim-bars">
                <div v-for="d in lessonDetail.quiz.dimensions || []" :key="d.name" class="dim-row">
                  <span class="dim-name">{{ d.name }}</span>
                  <div class="bar"><i :style="{ width: (d.percent || 0) + '%' }" /></div>
                  <span class="dim-score">{{ d.score }}/{{ d.maxScore }}</span>
                </div>
              </div>
            </template>
          </section>

          <section class="panel">
            <h3>学习评价 · 自评</h3>
            <div v-if="!lessonDetail.evaluation?.submitted" class="empty">本课尚未填写学习评价</div>
            <ul v-else class="eval-answers">
              <li v-for="(ans, i) in lessonDetail.evaluation.answers || []" :key="i" :class="{ concern: ans.concern }">
                <span class="aspect">{{ ans.aspect }}</span>
                <b>{{ ans.label || ('选项 ' + ans.selected) }}</b>
                <em v-if="ans.concern">需关注</em>
              </li>
            </ul>
          </section>

          <section class="panel full">
            <h3>过程轨迹</h3>
            <div v-if="!(lessonDetail.trajectory || []).length" class="empty">暂无提交轨迹</div>
            <ol v-else class="timeline">
              <li v-for="(t, i) in lessonDetail.trajectory" :key="i">
                <time>{{ formatTime(t.createdAt) }}</time>
                <span>{{ t.label }}</span>
              </li>
            </ol>
          </section>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  getClasses,
  teacherGetTextbooks,
  getEvaluationRoster,
  getEvaluationStudent,
  getEvaluationLesson
} from '../../api'

const loading = ref(false)
const classesReady = ref(false)
const classes = ref([])
const grades = ref([])
const classId = ref(null)
const gradeId = ref(null)

const view = ref('roster') // roster | student | lesson
const roster = ref(null)
const profile = ref(null)
const lessonDetail = ref(null)
const currentStudentId = ref(null)

const rosterStudents = computed(() => roster.value?.students || [])

const baselineText = computed(() => {
  const b = profile.value?.baseline || {}
  const parts = []
  if (b.g6Level || b.g6Total != null) {
    parts.push(`六上前测 ${b.g6Level || ''} ${b.g6Total != null ? b.g6Total + '分' : ''}`.trim())
  }
  if (b.g4Total != null) parts.push(`四上前测 ${b.g4Total}分`)
  if (b.tierOverride) parts.push(`教师覆盖档 ${b.tierOverride}`)
  return parts.join(' · ')
})

function dash(v) {
  return v == null || v === '' ? '—' : v
}

function levelClass(code) {
  if (code === 'EXCELLENT') return 'lv-excellent'
  if (code === 'GOOD') return 'lv-good'
  if (code === 'PASS') return 'lv-pass'
  if (code === 'WEAK') return 'lv-weak'
  return 'lv-none'
}

function statusLabel(s) {
  if (s === 'COMPLETED') return '已完成'
  if (s === 'IN_PROGRESS') return '学习中'
  return '未开始'
}

function formatSeconds(sec) {
  const n = Number(sec) || 0
  if (n < 60) return n + ' 秒'
  const m = Math.floor(n / 60)
  const s = n % 60
  if (m < 60) return s ? `${m} 分 ${s} 秒` : `${m} 分钟`
  const h = Math.floor(m / 60)
  return `${h} 小时 ${m % 60} 分`
}

function formatTime(v) {
  if (!v) return '—'
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return String(v)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

async function loadRoster() {
  if (!classId.value || !gradeId.value) {
    roster.value = null
    return
  }
  loading.value = true
  try {
    const res = await getEvaluationRoster(classId.value, gradeId.value)
    roster.value = res.data || null
    view.value = 'roster'
    profile.value = null
    lessonDetail.value = null
  } finally {
    loading.value = false
  }
}

async function openStudent(row) {
  if (!classId.value || !gradeId.value || !row?.studentId) return
  loading.value = true
  currentStudentId.value = row.studentId
  try {
    const res = await getEvaluationStudent(classId.value, row.studentId, gradeId.value)
    profile.value = res.data || null
    lessonDetail.value = null
    view.value = 'student'
  } finally {
    loading.value = false
  }
}

async function openLesson(les) {
  if (!classId.value || !currentStudentId.value || !les?.lessonId) return
  loading.value = true
  try {
    const res = await getEvaluationLesson(classId.value, currentStudentId.value, les.lessonId)
    lessonDetail.value = res.data || null
    view.value = 'lesson'
  } finally {
    loading.value = false
  }
}

function backToRoster() {
  view.value = 'roster'
  profile.value = null
  lessonDetail.value = null
}

function backToStudent() {
  view.value = 'student'
  lessonDetail.value = null
}

function onClassChange() {
  loadRoster()
}

function onGradeChange() {
  if (view.value === 'student' && currentStudentId.value) {
    openStudent({ studentId: currentStudentId.value })
  } else {
    loadRoster()
  }
}

function reload() {
  if (view.value === 'lesson' && lessonDetail.value?.lesson?.lessonId) {
    openLesson({ lessonId: lessonDetail.value.lesson.lessonId })
  } else if (view.value === 'student' && currentStudentId.value) {
    openStudent({ studentId: currentStudentId.value })
  } else {
    loadRoster()
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const [clsRes, gradeRes] = await Promise.all([
      getClasses(),
      teacherGetTextbooks('MAIN')
    ])
    classes.value = clsRes.data || []
    grades.value = gradeRes.data || []
    classesReady.value = true
    if (classes.value[0]) classId.value = classes.value[0].id
    if (grades.value[0]) gradeId.value = grades.value[0].id
    await loadRoster()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.eval-page {
  min-height: calc(100vh - 56px);
  background: #f5f3ff;
}
.eval-header {
  padding: 14px 20px 16px;
  background: linear-gradient(135deg, #5b4fc7 0%, #7c3aed 45%, #9333ea 100%);
  color: #fff;
  box-shadow: 0 4px 20px rgba(91, 79, 199, 0.35);
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  justify-content: space-between;
  align-items: flex-end;
}
.header-main h1 {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}
.title-icon { font-size: 20px; }
.header-main p {
  margin: 0;
  font-size: 12px;
  opacity: 0.92;
  line-height: 1.5;
  max-width: 640px;
}
.header-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.eval-body {
  padding: 16px 20px 32px;
  max-width: 1180px;
}
.summary-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}
.sum-item {
  background: #fff;
  border: 1px solid #e9e5ff;
  border-radius: 12px;
  padding: 10px 14px;
  min-width: 88px;
}
.sum-item span { display: block; font-size: 12px; color: #64748b; }
.sum-item b { font-size: 18px; color: #312e81; }
.panel {
  background: #fff;
  border: 1px solid #e9e5ff;
  border-radius: 14px;
  padding: 14px 16px 18px;
  box-shadow: 0 2px 12px rgba(91, 79, 199, 0.06);
  margin-bottom: 14px;
}
.panel-head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}
.panel-head h2, .panel h3 {
  margin: 0 0 10px;
  font-size: 16px;
  color: #312e81;
}
.panel-head h2 { margin-bottom: 0; }
.hint { margin: 0; color: #64748b; font-size: 12px; }
.rubric {
  margin: 12px 0 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.6;
}
.name-link {
  border: none;
  background: none;
  color: #4f46e5;
  font-weight: 700;
  cursor: pointer;
  padding: 0;
  font-size: 14px;
}
.name-link:hover { text-decoration: underline; }
.sub { font-size: 12px; color: #94a3b8; margin-top: 2px; }
.level-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  background: #e2e8f0;
  color: #475569;
}
.level-tag.sm { font-size: 11px; padding: 1px 8px; }
.level-tag.big { font-size: 16px; padding: 6px 14px; }
.lv-excellent, .level-tag.lv-excellent { background: #dcfce7; color: #166534; }
.lv-good, .level-tag.lv-good { background: #dbeafe; color: #1d4ed8; }
.lv-pass, .level-tag.lv-pass { background: #fef9c3; color: #a16207; }
.lv-weak, .level-tag.lv-weak { background: #fee2e2; color: #b91c1c; }
.lv-none, .level-tag.lv-none { background: #f1f5f9; color: #64748b; }
.action-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
}
.action-btn:disabled { opacity: 0.55; cursor: not-allowed; }
.action-btn.indigo { background: linear-gradient(135deg, #4338ca, #6366f1); }
.crumb {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  color: #475569;
  font-size: 13px;
}
.link-btn {
  border: none;
  background: #eef2ff;
  color: #4338ca;
  border-radius: 999px;
  padding: 6px 12px;
  cursor: pointer;
  font-weight: 600;
}
.profile-hero {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}
.hero-left h2 { margin: 0 0 4px; color: #312e81; }
.hero-left p { margin: 0; color: #64748b; font-size: 13px; }
.hero-meta { margin-top: 8px; font-size: 12px; color: #6366f1; }
.hero-right { text-align: right; }
.score-grid {
  display: flex;
  gap: 14px;
  margin-top: 10px;
  justify-content: flex-end;
}
.score-grid span { display: block; font-size: 11px; color: #64748b; }
.score-grid b { font-size: 18px; color: #312e81; }
.lesson-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
}
.lesson-card {
  text-align: left;
  border: 1px solid #e9e5ff;
  border-radius: 12px;
  padding: 12px;
  background: #faf9ff;
  cursor: pointer;
  transition: box-shadow .15s, transform .15s;
}
.lesson-card:hover {
  box-shadow: 0 6px 18px rgba(91, 79, 199, 0.12);
  transform: translateY(-1px);
}
.lesson-card.muted { opacity: 0.72; }
.lc-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: flex-start;
  margin-bottom: 4px;
}
.lc-top strong { color: #312e81; font-size: 14px; }
.lc-unit { font-size: 12px; color: #94a3b8; margin-bottom: 8px; }
.lc-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: #475569;
}
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.detail-grid .full { grid-column: 1 / -1; }
.kv, .act-list, .eval-answers, .timeline {
  list-style: none;
  margin: 0;
  padding: 0;
}
.kv li, .act-list li, .eval-answers li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed #ede9fe;
}
.kv li span { width: 88px; color: #64748b; font-size: 13px; }
.kv li b { color: #312e81; }
.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #cbd5e1;
  flex-shrink: 0;
}
.dot.on { background: #22c55e; }
.empty { color: #94a3b8; font-size: 13px; padding: 8px 0; }
.quiz-total { margin: 0 0 12px; color: #312e81; font-weight: 700; }
.dim-bars { display: flex; flex-direction: column; gap: 10px; }
.dim-row { display: grid; grid-template-columns: 72px 1fr 52px; gap: 8px; align-items: center; }
.dim-name { font-size: 12px; color: #475569; }
.bar {
  height: 8px;
  border-radius: 999px;
  background: #ede9fe;
  overflow: hidden;
}
.bar i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #6366f1, #a855f7);
}
.dim-score { font-size: 12px; color: #312e81; text-align: right; }
.eval-answers .aspect {
  width: 72px;
  color: #64748b;
  font-size: 13px;
}
.eval-answers .concern { background: #fff7ed; margin: 0 -8px; padding-left: 8px; padding-right: 8px; border-radius: 8px; }
.eval-answers em {
  margin-left: auto;
  font-style: normal;
  font-size: 11px;
  color: #c2410c;
  font-weight: 700;
}
.timeline li {
  display: grid;
  grid-template-columns: 160px 1fr;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px dashed #ede9fe;
  font-size: 13px;
}
.timeline time { color: #94a3b8; }
@media (max-width: 860px) {
  .detail-grid { grid-template-columns: 1fr; }
  .dim-row { grid-template-columns: 64px 1fr 44px; }
  .timeline li { grid-template-columns: 1fr; gap: 2px; }
}
</style>
