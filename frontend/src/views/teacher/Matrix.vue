<template>
  <div class="matrix-page">
    <h2 v-if="!embedded">全班完成矩阵</h2>
    <el-select v-model="classId" @change="loadMatrix" style="width:200px;margin-bottom:20px">
      <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
    </el-select>

    <el-table :data="rows" border stripe style="width:100%" max-height="600">
      <el-table-column prop="realName" label="学生" fixed width="100" />
      <el-table-column v-for="task in tasks" :key="task.taskId" :label="task.title" min-width="120" align="center">
        <template #header>
          <el-tooltip :content="task.lessonTitle" placement="top">
            <span>{{ task.title }}</span>
          </el-tooltip>
        </template>
        <template #default="{ row }">
          <span :class="cellClass(getCell(row, task.taskId))">
            {{ cellText(getCell(row, task.taskId)) }}
          </span>
        </template>
      </el-table-column>
    </el-table>

    <p class="legend">
      <span class="done">✓ 已完成</span>
      <span class="undone">✗ 未完成</span>
    </p>
  </div>
</template>

<script setup>
defineProps({ embedded: { type: Boolean, default: false } })

import { ref, onMounted } from 'vue'
import { getClasses, getMatrix } from '../../api'

const classes = ref([])
const classId = ref(1)
const tasks = ref([])
const rows = ref([])

onMounted(async () => {
  const res = await getClasses()
  classes.value = res.data.length ? res.data : [{ id: 1, name: '七年级1班' }]
  classId.value = classes.value[0].id
  await loadMatrix()
})

const loadMatrix = async () => {
  const res = await getMatrix(classId.value)
  tasks.value = res.data.tasks
  rows.value = res.data.rows
}

const getCell = (row, taskId) => row.cells.find(c => c.taskId === taskId)

const cellClass = (cell) => cell && cell.done ? 'done' : 'undone'
const cellText = (cell) => {
  if (!cell || !cell.done) return '✗'
  return cell.score != null ? `✓${cell.score}` : '✓'
}
</script>

<style scoped>
.matrix-page { max-width: 1200px; margin: 0 auto; }
.done { color: #67c23a; font-weight: bold; }
.undone { color: #f56c6c; }
.legend { margin-top: 16px; }
.legend span { margin-right: 20px; }
</style>
