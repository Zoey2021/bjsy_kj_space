<template>
  <div class="page-wrap">
    <el-button link class="back-btn" @click="router.push('/student/map')">← 返回课程地图</el-button>
    <ModulePlaceholder
      title="跨学科学习"
      description="融合多学科知识与方法的探究学习模块，内容正在建设中。"
      icon="🔗"
      :features="[
        '学科融合主题设计',
        '跨学科任务与活动',
        '协作探究与成果展示',
        '多元评价与反思'
      ]"
    />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import ModulePlaceholder from '../../components/teacher/ModulePlaceholder.vue'

const router = useRouter()
</script>

<style scoped>
.page-wrap {
  max-width: 880px;
  margin: 0 auto;
  padding: 8px 4px 32px;
}
.back-btn {
  color: #0f766e !important;
  margin-bottom: 8px;
}
</style>
