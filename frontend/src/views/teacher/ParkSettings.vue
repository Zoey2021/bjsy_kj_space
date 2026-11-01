<template>
  <div class="park-settings" v-loading="loading">
    <header class="park-header">
      <div class="header-main">
        <h1><span class="title-icon">🎮</span> 乐园游戏</h1>
        <p>这里的参数对全校学生生效。关闭的游戏不会出现在游学乐园里。</p>
      </div>
    </header>

    <div class="park-body">
      <section class="card">
        <h3>开放的游戏</h3>
        <div class="game-switches">
          <label v-for="game in gameList" :key="game.id" class="switch-row">
            <span>{{ game.title }}</span>
            <el-switch v-model="form.games[game.id]" />
          </label>
        </div>
      </section>

      <section class="card">
        <h3>接水果</h3>
        <p class="hint">水果用图案画出，不依赖电脑自带的表情符号。</p>
        <div class="field">
          <span>漏接几次结束</span>
          <el-input-number v-model="form.fruitLives" :min="1" :max="8" />
        </div>
        <div class="field">
          <span>下落速度</span>
          <el-radio-group v-model="form.fruitSpeed">
            <el-radio-button label="slow">慢</el-radio-button>
            <el-radio-button label="normal">正常</el-radio-button>
            <el-radio-button label="fast">快</el-radio-button>
          </el-radio-group>
        </div>
        <canvas ref="fruitPreview" width="420" height="120" class="preview"></canvas>
      </section>

      <section class="card">
        <h3>记忆翻翻乐</h3>
        <p class="hint">翻牌配对，就是课堂上说的猜猜乐。卡片正面是图案，不是问号表情。</p>
        <div class="field">
          <span>每次配对的图案数</span>
          <el-radio-group v-model="form.memoryPairs">
            <el-radio-button :label="4">4 对</el-radio-button>
            <el-radio-button :label="6">6 对</el-radio-button>
            <el-radio-button :label="8">8 对</el-radio-button>
          </el-radio-group>
        </div>
        <div class="icon-row">
          <span v-for="icon in icons" :key="icon.id" class="icon-chip" v-html="icon.svg"></span>
        </div>
      </section>

      <div class="actions">
        <button type="button" class="action-btn indigo" :disabled="saving" @click="save">
          {{ saving ? '保存中…' : '保存设置' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getParkSettings, saveParkSettings } from '../../api'
import { FRUIT_KINDS, MEMORY_ICONS, drawBasket, drawFruit } from '../../components/student/games/parkPictures'

const gameList = [
  { id: 'snake', title: '贪吃蛇' },
  { id: 'typing', title: '打字小能手' },
  { id: 'memory', title: '记忆翻翻乐' },
  { id: '2048', title: '2048' },
  { id: 'fruit', title: '接水果' },
  { id: 'rope', title: '切绳接球' }
]
const icons = MEMORY_ICONS
const loading = ref(false)
const saving = ref(false)
const fruitPreview = ref(null)
const form = reactive({
  games: {
    snake: true,
    typing: true,
    memory: true,
    2048: true,
    fruit: true,
    rope: true
  },
  fruitLives: 3,
  fruitSpeed: 'normal',
  memoryPairs: 8
})

const applyConfig = (data) => {
  if (!data) return
  if (data.games) {
    gameList.forEach((game) => {
      form.games[game.id] = data.games[game.id] !== false
    })
  }
  if (data.fruitLives) form.fruitLives = Number(data.fruitLives)
  if (data.fruitSpeed) form.fruitSpeed = data.fruitSpeed
  if (data.memoryPairs) form.memoryPairs = Number(data.memoryPairs)
}

const paintPreview = () => {
  const canvas = fruitPreview.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, 420, 120)
  ctx.fillStyle = '#e0f2fe'
  ctx.fillRect(0, 0, 420, 120)
  FRUIT_KINDS.forEach((kind, i) => drawFruit(ctx, kind, 50 + i * 70, 48))
  drawBasket(ctx, 310, 62)
}

const load = async () => {
  loading.value = true
  try {
    const res = await getParkSettings()
    applyConfig(res.data)
  } catch {
    ElMessage.warning('暂时没有读到已保存的设置，页面上是默认参数')
  } finally {
    loading.value = false
    await nextTick()
    paintPreview()
  }
}

const save = async () => {
  if (!gameList.some((game) => form.games[game.id])) {
    ElMessage.warning('请至少开放一个游戏')
    return
  }
  saving.value = true
  try {
    const res = await saveParkSettings({
      games: { ...form.games },
      fruitLives: form.fruitLives,
      fruitSpeed: form.fruitSpeed,
      memoryPairs: form.memoryPairs
    })
    applyConfig(res.data)
    ElMessage.success('游戏设置已保存')
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.park-settings {
  min-height: calc(100vh - 56px);
  background: #f5f3ff;
}
.park-header {
  padding: 14px 20px 16px;
  background: linear-gradient(135deg, #5b4fc7 0%, #7c3aed 45%, #9333ea 100%);
  color: #fff;
  box-shadow: 0 4px 20px rgba(91, 79, 199, 0.35);
}
.header-main h1 {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}
.title-icon { font-size: 20px; }
.header-main p {
  margin: 0;
  font-size: 12px;
  opacity: 0.9;
  line-height: 1.5;
  max-width: 720px;
}
.park-body {
  padding: 16px 20px 28px;
  max-width: 900px;
}
.card {
  background: #fff;
  border: 1px solid #e9e5ff;
  border-radius: 14px;
  padding: 16px 18px 18px;
  margin-bottom: 14px;
  box-shadow: 0 2px 12px rgba(91, 79, 199, 0.06);
}
.card h3 { margin: 0 0 8px; color: #312e81; font-size: 15px; }
.hint { margin: 0 0 12px; color: #64748b; font-size: 13px; }
.game-switches {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px 18px;
}
.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 10px;
  background: #faf8ff;
  border: 1px solid #ede9fe;
}
.field { display: flex; align-items: center; gap: 12px; margin: 10px 0; color: #334155; }
.preview { width: 100%; max-width: 420px; border-radius: 12px; display: block; margin-top: 8px; }
.icon-row { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.icon-chip {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: #faf8ff;
  border: 1px solid #e9e5ff;
  display: grid;
  place-items: center;
}
.icon-chip :deep(svg) { width: 40px; height: 40px; }
.actions { margin-top: 4px; }
.action-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
}
.action-btn:disabled { opacity: 0.55; cursor: not-allowed; }
.action-btn.indigo { background: linear-gradient(135deg, #4338ca, #6366f1); }
@media (max-width: 800px) {
  .game-switches { grid-template-columns: 1fr; }
}
</style>
