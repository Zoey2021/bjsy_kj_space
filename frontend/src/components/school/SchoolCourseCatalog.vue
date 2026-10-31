<template>
  <div class="catalog">
    <section class="hero">
      <div class="hero-text">
        <div class="hero-kicker">校本课程</div>
        <h1>{{ heroTitle }}</h1>
        <p>{{ heroSubtitle }}</p>
      </div>
      <div class="hero-stats">
        <span>{{ tree.units?.length || 0 }} 个板块</span>
        <span>{{ totalLessons }} 课</span>
        <span>{{ isAiEnlighten ? '点击课时查看本课' : '点击课时打开课件' }}</span>
      </div>
    </section>

    <div class="module-grid">
      <section
        v-for="(unit, idx) in tree.units || []"
        :key="unit.id"
        class="module"
        :style="moduleStyle(unit)"
      >
        <header class="module-head">
          <span class="module-icon">{{ moduleMeta(unit).icon }}</span>
          <div class="module-titles">
            <small>板块 {{ String(idx + 1).padStart(2, '0') }}</small>
            <h2>{{ moduleMeta(unit).label }}</h2>
          </div>
          <span class="module-count">{{ unit.lessons?.length || 0 }} 课</span>
        </header>
        <div class="module-lessons">
          <button
            v-for="(lesson, li) in unit.lessons || []"
            :key="lesson.id"
            type="button"
            class="lesson-row"
            @click="$emit('lesson-click', lesson, unit)"
          >
            <span class="lesson-no">{{ globalNo(idx, li) }}</span>
            <span class="lesson-title">{{ displayTitle(lesson.title) }}</span>
            <span class="lesson-cta">{{ moduleMeta(unit).key === 'aien' ? '进入' : '课件' }}</span>
          </button>
        </div>
      </section>
    </div>

    <el-empty v-if="!(tree.units || []).length" description="本册目录正在建设中" :image-size="72" />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  tree: { type: Object, required: true }
})

defineEmits(['lesson-click'])

const THEMES = {
  print3d: { icon: '▣', label: '3D打印', accent: '#d97706', tint: '#fff7ed', ink: '#9a3412' },
  robot: { icon: '◎', label: '机器人', accent: '#4f46e5', tint: '#eef2ff', ink: '#3730a3' },
  astro: { icon: '✧', label: '天文', accent: '#0f766e', tint: '#f0fdfa', ink: '#115e59' },
  ai: { icon: '◇', label: '人工智能', accent: '#7c3aed', tint: '#f5f3ff', ink: '#5b21b6' },
  aien: { icon: '◇', label: '人工智能启蒙', accent: '#6d28d9', tint: '#f5f3ff', ink: '#5b21b6' }
}

const totalLessons = computed(() =>
  (props.tree.units || []).reduce((n, u) => n + (u.lessons?.length || 0), 0)
)

const isAiEnlighten = computed(() => String(props.tree.description || '').includes('人工智能启蒙'))

const heroTitle = computed(() => {
  const name = props.tree.name || ''
  if (isAiEnlighten.value) return name ? `人工智能启蒙 · ${name}` : '人工智能启蒙'
  return name || '科创启航'
})

const heroSubtitle = computed(() => {
  const desc = props.tree.description || ''
  if (desc.includes('科创启航')) return '3D打印 · 机器人 · 天文 · 人工智能'
  if (desc.includes('AI初体验')) return 'AI初体验 · 认识智能新朋友'
  if (desc.includes('我是AI小玩家')) return '我是AI小玩家 · 探秘逻辑与未来'
  return desc || '点击课时即可打开教学课件'
})

const parseUnit = (unit) => {
  const raw = String(unit?.description || '')
  const key = raw.split('|')[0] || ''
  const blurb = raw.includes('|') ? raw.slice(raw.indexOf('|') + 1) : ''
  return { key, blurb }
}

const moduleMeta = (unit) => {
  const parsed = parseUnit(unit)
  const themeKey = parsed.key.startsWith('ai-') ? 'aien' : parsed.key
  const theme = THEMES[themeKey] || THEMES.robot
  const name = String(unit?.name || '')
  const label = name.includes('·') ? name.split('·').pop().trim() : theme.label
  return { ...theme, key: themeKey, label, blurb: parsed.blurb || theme.label }
}

const moduleStyle = (unit) => {
  const m = moduleMeta(unit)
  return { '--accent': m.accent, '--tint': m.tint, '--ink': m.ink }
}

const globalNo = (unitIdx, lessonIdx) => {
  let n = lessonIdx + 1
  const units = props.tree.units || []
  for (let i = 0; i < unitIdx; i++) n += units[i].lessons?.length || 0
  return String(n).padStart(2, '0')
}

const displayTitle = (title) => String(title || '').replace(/^第\d+课\s+/, '')
</script>

<style scoped>
.catalog {
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow: hidden;
  box-sizing: border-box;
}
.hero {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: linear-gradient(135deg, #312e81 0%, #4f46e5 52%, #6366f1 100%);
  border-radius: 16px;
  padding: 12px 18px;
  color: #fff;
  box-shadow: 0 10px 24px rgba(49, 46, 129, 0.18);
}
.hero-kicker {
  font-size: 11px;
  letter-spacing: 0.16em;
  opacity: 0.78;
  font-weight: 700;
}
.hero h1 {
  margin: 2px 0 0;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0.02em;
  line-height: 1.2;
}
.hero p {
  margin: 2px 0 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.84);
}
.hero-stats {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}
.hero-stats span {
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  white-space: nowrap;
}
.module-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
  gap: 10px;
}
.module {
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #e4e7f2;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.05);
}
.module-head {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: var(--tint);
  border-bottom: 1px solid rgba(15, 23, 42, 0.04);
}
.module-icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: #fff;
  color: var(--accent);
  font-size: 16px;
}
.module-titles {
  flex: 1;
  min-width: 0;
}
.module-titles small {
  display: block;
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--ink);
  opacity: 0.7;
  font-weight: 700;
}
.module-titles h2 {
  margin: 0;
  font-size: 16px;
  color: var(--ink);
  line-height: 1.2;
}
.module-count {
  font-size: 12px;
  font-weight: 700;
  color: var(--accent);
}
.module-lessons {
  flex: 1;
  min-height: 0;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.lesson-row {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 0;
  text-align: left;
  border: 1px solid transparent;
  background: #f8fafc;
  border-radius: 10px;
  padding: 0 10px;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, box-shadow 0.15s;
}
.lesson-row:hover {
  background: #fff;
  border-color: #c7d2fe;
  box-shadow: 0 6px 14px rgba(79, 70, 229, 0.08);
}
.lesson-no {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: var(--tint);
  color: var(--ink);
  font-weight: 800;
  font-size: 12px;
}
.lesson-title {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.lesson-cta {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 700;
  color: var(--accent);
}
@media (max-width: 900px) {
  .hero { flex-direction: column; align-items: flex-start; }
  .hero-stats { justify-content: flex-start; }
}
</style>
