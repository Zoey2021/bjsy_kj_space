<template>
  <div class="stats-page">
    <div class="page-head">
      <el-page-header @back="goBack" :content="stats.lessonTitle || '课时学情'" />
    </div>

    <div class="toolbar">
      <el-select v-model="classId" placeholder="选择班级" @change="loadStats" style="width:220px">
        <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="loadStats">刷新</el-button>
    </div>

    <el-row :gutter="16" class="summary" v-if="stats.lessonId">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="班级人数" :value="stats.totalStudents || 0" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="已提交" :value="stats.submittedCount || 0" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="完成率%" :value="stats.completionRate || 0" :precision="1" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="平均正确率%" :value="stats.avgCorrectRate || 0" :precision="1" />
        </el-card>
      </el-col>
    </el-row>

    <el-card class="table-card" v-loading="loading">
      <template #header>学生提交明细</template>
      <el-table :data="stats.students || []" stripe>
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column label="学习状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="是否提交" width="100">
          <template #default="{ row }">
            <el-tag :type="row.submitted ? 'success' : 'info'" size="small">
              {{ row.submitted ? '已提交' : '未提交' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="得分" width="80">
          <template #default="{ row }">{{ row.submitted ? row.score : '—' }}</template>
        </el-table-column>
        <el-table-column label="正确率%" width="100">
          <template #default="{ row }">
            {{ row.correctRate != null ? row.correctRate.toFixed(1) : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.submittedAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getClasses, teacherLessonStats } from '../../api'

const route = useRoute()
const router = useRouter()
const lessonId = Number(route.params.lessonId)
const classId = ref(route.query.classId ? Number(route.query.classId) : null)
const classes = ref([])
const stats = ref({})
const loading = ref(false)

const statusLabel = (s) => ({
  NOT_STARTED: '未开始',
  IN_PROGRESS: '学习中',
  COMPLETED: '已完成'
}[s] || s)

const statusType = (s) => ({
  COMPLETED: 'success',
  IN_PROGRESS: 'warning',
  NOT_STARTED: 'info'
}[s] || 'info')

const formatTime = (t) => {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

const goBack = () => router.push('/teacher/dashboard')

const loadStats = async () => {
  if (!classId.value) {
    ElMessage.warning('请先选择班级')
    return
  }
  loading.value = true
  try {
    const res = await teacherLessonStats(lessonId, classId.value)
    stats.value = res.data || {}
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const res = await getClasses()
  classes.value = res.data || []
  if (!classId.value && classes.value.length) {
    classId.value = classes.value[0].id
  }
  if (classId.value) loadStats()
})
</script>

<style scoped>
.stats-page { max-width: 1100px; margin: 0 auto; }
.page-head { margin-bottom: 16px; }
.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }
.summary { margin-bottom: 20px; }
.table-card { margin-top: 8px; }
</style>
