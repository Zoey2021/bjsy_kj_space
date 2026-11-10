/**
 * 教师端信息架构
 *
 * 上课主线（一级）
 *   课程地图 / 班级学情 / 后台管理
 * 备课中心
 *   课时配置 / 分层脚手架
 * AI实验室
 *   与学生端四馆对齐，上传应用内容
 * 激励乐园
 *   课堂积分 / 乐园游戏 / 科技世界
 * 评价中心
 *   智能评价
 *
 * 仅调整前端导航；AI 实验室配置用独立表，不改学生学习记录。
 */

export const TEACHER_MODULES = [
  {
    key: 'course-map',
    path: '/teacher/course-map',
    title: '课程地图',
    icon: 'MapLocation',
    desc: '选教材、进课时、看电子教材'
  },
  {
    key: 'dashboard',
    path: '/teacher/dashboard',
    title: '班级学情',
    icon: 'DataAnalysis',
    desc: '班级码、完成情况、催交与乐园授权'
  },
  {
    key: 'settings',
    path: '/teacher/settings',
    title: '后台管理',
    icon: 'Setting',
    desc: '教师、班级、学生账号与任课'
  },
  {
    key: 'prepare',
    title: '备课中心',
    icon: 'Notebook',
    desc: '配置分层任务与课时内容',
    children: [
      {
        key: 'activity-editor',
        path: '/teacher/activity-editor',
        title: '课时配置',
        icon: 'EditPen',
        desc: '选课时、上传 HTML、拆探究并发布'
      },
      {
        key: 'scaffold-workbench',
        path: '/teacher/scaffold-workbench',
        title: '分层脚手架',
        icon: 'Notebook',
        desc: '按课时配置 A/B/C 三档任务与提示'
      }
    ]
  },
  {
    key: 'ai-lab',
    path: '/teacher/ai-lab',
    title: 'AI实验室',
    icon: 'Cpu',
    desc: '配置体验馆 / 训练馆 / 创造营 / 小助手'
  },
  {
    key: 'engage',
    title: '激励乐园',
    icon: 'Trophy',
    desc: '课堂积分、乐园游戏与科技世界',
    children: [
      {
        key: 'points',
        path: '/teacher/points',
        title: '课堂积分',
        icon: 'Trophy',
        desc: '积分排行、规则与兑换开关'
      },
      {
        key: 'park-settings',
        path: '/teacher/park-settings',
        title: '乐园游戏',
        icon: 'Orange',
        desc: '全校开放哪些游戏及参数'
      },
      {
        key: 'tech-world',
        path: '/teacher/tech-world',
        title: '科技世界',
        icon: 'Monitor',
        desc: '预览学生商城与乐园（不扣分）'
      }
    ]
  },
  {
    key: 'evaluate',
    title: '评价中心',
    icon: 'MagicStick',
    desc: '智能评价与报告',
    children: [
      {
        key: 'ai-evaluation',
        path: '/teacher/ai-evaluation',
        title: '智能评价',
        icon: 'MagicStick',
        desc: '学生过程档案与综合等级'
      }
    ]
  }
]

/** 扁平化（含子项），供路由高亮、科创账号过滤等 */
export function flattenTeacherModules(modules = TEACHER_MODULES) {
  const out = []
  modules.forEach((m) => {
    if (m.children && m.children.length) {
      m.children.forEach((c) => out.push(c))
    } else if (m.path) {
      out.push(m)
    }
  })
  return out
}
