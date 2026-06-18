<template>
  <div class="map-page">
    <header class="page-header">
      <h1 class="page-title">课程地图</h1>
      <p class="page-sub">选择配套或校本教材，在线阅读电子课本，或进入建课管理</p>
    </header>

    <section class="panel panel-main">
      <h2 class="panel-title">配套课程</h2>
      <el-row :gutter="18" class="grade-row">
        <el-col v-for="g in mainGradeGroups" :key="g.key" :xs="24" :sm="12" :md="6">
          <div class="grade-card">
            <div class="grade-card-head">{{ g.cardTitle }}</div>
            <div class="dual-covers">
              <div
                v-for="slot in g.slots"
                :key="slot.book.id"
                class="book-slot"
                @click="openGrade(slot.book.id)"
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

    <section class="panel panel-school" v-if="schoolBooks.length">
      <h2 class="panel-title">校本课程</h2>
      <el-row :gutter="18" class="grade-row">
        <el-col :xs="24" :sm="12" :md="6">
          <div class="grade-card grade-card-school">
            <div class="grade-card-head">科创启航</div>
            <div class="dual-covers">
              <div
                v-for="book in schoolBooks"
                :key="book.id"
                class="book-slot"
                @click="openGrade(book.id)"
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { teacherGetTextbooks } from '../../api'

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

const openGrade = (gradeId) => {
  router.push(`/teacher/course-map/grade/${gradeId}`)
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
  max-width: 1120px;
  margin: 0 auto;
  padding: 8px 4px 32px;
}
.page-header { margin-bottom: 24px; }
.page-title {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 700;
  color: #1e293b;
}
.page-sub {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}
.panel {
  background: #fafbfc;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 22px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
}
.panel-main { border-top: 3px solid #2563eb; }
.panel-school {
  border-top: 3px solid #0d9488;
  background: linear-gradient(180deg, #fafcfb 0%, #f4faf8 100%);
  border-color: #d1e7dd;
}
.panel-title {
  margin: 0 0 18px;
  font-size: 18px;
  font-weight: 700;
}
.panel-main .panel-title { color: #1e40af; }
.panel-school .panel-title { color: #0f766e; }
.grade-row { align-items: stretch; }
.grade-card {
  background: linear-gradient(165deg, #f0f9ff 0%, #e0f2fe 40%, #f8fafc 100%);
  border: 1px solid #bae6fd;
  border-radius: 14px;
  padding: 14px 12px 16px;
  height: 100%;
  transition: transform 0.2s, box-shadow 0.2s;
}
.grade-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(37, 99, 235, 0.12);
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
.dual-covers { display: flex; gap: 10px; justify-content: center; }
.book-slot {
  flex: 1;
  min-width: 0;
  cursor: pointer;
  text-align: center;
  border-radius: 10px;
  padding: 6px 4px;
}
.book-slot:hover { background: rgba(255, 255, 255, 0.65); }
.book-thumb {
  aspect-ratio: 3 / 4;
  max-height: 200px;
  margin: 0 auto 8px;
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
.book-label { font-size: 12px; font-weight: 600; color: #334155; line-height: 1.35; }
</style>
