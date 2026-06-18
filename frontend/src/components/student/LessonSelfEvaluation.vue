<template>
  <div class="lesson-eval">
    <header class="eval-head">
      <h2>{{ evaluation.title || '学习评价' }}</h2>
      <p>{{ evaluation.intro || '请根据本课真实学习感受作答，没有标准答案，帮助你和老师了解学习状态。' }}</p>
    </header>

    <div v-if="submitted" class="eval-done">
      <div class="done-icon">✓</div>
      <h3>感谢你的反馈！</h3>
      <p>你的自评已提交，教师可在班级学情中查看元认知数据。</p>
      <el-button type="primary" link @click="retry">修改答案</el-button>
    </div>

    <template v-else>
      <div v-for="(q, qi) in questions" :key="q.id || qi" class="eval-item">
        <div class="q-meta">
          <span class="q-num">第 {{ qi + 1 }} 题</span>
          <el-tag size="small" type="info">{{ q.aspect || '元认知' }}</el-tag>
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
      </div>

      <div class="eval-actions">
        <el-button type="primary" size="large" :loading="submitting" @click="submit">
          提交学习评价
        </el-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  evaluation: { type: Object, default: () => ({}) },
  submitting: { type: Boolean, default: false }
})

const emit = defineEmits(['submit'])

const questions = computed(() => props.evaluation?.questions || [])
const answers = reactive({})
const submitted = ref(false)

const optionLabel = (i) => String.fromCharCode(65 + i)

watch(questions, () => {
  submitted.value = false
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

  submitted.value = true
  emit('submit', {
    type: 'evaluation',
    activityIndex: 98,
    answers: questions.value.map((q, i) => ({
      questionId: q.id,
      aspect: q.aspect,
      selected: answers[i],
      label: q.options[answers[i]]
    }))
  })
  ElMessage.success('学习评价已提交')
}

const retry = () => {
  submitted.value = false
}

defineExpose({
  restoreResult(result) {
    if (!result?.answers) return
    submitted.value = true
    result.answers.forEach((a, i) => { answers[i] = a.selected })
  }
})
</script>

<style scoped>
.lesson-eval {
  padding: 24px 28px;
  max-width: 720px;
}
.eval-head h2 {
  margin: 0 0 8px;
  font-size: 20px;
  color: #1e40af;
}
.eval-head p {
  margin: 0 0 24px;
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
}
.eval-item {
  background: #faf5ff;
  border: 1px solid #e9d5ff;
  border-radius: 12px;
  padding: 16px 18px;
  margin-bottom: 16px;
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
  color: #7c3aed;
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
.eval-actions {
  margin-top: 8px;
  text-align: center;
}
.eval-done {
  text-align: center;
  padding: 40px 16px;
}
.done-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: linear-gradient(135deg, #a78bfa, #7c3aed);
  color: #fff;
  font-size: 32px;
  line-height: 64px;
}
.eval-done h3 {
  margin: 0 0 8px;
  color: #334155;
}
.eval-done p {
  margin: 0 0 16px;
  color: #64748b;
  font-size: 14px;
}
</style>
