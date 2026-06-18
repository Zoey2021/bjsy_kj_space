<template>
  <div class="directory-panel">
    <p v-if="tree.description" class="desc">{{ tree.description }}</p>
    <p v-else class="desc">共 {{ totalLessons }} 课时，点击课时卡片进入{{ mode === 'teacher' ? '查看' : '学习' }}</p>

    <template v-if="tree.units && tree.units.length">
      <section
        v-for="(unit, idx) in tree.units"
        :key="unit.id"
        class="unit-block"
      >
        <div class="unit-head">
          <span class="unit-index">{{ idx + 1 }}</span>
          <h4>{{ unit.name }}</h4>
          <span class="unit-count">{{ unit.lessons?.length || 0 }} 课</span>
        </div>
        <div class="lesson-grid">
          <div
            v-for="lesson in unit.lessons"
            :key="lesson.id"
            class="lesson-card"
            :class="statusClass(lesson.status)"
            @click="onLessonClick(lesson)"
          >
            <div class="lesson-title">{{ lesson.title }}</div>
            <el-progress v-if="mode === 'student'" :percentage="lesson.progressPercent" :stroke-width="6" />
            <el-tag v-if="mode === 'student'" size="small" :type="tagType(lesson.status)">
              {{ statusText(lesson.status) }}
            </el-tag>
          </div>
        </div>
      </section>
    </template>
    <el-empty v-else description="本册教材单元与课时正在建设中，敬请期待" />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  tree: { type: Object, required: true },
  mode: { type: String, default: 'student' }
})

const emit = defineEmits(['lesson-click'])

const totalLessons = computed(() => {
  if (!props.tree?.units) return 0
  return props.tree.units.reduce((n, u) => n + (u.lessons?.length || 0), 0)
})

const onLessonClick = (lesson) => emit('lesson-click', lesson)

const statusClass = (s) => ({ NOT_STARTED: 'not-started', IN_PROGRESS: 'in-progress', COMPLETED: 'completed' }[s] || '')
const statusText = (s) => ({ NOT_STARTED: '未开始', IN_PROGRESS: '学习中', COMPLETED: '已完成' }[s] || s)
const tagType = (s) => ({ NOT_STARTED: 'info', IN_PROGRESS: 'warning', COMPLETED: 'success' }[s] || 'info')
</script>

<style scoped>
.desc {
  color: #64748b;
  margin: 4px 0 20px;
  font-size: 14px;
}
.unit-block {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 18px 20px 22px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04);
}
.unit-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px dashed #e2e8f0;
}
.unit-index {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #2563eb;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.unit-head h4 {
  margin: 0;
  flex: 1;
  font-size: 16px;
  color: #1e293b;
}
.unit-count {
  font-size: 13px;
  color: #64748b;
}
.lesson-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
}
.lesson-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px;
  cursor: pointer;
  background: #fafbfc;
  transition: transform 0.15s, box-shadow 0.15s;
}
.lesson-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(37, 99, 235, 0.1);
}
.lesson-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 10px;
  color: #334155;
  line-height: 1.4;
}
.lesson-card.in-progress { border-color: #fcd34d; }
.lesson-card.completed { border-color: #86efac; }
</style>
