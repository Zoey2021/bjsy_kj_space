<template>
  <div class="preview-root">
    <div class="preview-toolbar">
      <span class="tab active">互动挑战</span>
      <span class="tab">全班提交</span>
      <span class="tab edit">编辑活动</span>
    </div>

    <div class="preview-body">
      <div class="preview-card intro-card" v-if="showIntro">
        <div class="card-tag">课程介绍</div>
        <div class="rich" v-if="lesson?.content" v-html="lesson.content" />
        <p v-else class="empty-tip">暂未填写课程介绍，请在左侧「课程介绍」中编辑</p>
      </div>

      <div class="preview-columns" v-if="activeTask || resources.length">
        <div class="preview-card challenge-card" v-if="activeTask">
          <div class="card-head">
            <span class="emoji">💡</span>
            <span>今日挑战 · {{ activeTask.title }}</span>
          </div>
          <p class="task-desc">{{ activeTask.description || '请根据任务要求完成学习。' }}</p>

          <template v-if="activeTask.taskType === 'CHOICE'">
            <div v-for="q in choiceQuestions" :key="q.id" class="choice-block">
              <p>{{ q.text }}</p>
              <div class="choice-row">
                <span v-for="opt in q.options" :key="opt" class="choice-pill">{{ opt }}</span>
              </div>
            </div>
          </template>
          <template v-else>
            <div v-for="field in formFields" :key="field.name" class="form-preview">
              <label>{{ field.label }}</label>
              <div class="fake-input">{{ field.type === 'textarea' ? '在此输入…' : '在此填写' }}</div>
            </div>
          </template>
          <el-button type="primary" class="submit-demo" round>提交作业</el-button>
        </div>

        <div class="preview-card resource-card" v-if="resources.length">
          <div class="card-head">
            <span class="emoji">📎</span>
            <span>课程资源</span>
          </div>
          <div v-for="res in resources" :key="res.id" class="res-item">
            <h5>{{ res.title }}</h5>
            <div v-if="res.contentText" class="rich small" v-html="res.contentText" />
            <a v-if="res.contentUrl" :href="res.contentUrl" target="_blank" class="link">打开链接</a>
          </div>
        </div>
      </div>

      <div v-if="!lesson?.content && !activeTask && !resources.length" class="preview-empty">
        <p>学生端将在此看到本课介绍、互动任务与学习资源</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  lesson: { type: Object, default: null },
  resources: { type: Array, default: () => [] },
  tasks: { type: Array, default: () => [] },
  activeTaskId: { type: Number, default: null },
  panel: { type: String, default: 'challenge' }
})

const showIntro = computed(() => props.panel === 'intro' || (!props.activeTask && props.panel !== 'resource'))

const activeTask = computed(() => {
  if (props.panel === 'intro') return null
  const list = props.tasks || []
  if (!list.length) return null
  if (props.activeTaskId) return list.find(t => t.id === props.activeTaskId) || list[0]
  return list[0]
})

const parseConfig = (task) => {
  try { return JSON.parse(task?.configJson || '{}') } catch { return {} }
}

const choiceQuestions = computed(() => parseConfig(activeTask.value).questions || [])
const formFields = computed(() => parseConfig(activeTask.value).fields || [])
</script>

<style scoped>
.preview-root {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #f5f3ff 0%, #faf8ff 40%, #fff 100%);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #e9e5ff;
}
.preview-toolbar {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.85);
  border-bottom: 1px solid #ebe8ff;
}
.tab {
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  color: #64748b;
  background: #f1f5f9;
}
.tab.active {
  background: linear-gradient(135deg, #8b7cf7, #6d5ce8);
  color: #fff;
  font-weight: 600;
}
.tab.edit {
  background: #fce7f3;
  color: #be185d;
}
.preview-body {
  flex: 1;
  overflow: auto;
  padding: 16px;
}
.preview-columns {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 14px;
}
.preview-card {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 4px 20px rgba(109, 92, 232, 0.08);
  border: 1px solid #f0edff;
}
.intro-card { margin-bottom: 14px; }
.card-tag {
  display: inline-block;
  font-size: 12px;
  color: #6d5ce8;
  background: #f3f0ff;
  padding: 4px 10px;
  border-radius: 999px;
  margin-bottom: 10px;
}
.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: #4338ca;
  margin-bottom: 12px;
  font-size: 15px;
}
.emoji { font-size: 18px; }
.task-desc { color: #64748b; font-size: 13px; margin-bottom: 14px; line-height: 1.5; }
.choice-block { margin-bottom: 12px; }
.choice-block p { font-size: 13px; margin-bottom: 8px; }
.choice-row { display: flex; gap: 8px; flex-wrap: wrap; }
.choice-pill {
  padding: 6px 14px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  font-size: 13px;
}
.form-preview { margin-bottom: 12px; }
.form-preview label { display: block; font-size: 12px; color: #64748b; margin-bottom: 4px; }
.fake-input {
  border: 1px dashed #c4b5fd;
  border-radius: 8px;
  padding: 10px;
  color: #94a3b8;
  font-size: 13px;
  background: #faf5ff;
}
.submit-demo {
  margin-top: 12px;
  background: linear-gradient(135deg, #f472b6, #ec4899);
  border: none;
}
.res-item {
  padding: 10px 0;
  border-bottom: 1px dashed #e9e5ff;
}
.res-item:last-child { border-bottom: none; }
.res-item h5 { margin: 0 0 6px; font-size: 14px; color: #334155; }
.link { color: #6d5ce8; font-size: 13px; }
.rich :deep(p) { margin: 0 0 8px; line-height: 1.6; color: #334155; }
.rich.small { font-size: 13px; }
.empty-tip, .preview-empty {
  text-align: center;
  color: #94a3b8;
  padding: 40px 20px;
  font-size: 14px;
}
@media (max-width: 900px) {
  .preview-columns { grid-template-columns: 1fr; }
}
</style>
