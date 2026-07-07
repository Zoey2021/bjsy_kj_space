<template>
  <div class="page-wrap">
    <el-button link class="back-btn" @click="router.push('/teacher/course-map')">← 返回课程地图</el-button>
    <ModulePlaceholder
      title="跨学科学习"
      description="教师端跨学科探究课程配置与学情管理，功能正在建设中。"
      icon="🔗"
      :features="[
        '跨学科主题与融合点设计',
        '任务链与资源编排',
        '班级分组与过程跟踪',
        '评价量规与成果归档'
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
