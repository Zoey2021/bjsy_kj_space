<template>
  <div v-if="content" class="scaffold-panel">
    <div class="compact-row">
      <div class="compact-main">
        <span class="kicker">本关任务</span>
        <p v-if="taskText" class="task-text" :class="{ clamp: !expanded }">{{ taskText }}</p>
      </div>
      <div class="compact-actions">
        <el-button
          v-if="hints.length && revealed < hints.length"
          type="primary"
          plain
          size="small"
          :loading="reporting"
          @click="revealNext"
        >
          {{ revealed === 0 ? '看提示' : '再看一层' }}
        </el-button>
        <el-tag v-else-if="hints.length" size="small" type="info">提示已展开</el-tag>
        <el-button v-if="hasMore" size="small" text @click="expanded = !expanded">
          {{ expanded ? '收起' : '展开支架' }}
        </el-button>
      </div>
    </div>

    <div v-show="expanded || revealed > 0" class="scaffold-more">
      <section v-if="expanded && extraText" class="block extra">
        <h3>{{ extraTitle }}</h3>
        <p class="task-text">{{ extraText }}</p>
      </section>

      <section v-if="revealed > 0 && hints.length" class="block hints">
        <ol class="hint-list">
          <li v-for="(h, i) in hints.slice(0, revealed)" :key="i">{{ h }}</li>
        </ol>
      </section>

      <section v-if="expanded && traceItems.length" class="block trace">
        <h3>观察一下</h3>
        <p class="trace-tip">看完示例后，选出你认为正确的答案。</p>
        <div v-for="(q, qi) in traceItems" :key="qi" class="trace-item">
          <p class="q-stem">{{ qi + 1 }}. {{ q.stem }}</p>
          <el-radio-group v-model="traceAnswers[qi]" class="q-options" :disabled="traceChecked">
            <el-radio
              v-for="(opt, oi) in q.options"
              :key="oi"
              :label="oi"
              class="q-option"
            >
              {{ optionLabel(oi) }}. {{ opt }}
            </el-radio>
          </el-radio-group>
          <p v-if="traceChecked" class="q-feedback" :class="{ ok: traceAnswers[qi] === q.answer, bad: traceAnswers[qi] !== q.answer }">
            {{ traceAnswers[qi] === q.answer ? '✓ 回答正确' : '✗ 再看一遍示例试试' }}
          </p>
        </div>
        <el-button v-if="!traceChecked" type="primary" size="small" @click="checkTrace">看看对不对</el-button>
        <el-button v-else size="small" @click="resetTrace">再试一次</el-button>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { reportHintUsed } from '../../api'

const props = defineProps({
  lessonId: { type: Number, required: true },
  activityIndex: { type: Number, required: true },
  content: { type: Object, default: null }
})

const expanded = ref(false)
const revealed = ref(0)
const reporting = ref(false)
const traceAnswers = reactive({})
const traceChecked = ref(false)

const taskText = computed(() => String(props.content?.task || '').trim())
const extraTitle = computed(() => props.content?.extraTitle || '示例')
const extraText = computed(() => String(props.content?.extraText || '').trim())
const hints = computed(() => {
  const list = Array.isArray(props.content?.hints) ? props.content.hints : []
  return list.map((h) => String(h || '').trim()).filter(Boolean)
})
const traceItems = computed(() => {
  const list = Array.isArray(props.content?.traceQuestions) ? props.content.traceQuestions : []
  return list.map((q) => ({
    stem: q.question || q.stem || '',
    options: Array.isArray(q.options) ? q.options : [],
    answer: Number(q.answer)
  })).filter((q) => q.stem && q.options.length)
})
const hasMore = computed(() => !!(extraText.value || traceItems.value.length || hints.value.length))

const optionLabel = (i) => ['①', '②', '③', '④', '⑤'][i] || String(i + 1)

watch(
  () => [props.activityIndex, props.content],
  () => {
    expanded.value = false
    revealed.value = 0
    traceChecked.value = false
    Object.keys(traceAnswers).forEach((k) => { delete traceAnswers[k] })
  }
)

const revealNext = async () => {
  if (revealed.value >= hints.value.length) return
  const next = revealed.value
  reporting.value = true
  try {
    await reportHintUsed({
      lessonId: props.lessonId,
      activityIndex: props.activityIndex,
      hintIndex: next
    })
    revealed.value = next + 1
  } catch {
    /* 全局拦截器已提示 */
  } finally {
    reporting.value = false
  }
}

const checkTrace = () => {
  const missing = traceItems.value.some((_, i) => traceAnswers[i] == null)
  if (missing) {
    ElMessage.warning('请先完成全部观察题')
    return
  }
  traceChecked.value = true
}

const resetTrace = () => {
  traceChecked.value = false
  Object.keys(traceAnswers).forEach((k) => { delete traceAnswers[k] })
}
</script>

<style scoped>
.scaffold-panel {
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  padding: 8px 12px;
}
.compact-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
.compact-main {
  flex: 1;
  min-width: 0;
}
.kicker {
  display: inline-block;
  margin-right: 6px;
  font-size: 11px;
  font-weight: 700;
  color: #2563eb;
  letter-spacing: 0.04em;
}
.compact-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
}
.task-text {
  display: inline;
  margin: 0;
  font-size: 13px;
  line-height: 1.45;
  color: #334155;
  white-space: pre-wrap;
}
.task-text.clamp {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  white-space: normal;
}
.scaffold-more {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
}
.block { margin-bottom: 8px; }
.block:last-child { margin-bottom: 0; }
.block h3 {
  margin: 0 0 4px;
  font-size: 12px;
  color: #64748b;
}
.hint-list {
  margin: 0;
  padding-left: 18px;
  color: #334155;
  font-size: 13px;
  line-height: 1.5;
}
.trace-tip { margin: 0 0 6px; font-size: 12px; color: #64748b; }
.trace-item { margin-bottom: 8px; }
.q-stem { margin: 0 0 4px; font-size: 13px; color: #1e293b; }
.q-options { display: flex; flex-direction: column; align-items: flex-start; gap: 2px; }
.q-option { margin: 0; height: auto; white-space: normal; }
.q-feedback { margin: 4px 0 0; font-size: 12px; }
.q-feedback.ok { color: #15803d; }
.q-feedback.bad { color: #b91c1c; }
</style>
