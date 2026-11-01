/**
 * 学生端 · 科技空间
 * 课程地图之外的科技探索入口（可继续扩展）。
 */
export const TECH_SPACE = {
  title: '科技空间',
  subtitle: '在这里探索项目创作与跨学科科技活动'
}

export const TECH_SPACE_ENTRIES = [
  {
    id: 'pbl',
    name: '项目化学习',
    desc: '用 Python 与 AI 设计小游戏，完成自己的作品。',
    path: '/student/pbl',
    tone: 'indigo'
  },
  {
    id: 'create-lab',
    name: '创造营快线',
    desc: '跳转到人工智能实验室的创造营，用 AI 生成文字和图片。',
    path: '/student/ai-lab/create',
    tone: 'teal'
  },
  {
    id: 'agent',
    name: '学习小助手',
    desc: '有疑问时来对话，获得学习陪伴与答疑。',
    path: '/student/ai-lab/agent',
    tone: 'amber'
  }
]
