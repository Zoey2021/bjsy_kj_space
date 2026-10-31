<template>
  <div class="memory-wrap">
    <div class="toolbar">
      <span>步数：{{ moves }}</span>
      <span v-if="won" class="won">配对成功！</span>
      <el-button size="small" type="primary" @click="reset">重新开始</el-button>
    </div>
    <div class="board" :style="{ gridTemplateColumns: `repeat(${columns}, 1fr)` }">
      <button
        v-for="card in cards"
        :key="card.id"
        type="button"
        class="card"
        :class="{ flipped: card.flipped || card.matched, matched: card.matched }"
        :disabled="card.matched || lock"
        @click="flip(card)"
      >
        <span v-if="!(card.flipped || card.matched)" class="front">?</span>
        <span v-else class="back" v-html="card.svg"></span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { MEMORY_ICONS } from './parkPictures'

const props = defineProps({
  pairs: { type: Number, default: 8 }
})

const cards = ref([])
const first = ref(null)
const lock = ref(false)
const moves = ref(0)
const won = ref(false)

const columns = computed(() => (cards.value.length > 12 ? 4 : cards.value.length > 8 ? 4 : 4))

const reset = () => {
  const count = [4, 6, 8].includes(Number(props.pairs)) ? Number(props.pairs) : 8
  const icons = MEMORY_ICONS.slice(0, count)
  const pool = [...icons, ...icons]
    .sort(() => Math.random() - 0.5)
    .map((icon, i) => ({
      id: i,
      key: icon.id,
      svg: icon.svg,
      flipped: false,
      matched: false
    }))
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
  if (first.value.key === card.key) {
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
  gap: 10px;
  max-width: 460px;
  margin: 0 auto;
}
.card {
  height: 84px;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  background: #4338ca;
  color: #fff;
  font-size: 28px;
  padding: 0;
}
.card .front,
.card .back {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  border-radius: 12px;
}
.card .back { background: #fff7ed; }
.card .back :deep(svg) { width: 48px; height: 48px; }
.card.matched { cursor: default; }
.card.matched .back { background: #d1fae5; }
</style>
