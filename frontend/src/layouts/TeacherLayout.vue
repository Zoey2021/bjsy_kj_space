<template>
  <el-container class="layout">
    <el-header class="header">
      <span class="logo">教师工作台</span>
      <span class="header-sub">杭州市滨江实验小学信息科技学习平台</span>
      <span class="user">
        {{ realName }}
        <el-button link class="logout" @click="logout">退出</el-button>
      </span>
    </el-header>

    <el-container class="body">
      <el-aside :width="collapsed ? '64px' : '220px'" class="aside">
        <el-menu
          :default-active="activeMenu"
          :default-openeds="openedMenus"
          :collapse="collapsed"
          router
          class="side-menu"
        >
          <template v-for="m in visibleModules" :key="m.key">
            <el-sub-menu v-if="m.children?.length" :index="m.key">
              <template #title>
                <el-icon><component :is="iconMap[m.icon]" /></el-icon>
                <span>{{ m.title }}</span>
              </template>
              <el-menu-item
                v-for="c in m.children"
                :key="c.key"
                :index="c.path"
                :title="c.desc"
              >
                <el-icon><component :is="iconMap[c.icon]" /></el-icon>
                <span>{{ c.title }}</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item
              v-else
              :index="m.path"
              :title="m.desc"
            >
              <el-icon><component :is="iconMap[m.icon]" /></el-icon>
              <span>{{ m.title }}</span>
            </el-menu-item>
          </template>
        </el-menu>
        <el-button class="collapse-btn" text @click="collapsed = !collapsed">
          {{ collapsed ? '展开' : '收起' }}
        </el-button>
      </el-aside>

      <el-main :class="mainClass"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  MapLocation,
  EditPen,
  DataAnalysis,
  MagicStick,
  Trophy,
  Setting,
  Notebook,
  Orange,
  User,
  Monitor,
  Cpu
} from '@element-plus/icons-vue'
import { TEACHER_MODULES, flattenTeacherModules } from '../constants/teacherModules'
import { accountScope, SCOPE_KECHUANG } from '../utils/schoolCourseAccess'

const route = useRoute()
const router = useRouter()
const realName = localStorage.getItem('realName')
const collapsed = ref(false)

const iconMap = {
  MapLocation,
  EditPen,
  DataAnalysis,
  MagicStick,
  Trophy,
  Setting,
  Notebook,
  Orange,
  User,
  Monitor,
  Cpu
}

const visibleModules = computed(() => (
  accountScope() === SCOPE_KECHUANG
    ? TEACHER_MODULES.filter((m) => m.key === 'course-map')
    : TEACHER_MODULES
))

const flatModules = computed(() => flattenTeacherModules(visibleModules.value))

const openedMenus = computed(() => {
  const path = route.path
  const groups = []
  TEACHER_MODULES.forEach((m) => {
    if (!m.children?.length) return
    if (m.children.some((c) => path === c.path || path.startsWith(c.path + '/'))) {
      groups.push(m.key)
    }
  })
  return groups.length ? groups : ['prepare', 'engage', 'evaluate']
})

const activeMenu = computed(() => {
  if (route.path.startsWith('/teacher/pretest')) return '/teacher/course-map'
  const hit = flatModules.value.find((m) => m.path && route.path.startsWith(m.path))
  return hit ? hit.path : route.path
})

const mainClass = computed(() => ({
  'main-flush': route.path.includes('/activity-editor/grade/') || route.path.includes('/course-map/grade/') || /^\/teacher\/pretest\/g[46]$/.test(route.path) || route.path.startsWith('/teacher/scaffold-workbench'),
  'main-lesson': /^\/teacher\/lesson\/\d+$/.test(route.path) || /\/teacher\/pretest\/g[46]\/paper$/.test(route.path),
  'main-map': route.path === '/teacher/course-map',
  'main-pbl': route.path === '/teacher/pbl',
  'main-school': /\/teacher\/course-map\/grade\//.test(route.path) || route.path.startsWith('/teacher/school-ppt/')
}))

const logout = () => {
  localStorage.clear()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.header {
  flex-shrink: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  padding: 0 20px;
  height: 56px;
  background: linear-gradient(90deg, #5b4fc7 0%, #7c6cf0 50%, #9d8df8 100%);
  box-shadow: 0 2px 12px rgba(91, 79, 199, 0.35);
}
.logo {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  margin-right: 12px;
  white-space: nowrap;
}
.header-sub {
  color: rgba(255, 255, 255, 0.75);
  font-size: 13px;
  flex: 1;
}
.user {
  color: #fff;
  margin-left: auto;
  white-space: nowrap;
}
.logout { color: #fff !important; margin-left: 8px; }

.body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.aside {
  display: flex;
  flex-direction: column;
  background: #f8f9fc;
  border-right: 1px solid #e8e6f5;
  transition: width 0.2s;
}
.side-menu {
  flex: 1;
  border-right: none;
  padding: 8px 0;
  overflow-y: auto;
}
.side-menu :deep(.el-menu-item),
.side-menu :deep(.el-sub-menu__title) {
  height: auto;
  min-height: 48px;
  line-height: 1.35;
  padding: 10px 16px !important;
  margin: 4px 8px;
  border-radius: 10px;
  white-space: normal;
}
.side-menu :deep(.el-sub-menu .el-menu-item) {
  min-height: 42px;
  padding-left: 44px !important;
  margin: 2px 8px;
}
.side-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #ede9fe, #e0e7ff) !important;
  color: #5b4fc7;
  font-weight: 600;
}
.collapse-btn {
  margin: 8px;
  color: #64748b;
}
.layout :deep(.el-main) {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  background: #eef0f6;
  padding: 16px 20px 24px;
}
.main-flush {
  padding: 12px 16px 16px !important;
  overflow: hidden;
}
.layout :deep(.el-main.main-pbl) {
  padding: 0 !important;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.layout :deep(.el-main.main-pbl > *) {
  flex: 1;
  min-height: 0;
  height: 100%;
}
.layout :deep(.el-main.main-lesson) {
  padding: 0;
  overflow: hidden;
}
.layout :deep(.el-main.main-map) {
  overflow: hidden;
  padding: 10px 16px 12px;
  display: flex;
  flex-direction: column;
}
.layout :deep(.el-main.main-map > *) {
  flex: 1;
  min-height: 0;
  height: 100%;
  overflow: hidden;
}
.layout :deep(.el-main.main-school) {
  overflow: hidden;
  padding: 10px 16px 12px !important;
  display: flex;
  flex-direction: column;
}
.layout :deep(.el-main.main-school > *) {
  flex: 1;
  width: 100%;
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow: hidden;
  align-self: stretch;
}
</style>
