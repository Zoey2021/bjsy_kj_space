<template>
  <div class="pretest-teacher" v-loading="loading">
    <header class="pt-head">
      <div>
        <el-button link type="primary" @click="router.push('/teacher/course-map')">← 返回课程地图</el-button>
        <h1>四年级前测 · 提交监控</h1>
        <p>仅显示 2023 级班级。A 掌握最强（≥72分），B 基本掌握（54–71分），C 需要大量帮助（&lt;54分）。</p>
      </div>
      <div class="pt-actions">
        <el-select v-model="classId" placeholder="选择2023级班级" filterable style="width: 220px" @change="loadData">
          <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-button @click="openPaper">预览试卷</el-button>
        <el-button type="primary" :disabled="!rows.length" @click="exportCsv">导出 CSV</el-button>
      </div>
    </header>

    <div class="pt-stat" v-if="overview">
      <span class="pill">班级 {{ overview.className }}</span>
      <span class="pill">已提交 {{ overview.submittedCount }} / {{ overview.totalStudents }}</span>
      <span class="pill pill-a">A {{ levelCount.A }} 人</span>
      <span class="pill pill-b">B {{ levelCount.B }} 人</span>
      <span class="pill pill-c">C {{ levelCount.C }} 人</span>
    </div>

    <p v-if="!overview && !classes.length" class="empty-hint">当前账号下没有 2023 级班级，无法查看四年级前测。</p>

    <el-table :data="rows" stripe height="calc(100vh - 220px)" :empty-text="classes.length ? '请选择班级' : '没有可选的2023级班级'">
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
      <el-table-column label="选择" width="110">
        <template #default="{ row }">
          <span v-if="row.submitted">{{ row.choiceRight }}/10（{{ row.choiceScore }}分）</span>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column prop="opScore" label="操作/40" width="90" />
      <el-table-column prop="totalScore" label="机判总分/90" width="120" />
      <el-table-column label="分类" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.submitted" :type="levelTagType(row.totalScore)" size="small" effect="dark">
            {{ classifyLevel(row.totalScore) }}
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

    <el-dialog v-model="detailVisible" :title="detailTitle" width="640px" append-to-body>
      <div v-if="detail" class="detail-body">
        <p><b>一、填空题</b>（参考答案见右侧）</p>
        <div v-for="(ans, i) in detail.fill || []" :key="'f'+i" class="ans">
          {{ i + 1 }}. 学生答：{{ ans || '（空）' }}
          <span class="hint">参考：{{ FILL_KEY[i] }}</span>
        </div>
        <p><b>二、选择题</b></p>
        <div v-for="(ans, i) in detail.choice || []" :key="'c'+i" class="ans">
          {{ i + 1 }}. 学生选 {{ ans || '（空）' }}
          <span :class="ans === CHOICE_KEY[i] ? 'ok' : 'bad'">
            {{ ans === CHOICE_KEY[i] ? '✓' : '✗ 应 ' + CHOICE_KEY[i] }}
          </span>
        </div>
        <p v-if="detail.op"><b>三、操作题　合计 {{ detail.op.opTotal }}/40</b></p>
        <div v-if="detail.op" class="ans">文件管理 {{ detail.op.op1?.score }}/12　WPS {{ detail.op.op2?.score }}/12　数据侦探 {{ detail.op.op3?.score }}/16</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getClasses, teacherGetPretestG4 } from '../../api'
import { filterClassesByCohort } from '../../utils/classCohort'

const FILL_KEY = ['双击', '退格', '回车', '加粗', '幻灯片', '文件', '数据', '数据', '条形', '编码']
const CHOICE_KEY = ['B', 'B', 'A', 'B', 'A', 'A', 'A', 'A', 'A', 'A']

const router = useRouter()
const loading = ref(false)
const classes = ref([])
const classId = ref(null)
const overview = ref(null)
const detailVisible = ref(false)
const detail = ref(null)
const detailTitle = ref('')

const rows = computed(() => overview.value?.students || [])

/** A 掌握最强 ≥72；B 基本掌握 54–71；C 需要大量帮助 &lt;54（满分 90） */
const classifyLevel = (score) => {
  const n = Number(score)
  if (!Number.isFinite(n)) return ''
  if (n >= 72) return 'A'
  if (n >= 54) return 'B'
  return 'C'
}

const levelTagType = (score) => {
  const lv = classifyLevel(score)
  if (lv === 'A') return 'success'
  if (lv === 'B') return 'warning'
  return 'danger'
}

const levelCount = computed(() => {
  const c = { A: 0, B: 0, C: 0 }
  for (const r of rows.value) {
    if (!r.submitted) continue
    const lv = classifyLevel(r.totalScore)
    if (c[lv] != null) c[lv] += 1
  }
  return c
})

const formatTime = (v) => {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 19)
}

const loadData = async () => {
  if (!classId.value) {
    overview.value = null
    return
  }
  loading.value = true
  try {
    const res = await teacherGetPretestG4(classId.value)
    overview.value = res.data || null
  } finally {
    loading.value = false
  }
}

const openPaper = () => {
  router.push('/teacher/pretest/g4/paper')
}

const openDetail = (row) => {
  detailTitle.value = `${row.realName} · 前测明细`
  detail.value = row.detail || {}
  detailVisible.value = true
}

const exportCsv = () => {
  if (!rows.value.length) return
  const header = '姓名,班级,学号,状态,填空,选择答对,选择分,操作,机判总分,分类,提交时间'
  const lines = rows.value.map((r) => [
    r.realName,
    r.className,
    r.studentNo,
    r.submitted ? '已提交' : '未交',
    r.fillScore ?? '',
    r.choiceRight ?? '',
    r.choiceScore ?? '',
    r.opScore ?? '',
    r.totalScore ?? '',
    r.submitted ? classifyLevel(r.totalScore) : '',
    formatTime(r.submittedAt)
  ].join(','))
  const blob = new Blob(['\uFEFF' + [header, ...lines].join('\n')], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `四年级前测_${overview.value?.className || '班级'}.csv`
  a.click()
  ElMessage.success('已导出')
}

onMounted(async () => {
  const res = await getClasses().catch(() => ({ data: [] }))
  classes.value = filterClassesByCohort(res.data || [], '2023')
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
