<template>
  <div class="pbl-page">
    <header class="pbl-header">
      <div class="header-left">
        <el-button link class="back-btn" @click="router.push('/teacher/course-map')">← 返回课程地图</el-button>
        <div>
          <h1>项目化学习 · 教师工作台</h1>
          <p>立项配置、任务规划、资源准备与学生招募</p>
        </div>
      </div>
      <div class="header-actions">
        <el-tag v-if="project.recruitment.published" type="success" effect="plain">已发布至学生端</el-tag>
        <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
        <el-button type="success" :disabled="!canPublish" @click="handlePublish">发布项目说明</el-button>
      </div>
    </header>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <section class="pbl-card">
          <h2>① 项目立项</h2>
          <el-form label-position="top" class="pbl-form">
            <el-form-item label="项目主题">
              <el-input v-model="project.theme" placeholder="例如：校园垃圾分类智慧改造" maxlength="80" show-word-limit />
            </el-form-item>
            <el-form-item label="驱动性问题">
              <el-input
                v-model="project.drivingQuestion"
                type="textarea"
                :rows="3"
                placeholder="用一个问题引领整项探究，例如：如何让校园垃圾分类更智能、更高效？"
              />
            </el-form-item>
            <el-form-item label="跨学科融合点">
              <el-input
                v-model="project.interdisciplinary"
                type="textarea"
                :rows="2"
                placeholder="例如：信息科技 + 科学（传感器）+ 数学（数据统计）+ 语文（宣传文案）"
              />
            </el-form-item>
            <el-form-item label="课时周期">
              <el-input v-model="project.period" placeholder="例如：6 课时 / 3 周" />
            </el-form-item>
            <el-form-item label="实施场地">
              <el-radio-group v-model="project.venue">
                <el-radio value="campus">校内</el-radio>
                <el-radio value="offsite">校外实践基地</el-radio>
              </el-radio-group>
              <el-input
                v-model="project.venueDetail"
                class="venue-detail"
                :placeholder="project.venue === 'campus' ? '如：创客教室、图书馆、校园花园' : '如：科技馆、湿地实践基地'"
              />
            </el-form-item>
          </el-form>
        </section>

        <section class="pbl-card">
          <h2>② 任务与评价</h2>
          <el-form label-position="top" class="pbl-form">
            <el-form-item label="项目任务总表">
              <div class="task-table">
                <div class="task-head">
                  <span>任务名称</span>
                  <span>负责人/小组</span>
                  <span>完成节点</span>
                  <span />
                </div>
                <div v-for="(task, idx) in project.tasks" :key="idx" class="task-row">
                  <el-input v-model="task.name" placeholder="任务名称" />
                  <el-input v-model="task.owner" placeholder="负责人" />
                  <el-input v-model="task.deadline" placeholder="节点" />
                  <el-button link type="danger" :disabled="project.tasks.length <= 1" @click="removeTask(idx)">删除</el-button>
                </div>
                <el-button class="add-row-btn" @click="addTask">+ 添加任务</el-button>
              </div>
            </el-form-item>
            <el-form-item label="预期成果">
              <el-input
                v-model="project.outcomes"
                type="textarea"
                :rows="3"
                placeholder="例如：调研报告、原型模型、宣传海报、汇报 PPT 等"
              />
            </el-form-item>
            <el-form-item label="评价量规">
              <el-input
                v-model="project.rubric"
                type="textarea"
                :rows="4"
                placeholder="从过程参与、合作交流、成果质量、创新思维等维度描述评价标准"
              />
            </el-form-item>
          </el-form>
        </section>
      </el-col>

      <el-col :xs="24" :lg="12">
        <section class="pbl-card">
          <h2>③ 配套资源</h2>
          <el-form label-position="top" class="pbl-form">
            <el-form-item label="学习单">
              <el-input v-model="project.resources.worksheet" type="textarea" :rows="2" placeholder="学习单说明或链接" />
            </el-form-item>
            <el-form-item label="调研记录表">
              <el-input v-model="project.resources.researchLog" type="textarea" :rows="2" placeholder="调研记录表说明或链接" />
            </el-form-item>
            <el-form-item label="工具素材">
              <el-input v-model="project.resources.tools" type="textarea" :rows="2" placeholder="所需工具、模板、素材包等" />
            </el-form-item>
            <el-form-item label="外出实践报备材料">
              <el-input v-model="project.resources.offsitePrep" type="textarea" :rows="2" placeholder="校外实践所需报备清单与注意事项" />
            </el-form-item>
          </el-form>
        </section>

        <section class="pbl-card">
          <h2>④ 学生招募 / 选课</h2>
          <el-form label-position="top" class="pbl-form">
            <el-form-item label="参与人数上限">
              <el-input-number v-model="project.recruitment.maxParticipants" :min="1" :max="200" />
            </el-form-item>
            <el-form-item label="当前参与人数">
              <el-input-number v-model="project.recruitment.enrolledCount" :min="0" :max="project.recruitment.maxParticipants" />
            </el-form-item>
            <el-form-item label="项目任务说明（发布给学生）">
              <el-input
                v-model="project.recruitment.description"
                type="textarea"
                :rows="5"
                placeholder="面向学生的项目简介、招募要求、时间安排与注意事项"
              />
            </el-form-item>
          </el-form>
        </section>

        <section class="pbl-card">
          <h2>⑤ 学生端阶段内容（可选）</h2>
          <p class="card-hint">学生端五个阶段框的内容可在此预先填写，后续也可继续补充。</p>
          <el-collapse>
            <el-collapse-item v-for="phase in PBL_PHASES" :key="phase.key" :title="phase.title" :name="phase.key">
              <el-input
                v-model="project.phases[phase.key].content"
                type="textarea"
                :rows="4"
                :placeholder="`${phase.title}的具体指引、任务清单或链接`"
              />
            </el-collapse-item>
          </el-collapse>
        </section>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { PBL_PHASES } from '../../constants/pblModule'
import { loadPblProject, savePblProject } from '../../utils/pblStorage'

const router = useRouter()
const saving = ref(false)
const project = reactive(loadPblProject())

onMounted(() => {
  Object.assign(project, loadPblProject())
})

const canPublish = computed(() => Boolean(project.theme.trim() && project.recruitment.description.trim()))

const addTask = () => {
  project.tasks.push({ name: '', owner: '', deadline: '' })
}

const removeTask = (idx) => {
  project.tasks.splice(idx, 1)
}

const handleSave = async () => {
  saving.value = true
  try {
    savePblProject(project)
    ElMessage.success('项目配置已保存')
  } finally {
    saving.value = false
  }
}

const handlePublish = () => {
  project.recruitment.published = true
  savePblProject(project)
  ElMessage.success('项目说明已发布，学生可在项目化学习界面查看')
}
</script>

<style scoped>
.pbl-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 4px 4px 32px;
}

.pbl-header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
  padding: 18px 20px;
  border-radius: 16px;
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 55%, #f0fdf4 100%);
  border: 1px solid #99f6e4;
}

.header-left h1 {
  margin: 6px 0 4px;
  font-size: 24px;
  color: #065f46;
}

.header-left p {
  margin: 0;
  color: #047857;
  font-size: 14px;
}

.back-btn {
  color: #0f766e !important;
  padding-left: 0;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.pbl-card {
  background: #fff;
  border: 1px solid #d1fae5;
  border-radius: 16px;
  padding: 18px 20px 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(13, 148, 136, 0.06);
}

.pbl-card h2 {
  margin: 0 0 14px;
  font-size: 17px;
  color: #065f46;
}

.card-hint {
  margin: -6px 0 12px;
  font-size: 13px;
  color: #64748b;
}

.pbl-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #334155;
}

.venue-detail {
  margin-top: 10px;
}

.task-table {
  width: 100%;
}

.task-head,
.task-row {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr 56px;
  gap: 8px;
  align-items: center;
}

.task-head {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}

.add-row-btn {
  margin-top: 10px;
}

@media (max-width: 768px) {
  .task-head {
    display: none;
  }
  .task-row {
    grid-template-columns: 1fr;
  }
}
</style>
