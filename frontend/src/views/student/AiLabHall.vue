<template>
  <div class="hall-page" v-loading="loading">
    <header class="bar">
      <button type="button" class="back" @click="onBack">← {{ activeAssistant || activeApp ? '返回本馆' : '返回实验室' }}</button>
      <div v-if="!activeAssistant && !activeApp">
        <h1>{{ displayName }}</h1>
        <p>{{ displayDesc }}</p>
      </div>
      <div v-else-if="activeAssistant">
        <h1>{{ activeAssistant.name }}</h1>
        <p>{{ activeAssistant.tagline || activeAssistant.desc }}</p>
      </div>
      <div v-else>
        <h1>{{ activeApp.title }}</h1>
        <p>{{ activeApp.desc || '应用预览' }}</p>
      </div>
    </header>

    <!-- 智能体：优先打开同站聊天页（小派老师） -->
    <section v-if="activeAssistant && activeAssistant.chatPage" class="stage chat-page-stage">
      <iframe
        class="embed"
        :src="activeAssistant.chatPage"
        :title="activeAssistant.name"
        allow="microphone; clipboard-write; camera"
      />
    </section>

    <!-- 智能体对话界面（无 chatPage 时的兜底） -->
    <section v-else-if="activeAssistant" class="chat-shell">
      <aside class="chat-side">
        <img :src="activeAssistant.cover" :alt="activeAssistant.name" class="side-cover" />
        <h2>{{ activeAssistant.name }}</h2>
        <p>{{ activeAssistant.desc }}</p>
      </aside>
      <div class="chat-main">
        <div class="chat-top">
          <span class="dot" />
          <strong>{{ activeAssistant.name }}</strong>
          <span class="status">{{ chatReady ? '在线' : '界面预览' }}</span>
        </div>

        <iframe
          v-if="assistantEmbed"
          class="chat-embed"
          :src="assistantEmbed"
          :title="activeAssistant.name"
          allow="microphone; clipboard-write"
          referrerpolicy="no-referrer-when-downgrade"
        />

        <template v-else>
          <div ref="msgBox" class="chat-messages">
            <div class="bubble bot">
              <span class="who">{{ activeAssistant.name }}</span>
              <p>你好！我是{{ activeAssistant.name }}。请稍候配置对话服务。</p>
            </div>
            <div v-for="(m, i) in messages" :key="i" class="bubble" :class="m.role">
              <span class="who">{{ m.role === 'user' ? '我' : activeAssistant.name }}</span>
              <p>{{ m.text }}</p>
            </div>
          </div>
          <form class="chat-input" @submit.prevent="sendPreview">
            <input
              v-model="draft"
              type="text"
              maxlength="200"
              placeholder="输入消息"
              :disabled="sending"
            />
            <button type="submit" class="send-btn" :disabled="!draft.trim() || sending">发送</button>
          </form>
        </template>
      </div>
    </section>

    <!-- 普通应用打开 -->
    <section v-else-if="activeApp" class="stage">
      <iframe
        v-if="activeApp.type === 'html' || activeApp.type === 'embed'"
        class="embed"
        :src="activeApp.url"
        :title="activeApp.title"
        allow="microphone; clipboard-write; camera"
        referrerpolicy="no-referrer-when-downgrade"
      />
      <div v-else class="panel soft">
        <p>将在新窗口打开该应用。</p>
        <a :href="activeApp.url" target="_blank" rel="noopener" class="open-btn">打开 {{ activeApp.title }}</a>
      </div>
    </section>

    <!-- 本馆被教师关闭 -->
    <section v-else-if="!hallOpen" class="panel">
      <h2>本馆暂未开放</h2>
      <p>老师还没有开放「{{ displayName }}」，请先学习其他内容，或听老师安排后再来。</p>
    </section>

    <!-- 小助手：智能体卡片墙 -->
    <section v-else-if="hallId === 'agent'" class="agent-hall">
      <div v-if="assistants.length" class="assistant-grid">
        <button
          v-for="bot in assistants"
          :key="bot.id"
          type="button"
          class="assistant-card"
          @click="openAssistant(bot)"
        >
          <span class="cover-wrap">
            <img :src="bot.cover" :alt="bot.name" loading="lazy" decoding="async" />
          </span>
          <span class="card-meta">
            <b>{{ bot.name }}</b>
            <em>{{ bot.tagline || '点击开始对话' }}</em>
          </span>
        </button>
      </div>
      <div v-else class="panel soft" style="margin:0">
        <h2>暂无开放的小助手</h2>
        <p class="muted" style="margin:0">老师关闭了当前智能体，请稍后再来或使用其他应用。</p>
      </div>

      <div v-if="enabledItems.length" class="extra-apps">
        <h3>老师补充的应用</h3>
        <div class="app-grid">
          <button
            v-for="item in enabledItems"
            :key="item.id"
            type="button"
            class="app-card"
            @click="openItem(item)"
          >
            <b>{{ item.title }}</b>
            <span>{{ item.desc || '点击打开' }}</span>
          </button>
        </div>
      </div>
    </section>

    <!-- 其他馆 -->
    <section v-else-if="enabledItems.length" class="panel soft">
      <h2>选择一个应用开始</h2>
      <div class="app-grid">
        <button
          v-for="item in enabledItems"
          :key="item.id"
          type="button"
          class="app-card"
          @click="openItem(item)"
        >
          <b>{{ item.title }}</b>
          <span>{{ item.desc || '点击打开' }}</span>
        </button>
      </div>
    </section>
    <section v-else class="panel">
      <h2>{{ comingTitle }}</h2>
      <p>{{ comingBody }}</p>
      <ul>
        <li v-for="item in comingItems" :key="item">{{ item }}</li>
      </ul>
      <p class="muted">老师上传内容后，这里会自动出现可打开的应用。</p>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AI_HALLS, AI_ASSISTANTS, AI_AGENT, aiAgentReady } from '../../constants/aiLab'
import { getAiLabConfig } from '../../api'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const hallConfig = ref(null)
const activeApp = ref(null)
const activeAssistant = ref(null)
const draft = ref('')
const sending = ref(false)
const messages = ref([])
const msgBox = ref(null)
const sdkError = ref('')
let client = null

const hallId = computed(() => String(route.params.hall || ''))
const staticHall = computed(() => AI_HALLS.find((h) => h.id === hallId.value))
const hallOpen = computed(() => hallConfig.value == null || hallConfig.value.enabled !== false)

const assistants = computed(() => {
  const flags = {}
  ;(hallConfig.value?.assistants || []).forEach((a) => {
    if (a?.id) flags[a.id] = a.enabled !== false
  })
  const hasFlags = Object.keys(flags).length > 0
  return AI_ASSISTANTS.filter((bot) => {
    if (!hasFlags) return true
    return flags[bot.id] !== false
  })
})

const displayName = computed(() => hallConfig.value?.name || staticHall.value?.name || '人工智能实验室')
const displayDesc = computed(() => hallConfig.value?.desc || staticHall.value?.desc || '')

const enabledItems = computed(() =>
  (hallConfig.value?.items || []).filter((i) => i && i.enabled !== false && i.url)
)

const assistantEmbed = computed(() => {
  const a = activeAssistant.value
  if (!a) return ''
  return a.embedUrl || ''
})

const chatReady = computed(() => aiAgentReady(activeAssistant.value || {}))

const comingTitle = computed(() => {
  if (hallId.value === 'experience') return '体验馆准备中'
  if (hallId.value === 'train') return '训练馆准备中'
  if (hallId.value === 'create') return '创造营准备中'
  return '即将开放'
})

const comingBody = computed(() => {
  if (hallId.value === 'experience') return '这里将放入已经做好的小应用，例如：'
  if (hallId.value === 'train') return '这里将带你完成「数据 → 训练 → 模型」的小实验，例如：'
  if (hallId.value === 'create') return '这里将提供用 AI 创作的工具，例如：'
  return ''
})

const comingItems = computed(() => {
  if (hallId.value === 'experience') return ['人脸识别', '车牌识别', '你画我猜']
  if (hallId.value === 'train') return ['图像识别训练', '机器学习小实验']
  if (hallId.value === 'create') return ['AI 生成文字', 'AI 生成图片']
  return []
})

const onBack = () => {
  if (activeAssistant.value || activeApp.value) {
    destroyClient()
    activeAssistant.value = null
    activeApp.value = null
    messages.value = []
    draft.value = ''
    return
  }
  router.push('/student/ai-lab')
}

const openAssistant = (bot) => {
  activeApp.value = null
  activeAssistant.value = bot
  messages.value = []
  draft.value = ''
  nextTick(() => initAgentSdk(bot))
}

const openItem = (item) => {
  if (!item?.url) return
  if (item.type === 'link') {
    window.open(item.url, '_blank', 'noopener')
    return
  }
  activeAssistant.value = null
  activeApp.value = item
}

const sendPreview = async () => {
  const text = draft.value.trim()
  if (!text) return
  messages.value.push({ role: 'user', text })
  draft.value = ''
  sending.value = true
  await nextTick()
  if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
  await new Promise((r) => setTimeout(r, 450))
  messages.value.push({
    role: 'bot',
    text: '（界面预览）收到啦。扣子接口接通后，这里会返回正式回答。'
  })
  sending.value = false
  await nextTick()
  if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
}

function loadScript(src) {
  return new Promise((resolve, reject) => {
    if (document.querySelector('script[data-ai-lab-sdk="1"]')) {
      resolve()
      return
    }
    const el = document.createElement('script')
    el.src = src
    el.async = true
    el.dataset.aiLabSdk = '1'
    el.onload = () => resolve()
    el.onerror = () => reject(new Error('扣子 SDK 加载失败'))
    document.head.appendChild(el)
  })
}

const destroyClient = () => {
  try { client?.destroy?.() } catch { /* ignore */ }
  client = null
}

const initAgentSdk = async (bot) => {
  destroyClient()
  sdkError.value = ''
  if (!bot || bot.embedUrl || !aiAgentReady(bot)) return
  try {
    await loadScript(AI_AGENT.sdkUrl)
    const CozeWebSDK = window.CozeWebSDK
    if (!CozeWebSDK?.WebChatClient) {
      sdkError.value = '未找到 CozeWebSDK'
      return
    }
    client = new CozeWebSDK.WebChatClient({
      config: { bot_id: bot.botId },
      componentProps: { title: bot.name },
      auth: {
        type: 'token',
        token: bot.token,
        onRefreshToken: async () => bot.token
      }
    })
  } catch (e) {
    sdkError.value = e?.message || '小助手初始化失败'
  }
}

const loadConfig = async () => {
  loading.value = true
  activeApp.value = null
  activeAssistant.value = null
  try {
    const res = await getAiLabConfig()
    const halls = res.data?.halls || {}
    hallConfig.value = halls[hallId.value] || null
  } catch {
    hallConfig.value = null
  } finally {
    loading.value = false
  }
}

watch(hallId, loadConfig)
onMounted(loadConfig)
onBeforeUnmount(destroyClient)
</script>

<style scoped>
.hall-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow: hidden;
}
.hall-page:has(.chat-page-stage) {
  gap: 8px;
}
.bar {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.back {
  align-self: flex-start;
  border: 0;
  background: transparent;
  color: #0e7490;
  font-weight: 700;
  cursor: pointer;
  padding: 0;
  font-size: 13px;
}
.bar h1 {
  margin: 0 0 4px;
  font-size: 22px;
  color: #0f172a;
}
.bar p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
}

.agent-hall {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.assistant-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 360px));
  gap: 16px;
  justify-content: start;
}
.assistant-card {
  border: 0;
  padding: 0;
  border-radius: 18px;
  overflow: hidden;
  cursor: pointer;
  background: #fff;
  box-shadow: 0 12px 28px rgba(91, 79, 199, 0.14);
  text-align: left;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
.assistant-card:hover {
  transform: translateY(-3px) scale(1.01);
  box-shadow: 0 18px 36px rgba(91, 79, 199, 0.2);
}
.cover-wrap {
  display: block;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: linear-gradient(135deg, #ddd6fe, #c4b5fd);
}
.cover-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.card-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px 14px;
}
.card-meta b {
  font-size: 16px;
  color: #312e81;
}
.card-meta em {
  font-style: normal;
  font-size: 13px;
  color: #64748b;
}

.chat-shell {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 14px;
}
.chat-side {
  border-radius: 18px;
  background: linear-gradient(180deg, #ede9fe 0%, #f5f3ff 100%);
  border: 1px solid #ddd6fe;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.side-cover {
  width: 100%;
  border-radius: 14px;
  display: block;
  aspect-ratio: 16 / 10;
  object-fit: cover;
}
.chat-side h2 {
  margin: 0;
  font-size: 18px;
  color: #312e81;
}
.chat-side p {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
}
.chat-main {
  min-height: 0;
  border-radius: 18px;
  background: #fff;
  border: 1px solid #e9e5ff;
  box-shadow: 0 10px 28px rgba(49, 46, 129, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.chat-top {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid #ede9fe;
  background: linear-gradient(90deg, #faf8ff, #fff);
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
}
.chat-top strong { color: #312e81; }
.status {
  margin-left: auto;
  font-size: 12px;
  color: #64748b;
}
.chat-embed {
  flex: 1;
  width: 100%;
  border: 0;
  min-height: 480px;
}
.chat-messages {
  flex: 1;
  min-height: 320px;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background:
    radial-gradient(500px 200px at 10% 0%, rgba(196, 181, 253, 0.18), transparent 60%),
    #fafafe;
}
.bubble {
  max-width: 78%;
  padding: 10px 12px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #e9e5ff;
}
.bubble.user {
  align-self: flex-end;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-color: transparent;
  color: #fff;
}
.bubble.bot { align-self: flex-start; }
.bubble .who {
  display: block;
  font-size: 11px;
  opacity: 0.75;
  margin-bottom: 4px;
}
.bubble p {
  margin: 0;
  font-size: 14px;
  line-height: 1.55;
}
.chat-input {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid #ede9fe;
  background: #fff;
}
.chat-input input {
  flex: 1;
  border: 1px solid #ddd6fe;
  border-radius: 999px;
  padding: 10px 14px;
  font-size: 14px;
  outline: none;
}
.chat-input input:focus { border-color: #8b5cf6; }
.send-btn {
  border: 0;
  border-radius: 999px;
  padding: 0 18px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #4338ca, #7c3aed);
  cursor: pointer;
}
.send-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.chat-tip {
  margin: 0;
  padding: 0 14px 12px;
  font-size: 12px;
  color: #94a3b8;
}

.panel, .stage {
  flex: 1;
  min-height: 0;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
}
.panel { padding: 24px; }
.panel.soft { padding: 18px 20px; }
.panel h2 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #0f172a;
}
.panel p, .panel li {
  color: #475569;
  line-height: 1.7;
  font-size: 14px;
}
.panel ul {
  margin: 10px 0 14px;
  padding-left: 1.2rem;
}
.muted {
  color: #64748b !important;
  font-size: 13px !important;
}
.extra-apps h3 {
  margin: 0 0 10px;
  font-size: 15px;
  color: #312e81;
}
.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}
.app-card {
  text-align: left;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  border-radius: 14px;
  padding: 16px;
  cursor: pointer;
}
.app-card:hover {
  border-color: #6366f1;
  background: #eef2ff;
}
.app-card b {
  display: block;
  margin-bottom: 6px;
  color: #312e81;
  font-size: 15px;
}
.app-card span {
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}
.open-btn {
  display: inline-block;
  margin-top: 10px;
  padding: 8px 14px;
  border-radius: 999px;
  background: #4f46e5;
  color: #fff;
  text-decoration: none;
  font-weight: 600;
  font-size: 13px;
}
.embed {
  width: 100%;
  height: 100%;
  border: 0;
  display: block;
}
.chat-page-stage {
  flex: 1 1 auto;
  min-height: 0;
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border-radius: 16px;
}
.chat-page-stage .embed {
  flex: 1 1 auto;
  width: 100%;
  height: 100%;
  min-height: 0;
  border: 0;
  border-radius: 16px;
  background: #f7f9ff;
}

@media (max-width: 900px) {
  .chat-shell { grid-template-columns: 1fr; }
  .chat-side { display: none; }
  .assistant-grid { grid-template-columns: 1fr; }
}
</style>
