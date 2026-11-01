<template>
  <div class="tech-space">
    <header class="hero">
      <p class="brand">信息科技学习空间</p>
      <h1>{{ TECH_SPACE.title }}</h1>
      <p class="sub">{{ TECH_SPACE.subtitle }}</p>
    </header>

    <div class="entry-row" role="list">
      <button
        v-for="item in TECH_SPACE_ENTRIES"
        :key="item.id"
        type="button"
        class="entry"
        :class="item.tone"
        role="listitem"
        @click="go(item.path)"
      >
        <span class="entry-name">{{ item.name }}</span>
        <span class="entry-desc">{{ item.desc }}</span>
        <span class="entry-go">进入 →</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { TECH_SPACE, TECH_SPACE_ENTRIES } from '../../constants/techSpace'

const router = useRouter()
const go = (path) => router.push(path)
</script>

<style scoped>
.tech-space {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 28px;
  padding: 8px 4px 12px;
  background:
    radial-gradient(900px 340px at 12% 0%, rgba(99, 102, 241, 0.18), transparent 58%),
    radial-gradient(700px 280px at 90% 8%, rgba(45, 212, 191, 0.14), transparent 55%),
    linear-gradient(180deg, #f3f4ff 0%, #eef2f7 100%);
  border-radius: 18px;
}
.hero {
  text-align: center;
  padding-top: 12px;
}
.brand {
  margin: 0 0 8px;
  font-size: 13px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #6366f1;
  font-weight: 700;
}
.hero h1 {
  margin: 0 0 8px;
  font-size: 32px;
  color: #1e1b4b;
  letter-spacing: 0.06em;
  font-weight: 800;
}
.sub {
  margin: 0 auto;
  max-width: 520px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
}
.entry-row {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  align-content: center;
  max-width: 980px;
  width: 100%;
  margin: 0 auto;
}
.entry {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  text-align: left;
  border: 1px solid rgba(99, 102, 241, 0.14);
  border-radius: 18px;
  padding: 22px 20px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 10px 28px rgba(49, 46, 129, 0.08);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}
.entry:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 36px rgba(49, 46, 129, 0.14);
  border-color: rgba(99, 102, 241, 0.35);
}
.entry-name {
  font-size: 18px;
  font-weight: 700;
  color: #312e81;
}
.entry-desc {
  font-size: 13px;
  line-height: 1.65;
  color: #64748b;
  flex: 1;
}
.entry-go {
  font-size: 13px;
  font-weight: 700;
  color: #4f46e5;
}
.entry.teal .entry-go { color: #0f766e; }
.entry.amber .entry-go { color: #b45309; }
.entry.teal:hover { border-color: rgba(13, 148, 136, 0.35); }
.entry.amber:hover { border-color: rgba(217, 119, 6, 0.35); }

@media (max-width: 900px) {
  .entry-row { grid-template-columns: 1fr; align-content: start; }
  .hero h1 { font-size: 26px; }
}
</style>
