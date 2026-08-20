<template>
  <div class="typing-wrap">
    <div class="stats">
      <span>正确 {{ correct }}</span>
      <span>错误 {{ wrong }}</span>
      <span>速度 {{ wpm }} 词/分</span>
    </div>
    <div class="prompt">{{ current }}</div>
    <el-input
      ref="inputRef"
      v-model="typed"
      size="large"
      placeholder="在此输入上方词语后按回车"
      @keyup.enter="check"
    />
    <div class="actions">
      <el-button type="primary" @click="check">确认</el-button>
      <el-button @click="reset">重新开始</el-button>
    </div>
    <p class="hint">提示：输入与上方完全一致后按回车</p>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'

const WORDS = [
  '信息', '科技', '算法', '数据', '网络', '编程', '智能', '编码',
  '键盘', '鼠标', '软件', '硬件', '云计算', '人工智能', '学习空间',
  'hello', 'world', 'python', 'scratch', 'code', 'binary', 'robot'
]

const current = ref('')
const typed = ref('')
const correct = ref(0)
const wrong = ref(0)
const startedAt = ref(0)
const inputRef = ref(null)

const wpm = computed(() => {
  if (!startedAt.value || correct.value === 0) return 0
  const minutes = (Date.now() - startedAt.value) / 60000
  return minutes > 0 ? Math.round(correct.value / minutes) : 0
})

const nextWord = () => {
  current.value = WORDS[Math.floor(Math.random() * WORDS.length)]
  typed.value = ''
  nextTick(() => inputRef.value?.focus?.())
}

const check = () => {
  if (!typed.value.trim()) return
  if (!startedAt.value) startedAt.value = Date.now()
  if (typed.value.trim() === current.value) {
    correct.value += 1
  } else {
    wrong.value += 1
  }
  nextWord()
}

const reset = () => {
  correct.value = 0
  wrong.value = 0
  startedAt.value = 0
  nextWord()
}

onMounted(reset)
</script>

<style scoped>
.typing-wrap { max-width: 520px; margin: 0 auto; }
.stats {
  display: flex;
  justify-content: space-between;
  margin-bottom: 18px;
  color: #475569;
  font-weight: 600;
}
.prompt {
  text-align: center;
  font-size: 36px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: 0.08em;
  margin-bottom: 18px;
  min-height: 52px;
}
.actions {
  margin-top: 14px;
  display: flex;
  gap: 10px;
  justify-content: center;
}
.hint { text-align: center; color: #94a3b8; font-size: 12px; margin-top: 12px; }
</style>
