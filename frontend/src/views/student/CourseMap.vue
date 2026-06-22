<template>
  <div class="map-page">
    <header class="page-header">
      <h1 class="page-title">我的课程地图</h1>
      <p class="page-sub">选择配套或校本教材，阅读电子课本或进入单元课时学习</p>
    </header>

    <!-- 配套课程：四个年级卡，每卡内上下册并排 -->
    <section class="panel panel-main">
      <h2 class="panel-title">配套课程</h2>
      <el-row :gutter="18" class="grade-row">
        <el-col
          v-for="g in mainGradeGroups"
          :key="g.key"
          :xs="24"
          :sm="12"
          :md="6"
        >
          <div class="grade-card">
            <button
              v-if="showCompletedBadge(g.key)"
              type="button"
              class="completed-badge"
              title="查看成绩"
              @click.stop="openGradeResult(g)"
            >
              ✅已完成
            </button>
            <div class="grade-card-head">{{ g.cardTitle }}</div>
            <div class="dual-covers">
              <div
                v-for="slot in g.slots"
                :key="slot.book.id"
                class="book-slot"
                @click="openBook(slot.book.id)"
              >
                <div class="book-thumb">
                  <img v-if="slot.book.coverUrl" :src="slot.book.coverUrl" :alt="slot.book.name" />
                  <div v-else class="thumb-placeholder">封面</div>
                </div>
                <div class="book-label">{{ slot.book.name }}</div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </section>

    <!-- 校本课程：按系列分组展示 -->
    <section class="panel panel-school" v-if="schoolSeriesGroups.length">
      <h2 class="panel-title">校本课程</h2>
      <el-row :gutter="18" class="grade-row">
        <el-col
          v-for="series in schoolSeriesGroups"
          :key="series.key"
          :xs="24"
          :sm="12"
          :md="6"
        >
          <div class="grade-card grade-card-school">
            <div class="grade-card-head">{{ series.cardTitle }}</div>
            <div class="dual-covers">
              <div
                v-for="book in series.books"
                :key="book.id"
                class="book-slot"
                @click="openBook(book.id)"
              >
                <div class="book-thumb school-thumb">
                  <img v-if="book.coverUrl" :src="book.coverUrl" :alt="book.name" />
                  <div v-else class="thumb-placeholder school-ph">本</div>
                </div>
                <div class="book-label">{{ book.name }}</div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </section>
    <section class="panel panel-school panel-school-empty" v-else>
      <h2 class="panel-title">校本课程</h2>
      <el-empty description="校本课程即将上线" :image-size="80" />
    </section>

    <el-dialog
      v-model="gradeResultVisible"
      :title="gradeResultTitle + ' · 成绩'"
      width="360px"
      align-center
      class="grade-result-dialog"
    >
      <div class="grade-result-body">
        <p><span class="result-label">上册</span><span class="result-value">优秀</span></p>
        <p><span class="result-label">下册</span><span class="result-value">优秀</span></p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTextbooks, getMe } from '../../api'
import { groupSchoolTextbooks } from '../../utils/schoolTextbooks'

const router = useRouter()
const mainBooks = ref([])
const schoolBooks = ref([])
const isCohort2021 = ref(false)
const gradeResultVisible = ref(false)
const gradeResultTitle = ref('')

/** 2021 级学生已完成的三至五年级配套课程 */
const COMPLETED_GRADE_KEYS = ['三年级', '四年级', '五年级']

/** 按「X年级上/下册」聚成四个年级卡，与版式图一致 */
const mainGradeGroups = computed(() => {
  const books = [...mainBooks.value].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
  const order = ['三年级', '四年级', '五年级', '六年级']
  const byGrade = {}
  for (const g of order) {
    byGrade[g] = { key: g, cardTitle: `${g}课程`, up: null, down: null }
  }
  for (const b of books) {
    const m = String(b.name || '').match(/^(.+?)(上|下)册$/)
    if (!m) continue
    const gradePrefix = m[1]
    const vol = m[2]
    if (!byGrade[gradePrefix]) continue
    if (vol === '上') byGrade[gradePrefix].up = b
    if (vol === '下') byGrade[gradePrefix].down = b
  }
  return order
    .filter((g) => byGrade[g].up || byGrade[g].down)
    .map((g) => {
      const row = byGrade[g]
      const slots = []
      if (row.up) slots.push({ book: row.up })
      if (row.down) slots.push({ book: row.down })
      return { key: g, cardTitle: row.cardTitle, slots }
    })
})

const schoolSeriesGroups = computed(() => groupSchoolTextbooks(schoolBooks.value))

const showCompletedBadge = (gradeKey) => isCohort2021.value && COMPLETED_GRADE_KEYS.includes(gradeKey)

const openGradeResult = (group) => {
  gradeResultTitle.value = group.cardTitle
  gradeResultVisible.value = true
}

const detectCohort2021 = (profile) => {
  const className = profile?.className || ''
  const enrollmentYear = profile?.enrollmentYear ?? localStorage.getItem('enrollmentYear')
  const username = profile?.username || localStorage.getItem('username') || ''
  if (String(enrollmentYear) === '2021') return true
  if (className.includes('2021级')) return true
  return /2021/.test(username)
}

onMounted(async () => {
  const [main, school, meRes] = await Promise.all([
    getTextbooks('MAIN'),
    getTextbooks('SCHOOL'),
    getMe().catch(() => ({ data: {} }))
  ])
  mainBooks.value = main.data || []
  schoolBooks.value = school.data || []
  isCohort2021.value = detectCohort2021(meRes.data || {})
})

const openBook = (gradeId) => {
  router.push(`/student/textbook/${gradeId}`)
}
</script>

<style scoped>
/* 页面底色：浅灰蓝，干净不刺眼 */
.map-page {
  max-width: 1120px;
  margin: 0 auto;
  padding: 24px 16px 48px;
  min-height: 100vh;
  background: linear-gradient(180deg, #eef2f7 0%, #e8ecf2 100%);
}

.page-header {
  margin-bottom: 28px;
}
.page-title {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: 0.02em;
}
.page-sub {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

/* 大区块：白底圆角 + 轻边框，替代黄框 */
.panel {
  background: #fafbfc;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 22px 22px 26px;
  margin-bottom: 28px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
}
.panel-main {
  border-top: 3px solid #2563eb;
}
.panel-school {
  border-top: 3px solid #0d9488;
  background: linear-gradient(180deg, #fafcfb 0%, #f4faf8 100%);
  border-color: #d1e7dd;
}

.panel-title {
  margin: 0 0 18px;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}
.panel-main .panel-title {
  color: #1e40af;
}
.panel-school .panel-title {
  color: #0f766e;
}

.grade-row {
  align-items: stretch;
}

/* 年级卡：浅青蓝底 + 白内衬，替代示意图中的浅蓝块 */
.grade-card {
  position: relative;
  background: linear-gradient(165deg, #f0f9ff 0%, #e0f2fe 40%, #f8fafc 100%);
  border: 1px solid #bae6fd;
  border-radius: 14px;
  padding: 14px 12px 16px;
  height: 100%;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.75);
  transition: box-shadow 0.2s, transform 0.2s;
}
.grade-card:hover {
  box-shadow: 0 8px 28px rgba(37, 99, 235, 0.12);
  transform: translateY(-2px);
}
.grade-card-head {
  text-align: center;
  font-size: 16px;
  font-weight: 700;
  color: #0c4a6e;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #94a3b8;
}
.completed-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
  border: 1px solid #86efac;
  background: rgba(255, 255, 255, 0.92);
  color: #15803d;
  font-size: 11px;
  font-weight: 700;
  padding: 4px 8px;
  border-radius: 999px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(22, 163, 74, 0.15);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.completed-badge:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(22, 163, 74, 0.22);
  background: #f0fdf4;
}

.grade-result-body {
  padding: 4px 0 8px;
}
.grade-result-body p {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 0 0 12px;
  padding: 12px 14px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 10px;
  font-size: 15px;
}
.grade-result-body p:last-child {
  margin-bottom: 0;
}
.result-label {
  color: #64748b;
  font-weight: 600;
}
.result-value {
  color: #15803d;
  font-weight: 700;
}

.dual-covers {
  display: flex;
  gap: 10px;
  justify-content: center;
}
.book-slot {
  flex: 1;
  min-width: 0;
  cursor: pointer;
  text-align: center;
  border-radius: 10px;
  padding: 6px 4px 4px;
  transition: background 0.2s;
}
.book-slot:hover {
  background: rgba(255, 255, 255, 0.65);
}

.book-thumb {
  aspect-ratio: 3 / 4;
  max-height: 200px;
  margin: 0 auto 8px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #cbd5e1;
  background: #fff;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
}
.book-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.thumb-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #64748b;
  background: linear-gradient(145deg, #f1f5f9, #e2e8f0);
}
.school-thumb {
  border-color: #99f6e4;
  background: linear-gradient(165deg, #0c4a6e 0%, #164e63 50%, #ecfdf5 100%);
}
.school-thumb img {
  object-fit: contain;
  object-position: center top;
}
.school-ph {
  background: linear-gradient(145deg, #ecfdf5, #d1fae5);
  color: #0f766e;
  font-size: 22px;
  font-weight: 700;
}

.book-label {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  line-height: 1.35;
  word-break: break-all;
}

/* 校本卡：复用年级卡尺寸，仅配色不同 */
.grade-card-school {
  background: linear-gradient(165deg, #ecfdf5 0%, #d1fae5 40%, #f8fafc 100%);
  border-color: #99f6e4;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.75);
}
.grade-card-school:hover {
  box-shadow: 0 8px 28px rgba(13, 148, 136, 0.12);
}
.grade-card-school .grade-card-head {
  color: #115e59;
  border-bottom-color: #5eead4;
}
.panel-school-empty {
  border-top-color: #94a3b8;
  background: #fafbfc;
}
</style>
