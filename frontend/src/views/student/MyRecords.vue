<template>
  <div class="records-page">
    <h2>📚 我的学习记录</h2>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-statistic title="总积分" :value="totalPoints" />
      </el-col>
      <el-col :span="8">
        <el-statistic title="已完成课时" :value="completedCount" />
      </el-col>
      <el-col :span="8">
        <el-statistic title="提交次数" :value="submissionLogs.length" />
      </el-col>
    </el-row>

    <el-card class="section">
      <template #header>课时进度</template>
      <el-table :data="progress" stripe>
        <el-table-column prop="lessonId" label="课时ID" width="100" />
        <el-table-column prop="status" label="状态" />
        <el-table-column prop="progressPercent" label="进度%">
          <template #default="{ row }">{{ row.progressPercent }}%</template>
        </el-table-column>
        <el-table-column prop="studySeconds" label="学习时长(秒)" />
      </el-table>
      <el-empty v-if="!progress.length" description="暂无课时进度" :image-size="64" />
    </el-card>

    <el-card class="section">
      <template #header>每次提交记录</template>
      <el-table :data="submissionLogs" stripe>
        <el-table-column prop="lessonId" label="课时ID" width="90" />
        <el-table-column prop="activityKey" label="活动" width="120" />
        <el-table-column prop="score" label="得分" width="80" />
        <el-table-column prop="createdAt" label="提交时间" />
      </el-table>
      <el-empty v-if="!submissionLogs.length" description="暂无提交记录" :image-size="64" />
    </el-card>

    <el-card class="section">
      <template #header>积分记录</template>
      <el-table :data="pointsHistory" stripe>
        <el-table-column prop="description" label="说明" />
        <el-table-column prop="points" label="积分" width="80" />
        <el-table-column prop="createdAt" label="时间" />
      </el-table>
      <el-empty v-if="!pointsHistory.length" description="暂无积分记录" :image-size="64" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyRecords } from '../../api'

const progress = ref([])
const submissionLogs = ref([])
const pointsHistory = ref([])
const totalPoints = ref(0)

const completedCount = computed(() => progress.value.filter((p) => p.status === 'COMPLETED').length)

onMounted(async () => {
  try {
    const res = await getMyRecords()
    const data = res.data || {}
    progress.value = data.progress || []
    submissionLogs.value = data.submissionLogs || []
    pointsHistory.value = data.pointsHistory || []
    totalPoints.value = data.totalPoints || 0
  } catch {
    ElMessage.error('加载学习记录失败，请稍后重试')
  }
})
</script>

<style scoped>
.records-page { max-width: 900px; margin: 0 auto; }
.section { margin-top: 20px; }
</style>
