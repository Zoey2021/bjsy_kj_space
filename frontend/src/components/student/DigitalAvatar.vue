<template>
  <div class="doll" :class="figure === 'girl' ? 'is-girl' : 'is-boy'">
    <canvas ref="board" width="320" height="480" role="img" :aria-label="figure === 'girl' ? '女生形象' : '男生形象'" />
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { avatarLayers } from '../../utils/avatarLook'

const props = defineProps({
  figure: { type: String, default: 'boy' },
  hair: { type: String, default: '' },
  outfit: { type: String, default: '' },
  extra: { type: String, default: '' }
})

const board = ref(null)
const cache = new Map()
let seq = 0

const loadImage = (src) => {
  if (!src) return Promise.resolve(null)
  if (cache.has(src)) return cache.get(src)
  const pending = new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = () => reject(new Error(src))
    img.src = src
  })
  cache.set(src, pending)
  return pending
}

const WIDTH = 320
const HEIGHT = 480

const stamp = (ctx, baseImg, layerImg) => {
  const width = WIDTH
  const height = HEIGHT
  const tmp = document.createElement('canvas')
  tmp.width = width
  tmp.height = height
  const pen = tmp.getContext('2d', { willReadFrequently: true })
  pen.drawImage(baseImg, 0, 0, width, height)
  const base = pen.getImageData(0, 0, width, height).data
  pen.clearRect(0, 0, width, height)
  pen.drawImage(layerImg, 0, 0, width, height)
  const layer = pen.getImageData(0, 0, width, height).data
  const frame = ctx.getImageData(0, 0, width, height)
  const out = frame.data
  for (let i = 0; i < layer.length; i += 4) {
    if (layer[i + 3] < 40) continue
    const lum = (layer[i] + layer[i + 1] + layer[i + 2]) / 3
    const chroma = Math.max(layer[i], layer[i + 1], layer[i + 2]) - Math.min(layer[i], layer[i + 1], layer[i + 2])
    if (!(lum < 200 || chroma > 28)) continue
    const dist = Math.abs(layer[i] - base[i]) + Math.abs(layer[i + 1] - base[i + 1]) + Math.abs(layer[i + 2] - base[i + 2])
    if (dist <= 48) continue
    out[i] = layer[i]
    out[i + 1] = layer[i + 1]
    out[i + 2] = layer[i + 2]
    out[i + 3] = layer[i + 3]
  }
  ctx.putImageData(frame, 0, 0)
}

const draw = async () => {
  const token = ++seq
  const layers = avatarLayers(props.figure, props.hair, props.outfit, props.extra)
  let base
  let outfit
  let hair
  let extra
  try {
    ;[base, outfit, hair, extra] = await Promise.all([
      loadImage(layers.base),
      loadImage(layers.outfit),
      loadImage(layers.hair),
      loadImage(layers.extra)
    ])
  } catch {
    return
  }
  if (token !== seq || !board.value || !base) return
  const ctx = board.value.getContext('2d', { willReadFrequently: true })
  ctx.clearRect(0, 0, WIDTH, HEIGHT)
  ctx.drawImage(outfit || base, 0, 0, WIDTH, HEIGHT)
  if (hair) stamp(ctx, base, hair)
  if (extra) stamp(ctx, base, extra)
}

watch(() => [props.figure, props.hair, props.outfit, props.extra], draw)
onMounted(draw)
</script>

<style scoped>
.doll {
  width: 168px;
  line-height: 0;
}
canvas {
  width: 100%;
  height: auto;
  display: block;
}
</style>
