<template>
  <div class="snake-wrap">
    <div class="toolbar">
      <span>得分：{{ score }}</span>
      <span v-if="over" class="over">游戏结束</span>
      <el-button size="small" type="primary" @click="restart">重新开始</el-button>
    </div>
    <canvas ref="canvasRef" width="560" height="400" tabindex="0" @keydown="onKey" />
    <p class="hint">方向键或 WASD 控制，空格暂停</p>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'

const canvasRef = ref(null)
const score = ref(0)
const over = ref(false)

const CELL = 20
const COLS = 28
const ROWS = 20
let snake = []
let dir = { x: 1, y: 0 }
let nextDir = { x: 1, y: 0 }
let food = { x: 10, y: 10 }
let timer = null
let paused = false

const randFood = () => {
  let p
  do {
    p = { x: Math.floor(Math.random() * COLS), y: Math.floor(Math.random() * ROWS) }
  } while (snake.some((s) => s.x === p.x && s.y === p.y))
  food = p
}

const draw = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = '#0f172a'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  ctx.fillStyle = '#f59e0b'
  ctx.fillRect(food.x * CELL, food.y * CELL, CELL - 1, CELL - 1)
  snake.forEach((s, i) => {
    ctx.fillStyle = i === 0 ? '#34d399' : '#10b981'
    ctx.fillRect(s.x * CELL, s.y * CELL, CELL - 1, CELL - 1)
  })
}

const tick = () => {
  if (paused || over.value) return
  dir = nextDir
  const head = { x: snake[0].x + dir.x, y: snake[0].y + dir.y }
  if (head.x < 0 || head.y < 0 || head.x >= COLS || head.y >= ROWS
    || snake.some((s) => s.x === head.x && s.y === head.y)) {
    over.value = true
    draw()
    return
  }
  snake.unshift(head)
  if (head.x === food.x && head.y === food.y) {
    score.value += 10
    randFood()
  } else {
    snake.pop()
  }
  draw()
}

const restart = () => {
  snake = [{ x: 8, y: 10 }, { x: 7, y: 10 }, { x: 6, y: 10 }]
  dir = { x: 1, y: 0 }
  nextDir = { x: 1, y: 0 }
  score.value = 0
  over.value = false
  paused = false
  randFood()
  draw()
  canvasRef.value?.focus()
}

const onKey = (e) => {
  const map = {
    ArrowUp: { x: 0, y: -1 },
    ArrowDown: { x: 0, y: 1 },
    ArrowLeft: { x: -1, y: 0 },
    ArrowRight: { x: 1, y: 0 },
    w: { x: 0, y: -1 },
    s: { x: 0, y: 1 },
    a: { x: -1, y: 0 },
    d: { x: 1, y: 0 }
  }
  const key = e.key.length === 1 ? e.key.toLowerCase() : e.key
  if (key === ' ' || key === 'Spacebar') {
    paused = !paused
    e.preventDefault()
    return
  }
  const nd = map[key]
  if (!nd) return
  if (nd.x + dir.x === 0 && nd.y + dir.y === 0) return
  nextDir = nd
  e.preventDefault()
}

onMounted(() => {
  restart()
  timer = setInterval(tick, 140)
  canvasRef.value?.focus()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.snake-wrap { text-align: center; }
.toolbar {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-bottom: 10px;
  font-weight: 600;
  color: #1e293b;
}
.over { color: #ef4444; }
canvas {
  display: block;
  margin: 0 auto;
  border-radius: 12px;
  outline: none;
  max-width: 100%;
}
.hint { margin: 10px 0 0; color: #94a3b8; font-size: 12px; }
</style>
