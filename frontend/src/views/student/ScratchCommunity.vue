<template>
  <div class="community" v-loading="loading">
    <header class="head">
      <div>
        <button type="button" class="back" @click="router.push('/student/tech-space')">← 科技空间</button>
        <h1>编程作品社区</h1>
        <p>看看老师推荐和同学作品，点进去就能玩，给喜欢的作品点赞！</p>
      </div>
      <div class="head-actions">
        <div class="sort">
          <button type="button" :class="{ on: sort === 'hot' }" @click="changeSort('hot')">最受欢迎</button>
          <button type="button" :class="{ on: sort === 'new' }" @click="changeSort('new')">最新发布</button>
        </div>
        <el-button type="primary" @click="openPublish">发布我的作品</el-button>
      </div>
    </header>

    <section v-if="mine.length" class="mine">
      <h3>我的投稿</h3>
      <div class="mine-list">
        <div v-for="item in mine" :key="'m' + item.id" class="mine-row">
          <b>{{ item.title }}</b>
          <span class="st" :class="item.status">{{ item.statusLabel }}</span>
          <span v-if="item.status === 'rejected' && item.rejectReason" class="reason">{{ item.rejectReason }}</span>
        </div>
      </div>
    </section>

    <div v-if="!loading && !list.length" class="empty">还没有已发布的作品，审核通过后会出现在这里。</div>

    <div class="grid" role="list">
      <button
        v-for="item in list"
        :key="item.id"
        type="button"
        class="card"
        role="listitem"
        @click="open(item.id)"
      >
        <div class="cover-wrap">
          <div class="cover-fallback">Scratch</div>
          <img
            v-if="item.coverUrl"
            class="cover"
            :src="item.coverUrl"
            :alt="item.title"
            loading="lazy"
            @error="onCoverError"
          />
        </div>
        <div class="body">
          <h2>
            {{ item.title }}
            <span v-if="item.featured" class="feat">老师推荐</span>
          </h2>
          <div class="stats">
            <span title="浏览">浏览 {{ formatCount(item.viewCount) }}</span>
            <button
              type="button"
              class="like-chip"
              :class="{ on: item.liked }"
              :disabled="likingId === item.id"
              title="点赞"
              @click.stop="toggleLike(item)"
            >
              {{ item.liked ? '已赞' : '点赞' }} {{ formatCount(item.likeCount) }}
            </button>
          </div>
          <div class="author">
            <span class="avatar">{{ (item.authorName || '?').slice(0, 1) }}</span>
            <span class="name">{{ item.authorName || '同学' }}</span>
            <span v-if="item.className" class="klass">{{ item.className }}</span>
          </div>
        </div>
      </button>
    </div>

    <el-dialog v-model="showPublish" title="发布我的作品" width="480px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="作品名称" required>
          <el-input v-model="form.title" maxlength="40" show-word-limit placeholder="给你的作品取个名字" />
        </el-form-item>
        <el-form-item label="上传 Scratch 作品（.sb3）" required>
          <input type="file" accept=".sb3,application/octet-stream" @change="onFile" />
          <p class="hint">在 Scratch 中：文件 → 保存到电脑，得到 .sb3 后在这里上传。提交后需老师审核才会出现在社区。</p>
          <p v-if="form.fileName" class="file-name">已选：{{ form.fileName }}</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublish = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="publish">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  likeScratchProject,
  listMyScratchProjects,
  listScratchProjects,
  submitScratchProject
} from '../../api'

const router = useRouter()
const loading = ref(false)
const publishing = ref(false)
const showPublish = ref(false)
const likingId = ref(null)
const sort = ref('hot')
const list = ref([])
const mine = ref([])
const form = reactive({ title: '', file: null, fileName: '' })

const formatCount = (n) => {
  const v = Number(n || 0)
  if (v >= 10000) return (v / 10000).toFixed(1).replace(/\.0$/, '') + '万'
  return String(v)
}

const onCoverError = (e) => {
  e.target.style.display = 'none'
}

const load = async () => {
  loading.value = true
  try {
    const [pub, my] = await Promise.all([
      listScratchProjects(sort.value),
      listMyScratchProjects().catch(() => ({ data: [] }))
    ])
    list.value = pub.data || []
    mine.value = my.data || []
  } finally {
    loading.value = false
  }
}

const changeSort = async (next) => {
  if (sort.value === next) return
  sort.value = next
  await load()
}

const open = (id) => router.push(`/student/tech-space/scratch/${id}`)

const toggleLike = async (item) => {
  if (!item?.id || likingId.value === item.id) return
  likingId.value = item.id
  try {
    const res = await likeScratchProject(item.id)
    const liked = !!res.data?.liked
    const likeCount = Number(res.data?.likeCount ?? item.likeCount ?? 0)
    item.liked = liked
    item.likeCount = likeCount
    const idx = list.value.findIndex((x) => x.id === item.id)
    if (idx >= 0) {
      list.value[idx] = { ...list.value[idx], liked, likeCount }
    }
    ElMessage.success(res.data?.message || (liked ? '已点赞' : '已取消点赞'))
  } catch (e) {
    // 错误由 request 拦截器提示
  } finally {
    likingId.value = null
  }
}

const openPublish = () => {
  form.title = ''
  form.file = null
  form.fileName = ''
  showPublish.value = true
}

const onFile = (e) => {
  const f = e.target.files && e.target.files[0]
  form.file = f || null
  form.fileName = f ? f.name : ''
}

const publish = async () => {
  if (!String(form.title || '').trim()) {
    ElMessage.warning('请先为作品取名字')
    return
  }
  if (!form.file) {
    ElMessage.warning('请上传 .sb3 作品文件')
    return
  }
  publishing.value = true
  try {
    await submitScratchProject(form.title.trim(), form.file)
    ElMessage.success('已提交，等待老师审核')
    showPublish.value = false
    await load()
  } finally {
    publishing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.community {
  height: 100%;
  min-height: 0;
  overflow: auto;
  max-width: 1100px;
  width: 100%;
  margin: 0 auto;
  padding: 4px 4px 28px;
  box-sizing: border-box;
}
.head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-end;
  flex-wrap: wrap;
  margin-bottom: 18px;
}
.back {
  border: 0;
  background: transparent;
  color: #6366f1;
  font-weight: 700;
  padding: 0;
  cursor: pointer;
  margin-bottom: 6px;
}
.head h1 {
  margin: 0 0 6px;
  font-size: 26px;
  color: #1e1b4b;
}
.head p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
}
.head-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}
.sort {
  display: inline-flex;
  background: #f1f5f9;
  border-radius: 999px;
  padding: 3px;
}
.sort button {
  border: 0;
  background: transparent;
  border-radius: 999px;
  padding: 6px 12px;
  cursor: pointer;
  color: #64748b;
  font-weight: 700;
  font-size: 13px;
}
.sort button.on {
  background: #fff;
  color: #4338ca;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.08);
}
.mine {
  margin-bottom: 16px;
  background: #f8fafc;
  border-radius: 14px;
  padding: 12px 14px;
}
.mine h3 { margin: 0 0 8px; font-size: 14px; color: #334155; }
.mine-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  padding: 6px 0;
  border-top: 1px solid #e2e8f0;
  font-size: 13px;
}
.mine-row:first-of-type { border-top: 0; }
.st {
  font-size: 12px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #475569;
}
.st.pending { background: #ffedd5; color: #c2410c; }
.st.published { background: #dcfce7; color: #15803d; }
.st.rejected { background: #fee2e2; color: #b91c1c; }
.st.withdrawn { background: #e2e8f0; color: #64748b; }
.reason { color: #b91c1c; }
.empty {
  padding: 48px;
  text-align: center;
  color: #94a3b8;
  background: #f8fafc;
  border-radius: 16px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}
.card {
  border: 1px solid #e2e8f0;
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  padding: 0;
  text-align: left;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
}
.cover-wrap {
  position: relative;
  aspect-ratio: 4 / 3;
  background: #e2e8f0;
}
.cover-fallback {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: #64748b;
  font-weight: 800;
  letter-spacing: 0.08em;
}
.cover {
  position: relative;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.body { padding: 10px 12px 12px; }
.body h2 {
  margin: 0 0 8px;
  font-size: 15px;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: flex;
  align-items: center;
  gap: 6px;
}
.feat {
  flex: 0 0 auto;
  font-size: 11px;
  font-weight: 700;
  color: #b45309;
  background: #ffedd5;
  border-radius: 999px;
  padding: 2px 7px;
}
.stats {
  display: flex;
  gap: 10px;
  align-items: center;
  color: #94a3b8;
  font-size: 12px;
  margin-bottom: 10px;
}
.like-chip {
  border: 1px solid #fdba74;
  background: #fff7ed;
  color: #c2410c;
  border-radius: 999px;
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
  font-family: inherit;
}
.like-chip.on {
  background: #ea580c;
  border-color: #ea580c;
  color: #fff;
}
.like-chip:disabled {
  opacity: 0.6;
  cursor: wait;
}
.author {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.avatar {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #c7d2fe;
  color: #3730a3;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 800;
  flex: none;
}
.name {
  font-size: 12px;
  color: #334155;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.klass {
  margin-left: auto;
  font-size: 11px;
  color: #94a3b8;
  flex: none;
}
.hint {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}
.file-name { margin: 6px 0 0; color: #4338ca; font-size: 12px; font-weight: 700; }
@media (max-width: 1000px) {
  .grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 720px) {
  .grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .head h1 { font-size: 22px; }
}
</style>
