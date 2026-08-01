<template>
  <div class="admin-page">
    <h2>账号管理</h2>
    <el-button type="primary" @click="showDialog = true" style="margin-bottom:16px">新增账号</el-button>

    <el-table :data="users" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="账号" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="role" label="角色" />
      <el-table-column prop="classId" label="班级ID" width="80" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" @click="toggle(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showDialog" title="新增账号" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="账号"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" placeholder="留空则自动生成6位随机密码" />
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role">
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级ID" v-if="form.role === 'STUDENT'">
          <el-input v-model.number="form.classId" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="create">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUsers, createUser, toggleUser } from '../../api'

const users = ref([])
const showDialog = ref(false)
const form = ref({ username: '', password: '', realName: '', role: 'STUDENT', classId: 1 })

onMounted(load)

async function load() {
  const res = await getUsers()
  users.value = res.data
}

async function create() {
  const payload = { ...form.value }
  if (!payload.password) delete payload.password
  const res = await createUser(payload)
  const pwd = res.data?.initialPassword
  if (pwd) {
    ElMessage.success(`创建成功，初始密码：${pwd}（请妥善保存）`)
  } else {
    ElMessage.success('创建成功')
  }
  showDialog.value = false
  form.value = { username: '', password: '', realName: '', role: 'STUDENT', classId: 1 }
  load()
}

async function toggle(row) {
  await toggleUser(row.id)
  ElMessage.success('操作成功')
  load()
}
</script>

<style scoped>
.admin-page { max-width: 1000px; margin: 0 auto; }
</style>
