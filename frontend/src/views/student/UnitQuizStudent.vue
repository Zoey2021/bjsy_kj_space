<template>
  <div class="uq-page" v-loading="loading">
    <header class="uq-head">
      <el-page-header @back="goBack" :content="title || '单元小测'" />
      <p v-if="subtitle" class="sub">{{ subtitle }}</p>
    </header>

    <section v-if="mine?.graded" class="result-card" :class="'lv-' + (mine.gradeLevel || 'D')">
      <div class="lv-badge">{{ mine.gradeLevel || 'D' }}</div>
      <h2>已自动批改</h2>
      <p class="feedback">{{ mine.feedback }}</p>
      <p class="hint">具体分数仅教师可见。如有疑问，请找张老师聊聊。</p>
      <div class="wait-actions">
        <el-button type="primary" @click="goPark">申请游学乐园</el-button>
      </div>
    </section>

    <section v-else-if="mine?.submitted && !editing" class="wait-card">
      <h2>已提交</h2>
      <p>提交时间：{{ formatTime(mine.submittedAt) }}</p>
      <p class="hint">系统正在出等级，请点刷新；完成后可申请游学乐园。</p>
      <div class="wait-actions">
        <el-button @click="load" :loading="loading">刷新状态</el-button>
      </div>
    </section>

    <section v-else class="paper-card">
      <p class="intro">{{ intro }}</p>

      <template v-for="sec in sections" :key="sec.id">
        <h3 class="sec-title">{{ sec.title }}<span v-if="sec.hint">{{ sec.hint }}</span></h3>
        <div v-for="q in questionsBySection(sec.id)" :key="q.id" class="q-block">
          <div class="q-top">
            <div class="q-stem">
              <span class="q-no">{{ q.no }}.</span>
              <span>{{ q.stem }}</span>
            </div>
            <label class="unsure">
              <input type="checkbox" v-model="unsure[q.id]" />
              <span>?</span>
              <em>不确定</em>
            </label>
          </div>

          <pre v-if="q.code" class="code">{{ q.code }}</pre>

          <el-radio-group v-if="q.type === 'single' || q.type === 'choice'" v-model="answers[q.id]" class="choice-group">
            <el-radio v-for="opt in q.options || []" :key="opt.value" :label="opt.value">
              {{ opt.value }}. {{ opt.label }}
            </el-radio>
          </el-radio-group>

          <el-checkbox-group v-else-if="q.type === 'multi'" v-model="answers[q.id]" class="choice-group">
            <el-checkbox v-for="opt in q.options || []" :key="opt.value" :label="opt.value">
              {{ opt.value }}. {{ opt.label }}
            </el-checkbox>
          </el-checkbox-group>

          <div v-else-if="q.type === 'fill_group'" class="fill-group">
            <div v-for="b in q.blanks || []" :key="b.id" class="fill-row">
              <label>{{ b.label }}</label>
              <el-input v-model="answers[b.id]" placeholder="请填写" maxlength="200" />
            </div>
          </div>

          <el-input
            v-else
            v-model="answers[q.id]"
            type="textarea"
            :rows="q.rows || 3"
            maxlength="2000"
            show-word-limit
            placeholder="请输入你的答案"
          />
        </div>
      </template>

      <div class="actions">
        <el-button type="primary" size="large" :loading="submitting" @click="doSubmit">提交小测</el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUnitQuiz, submitUnitQuiz } from '../../api'

const route = useRoute()
const router = useRouter()
const lessonId = computed(() => Number(route.params.lessonId || route.params.id))

const loading = ref(false)
const submitting = ref(false)
const editing = ref(false)
const title = ref('')
const subtitle = ref('')
const intro = ref('')
const sections = ref([])
const questions = ref([])
const mine = ref(null)
const answers = reactive({})
const unsure = reactive({})

const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push('/student/map')
}

const goPark = () => router.push('/student/park')

const formatTime = (v) => {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 19)
}

const questionsBySection = (sid) => questions.value.filter((q) => q.section === sid)

const resetAnswers = (qs, saved) => {
  Object.keys(answers).forEach((k) => delete answers[k])
  Object.keys(unsure).forEach((k) => delete unsure[k])
  const src = saved || {}
  const unsureSrc = src.unsure || {}
  for (const q of qs || []) {
    if (q.type === 'multi') {
      answers[q.id] = Array.isArray(src[q.id]) ? [...src[q.id]] : []
    } else if (q.type === 'fill_group') {
      for (const b of q.blanks || []) {
        answers[b.id] = src[b.id] != null ? src[b.id] : ''
      }
    } else {
      answers[q.id] = src[q.id] != null ? src[q.id] : ''
    }
    unsure[q.id] = !!unsureSrc[q.id]
  }
}

const buildPayload = () => {
  const payload = { ...answers }
  const u = {}
  for (const [k, v] of Object.entries(unsure)) {
    if (v) u[k] = true
  }
  payload.unsure = u
  return payload
}

const load = async () => {
  loading.value = true
  try {
    const res = await getUnitQuiz(lessonId.value)
    const data = res.data || {}
    title.value = data.title || '单元小测'
    subtitle.value = data.subtitle || ''
    intro.value = data.intro || ''
    sections.value = data.sections || []
    questions.value = data.questions || []
    mine.value = data.mine || null
    editing.value = false
    resetAnswers(questions.value, data.mine?.answers)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const doSubmit = async () => {
  for (const q of questions.value) {
    if (!q.required) continue
    if (q.type === 'fill_group') {
      for (const b of q.blanks || []) {
        if (!String(answers[b.id] || '').trim()) {
          ElMessage.warning(`请完成第 ${q.no} 题：${b.label}`)
          return
        }
      }
    } else if (q.type === 'multi') {
      if (!Array.isArray(answers[q.id]) || !answers[q.id].length) {
        ElMessage.warning(`请完成第 ${q.no} 题（多选）`)
        return
      }
    } else if (!String(answers[q.id] || '').trim()) {
      ElMessage.warning(`请完成第 ${q.no} 题`)
      return
    }
  }
  try {
    await ElMessageBox.confirm('提交后将自动批改并给出等级，不可再改。确认提交吗？', '提交单元小测', {
      type: 'warning',
      confirmButtonText: '确认提交',
      cancelButtonText: '再检查一下'
    })
  } catch {
    return
  }
  submitting.value = true
  try {
    const res = await submitUnitQuiz(lessonId.value, { answers: buildPayload() })
    mine.value = res.data || { submitted: true, graded: true, status: 'GRADED' }
    editing.value = false
    ElMessage.success('提交成功，已自动批改')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.uq-page {
  max-width: 860px;
  margin: 0 auto;
  padding: 20px 20px 56px;
}
.uq-head { margin-bottom: 18px; }
.sub { margin: 8px 0 0; color: #64748b; font-size: 14px; }
.intro { color: #475569; line-height: 1.65; margin: 0 0 18px; }
.paper-card, .wait-card, .result-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 22px 24px;
  box-shadow: 0 4px 18px rgba(15, 23, 42, 0.05);
}
.sec-title {
  margin: 22px 0 12px;
  padding: 8px 12px;
  font-size: 16px;
  background: #eef4fb;
  border-left: 4px solid #2b6cb0;
  color: #0f172a;
}
.sec-title span {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  font-weight: 400;
  color: #64748b;
}
.q-block {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px dashed #e2e8f0;
}
.q-top {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 10px;
}
.q-stem {
  font-weight: 600;
  color: #1e293b;
  line-height: 1.55;
  flex: 1;
}
.q-no { color: #2563eb; margin-right: 4px; }
.unsure {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  cursor: pointer;
  user-select: none;
  color: #b45309;
  font-size: 13px;
  background: #fffbeb;
  border: 1px solid #fcd34d;
  border-radius: 8px;
  padding: 4px 8px;
}
.unsure span { font-weight: 800; font-size: 15px; }
.unsure em { font-style: normal; color: #92400e; }
.code {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12.5px;
  line-height: 1.55;
  background: #fafafa;
  border: 1px solid #d8dde5;
  border-left: 3px solid #6b8db8;
  padding: 10px 12px;
  margin: 0 0 12px;
  white-space: pre;
  overflow-x: auto;
  border-radius: 6px;
}
.choice-group {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}
.fill-group { display: flex; flex-direction: column; gap: 10px; }
.fill-row label {
  display: block;
  margin-bottom: 4px;
  color: #334155;
  font-size: 14px;
}
.actions { margin-top: 16px; text-align: center; }
.wait-card h2, .result-card h2 { margin: 0 0 10px; color: #0f172a; }
.hint { color: #64748b; font-size: 13px; }
.result-card { text-align: center; }
.lv-badge {
  width: 88px; height: 88px; margin: 8px auto 16px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 40px; font-weight: 800; color: #fff; background: #64748b;
}
.lv-A .lv-badge { background: linear-gradient(135deg, #16a34a, #22c55e); }
.lv-B .lv-badge { background: linear-gradient(135deg, #2563eb, #3b82f6); }
.lv-C .lv-badge { background: linear-gradient(135deg, #d97706, #f59e0b); }
.lv-D .lv-badge { background: linear-gradient(135deg, #dc2626, #ef4444); }
.feedback { font-size: 18px; line-height: 1.6; color: #1e293b; margin: 0 0 12px; }
.wait-actions { display: flex; gap: 10px; margin-top: 12px; }
</style>
