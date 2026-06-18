<template>
  <ModulePlaceholder
    title="智能评价"
    description="自动批改、自动评级、生成学生与课程分析报告"
    icon="🤖"
    :features="[
      '一键批量评价全班学生',
      '查看分数、SOLO 思维层级、等级（优秀/良好/待提升）',
      '导出学生 PDF 评价报告',
      '生成整节课课程分析报告，定位教学问题'
    ]"
  />
</template>

<script setup>
import ModulePlaceholder from '../../components/teacher/ModulePlaceholder.vue'
</script>
