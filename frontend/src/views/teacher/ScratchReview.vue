<template>
  <div class="page" v-loading="loading">
    <header class="head">
      <button type="button" class="back" @click="router.push('/teacher/tech-world')">← 科技世界</button>
      <h1>编程作品社区</h1>
      <p>上传 .sb3 后立即出现在学生「科技空间 → 编程作品社区」，学生可在线玩耍并点赞。下方也可审核学生投稿。</p>
    </header>

    <section class="upload-card">
      <h2>上传并发布作品</h2>
      <el-form label-position="top" class="upload-form">
        <el-form-item label="作品名称" required>
          <el-input v-model="form.title" maxlength="40" show-word-limit placeholder="例如：小猫接苹果" />
        </el-form-item>
        <el-form-item label="Scratch 作品文件（.sb3）" required>
          <input type="file" accept=".sb3,application/octet-stream" @change="onFile" />
          <p class="hint">在 Scratch 中：文件 → 保存到电脑。上传后会立刻发布到学生社区。</p>
          <p v-if="form.fileName" class="file-name">已选：{{ form.fileName }}</p>
        </el-form-item>
        <el-button type="primary" :loading="publishing" @click="publish">上传并发布</el-button>
      </el-form>
    </section>

    <div class="tabs">
      <button
        v-for="t in tabs"
        :key="t.key"
        type="button"
        :class="{ on: status === t.key }"
        @click="changeStatus(t.key)"
      >
        {{ t.label }}
        <span v-if="counts[t.key] != null" class="n">{{ counts[t.key] }}</span>
      </button>
    </div>

    <div v-if="!list.length" class="empty">这一栏暂时没有作品。</div>

    <div v-for="item in list" :key="item.id" class="row">
      <div class="main">
        <h3>
          {{ item.title }}
          <span v-if="item.featured" class="tag">老师推荐</span>
        </h3>
        <p>
          {{ item.authorName || '同学' }}
          <span v-if="item.className"> · {{ item.className }}</span>
          <span> · {{ item.statusLabel }}</span>
          <span v-if="item.createdAt"> · {{ formatTime(item.createdAt) }}</span>
          <span v-if="item.likeCount != null"> · 点赞 {{ item.likeCount }}</span>
        </p>
        <p v-if="item.rejectReason" class="reason">退回原因：{{ item.rejectReason }}</p>
      </div>
      <div class="ops">
        <a
          v-if="item.id"
          class="link"
          :href="playHref(item)"
          target="_blank"
          rel="noopener"
        >试玩</a>
        <a v-if="item.id" class="link" :href="`/api/scratch/teacher/projects/${item.id}/sb3`" target="_blank" rel="noopener">下载</a>
        <el-button
          v-if="item.status === 'pending' || item.status === 'rejected' || item.status === 'withdrawn'"
          type="primary"
          size="small"
          :loading="busyId === item.id"
          @click="doReview(item, 'approve')"
        >发布</el-button>
        <el-button
          v-if="item.status === 'pending'"
          size="small"
          :loading="busyId === item.id"
          @click="reject(item)"
        >退回</el-button>
        <el-button
          v-if="item.status === 'published'"
          type="warning"
          size="small"
          :loading="busyId === item.id"
          @click="doReview(item, 'withdraw')"
        >撤回</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  teacherScratchCounts,
  teacherScratchList,
  teacherScratchPublish,
  teacherScratchReview
} from '../../api'

const router = useRouter()
const loading = ref(false)
const publishing = ref(false)
const busyId = ref(null)
const status = ref('published')
const list = ref([])
const counts = ref({})
const form = reactive({ title: '', file: null, fileName: '' })
const tabs = [
  { key: 'published', label: '已发布' },
  { key: 'pending', label: '待审核' },
  { key: 'rejected', label: '已退回' },
  { key: 'withdrawn', label: '已撤回' }
]

const formatTime = (v) => (v ? String(v).replace('T', ' ').slice(0, 19) : '')

const playHref = (item) => {
  // 一律走后端 API；播放器会带上本页登录 token
  const sb3 = item.status === 'published'
    ? `/api/scratch/projects/${item.id}/sb3`
    : `/api/scratch/teacher/projects/${item.id}/sb3`
  return `/scratch-player/index.html?sb3=${encodeURIComponent(sb3)}&fit=1`
}

const onFile = (e) => {
  const f = e.target.files && e.target.files[0]
  form.file = f || null
  form.fileName = f ? f.name : ''
}

const load = async () => {
  loading.value = true
  try {
    const [listRes, countRes] = await Promise.all([
      teacherScratchList(status.value),
      teacherScratchCounts()
    ])
    list.value = listRes.data || []
    counts.value = countRes.data || {}
  } finally {
    loading.value = false
  }
}

const changeStatus = async (next) => {
  status.value = next
  await load()
}

const publish = async () => {
  if (!form.title.trim()) {
    ElMessage.warning('请填写作品名称')
    return
  }
  if (!form.file) {
    ElMessage.warning('请选择 .sb3 文件')
    return
  }
  publishing.value = true
  try {
    const res = await teacherScratchPublish(form.title.trim(), form.file)
    ElMessage.success(res.data?.message || '已发布到编程作品社区')
    form.title = ''
    form.file = null
    form.fileName = ''
    status.value = 'published'
    await load()
  } finally {
    publishing.value = false
  }
}

const doReview = async (item, action) => {
  busyId.value = item.id
  try {
    const res = await teacherScratchReview(item.id, { action })
    ElMessage.success(res.data?.message || '已处理')
    await load()
  } finally {
    busyId.value = null
  }
}

const reject = async (item) => {
  let reason = ''
  try {
    const { value } = await ElMessageBox.prompt('可填写退回原因（选填）', '退回作品', {
      confirmButtonText: '确认退回',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：请补充说明或重新导出 sb3'
    })
    reason = value || ''
  } catch {
    return
  }
  busyId.value = item.id
  try {
    const res = await teacherScratchReview(item.id, { action: 'reject', reason })
    ElMessage.success(res.data?.message || '已退回')
    await load()
  } finally {
    busyId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.page { max-width: 920px; margin: 0 auto; padding: 8px 4px 28px; }
.back {
  border: 0; background: transparent; color: #6366f1; font-weight: 700;
  padding: 0; cursor: pointer; margin-bottom: 6px;
}
.head h1 { margin: 0 0 6px; font-size: 22px; color: #1e1b4b; }
.head p { margin: 0 0 14px; color: #64748b; font-size: 13px; line-height: 1.6; }
.upload-card {
  background: #fff;
  border: 1px solid #e9e5ff;
  border-radius: 16px;
  padding: 16px 18px 18px;
  margin-bottom: 16px;
  box-shadow: 0 8px 24px rgba(49, 46, 129, 0.06);
}
.upload-card h2 {
  margin: 0 0 10px;
  font-size: 16px;
  color: #312e81;
}
.upload-form { max-width: 520px; }
.hint { margin: 6px 0 0; font-size: 12px; color: #94a3b8; line-height: 1.55; }
.file-name { margin: 6px 0 0; font-size: 13px; color: #4f46e5; font-weight: 600; }
.tabs { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; }
.tabs button {
  border: 1px solid #e2e8f0; background: #fff; border-radius: 999px;
  padding: 6px 12px; cursor: pointer; font-weight: 700; color: #64748b;
}
.tabs button.on { background: #eef2ff; border-color: #818cf8; color: #3730a3; }
.n {
  margin-left: 4px; background: #fee2e2; color: #b91c1c;
  border-radius: 999px; padding: 0 6px; font-size: 12px;
}
.tabs button.on .n { background: #c7d2fe; color: #3730a3; }
.empty {
  padding: 36px; text-align: center; color: #94a3b8;
  background: #f8fafc; border-radius: 14px;
}
.row {
  display: flex; gap: 12px; justify-content: space-between; align-items: center;
  flex-wrap: wrap; background: #fff; border: 1px solid #e9e5ff;
  border-radius: 14px; padding: 14px 16px; margin-bottom: 10px;
}
.main h3 { margin: 0 0 4px; font-size: 16px; color: #312e81; display: flex; align-items: center; gap: 8px; }
.tag {
  font-size: 11px; font-weight: 700; color: #b45309; background: #ffedd5;
  border-radius: 999px; padding: 2px 8px;
}
.main p { margin: 0; color: #64748b; font-size: 13px; }
.reason { color: #b91c1c !important; margin-top: 4px !important; }
.ops { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.link { color: #4f46e5; font-size: 13px; font-weight: 700; text-decoration: none; }
</style>
