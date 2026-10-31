<template>
  <div class="g2048" tabindex="0" @keydown="onKey">
    <div class="toolbar">
      <span>分数：{{ score }}</span>
      <span v-if="won" class="won">达成 2048</span>
      <span v-else-if="lost" class="lost">没有可移动的格子了</span>
      <el-button size="small" type="primary" @click="reset">重新开始</el-button>
    </div>
    <p class="hint">用方向键滑动。相同数字碰在一起会合成更大的数字。</p>
    <div class="board">
      <div v-for="(row, y) in board" :key="y" class="row">
        <div v-for="(n, x) in row" :key="x" class="cell" :class="'n' + n">{{ n || '' }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'

const board = ref([])
const score = ref(0)
const won = ref(false)
const lost = ref(false)

const emptyBoard = () => Array.from({ length: 4 }, () => Array(4).fill(0))

const addTile = (grid) => {
  const spots = []
  grid.forEach((row, y) => row.forEach((n, x) => { if (!n) spots.push([y, x]) }))
  if (!spots.length) return false
  const [y, x] = spots[Math.floor(Math.random() * spots.length)]
  grid[y][x] = Math.random() < 0.9 ? 2 : 4
  return true
}

const slide = (line) => {
  const arr = line.filter((n) => n)
  let gained = 0
  for (let i = 0; i < arr.length - 1; i++) {
    if (arr[i] === arr[i + 1]) {
      arr[i] *= 2
      gained += arr[i]
      arr.splice(i + 1, 1)
    }
  }
  while (arr.length < 4) arr.push(0)
  return { arr, gained }
}

const rotate = (grid) => {
  const next = emptyBoard()
  for (let y = 0; y < 4; y++) {
    for (let x = 0; x < 4; x++) next[x][3 - y] = grid[y][x]
  }
  return next
}

const moveLeft = (grid) => {
  let gained = 0
  let moved = false
  const next = grid.map((row) => {
    const { arr, gained: g } = slide(row)
    gained += g
    if (arr.some((n, i) => n !== row[i])) moved = true
    return arr
  })
  return { next, gained, moved }
}

const canMove = (grid) => {
  for (let y = 0; y < 4; y++) {
    for (let x = 0; x < 4; x++) {
      if (!grid[y][x]) return true
      if (x < 3 && grid[y][x] === grid[y][x + 1]) return true
      if (y < 3 && grid[y][x] === grid[y + 1][x]) return true
    }
  }
  return false
}

const reset = () => {
  const grid = emptyBoard()
  addTile(grid)
  addTile(grid)
  board.value = grid
  score.value = 0
  won.value = false
  lost.value = false
}

const move = (dir) => {
  if (lost.value) return
  let grid = board.value.map((row) => row.slice())
  const turns = { left: 0, up: 3, right: 2, down: 1 }[dir]
  for (let i = 0; i < turns; i++) grid = rotate(grid)
  const { next, gained, moved } = moveLeft(grid)
  if (!moved) return
  grid = next
  for (let i = 0; i < (4 - turns) % 4; i++) grid = rotate(grid)
  addTile(grid)
  score.value += gained
  board.value = grid
  if (grid.some((row) => row.includes(2048))) won.value = true
  if (!canMove(grid)) lost.value = true
}

const onKey = (e) => {
  const map = { ArrowLeft: 'left', ArrowRight: 'right', ArrowUp: 'up', ArrowDown: 'down' }
  if (!map[e.key]) return
  e.preventDefault()
  move(map[e.key])
}

onMounted(() => {
  reset()
  document.querySelector('.g2048')?.focus()
})
</script>

<style scoped>
.g2048 { outline: none; }
.toolbar { display: flex; gap: 12px; align-items: center; margin-bottom: 6px; }
.hint { margin: 0 0 10px; color: #64748b; font-size: 13px; }
.won { color: #059669; font-weight: 700; }
.lost { color: #dc2626; }
.board { background: #bbada0; border-radius: 10px; padding: 8px; width: max-content; }
.row { display: flex; gap: 8px; margin-bottom: 8px; }
.row:last-child { margin-bottom: 0; }
.cell {
  width: 72px; height: 72px; border-radius: 6px; background: #cdc1b4;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; font-weight: 800; color: #776e65;
}
.n2 { background: #eee4da; }
.n4 { background: #ede0c8; }
.n8 { background: #f2b179; color: #f9f6f2; }
.n16 { background: #f59563; color: #f9f6f2; }
.n32 { background: #f67c5f; color: #f9f6f2; }
.n64 { background: #f65e3b; color: #f9f6f2; }
.n128 { background: #edcf72; color: #f9f6f2; font-size: 20px; }
.n256, .n512 { background: #edcc61; color: #f9f6f2; font-size: 20px; }
.n1024, .n2048 { background: #edc22e; color: #f9f6f2; font-size: 18px; }
</style>
