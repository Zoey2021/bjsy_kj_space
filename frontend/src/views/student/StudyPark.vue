<template>
  <div class="park-page" v-loading="loading">
    <header class="park-hero">
      <div class="hero-text">
        <h1>游学乐园</h1>
        <p>做完当节课的全部探究活动、课堂小测和学习评价后，才能申请开启。开启后每分钟消耗 2 积分。</p>
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
          <el-button v-else-if="status.accessStatus === 'PENDING'" disabled>
            等待教师授权
          </el-button>
          <el-button v-else-if="!status.unlocked && status.currentLessonId && !status.taskCompleted" @click="goLesson">
            继续完成任务
          </el-button>
          <el-button text @click="refresh">刷新状态</el-button>
        </div>
      </div>
    </header>

    <section class="games-section">
      <h2>益智游戏</h2>
      <p class="games-note">每分钟 2 积分，不足 1 分钟也按 1 分钟算。你现在有 {{ status.redeemablePoints || 0 }} 分，可以玩 {{ status.parkMinutes || 0 }} 分钟。</p>
      <div class="game-grid">
        <article
          v-for="game in visibleGames"
          :key="game.id"
          class="game-card"
          :class="{ locked: !status.unlocked }"
          @click="openGame(game)"
        >
          <div class="game-icon" :style="{ background: game.bg }">
            <span v-if="game.mark" class="mark" v-html="game.mark"></span>
            <template v-else>{{ game.icon }}</template>
          </div>
          <div class="game-body">
            <h3>{{ game.title }}</h3>
            <p>{{ game.desc }}</p>
          </div>
          <span v-if="!status.unlocked" class="lock-badge">未解锁</span>
          <span v-else class="play-badge">开始玩</span>
        </article>
      </div>
      <p v-if="!visibleGames.length" class="games-note">教师暂时关闭了全部游戏。</p>
    </section>

    <el-dialog
      v-model="gameVisible"
      :title="activeGame?.title || '游戏'"
      width="760px"
      destroy-on-close
      class="game-dialog"
      @closed="onGameClosed"
    >
      <SnakeGame v-if="activeGame?.id === 'snake' && gameVisible" />
      <TypingGame v-else-if="activeGame?.id === 'typing' && gameVisible" />
      <MemoryGame v-else-if="activeGame?.id === 'memory' && gameVisible" :pairs="memoryPairs" />
      <Game2048 v-else-if="activeGame?.id === '2048' && gameVisible" />
      <FruitGame v-else-if="activeGame?.id === 'fruit' && gameVisible" :lives="fruitLives" :speed="fruitSpeed" />
      <RopeGame v-else-if="activeGame?.id === 'rope' && gameVisible" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { applyParkAccess, getParkStatus, recordParkPlay } from '../../api'
import SnakeGame from '../../components/student/games/SnakeGame.vue'
import TypingGame from '../../components/student/games/TypingGame.vue'
import MemoryGame from '../../components/student/games/MemoryGame.vue'
import Game2048 from '../../components/student/games/Game2048.vue'
import FruitGame from '../../components/student/games/FruitGame.vue'
import RopeGame from '../../components/student/games/RopeGame.vue'
import { MEMORY_ICONS } from '../../components/student/games/parkPictures'

const picture = (id) => MEMORY_ICONS.find((icon) => icon.id === id)?.svg || ''

const router = useRouter()
const loading = ref(false)
const applying = ref(false)
const status = ref({})
const gameVisible = ref(false)
const activeGame = ref(null)

const visibleGames = computed(() => {
  const enabled = status.value.parkConfig?.games
  if (!enabled) return games
  return games.filter((game) => enabled[game.id] !== false)
})
const fruitLives = computed(() => Number(status.value.parkConfig?.fruitLives) || 3)
const fruitSpeed = computed(() => status.value.parkConfig?.fruitSpeed || 'normal')
const memoryPairs = computed(() => Number(status.value.parkConfig?.memoryPairs) || 8)

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
    desc: '翻开卡片，找出相同图案',
    icon: '🧠',
    mark: picture('star'),
    bg: 'linear-gradient(135deg, #fbbf24, #d97706)'
  },
  {
    id: '2048',
    title: '2048',
    desc: '滑动合并相同数字，凑到 2048',
    icon: '🔢',
    bg: 'linear-gradient(135deg, #fb7185, #e11d48)'
  },
  {
    id: 'fruit',
    title: '接水果',
    desc: '移动鼠标接住掉下来的水果',
    icon: '🍎',
    mark: picture('apple'),
    bg: 'linear-gradient(135deg, #4ade80, #16a34a)'
  },
  {
    id: 'rope',
    title: '切绳接球',
    desc: '看准时机点断绳子，让小球落入篮子',
    icon: '🪢',
    bg: 'linear-gradient(135deg, #c084fc, #7c3aed)'
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

const refresh = async (quiet = false) => {
  if (!quiet) loading.value = true
  try {
    const res = await getParkStatus()
    status.value = res.data || {}
  } catch {
    if (!quiet) ElMessage.error('获取乐园状态失败')
    if (!status.value.message) status.value = { ...status.value, message: '暂时没有拿到乐园状态，请点刷新再试' }
  } finally {
    if (!quiet) loading.value = false
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

let playStartedAt = 0
let playSaved = false
let limitTimer = null

const clearLimit = () => {
  if (limitTimer) {
    clearTimeout(limitTimer)
    limitTimer = null
  }
}

const flushPlay = async () => {
  if (playSaved || !playStartedAt || !activeGame.value) return
  if (status.value.preview) {
    playSaved = true
    clearLimit()
    playStartedAt = 0
    return
  }
  playSaved = true
  clearLimit()
  const seconds = Math.round((Date.now() - playStartedAt) / 1000)
  const gameId = activeGame.value.id
  playStartedAt = 0
  if (seconds < 1) return
  try {
    const res = await recordParkPlay({ gameId, durationSeconds: seconds })
    const bill = res.data || {}
    if (bill.saved && bill.charged) {
      status.value.redeemablePoints = bill.redeemablePoints
      status.value.parkMinutes = bill.parkMinutes
      ElMessage.success(`这次按 ${bill.minutes} 分钟计算，用了 ${bill.charged} 积分，还剩 ${bill.redeemablePoints} 分`)
    }
  } catch {
    // 请求拦截器已经提示失败原因
  }
}

const openGame = (game) => {
  if (!status.value.unlocked) {
    ElMessage.warning(status.value.message || '请先完成任务并获得教师授权')
    return
  }
  const points = Number(status.value.redeemablePoints || 0)
  const minutes = Math.floor(points / 2)
  if (minutes < 1) {
    ElMessage.warning(`还差 ${2 - Math.max(points, 0)} 积分。游学乐园每分钟需要 2 积分。`)
    return
  }
  activeGame.value = game
  playStartedAt = Date.now()
  playSaved = false
  gameVisible.value = true
  clearLimit()
  limitTimer = setTimeout(() => {
    ElMessage.info('积分只够玩到这里啦，先去完成课堂任务再来吧。')
    gameVisible.value = false
  }, minutes * 60 * 1000)
}

const onGameClosed = () => {
  flushPlay()
  activeGame.value = null
}

let pollTimer = null
let toldParkClosed = 0
const tellParkClosed = () => {
  const now = Date.now()
  if (now - toldParkClosed < 4000) return
  toldParkClosed = now
  ElMessage.info('游学乐园已关闭，可以继续做其他内容')
}
const shutPark = async () => {
  const playing = gameVisible.value
  await refresh(true)
  if (playing && !status.value.unlocked) {
    gameVisible.value = false
    tellParkClosed()
  }
}
const onParkClosed = () => shutPark()
onMounted(() => {
  refresh()
  window.addEventListener('learn-park-closed', onParkClosed)
  pollTimer = setInterval(async () => {
    const wasOpen = !!status.value.unlocked || gameVisible.value
    if (!wasOpen && status.value.accessStatus !== 'PENDING') return
    await refresh(true)
    if (wasOpen && !status.value.unlocked && gameVisible.value) {
      gameVisible.value = false
      tellParkClosed()
    }
  }, 8000)
})
onUnmounted(() => {
  window.removeEventListener('learn-park-closed', onParkClosed)
  flushPlay()
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
.games-note { margin: -4px 0 12px; color: var(--park-muted); font-size: 13px; }
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
.game-icon :deep(svg) { width: 36px; height: 36px; }
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
