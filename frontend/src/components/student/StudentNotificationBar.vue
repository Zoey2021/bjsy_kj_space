<template>
  <transition name="notify-slide">
    <div v-if="visible" class="notify-bar" :class="'notify-' + (item?.type || 'remind')">
      <div class="notify-body" @click="onClick">
        <span class="notify-icon">{{ icon }}</span>
        <div class="notify-text">
          <strong>{{ title }}</strong>
          <p>{{ item?.message || item?.data?.message }}</p>
        </div>
      </div>
      <button type="button" class="notify-close" aria-label="关闭" @click="$emit('close')">×</button>
    </div>
  </transition>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  item: { type: Object, default: null },
  visible: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'action'])

const title = computed(() => {
  const t = props.item?.type || props.item?.data?.type
  if (t === 'guide') return '学习建议'
  if (t === 'broadcast') return '全班提醒'
  if (t === 'park_approve') return '游学乐园已开启'
  if (t === 'park_reject') return '游学乐园申请未通过'
  if (t === 'park_revoke') return '游学乐园已关闭'
  return '教师提醒'
})

const icon = computed(() => {
  const t = props.item?.type || props.item?.data?.type
  if (t === 'guide') return '💡'
  if (t === 'broadcast') return '📢'
  if (t && String(t).startsWith('park_')) return '🎮'
  return '🔔'
})

const onClick = () => {
  emit('action', props.item)
}
</script>

<style scoped>
.notify-bar {
  display: flex;
  align-items: stretch;
  gap: 8px;
  margin: 0 0 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid rgba(79, 70, 229, 0.18);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.9);
}
.notify-remind { border-left: 4px solid #f59e0b; }
.notify-guide { border-left: 4px solid #6366f1; }
.notify-broadcast { border-left: 4px solid #ef4444; }
.notify-park_approve { border-left: 4px solid #10b981; }
.notify-park_reject,
.notify-park_revoke { border-left: 4px solid #f97316; }
.notify-body {
  flex: 1;
  display: flex;
  gap: 12px;
  cursor: pointer;
  min-width: 0;
}
.notify-icon { font-size: 22px; line-height: 1; }
.notify-text strong {
  display: block;
  font-size: 14px;
  color: #1e293b;
  margin-bottom: 4px;
}
.notify-text p {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: #64748b;
}
.notify-close {
  border: none;
  background: transparent;
  color: #94a3b8;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  padding: 0 4px;
}
.notify-close:hover { color: #475569; }
.notify-slide-enter-active,
.notify-slide-leave-active {
  transition: all 0.28s ease;
}
.notify-slide-enter-from,
.notify-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
