<template>
  <div class="placeholder-page">
    <el-card shadow="never" class="hero-card">
      <div class="hero-icon">{{ icon }}</div>
      <h2>{{ title }}</h2>
      <p class="desc">{{ description }}</p>
      <el-tag type="warning" effect="plain">功能建设中</el-tag>
    </el-card>
    <el-card shadow="never" class="feature-card">
      <template #header>规划能力</template>
      <ul class="feature-list">
        <li v-for="(item, i) in features" :key="i">{{ item }}</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
  icon: { type: String, default: '🚧' },
  features: { type: Array, default: () => [] }
})
</script>

<style scoped>
.placeholder-page {
  max-width: 880px;
  margin: 0 auto;
}
.hero-card {
  text-align: center;
  margin-bottom: 16px;
  border-radius: 14px;
}
.hero-icon {
  font-size: 48px;
  margin-bottom: 8px;
}
.hero-card h2 {
  margin: 0 0 8px;
  font-size: 22px;
  color: #1e293b;
}
.desc {
  color: #64748b;
  margin: 0 0 14px;
  line-height: 1.6;
}
.feature-card {
  border-radius: 14px;
}
.feature-list {
  margin: 0;
  padding-left: 20px;
  color: #475569;
  line-height: 1.9;
}
</style>
