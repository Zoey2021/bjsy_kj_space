<template>
  <div class="pretest-teacher" v-loading="loading">
    <header class="pt-head">
      <div>
        <el-button link type="primary" @click="router.push('/teacher/course-map')">← 返回课程地图</el-button>
        <h1>六年级前测 · 提交监控</h1>
        <p>仅显示 2021 级班级。A 准备度很好（总分≥85 且操作关键项满分），B 基本能跟上，C 需要脚手架（总分&lt;65 或概念较模糊）。满分 100。</p>
      </div>
      <div class="pt-actions">
        <el-select v-model="classId" placeholder="选择2021级班级" filterable style="width: 220px" @change="loadData">
          <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-button @click="openPaper">预览试卷</el-button>
        <el-button type="primary" :disabled="!rows.length" @click="exportCsv">导出 CSV</el-button>
        <el-button type="danger" plain :disabled="!classId || !submittedCount" @click="clearClass">清空本班</el-button>
      </div>
    </header>

    <div class="pt-stat" v-if="overview">
      <span class="pill">班级 {{ overview.className }}</span>
      <span class="pill">已提交 {{ overview.submittedCount }} / {{ overview.totalStudents }}</span>
      <span class="pill pill-a">A {{ levelCount.A }} 人</span>
      <span class="pill pill-b">B {{ levelCount.B }} 人</span>
      <span class="pill pill-c">C {{ levelCount.C }} 人</span>
    </div>
    <p v-else-if="!classes.length" class="empty-hint">当前账号下没有 2021 级班级，无法查看六年级前测。</p>

    <el-table :data="rows" stripe height="calc(100vh - 220px)" :empty-text="classes.length ? '请选择班级' : '没有可选的2021级班级'">
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column prop="studentNo" label="学号" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.submitted ? 'success' : 'info'" size="small">
            {{ row.submitted ? '已提交' : '未交' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="fillScore" label="填空/20" width="90" />
      <el-table-column label="选择" width="120">
        <template #default="{ row }">
          <span v-if="row.submitted">{{ row.choiceRight }}/10（{{ row.choiceScore }}分）</span>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column prop="opScore" label="操作/40" width="90" />
      <el-table-column prop="attScore" label="情感/10" width="90" />
      <el-table-column prop="totalScore" label="总分/100" width="100" />
      <el-table-column label="分类" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.submitted" :type="levelTagType(row)" size="small" effect="dark">
            {{ classifyLevel(row) }}
          </el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" min-width="160">
        <template #default="{ row }">{{ formatTime(row.submittedAt) }}</template>
      </el-table-column>
      <el-table-column label="明细" width="80" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.submitted" link type="primary" @click="openDetail(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="detailVisible" :title="detailTitle" width="680px" append-to-body>
      <div v-if="detail" class="detail-body">
        <p><b>一、填空题</b></p>
        <div v-for="(ans, i) in detail.fill || []" :key="'f'+i" class="ans">
          {{ i + 1 }}. 学生答：{{ formatFill(ans) }}
          <span class="hint">参考：{{ FILL_HINT[i] }}</span>
        </div>
        <p><b>二、选择题</b></p>
        <div v-for="(ans, i) in detail.choice || []" :key="'c'+i" class="ans">
          {{ i + 1 }}. 学生选 {{ ans || '（空）' }}
          <span :class="ans === CHOICE_KEY[i] ? 'ok' : 'bad'">
            {{ ans === CHOICE_KEY[i] ? '✓' : '✗ 应 ' + CHOICE_KEY[i] }}
          </span>
        </div>
        <p><b>三、操作题</b></p>
        <div class="ans">泡茶顺序：{{ (detail.op?.order1 || []).join(' → ') || '—' }}</div>
        <div class="ans">过马路顺序：{{ (detail.op?.order2 || []).join(' → ') || '—' }}</div>
        <div class="ans">判断步骤：{{ detail.op?.branchPick || '—' }}　图形：{{ detail.op?.shapePick || '—' }}　流程图：{{ detail.op?.flowGenerated ? '已生成' : '未生成' }}</div>
        <div class="ans">Excel 总人数：{{ detail.op?.q31 || '—' }}</div>
        <div class="ans">Excel 方法：{{ detail.op?.q32 || '—' }}　最受欢迎：{{ detail.op?.fruitPick || '—' }}</div>
        <p><b>四、情感态度</b></p>
        <div v-for="(ans, i) in detail.att || []" :key="'a'+i" class="ans">{{ i + 1 }}. {{ ans || '（空）' }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getClasses, teacherGetPretestG6, teacherClearPretestG6 } from '../../api'
import { filterClassesByCohort } from '../../utils/classCohort'

const FILL_HINT = ['顺序/步骤', '流程', '有限/有穷、确定、输出', '顺序', '分支/选择', '循环', 'COUNT/计数', '分解', '抽象', 'AVERAGE/平均']
const CHOICE_KEY = ['B', 'B', 'B', 'C', 'B', 'C', 'B', 'A', 'A', 'B']

const router = useRouter()
const loading = ref(false)
const classes = ref([])
const classId = ref(null)
const overview = ref(null)
const detailVisible = ref(false)
const detail = ref(null)
const detailTitle = ref('')

const rows = computed(() => overview.value?.students || [])
const submittedCount = computed(() => Number(overview.value?.submittedCount || 0))

const classifyLevel = (row) => {
  const lv = String(row?.level || '').toUpperCase()
  if (lv === 'A' || lv === 'B' || lv === 'C') return lv
  const n = Number(row?.totalScore)
  if (!Number.isFinite(n)) return ''
  if (n >= 85) return 'A'
  if (n >= 65) return 'B'
  return 'C'
}

const levelTagType = (row) => {
  const lv = classifyLevel(row)
  if (lv === 'A') return 'success'
  if (lv === 'B') return 'warning'
  return 'danger'
}

const levelCount = computed(() => {
  const c = { A: 0, B: 0, C: 0 }
  for (const r of rows.value) {
    if (!r.submitted) continue
    const lv = classifyLevel(r)
    if (c[lv] != null) c[lv] += 1
  }
  return c
})

const formatTime = (v) => {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 19)
}

const formatFill = (ans) => {
  if (Array.isArray(ans)) return ans.filter(Boolean).join(' / ') || '（空）'
  return ans || '（空）'
}

const loadData = async () => {
  if (!classId.value) {
    overview.value = null
    return
  }
  loading.value = true
  try {
    const res = await teacherGetPretestG6(classId.value)
    overview.value = res.data || null
  } finally {
    loading.value = false
  }
}

const openPaper = () => {
  router.push('/teacher/pretest/g6/paper')
}

const openDetail = (row) => {
  detailTitle.value = `${row.realName} · 前测明细`
  detail.value = row.detail || {}
  detailVisible.value = true
}

const clearClass = async () => {
  if (!classId.value || !submittedCount.value) return
  const className = overview.value?.className || '当前班级'
  try {
    await ElMessageBox.confirm(
      `确定清空「${className}」的 ${submittedCount.value} 条六年级前测提交？清空后学生可重新作答，此操作不可恢复。`,
      '清空确认',
      { type: 'warning', confirmButtonText: '确定清空', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  loading.value = true
  try {
    const res = await teacherClearPretestG6(classId.value)
    ElMessage.success(res.message || '已清空')
    await loadData()
  } finally {
    loading.value = false
  }
}

const exportCsv = () => {
  if (!rows.value.length) return
  const header = '姓名,班级,学号,状态,填空,选择答对,选择分,操作,情感,总分,分类,提交时间'
  const lines = rows.value.map((r) => [
    r.realName,
    r.className,
    r.studentNo,
    r.submitted ? '已提交' : '未交',
    r.fillScore ?? '',
    r.choiceRight ?? '',
    r.choiceScore ?? '',
    r.opScore ?? '',
    r.attScore ?? '',
    r.totalScore ?? '',
    r.submitted ? classifyLevel(r) : '',
    formatTime(r.submittedAt)
  ].join(','))
  const blob = new Blob(['\uFEFF' + [header, ...lines].join('\n')], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `六年级前测_${overview.value?.className || '班级'}.csv`
  a.click()
  ElMessage.success('已导出')
}

onMounted(async () => {
  const res = await getClasses().catch(() => ({ data: [] }))
  classes.value = filterClassesByCohort(res.data || [], '2021')
  if (classes.value[0]) {
    classId.value = classes.value[0].id
    await loadData()
  }
})
</script>

<style scoped>
.pretest-teacher {
  height: 100%;
  min-height: 0;
  padding: 8px 4px 0;
}
.pt-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 12px;
}
.pt-head h1 {
  margin: 4px 0 4px;
  font-size: 20px;
}
.pt-head p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}
.pt-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  align-items: center;
}
.pt-stat {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}
.empty-hint {
  color: #b45309;
  font-size: 13px;
  margin: 0 0 10px;
}
.pill {
  background: #eef3fa;
  border: 1px solid #c5d4e8;
  border-radius: 999px;
  padding: 4px 12px;
  font-size: 13px;
}
.pill-a { background: #ecfdf5; border-color: #86efac; color: #15803d; }
.pill-b { background: #fffbeb; border-color: #fcd34d; color: #b45309; }
.pill-c { background: #fef2f2; border-color: #fca5a5; color: #b91c1c; }
.detail-body p { margin: 12px 0 6px; }
.ans { font-size: 13px; margin: 4px 0; }
.hint { color: #94a3b8; margin-left: 8px; }
.ok { color: #15803d; font-weight: 700; }
.bad { color: #b91c1c; font-weight: 700; }
</style>
