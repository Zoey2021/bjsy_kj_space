<template>
  <div class="detail" v-loading="loading">
    <header class="meta" v-if="project">
      <button type="button" class="back" @click="router.push('/student/tech-space/scratch')">← 编程作品社区</button>
      <div class="title-row">
        <h1>{{ project.title }}</h1>
        <span v-if="project.featured" class="feat">老师推荐</span>
      </div>
      <div class="sub">
        <span>作者 {{ project.authorName || '同学' }}</span>
        <span v-if="project.className">· {{ project.className }}</span>
        <span>· 浏览 {{ formatCount(project.viewCount) }}</span>
        <span>· 点赞 {{ formatCount(project.likeCount) }}</span>
      </div>
    </header>

    <section class="player" v-if="project">
      <iframe
        v-if="project.playUrl"
        class="frame"
        :src="project.playUrl"
        allowfullscreen
        allow="autoplay; fullscreen"
        title="Scratch 在线体验"
      />
      <div v-else class="player-empty">
        这个作品没有可播放的 .sb3 文件。<br>
        请老师重新上传后再体验。
      </div>
    </section>

    <footer class="actions" v-if="project">
      <button
        type="button"
        class="like-btn"
        :class="{ on: project.liked }"
        :disabled="liking || !canLike"
        @click="toggleLike"
      >
        <span class="heart" aria-hidden="true">{{ project.liked ? '♥' : '♡' }}</span>
        {{ project.liked ? '已点赞' : '给作品点赞' }}
        <b>{{ formatCount(project.likeCount) }}</b>
      </button>
      <a
        v-if="project.playUrl"
        class="ext"
        :href="project.playUrl"
        target="_blank"
        rel="noopener"
      >新窗口打开</a>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getScratchProject, likeScratchProject } from '../../api'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const liking = ref(false)
const project = ref(null)

const canLike = computed(() => {
  const p = project.value
  if (!p) return false
  return p.published === true || p.status === 'published'
})

const formatCount = (n) => {
  const v = Number(n || 0)
  if (v >= 10000) return (v / 10000).toFixed(1).replace(/\.0$/, '') + '万'
  return String(v)
}

const load = async () => {
  loading.value = true
  try {
    const res = await getScratchProject(route.params.id)
    project.value = res.data
  } finally {
    loading.value = false
  }
}

const toggleLike = async () => {
  if (!project.value || liking.value || !canLike.value) return
  liking.value = true
  try {
    const res = await likeScratchProject(project.value.id)
    project.value = { ...project.value, ...res.data }
    ElMessage.success(res.data?.message || (res.data?.liked ? '已点赞' : '已取消点赞'))
  } catch (e) {
    // 错误提示由 request 拦截器统一弹出
  } finally {
    liking.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.detail {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: none;
  width: 100%;
  margin: 0;
  padding: 0 8px 10px;
  box-sizing: border-box;
  overflow: hidden;
}
.back {
  border: 0;
  background: transparent;
  color: #6366f1;
  font-weight: 700;
  padding: 0;
  cursor: pointer;
  margin-bottom: 4px;
}
.meta { flex: 0 0 auto; }
.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.title-row h1 {
  margin: 0;
  font-size: 20px;
  color: #0f172a;
}
.feat {
  font-size: 12px;
  font-weight: 700;
  color: #b45309;
  background: #ffedd5;
  border-radius: 999px;
  padding: 2px 10px;
}
.sub {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px 0;
}
.player {
  flex: 1 1 auto;
  min-height: 320px;
  background: #0f172a;
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  align-items: stretch;
}
.frame {
  width: 100%;
  height: 100%;
  border: 0;
  display: block;
  background: #0f172a;
}
.player-empty {
  color: #cbd5e1;
  padding: 48px;
  text-align: center;
  width: 100%;
  align-self: center;
}
.actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  flex: 0 0 auto;
  padding: 4px 2px 2px;
  background: #fff;
}
.like-btn {
  border: 1.5px solid #fdba74;
  background: #fff7ed;
  border-radius: 999px;
  padding: 10px 18px;
  font-weight: 800;
  cursor: pointer;
  color: #c2410c;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}
.like-btn .heart { font-size: 16px; line-height: 1; }
.like-btn b {
  font-weight: 800;
  min-width: 1.2em;
}
.like-btn:hover:not(:disabled) {
  background: #ffedd5;
  transform: translateY(-1px);
}
.like-btn.on {
  background: #ea580c;
  border-color: #ea580c;
  color: #fff;
}
.like-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.ext {
  color: #4f46e5;
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
}
</style>
