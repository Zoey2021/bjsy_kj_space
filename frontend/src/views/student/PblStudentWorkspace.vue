<template>
  <div class="pbl-page">
    <header class="pbl-hero">
      <el-button link class="back-btn" @click="router.push('/student/map')">← 返回课程地图</el-button>
      <div class="hero-body">
        <img class="hero-cover" :src="PBL_MODULE.coverUrl" alt="项目化学习" />
        <div class="hero-text">
          <h1>{{ project.theme || '项目化学习' }}</h1>
          <p v-if="project.drivingQuestion" class="driving-q">{{ project.drivingQuestion }}</p>
          <p v-else class="driving-q muted">教师尚未发布项目说明，请稍后再来查看。</p>
          <div v-if="metaTags.length" class="meta-tags">
            <el-tag v-for="tag in metaTags" :key="tag" type="success" effect="plain">{{ tag }}</el-tag>
          </div>
        </div>
      </div>
    </header>

    <section class="phase-section">
      <h2 class="section-title">项目学习路径</h2>
      <p class="section-sub">点击各阶段查看相应内容</p>
      <div class="phase-grid">
        <button
          v-for="(phase, index) in PBL_PHASES"
          :key="phase.key"
          type="button"
          class="phase-card"
          :class="{ active: activePhase === phase.key }"
          @click="openPhase(phase.key)"
        >
          <span class="phase-index">{{ index + 1 }}</span>
          <span class="phase-icon">{{ phase.icon }}</span>
          <span class="phase-title">{{ phase.title }}</span>
          <span class="phase-sub">{{ phase.subtitle }}</span>
        </button>
      </div>
    </section>

    <section v-if="activePhase" class="detail-panel">
      <header class="detail-head">
        <h3>{{ currentPhase?.title }}</h3>
        <el-button link @click="activePhase = ''">收起</el-button>
      </header>
      <div v-if="phaseContent" class="detail-content">{{ phaseContent }}</div>
      <el-empty v-else description="该阶段内容待补充，请留意教师通知" :image-size="88" />
    </section>

    <section v-if="project.recruitment.description" class="brief-panel">
      <h3>项目任务说明</h3>
      <p>{{ project.recruitment.description }}</p>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { PBL_MODULE, PBL_PHASES } from '../../constants/pblModule'
import { loadPblProject } from '../../utils/pblStorage'

const router = useRouter()
const project = ref(loadPblProject())
const activePhase = ref('')

onMounted(() => {
  project.value = loadPblProject()
})

const currentPhase = computed(() => PBL_PHASES.find((p) => p.key === activePhase.value))

const phaseContent = computed(() => {
  if (!activePhase.value) return ''
  return project.value.phases?.[activePhase.value]?.content?.trim() || ''
})

const metaTags = computed(() => {
  const tags = []
  if (project.value.period) tags.push(`课时：${project.value.period}`)
  if (project.value.venue === 'campus') tags.push('校内实施')
  if (project.value.venue === 'offsite') tags.push('校外实践')
  if (project.value.interdisciplinary) tags.push('跨学科')
  return tags
})

const openPhase = (key) => {
  activePhase.value = activePhase.value === key ? '' : key
}
</script>

<style scoped>
.pbl-page {
  max-width: 980px;
  margin: 0 auto;
  padding: 8px 8px 40px;
}

.pbl-hero {
  padding: 16px 18px 20px;
  margin-bottom: 24px;
  border-radius: 18px;
  background: linear-gradient(160deg, #ecfdf5 0%, #f0fdf4 45%, #fff 100%);
  border: 1px solid #bbf7d0;
}

.back-btn {
  color: #0f766e !important;
  margin-bottom: 8px;
}

.hero-body {
  display: flex;
  gap: 18px;
  align-items: center;
}

.hero-cover {
  width: 120px;
  height: 160px;
  object-fit: cover;
  border-radius: 14px;
  border: 2px solid #fff;
  box-shadow: 0 8px 24px rgba(13, 148, 136, 0.18);
  flex-shrink: 0;
}

.hero-text h1 {
  margin: 0 0 10px;
  font-size: 26px;
  color: #065f46;
}

.driving-q {
  margin: 0 0 12px;
  line-height: 1.7;
  color: #334155;
  font-size: 15px;
}

.driving-q.muted {
  color: #94a3b8;
}

.meta-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.section-title {
  margin: 0 0 6px;
  font-size: 20px;
  color: #065f46;
}

.section-sub {
  margin: 0 0 18px;
  color: #64748b;
  font-size: 14px;
}

.phase-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 14px;
}

.phase-card {
  appearance: none;
  border: 2px solid #bbf7d0;
  background: linear-gradient(165deg, #f0fdf4 0%, #ecfdf5 100%);
  border-radius: 18px;
  padding: 18px 14px 16px;
  text-align: center;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-height: 168px;
}

.phase-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 28px rgba(13, 148, 136, 0.14);
  border-color: #34d399;
}

.phase-card.active {
  border-color: #059669;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.18);
  background: linear-gradient(165deg, #d1fae5 0%, #ecfdf5 100%);
}

.phase-index {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  background: #059669;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.phase-icon {
  font-size: 28px;
  line-height: 1;
}

.phase-title {
  font-size: 15px;
  font-weight: 700;
  color: #065f46;
  line-height: 1.35;
}

.phase-sub {
  font-size: 12px;
  color: #64748b;
  line-height: 1.45;
}

.detail-panel,
.brief-panel {
  margin-top: 20px;
  padding: 18px 20px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #d1fae5;
  box-shadow: 0 2px 12px rgba(13, 148, 136, 0.06);
}

.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.detail-head h3 {
  margin: 0;
  color: #065f46;
  font-size: 18px;
}

.detail-content {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #334155;
  font-size: 15px;
}

.brief-panel h3 {
  margin: 0 0 10px;
  color: #065f46;
}

.brief-panel p {
  margin: 0;
  line-height: 1.8;
  color: #475569;
  white-space: pre-wrap;
}

@media (max-width: 640px) {
  .hero-body {
    flex-direction: column;
    text-align: center;
  }
  .meta-tags {
    justify-content: center;
  }
}
</style>
