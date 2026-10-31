<template>
  <div class="rope">
    <div class="toolbar">
      <span>{{ message }}</span>
      <el-button size="small" type="primary" @click="reset">再来一次</el-button>
    </div>
    <p class="hint">小球会来回摆动。看准时机点一下绳子，让它落进右边的篮子。</p>
    <canvas ref="canvasRef" width="640" height="380" @click="cut" />
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'

const canvasRef = ref(null)
const message = ref('等待切开绳子')
let raf = 0
let angle = -0.9
let angVel = 0
let cutOff = false
let ball = { x: 180, y: 80, vx: 0, vy: 0 }
let settled = false
const anchor = { x: 180, y: 36 }
const length = 150

const reset = () => {
  angle = -0.9
  angVel = 0
  cutOff = false
  settled = false
  message.value = '等待切开绳子'
}

const cut = (e) => {
  if (cutOff || settled) return
  const rect = canvasRef.value.getBoundingClientRect()
  const scale = 640 / rect.width
  const px = (e.clientX - rect.left) * scale
  const py = (e.clientY - rect.top) * scale
  const bx = anchor.x + Math.sin(angle) * length
  const by = anchor.y + Math.cos(angle) * length
  const dist = pointLineDistance(px, py, anchor.x, anchor.y, bx, by)
  if (dist > 16) return
  cutOff = true
  ball = {
    x: bx,
    y: by,
    vx: Math.cos(angle) * angVel * length,
    vy: -Math.sin(angle) * angVel * length
  }
  message.value = '绳子断开了'
}

const pointLineDistance = (px, py, x1, y1, x2, y2) => {
  const dx = x2 - x1
  const dy = y2 - y1
  const len = Math.hypot(dx, dy) || 1
  let t = ((px - x1) * dx + (py - y1) * dy) / (len * len)
  t = Math.max(0, Math.min(1, t))
  return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy))
}

const loop = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, 640, 380)
  ctx.fillStyle = '#f8fafc'
  ctx.fillRect(0, 0, 640, 380)

  if (!cutOff) {
    angVel += -0.004 * Math.sin(angle)
    angVel *= 0.995
    angle += angVel
    ball.x = anchor.x + Math.sin(angle) * length
    ball.y = anchor.y + Math.cos(angle) * length
  } else if (!settled) {
    ball.vy += 0.28
    ball.x += ball.vx
    ball.y += ball.vy
    if (ball.y > 340) {
      settled = true
      const inBasket = ball.x > 470 && ball.x < 590
      message.value = inBasket ? '接住了！' : '没接住，再试一次'
    }
  }

  ctx.strokeStyle = '#94a3b8'
  ctx.lineWidth = 3
  if (!cutOff) {
    ctx.beginPath()
    ctx.moveTo(anchor.x, anchor.y)
    ctx.lineTo(ball.x, ball.y)
    ctx.stroke()
  }
  ctx.fillStyle = '#334155'
  ctx.beginPath()
  ctx.arc(anchor.x, anchor.y, 6, 0, Math.PI * 2)
  ctx.fill()
  ctx.font = '32px sans-serif'
  ctx.fillText('⚽', ball.x - 16, ball.y + 10)
  ctx.fillStyle = '#b45309'
  ctx.fillRect(470, 332, 120, 18)
  ctx.fillRect(470, 300, 8, 32)
  ctx.fillRect(582, 300, 8, 32)
  raf = requestAnimationFrame(loop)
}

onMounted(loop)
onUnmounted(() => cancelAnimationFrame(raf))
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; align-items: center; margin-bottom: 6px; }
.hint { margin: 0 0 8px; color: #64748b; font-size: 13px; }
canvas { width: 100%; max-width: 640px; border-radius: 12px; display: block; cursor: pointer; }
</style>
