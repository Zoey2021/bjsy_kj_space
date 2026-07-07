/** 校本课程 · 融合探究课程（双入口） */
export const INTEGRATED_INQUIRY_COURSE = {
  cardTitle: '融合探究课程',
  slots: [
    {
      key: 'pbl',
      label: '项目化学习',
      coverUrl: '/course-covers/pbl-project-learning.png',
      studentRoute: '/student/pbl',
      teacherRoute: '/teacher/pbl'
    },
    {
      key: 'interdisciplinary',
      label: '跨学科学习',
      coverUrl: '/course-covers/interdisciplinary-learning.png',
      studentRoute: '/student/interdisciplinary',
      teacherRoute: '/teacher/interdisciplinary'
    }
  ]
}

/** 校本课程 · 项目化学习模块 */
export const PBL_MODULE = {
  key: 'pbl',
  seriesTitle: '项目化学习',
  cardTitle: '项目化学习',
  coverUrl: '/course-covers/pbl-project-learning.png',
  studentRoute: '/student/pbl',
  teacherRoute: '/teacher/pbl'
}

export const PBL_PHASES = [
  {
    key: 'launch',
    title: '入项启动',
    subtitle: '了解项目背景，明确任务与分工',
    icon: '🚀'
  },
  {
    key: 'inquiry',
    title: '探究与实践过程',
    subtitle: '调研、实验、协作完成项目任务',
    icon: '🔍'
  },
  {
    key: 'output',
    title: '成果产出与展示',
    subtitle: '整理作品，准备汇报与展示',
    icon: '🎨'
  },
  {
    key: 'evaluation',
    title: '多元评价',
    subtitle: '自评、互评与教师评价',
    icon: '⭐'
  },
  {
    key: 'review',
    title: '复盘总结与拓展延伸',
    subtitle: '反思过程，规划后续学习',
    icon: '📝'
  }
]

export const PBL_STORAGE_KEY = 'learnspace_pbl_project'

export function createEmptyPblProject() {
  const phases = {}
  for (const p of PBL_PHASES) {
    phases[p.key] = { title: p.title, content: '' }
  }
  return {
    theme: '',
    drivingQuestion: '',
    interdisciplinary: '',
    period: '',
    venue: 'campus',
    venueDetail: '',
    tasks: [{ name: '', owner: '', deadline: '' }],
    outcomes: '',
    rubric: '',
    resources: {
      worksheet: '',
      researchLog: '',
      tools: '',
      offsitePrep: ''
    },
    recruitment: {
      published: false,
      maxParticipants: 30,
      description: '',
      enrolledCount: 0
    },
    phases,
    updatedAt: null
  }
}
