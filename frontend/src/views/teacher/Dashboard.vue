<template>
  <div class="dashboard-page">
    <div v-if="!embedded" class="page-head">
      <h2>班级学情 <el-tag type="success" v-if="sseConnected">实时连接中</el-tag></h2>
      <el-button type="primary" size="large" round @click="$router.push('/teacher/course-map')">
        课程地图
      </el-button>
    </div>

    <el-select v-model="classId" placeholder="选择班级" @change="loadData" style="width:200px;margin-bottom:20px">
      <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
    </el-select>

    <el-row :gutter="16" class="stats">
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="班级人数" :value="data.totalStudents || 0" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="已提交" :value="data.submittedCount || 0" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="未提交" :value="data.unsubmittedCount || 0" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="完成率%" :value="data.completionRate || 0" :precision="1" /></el-card></el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card>
          <template #header>完成率图表</template>
          <canvas ref="chartRef" height="200"></canvas>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>学生提交明细</template>
          <el-table :data="data.studentDetails || []" stripe max-height="300">
            <el-table-column prop="realName" label="姓名" />
            <el-table-column label="提交进度">
              <template #default="{ row }">{{ row.submittedCount }}/{{ row.totalTasks }}</template>
            </el-table-column>
            <el-table-column label="状态">
              <template #default="{ row }">
                <el-tag :type="row.completed ? 'success' : 'warning'" size="small">
                  {{ row.completed ? '全部完成' : '进行中' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
defineProps({ embedded: { type: Boolean, default: false } })

import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { Chart, DoughnutController, ArcElement, Tooltip, Legend } from 'chart.js'
import { getClasses, getDashboard } from '../../api'

Chart.register(DoughnutController, ArcElement, Tooltip, Legend)

const classes = ref([])
const classId = ref(null)
const data = ref({})
const sseConnected = ref(false)
const chartRef = ref(null)
let chart = null
let eventSource = null

onMounted(async () => {
  const res = await getClasses()
  classes.value = res.data
  if (classes.value.length) {
    classId.value = classes.value[0].id
    await loadData()
    connectSse()
  } else {
  // 演示：教师默认看班级1
    classId.value = 1
    await loadData()
    connectSse()
  }
})

onUnmounted(() => {
  if (eventSource) eventSource.close()
  if (chart) chart.destroy()
})

const loadData = async () => {
  if (!classId.value) return
  const res = await getDashboard(classId.value)
  data.value = res.data
  await nextTick()
  renderChart()
}

const renderChart = () => {
  if (!chartRef.value) return
  if (chart) chart.destroy()
  const submitted = data.value.submittedCount || 0
  const unsubmitted = data.value.unsubmittedCount || 0
  chart = new Chart(chartRef.value, {
    type: 'doughnut',
    data: {
      labels: ['已提交', '未提交'],
      datasets: [{ data: [submitted, unsubmitted], backgroundColor: ['#67c23a', '#dcdfe6'] }]
    },
    options: { responsive: true, plugins: { legend: { position: 'bottom' } } }
  })
}

/**
 * SSE 实时连接（核心亮点）
 * EventSource 不支持自定义请求头，所以 Token 通过 URL 参数传递
 * 学生提交任务后，后端推送 dashboard 事件，此处自动刷新看板
 */
const connectSse = () => {
  if (eventSource) eventSource.close()
  const token = localStorage.getItem('token')
  eventSource = new EventSource(`/api/dashboard/sse/${classId.value}?token=${token}`)

  eventSource.addEventListener('connected', () => { sseConnected.value = true })
  eventSource.addEventListener('dashboard', (e) => {
    data.value = JSON.parse(e.data)
    renderChart()
  })
  eventSource.onerror = () => { sseConnected.value = false }
}
</script>

<style scoped>
.dashboard-page { max-width: 1100px; margin: 0 auto; }
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 8px;
}
.page-head h2 { margin: 0; }
.page-head .el-button {
  background: linear-gradient(135deg, #7c6cf0, #6d5ce8);
  border: none;
}
.stats .el-card { text-align: center; }
</style>
