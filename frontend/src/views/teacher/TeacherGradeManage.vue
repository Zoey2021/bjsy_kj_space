<template>
  <div class="grade-manage" :class="{ embedded }">
    <div class="top-bar" v-if="!embedded">
      <el-page-header @back="goMap" :content="outline?.name || '教材管理'" />
      <el-button type="primary" @click="openCreateDialog">+ 新增课时</el-button>
    </div>
    <div class="top-bar embedded-bar" v-else>
      <span class="embedded-title">单元与课时</span>
      <el-button type="primary" size="small" @click="openCreateDialog">+ 新增课时</el-button>
    </div>

    <div class="layout">
      <aside class="lesson-aside">
        <p class="aside-tip">单元 → 课时（点击编辑课程介绍与资源）</p>
        <template v-for="unit in outline?.units || []" :key="unit.id">
          <p class="unit-name">{{ unit.name }}</p>
          <div v-for="les in unit.lessons || []" :key="les.id" class="lesson-block">
            <div
              class="lesson-item"
              :class="{ active: lesson?.id === les.id }"
              @click="selectLesson(les.id)"
            >
              {{ les.title }}
            </div>
            <el-button
              link
              type="primary"
              size="small"
              class="to-activity"
              @click.stop="goActivityEditor(les.id)"
            >
              配置活动 →
            </el-button>
          </div>
        </template>
      </aside>

      <main class="edit-main" v-if="lesson">
        <h3>{{ lesson.title }}</h3>
        <el-tabs v-model="tab">
          <el-tab-pane label="课程介绍" name="intro">
            <el-input v-model="introDraft" type="textarea" :rows="12" placeholder="本课介绍、教学目标等" />
            <el-button type="primary" class="save-btn" :loading="saving" @click="saveIntro">保存介绍</el-button>
          </el-tab-pane>
          <el-tab-pane label="课程资源" name="resource">
            <el-button type="primary" size="small" @click="openAddResource">+ 新增资源</el-button>
            <div v-for="res in resources" :key="res.id" class="res-row">
              <div>
                <strong>{{ res.title }}</strong>
                <el-tag size="small" style="margin-left:8px">{{ res.resType }}</el-tag>
                <p v-if="res.contentUrl" class="res-url">{{ res.contentUrl }}</p>
              </div>
              <el-button link type="danger" @click="removeResource(res.id)">删除</el-button>
            </div>
            <el-empty v-if="!resources.length" description="暂无资源" :image-size="64" />
          </el-tab-pane>
        </el-tabs>
      </main>
      <main v-else class="edit-main empty">
        <el-empty description="请从左侧选择课时" />
      </main>
    </div>

    <el-dialog v-model="createDialogVisible" title="新增课时" width="480px">
      <el-form label-width="88px">
        <el-form-item label="所属单元">
          <el-select v-model="createForm.unitId" class="full">
            <el-option v-for="u in outline?.units || []" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课时标题">
          <el-input v-model="createForm.title" placeholder="如：第8课 体验控制系统" />
        </el-form-item>
        <el-form-item label="课程介绍">
          <el-input v-model="createForm.content" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreateLesson">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resourceDialogVisible" title="新增课程资源" width="520px">
      <el-form label-width="88px">
        <el-form-item label="标题"><el-input v-model="resourceForm.title" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="resourceForm.resType">
            <el-option label="图文" value="TEXT" />
            <el-option label="网页探究单" value="WEB" />
            <el-option label="外链" value="LINK" />
          </el-select>
        </el-form-item>
        <el-form-item label="网页地址" v-if="resourceForm.resType === 'WEB'">
          <el-input v-model="resourceForm.contentUrl" placeholder="/lessons/..." />
        </el-form-item>
        <el-form-item label="内容" v-else-if="resourceForm.resType === 'TEXT'">
          <el-input v-model="resourceForm.contentText" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="链接" v-else>
          <el-input v-model="resourceForm.contentUrl" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resourceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResource">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

defineProps({
  /** 嵌入教材详情页「建课管理」Tab 时隐藏页头返回 */
  embedded: { type: Boolean, default: false }
})
import { ElMessage } from 'element-plus'
import {
  teacherGetOutline,
  teacherGetLesson,
  teacherCreateLesson,
  teacherUpdateLesson,
  teacherCreateResource,
  teacherDeleteResource
} from '../../api'

const route = useRoute()
const router = useRouter()
const gradeId = Number(route.params.gradeId)

const outline = ref(null)
const lesson = ref(null)
const resources = ref([])
const introDraft = ref('')
const tab = ref('intro')
const saving = ref(false)

const createDialogVisible = ref(false)
const createForm = ref({ unitId: null, title: '', content: '' })
const resourceDialogVisible = ref(false)
const resourceForm = ref({ title: '', resType: 'TEXT', contentText: '', contentUrl: '' })

const goMap = () => router.push('/teacher/course-map')
const goActivityEditor = (lessonId) => {
  router.push({ path: '/teacher/activity-editor', query: { lessonId } })
}

const loadOutline = async () => {
  const res = await teacherGetOutline(gradeId)
  outline.value = res.data
  const first = res.data?.units?.[0]?.lessons?.[0]
  if (first && !lesson.value) await selectLesson(first.id)
}

const selectLesson = async (id) => {
  const res = await teacherGetLesson(id)
  lesson.value = res.data.lesson
  resources.value = res.data.resources || []
  introDraft.value = lesson.value?.content || ''
}

const saveIntro = async () => {
  if (!lesson.value?.id) return
  saving.value = true
  try {
    await teacherUpdateLesson(lesson.value.id, { content: introDraft.value })
    lesson.value.content = introDraft.value
    ElMessage.success('课程介绍已保存')
  } finally {
    saving.value = false
  }
}

const openCreateDialog = () => {
  createForm.value = {
    unitId: outline.value?.units?.[0]?.id || null,
    title: '',
    content: ''
  }
  createDialogVisible.value = true
}

const submitCreateLesson = async () => {
  if (!createForm.value.unitId || !createForm.value.title?.trim()) {
    ElMessage.warning('请选择单元并填写标题')
    return
  }
  const res = await teacherCreateLesson({
    unitId: createForm.value.unitId,
    title: createForm.value.title.trim(),
    content: createForm.value.content
  })
  ElMessage.success('课时已创建')
  createDialogVisible.value = false
  await loadOutline()
  await selectLesson(res.data.id)
}

const openAddResource = () => {
  if (!lesson.value?.id) {
    ElMessage.warning('请先选择课时')
    return
  }
  resourceForm.value = { title: '', resType: 'TEXT', contentText: '', contentUrl: '' }
  resourceDialogVisible.value = true
}

const submitResource = async () => {
  if (!lesson.value?.id || !resourceForm.value.title?.trim()) {
    ElMessage.warning('请填写资源标题')
    return
  }
  await teacherCreateResource({
    lessonId: lesson.value.id,
    title: resourceForm.value.title.trim(),
    resType: resourceForm.value.resType,
    contentText: resourceForm.value.contentText,
    contentUrl: resourceForm.value.contentUrl
  })
  ElMessage.success('资源已添加')
  resourceDialogVisible.value = false
  await selectLesson(lesson.value.id)
  tab.value = 'resource'
}

const removeResource = async (id) => {
  await teacherDeleteResource(id)
  ElMessage.success('已删除')
  await selectLesson(lesson.value.id)
}

onMounted(async () => {
  const qLesson = route.query.lessonId
  await loadOutline()
  if (qLesson) await selectLesson(Number(qLesson))
})
</script>

<style scoped>
.grade-manage {
  height: calc(100vh - 56px - 32px);
  display: flex;
  flex-direction: column;
  max-width: 1200px;
  margin: 0 auto;
}
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.layout {
  flex: 1;
  display: flex;
  gap: 16px;
  min-height: 0;
}
.lesson-aside {
  width: 280px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 14px;
  overflow-y: auto;
}
.aside-tip { font-size: 12px; color: #64748b; margin: 0 0 12px; }
.unit-name {
  font-size: 13px;
  font-weight: 700;
  color: #1e40af;
  margin: 12px 0 6px;
}
.lesson-block {
  margin-bottom: 6px;
  padding-bottom: 4px;
  border-bottom: 1px dashed #e2e8f0;
}
.lesson-item {
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  background: #f8fafc;
}
.lesson-item.active {
  background: #ede9fe;
  color: #5b4fc7;
  font-weight: 600;
}
.to-activity {
  margin: 0 0 8px 4px;
  font-size: 12px;
}
.edit-main {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 20px;
  overflow-y: auto;
}
.edit-main.empty {
  display: flex;
  align-items: center;
  justify-content: center;
}
.save-btn { margin-top: 12px; }
.res-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}
.res-url { font-size: 12px; color: #64748b; margin: 4px 0 0; word-break: break-all; }
.full { width: 100%; }
.grade-manage.embedded .layout {
  min-height: calc(100vh - 220px);
}
.embedded-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.embedded-title {
  font-size: 15px;
  font-weight: 600;
  color: #334155;
}
</style>
