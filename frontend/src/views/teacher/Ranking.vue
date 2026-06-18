<template>
  <div class="ranking-page">
    <h2 v-if="!embedded">班级积分排行</h2>
    <el-select v-model="classId" @change="loadRanking" style="width:200px;margin-bottom:20px">
      <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
    </el-select>
    <el-table :data="ranking" stripe>
      <el-table-column prop="rank" label="排名" width="80" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="points" label="积分" />
    </el-table>
    <canvas ref="chartRef" height="250" style="margin-top:20px"></canvas>
  </div>
</template>

<script setup>
defineProps({ embedded: { type: Boolean, default: false } })

import { ref, onMounted, nextTick } from 'vue'
import { Chart, BarController, BarElement, CategoryScale, LinearScale, Tooltip, Legend } from 'chart.js'
import { getClasses, getRanking } from '../../api'

Chart.register(BarController, BarElement, CategoryScale, LinearScale, Tooltip, Legend)

const classes = ref([])
const classId = ref(1)
const ranking = ref([])
const chartRef = ref(null)
let chart = null

onMounted(async () => {
  const res = await getClasses()
  classes.value = res.data.length ? res.data : [{ id: 1, name: '七年级1班' }]
  classId.value = classes.value[0].id
  await loadRanking()
})

const loadRanking = async () => {
  const res = await getRanking(classId.value)
  ranking.value = res.data
  await nextTick()
  if (chart) chart.destroy()
  if (!chartRef.value) return
  chart = new Chart(chartRef.value, {
    type: 'bar',
    data: {
      labels: ranking.value.map(r => r.realName),
      datasets: [{ label: '积分', data: ranking.value.map(r => r.points), backgroundColor: '#1a73e8' }]
    },
    options: { responsive: true, plugins: { legend: { display: false } } }
  })
}
</script>

<style scoped>
.ranking-page { max-width: 800px; margin: 0 auto; }
</style>
