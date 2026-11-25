<template>
  <div class="uq-teacher" v-loading="loading">
    <header class="head">
      <div>
        <el-button link type="primary" @click="goBack">← 返回</el-button>
        <h1>{{ title || '第一单元小测' }}</h1>
        <p>学生提交后系统已自动批改。此处可核对作答、微调分数；学生端只显示等级。请将「当前课时」设为第一单元小测，完成的学生才能申请游学乐园。</p>
      </div>
      <div class="actions">
        <el-select v-model="classId" placeholder="选择班级" filterable style="width: 220px" @change="loadData">
          <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-button v-if="paperHtmlTeacher" @click="openTeacherPaper">教师用卷</el-button>
        <el-button @click="loadData" :disabled="!classId">刷新</el-button>
      </div>
    </header>

    <div class="stat" v-if="overview">
      <span class="pill">班级 {{ overview.className }}</span>
      <span class="pill">已提交 {{ overview.submittedCount }} / {{ overview.totalStudents }}</span>
      <span class="pill">已批改 {{ overview.gradedCount }}</span>
      <span class="pill pill-a">A {{ levelCount.A || 0 }}</span>
      <span class="pill pill-b">B {{ levelCount.B || 0 }}</span>
      <span class="pill pill-c">C {{ levelCount.C || 0 }}</span>
      <span class="pill pill-d">D {{ levelCount.D || 0 }}</span>
    </div>

    <el-table :data="rows" stripe height="calc(100vh - 220px)" :empty-text="classId ? '本班暂无学生' : '请选择班级'">
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column prop="studentNo" label="学号" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.graded" type="success" size="small">已批改</el-tag>
          <el-tag v-else-if="row.submitted" type="warning" size="small">待批改</el-tag>
          <el-tag v-else type="info" size="small">未交</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="?" width="70">
        <template #default="{ row }">
          <span v-if="unsureCount(row)">{{ unsureCount(row) }}</span>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="分数" width="90">
        <template #default="{ row }">{{ row.graded ? row.score : '—' }}</template>
      </el-table-column>
      <el-table-column label="等级" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.graded" :type="levelTag(row.gradeLevel)" size="small" effect="dark">
            {{ row.gradeLevel }}
          </el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" min-width="160">
        <template #default="{ row }">{{ formatTime(row.submittedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.submitted" link type="primary" @click="openDetail(row)">查看/批改</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="detailVisible" :title="detailTitle" width="860px" append-to-body destroy-on-close top="4vh">
      <div v-if="current" class="detail" v-loading="scoring">
        <div class="detail-toolbar">
          <el-button type="primary" plain @click="runAutoScore">一键自动计分</el-button>
          <span class="tiny">填空按参考答法模糊匹配；17（2）写「=」不计分；19（1）写「≠」建议半分。</span>
        </div>

        <div v-for="item in scoreDetail" :key="item.id" class="ans-row" :class="{ wrong: item.score === 0, half: item.score > 0 && item.score < item.maxScore, ok: item.score === item.maxScore }">
          <div class="ans-head">
            <b>第 {{ item.no }} 题{{ item.label ? ' · ' + item.label : '' }}</b>
            <span>{{ item.score }} / {{ item.maxScore }}</span>
            <el-tag v-if="isUnsure(current, item.questionId || item.id)" size="small" type="warning">标了 ?</el-tag>
          </div>
          <p><span class="lbl">学生：</span>{{ formatStudentAns(item) }}</p>
          <p><span class="lbl">参考：</span>{{ formatRefAns(item) }}</p>
        </div>

        <el-divider />
        <div class="grade-row">
          <span>总分（0～{{ maxScore }}）</span>
          <el-input-number v-model="scoreInput" :min="0" :max="maxScore" :step="1" />
          <el-tag v-if="previewLevel" :type="levelTag(previewLevel)" effect="dark">预览 {{ previewLevel }}</el-tag>
        </div>
        <p class="preview-fb">{{ previewFeedback }}</p>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">取消</el-button>
        <el-button type="primary" :loading="grading" @click="saveGrade">保存批改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getClasses, teacherGetUnitQuiz, teacherGradeUnitQuiz, teacherAutoScoreUnitQuiz } from '../../api'

const route = useRoute()
const router = useRouter()
const lessonId = computed(() => Number(route.params.lessonId || route.params.id))

const loading = ref(false)
const grading = ref(false)
const scoring = ref(false)
const classes = ref([])
const classId = ref(null)
const overview = ref(null)
const detailVisible = ref(false)
const detailTitle = ref('')
const current = ref(null)
const scoreInput = ref(0)
const scoreDetail = ref([])
const title = ref('')
const questions = ref([])
const maxScore = ref(100)
const paperHtmlTeacher = ref('')

const rows = computed(() => overview.value?.students || [])
const levelCount = computed(() => overview.value?.levelCount || { A: 0, B: 0, C: 0, D: 0 })

const previewLevel = computed(() => {
  const n = Number(scoreInput.value)
  if (!Number.isFinite(n)) return ''
  if (n >= 80) return 'A'
  if (n >= 60) return 'B'
  if (n >= 40) return 'C'
  return 'D'
})

const previewFeedback = computed(() => {
  const map = {
    A: '本单元学的很好！继续保持',
    B: '本单元学的还行！但是还需要查缺补漏，有问题多问问张老师噢！',
    C: '本单元学起来是不是感觉很困难！多找张老师聊一聊吧！',
    D: '你是不是没有好好学？'
  }
  return map[previewLevel.value] || ''
})

const levelTag = (lv) => {
  if (lv === 'A') return 'success'
  if (lv === 'B') return ''
  if (lv === 'C') return 'warning'
  return 'danger'
}

const formatTime = (v) => {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 19)
}

const unsureCount = (row) => {
  const u = row?.answers?.unsure
  if (!u || typeof u !== 'object') return 0
  return Object.values(u).filter(Boolean).length
}

const isUnsure = (row, qid) => {
  const u = row?.answers?.unsure || {}
  if (u[qid]) return true
  // fill blank ids like q16_1 → check parent q16
  const parent = String(qid || '').split('_')[0]
  return !!u[parent]
}

const formatStudentAns = (item) => {
  const v = item.student
  if (v == null || v === '') return '（空）'
  if (Array.isArray(v)) return v.join('') || '（空）'
  return String(v)
}

const formatRefAns = (item) => {
  const v = item.answer
  if (v == null) return '—'
  if (Array.isArray(v)) return v.join('')
  if (typeof v === 'object') return JSON.stringify(v)
  return String(v)
}

const goBack = () => {
  if (route.query.gradeId) {
    router.push(`/teacher/course-map/grade/${route.query.gradeId}`)
    return
  }
  router.push('/teacher/course-map')
}

const openTeacherPaper = () => {
  if (paperHtmlTeacher.value) window.open(paperHtmlTeacher.value, '_blank')
}

const loadData = async () => {
  if (!classId.value) return
  loading.value = true
  try {
    const res = await teacherGetUnitQuiz(lessonId.value, classId.value)
    const data = res.data || {}
    overview.value = data
    title.value = data.title || '第一单元小测'
    questions.value = data.questions || []
    maxScore.value = Number(data.maxScore) || 100
    paperHtmlTeacher.value = data.paperHtmlTeacher || ''
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const openDetail = async (row) => {
  current.value = row
  detailTitle.value = `${row.realName} · 作答详情`
  scoreInput.value = row.graded ? Number(row.score) : 0
  scoreDetail.value = []
  detailVisible.value = true
  await runAutoScore(!row.graded)
}

const runAutoScore = async (applySuggested = true) => {
  if (!current.value) return
  scoring.value = true
  try {
    const res = await teacherAutoScoreUnitQuiz(lessonId.value, current.value.studentId)
    const data = res.data || {}
    scoreDetail.value = data.detail || []
    if (applySuggested) {
      scoreInput.value = Number(data.suggestedScore) || 0
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '自动计分失败')
  } finally {
    scoring.value = false
  }
}

const saveGrade = async () => {
  if (!current.value) return
  grading.value = true
  try {
    await teacherGradeUnitQuiz(lessonId.value, {
      studentId: current.value.studentId,
      score: Number(scoreInput.value)
    })
    ElMessage.success('批改已保存')
    detailVisible.value = false
    await loadData()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  } finally {
    grading.value = false
  }
}

onMounted(async () => {
  const res = await getClasses().catch(() => ({ data: [] }))
  classes.value = res.data || []
  if (classes.value.length) {
    classId.value = classes.value[0].id
    await loadData()
  }
})
</script>

<style scoped>
.uq-teacher { padding: 16px 20px 24px; }
.head {
  display: flex; justify-content: space-between; gap: 16px;
  align-items: flex-start; margin-bottom: 14px;
}
.head h1 { margin: 4px 0 6px; font-size: 22px; color: #0f172a; }
.head p { margin: 0; color: #64748b; font-size: 13px; }
.actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.stat { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.pill {
  background: #f1f5f9; color: #334155; border-radius: 999px;
  padding: 4px 12px; font-size: 13px;
}
.pill-a { background: #dcfce7; color: #166534; }
.pill-b { background: #dbeafe; color: #1e40af; }
.pill-c { background: #ffedd5; color: #9a3412; }
.pill-d { background: #fee2e2; color: #991b1b; }
.detail { max-height: 62vh; overflow: auto; padding-right: 4px; }
.detail-toolbar {
  display: flex; align-items: center; gap: 12px; margin-bottom: 12px; flex-wrap: wrap;
}
.tiny { font-size: 12px; color: #64748b; }
.ans-row {
  border: 1px solid #e2e8f0; border-radius: 10px;
  padding: 10px 12px; margin-bottom: 8px; background: #f8fafc;
}
.ans-row.ok { border-color: #86efac; background: #f0fdf4; }
.ans-row.half { border-color: #fcd34d; background: #fffbeb; }
.ans-row.wrong { border-color: #fca5a5; background: #fef2f2; }
.ans-head {
  display: flex; align-items: center; gap: 10px; margin-bottom: 4px;
}
.ans-head b { flex: 1; color: #0f172a; }
.lbl { color: #64748b; margin-right: 4px; }
.ans-row p { margin: 2px 0; color: #334155; white-space: pre-wrap; line-height: 1.45; }
.grade-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.preview-fb { margin: 10px 0 0; color: #475569; }
</style>
