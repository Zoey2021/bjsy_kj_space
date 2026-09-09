<template>
  <div class="map-page">
    <header class="page-header">
      <h1 class="page-title">课程地图</h1>
      <p class="page-sub">选择配套或校本教材，查看目录或在线阅读电子课本</p>
    </header>

    <div class="map-body">
    <section class="panel panel-main">
      <h2 class="panel-title">配套课程</h2>
      <el-row :gutter="12" class="grade-row">
        <el-col v-for="g in mainGradeGroups" :key="g.key" :span="6">
          <div class="grade-card">
            <div class="grade-card-head">
              <button
                v-if="g.key === '四年级' || g.key === '六年级'"
                type="button"
                class="pretest-btn"
                @click.stop="openPretest(g.key)"
              >前测</button>
              <span>{{ g.cardTitle }}</span>
            </div>
            <div class="dual-covers">
              <div
                v-for="slot in g.slots"
                :key="slot.book.id"
                class="book-slot"
                @click="openGrade(slot.book.id)"
              >
                <div class="book-thumb">
                  <img v-if="slot.book.coverUrl" :src="slot.book.coverUrl" :alt="slot.book.name" loading="lazy" decoding="async" />
                  <div v-else class="thumb-placeholder">封面</div>
                </div>
                <div class="book-label">{{ slot.book.name }}</div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </section>

    <section class="panel panel-school">
      <h2 class="panel-title">校本课程</h2>
      <el-row :gutter="12" class="grade-row">
        <el-col
          v-for="series in schoolSeriesGroups"
          :key="series.key"
          :span="6"
        >
          <div class="grade-card grade-card-school">
            <div class="grade-card-head">{{ series.cardTitle }}</div>
            <div class="dual-covers">
              <div
                v-for="book in series.books"
                :key="book.id"
                class="book-slot"
                @click="openGrade(book.id)"
              >
                <div class="book-thumb school-thumb">
                  <img v-if="book.coverUrl" :src="book.coverUrl" :alt="book.name" loading="lazy" decoding="async" />
                  <div v-else class="thumb-placeholder school-ph">本</div>
                </div>
                <div class="book-label">{{ book.name }}</div>
              </div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="grade-card grade-card-school">
            <div class="grade-card-head">{{ integratedInquiry.cardTitle }}</div>
            <div class="dual-covers">
              <div
                v-for="slot in integratedInquiry.slots"
                :key="slot.key"
                class="book-slot"
                @click="openInquirySlot(slot)"
              >
                <div class="book-thumb school-thumb">
                  <img :src="slot.coverUrl" :alt="slot.label" loading="lazy" decoding="async" />
                </div>
                <div class="book-label">{{ slot.label }}</div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { teacherGetTextbooks } from '../../api'
import { groupSchoolTextbooks } from '../../utils/schoolTextbooks'
import { INTEGRATED_INQUIRY_COURSE } from '../../constants/pblModule'

const router = useRouter()
const mainBooks = ref([])
const schoolBooks = ref([])

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
const integratedInquiry = INTEGRATED_INQUIRY_COURSE

const openGrade = (gradeId) => {
  router.push(`/teacher/course-map/grade/${gradeId}`)
}

const openPretest = (gradeKey) => {
  router.push(gradeKey === '六年级' ? '/teacher/pretest/g6' : '/teacher/pretest/g4')
}

const openInquirySlot = (slot) => {
  router.push(slot.teacherRoute)
}

onMounted(async () => {
  const [main, school] = await Promise.all([
    teacherGetTextbooks('MAIN'),
    teacherGetTextbooks('SCHOOL')
  ])
  mainBooks.value = main.data || []
  schoolBooks.value = school.data || []
})
</script>

<style scoped>
.map-page {
  height: 100%;
  max-height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0;
  width: 100%;
  box-sizing: border-box;
}
.page-header {
  flex-shrink: 0;
  margin-bottom: 8px;
}
.page-title {
  margin: 0 0 2px;
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}
.page-sub {
  margin: 0;
  font-size: 12px;
  color: #64748b;
}
.map-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fafbfc;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 10px 12px 12px;
  margin-bottom: 0;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
  overflow: hidden;
}
.panel-main { border-top: 3px solid #2563eb; }
.panel-school {
  border-top: 3px solid #0d9488;
  background: linear-gradient(180deg, #fafcfb 0%, #f4faf8 100%);
  border-color: #d1e7dd;
}
.panel-title {
  flex-shrink: 0;
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 700;
}
.panel-main .panel-title { color: #1e40af; }
.panel-school .panel-title { color: #0f766e; }
.grade-row {
  flex: 1;
  min-height: 0;
  height: 100%;
  align-items: stretch;
  flex-wrap: nowrap !important;
}
.grade-row :deep(.el-col) {
  display: flex;
  height: 100%;
  min-height: 0;
  max-height: 100%;
}
.grade-card {
  width: 100%;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: linear-gradient(165deg, #f0f9ff 0%, #e0f2fe 40%, #f8fafc 100%);
  border: 1px solid #bae6fd;
  border-radius: 12px;
  padding: 8px 8px 10px;
  overflow: hidden;
  transition: box-shadow 0.2s;
}
.grade-card:hover {
  box-shadow: 0 6px 20px rgba(37, 99, 235, 0.12);
}
.grade-card-head {
  position: relative;
  flex-shrink: 0;
  text-align: center;
  font-size: 14px;
  font-weight: 700;
  color: #0c4a6e;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px dashed #94a3b8;
}
.pretest-btn {
  position: absolute;
  left: 4px;
  top: 50%;
  transform: translateY(-70%);
  border: none;
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.28);
}
.pretest-btn:hover {
  background: linear-gradient(135deg, #1d4ed8, #1e40af);
}
.dual-covers {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 8px;
  justify-content: center;
}
.book-slot {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  text-align: center;
  border-radius: 10px;
  padding: 2px;
}
.book-slot:hover { background: rgba(255, 255, 255, 0.65); }
.book-thumb {
  flex: 1;
  min-height: 0;
  width: 100%;
  margin: 0 auto 4px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #cbd5e1;
  background: #fff;
}
.book-thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.thumb-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #64748b;
  background: #f1f5f9;
}
.grade-card-school {
  background: linear-gradient(165deg, #ecfdf5 0%, #d1fae5 40%, #f8fafc 100%);
  border-color: #99f6e4;
}
.grade-card-school .grade-card-head { color: #115e59; border-bottom-color: #5eead4; }
.school-thumb img { object-fit: contain; object-position: center top; }
.school-ph { font-size: 22px; font-weight: 700; color: #0f766e; }
.book-label {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 600;
  color: #334155;
  line-height: 1.3;
}
</style>
