<template>
  <div class="pdf-viewer">
    <div class="toolbar">
      <el-button :disabled="currentPage <= 1 || loading" @click="goPrev">上一页</el-button>
      <span class="page-info">
        第
        <el-input-number
          v-model="currentPage"
          :min="1"
          :max="totalPages || 9999"
          :disabled="loading"
          controls-position="right"
          size="small"
          class="page-input"
          @change="onPageInput"
        />
        页<span v-if="totalPages"> / 共 {{ totalPages }} 页</span>
      </span>
      <el-button :disabled="totalPages ? currentPage >= totalPages : false" @click="goNext">下一页</el-button>
      <span class="zoom-hint">可在下方阅读区使用浏览器自带缩放</span>
    </div>

    <div v-loading="loading" class="frame-wrap">
      <iframe
        v-if="iframeSrc && !error"
        :key="iframeSrc"
        :src="iframeSrc"
        class="pdf-frame"
        title="电子教材"
      />
      <el-empty v-if="!loading && error" :description="error" :image-size="72" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'

const props = defineProps({
  src: { type: String, default: '' },
  initialPage: { type: Number, default: 1 }
})

const currentPage = ref(1)
const totalPages = ref(0)
const loading = ref(false)
const error = ref('')

let pdfDoc = null

const iframeSrc = computed(() => {
  if (!props.src || error.value) return ''
  const base = props.src.split('#')[0]
  return `${base}#page=${currentPage.value}`
})

async function loadPageCount(url) {
  try {
    const pdfjs = await import('pdfjs-dist')
    pdfjs.GlobalWorkerOptions.workerSrc = '/pdfjs/pdf.worker.min.mjs'
    pdfDoc = await pdfjs.getDocument({ url, withCredentials: false }).promise
    totalPages.value = pdfDoc.numPages
    const start = Math.min(Math.max(1, props.initialPage), totalPages.value)
    currentPage.value = start
  } catch {
    totalPages.value = 0
  }
}

async function loadPdf(url) {
  destroyPdf()
  if (!url) {
    error.value = '本册尚未配置电子教材 PDF'
    return
  }
  loading.value = true
  error.value = ''
  currentPage.value = Math.max(1, props.initialPage)
  totalPages.value = 0
  try {
    const head = await fetch(url, { method: 'HEAD' })
    if (!head.ok) {
      if (head.status === 403) {
        throw new Error('FORBIDDEN')
      }
      if (head.status === 404) {
        throw new Error('NOT_FOUND')
      }
      throw new Error(`HTTP_${head.status}`)
    }
    await loadPageCount(url)
  } catch (e) {
    if (e?.message === 'FORBIDDEN') {
      error.value = 'PDF 无法访问（403）。请执行：chmod 644 frontend/public/textbooks/*.pdf 后刷新'
    } else if (e?.message === 'NOT_FOUND') {
      error.value = `未找到 ${url}，请确认 PDF 在 frontend/public/textbooks/ 目录中`
    } else {
      error.value = `PDF 无法打开（${url}），请检查文件与权限后刷新`
    }
  } finally {
    loading.value = false
  }
}

function goPrev() {
  if (currentPage.value > 1) currentPage.value -= 1
}

function goNext() {
  if (!totalPages.value || currentPage.value < totalPages.value) {
    currentPage.value += 1
  }
}

function onPageInput(val) {
  if (!val || val < 1) return
  if (totalPages.value && val > totalPages.value) {
    currentPage.value = totalPages.value
  }
}

function destroyPdf() {
  if (pdfDoc) {
    pdfDoc.destroy().catch(() => {})
    pdfDoc = null
  }
  totalPages.value = 0
}

watch(() => props.src, (url) => loadPdf(url), { immediate: true })

watch(
  () => props.initialPage,
  (p) => {
    if (!p || p < 1) return
    const max = totalPages.value || p
    currentPage.value = Math.min(p, max)
  }
)

onBeforeUnmount(destroyPdf)
</script>

<style scoped>
.pdf-viewer {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 480px;
  background: #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #cbd5e1;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}
.page-info {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #334155;
}
.page-input {
  width: 108px;
}
.zoom-hint {
  margin-left: auto;
  font-size: 12px;
  color: #94a3b8;
}
.frame-wrap {
  flex: 1;
  min-height: 360px;
  background: #cbd5e1;
}
.pdf-frame {
  width: 100%;
  height: 100%;
  min-height: 520px;
  border: none;
  display: block;
  background: #fff;
}
</style>
