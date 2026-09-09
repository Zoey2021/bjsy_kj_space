<template>
  <div class="pretest-page">
    <header class="pretest-bar">
      <el-button link type="primary" @click="router.push('/teacher/pretest/g4')">← 返回前测监控</el-button>
      <span class="pretest-title">四年级前测试卷预览</span>
      <span class="pretest-who">教师预览 · 不可提交</span>
    </header>
    <iframe class="pretest-frame" src="/pretest/g4-pretest.html?preview=1" title="四年级前测预览" />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
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
.pretest-title { font-weight: 700; color: #1e3a8a; }
.pretest-who { margin-left: auto; font-size: 13px; color: #64748b; }
.pretest-frame { flex: 1; min-height: 0; width: 100%; border: 0; }
</style>
