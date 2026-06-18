<template>
  <div class="records-panel" v-loading="loading">
    <header class="panel-head">
      <h2>📚 我的学习记录</h2>
      <el-tag type="info" size="small">共 {{ totalLogCount }} 次提交</el-tag>
    </header>
    <p class="panel-tip">展示本课各活动的提交情况，每次提交都会保留记录；每完成一个活动可获得 2 积分。</p>

    <div class="summary-row">
      <div class="summary-card">
        <span class="s-icon">🏆</span>
        <div>
          <div class="s-val">{{ data.totalPoints || 0 }}</div>
          <div class="s-label">排名积分</div>
        </div>
      </div>
      <div class="summary-card">
        <span class="s-icon">✅</span>
        <div>
          <div class="s-val">{{ submittedCount }}</div>
          <div class="s-label">已完成项</div>
        </div>
      </div>
    </div>

    <div class="tab-row">
      <button type="button" class="tab-btn" :class="{ active: tab === 'works' }" @click="tab = 'works'">我的作品</button>
      <button type="button" class="tab-btn" :class="{ active: tab === 'points' }" @click="tab = 'points'">积分流水</button>
    </div>

    <div v-show="tab === 'works'" class="works-grid">
      <div
        v-for="act in data.activities || []"
        :key="act.key"
        class="work-card"
        :class="{ done: act.submitted }"
      >
        <div class="work-title">{{ activityLabel(act) }}</div>
        <template v-if="act.submitted">
          <div class="work-status ok">已提交 · {{ act.submitCount || 1 }} 次</div>
          <ul v-if="act.logs?.length" class="log-list">
            <li v-for="log in act.logs.slice(0, 3)" :key="log.id">
              {{ formatTime(log.createdAt) }}
              <span v-if="log.score != null"> · {{ log.score }} 分</span>
            </li>
          </ul>
        </template>
        <template v-else>
          <div class="work-status pending">未提交</div>
          <button type="button" class="go-btn" @click="$emit('go-activity', act)">去完成挑战 →</button>
        </template>
      </div>
    </div>

    <div v-show="tab === 'points'" class="points-list">
      <div v-for="p in lessonPoints" :key="p.id" class="point-row">
        <span>{{ p.description }}</span>
        <strong>+{{ p.points }}</strong>
        <span class="time">{{ formatTime(p.createdAt) }}</span>
      </div>
      <el-empty v-if="!lessonPoints.length" description="本课暂无积分记录" :image-size="64" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { getLessonRecords } from '../../api'

const props = defineProps({
  lessonId: { type: Number, required: true }
})

defineEmits(['go-activity'])

const data = ref({})
const loading = ref(false)
const tab = ref('works')

const chineseNum = (n) => ({ 1: '一', 2: '二', 3: '三', 4: '四', 5: '五', 6: '六' }[n] || n)

const activityLabel = (act) => {
  if (act.type === 'QUIZ') return '课堂小测'
  if (act.type === 'EVALUATION') return '学习评价'
  return `活动${chineseNum(act.index)}：${act.title}`
}

const submittedCount = computed(() =>
  (data.value.activities || []).filter((a) => a.submitted).length
)

const totalLogCount = computed(() =>
  (data.value.submissionLogs || []).length
)

const lessonPoints = computed(() => {
  const lessonId = props.lessonId
  return (data.value.pointsHistory || []).filter((p) => {
    if (p.sourceType !== 'ACTIVITY') return false
    const sid = p.sourceId
    return sid && Math.floor(sid / 100) === lessonId
  })
})

const formatTime = (t) => {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

const load = async () => {
  if (!props.lessonId) return
  loading.value = true
  try {
    const res = await getLessonRecords(props.lessonId)
    data.value = res.data || {}
  } finally {
    loading.value = false
  }
}

watch(() => props.lessonId, load)
onMounted(load)

defineExpose({ reload: load })
</script>

<style scoped>
.records-panel {
  padding: 20px 24px;
  max-width: 900px;
}
.panel-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.panel-head h2 {
  margin: 0;
  font-size: 18px;
  color: #1e40af;
}
.panel-tip {
  margin: 0 0 16px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.55;
}
.summary-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
}
.summary-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #e8edf4;
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}
.s-icon { font-size: 28px; }
.s-val { font-size: 22px; font-weight: 800; color: #334155; }
.s-label { font-size: 12px; color: #94a3b8; }
.tab-row {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}
.tab-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 10px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 13px;
  cursor: pointer;
}
.tab-btn.active {
  background: #eff6ff;
  color: #2563eb;
  font-weight: 600;
  box-shadow: inset 0 0 0 1px #bfdbfe;
}
.works-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}
.work-card {
  padding: 16px;
  border-radius: 14px;
  border: 1px dashed #cbd5e1;
  background: #fafbfc;
  min-height: 120px;
}
.work-card.done {
  border-style: solid;
  border-color: #bbf7d0;
  background: #f0fdf4;
}
.work-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 10px;
  line-height: 1.45;
}
.work-status {
  font-size: 13px;
  margin-bottom: 8px;
}
.work-status.ok { color: #16a34a; font-weight: 600; }
.work-status.pending { color: #94a3b8; }
.log-list {
  margin: 0;
  padding-left: 16px;
  font-size: 11px;
  color: #64748b;
}
.go-btn {
  border: none;
  background: none;
  color: #2563eb;
  font-size: 13px;
  cursor: pointer;
  padding: 0;
}
.points-list { display: flex; flex-direction: column; gap: 8px; }
.point-row {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #f1f5f9;
  font-size: 13px;
}
.point-row strong { color: #5b4fc7; }
.time { color: #94a3b8; font-size: 12px; }
</style>
