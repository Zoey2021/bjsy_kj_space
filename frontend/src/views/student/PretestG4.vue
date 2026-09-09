<template>
  <div class="pretest-page">
    <header class="pretest-bar">
      <el-button link type="primary" @click="router.push('/student/map')">← 返回课程地图</el-button>
      <span class="pretest-title">四年级信息科技基础水平测试</span>
      <span class="pretest-who">{{ who }}</span>
    </header>
    <iframe class="pretest-frame" src="/pretest/g4-pretest.html" title="四年级前测" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const who = computed(() => {
  const name = localStorage.getItem('realName') || ''
  const cls = localStorage.getItem('className') || ''
  return [name, cls].filter(Boolean).join(' · ')
})
</script>

<style scoped>
.pretest-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #eef2f7;
}
.pretest-bar {
  flex-shrink: 0;
  height: 48px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid #dbe3ee;
}
.pretest-title {
  font-weight: 700;
  color: #1e3a8a;
}
.pretest-who {
  margin-left: auto;
  font-size: 13px;
  color: #64748b;
}
.pretest-frame {
  flex: 1;
  min-height: 0;
  width: 100%;
  border: 0;
  background: #eef2f7;
}
</style>
