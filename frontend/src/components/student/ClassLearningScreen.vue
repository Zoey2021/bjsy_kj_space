<template>
  <Teleport to="body">
    <div v-if="visible" class="screen-mask" @click.self="close">
      <div class="screen-board">
        <header class="screen-head">
          <div>
            <p class="eyebrow">班级学习大屏</p>
            <h2>{{ data.className || '我的班级' }}</h2>
            <p class="sub">{{ data.lessonTitle || '本课' }} · 共 {{ data.totalStudents || 0 }} 人 · 实时刷新</p>
          </div>
          <div class="head-right">
            <div class="me-chip" v-if="data.myRank">
              我的排名 <b>第 {{ data.myRank }} 名</b>
              <span>{{ data.myPoints || 0 }} 分</span>
            </div>
            <button type="button" class="close-btn" @click="close">关闭</button>
          </div>
        </header>

        <section class="rank-wrap" v-loading="loading">
          <h3>🏆 班级积分排行榜</h3>
          <div class="podium" v-if="top3.length">
            <div v-for="row in podiumOrder" :key="'p' + row.rank" class="podium-col" :class="'place-' + row.rank">
              <div class="avatar">{{ medal(row.rank) }}</div>
              <div class="pname">{{ row.realName }}</div>
              <div class="pscore">{{ row.points }} 分</div>
              <div class="pstep">{{ row.rank }}</div>
            </div>
          </div>
          <ol class="rank-list">
            <li
              v-for="row in data.ranking || []"
              :key="row.studentId"
              :class="{ me: row.isMe, top: row.rank <= 3 }"
            >
              <span class="r-no">{{ row.rank }}</span>
              <span class="r-name">{{ row.realName }}</span>
              <span class="r-pts">{{ row.points }} 分</span>
            </li>
          </ol>
        </section>

        <section class="act-wrap">
          <h3>📌 本课环节完成人数</h3>
          <div class="act-grid">
            <div v-for="act in inquiryActs" :key="act.index" class="act-card">
              <div class="act-title">{{ actLabel(act) }}</div>
              <div class="act-num">
                <b>{{ act.submittedCount || 0 }}</b>
                <span>/ {{ act.totalStudents || 0 }} 人</span>
              </div>
              <div class="act-bar">
                <i :style="{ width: barWidth(act) }"></i>
              </div>
            </div>
          </div>
          <p v-if="!inquiryActs.length" class="empty">本课暂无探究环节数据</p>
        </section>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, onUnmounted, ref, watch } from 'vue'
import { getClassScreen } from '../../api'

const props = defineProps({
  visible: { type: Boolean, default: false },
  lessonId: { type: [Number, String], default: null }
})
const emit = defineEmits(['update:visible'])

const loading = ref(false)
const data = ref({})
let timer = null

const inquiryActs = computed(() =>
  (data.value.activities || []).filter((a) => a.type !== 'QUIZ' && a.type !== 'EVALUATION')
)

const top3 = computed(() => (data.value.ranking || []).slice(0, 3))
const podiumOrder = computed(() => {
  const [a, b, c] = top3.value
  return [b, a, c].filter(Boolean)
})

const medal = (rank) => ({ 1: '🥇', 2: '🥈', 3: '🥉' }[rank] || '⭐')

const actLabel = (act) => {
  const n = { 1: '一', 2: '二', 3: '三', 4: '四', 5: '五', 6: '六' }[act.index]
  if (n) return `探究${n}：${act.title}`
  return act.title
}

const barWidth = (act) => {
  const total = Number(act.totalStudents || 0)
  const done = Number(act.submittedCount || 0)
  if (!total) return '0%'
  return Math.min(100, Math.round(done * 100 / total)) + '%'
}

const close = () => emit('update:visible', false)

const load = async () => {
  if (!props.lessonId) return
  loading.value = !data.value.ranking
  try {
    const res = await getClassScreen(props.lessonId)
    data.value = res.data || {}
  } finally {
    loading.value = false
  }
}

watch(() => props.visible, (v) => {
  if (v) {
    load()
    timer = setInterval(load, 4000)
  } else if (timer) {
    clearInterval(timer)
    timer = null
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.screen-mask {
  position: fixed;
  inset: 0;
  z-index: 4000;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px;
}
.screen-board {
  width: min(1080px, 100%);
  max-height: 92vh;
  overflow: auto;
  border-radius: 24px;
  background: linear-gradient(180deg, #1e3a8a 0%, #312e81 42%, #fff7ed 42%, #fff 100%);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.28);
  padding: 22px 24px 20px;
}
.screen-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  color: #fff;
  margin-bottom: 16px;
}
.eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 2px;
  opacity: 0.85;
}
.screen-head h2 {
  margin: 4px 0 2px;
  font-size: 28px;
}
.sub { margin: 0; font-size: 13px; opacity: 0.88; }
.head-right { display: flex; align-items: flex-start; gap: 10px; }
.me-chip {
  background: rgba(255,255,255,0.16);
  border: 1px solid rgba(255,255,255,0.28);
  border-radius: 14px;
  padding: 8px 12px;
  font-size: 13px;
}
.me-chip b { margin: 0 6px; }
.close-btn {
  border: 0;
  background: #fff;
  color: #1e3a8a;
  border-radius: 999px;
  padding: 8px 14px;
  font-weight: 700;
  cursor: pointer;
}
.rank-wrap, .act-wrap {
  background: #fff;
  border-radius: 18px;
  padding: 16px;
  margin-bottom: 12px;
}
.rank-wrap h3, .act-wrap h3 {
  margin: 0 0 12px;
  font-size: 16px;
  color: #1e293b;
}
.podium {
  display: grid;
  grid-template-columns: 1fr 1.15fr 1fr;
  gap: 10px;
  align-items: end;
  margin-bottom: 14px;
}
.podium-col {
  text-align: center;
  background: #fff7ed;
  border-radius: 16px 16px 8px 8px;
  padding: 10px 8px 0;
}
.place-1 { background: linear-gradient(180deg, #fef3c7, #fde68a); min-height: 150px; }
.place-2 { background: linear-gradient(180deg, #e2e8f0, #cbd5e1); min-height: 124px; }
.place-3 { background: linear-gradient(180deg, #ffedd5, #fdba74); min-height: 108px; }
.avatar { font-size: 28px; }
.pname { font-weight: 700; color: #9a3412; }
.pscore { font-size: 13px; color: #c2410c; }
.pstep {
  margin-top: 8px;
  background: #ea580c;
  color: #fff;
  font-weight: 800;
  border-radius: 8px 8px 0 0;
}
.rank-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 220px;
  overflow: auto;
}
.rank-list li {
  display: grid;
  grid-template-columns: 42px 1fr 80px;
  align-items: center;
  padding: 8px 10px;
  border-radius: 10px;
}
.rank-list li.top { background: #fffbeb; }
.rank-list li.me {
  outline: 2px solid #f59e0b;
  background: #fef3c7;
}
.r-no { font-weight: 800; color: #ea580c; }
.r-name { font-weight: 600; }
.r-pts { text-align: right; color: #c2410c; font-weight: 700; }
.act-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 10px;
}
.act-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 12px;
}
.act-title { font-size: 13px; font-weight: 700; color: #334155; min-height: 36px; }
.act-num { margin: 6px 0; color: #64748b; }
.act-num b { font-size: 22px; color: #4f46e5; margin-right: 4px; }
.act-bar {
  height: 8px;
  background: #e2e8f0;
  border-radius: 999px;
  overflow: hidden;
}
.act-bar i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #6366f1, #f59e0b);
}
.empty { color: #94a3b8; font-size: 13px; }
</style>
