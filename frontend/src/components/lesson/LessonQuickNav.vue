<template>
  <div class="lesson-quick-nav">
    <button
      v-for="item in items"
      :key="item.key"
      type="button"
      class="nav-chip"
      :class="[item.tone, { active: activeKey === item.key }]"
      @click="$emit('select', item.key)"
    >
      <span class="chip-icon" aria-hidden="true">{{ item.icon }}</span>
      <span class="chip-label">{{ item.label }}</span>
    </button>
  </div>
</template>

<script setup>
defineProps({
  activeKey: { type: String, default: 'intro' },
  items: {
    type: Array,
    default: () => ([
      { key: 'intro', label: '课程介绍', icon: '📖', tone: 'purple' },
      { key: 'records', label: '学习记录', icon: '📋', tone: 'blue' },
      { key: 'ranking', label: '积分榜', icon: '🏆', tone: 'orange' }
    ])
  }
})

defineEmits(['select'])
</script>

<style scoped>
.lesson-quick-nav {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 12px 10px;
}
.nav-chip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 8px;
  border: none;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
}
.nav-chip:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
}
.nav-chip.purple {
  background: linear-gradient(180deg, #f5f3ff 0%, #ede9fe 100%);
  color: #5b4fc7;
}
.nav-chip.blue {
  background: linear-gradient(180deg, #eff6ff 0%, #dbeafe 100%);
  color: #2563eb;
}
.nav-chip.orange {
  background: linear-gradient(180deg, #fff7ed 0%, #ffedd5 100%);
  color: #ea580c;
}
.nav-chip.green {
  background: linear-gradient(180deg, #f0fdf4 0%, #dcfce7 100%);
  color: #16a34a;
}
.nav-chip.active {
  outline: 2px solid currentColor;
  outline-offset: 1px;
  box-shadow: 0 4px 14px rgba(91, 79, 199, 0.18);
}
.chip-icon {
  font-size: 15px;
  line-height: 1;
}
.chip-label {
  white-space: nowrap;
}
</style>
