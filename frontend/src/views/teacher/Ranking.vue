<template>
  <div class="ranking-page">
    <div class="rank-toolbar" v-if="!embedded">
      <h2>班级积分排行</h2>
    </div>
    <el-select v-model="classId" placeholder="选择班级" filterable style="width:220px;margin-bottom:16px" @change="loadRanking">
      <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
    </el-select>

    <div class="board" v-loading="loading">
      <div class="podium" v-if="top3.length">
        <div v-for="row in podiumOrder" :key="'p' + row.rank" class="podium-col" :class="'place-' + row.rank">
          <div class="medal">{{ medal(row.rank) }}</div>
          <div class="name">{{ row.realName }}</div>
          <div class="score">{{ row.points }} 分</div>
          <div class="step">NO.{{ row.rank }}</div>
        </div>
      </div>
      <el-empty v-else-if="!loading" description="该班还没有积分记录" :image-size="72" />

      <ol class="rank-list">
        <li v-for="row in ranking" :key="row.studentId" :class="{ top: row.rank <= 3 }">
          <span class="r-no">{{ row.rank }}</span>
          <span class="r-name">{{ row.realName }}</span>
          <span class="r-bar"><i :style="{ width: barWidth(row.points) }"></i></span>
          <span class="r-pts">{{ row.points }}</span>
        </li>
      </ol>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getClasses, getRanking } from '../../api'

defineProps({ embedded: { type: Boolean, default: false } })

const classes = ref([])
const classId = ref(null)
const ranking = ref([])
const loading = ref(false)

const top3 = computed(() => ranking.value.slice(0, 3))
const podiumOrder = computed(() => {
  const [a, b, c] = top3.value
  return [b, a, c].filter(Boolean)
})
const maxPoints = computed(() => Math.max(1, ...ranking.value.map((r) => Number(r.points || 0))))

const medal = (rank) => ({ 1: '🥇', 2: '🥈', 3: '🥉' }[rank] || '')
const barWidth = (pts) => Math.round(Number(pts || 0) * 100 / maxPoints.value) + '%'

const loadRanking = async () => {
  if (!classId.value) {
    ranking.value = []
    return
  }
  loading.value = true
  try {
    const res = await getRanking(classId.value)
    ranking.value = res.data || []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const res = await getClasses().catch(() => ({ data: [] }))
  classes.value = res.data || []
  if (classes.value[0]) {
    classId.value = classes.value[0].id
    await loadRanking()
  }
})
</script>

<style scoped>
.ranking-page { max-width: 860px; margin: 0 auto; }
.rank-toolbar h2 { margin: 0 0 12px; }
.board {
  background: linear-gradient(180deg, #eef2ff 0%, #fff 120px);
  border: 1px solid #e0e7ff;
  border-radius: 20px;
  padding: 18px;
}
.podium {
  display: grid;
  grid-template-columns: 1fr 1.2fr 1fr;
  gap: 12px;
  align-items: end;
  margin-bottom: 18px;
}
.podium-col {
  text-align: center;
  border-radius: 18px 18px 10px 10px;
  padding: 12px 8px 0;
  box-shadow: 0 8px 18px rgba(79, 70, 229, 0.08);
}
.place-1 { background: linear-gradient(180deg, #fef3c7, #fde68a); min-height: 168px; }
.place-2 { background: linear-gradient(180deg, #e2e8f0, #cbd5e1); min-height: 140px; }
.place-3 { background: linear-gradient(180deg, #ffedd5, #fdba74); min-height: 122px; }
.medal { font-size: 32px; }
.name { font-weight: 800; color: #1e293b; }
.score { color: #c2410c; font-weight: 700; margin: 4px 0 8px; }
.step {
  background: #4f46e5;
  color: #fff;
  font-weight: 800;
  border-radius: 10px 10px 0 0;
  padding: 6px 0;
}
.place-1 .step { background: #d97706; }
.rank-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.rank-list li {
  display: grid;
  grid-template-columns: 48px 110px 1fr 56px;
  gap: 10px;
  align-items: center;
  padding: 9px 10px;
  border-radius: 12px;
}
.rank-list li.top { background: #fffbeb; }
.rank-list li:nth-child(even):not(.top) { background: #f8fafc; }
.r-no { font-weight: 800; color: #6366f1; }
.r-name { font-weight: 600; }
.r-bar {
  height: 8px;
  background: #e2e8f0;
  border-radius: 999px;
  overflow: hidden;
}
.r-bar i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #6366f1, #f59e0b);
}
.r-pts { text-align: right; font-weight: 800; color: #ea580c; }
</style>
