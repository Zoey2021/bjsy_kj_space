<template>
  <div class="workbench" v-loading="booting">
    <header class="wb-head">
      <div>
        <h1>分层脚手架工作台</h1>
        <p>为每个探究活动配置 A / B / C 三档任务。发布后学生按前测档位自动收到对应内容，学生端不显示档位标签。</p>
      </div>
      <el-tag v-if="status === 'published'" type="success">已发布</el-tag>
      <el-tag v-else-if="lessonId" type="warning">草稿</el-tag>
    </header>

    <el-steps :active="step" align-center finish-status="success" class="wb-steps">
      <el-step title="选课时与模板" class="step-hit" @click="goStep(0)" />
      <el-step title="AI 生成草稿" class="step-hit" @click="goStep(1)" />
      <el-step title="审核发布" class="step-hit" @click="goStep(2)" />
    </el-steps>
    <p class="step-jump">
      <el-button link type="primary" @click="goStep(0)">1. 选课时</el-button>
      <span>·</span>
      <el-button link type="primary" @click="goStep(1)">2. 去 AI 生成</el-button>
      <span>·</span>
      <el-button link type="primary" @click="goStep(2)">3. 去审核发布</el-button>
    </p>

    <div class="wb-body">
      <aside class="wb-aside">
        <el-select v-model="gradeId" placeholder="选择教材" filterable class="full" @change="onGradeChange">
          <el-option v-for="g in grades" :key="g.id" :label="g.name" :value="g.id" />
        </el-select>
        <p class="aside-tip">单元 → 课时（需先在活动编辑中配置探究活动）</p>
        <template v-for="unit in outline?.units || []" :key="unit.id">
          <p class="unit-name">{{ unit.name }}</p>
          <div
            v-for="les in unit.lessons || []"
            :key="les.id"
            class="lesson-item"
            :class="{ active: lessonId === les.id }"
            @click="selectLesson(les.id)"
          >
            {{ les.title }}
          </div>
        </template>
      </aside>

      <main class="wb-main" v-if="lessonId" v-loading="generating" :element-loading-text="generatingText">
        <!-- 第一步 -->
        <section v-show="step === 0">
          <h2>{{ lessonTitle || '未选择课时' }}</h2>
          <el-form label-width="108px" class="tpl-form">
            <el-form-item label="脚手架模板">
              <el-select v-model="templateId" class="full" @change="applyTemplate">
                <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="模板名称">
              <el-input v-model="templateName" />
            </el-form-item>
          </el-form>
          <div class="tier-grid">
            <div v-for="code in ['A', 'B', 'C']" :key="code" class="tier-card">
              <h3>{{ code }} 档目标</h3>
              <el-input v-model="tierDefs[code].goal" type="textarea" :rows="3" />
              <p class="mini-label">达成标准（每行一条）</p>
              <el-input v-model="tierCriteriaText[code]" type="textarea" :rows="4" />
            </div>
          </div>
          <el-button @click="saveTemplate">保存模板</el-button>
          <h3 class="sub-title">本课探究活动</h3>
          <el-table :data="activities" stripe empty-text="该课时还没有探究活动">
            <el-table-column prop="index" label="#" width="60" />
            <el-table-column prop="title" label="活动名称" />
          </el-table>
          <p v-if="!aiReady" class="ai-warn">尚未配置百炼工作流。请在服务器设置 AI_APP_ID、AI_API_KEY 后重建后端；也可以先手动填写三档内容再发布。</p>
          <div class="wb-actions">
            <el-tooltip
              :disabled="!!activities.length"
              content="该课时还没有探究活动，请先到「活动编辑」里配置"
              placement="top"
            >
              <el-button type="primary" :disabled="!activities.length" @click="goStep(1)">下一步：生成草稿</el-button>
            </el-tooltip>
          </div>
        </section>

        <!-- 第二步 -->
        <section v-show="step === 1">
          <div class="row-between">
            <h2>编辑三档草稿</h2>
            <div>
              <el-button type="primary" :loading="generatingAll" :disabled="locked || generating" @click="generateAll">整课时批量生成</el-button>
              <el-button v-if="generatingAll" @click="cancelGenerate">取消</el-button>
              <el-button @click="saveDraft">保存草稿</el-button>
            </div>
          </div>
          <p v-if="!activities.length" class="empty-hint">没有可生成的活动。</p>
          <div v-for="item in items" :key="item.activityIndex" class="item-card">
            <div class="item-head">
              <strong>#{{ item.activityIndex }} {{ item.title }}</strong>
              <el-button
                type="primary"
                plain
                size="small"
                :loading="generatingIndex === item.activityIndex"
                :disabled="locked || generating"
                @click="generateOne(item.activityIndex)"
              >
                AI 生成
              </el-button>
            </div>
            <el-tabs>
              <el-tab-pane v-for="code in ['A', 'B', 'C']" :key="code" :label="code + ' 档'">
                <el-form label-width="92px">
                  <el-form-item label="任务描述">
                    <el-input v-model="item.tiers[code].task" type="textarea" :rows="3" :disabled="locked" />
                  </el-form-item>
                  <el-form-item v-for="(h, hi) in item.tiers[code].hints" :key="hi" :label="'提示 ' + (hi + 1)">
                    <el-input v-model="item.tiers[code].hints[hi]" :disabled="locked" />
                  </el-form-item>
                  <el-form-item v-if="code === 'A'" label="变式挑战">
                    <el-input v-model="item.tiers.A.challenge" type="textarea" :rows="2" :disabled="locked" />
                  </el-form-item>
                  <el-form-item v-if="code === 'B'" label="半成品示例">
                    <el-input v-model="item.tiers.B.example" type="textarea" :rows="2" :disabled="locked" />
                  </el-form-item>
                  <el-form-item v-if="code === 'C'" label="完整示例">
                    <el-input v-model="item.tiers.C.workedExample" type="textarea" :rows="3" :disabled="locked" />
                  </el-form-item>
                </el-form>
                <template v-if="code === 'C'">
                  <div class="trace-head">
                    <span>Tracing 观察题</span>
                    <el-button size="small" :disabled="locked" @click="addTrace(item)">+ 题目</el-button>
                  </div>
                  <div v-for="(q, qi) in item.tiers.C.traceQuestions" :key="qi" class="trace-editor">
                    <el-input v-model="q.question" placeholder="题干" :disabled="locked" />
                    <el-input
                      v-for="(_, oi) in q.options"
                      :key="oi"
                      v-model="q.options[oi]"
                      :placeholder="'选项 ' + optionLabel(oi)"
                      :disabled="locked"
                      style="margin-top:6px"
                    />
                    <el-select v-model="q.answer" placeholder="正确答案" :disabled="locked" style="margin-top:6px;width:160px">
                      <el-option v-for="(opt, oi) in q.options" :key="oi" :label="'答案 ' + optionLabel(oi)" :value="oi" />
                    </el-select>
                    <el-button link type="danger" :disabled="locked" @click="item.tiers.C.traceQuestions.splice(qi, 1)">删除</el-button>
                  </div>
                </template>
              </el-tab-pane>
            </el-tabs>
          </div>
          <div class="wb-actions">
            <el-button @click="goStep(0)">上一步</el-button>
            <el-button type="primary" :disabled="!items.length" @click="goReview">下一步：审核发布</el-button>
          </div>
        </section>

        <!-- 第三步 -->
        <section v-show="step === 2">
          <div class="row-between">
            <h2>审核后发布</h2>
            <el-button v-if="status === 'published'" @click="reEdit">重新编辑</el-button>
          </div>
          <p class="aside-tip">全部勾选「已审核」后才能发布。发布后学生才会按档收到任务。</p>
          <el-table :data="items" stripe>
            <el-table-column prop="activityIndex" label="#" width="60" />
            <el-table-column prop="title" label="活动" />
            <el-table-column label="已审核" width="120">
              <template #default="{ row }">
                <el-checkbox v-model="row.reviewed" :disabled="locked && status === 'published'" />
              </template>
            </el-table-column>
          </el-table>
          <div class="wb-actions">
            <el-button @click="goStep(1)">上一步</el-button>
            <el-button type="primary" :disabled="!canPublish" :loading="publishing" @click="publish">发布到学生端</el-button>
          </div>
        </section>
      </main>
      <main v-else class="wb-main empty">
        <el-empty description="请从左侧选择课时" />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  teacherGetTextbooks,
  teacherGetOutline,
  teacherGetLessonScaffold,
  teacherSaveLessonScaffold,
  teacherPublishLessonScaffold,
  teacherGenerateScaffold,
  teacherSaveScaffoldTemplate
} from '../../api'

const route = useRoute()
const booting = ref(false)
const step = ref(0)
const grades = ref([])
const gradeId = ref(null)
const outline = ref(null)
const lessonId = ref(null)
const lessonTitle = ref('')
const status = ref('draft')
const templates = ref([])
const templateId = ref(null)
const templateName = ref('')
const activities = ref([])
const items = ref([])
const generatingAll = ref(false)
const generatingIndex = ref(null)
const publishing = ref(false)
const aiReady = ref(true)
let abortCtrl = null

const generating = computed(() => generatingAll.value || generatingIndex.value != null)
const generatingText = computed(() =>
  generatingAll.value ? '正在整课时调用百炼工作流，大约 1～3 分钟，请稍候…' : '正在调用百炼工作流，大约 1～2 分钟，请稍候…'
)

const tierDefs = reactive({
  A: { goal: '', criteria: [] },
  B: { goal: '', criteria: [] },
  C: { goal: '', criteria: [] }
})
const tierCriteriaText = reactive({ A: '', B: '', C: '' })

const locked = computed(() => status.value === 'published')
const canPublish = computed(() => items.value.length > 0 && items.value.every((i) => i.reviewed))

const optionLabel = (i) => ['A', 'B', 'C', 'D'][i] || String(i + 1)

const emptyTier = (code) => {
  const t = { task: '', hints: ['', '', '', ''] }
  if (code === 'A') t.challenge = ''
  if (code === 'B') t.example = ''
  if (code === 'C') {
    t.workedExample = ''
    t.traceQuestions = []
  }
  return t
}

const normalizeItem = (raw) => {
  const item = {
    activityIndex: raw.activityIndex,
    title: raw.title,
    reviewed: !!raw.reviewed,
    tiers: { A: emptyTier('A'), B: emptyTier('B'), C: emptyTier('C') }
  }
  for (const code of ['A', 'B', 'C']) {
    const src = raw.tiers?.[code] || {}
    Object.assign(item.tiers[code], src)
    const hints = Array.isArray(src.hints) ? [...src.hints] : []
    while (hints.length < 4) hints.push('')
    item.tiers[code].hints = hints.slice(0, 4)
    if (code === 'C' && !Array.isArray(item.tiers.C.traceQuestions)) {
      item.tiers.C.traceQuestions = []
    }
  }
  return item
}

const applyTemplate = () => {
  const t = templates.value.find((x) => x.id === templateId.value)
  if (!t) return
  templateName.value = t.name
  for (const code of ['A', 'B', 'C']) {
    const src = t['tier' + code] || {}
    tierDefs[code].goal = src.goal || ''
    tierDefs[code].criteria = Array.isArray(src.criteria) ? src.criteria : []
    tierCriteriaText[code] = (tierDefs[code].criteria || []).join('\n')
  }
}

const readCriteria = () => {
  const defs = { A: {}, B: {}, C: {} }
  for (const code of ['A', 'B', 'C']) {
    defs[code] = {
      goal: tierDefs[code].goal,
      criteria: String(tierCriteriaText[code] || '').split('\n').map((s) => s.trim()).filter(Boolean)
    }
  }
  return defs
}

const applyPayload = (data) => {
  lessonTitle.value = data.lessonTitle || lessonTitle.value
  status.value = data.status || 'draft'
  templateId.value = data.templateId || templateId.value
  activities.value = data.activities || []
  templates.value = data.templates || templates.value
  items.value = (data.items || []).map(normalizeItem)
  if (data.aiReady != null) aiReady.value = !!data.aiReady
  if (templateId.value) applyTemplate()
}

const goStep = (n) => {
  if (!lessonId.value) {
    ElMessage.warning('请先在左侧选择课时')
    return
  }
  if (n >= 1 && !activities.value.length) {
    ElMessage.warning('该课时还没有探究活动，请先到「活动编辑」里配置后再生成')
    return
  }
  step.value = n
}

const loadLesson = async (id) => {
  lessonId.value = id
  const res = await teacherGetLessonScaffold(id)
  applyPayload(res.data || {})
}

const selectLesson = async (id) => {
  step.value = 0
  await loadLesson(id)
}

const onGradeChange = async () => {
  if (!gradeId.value) return
  const res = await teacherGetOutline(gradeId.value)
  outline.value = res.data || {}
  lessonId.value = null
  items.value = []
}

const saveTemplate = async () => {
  const defs = readCriteria()
  const res = await teacherSaveScaffoldTemplate({
    id: templateId.value,
    name: templateName.value || '未命名模板',
    tierA: defs.A,
    tierB: defs.B,
    tierC: defs.C
  })
  ElMessage.success('模板已保存')
  const saved = res.data
  if (saved?.id) {
    const idx = templates.value.findIndex((t) => t.id === saved.id)
    if (idx >= 0) templates.value[idx] = saved
    else templates.value.push(saved)
    templateId.value = saved.id
  }
}

const payloadItems = () => items.value.map((item) => ({
  activityIndex: item.activityIndex,
  title: item.title,
  reviewed: !!item.reviewed,
  tiers: item.tiers
}))

const saveDraft = async () => {
  const res = await teacherSaveLessonScaffold(lessonId.value, {
    templateId: templateId.value,
    items: payloadItems()
  })
  applyPayload(res.data || {})
  ElMessage.success('草稿已保存')
}

const cancelGenerate = () => {
  if (abortCtrl) abortCtrl.abort()
}

const runGenerate = async (activityIndex) => {
  if (locked.value) {
    ElMessage.warning('已发布的课时请先点「重新编辑」')
    return
  }
  if (!aiReady.value) {
    ElMessage.warning('未配置百炼工作流。请在服务器设置 AI_APP_ID、AI_API_KEY 后重建后端；也可先手动填写三档内容。')
    return
  }
  abortCtrl = new AbortController()
  ElMessage.info('开始生成，大约需要 1～2 分钟，请不要关闭页面')
  try {
    const res = await teacherGenerateScaffold({
      lessonId: lessonId.value,
      templateId: templateId.value,
      activityIndex,
      tierDefinitions: readCriteria()
    }, { signal: abortCtrl.signal })
    applyPayload(res.data || {})
    ElMessage.success('草稿已生成，请检查后可再改')
  } catch (err) {
    if (err?.code === 'ERR_CANCELED' || err?.name === 'CanceledError') {
      ElMessage.info('已取消生成')
      return
    }
    const msg = err?.message || err?.response?.data?.message || ''
    if (!msg) {
      ElMessage.error('AI 生成失败，请手动填写或重试')
    }
  } finally {
    abortCtrl = null
  }
}

const generateOne = async (index) => {
  generatingIndex.value = index
  try {
    await runGenerate(index)
  } finally {
    generatingIndex.value = null
  }
}

const generateAll = async () => {
  generatingAll.value = true
  try {
    await runGenerate('all')
  } finally {
    generatingAll.value = false
  }
}

const addTrace = (item) => {
  if (!Array.isArray(item.tiers.C.traceQuestions)) item.tiers.C.traceQuestions = []
  item.tiers.C.traceQuestions.push({ question: '', options: ['', '', ''], answer: 0 })
}

const goReview = async () => {
  await saveDraft()
  step.value = 2
}

const reEdit = async () => {
  await saveDraft()
  step.value = 1
  ElMessage.success('已回到草稿，修改后需再次发布')
}

const publish = async () => {
  publishing.value = true
  try {
    const res = await teacherPublishLessonScaffold(lessonId.value, {
      templateId: templateId.value,
      items: payloadItems()
    })
    applyPayload(res.data || {})
    ElMessage.success('已发布')
  } finally {
    publishing.value = false
  }
}

onMounted(async () => {
  booting.value = true
  try {
    const [main, school] = await Promise.all([
      teacherGetTextbooks('MAIN'),
      teacherGetTextbooks('SCHOOL')
    ])
    grades.value = [...(main.data || []), ...(school.data || [])]
    const prefer = grades.value.find((g) => String(g.name || '').includes('六年级上'))
    gradeId.value = prefer?.id || grades.value[0]?.id || null
    if (gradeId.value) {
      const res = await teacherGetOutline(gradeId.value)
      outline.value = res.data || {}
    }
    const q = Number(route.query.lessonId)
    if (q) await selectLesson(q)
  } finally {
    booting.value = false
  }
})
</script>

<style scoped>
.workbench {
  height: calc(100vh - 56px);
  display: flex;
  flex-direction: column;
  padding: 12px 16px 16px;
}
.wb-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 8px;
}
.wb-head h1 { margin: 0 0 4px; font-size: 20px; }
.wb-head p { margin: 0; color: #64748b; font-size: 13px; }
.wb-steps { margin: 8px 0 4px; }
.wb-steps :deep(.el-step__head),
.wb-steps :deep(.el-step__title) { cursor: pointer; }
.step-jump {
  text-align: center;
  margin: 0 0 10px;
  color: #64748b;
  font-size: 13px;
}
.ai-warn { color: #b45309; font-size: 13px; margin: 8px 0 0; }
.wb-body {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 16px;
}
.wb-aside {
  width: 260px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  overflow-y: auto;
}
.aside-tip { font-size: 12px; color: #64748b; margin: 10px 0; }
.unit-name { font-size: 13px; font-weight: 700; color: #1e40af; margin: 10px 0 6px; }
.lesson-item {
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  background: #f8fafc;
  margin-bottom: 6px;
}
.lesson-item.active { background: #ede9fe; color: #5b4fc7; font-weight: 600; }
.wb-main {
  flex: 1;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px 20px;
  overflow-y: auto;
}
.wb-main.empty { display: flex; align-items: center; justify-content: center; }
.full { width: 100%; }
.tier-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}
.tier-card {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px;
  background: #f8fafc;
}
.tier-card h3 { margin: 0 0 8px; font-size: 14px; }
.mini-label { margin: 8px 0 4px; font-size: 12px; color: #64748b; }
.sub-title { margin: 16px 0 8px; font-size: 15px; }
.wb-actions { margin-top: 16px; display: flex; gap: 8px; }
.row-between { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.item-card {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 12px;
}
.item-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.trace-head { display: flex; justify-content: space-between; align-items: center; margin: 8px 0; }
.trace-editor { border-top: 1px dashed #e2e8f0; padding: 8px 0; }
.empty-hint { color: #94a3b8; }
@media (max-width: 1100px) {
  .tier-grid { grid-template-columns: 1fr; }
}
</style>
