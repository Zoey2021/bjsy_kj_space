<template>
  <div class="settings-page" v-loading="booting">
    <div class="page-head">
      <h2>后台管理</h2>
      <p>教师、班级、学生账号与班级归属，数据直接写入数据库</p>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- 教师管理 -->
      <el-tab-pane label="教师管理" name="teachers">
        <div class="toolbar">
          <el-button type="primary" @click="openTeacherDialog()">新增教师</el-button>
          <el-button @click="loadTeachers">刷新</el-button>
        </div>
        <el-table :data="teachers" stripe border size="small" v-loading="loadingTeachers">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="username" label="账号" min-width="120" />
          <el-table-column prop="realName" label="姓名" min-width="100" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openTeacherDialog(row)">编辑</el-button>
              <el-button link type="warning" size="small" @click="resetTeacherPwd(row)">重置密码</el-button>
              <el-button
                link
                size="small"
                @click="toggleTeacherStatus(row)"
              >{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
              <el-button link type="danger" size="small" @click="removeTeacher(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 班级管理 -->
      <el-tab-pane label="班级管理" name="classes">
        <div class="toolbar">
          <el-button type="primary" @click="openClassDialog()">新增班级</el-button>
          <el-button @click="loadClasses">刷新</el-button>
        </div>
        <el-table :data="classes" stripe border size="small" v-loading="loadingClasses">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="班级名称" min-width="140" />
          <el-table-column prop="gradeName" label="年级" width="100" />
          <el-table-column prop="teacherName" label="任课教师" width="110" />
          <el-table-column prop="studentCount" label="学生数" width="90" align="center" />
          <el-table-column prop="loginCode" label="班级码" width="100" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openClassDialog(row)">编辑</el-button>
              <el-button link type="success" size="small" @click="goStudents(row)">学生</el-button>
              <el-button link type="danger" size="small" @click="removeClass(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 学生管理 -->
      <el-tab-pane label="学生管理" name="students">
        <div class="toolbar">
          <el-select
            v-model="filterClassId"
            filterable
            placeholder="请选择班级"
            style="width: 260px"
            @change="loadStudents"
          >
            <el-option
              v-for="c in classes"
              :key="c.id"
              :label="`${c.name}（${c.studentCount ?? 0}人）`"
              :value="c.id"
            />
          </el-select>
          <el-button type="primary" @click="openStudentDialog()">新增学生</el-button>
          <el-button type="success" @click="openBatchDialog">批量添加学生</el-button>
          <el-button :disabled="!filterClassId" @click="downloadStudentRoster">下载学生名单</el-button>
          <el-button :disabled="!filterClassId" @click="loadStudents">刷新</el-button>
        </div>
        <p v-if="selectedClass" class="roster-hint">
          当前班级：<strong>{{ selectedClass.name }}</strong>
          共 <strong>{{ students.length }}</strong> 人
        </p>
        <p v-else class="roster-hint">请先选择班级，再查看该班全部学生名单。</p>
        <el-table
          :data="students"
          stripe
          border
          size="small"
          v-loading="loadingStudents"
          :empty-text="filterClassId ? '该班暂无学生' : '请先选择班级'"
        >
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="username" label="账号" min-width="140" />
          <el-table-column prop="studentNo" label="学号" width="80" />
          <el-table-column prop="realName" label="姓名" min-width="100" />
          <el-table-column prop="className" label="班级" min-width="140" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openStudentDialog(row)">编辑</el-button>
              <el-button link type="warning" size="small" @click="resetStudentPwd(row)">重置密码</el-button>
              <el-button link size="small" @click="toggleStudentStatus(row)">
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button link type="danger" size="small" @click="removeStudent(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 其他占位 -->
      <el-tab-pane label="更多配置" name="more">
        <el-row :gutter="16">
          <el-col :xs="24" :md="12" v-for="block in moreBlocks" :key="block.key">
            <el-card shadow="hover" class="block-card">
              <div class="block-icon">{{ block.icon }}</div>
              <h3>{{ block.title }}</h3>
              <ul>
                <li v-for="(f, i) in block.features" :key="i">{{ f }}</li>
              </ul>
              <el-tag size="small" type="info">即将上线</el-tag>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>

    <!-- 教师弹窗 -->
    <el-dialog v-model="teacherVisible" :title="teacherForm.id ? '编辑教师' : '新增教师'" width="440px">
      <el-form :model="teacherForm" label-width="88px">
        <el-form-item label="账号" required>
          <el-input v-model="teacherForm.username" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="teacherForm.realName" />
        </el-form-item>
        <el-form-item v-if="!teacherForm.id" label="密码">
          <el-input v-model="teacherForm.password" placeholder="留空则自动生成6位密码" />
        </el-form-item>
        <el-form-item v-else label="新密码">
          <el-input v-model="teacherForm.password" placeholder="留空表示不修改密码" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="teacherForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="teacherVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveTeacher">保存</el-button>
      </template>
    </el-dialog>

    <!-- 班级弹窗 -->
    <el-dialog v-model="classVisible" :title="classForm.id ? '编辑班级' : '新增班级'" width="440px">
      <el-form :model="classForm" label-width="88px">
        <el-form-item label="班级名称" required>
          <el-input v-model="classForm.name" placeholder="如：2023级4班" />
        </el-form-item>
        <el-form-item label="年级" required>
          <el-input v-model="classForm.gradeName" placeholder="如：2023级 / 六年级" />
        </el-form-item>
        <el-form-item label="任课教师" required>
          <el-select v-model="classForm.teacherId" filterable placeholder="选择教师" style="width:100%">
            <el-option
              v-for="t in teachers"
              :key="t.id"
              :label="`${t.realName}（${t.username}）`"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveClass">保存</el-button>
      </template>
    </el-dialog>

    <!-- 学生弹窗 -->
    <el-dialog v-model="studentVisible" :title="studentForm.id ? '编辑学生' : '新增学生'" width="440px">
      <el-form :model="studentForm" label-width="88px">
        <el-form-item label="账号/学号" required>
          <el-input v-model="studentForm.username" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="studentForm.realName" />
        </el-form-item>
        <el-form-item label="班级" required>
          <el-select v-model="studentForm.classId" filterable placeholder="选择班级" style="width:100%">
            <el-option
              v-for="c in classes"
              :key="c.id"
              :label="`${c.name}（${c.gradeName}）`"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!studentForm.id" label="密码">
          <el-input v-model="studentForm.password" placeholder="留空则自动生成6位密码" />
        </el-form-item>
        <el-form-item v-else label="新密码">
          <el-input v-model="studentForm.password" placeholder="留空表示不修改密码" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="studentForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="studentVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveStudent">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchVisible" title="批量添加学生" width="720px" destroy-on-close>
      <p class="batch-tip">每行填写：班级、学号、姓名。班级名称须与「班级管理」一致（如 2023级4班）。每班最多 50 人，学号为 1～50。</p>
      <div class="toolbar" style="margin-bottom: 10px">
        <el-button @click="downloadStudentTemplate">下载名单模板</el-button>
        <el-upload :show-file-list="false" accept=".csv,.txt,.tsv" :before-upload="onRosterFile">
          <el-button>上传 CSV</el-button>
        </el-upload>
      </div>
      <el-input
        v-model="batchText"
        type="textarea"
        :rows="10"
        placeholder="班级,学号,姓名&#10;2023级4班,1,张三&#10;2023级4班,2,李四"
      />
      <div v-if="batchResult" class="batch-result">
        <p>成功 {{ batchResult.createdCount || 0 }} 人，失败 {{ batchResult.failedCount || 0 }} 人。</p>
        <el-button v-if="batchResult.created?.length" size="small" @click="downloadCreatedAccounts">下载本次账号密码</el-button>
        <el-table v-if="batchResult.failed?.length" :data="batchResult.failed" size="small" max-height="220" style="margin-top:8px">
          <el-table-column prop="line" label="行" width="60" />
          <el-table-column prop="className" label="班级" width="120" />
          <el-table-column prop="studentNo" label="学号" width="70" />
          <el-table-column prop="realName" label="姓名" width="90" />
          <el-table-column prop="reason" label="原因" min-width="180" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="batchVisible = false">关闭</el-button>
        <el-button type="primary" :loading="saving" @click="submitBatch">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  manageListTeachers,
  manageCreateTeacher,
  manageUpdateTeacher,
  manageDeleteTeacher,
  manageResetTeacherPassword,
  manageListClasses,
  manageCreateClass,
  manageUpdateClass,
  manageDeleteClass,
  manageListStudents,
  manageCreateStudent,
  manageBatchCreateStudents,
  manageUpdateStudent,
  manageDeleteStudent,
  manageResetStudentPassword
} from '../../api'

const activeTab = ref('teachers')
const booting = ref(false)
const saving = ref(false)

const teachers = ref([])
const classes = ref([])
const students = ref([])
const loadingTeachers = ref(false)
const loadingClasses = ref(false)
const loadingStudents = ref(false)
const filterClassId = ref(null)
const selectedClass = computed(() => classes.value.find((c) => c.id === filterClassId.value) || null)

const teacherVisible = ref(false)
const classVisible = ref(false)
const studentVisible = ref(false)
const batchVisible = ref(false)
const batchText = ref('')
const batchResult = ref(null)

const teacherForm = ref({ id: null, username: '', realName: '', password: '', status: 1 })
const classForm = ref({ id: null, name: '', gradeName: '', teacherId: null })
const studentForm = ref({ id: null, username: '', realName: '', password: '', classId: null, status: 1 })

const moreBlocks = [
  {
    key: 'ai',
    icon: '⚙️',
    title: 'AI 配置',
    features: ['评价规则与提示词', 'SOLO 层级标准', 'AI 调用额度']
  },
  {
    key: 'export',
    icon: '📤',
    title: '数据导出',
    features: ['导出学情 Excel', '导出作业提交', '导出评价报告']
  }
]

const showPwd = (res, fallback) => {
  const pwd = res?.data?.initialPassword
  if (pwd) {
    ElMessageBox.alert(`初始密码：<strong>${pwd}</strong><br/>请妥善告知用户并提醒尽快修改。`, '密码已生成', {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '知道了'
    })
  } else {
    ElMessage.success(fallback)
  }
}

const loadTeachers = async () => {
  loadingTeachers.value = true
  try {
    const res = await manageListTeachers()
    teachers.value = res.data || []
  } catch (e) {
    ElMessage.error(e?.message || '加载教师失败')
  } finally {
    loadingTeachers.value = false
  }
}

const loadClasses = async () => {
  loadingClasses.value = true
  try {
    const res = await manageListClasses()
    classes.value = res.data || []
  } catch (e) {
    ElMessage.error(e?.message || '加载班级失败')
  } finally {
    loadingClasses.value = false
  }
}

const loadStudents = async () => {
  if (!filterClassId.value) {
    students.value = []
    return
  }
  loadingStudents.value = true
  try {
    const res = await manageListStudents(filterClassId.value)
    students.value = res.data || []
  } catch (e) {
    ElMessage.error(e?.message || '加载学生失败')
  } finally {
    loadingStudents.value = false
  }
}

const onTabChange = async (name) => {
  if (name === 'teachers') loadTeachers()
  if (name === 'classes') {
    loadTeachers()
    loadClasses()
  }
  if (name === 'students') {
    await loadClasses()
    if (!filterClassId.value && classes.value[0]) {
      filterClassId.value = classes.value[0].id
    }
    await loadStudents()
  }
}

const goStudents = (row) => {
  filterClassId.value = row.id
  activeTab.value = 'students'
  loadStudents()
}

const openTeacherDialog = (row) => {
  if (row) {
    teacherForm.value = {
      id: row.id,
      username: row.username,
      realName: row.realName,
      password: '',
      status: row.status
    }
  } else {
    teacherForm.value = { id: null, username: '', realName: '', password: '', status: 1 }
  }
  teacherVisible.value = true
}

const saveTeacher = async () => {
  if (!teacherForm.value.username?.trim() || !teacherForm.value.realName?.trim()) {
    ElMessage.warning('请填写账号和姓名')
    return
  }
  saving.value = true
  try {
    const payload = {
      username: teacherForm.value.username.trim(),
      realName: teacherForm.value.realName.trim(),
      status: teacherForm.value.status
    }
    if (teacherForm.value.password) payload.password = teacherForm.value.password
    if (teacherForm.value.id) {
      await manageUpdateTeacher(teacherForm.value.id, payload)
      ElMessage.success('已保存')
    } else {
      const res = await manageCreateTeacher(payload)
      showPwd(res, '创建成功')
    }
    teacherVisible.value = false
    await loadTeachers()
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const toggleTeacherStatus = async (row) => {
  try {
    await manageUpdateTeacher(row.id, { status: row.status === 1 ? 0 : 1 })
    ElMessage.success('状态已更新')
    await loadTeachers()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const resetTeacherPwd = async (row) => {
  try {
    await ElMessageBox.confirm(`确定重置教师「${row.realName}」的密码？`, '提示', { type: 'warning' })
    const res = await manageResetTeacherPassword(row.id)
    showPwd(res, '密码已重置')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '重置失败')
  }
}

const removeTeacher = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除教师「${row.realName}」？此操作不可恢复。`, '删除确认', { type: 'warning' })
    await manageDeleteTeacher(row.id)
    ElMessage.success('已删除')
    await loadTeachers()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

const openClassDialog = async (row) => {
  if (!teachers.value.length) await loadTeachers()
  if (row) {
    classForm.value = {
      id: row.id,
      name: row.name,
      gradeName: row.gradeName,
      teacherId: row.teacherId
    }
  } else {
    const meName = localStorage.getItem('realName')
    const me = teachers.value.find((t) => t.realName === meName) || teachers.value[0]
    classForm.value = {
      id: null,
      name: '',
      gradeName: '',
      teacherId: me?.id || null
    }
  }
  classVisible.value = true
}

const saveClass = async () => {
  if (!classForm.value.name?.trim() || !classForm.value.gradeName?.trim() || !classForm.value.teacherId) {
    ElMessage.warning('请完整填写班级信息')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: classForm.value.name.trim(),
      gradeName: classForm.value.gradeName.trim(),
      teacherId: classForm.value.teacherId
    }
    if (classForm.value.id) {
      await manageUpdateClass(classForm.value.id, payload)
      ElMessage.success('已保存')
    } else {
      await manageCreateClass(payload)
      ElMessage.success('创建成功')
    }
    classVisible.value = false
    await loadClasses()
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const removeClass = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除班级「${row.name}」？班级内若有学生将无法删除。`,
      '删除确认',
      { type: 'warning' }
    )
    await manageDeleteClass(row.id)
    ElMessage.success('已删除')
    await loadClasses()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

const openStudentDialog = async (row) => {
  if (!classes.value.length) await loadClasses()
  if (row) {
    studentForm.value = {
      id: row.id,
      username: row.username,
      realName: row.realName,
      password: '',
      classId: row.classId,
      status: row.status
    }
  } else {
    studentForm.value = {
      id: null,
      username: '',
      realName: '',
      password: '',
      classId: filterClassId.value || (classes.value[0]?.id ?? null),
      status: 1
    }
  }
  studentVisible.value = true
}

const saveStudent = async () => {
  if (!studentForm.value.username?.trim() || !studentForm.value.realName?.trim() || !studentForm.value.classId) {
    ElMessage.warning('请填写账号、姓名并选择班级')
    return
  }
  saving.value = true
  try {
    const payload = {
      username: studentForm.value.username.trim(),
      realName: studentForm.value.realName.trim(),
      classId: studentForm.value.classId,
      status: studentForm.value.status
    }
    if (studentForm.value.password) payload.password = studentForm.value.password
    if (studentForm.value.id) {
      await manageUpdateStudent(studentForm.value.id, payload)
      ElMessage.success('已保存')
    } else {
      const res = await manageCreateStudent(payload)
      showPwd(res, '创建成功')
    }
    studentVisible.value = false
    await loadStudents()
    await loadClasses()
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const toggleStudentStatus = async (row) => {
  try {
    await manageUpdateStudent(row.id, { status: row.status === 1 ? 0 : 1 })
    ElMessage.success('状态已更新')
    await loadStudents()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const resetStudentPwd = async (row) => {
  try {
    await ElMessageBox.confirm(`确定重置学生「${row.realName}」的密码？`, '提示', { type: 'warning' })
    const res = await manageResetStudentPassword(row.id)
    showPwd(res, '密码已重置')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '重置失败')
  }
}

const removeStudent = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除学生「${row.realName}」？此操作不可恢复。`, '删除确认', { type: 'warning' })
    await manageDeleteStudent(row.id)
    ElMessage.success('已删除')
    await loadStudents()
    await loadClasses()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

const downloadCsv = (filename, header, lines) => {
  const blob = new Blob(['\uFEFF' + [header, ...lines].join('\n')], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = filename
  a.click()
}

const rosterStudentNo = (row) => {
  if (row.studentNo) return row.studentNo
  const m = String(row.username || '').match(/20\d{2}(\d{2})(\d{2})$/)
  return m ? String(parseInt(m[2], 10)) : ''
}

const downloadStudentRoster = () => {
  if (!students.value.length) {
    ElMessage.warning('当前没有可下载的学生，请先选择班级或刷新名单')
    return
  }
  const lines = students.value.map((r) => [r.className || '', rosterStudentNo(r), r.realName || ''].join(','))
  const name = filterClassId.value
    ? (classes.value.find((c) => c.id === filterClassId.value)?.name || '班级')
    : '全部班级'
  downloadCsv(`学生名单_${name}.csv`, '班级,学号,姓名', lines)
  ElMessage.success('已下载学生名单')
}

const downloadStudentTemplate = () => {
  downloadCsv('学生名单模板.csv', '班级,学号,姓名', ['2023级4班,1,张三', '2023级4班,2,李四'])
}

const parseRosterText = (text) => {
  const rows = []
  for (const raw of String(text || '').replace(/^\uFEFF/, '').split(/\r?\n/)) {
    const line = raw.trim()
    if (!line) continue
    const parts = line.split(/[,，\t]/).map((s) => s.trim().replace(/^"|"$/g, ''))
    if (parts[0] === '班级' || parts[0].toLowerCase() === 'classname') continue
    rows.push({
      className: parts[0] || '',
      studentNo: parts[1] || '',
      realName: parts[2] || ''
    })
  }
  return rows
}

const onRosterFile = (file) => {
  const reader = new FileReader()
  reader.onload = () => {
    batchText.value = String(reader.result || '')
    ElMessage.success('已读入文件，请确认后导入')
  }
  reader.readAsText(file, 'UTF-8')
  return false
}

const openBatchDialog = () => {
  batchText.value = '班级,学号,姓名\n'
  batchResult.value = null
  batchVisible.value = true
}

const submitBatch = async () => {
  const rows = parseRosterText(batchText.value)
  if (!rows.length) {
    ElMessage.warning('请先粘贴或上传名单（班级、学号、姓名）')
    return
  }
  saving.value = true
  try {
    const res = await manageBatchCreateStudents({ rows })
    batchResult.value = res.data || {}
    ElMessage.success(res.message || '导入完成')
    await loadStudents()
    await loadClasses()
  } catch (e) {
    ElMessage.error(e?.message || '导入失败')
  } finally {
    saving.value = false
  }
}

const downloadCreatedAccounts = () => {
  const list = batchResult.value?.created || []
  if (!list.length) return
  const lines = list.map((r) => [r.className || '', r.studentNo || '', r.realName || '', r.username || '', r.initialPassword || ''].join(','))
  downloadCsv('批量导入账号.csv', '班级,学号,姓名,账号,初始密码', lines)
}

onMounted(async () => {
  booting.value = true
  try {
    await Promise.all([loadTeachers(), loadClasses()])
  } finally {
    booting.value = false
  }
})
</script>

<style scoped>
.settings-page { max-width: 1100px; margin: 0 auto; }
.page-head { margin-bottom: 16px; }
.page-head h2 { margin: 0 0 4px; font-size: 22px; }
.page-head p { margin: 0; color: #64748b; font-size: 13px; }
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
  align-items: center;
}
.roster-hint {
  margin: 0 0 10px;
  font-size: 13px;
  color: #475569;
}
.block-card { border-radius: 12px; margin-bottom: 16px; min-height: 200px; }
.block-icon { font-size: 36px; margin-bottom: 8px; }
.block-card h3 { margin: 0 0 12px; font-size: 16px; }
.block-card ul {
  margin: 0 0 14px;
  padding-left: 18px;
  color: #475569;
  font-size: 13px;
  line-height: 1.8;
}
.batch-tip { margin: 0 0 10px; color: #64748b; font-size: 13px; line-height: 1.6; }
.batch-result { margin-top: 12px; font-size: 13px; color: #334155; }
</style>
