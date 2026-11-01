<template>
  <div class="ai-lab-teacher" v-loading="loading">
    <header class="lab-header">
      <div class="header-main">
        <h1><span class="title-icon">🤖</span> AI实验室</h1>
        <p>与学生端「人工智能实验室」四馆对齐。可控制每馆是否对学生开放，并为馆内应用 / 智能体单独开关。</p>
      </div>
      <button type="button" class="action-btn indigo" :disabled="saving" @click="saveAll">
        {{ saving ? '保存中…' : '保存配置' }}
      </button>
    </header>

    <div class="lab-body">
      <div class="hall-tabs">
        <button
          v-for="h in hallsMeta"
          :key="h.id"
          type="button"
          class="hall-tab"
          :class="{ active: activeHall === h.id, closed: config.halls[h.id] && config.halls[h.id].enabled === false }"
          @click="activeHall = h.id"
        >
          <img :src="h.cover" :alt="h.name" class="tab-cover" />
          <span class="tab-text">
            <b>{{ h.name }}</b>
            <em>{{ config.halls[h.id]?.enabled === false ? '已关闭' : '开放中' }}</em>
          </span>
        </button>
      </div>

      <section v-if="currentHall" class="panel">
        <div class="panel-head">
          <div>
            <h2>{{ currentHall.name || activeMeta?.name }}</h2>
            <p>{{ currentHall.desc || activeMeta?.desc }}</p>
          </div>
          <div class="head-right">
            <div class="open-switch">
              <span>对学生开放</span>
              <el-switch v-model="currentHall.enabled" inline-prompt active-text="开" inactive-text="关" />
            </div>
            <el-tag effect="plain" type="success">应用 {{ enabledCount }} 个已启用</el-tag>
          </div>
        </div>

        <el-form label-width="88px" class="meta-form">
          <el-form-item label="馆名称">
            <el-input v-model="currentHall.name" placeholder="如：体验馆" />
          </el-form-item>
          <el-form-item label="简介">
            <el-input v-model="currentHall.desc" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>

        <!-- 小助手：预置智能体开关 -->
        <div v-if="activeHall === 'agent'" class="agent-box">
          <h3>预置智能体</h3>
          <p class="hint">控制学生端「小助手」馆里能看到哪些智能体。关闭后学生看不到对应卡片。</p>
          <article
            v-for="bot in assistantsMeta"
            :key="bot.id"
            class="assistant-row"
          >
            <img :src="bot.cover" :alt="bot.name" class="assistant-cover" />
            <div class="assistant-meta">
              <b>{{ bot.name }}</b>
              <span>{{ bot.tagline || bot.desc }}</span>
            </div>
            <el-switch
              :model-value="assistantEnabled(bot.id)"
              inline-prompt
              active-text="开"
              inactive-text="关"
              @change="(v) => setAssistantEnabled(bot.id, v)"
            />
          </article>
        </div>

        <!-- 小助手旧字段（可选兜底） -->
        <div v-if="activeHall === 'agent'" class="agent-box">
          <h3>整馆兜底接入（可选）</h3>
          <p class="hint">一般不用填。优先用上方预置智能体；也可另填网页发布链接或扣子 Bot。</p>
          <el-form label-width="100px">
            <el-form-item label="网页链接">
              <el-input v-model="currentHall.embedUrl" placeholder="https://..." />
            </el-form-item>
            <el-form-item label="Bot ID">
              <el-input v-model="currentHall.botId" placeholder="可选" />
            </el-form-item>
            <el-form-item label="Token">
              <el-input v-model="currentHall.token" placeholder="可选" show-password />
            </el-form-item>
          </el-form>
        </div>

        <div class="items-toolbar">
          <h3>本馆应用</h3>
          <div class="toolbar-actions">
            <button type="button" class="action-btn ghost sm" @click="addItem">＋ 添加链接</button>
            <button type="button" class="action-btn indigo sm" @click="triggerUpload">上传 HTML</button>
          </div>
        </div>

        <div v-if="!(currentHall.items || []).length" class="empty-hint">
          还没有应用。可添加外链，或上传 HTML 页面供学生在本馆打开。
        </div>

        <article v-for="(item, idx) in currentHall.items || []" :key="item.id || idx" class="item-card">
          <div class="item-row">
            <el-input v-model="item.title" placeholder="应用名称" class="grow" />
            <el-select v-model="item.type" style="width: 110px">
              <el-option label="链接" value="link" />
              <el-option label="嵌入" value="embed" />
              <el-option label="HTML" value="html" />
            </el-select>
            <el-switch v-model="item.enabled" inline-prompt active-text="开" inactive-text="关" />
            <button type="button" class="action-btn ghost xs danger" @click="removeItem(idx)">删除</button>
          </div>
          <el-input v-model="item.desc" placeholder="一句话介绍（可选）" class="mt8" />
          <el-input v-model="item.url" placeholder="https://… 或 /lessons/generated/ai-lab/…" class="mt8" />
          <div v-if="item.url" class="item-preview-actions">
            <a :href="item.url" target="_blank" rel="noopener" class="preview-link">打开预览</a>
          </div>
        </article>

        <input ref="fileInput" type="file" accept=".html,text/html" class="hidden-file" @change="onUpload" />
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { AI_HALLS, AI_ASSISTANTS } from '../../constants/aiLab'
import {
  teacherGetAiLabConfig,
  teacherSaveAiLabConfig,
  teacherUploadAiLabHtml
} from '../../api'

const hallsMeta = AI_HALLS
const assistantsMeta = AI_ASSISTANTS
const loading = ref(false)
const saving = ref(false)
const activeHall = ref('agent')
const fileInput = ref(null)
const config = reactive({ halls: {} })

const ensureAssistants = (list) => {
  const byId = {}
  ;(Array.isArray(list) ? list : []).forEach((a) => {
    if (a?.id) byId[a.id] = a.enabled !== false
  })
  return assistantsMeta.map((bot) => ({
    id: bot.id,
    enabled: byId[bot.id] !== false
  }))
}

const ensureHall = (id) => {
  if (!config.halls[id]) {
    const meta = hallsMeta.find((h) => h.id === id)
    config.halls[id] = {
      name: meta?.name || id,
      desc: meta?.desc || '',
      enabled: true,
      items: [],
      embedUrl: '',
      botId: '',
      token: '',
      assistants: id === 'agent' ? ensureAssistants([]) : undefined
    }
  }
  if (!Array.isArray(config.halls[id].items)) config.halls[id].items = []
  if (id === 'agent' && !Array.isArray(config.halls[id].assistants)) {
    config.halls[id].assistants = ensureAssistants([])
  }
  if (typeof config.halls[id].enabled !== 'boolean') {
    config.halls[id].enabled = config.halls[id].enabled !== false
  }
  return config.halls[id]
}

const currentHall = computed(() => ensureHall(activeHall.value))
const activeMeta = computed(() => hallsMeta.find((h) => h.id === activeHall.value))
const enabledCount = computed(() =>
  (currentHall.value.items || []).filter((i) => i.enabled !== false).length
)

const assistantEnabled = (id) => {
  const hall = ensureHall('agent')
  const row = (hall.assistants || []).find((a) => a.id === id)
  return row ? row.enabled !== false : true
}

const setAssistantEnabled = (id, on) => {
  const hall = ensureHall('agent')
  const list = ensureAssistants(hall.assistants)
  const row = list.find((a) => a.id === id)
  if (row) row.enabled = !!on
  hall.assistants = list
}

const applyConfig = (data) => {
  const halls = data?.halls || {}
  hallsMeta.forEach((h) => {
    const src = halls[h.id] || {}
    config.halls[h.id] = {
      name: src.name || h.name,
      desc: src.desc || h.desc,
      enabled: src.enabled !== false,
      items: Array.isArray(src.items) ? src.items.map((it) => ({
        id: it.id,
        title: it.title || '',
        desc: it.desc || '',
        type: it.type || 'link',
        url: it.url || '',
        enabled: it.enabled !== false
      })) : [],
      embedUrl: src.embedUrl || '',
      botId: src.botId || '',
      token: src.token || '',
      assistants: h.id === 'agent' ? ensureAssistants(src.assistants) : undefined
    }
  })
}

const load = async () => {
  loading.value = true
  try {
    const res = await teacherGetAiLabConfig()
    applyConfig(res.data || {})
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
    applyConfig({})
  } finally {
    loading.value = false
  }
}

const saveAll = async () => {
  saving.value = true
  try {
    const res = await teacherSaveAiLabConfig({ halls: JSON.parse(JSON.stringify(config.halls)) })
    applyConfig(res.data || {})
    ElMessage.success(res.message || '已保存')
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const addItem = () => {
  ensureHall(activeHall.value).items.push({
    id: `item-${Date.now()}`,
    title: '',
    desc: '',
    type: 'link',
    url: '',
    enabled: true
  })
}

const removeItem = (idx) => {
  ensureHall(activeHall.value).items.splice(idx, 1)
}

const triggerUpload = () => fileInput.value?.click()

const onUpload = async (ev) => {
  const file = ev?.target?.files?.[0]
  if (fileInput.value) fileInput.value.value = ''
  if (!file) return
  if (!String(file.name || '').toLowerCase().endsWith('.html')) {
    ElMessage.warning('请上传 .html 文件')
    return
  }
  loading.value = true
  try {
    const title = file.name.replace(/\.html$/i, '')
    const res = await teacherUploadAiLabHtml(activeHall.value, file, title)
    applyConfig(res.data?.config || {})
    ElMessage.success(res.message || '已上传')
  } catch (e) {
    ElMessage.error(e?.message || '上传失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.ai-lab-teacher {
  min-height: calc(100vh - 56px);
  background: #f5f3ff;
}
.lab-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 20px 16px;
  background: linear-gradient(135deg, #5b4fc7 0%, #7c3aed 45%, #9333ea 100%);
  color: #fff;
  box-shadow: 0 4px 20px rgba(91, 79, 199, 0.35);
}
.header-main h1 {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}
.title-icon { font-size: 20px; }
.header-main p {
  margin: 0;
  font-size: 12px;
  opacity: 0.9;
  line-height: 1.5;
  max-width: 720px;
}
.lab-body { padding: 16px 20px 28px; }
.hall-tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}
.hall-tab {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid #e9e5ff;
  background: #fff;
  border-radius: 12px;
  padding: 10px 12px;
  cursor: pointer;
  font-weight: 600;
  color: #4338ca;
  box-shadow: 0 2px 10px rgba(91, 79, 199, 0.05);
}
.hall-tab.active {
  border-color: #7c3aed;
  background: linear-gradient(135deg, #4338ca, #6366f1);
  color: #fff;
}
.hall-tab.closed { opacity: 0.72; }
.hall-tab.closed:not(.active) { background: #f8fafc; color: #64748b; }
.tab-text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  min-width: 0;
}
.tab-text b { font-size: 14px; font-weight: 700; }
.tab-text em {
  font-style: normal;
  font-size: 11px;
  opacity: 0.85;
  font-weight: 600;
}
.hall-tab.active .tab-text em { opacity: 0.9; }
.tab-cover {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}
.head-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}
.open-switch {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  font-weight: 700;
  color: #4338ca;
}
.assistant-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  margin-top: 8px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e9e5ff;
}
.assistant-cover {
  width: 56px;
  height: 40px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}
.assistant-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.assistant-meta b { font-size: 14px; color: #312e81; }
.assistant-meta span { font-size: 12px; color: #64748b; line-height: 1.4; }
.panel {
  background: #fff;
  border: 1px solid #e9e5ff;
  border-radius: 14px;
  padding: 16px 18px 18px;
  box-shadow: 0 2px 12px rgba(91, 79, 199, 0.06);
}
.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.panel-head h2 { margin: 0 0 4px; font-size: 16px; color: #312e81; }
.panel-head p { margin: 0; font-size: 13px; color: #64748b; }
.meta-form { max-width: 720px; }
.agent-box {
  margin: 8px 0 16px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #faf8ff;
  border: 1px solid #ede9fe;
}
.agent-box h3 { margin: 0 0 6px; font-size: 14px; color: #312e81; }
.hint { margin: 0 0 10px; font-size: 12px; color: #64748b; }
.items-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin: 8px 0 12px;
}
.items-toolbar h3 { margin: 0; font-size: 15px; color: #312e81; }
.toolbar-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.empty-hint {
  padding: 18px;
  border-radius: 12px;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
}
.item-card {
  border: 1px solid #e9e5ff;
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 10px;
  background: #faf8ff;
}
.item-row {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.grow { flex: 1; min-width: 160px; }
.mt8 { margin-top: 8px; }
.item-preview-actions { margin-top: 8px; }
.preview-link {
  font-size: 12px;
  color: #5b4fc7;
  text-decoration: none;
}
.preview-link:hover { text-decoration: underline; }
.hidden-file { display: none; }
.action-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  white-space: nowrap;
}
.action-btn.sm { padding: 6px 12px; font-size: 12px; }
.action-btn.xs { padding: 5px 10px; font-size: 12px; }
.action-btn:disabled { opacity: 0.55; cursor: not-allowed; }
.action-btn.indigo { background: linear-gradient(135deg, #4338ca, #6366f1); }
.action-btn.ghost {
  background: #fff;
  color: #5b4fc7;
  border: 1px solid #c4b5fd;
}
.action-btn.danger { color: #b91c1c; border-color: #fecaca; }
@media (max-width: 900px) {
  .hall-tabs { grid-template-columns: 1fr 1fr; }
  .lab-header { flex-direction: column; }
}
</style>
