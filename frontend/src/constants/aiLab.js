/**
 * 人工智能实验室
 * 四个馆：体验馆 / 训练馆 / 创造营 / 小助手
 *
 * 小助手：预置智能体卡片；扣子 botId/token/embedUrl 稍后接入。
 */
export const AI_LAB = {
  title: '人工智能实验室',
  subtitle: '点进一个馆，开始探索人工智能'
}

export const AI_HALLS = [
  {
    id: 'experience',
    path: '/student/ai-lab/experience',
    name: '体验馆',
    cover: '/ai-lab/experience.jpg',
    desc: '玩现成的 AI 小应用，感受人工智能能做什么。',
    features: ['人脸识别', '车牌识别', '你画我猜']
  },
  {
    id: 'train',
    path: '/student/ai-lab/train',
    name: '训练馆',
    cover: '/ai-lab/train.jpg',
    desc: '上传数据、标注并训练，做出自己的小模型。',
    features: ['图像识别训练', '机器学习小实验', '试一试你的模型']
  },
  {
    id: 'create',
    path: '/student/ai-lab/create',
    name: '创造营',
    cover: '/ai-lab/create.jpg',
    desc: '用 AI 生成文字和图片，把想法变成作品。',
    features: ['AI 生成文字', 'AI 生成图片']
  },
  {
    id: 'agent',
    path: '/student/ai-lab/agent',
    name: '小助手',
    cover: '/ai-lab/agent.jpg',
    desc: '和学习小助手对话，遇到问题随时请教。',
    features: ['课程答疑', '学习陪伴', '智能对话']
  }
]

/**
 * 小助手馆 · 预置智能体（学生端展示）
 * 扣子字段先留空，下一步再接接口。
 */
export const AI_ASSISTANTS = [
  {
    id: 'suanfa',
    name: '算法小助手',
    cover: '/ai-lab/suanfa-assistant.jpg',
    tagline: '和小派老师聊聊算法与 Python',
    desc: '点开后进入小派老师对话页：可文字提问，也可上传算法流程图。',
    // 同站聊天页（经后端代理调用扣子，Token 不暴露给前端）
    chatPage: '/ai-lab/xiaopai-chat.html',
    botId: '',
    token: '',
    embedUrl: ''
  }
]

/** 兼容旧配置：默认指向第一个助手 */
export const AI_AGENT = {
  botId: '',
  token: '',
  sdkUrl: 'https://lf-cdn.coze.cn/obj/unpkg/flow-platform/chat-app-sdk/1.2.0-beta.6/libs/cn/index.js',
  embedUrl: ''
}

export function aiAgentReady(agent = AI_AGENT) {
  return Boolean(agent?.embedUrl || (agent?.botId && agent?.token))
}
