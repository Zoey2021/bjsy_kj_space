<template>
  <div class="points-page">
    <header class="pts-header">
      <div class="header-main">
        <h1><span class="title-icon">🏆</span> 课堂积分</h1>
        <p>完成环节得积分，查看班级排行、设置规则，并管理兑换商城申请。上课前可在排行榜重置本班积分。</p>
      </div>
    </header>

    <div class="pts-body">
      <div class="panel">
        <el-tabs v-model="tab" class="soft-tabs">
          <el-tab-pane label="积分排行榜" name="ranking">
            <Ranking embedded />
          </el-tab-pane>
          <el-tab-pane label="积分规则" name="rules">
            <div class="rule-card">
              <p class="rule-lead">当前可设置每个探究环节的积分，以及完整完成拓展任务的额外奖励。</p>
              <div class="rule-row">
                <label>每个环节</label>
                <el-input-number v-model="rules.pointsPerActivity" :min="0" :max="20" />
                <span>分</span>
              </div>
              <div class="rule-row">
                <label>完整拓展任务额外</label>
                <el-input-number v-model="rules.extensionBonus" :min="0" :max="10" />
                <span>分</span>
              </div>
              <button type="button" class="action-btn indigo" :disabled="savingRules" @click="saveRules">
                {{ savingRules ? '保存中…' : '保存规则' }}
              </button>
              <el-alert type="info" show-icon :closable="false" style="margin-top:16px">
                学生每完成 1 个探究环节获得「每个环节」积分；若该环节是拓展任务且完整完成，再额外获得上列分数。课堂小测与学习评价按同一环节分计。
              </el-alert>
            </div>
          </el-tab-pane>
          <el-tab-pane label="积分商城" name="shop">
            <div class="shop-toolbar">
              <el-select v-model="shopClassId" placeholder="选择班级" filterable style="width:220px" @change="loadMall">
                <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
              <button
                type="button"
                class="action-btn"
                :class="mall.applyOpen ? 'orange' : 'indigo'"
                :disabled="!shopClassId || toggling"
                @click="toggleOpen"
              >
                {{ toggling ? '处理中…' : (mall.applyOpen ? '关闭申请' : '开放申请') }}
              </button>
              <span class="open-hint" :class="{ on: mall.applyOpen }">
                {{ mall.applyOpen ? '学生可在兑换商城申请' : '学生暂不能申请兑换' }}
              </span>
            </div>
            <p class="shop-note">按一学期 18 节课、每节大约 11～15 分来定价。零食、文具、表扬信是大多数同学期末能换到的；奶茶留给积分更高的同学。游学乐园每分钟消耗 2 积分，从可兑积分里扣，星星和等级不降。</p>
            <MallGiftGrid :items="mall.items || []" />
            <h3 class="sec-title">本班兑换申请</h3>
            <el-table :data="mall.orders || []" stripe empty-text="该班暂无兑换申请" class="soft-table">
              <el-table-column prop="studentName" label="学生" width="120" />
              <el-table-column label="奖品" min-width="180">
                <template #default="{ row }">
                  <span class="order-item">
                    <img v-if="mallIconSrc(row)" class="order-thumb" :src="mallIconSrc(row)" :alt="row.itemName" />
                    <span>{{ row.itemName }}</span>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="points" label="扣除积分" width="110" />
              <el-table-column label="状态" width="100">
                <template #default>已申请</template>
              </el-table-column>
              <el-table-column label="时间" min-width="170">
                <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import Ranking from './Ranking.vue'
import MallGiftGrid from '../../components/points/MallGiftGrid.vue'
import { mallIconSrc } from '../../constants/mallIcons'
import { getClasses, getTeacherMall, getTeacherPointsRules, saveTeacherPointsRules, setMallApplyOpen } from '../../api'

const route = useRoute()
const router = useRouter()
const tab = ref(route.query.tab || 'ranking')
const classes = ref([])
const shopClassId = ref(null)
const savingRules = ref(false)
const toggling = ref(false)
const mall = ref({})
const rules = reactive({ pointsPerActivity: 2, extensionBonus: 1 })

watch(tab, (v) => {
  router.replace({ query: { tab: v } })
})
watch(() => route.query.tab, (v) => {
  if (v) tab.value = v
})

const formatTime = (v) => (v ? String(v).replace('T', ' ').slice(0, 19) : '—')

const loadRules = async () => {
  const res = await getTeacherPointsRules().catch(() => ({ data: {} }))
  rules.pointsPerActivity = Number(res.data?.pointsPerActivity ?? 2)
  rules.extensionBonus = Number(res.data?.extensionBonus ?? 1)
}

const saveRules = async () => {
  savingRules.value = true
  try {
    await saveTeacherPointsRules({
      pointsPerActivity: rules.pointsPerActivity,
      extensionBonus: rules.extensionBonus
    })
    ElMessage.success('规则已保存')
  } finally {
    savingRules.value = false
  }
}

const loadMall = async () => {
  const res = await getTeacherMall(shopClassId.value).catch(() => ({ data: {} }))
  mall.value = res.data || {}
}

const toggleOpen = async () => {
  if (!shopClassId.value) return
  toggling.value = true
  try {
    await setMallApplyOpen({ classId: shopClassId.value, open: !mall.value.applyOpen })
    ElMessage.success(mall.value.applyOpen ? '已关闭申请' : '已开放申请')
    await loadMall()
  } finally {
    toggling.value = false
  }
}

onMounted(async () => {
  await loadRules()
  const res = await getClasses().catch(() => ({ data: [] }))
  classes.value = res.data || []
  if (classes.value[0]) {
    shopClassId.value = classes.value[0].id
    await loadMall()
  } else {
    await loadMall()
  }
})
</script>

<style scoped>
.points-page {
  min-height: calc(100vh - 56px);
  background: #f5f3ff;
}
.pts-header {
  padding: 14px 20px 16px;
  background: linear-gradient(135deg, #5b4fc7 0%, #7c3aed 45%, #9333ea 100%);
  color: #fff;
  box-shadow: 0 4px 20px rgba(91, 79, 199, 0.35);
}
.header-main h1 {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}
.title-icon { font-size: 20px; }
.header-main p {
  margin: 0;
  font-size: 12px;
  opacity: 0.9;
  line-height: 1.5;
  max-width: 820px;
}
.pts-body {
  padding: 16px 20px 28px;
  max-width: 1120px;
}
.panel {
  background: #fff;
  border: 1px solid #e9e5ff;
  border-radius: 14px;
  padding: 12px 16px 18px;
  box-shadow: 0 2px 12px rgba(91, 79, 199, 0.06);
}
.soft-tabs :deep(.el-tabs__item.is-active) { color: #5b4fc7; }
.soft-tabs :deep(.el-tabs__active-bar) { background: #7c3aed; }
.rule-card { padding: 4px 2px 8px; }
.rule-lead { margin: 0 0 16px; color: #475569; }
.rule-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.rule-row label { width: 160px; color: #334155; }
.action-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
}
.action-btn:disabled { opacity: 0.55; cursor: not-allowed; }
.action-btn.indigo { background: linear-gradient(135deg, #4338ca, #6366f1); }
.action-btn.orange { background: linear-gradient(135deg, #f97316, #fb923c); }
.shop-note { margin: 0 0 14px; color: #64748b; font-size: 13px; line-height: 1.6; }
.shop-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 16px;
}
.open-hint { font-size: 13px; color: #b91c1c; }
.open-hint.on { color: #15803d; }
.sec-title { margin: 22px 0 10px; font-size: 16px; color: #312e81; }
.order-item { display: inline-flex; align-items: center; gap: 8px; }
.order-thumb {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  object-fit: cover;
}
</style>
