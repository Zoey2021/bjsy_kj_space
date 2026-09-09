<template>
  <div class="mall-page" v-loading="loading">
    <header class="mall-head">
      <h2>🎁 兑换商城</h2>
      <p>用课堂积分兑换可爱奖励。教师点击「开放申请」后才能提交兑换。</p>
    </header>

    <div class="balance-row">
      <div class="balance-card">
        <span>可兑奖积分</span>
        <strong>{{ data.redeemablePoints || 0 }}</strong>
      </div>
      <div class="balance-card muted">
        <span>累计获得</span>
        <strong>{{ data.earnedPoints || 0 }}</strong>
      </div>
      <div class="open-tag" :class="{ on: data.applyOpen }">
        {{ data.applyOpen ? '教师已开放申请' : '教师尚未开放申请' }}
      </div>
    </div>

    <MallGiftGrid
      mode="apply"
      :items="data.items || []"
      :apply-open="!!data.applyOpen"
      :redeemable-points="data.redeemablePoints || 0"
      :applying="applying"
      @apply="onApply"
    />

    <h3 class="sec-title">我的兑换记录</h3>
    <el-table :data="data.orders || []" stripe empty-text="还没有兑换记录">
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
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStudentMall, redeemMallItem } from '../../api'
import MallGiftGrid from '../../components/points/MallGiftGrid.vue'
import { mallIconSrc } from '../../constants/mallIcons'

const loading = ref(false)
const applying = ref(false)
const data = ref({})

const formatTime = (v) => (v ? String(v).replace('T', ' ').slice(0, 19) : '—')

const load = async () => {
  loading.value = true
  try {
    const res = await getStudentMall()
    data.value = res.data || {}
  } finally {
    loading.value = false
  }
}

const onApply = async (item) => {
  try {
    await ElMessageBox.confirm(
      `确定用 ${item.cost} 积分兑换「${item.name}」？申请后立即扣分，不可撤销。`,
      '兑换确认',
      { type: 'warning', confirmButtonText: '确定兑换' }
    )
  } catch {
    return
  }
  applying.value = true
  try {
    await redeemMallItem(item.id)
    ElMessage.success('申请成功，积分已扣除')
    await load()
  } finally {
    applying.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.mall-page { max-width: 960px; margin: 0 auto; padding: 8px 4px 24px; }
.mall-head h2 { margin: 0 0 4px; font-size: 24px; }
.mall-head p { margin: 0 0 16px; color: #64748b; font-size: 13px; }
.balance-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 18px;
}
.balance-card {
  background: linear-gradient(135deg, #fff7ed, #ffedd5);
  border-radius: 16px;
  padding: 12px 18px;
  min-width: 140px;
}
.balance-card span { display: block; font-size: 12px; color: #9a3412; }
.balance-card strong { font-size: 26px; color: #c2410c; }
.balance-card.muted { background: #f1f5f9; }
.balance-card.muted span { color: #64748b; }
.balance-card.muted strong { color: #334155; }
.open-tag {
  margin-left: auto;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 700;
  background: #fee2e2;
  color: #b91c1c;
}
.open-tag.on { background: #dcfce7; color: #15803d; }
.sec-title { margin: 22px 0 10px; font-size: 16px; }
.order-item { display: inline-flex; align-items: center; gap: 8px; }
.order-thumb {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  object-fit: cover;
}
</style>
