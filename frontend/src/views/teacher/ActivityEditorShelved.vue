<template>
  <div class="shelved-page">
    <ModulePlaceholder
      title="活动编辑"
      description="独立「活动编辑」入口暂搁置。当前请先在「课程地图 → 选择册次 → 课程管理」中编辑课时与探究活动；完整 AI 活动编辑能力将在下一阶段与课程地图一并完善。"
      icon="✏️"
      :features="[
        '课程地图内按册次管理课时与活动（当前可用）',
        'AI 生成活动草案与同步学生端（筹备中）',
        '飞象互动页上传与活动槽位配置（筹备中）'
      ]"
    />
    <div class="actions">
      <el-button type="primary" size="large" @click="$router.push('/teacher/course-map')">
        前往课程地图
      </el-button>
    </div>
  </div>
</template>

<script setup>
import ModulePlaceholder from '../../components/teacher/ModulePlaceholder.vue'
</script>

<style scoped>
.shelved-page {
  max-width: 880px;
  margin: 0 auto;
}
.actions {
  text-align: center;
  margin-top: -8px;
}
</style>
