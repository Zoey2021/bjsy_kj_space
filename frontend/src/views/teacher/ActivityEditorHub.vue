<template>
  <div class="lesson-config" v-loading="booting || loadingConfig">
    <header class="lc-header">
      <div class="header-row">
        <h1 class="header-title">
          <span class="title-icon">📝</span>
          课时配置
        </h1>
        <div class="header-pills">
          <div class="pill pill-select">
            <span class="pill-label">年级</span>
            <el-select
              v-model="gradeId"
              size="small"
              class="filter-select grade-select"
              placeholder="选择年级"
              filterable
              @change="onGradeChange"
            >
              <el-option v-for="g in grades" :key="g.id" :label="g.name" :value="g.id" />
            </el-select>
          </div>
          <div class="pill pill-select">
            <span class="pill-label">课程</span>
            <el-select
              v-model="lessonId"
              size="small"
              class="filter-select lesson-select"
              placeholder="选择课时"
              filterable
              :disabled="!lessons.length"
              @change="onLessonChange"
            >
              <el-option v-for="les in lessons" :key="les.id" :label="les.title" :value="les.id" />
            </el-select>
          </div>
          <div v-if="currentLessonTitle" class="pill">
            <span class="pill-label">当前</span>
            <span class="pill-value">{{ currentLessonTitle }}</span>
          </div>
          <div v-if="lessonId" class="pill">
            <span class="pill-label">状态</span>
            <span class="pill-value">{{ statusLabel }}</span>
          </div>
        </div>
      </div>
      <p class="header-sub">先查看本课已有探究；可单独改标题或替换某一探究。整课重新上传会覆盖全部探究，请谨慎操作。</p>
    </header>

    <div class="lc-body" v-if="lessonId">
      <!-- 已发布 / 平台预置内容 -->
      <section v-if="publishedActivities.length" class="panel current-panel">
        <div class="panel-head row">
          <div>
            <h2>当前学习内容</h2>
            <p>
              {{ sourceLabel }} · 共 {{ publishedActivities.length }} 个探究。
              改标题立即生效；单独替换只更新该探究，不影响其它内容。
            </p>
          </div>
          <el-tag :type="configSource === 'platform' ? 'info' : 'success'" effect="plain">
            {{ statusLabel }}
          </el-tag>
        </div>

        <el-form label-width="88px" class="meta-form">
          <el-form-item label="学习目标">
            <div class="meta-row">
              <el-input
                v-model="objectives"
                type="textarea"
                :rows="2"
                placeholder="展示在学生工作台的学习目标"
              />
              <button type="button" class="action-btn ghost sm" :disabled="savingMeta" @click="saveObjectives">
                {{ savingMeta ? '保存中…' : '保存目标' }}
              </button>
            </div>
          </el-form-item>
        </el-form>

        <div class="act-tabs">
          <button
            v-for="act in publishedActivities"
            :key="'tab-pub-' + act.seq"
            type="button"
            class="act-tab"
            :class="{ active: activePubSeq === act.seq }"
            @click="activePubSeq = act.seq"
          >探究 {{ act.seq }}{{ act.title ? ' · ' + act.title : '' }}</button>
        </div>

        <article v-if="activePublished" class="preview-card single">
          <div class="preview-card-head">
            <span class="seq-badge">探究 {{ activePublished.seq }}</span>
            <el-input v-model="activePublished.title" size="small" class="title-input" placeholder="探究标题" />
            <button
              type="button"
              class="action-btn ghost xs"
              :disabled="savingTitleSeq === activePublished.seq"
              @click="saveTitle(activePublished)"
            >{{ savingTitleSeq === activePublished.seq ? '…' : '保存标题' }}</button>
          </div>
          <div class="card-actions">
            <button type="button" class="action-btn indigo xs" @click="triggerReplace(activePublished.seq)">单独替换 HTML</button>
            <a class="preview-link" :href="previewUrl(activePublished.path)" target="_blank" rel="noopener">新窗口预览</a>
          </div>
          <iframe
            class="preview-frame tall"
            :src="previewUrl(activePublished.path)"
            :title="activePublished.title"
            sandbox="allow-scripts allow-same-origin"
          />
        </article>
        <input
          ref="replaceInput"
          type="file"
          accept=".html,text/html"
          class="hidden-file"
          @change="onReplaceFile"
        />
      </section>

      <section v-else class="panel empty-panel">
        <div class="panel-head">
          <h2>本课尚未配置探究</h2>
          <p>请在下方上传整课 HTML，平台将拆成 2～3 个探究供你预览后发布。</p>
        </div>
      </section>

      <!-- 整课草稿预览（拆分后） -->
      <section v-if="draftActivities.length" class="panel preview-panel">
        <div class="panel-head row">
          <div>
            <h2>整课草稿预览</h2>
            <p>
              共 {{ draftActivities.length }} 个探究。
              <span v-if="publishedActivities.length" class="warn-text">确认发布将覆盖本课全部探究页面。</span>
            </p>
          </div>
          <div class="head-actions">
            <button type="button" class="action-btn ghost" :disabled="publishing" @click="cancelDraft">取消草稿</button>
            <button type="button" class="action-btn orange" :disabled="publishing" @click="doPublish">
              {{ publishing ? '发布中…' : '确认发布（覆盖）' }}
            </button>
          </div>
        </div>
        <div class="act-tabs">
          <button
            v-for="act in draftActivities"
            :key="'tab-draft-' + act.seq"
            type="button"
            class="act-tab draft-tab"
            :class="{ active: activeDraftSeq === act.seq }"
            @click="activeDraftSeq = act.seq"
          >草稿 {{ act.seq }}{{ act.title ? ' · ' + act.title : '' }}</button>
        </div>
        <article v-if="activeDraft" class="preview-card single draft">
          <div class="preview-card-head">
            <span class="seq-badge draft-badge">草稿 {{ activeDraft.seq }}</span>
            <el-input v-model="activeDraft.title" size="small" class="title-input" placeholder="探究标题" />
          </div>
          <iframe
            class="preview-frame tall"
            :src="previewUrl(activeDraft.previewPath)"
            :title="activeDraft.title"
            sandbox="allow-scripts allow-same-origin"
          />
        </article>
      </section>

      <!-- 整课重新上传：有内容时折叠 -->
      <section class="panel upload-panel" v-if="!draftActivities.length">
        <el-collapse v-model="uploadCollapse">
          <el-collapse-item :name="'upload'">
            <template #title>
              <div class="collapse-title">
                <strong>{{ publishedActivities.length ? '整课重新上传（会覆盖全部探究）' : '1. 上传课时 HTML' }}</strong>
                <span class="collapse-hint">按「探究一 / 二 / 三」或大标题自动拆分</span>
              </div>
            </template>
            <el-upload
              class="html-upload"
              drag
              :auto-upload="false"
              :show-file-list="false"
              accept=".html,text/html"
              :disabled="splitting"
              @change="onFileChange"
            >
              <div class="upload-inner">
                <p class="upload-title">{{ selectedFileName || '将 HTML 拖到此处，或点击选择' }}</p>
                <p class="upload-hint">仅 .html · 建议含「探究一」「探究二」等分段标题</p>
              </div>
            </el-upload>
            <div class="upload-actions">
              <button
                type="button"
                class="action-btn indigo"
                :disabled="!selectedFile || splitting"
                @click="doSplit"
              >{{ splitting ? '拆分中…' : '拆分为探究活动' }}</button>
              <button
                v-if="selectedFile"
                type="button"
                class="action-btn ghost"
                :disabled="splitting"
                @click="clearSelectedFile"
              >清除文件</button>
            </div>
            <p v-if="publishedActivities.length" class="overwrite-tip">
              拆分并确认发布后，将覆盖当前全部探究页面；测验与学习评价配置会尽量保留。
            </p>
          </el-collapse-item>
        </el-collapse>
      </section>

      <el-alert
        v-if="tipMessage"
        :type="tipType"
        show-icon
        :closable="true"
        :title="tipMessage"
        class="publish-alert"
        @close="tipMessage = ''"
      />
    </div>

    <div v-else class="lc-empty">
      <p>请先在上方选择年级与课时（与班级学情看板同一套配套课程）。</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  teacherGetTextbooks,
  teacherGetOutline,
  teacherGetLessonHtmlConfig,
  teacherUpdateActivityTitle,
  teacherUpdateLessonHtmlMeta,
  teacherReplaceActivityHtml,
  teacherUploadSplitHtml,
  teacherPublishHtmlPack
} from '../../api'

const booting = ref(true)
const loadingConfig = ref(false)
const grades = ref([])
const lessons = ref([])
const gradeId = ref(null)
const lessonId = ref(null)

const configSource = ref('empty')
const publishedActivities = ref([])
const activePubSeq = ref(1)
const objectives = ref('')
const previewBust = ref(Date.now())

const selectedFile = ref(null)
const selectedFileName = ref('')
const splitting = ref(false)
const publishing = ref(false)
const draftActivities = ref([])
const activeDraftSeq = ref(1)
const uploadCollapse = ref([])

const savingMeta = ref(false)
const savingTitleSeq = ref(null)
const replacingSeq = ref(null)
const replaceInput = ref(null)
const tipMessage = ref('')
const tipType = ref('success')

const currentLessonTitle = computed(() =>
  lessons.value.find((l) => l.id === lessonId.value)?.title || ''
)

const statusLabel = computed(() => {
  if (!lessonId.value) return ''
  if (configSource.value === 'empty' || !publishedActivities.value.length) return '未配置'
  if (configSource.value === 'platform') return `已发布 · 平台预置`
  if (configSource.value === 'teacher_pack') return `已发布 · 教师上传`
  return `已发布 · ${publishedActivities.value.length} 个探究`
})

const sourceLabel = computed(() => {
  if (configSource.value === 'platform') return '平台预置内容'
  if (configSource.value === 'teacher_pack') return '教师上传内容'
  if (configSource.value === 'generated') return '平台生成内容'
  return '已配置内容'
})

const activePublished = computed(() =>
  publishedActivities.value.find((a) => a.seq === activePubSeq.value)
    || publishedActivities.value[0]
    || null
)

const activeDraft = computed(() =>
  draftActivities.value.find((a) => a.seq === activeDraftSeq.value)
    || draftActivities.value[0]
    || null
)

const previewUrl = (path) => {
  if (!path) return 'about:blank'
  const base = path.startsWith('http') ? path : (path.startsWith('/') ? path : `/${path}`)
  const sep = base.includes('?') ? '&' : '?'
  return `${base}${sep}t=${previewBust.value}`
}

const flattenLessons = (outline) => {
  const list = []
  for (const unit of outline?.units || []) {
    for (const les of unit.lessons || []) {
      list.push({
        id: les.id,
        title: unit.name ? `${unit.name} · ${les.title}` : les.title
      })
    }
  }
  return list
}

const applyConfig = (data) => {
  const cfg = data || {}
  configSource.value = cfg.source || 'empty'
  objectives.value = cfg.objectives || ''
  publishedActivities.value = (cfg.activities || []).map((a) => ({
    seq: a.seq || a.index,
    title: a.title || `探究${a.seq || a.index}`,
    path: a.path || a.previewPath || ''
  }))
  if (publishedActivities.value.length) {
    const keep = publishedActivities.value.some((a) => a.seq === activePubSeq.value)
    activePubSeq.value = keep ? activePubSeq.value : publishedActivities.value[0].seq
    uploadCollapse.value = []
  } else {
    activePubSeq.value = 1
    uploadCollapse.value = ['upload']
  }
  previewBust.value = Date.now()
}

const loadLessonConfig = async () => {
  if (!lessonId.value) {
    publishedActivities.value = []
    configSource.value = 'empty'
    return
  }
  loadingConfig.value = true
  try {
    const res = await teacherGetLessonHtmlConfig(lessonId.value)
    applyConfig(res.data || {})
  } catch (e) {
    publishedActivities.value = []
    configSource.value = 'empty'
    ElMessage.error(e?.message || '加载课时配置失败')
  } finally {
    loadingConfig.value = false
  }
}

const loadLessonsForGrade = async (keepLessonId) => {
  if (!gradeId.value) {
    lessons.value = []
    lessonId.value = null
    return
  }
  const outline = await teacherGetOutline(gradeId.value)
  lessons.value = flattenLessons(outline.data || {})
  if (keepLessonId && lessons.value.some((l) => l.id === keepLessonId)) {
    lessonId.value = keepLessonId
  } else if (lessons.value.length) {
    lessonId.value = lessons.value[0].id
  } else {
    lessonId.value = null
  }
  await loadLessonConfig()
}

const clearDraftOnly = () => {
  selectedFile.value = null
  selectedFileName.value = ''
  draftActivities.value = []
}

const clearSelectedFile = () => {
  selectedFile.value = null
  selectedFileName.value = ''
}

const cancelDraft = () => {
  clearDraftOnly()
  tipMessage.value = ''
}

const onGradeChange = async () => {
  clearDraftOnly()
  tipMessage.value = ''
  await loadLessonsForGrade()
}

const onLessonChange = async () => {
  clearDraftOnly()
  tipMessage.value = ''
  await loadLessonConfig()
}

const onFileChange = (uploadFile) => {
  const raw = uploadFile?.raw
  if (!raw) return
  const name = raw.name || ''
  if (!name.toLowerCase().endsWith('.html')) {
    ElMessage.warning('请上传 .html 文件')
    return
  }
  selectedFile.value = raw
  selectedFileName.value = name
  draftActivities.value = []
  tipMessage.value = ''
}

const doSplit = async () => {
  if (!lessonId.value || !selectedFile.value) {
    ElMessage.warning('请先选择课时并上传 HTML')
    return
  }
  if (publishedActivities.value.length) {
    try {
      await ElMessageBox.confirm(
        '拆分后若确认发布，将覆盖本课全部探究页面（测验与学习评价会尽量保留）。是否继续拆分？',
        '整课重新上传',
        { type: 'warning', confirmButtonText: '继续拆分', cancelButtonText: '取消' }
      )
    } catch {
      return
    }
  }
  splitting.value = true
  tipMessage.value = ''
  try {
    const res = await teacherUploadSplitHtml(lessonId.value, selectedFile.value)
    const data = res.data || {}
    draftActivities.value = (data.activities || []).map((a) => ({
      seq: a.seq,
      title: a.title || `探究${a.seq}`,
      previewPath: a.previewPath
    }))
    if (!draftActivities.value.length) {
      ElMessage.warning('未能拆出探究，请检查 HTML 结构')
      return
    }
    activeDraftSeq.value = draftActivities.value[0].seq
    ElMessage.success(data.tip || `已拆成 ${draftActivities.value.length} 个探究，请预览后发布`)
  } catch (e) {
    ElMessage.error(e?.message || '拆分失败')
  } finally {
    splitting.value = false
  }
}

const doPublish = async () => {
  if (!lessonId.value || !draftActivities.value.length) return
  if (publishedActivities.value.length) {
    try {
      await ElMessageBox.confirm(
        `确认用当前草稿覆盖「${currentLessonTitle.value}」的全部探究？此操作会影响学生课程地图中的本课内容。`,
        '确认覆盖发布',
        { type: 'warning', confirmButtonText: '确认覆盖', cancelButtonText: '取消' }
      )
    } catch {
      return
    }
  }
  publishing.value = true
  try {
    const res = await teacherPublishHtmlPack(lessonId.value, {
      objectives: objectives.value,
      activities: draftActivities.value.map((a) => ({ seq: a.seq, title: a.title }))
    })
    const data = res.data || {}
    clearDraftOnly()
    await loadLessonConfig()
    tipType.value = 'success'
    tipMessage.value = `已发布「${data.lessonTitle || currentLessonTitle.value}」共 ${data.activityCount || publishedActivities.value.length} 个探究。`
    ElMessage.success(res.message || '已发布')
  } catch (e) {
    ElMessage.error(e?.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

const saveTitle = async (act) => {
  if (!lessonId.value || !act?.seq) return
  savingTitleSeq.value = act.seq
  try {
    const res = await teacherUpdateActivityTitle(lessonId.value, act.seq, act.title)
    applyConfig(res.data || {})
    ElMessage.success(res.message || '标题已保存')
  } catch (e) {
    ElMessage.error(e?.message || '保存标题失败')
  } finally {
    savingTitleSeq.value = null
  }
}

const saveObjectives = async () => {
  if (!lessonId.value) return
  savingMeta.value = true
  try {
    const res = await teacherUpdateLessonHtmlMeta(lessonId.value, { objectives: objectives.value })
    applyConfig(res.data || {})
    ElMessage.success(res.message || '学习目标已保存')
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    savingMeta.value = false
  }
}

const triggerReplace = async (seq) => {
  replacingSeq.value = seq
  await nextTick()
  replaceInput.value?.click()
}

const onReplaceFile = async (ev) => {
  const file = ev?.target?.files?.[0]
  const seq = replacingSeq.value
  if (replaceInput.value) replaceInput.value.value = ''
  if (!file || !seq || !lessonId.value) return
  if (!String(file.name || '').toLowerCase().endsWith('.html')) {
    ElMessage.warning('请上传 .html 文件')
    return
  }
  try {
    await ElMessageBox.confirm(
      `将只替换「探究 ${seq}」的页面，其它探究与测验配置不受影响。是否继续？`,
      '单独替换',
      { type: 'warning', confirmButtonText: '替换', cancelButtonText: '取消' }
    )
  } catch {
    replacingSeq.value = null
    return
  }
  loadingConfig.value = true
  try {
    const res = await teacherReplaceActivityHtml(lessonId.value, seq, file)
    applyConfig(res.data || {})
    tipType.value = 'success'
    tipMessage.value = `探究 ${seq} 已更新，学生刷新后即可看到新内容。`
    ElMessage.success(res.message || '该探究已更新')
  } catch (e) {
    ElMessage.error(e?.message || '替换失败')
  } finally {
    loadingConfig.value = false
    replacingSeq.value = null
  }
}

onMounted(async () => {
  booting.value = true
  try {
    const res = await teacherGetTextbooks('MAIN')
    grades.value = res.data || []
    const prefer = grades.value.find((g) => String(g.name || '').includes('六年级上'))
      || grades.value.find((g) => String(g.name || '').includes('四年级上'))
    gradeId.value = prefer?.id || grades.value[0]?.id || null
    if (gradeId.value) await loadLessonsForGrade()
  } finally {
    booting.value = false
  }
})
</script>

<style scoped>
.lesson-config {
  min-height: calc(100vh - 56px);
  background: #f5f3ff;
}
.lc-header {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 20px 16px;
  background: linear-gradient(135deg, #5b4fc7 0%, #7c3aed 45%, #9333ea 100%);
  color: #fff;
  box-shadow: 0 4px 20px rgba(91, 79, 199, 0.35);
}
.header-row {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex-wrap: wrap;
}
.header-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  white-space: nowrap;
}
.title-icon { font-size: 20px; }
.header-pills {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.pill {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  font-size: 12px;
  backdrop-filter: blur(4px);
  flex-shrink: 0;
}
.pill-label { opacity: 0.85; white-space: nowrap; }
.pill-value { font-weight: 600; white-space: nowrap; max-width: 220px; overflow: hidden; text-overflow: ellipsis; }
.pill-select { padding-right: 4px; }
.filter-select { width: 140px; }
.grade-select { width: 150px; }
.lesson-select { width: 260px; }
.filter-select :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: none;
}
.header-sub {
  margin: 0;
  font-size: 12px;
  opacity: 0.9;
  line-height: 1.5;
  max-width: 960px;
}

.lc-body {
  padding: 16px 20px 28px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.lc-empty {
  padding: 64px 20px;
  text-align: center;
  color: #64748b;
  font-size: 14px;
}
.panel {
  background: #fff;
  border: 1px solid #e9e5ff;
  border-radius: 14px;
  padding: 16px 18px 18px;
  box-shadow: 0 2px 12px rgba(91, 79, 199, 0.06);
}
.panel-head h2 {
  margin: 0 0 4px;
  font-size: 16px;
  color: #312e81;
}
.panel-head p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}
.panel-head.row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}
.head-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.warn-text { color: #c2410c; font-weight: 600; }

.meta-form { margin-bottom: 8px; max-width: 900px; }
.meta-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  width: 100%;
}
.meta-row .el-textarea { flex: 1; }

.html-upload { margin-top: 4px; }
.html-upload :deep(.el-upload-dragger) {
  border-radius: 12px;
  border: 1.5px dashed #c4b5fd;
  background: #faf8ff;
  padding: 28px 16px;
}
.html-upload :deep(.el-upload-dragger:hover) {
  border-color: #7c3aed;
  background: #f5f3ff;
}
.upload-inner { text-align: center; }
.upload-title {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
  color: #312e81;
}
.upload-hint {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
}
.upload-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  flex-wrap: wrap;
}
.overwrite-tip {
  margin: 10px 0 0;
  font-size: 12px;
  color: #c2410c;
}
.collapse-title {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.4;
}
.collapse-title strong { color: #312e81; font-size: 14px; }
.collapse-hint { color: #94a3b8; font-size: 12px; font-weight: 400; }
.upload-panel :deep(.el-collapse) { border: none; }
.upload-panel :deep(.el-collapse-item__header) {
  height: auto;
  line-height: 1.4;
  padding: 4px 0 10px;
  border: none;
  background: transparent;
}
.upload-panel :deep(.el-collapse-item__wrap) { border: none; }
.upload-panel :deep(.el-collapse-item__content) { padding-bottom: 4px; }

.action-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  white-space: nowrap;
}
.action-btn.sm { padding: 7px 12px; font-size: 12px; margin-top: 2px; }
.action-btn.xs { padding: 5px 10px; font-size: 12px; }
.action-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.action-btn.indigo { background: linear-gradient(135deg, #4338ca, #6366f1); }
.action-btn.orange { background: linear-gradient(135deg, #f97316, #fb923c); }
.action-btn.ghost {
  background: #fff;
  color: #5b4fc7;
  border: 1px solid #c4b5fd;
}

.act-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 4px 0 12px;
}
.act-tab {
  border: 1px solid #ddd6fe;
  background: #faf8ff;
  color: #5b4fc7;
  border-radius: 999px;
  padding: 7px 14px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.act-tab:hover { border-color: #7c3aed; }
.act-tab.active {
  background: linear-gradient(135deg, #4338ca, #6366f1);
  border-color: transparent;
  color: #fff;
}
.act-tab.draft-tab {
  border-color: #fdba74;
  background: #fff7ed;
  color: #c2410c;
}
.act-tab.draft-tab.active {
  background: linear-gradient(135deg, #ea580c, #fb923c);
  border-color: transparent;
  color: #fff;
}
.preview-card {
  border: 1px solid #e9e5ff;
  border-radius: 12px;
  overflow: hidden;
  background: #faf8ff;
  display: flex;
  flex-direction: column;
}
.preview-card.single { min-height: 520px; }
.preview-card.draft { border-color: #fdba74; background: #fff7ed; }
.preview-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #fff;
  border-bottom: 1px solid #ede9fe;
}
.card-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #fff;
  border-bottom: 1px solid #f1f5f9;
}
.preview-link {
  font-size: 12px;
  color: #5b4fc7;
  text-decoration: none;
}
.preview-link:hover { text-decoration: underline; }
.seq-badge {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 700;
  color: #5b4fc7;
  background: #ede9fe;
  padding: 4px 8px;
  border-radius: 999px;
}
.draft-badge {
  color: #c2410c;
  background: #ffedd5;
}
.title-input { flex: 1; min-width: 0; }
.preview-frame {
  flex: 1;
  width: 100%;
  min-height: 280px;
  border: 0;
  background: #fff;
}
.preview-frame.tall {
  min-height: 560px;
  height: 62vh;
}
.hidden-file { display: none; }
.publish-alert { margin-top: 4px; }

@media (max-width: 768px) {
  .lesson-select { width: 200px; }
  .panel-head.row { flex-direction: column; }
  .meta-row { flex-direction: column; }
}
</style>
