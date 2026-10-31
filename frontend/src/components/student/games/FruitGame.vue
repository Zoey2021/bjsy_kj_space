<template>
  <div class="fruit">
    <div class="toolbar">
      <span>得分：{{ score }}</span>
      <span>生命：{{ livesLeft }}</span>
      <el-button size="small" type="primary" @click="reset">重新开始</el-button>
    </div>
    <p class="hint">移动鼠标接住掉下来的水果。漏掉 {{ maxLives }} 个就结束这一局。</p>
    <canvas ref="canvasRef" width="640" height="380" @mousemove="onMove" />
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { FRUIT_KINDS, drawBasket, drawFruit } from './parkPictures'

const props = defineProps({
  lives: { type: Number, default: 3 },
  speed: { type: String, default: 'normal' }
})

const canvasRef = ref(null)
const score = ref(0)
const livesLeft = ref(3)
const maxLives = ref(3)
let raf = 0
let fruits = []
let basket = 280
let spawn = 0
let running = true

const speedFactor = () => (props.speed === 'slow' ? 0.72 : props.speed === 'fast' ? 1.45 : 1)
const spawnEvery = () => (props.speed === 'slow' ? 62 : props.speed === 'fast' ? 32 : 45)

const reset = () => {
  maxLives.value = Math.min(8, Math.max(1, Number(props.lives) || 3))
  score.value = 0
  livesLeft.value = maxLives.value
  fruits = []
  spawn = 0
  running = true
}

const onMove = (e) => {
  const rect = canvasRef.value.getBoundingClientRect()
  const scale = 640 / rect.width
  basket = (e.clientX - rect.left) * scale - 40
  basket = Math.max(0, Math.min(560, basket))
}

const loop = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, 640, 380)
  const sky = ctx.createLinearGradient(0, 0, 0, 380)
  sky.addColorStop(0, '#e0f2fe')
  sky.addColorStop(1, '#ecfccb')
  ctx.fillStyle = sky
  ctx.fillRect(0, 0, 640, 380)

  if (running) {
    spawn += 1
    if (spawn % spawnEvery() === 0) {
      fruits.push({
        x: 36 + Math.random() * 540,
        y: -24,
        vy: (2.2 + Math.random() * 1.8) * speedFactor(),
        kind: FRUIT_KINDS[Math.floor(Math.random() * FRUIT_KINDS.length)]
      })
    }
    fruits.forEach((f) => { f.y += f.vy })
    const kept = []
    fruits.forEach((f) => {
      const caught = f.y > 300 && f.y < 348 && f.x > basket + 6 && f.x < basket + 74
      if (caught) {
        score.value += 1
        return
      }
      if (f.y > 390) {
        livesLeft.value -= 1
        if (livesLeft.value <= 0) running = false
        return
      }
      kept.push(f)
    })
    fruits = kept
  }

  fruits.forEach((f) => drawFruit(ctx, f.kind, f.x, f.y))
  drawBasket(ctx, basket, 318)
  if (!running) {
    ctx.fillStyle = 'rgba(15, 23, 42, 0.72)'
    ctx.fillRect(150, 140, 340, 70)
    ctx.fillStyle = '#fff'
    ctx.font = '22px sans-serif'
    ctx.fillText('本局结束，点重新开始', 188, 182)
  }
  raf = requestAnimationFrame(loop)
}

reset()
onMounted(loop)
onUnmounted(() => cancelAnimationFrame(raf))
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; align-items: center; margin-bottom: 6px; }
.hint { margin: 0 0 8px; color: #64748b; font-size: 13px; }
canvas { width: 100%; max-width: 640px; border-radius: 12px; display: block; }
</style>
