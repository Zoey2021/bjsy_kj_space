<template>
  <div class="park-page" v-loading="loading">
    <header class="park-hero">
      <div class="hero-text">
        <h1>游学乐园</h1>
        <p>完成当前课程任务后申请，经教师授权即可畅玩益智小游戏</p>
      </div>
      <div class="status-card" :class="statusTone">
        <div class="status-label">当前状态</div>
        <div class="status-value">{{ statusLabel }}</div>
        <p class="status-msg">{{ status.message || '加载中…' }}</p>
        <div v-if="status.currentLessonTitle" class="lesson-chip">
          当前课时：{{ status.currentLessonTitle }}
        </div>
        <div class="status-actions">
          <el-button v-if="status.canApply" type="primary" :loading="applying" @click="doApply">
            申请开启乐园
          </el-button>
          <el-button v-else-if="!status.unlocked && status.currentLessonId" @click="goLesson">
            继续完成任务
          </el-button>
          <el-button text @click="refresh">刷新状态</el-button>
        </div>
      </div>
    </header>

    <section class="games-section">
      <h2>娱乐活动</h2>
      <div class="game-grid">
        <article
          v-for="game in games"
          :key="game.id"
          class="game-card"
          :class="{ locked: !status.unlocked }"
          @click="openGame(game)"
        >
          <div class="game-icon" :style="{ background: game.bg }">{{ game.icon }}</div>
          <div class="game-body">
            <h3>{{ game.title }}</h3>
            <p>{{ game.desc }}</p>
          </div>
          <span v-if="!status.unlocked" class="lock-badge">未解锁</span>
          <span v-else class="play-badge">开始玩</span>
        </article>
      </div>
    </section>

    <el-dialog
      v-model="gameVisible"
      :title="activeGame?.title || '游戏'"
      width="720px"
      destroy-on-close
      class="game-dialog"
      @closed="onGameClosed"
    >
      <SnakeGame v-if="activeGame?.id === 'snake' && gameVisible" />
      <TypingGame v-else-if="activeGame?.id === 'typing' && gameVisible" />
      <MemoryGame v-else-if="activeGame?.id === 'memory' && gameVisible" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { applyParkAccess, getParkStatus } from '../../api'
import SnakeGame from '../../components/student/games/SnakeGame.vue'
import TypingGame from '../../components/student/games/TypingGame.vue'
import MemoryGame from '../../components/student/games/MemoryGame.vue'

const router = useRouter()
const loading = ref(false)
const applying = ref(false)
const status = ref({})
const gameVisible = ref(false)
const activeGame = ref(null)

const games = [
  {
    id: 'snake',
    title: '贪吃蛇',
    desc: '经典方向键操控，吃到果实不断变长',
    icon: '🐍',
    bg: 'linear-gradient(135deg, #34d399, #059669)'
  },
  {
    id: 'typing',
    title: '打字小能手',
    desc: '看词打字，练速度与正确率',
    icon: '⌨️',
    bg: 'linear-gradient(135deg, #60a5fa, #2563eb)'
  },
  {
    id: 'memory',
    title: '记忆翻翻乐',
    desc: '翻开卡片配对相同图案',
    icon: '🧠',
    bg: 'linear-gradient(135deg, #fbbf24, #d97706)'
  }
]

const statusLabel = computed(() => {
  if (status.value.unlocked) return '已解锁'
  const s = status.value.accessStatus
  if (s === 'PENDING') return '待教师授权'
  if (s === 'REJECTED') return '申请未通过'
  if (s === 'REVOKED') return '已关闭'
  if (status.value.taskCompleted) return '可申请'
  return '未解锁'
})

const statusTone = computed(() => {
  if (status.value.unlocked) return 'tone-ok'
  if (status.value.accessStatus === 'PENDING') return 'tone-wait'
  if (status.value.accessStatus === 'REJECTED' || status.value.accessStatus === 'REVOKED') return 'tone-warn'
  return 'tone-lock'
})

const refresh = async () => {
  loading.value = true
  try {
    const res = await getParkStatus()
    status.value = res.data || {}
  } catch {
    ElMessage.error('获取乐园状态失败')
  } finally {
    loading.value = false
  }
}

const doApply = async () => {
  applying.value = true
  try {
    const res = await applyParkAccess()
    status.value = res.data || {}
    ElMessage.success('申请已提交，请等待教师授权')
  } catch (e) {
    ElMessage.error(e?.message || '申请失败')
  } finally {
    applying.value = false
  }
}

const goLesson = () => {
  const id = status.value.currentLessonId
  if (id) router.push(`/student/lesson/${id}`)
  else router.push('/student/map')
}

const openGame = (game) => {
  if (!status.value.unlocked) {
    ElMessage.warning(status.value.message || '请先完成任务并获得教师授权')
    return
  }
  activeGame.value = game
  gameVisible.value = true
}

const onGameClosed = () => {
  activeGame.value = null
}

let pollTimer = null
onMounted(() => {
  refresh()
  // 等待教师授权时轮询状态
  pollTimer = setInterval(() => {
    if (!status.value.unlocked && status.value.accessStatus === 'PENDING') {
      refresh()
    }
  }, 8000)
})
onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.park-page {
  max-width: 1080px;
  margin: 0 auto;
  --park-ink: #1e293b;
  --park-muted: #64748b;
  --park-line: #e2e8f0;
}
.park-hero {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 20px;
  align-items: stretch;
  margin-bottom: 28px;
}
.hero-text h1 {
  margin: 0 0 8px;
  font-size: 32px;
  color: var(--park-ink);
  letter-spacing: 0.02em;
}
.hero-text p {
  margin: 0;
  color: var(--park-muted);
  line-height: 1.6;
  font-size: 15px;
}
.status-card {
  border-radius: 16px;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid var(--park-line);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}
.status-card.tone-ok { border-color: #86efac; background: linear-gradient(180deg, #f0fdf4, #fff); }
.status-card.tone-wait { border-color: #fcd34d; background: linear-gradient(180deg, #fffbeb, #fff); }
.status-card.tone-warn { border-color: #fca5a5; background: linear-gradient(180deg, #fef2f2, #fff); }
.status-card.tone-lock { border-color: #cbd5e1; }
.status-label {
  font-size: 12px;
  color: var(--park-muted);
  margin-bottom: 4px;
}
.status-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--park-ink);
}
.status-msg {
  margin: 8px 0 12px;
  color: #475569;
  font-size: 14px;
  line-height: 1.5;
  min-height: 42px;
}
.lesson-chip {
  display: inline-block;
  margin-bottom: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: #eef2ff;
  color: #4338ca;
  font-size: 12px;
}
.status-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.games-section h2 {
  margin: 0 0 14px;
  font-size: 20px;
  color: var(--park-ink);
}
.game-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.game-card {
  position: relative;
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 18px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid var(--park-line);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
.game-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
}
.game-card.locked {
  opacity: 0.72;
}
.game-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-size: 28px;
  flex-shrink: 0;
  color: #fff;
}
.game-body h3 {
  margin: 0 0 4px;
  font-size: 17px;
  color: var(--park-ink);
}
.game-body p {
  margin: 0;
  font-size: 13px;
  color: var(--park-muted);
  line-height: 1.45;
}
.lock-badge,
.play-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
}
.lock-badge {
  background: #f1f5f9;
  color: #64748b;
}
.play-badge {
  background: #dbeafe;
  color: #1d4ed8;
}
@media (max-width: 860px) {
  .park-hero,
  .game-grid {
    grid-template-columns: 1fr;
  }
}
</style>
