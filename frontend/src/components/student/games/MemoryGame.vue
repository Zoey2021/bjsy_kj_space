<template>
  <div class="memory-wrap">
    <div class="toolbar">
      <span>步数：{{ moves }}</span>
      <span v-if="won" class="won">配对成功！</span>
      <el-button size="small" type="primary" @click="reset">重新开始</el-button>
    </div>
    <div class="board">
      <button
        v-for="card in cards"
        :key="card.id"
        type="button"
        class="card"
        :class="{ flipped: card.flipped || card.matched, matched: card.matched }"
        :disabled="card.matched || lock"
        @click="flip(card)"
      >
        <span class="front">?</span>
        <span class="back">{{ card.emoji }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const EMOJIS = ['🍎', '🚀', '🎹', '🐱', '🌟', '⚽', '📚', '🧩']
const cards = ref([])
const first = ref(null)
const lock = ref(false)
const moves = ref(0)
const won = ref(false)

const reset = () => {
  const pool = [...EMOJIS, ...EMOJIS]
    .sort(() => Math.random() - 0.5)
    .map((emoji, i) => ({ id: i, emoji, flipped: false, matched: false }))
  cards.value = pool
  first.value = null
  lock.value = false
  moves.value = 0
  won.value = false
}

const flip = (card) => {
  if (lock.value || card.flipped || card.matched) return
  card.flipped = true
  if (!first.value) {
    first.value = card
    return
  }
  moves.value += 1
  lock.value = true
  if (first.value.emoji === card.emoji) {
    first.value.matched = true
    card.matched = true
    first.value = null
    lock.value = false
    won.value = cards.value.every((c) => c.matched)
  } else {
    const a = first.value
    const b = card
    setTimeout(() => {
      a.flipped = false
      b.flipped = false
      first.value = null
      lock.value = false
    }, 650)
  }
}

reset()
</script>

<style scoped>
.memory-wrap { text-align: center; }
.toolbar {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
  font-weight: 600;
  color: #1e293b;
}
.won { color: #059669; }
.board {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  max-width: 420px;
  margin: 0 auto;
}
.card {
  position: relative;
  height: 72px;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  background: #312e81;
  color: #fff;
  font-size: 28px;
  perspective: 600px;
}
.card .front,
.card .back {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  border-radius: 12px;
  backface-visibility: hidden;
  transition: transform 0.25s ease;
}
.card .front { background: #4338ca; }
.card .back {
  background: #fff;
  transform: rotateY(180deg);
}
.card.flipped .front { transform: rotateY(180deg); }
.card.flipped .back { transform: rotateY(0); }
.card.matched {
  background: #d1fae5;
  cursor: default;
}
.card.matched .back { background: #d1fae5; }
</style>
