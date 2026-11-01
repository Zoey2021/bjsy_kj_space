<template>
  <div class="ai-lab" v-loading="loading">
    <header class="hero">
      <h1>{{ AI_LAB.title }}</h1>
      <p>{{ AI_LAB.subtitle }}</p>
    </header>

    <div v-if="!loading && openHalls.length === 0" class="empty">
      各馆暂未开放，请听老师安排后再来。
    </div>

    <div v-else class="hall-row" role="list">
      <button
        v-for="hall in openHalls"
        :key="hall.id"
        type="button"
        class="hall"
        role="listitem"
        @click="go(hall.path)"
      >
        <span class="cover">
          <img :src="hall.cover" :alt="hall.name" loading="lazy" decoding="async" />
          <span class="hover-panel" aria-hidden="true">
            <span class="hover-title">可以做什么</span>
            <span class="hover-desc">{{ hall.desc }}</span>
            <span class="hover-tags">
              <span v-for="f in hall.features" :key="f" class="tag">{{ f }}</span>
            </span>
          </span>
        </span>
        <span class="name">{{ hall.name }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { AI_LAB, AI_HALLS } from '../../constants/aiLab'
import { getAiLabConfig } from '../../api'

const router = useRouter()
const loading = ref(false)
const hallFlags = ref({})

const openHalls = computed(() =>
  AI_HALLS.filter((h) => {
    const cfg = hallFlags.value[h.id]
    if (!cfg) return true
    return cfg.enabled !== false
  }).map((h) => {
    const cfg = hallFlags.value[h.id] || {}
    return {
      ...h,
      name: cfg.name || h.name,
      desc: cfg.desc || h.desc
    }
  })
)

const go = (path) => router.push(path)

const load = async () => {
  loading.value = true
  try {
    const res = await getAiLabConfig()
    hallFlags.value = res.data?.halls || {}
  } catch {
    hallFlags.value = {}
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.ai-lab {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 4px 2px 8px;
  background:
    radial-gradient(900px 320px at 20% 0%, rgba(125, 211, 252, 0.22), transparent 60%),
    radial-gradient(700px 280px at 85% 10%, rgba(167, 243, 208, 0.18), transparent 55%),
    linear-gradient(180deg, #f4faff 0%, #eef3f9 100%);
  border-radius: 18px;
}
.hero {
  text-align: center;
}
.hero h1 {
  margin: 0 0 6px;
  font-size: 26px;
  color: #0f172a;
  letter-spacing: 0.04em;
}
.hero p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}
.empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 15px;
  padding: 40px 16px;
}
.hall-row {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  align-content: center;
}
@media (max-width: 960px) {
  .hall-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 560px) {
  .hall-row {
    grid-template-columns: 1fr;
  }
}
.hall {
  appearance: none;
  border: 0;
  background: transparent;
  padding: 0;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  text-align: center;
}
.cover {
  position: relative;
  width: 100%;
  aspect-ratio: 3 / 2;
  border-radius: 18px;
  overflow: hidden;
  background: #e2e8f0;
  box-shadow:
    0 12px 28px rgba(15, 23, 42, 0.1),
    inset 0 0 0 1px rgba(255, 255, 255, 0.55);
  transition: transform .22s ease, box-shadow .22s ease;
}
.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform .35s ease, filter .25s ease;
}
.hover-panel {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 6px;
  padding: 14px 14px 16px;
  text-align: left;
  color: #fff;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.08) 10%, rgba(15, 23, 42, 0.78) 100%);
  opacity: 0;
  transform: translateY(8px);
  transition: opacity .22s ease, transform .22s ease;
  pointer-events: none;
}
.hover-title {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  opacity: 0.9;
}
.hover-desc {
  font-size: 13px;
  line-height: 1.55;
  font-weight: 600;
}
.hover-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}
.tag {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(4px);
}
.name {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: 0.06em;
}
.hall:hover .cover,
.hall:focus-visible .cover {
  transform: translateY(-4px);
  box-shadow:
    0 18px 36px rgba(15, 23, 42, 0.14),
    inset 0 0 0 1px rgba(255, 255, 255, 0.7);
}
.hall:hover .cover img,
.hall:focus-visible .cover img {
  transform: scale(1.04);
  filter: brightness(0.92);
}
.hall:hover .hover-panel,
.hall:focus-visible .hover-panel {
  opacity: 1;
  transform: translateY(0);
}
.hall:focus-visible {
  outline: none;
}
.hall:focus-visible .name {
  color: #0e7490;
}
</style>
