<template>
  <div class="ai-page">
    <h2>AI 体验中心</h2>
    <el-tabs v-model="activeTab">
      <!-- TTS 语音合成 -->
      <el-tab-pane label="TTS 语音合成" name="tts">
        <el-card>
          <p>输入文字，点击播放，浏览器会朗读出来（使用 Web Speech API）</p>
          <el-input v-model="ttsText" type="textarea" :rows="4" placeholder="请输入要朗读的文字" />
          <el-button type="primary" style="margin-top:12px" @click="speak">播放朗读</el-button>
          <el-button @click="stopSpeak">停止</el-button>
        </el-card>
      </el-tab-pane>

      <!-- OCR 文字识别（简易版：使用 Tesseract.js 需额外安装，此处用 Canvas 演示流程） -->
      <el-tab-pane label="OCR 文字识别" name="ocr">
        <el-card>
          <p>上传图片，提取文字（演示版：识别图片中的简单文字区域）</p>
          <input type="file" accept="image/*" @change="onImageUpload" />
          <canvas ref="ocrCanvas" style="max-width:100%;margin-top:12px;border:1px solid #ddd"></canvas>
          <el-input v-model="ocrResult" type="textarea" :rows="3" placeholder="识别结果" style="margin-top:12px" />
          <p class="tip">提示：完整 OCR 可接入百度/腾讯云 API，此处为教学演示框架</p>
        </el-card>
      </el-tab-pane>

      <!-- 人脸姿态检测 -->
      <el-tab-pane label="人脸姿态检测" name="face">
        <el-card>
          <p>调用摄像头，实时显示画面（完整人脸检测可接入 face-api.js）</p>
          <video ref="videoRef" autoplay playsinline width="480" style="border-radius:8px;background:#000"></video>
          <br />
          <el-button type="primary" @click="startCamera" style="margin-top:12px">开启摄像头</el-button>
          <el-button @click="stopCamera">关闭摄像头</el-button>
          <p class="tip">提示：face-api.js 模型文件较大，此处演示摄像头调用流程</p>
        </el-card>
      </el-tab-pane>

      <!-- 算法可视化 -->
      <el-tab-pane label="算法可视化" name="algo">
        <el-card>
          <h4>冒泡排序演示</h4>
          <canvas ref="algoCanvas" width="600" height="200" style="border:1px solid #ddd"></canvas>
          <el-button type="primary" @click="startSort" style="margin-top:12px" :disabled="sorting">开始排序</el-button>
          <el-button @click="resetArray">重置</el-button>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'

const activeTab = ref('tts')
const ttsText = ref('欢迎来到信息科技学习空间！')
const ocrResult = ref('')
const ocrCanvas = ref(null)
const videoRef = ref(null)
const algoCanvas = ref(null)
let mediaStream = null
let array = []
let sorting = ref(false)

// TTS：浏览器内置语音合成
const speak = () => {
  if (!ttsText.value) return
  const utter = new SpeechSynthesisUtterance(ttsText.value)
  utter.lang = 'zh-CN'
  speechSynthesis.speak(utter)
}
const stopSpeak = () => speechSynthesis.cancel()

// OCR 演示：读取图片显示到 Canvas
const onImageUpload = (e) => {
  const file = e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (ev) => {
    const img = new Image()
    img.onload = () => {
      const canvas = ocrCanvas.value
      const ctx = canvas.getContext('2d')
      canvas.width = img.width
      canvas.height = img.height
      ctx.drawImage(img, 0, 0)
      ocrResult.value = '图片已加载。接入 OCR API 后可自动提取文字。'
    }
    img.src = ev.target.result
  }
  reader.readAsDataURL(file)
}

// 摄像头
const startCamera = async () => {
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({ video: true })
    videoRef.value.srcObject = mediaStream
  } catch {
    ElMessage.error('无法访问摄像头，请检查浏览器权限')
  }
}
const stopCamera = () => {
  if (mediaStream) {
    mediaStream.getTracks().forEach(t => t.stop())
    mediaStream = null
  }
}

// 冒泡排序可视化
const resetArray = () => {
  array = Array.from({ length: 20 }, () => Math.floor(Math.random() * 180) + 20)
  drawArray()
}
const drawArray = (highlight = []) => {
  const canvas = algoCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, 600, 200)
  const barW = 600 / array.length
  array.forEach((v, i) => {
    ctx.fillStyle = highlight.includes(i) ? '#f56c6c' : '#1a73e8'
    ctx.fillRect(i * barW, 200 - v, barW - 2, v)
  })
}
const startSort = async () => {
  sorting.value = true
  for (let i = 0; i < array.length; i++) {
    for (let j = 0; j < array.length - i - 1; j++) {
      drawArray([j, j + 1])
      await new Promise(r => setTimeout(r, 100))
      if (array[j] > array[j + 1]) {
        ;[array[j], array[j + 1]] = [array[j + 1], array[j]]
      }
    }
  }
  drawArray()
  sorting.value = false
}

onMounted(resetArray)
onUnmounted(stopCamera)
</script>

<style scoped>
.ai-page { max-width: 900px; margin: 0 auto; }
.tip { color: #999; font-size: 12px; margin-top: 8px; }
</style>
