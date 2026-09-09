/** 教师端六大核心板块导航配置 */
export const TEACHER_MODULES = [
  {
    key: 'course-map',
    path: '/teacher/course-map',
    title: '课程地图',
    icon: 'MapLocation',
    desc: '目录导航与电子教材'
  },
  {
    key: 'dashboard',
    path: '/teacher/dashboard',
    title: '班级学情',
    icon: 'DataAnalysis',
    desc: '实时掌握全班学习情况'
  },
  {
    key: 'activity-editor',
    path: '/teacher/activity-editor',
    title: '活动编辑',
    icon: 'EditPen',
    desc: '编辑课时介绍、资源与探究活动'
  },
  {
    key: 'ai-evaluation',
    path: '/teacher/ai-evaluation',
    title: '智能评价',
    icon: 'MagicStick',
    desc: '自动批改、评级与报告'
  },
  {
    key: 'points',
    path: '/teacher/points',
    title: '课堂积分',
    icon: 'Trophy',
    desc: '积分排行与奖励兑换'
  },
  {
    key: 'settings',
    path: '/teacher/settings',
    title: '后台管理',
    icon: 'Setting',
    desc: '班级、AI与数据配置'
  }
]
