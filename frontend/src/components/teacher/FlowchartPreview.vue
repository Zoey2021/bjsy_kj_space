<template>
  <div class="fc-preview">
    <svg
      class="fc-svg"
      :viewBox="viewBox"
      preserveAspectRatio="xMidYMid meet"
    >
      <defs>
        <marker id="fc-arrow" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto">
          <polygon points="0 0, 8 3, 0 6" fill="#264653" />
        </marker>
      </defs>
      <path
        v-for="(p, i) in paths"
        :key="'p' + i"
        :d="p.d"
        fill="none"
        stroke="#264653"
        stroke-width="2.5"
        marker-end="url(#fc-arrow)"
      />
      <text
        v-for="(lb, i) in labels"
        :key="'l' + i"
        :x="lb.x"
        :y="lb.y"
        text-anchor="middle"
        font-size="12"
        fill="#2A9D8F"
        font-weight="700"
      >{{ lb.text }}</text>
      <g v-for="n in sizedNodes" :key="n.id">
        <rect
          v-if="n.type === 'start' || n.type === 'end'"
          :x="n.x" :y="n.y" :width="n.w" :height="n.h" rx="23"
          fill="#264653" stroke="#264653" stroke-width="2"
        />
        <rect
          v-else-if="n.type === 'process'"
          :x="n.x" :y="n.y" :width="n.w" :height="n.h" rx="4"
          fill="#E8F4F2" stroke="#2A9D8F" stroke-width="2"
        />
        <polygon
          v-else-if="n.type === 'io'"
          :points="ioPoints(n)"
          fill="#E3F2FD" stroke="#64B5F6" stroke-width="2"
        />
        <polygon
          v-else-if="n.type === 'decision'"
          :points="diamondPoints(n)"
          fill="#FEF9E7" stroke="#E9C46A" stroke-width="2"
        />
        <rect
          v-else
          :x="n.x" :y="n.y" :width="n.w" :height="n.h" rx="4"
          fill="#fff" stroke="#94a3b8" stroke-width="2"
        />
        <text
          v-for="(line, li) in wrapText(n.text, n.type === 'decision' ? 6 : 9)"
          :key="n.id + '-' + li"
          :x="n.x + n.w / 2"
          :y="n.y + n.h / 2 + (li - (wrapText(n.text, n.type === 'decision' ? 6 : 9).length - 1) / 2) * 14"
          text-anchor="middle"
          :fill="n.type === 'start' || n.type === 'end' ? '#fff' : '#264653'"
          font-size="12"
          font-weight="600"
        >{{ line }}</text>
      </g>
    </svg>
    <p v-if="!sizedNodes.length" class="fc-empty">暂无流程图数据</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  flowchart: { type: Object, default: () => ({ nodes: [], connections: [] }) }
})

const SIZE = {
  start: { w: 125, h: 46 },
  end: { w: 125, h: 46 },
  process: { w: 150, h: 52 },
  io: { w: 150, h: 48 },
  decision: { w: 120, h: 80 }
}

const sizedNodes = computed(() => (props.flowchart?.nodes || []).map((n) => {
  const sz = SIZE[n.type] || SIZE.process
  return { ...n, w: sz.w, h: sz.h }
}))

const nodeMap = computed(() => {
  const map = new Map()
  sizedNodes.value.forEach((n) => map.set(n.id, n))
  return map
})

const wrapText = (text, max = 9) => {
  const s = String(text || '')
  const lines = []
  let cur = ''
  for (const ch of s) {
    cur += ch
    if (cur.length >= max) {
      lines.push(cur)
      cur = ''
    }
  }
  if (cur) lines.push(cur)
  return lines.slice(0, 3)
}

const normPort = (port) => {
  if (port === 'in') return 'top'
  if (port === 'out') return 'bottom'
  if (port === 'out-yes') return 'left'
  if (port === 'out-no') return 'right'
  return port
}

const portPos = (node, portSide) => {
  const side = normPort(portSide)
  const cx = node.x + node.w / 2
  const cy = node.y + node.h / 2
  if (side === 'top') return { x: cx, y: node.y }
  if (side === 'bottom') return { x: cx, y: node.y + node.h }
  if (side === 'left') return { x: node.x, y: cy }
  if (side === 'right') return { x: node.x + node.w, y: cy }
  return { x: cx, y: cy }
}

const offsetPoint = (p, side, dist) => {
  const s = normPort(side)
  if (s === 'top') return { x: p.x, y: p.y - dist }
  if (s === 'bottom') return { x: p.x, y: p.y + dist }
  if (s === 'left') return { x: p.x - dist, y: p.y }
  if (s === 'right') return { x: p.x + dist, y: p.y }
  return p
}

const orthoD = (from, fp, to, tp) => {
  const GAP = 18
  const a = offsetPoint(from, fp, GAP)
  const b = offsetPoint(to, tp, GAP)
  const pts = [from, a]
  const loopBack = (to.y + 24 < from.y) && (fp === 'bottom' || tp === 'top' || tp === 'left')
  if (loopBack) {
    const sideX = Math.min(from.x, to.x) - 78
    pts.push({ x: sideX, y: a.y }, { x: sideX, y: b.y }, b, to)
  } else {
    const vFrom = fp === 'top' || fp === 'bottom'
    const vTo = tp === 'top' || tp === 'bottom'
    if (vFrom && vTo) {
      if (Math.abs(a.x - b.x) < 2) pts.push(b)
      else {
        const midY = Math.round((a.y + b.y) / 2)
        pts.push({ x: a.x, y: midY }, { x: b.x, y: midY }, b)
      }
    } else if (!vFrom && !vTo) {
      if (Math.abs(a.y - b.y) < 2) pts.push(b)
      else {
        const midX = Math.round((a.x + b.x) / 2)
        pts.push({ x: midX, y: a.y }, { x: midX, y: b.y }, b)
      }
    } else if (vFrom) pts.push({ x: a.x, y: b.y }, b)
    else pts.push({ x: b.x, y: a.y }, b)
    pts.push(to)
  }
  return pts.map((p, i) => `${i === 0 ? 'M' : 'L'} ${Math.round(p.x)} ${Math.round(p.y)}`).join(' ')
}

const connLabel = (fromNode, fromPort, explicit) => {
  if (explicit) return explicit
  if (fromNode?.type === 'decision') {
    if (normPort(fromPort) === 'left') return '是'
    if (normPort(fromPort) === 'right') return '否'
  }
  return ''
}

const paths = computed(() => {
  const map = nodeMap.value
  return (props.flowchart?.connections || []).map((c) => {
    const fromN = map.get(c.fromId)
    const toN = map.get(c.toId)
    if (!fromN || !toN) return { d: '' }
    const fromPort = normPort(c.fromPort)
    const toPort = normPort(c.toPort || 'top')
    const from = portPos(fromN, fromPort)
    const to = portPos(toN, toPort)
    return { d: orthoD(from, fromPort, to, toPort) }
  }).filter((p) => p.d)
})

const labels = computed(() => {
  const map = nodeMap.value
  return (props.flowchart?.connections || []).map((c) => {
    const fromN = map.get(c.fromId)
    const toN = map.get(c.toId)
    if (!fromN || !toN) return null
    const text = connLabel(fromN, c.fromPort, c.label)
    if (!text) return null
    const from = portPos(fromN, c.fromPort)
    const to = portPos(toN, c.toPort || 'top')
    return { x: (from.x + to.x) / 2, y: (from.y + to.y) / 2 - 8, text }
  }).filter(Boolean)
})

const viewBox = computed(() => {
  const nodes = sizedNodes.value
  if (!nodes.length) return '0 0 800 500'
  let minX = Infinity
  let minY = Infinity
  let maxX = 0
  let maxY = 0
  nodes.forEach((n) => {
    minX = Math.min(minX, n.x)
    minY = Math.min(minY, n.y)
    maxX = Math.max(maxX, n.x + n.w)
    maxY = Math.max(maxY, n.y + n.h)
  })
  const pad = 40
  return `${minX - pad} ${minY - pad} ${maxX - minX + pad * 2} ${maxY - minY + pad * 2}`
})

const ioPoints = (n) => {
  const s = 16
  return `${n.x + s},${n.y} ${n.x + n.w},${n.y} ${n.x + n.w - s},${n.y + n.h} ${n.x},${n.y + n.h}`
}

const diamondPoints = (n) => {
  const cx = n.x + n.w / 2
  const cy = n.y + n.h / 2
  return `${cx},${n.y} ${n.x + n.w},${cy} ${cx},${n.y + n.h} ${n.x},${cy}`
}
</script>

<style scoped>
.fc-preview {
  min-height: 62vh;
  background:
    radial-gradient(circle, #ddd 1px, transparent 1px);
  background-size: 20px 20px;
  background-color: #f8fafc;
  border-radius: 12px;
  overflow: auto;
}
.fc-svg {
  width: 100%;
  min-height: 62vh;
  display: block;
}
.fc-empty {
  text-align: center;
  color: #94a3b8;
  padding: 40px 0;
}
</style>
