<template>
  <div class="mall-grid">
    <button
      v-for="item in items"
      :key="item.id || item.code"
      type="button"
      class="gift-card"
      :class="{ locked: mode === 'apply' && !canClick(item) }"
      :disabled="mode === 'apply' && applying"
      :title="item.name"
      @click="onClick(item)"
    >
      <img v-if="mallIconSrc(item)" class="gift-art" :src="mallIconSrc(item)" :alt="item.name" />
      <span v-else class="gift-icon">{{ item.icon || '🎁' }}</span>
      <span v-if="mode === 'apply'" class="gift-action">{{ actionLabel(item) }}</span>
    </button>
  </div>
</template>

<script setup>
import { mallIconSrc } from '../../constants/mallIcons'

const props = defineProps({
  items: { type: Array, default: () => [] },
  mode: { type: String, default: 'display' },
  applyOpen: { type: Boolean, default: false },
  redeemablePoints: { type: Number, default: 0 },
  applying: { type: Boolean, default: false }
})

const emit = defineEmits(['apply'])

const canClick = (item) => props.applyOpen && props.redeemablePoints >= Number(item.cost || 0)

const actionLabel = (item) => {
  if (!props.applyOpen) return '待开放'
  if (props.redeemablePoints < Number(item.cost || 0)) return '积分不足'
  return '申请兑换'
}

const onClick = (item) => {
  if (props.mode !== 'apply' || !canClick(item)) return
  emit('apply', item)
}
</script>

<style scoped>
.mall-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(168px, 1fr));
  gap: 16px;
}
.gift-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 0;
  border: 0;
  border-radius: 22px;
  overflow: hidden;
  background: transparent;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.gift-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 28px rgba(15, 23, 42, 0.16);
}
.gift-card.locked {
  filter: grayscale(0.2);
  opacity: 0.88;
}
.gift-art {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  display: block;
}
.gift-icon {
  font-size: 42px;
  line-height: 1;
  padding: 36px 0;
  text-align: center;
}
.gift-action {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 8px 10px;
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(180deg, transparent, rgba(15, 23, 42, 0.72));
}
</style>
