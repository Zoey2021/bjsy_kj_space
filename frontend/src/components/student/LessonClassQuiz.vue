<template>
  <div class="lesson-quiz">
    <header class="quiz-head">
      <h2>{{ quiz.title || '课堂小测' }}</h2>
      <p>共 {{ questions.length }} 道选择题，从「知识掌握」「能力提升」「素养变化」三个维度考查，请独立完成后提交。</p>
    </header>

    <div v-if="submitted" class="quiz-result">
      <div class="score-ring" :class="scoreLevel">
        <span class="score-num">{{ score }}</span>
        <span class="score-label">分</span>
      </div>
      <p class="result-text">
        答对 <strong>{{ correctCount }}</strong> / {{ questions.length }} 题
        <span v-if="score >= 80">，表现优秀！</span>
        <span v-else-if="score >= 60">，继续加油！</span>
        <span v-else>，建议回顾本课内容后再试。</span>
      </p>
      <div v-if="dimensionScores.length" class="dim-scores">
        <div v-for="d in dimensionScores" :key="d.name" class="dim-row">
          <span>{{ d.name }}</span>
          <el-progress :percentage="d.rate" :stroke-width="8" :color="d.color" />
          <span class="dim-num">{{ d.correct }}/{{ d.total }}</span>
        </div>
      </div>
      <el-button type="primary" @click="retry">重新作答</el-button>
    </div>

    <template v-else>
      <template v-for="group in questionGroups" :key="group.dimension">
        <div class="dim-section">
          <h3 class="dim-title">{{ group.dimension }}</h3>
          <div v-for="{ q, qi } in group.items" :key="q.id || qi" class="quiz-item">
            <div class="q-meta">
              <span class="q-num">第 {{ qi + 1 }} 题</span>
              <el-tag size="small" :type="dimTag(q.dimension)">{{ q.dimension }}</el-tag>
            </div>
            <p class="q-stem">{{ q.stem }}</p>
            <el-radio-group v-model="answers[qi]" class="q-options">
              <el-radio
                v-for="(opt, oi) in q.options"
                :key="oi"
                :label="oi"
                class="q-option"
              >
                {{ optionLabel(oi) }}. {{ opt }}
              </el-radio>
            </el-radio-group>
            <p v-if="showReview && answers[qi] != null" class="q-explain" :class="{ wrong: answers[qi] !== q.answer }">
              {{ answers[qi] === q.answer ? '✓ 回答正确' : '✗ 回答错误' }} — {{ q.explanation }}
            </p>
          </div>
        </div>
      </template>

      <div class="quiz-actions">
        <el-button type="primary" size="large" :loading="submitting" @click="submit">
          提交小测
        </el-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  quiz: { type: Object, default: () => ({}) },
  submitting: { type: Boolean, default: false }
})

const emit = defineEmits(['submit'])

const DIMENSION_ORDER = ['知识掌握', '能力提升', '素养变化']

const questions = computed(() => props.quiz?.questions || [])
const answers = reactive({})
const submitted = ref(false)
const showReview = ref(false)
const score = ref(0)
const correctCount = ref(0)

const scoreLevel = computed(() => {
  if (score.value >= 80) return 'high'
  if (score.value >= 60) return 'mid'
  return 'low'
})

const questionGroups = computed(() => {
  const map = new Map()
  questions.value.forEach((q, qi) => {
    const dim = q.dimension || '其他'
    if (!map.has(dim)) map.set(dim, [])
    map.get(dim).push({ q, qi })
  })
  return DIMENSION_ORDER.filter((d) => map.has(d)).map((dimension) => ({
    dimension,
    items: map.get(dimension)
  }))
})

const dimensionScores = computed(() => {
  const colors = { 知识掌握: '#6366f1', 能力提升: '#22c55e', 素养变化: '#f59e0b' }
  return DIMENSION_ORDER.map((name) => {
    const qs = questions.value
      .map((q, i) => ({ q, i }))
      .filter(({ q }) => (q.dimension || '') === name)
    const total = qs.length
    const correct = qs.filter(({ q, i }) => answers[i] === q.answer).length
    return {
      name,
      total,
      correct,
      rate: total ? Math.round((correct / total) * 100) : 0,
      color: colors[name] || '#64748b'
    }
  }).filter((d) => d.total > 0)
})

const optionLabel = (i) => String.fromCharCode(65 + i)

const dimTag = (d) => {
  if (d === '知识掌握') return 'primary'
  if (d === '能力提升') return 'success'
  if (d === '素养变化') return 'warning'
  return 'info'
}

watch(questions, () => {
  submitted.value = false
  showReview.value = false
  Object.keys(answers).forEach((k) => delete answers[k])
}, { immediate: true })

const submit = () => {
  const total = questions.value.length
  for (let i = 0; i < total; i++) {
    if (answers[i] == null) {
      ElMessage.warning(`请完成第 ${i + 1} 题`)
      return
    }
  }
  let correct = 0
  questions.value.forEach((q, i) => {
    if (answers[i] === q.answer) correct++
  })
  correctCount.value = correct
  score.value = Math.round((correct / total) * 100)
  submitted.value = true
  showReview.value = true

  const byDimension = {}
  questions.value.forEach((q, i) => {
    const dim = q.dimension || '其他'
    if (!byDimension[dim]) byDimension[dim] = { correct: 0, total: 0 }
    byDimension[dim].total++
    if (answers[i] === q.answer) byDimension[dim].correct++
  })

  emit('submit', {
    type: 'quiz',
    quizScore: score.value,
    correctCount: correct,
    totalQuestions: total,
    dimensionScores: byDimension,
    answers: questions.value.map((q, i) => ({
      questionId: q.id,
      dimension: q.dimension,
      selected: answers[i],
      correct: q.answer,
      isCorrect: answers[i] === q.answer
    })),
    activityIndex: 99
  })
}

const retry = () => {
  submitted.value = false
  showReview.value = false
  Object.keys(answers).forEach((k) => delete answers[k])
}

defineExpose({
  restoreResult(result) {
    if (!result) return
    submitted.value = true
    showReview.value = true
    score.value = result.quizScore || 0
    correctCount.value = result.correctCount || 0
    ;(result.answers || []).forEach((a, i) => { answers[i] = a.selected })
  }
})
</script>

<style scoped>
.lesson-quiz {
  padding: 24px 28px;
  max-width: 760px;
}
.quiz-head h2 {
  margin: 0 0 8px;
  font-size: 20px;
  color: #1e40af;
}
.quiz-head p {
  margin: 0 0 24px;
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
}
.dim-section {
  margin-bottom: 24px;
}
.dim-title {
  margin: 0 0 12px;
  padding: 8px 14px;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  background: linear-gradient(90deg, #eef2ff, transparent);
  border-left: 4px solid #6366f1;
  border-radius: 0 8px 8px 0;
}
.dim-section:nth-child(2) .dim-title {
  border-left-color: #22c55e;
  background: linear-gradient(90deg, #f0fdf4, transparent);
}
.dim-section:nth-child(3) .dim-title {
  border-left-color: #f59e0b;
  background: linear-gradient(90deg, #fffbeb, transparent);
}
.quiz-item {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px 18px;
  margin-bottom: 12px;
}
.q-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.q-num {
  font-size: 13px;
  font-weight: 700;
  color: #5b4fc7;
}
.q-stem {
  margin: 0 0 12px;
  font-size: 15px;
  line-height: 1.65;
  color: #334155;
  font-weight: 500;
}
.q-options {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}
.q-option {
  margin: 0;
  white-space: normal;
  height: auto;
  line-height: 1.5;
}
.q-explain {
  margin: 12px 0 0;
  font-size: 13px;
  color: #16a34a;
  line-height: 1.55;
  padding: 10px 12px;
  background: #f0fdf4;
  border-radius: 8px;
}
.q-explain.wrong {
  color: #dc2626;
  background: #fef2f2;
}
.quiz-actions {
  margin-top: 8px;
  text-align: center;
}
.quiz-result {
  text-align: center;
  padding: 32px 16px;
}
.score-ring {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  margin: 0 auto 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.score-ring.high { background: linear-gradient(135deg, #22c55e, #16a34a); }
.score-ring.mid { background: linear-gradient(135deg, #6366f1, #5b4fc7); }
.score-ring.low { background: linear-gradient(135deg, #f97316, #ea580c); }
.score-num { font-size: 36px; font-weight: 800; line-height: 1; }
.score-label { font-size: 14px; opacity: 0.9; }
.result-text { margin: 0 0 20px; color: #475569; font-size: 15px; }
.dim-scores {
  max-width: 360px;
  margin: 0 auto 24px;
  text-align: left;
}
.dim-row {
  display: grid;
  grid-template-columns: 72px 1fr 40px;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
  font-size: 13px;
  color: #475569;
}
.dim-num { text-align: right; font-weight: 600; color: #334155; }
</style>
