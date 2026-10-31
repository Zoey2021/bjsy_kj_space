<template>
  <div class="mall-page" v-loading="loading">
    <header class="mall-head">
      <h2>积分商城</h2>
      <p v-if="data.preview">教师查看。下面用 15 积分演示星星和奖品，不会真正扣分，也不能代学生兑换。</p>
      <p v-else>{{ studentName }}，完成每节课的任务就能获得积分。换礼物和玩乐园只扣可兑积分，星星不会被收回。</p>
    </header>

    <section class="growth">
      <div class="balance-card">
        <span>可兑积分</span>
        <strong>{{ spendable }}</strong>
      </div>
      <div class="balance-card muted">
        <span>累计积分</span>
        <strong>{{ earned }}</strong>
      </div>
      <div class="balance-card sky">
        <span>等级</span>
        <strong>{{ growth.level }}</strong>
      </div>
      <div class="bodies" :aria-label="`等级 ${growth.level}`">
        <svg v-for="n in shownSuns" :key="'sun' + n" class="mark" viewBox="0 0 32 32" aria-hidden="true">
          <circle cx="16" cy="16" r="7" fill="#f97316"/>
          <g stroke="#fb923c" stroke-width="2" stroke-linecap="round">
            <path d="M16 3v4M16 25v4M3 16h4M25 16h4M6.5 6.5l3 3M22.5 22.5l3 3M25.5 6.5l-3 3M9.5 22.5l-3 3"/>
          </g>
        </svg>
        <svg v-for="n in growth.holdMoons" :key="'moon' + n" class="mark" viewBox="0 0 32 32" aria-hidden="true">
          <path d="M21 5a11 11 0 1 0 7 18A9 9 0 1 1 21 5z" fill="#fbbf24"/>
        </svg>
        <svg v-for="n in growth.holdStars" :key="'star' + n" class="mark" viewBox="0 0 32 32" aria-hidden="true">
          <polygon points="16,3 19.6,12 29,12.4 21.6,18.4 24.2,28 16,22.6 7.8,28 10.4,18.4 3,12.4 12.4,12" fill="#f59e0b"/>
        </svg>
        <svg v-if="remainder > 0" class="mark next" viewBox="0 0 32 32" aria-hidden="true">
          <defs>
            <clipPath id="mall-next-star">
              <rect :width="32 * remainder / 10" height="32"/>
            </clipPath>
          </defs>
          <polygon points="16,3 19.6,12 29,12.4 21.6,18.4 24.2,28 16,22.6 7.8,28 10.4,18.4 3,12.4 12.4,12" fill="#e2e8f0"/>
          <polygon points="16,3 19.6,12 29,12.4 21.6,18.4 24.2,28 16,22.6 7.8,28 10.4,18.4 3,12.4 12.4,12" fill="#f59e0b" clip-path="url(#mall-next-star)"/>
        </svg>
        <svg v-if="!growth.stars && !remainder" class="mark" viewBox="0 0 32 32" aria-hidden="true">
          <polygon points="16,3 19.6,12 29,12.4 21.6,18.4 24.2,28 16,22.6 7.8,28 10.4,18.4 3,12.4 12.4,12" fill="#e2e8f0"/>
        </svg>
        <span v-if="growth.holdSuns > shownSuns" class="more">+{{ growth.holdSuns - shownSuns }}</span>
        <span class="park-pill">乐园 {{ minutes }} 分钟</span>
      </div>
      <div class="open-tag" :class="{ on: data.applyOpen }">
        {{ data.applyOpen ? '教师已开放申请' : '教师尚未开放申请' }}
      </div>
    </section>

    <section class="legend" aria-label="等级换算">
      <div class="legend-step">
        <b>10</b>
        <span>→</span>
        <svg viewBox="0 0 32 32"><polygon points="16,3 19.6,12 29,12.4 21.6,18.4 24.2,28 16,22.6 7.8,28 10.4,18.4 3,12.4 12.4,12" fill="#f59e0b"/></svg>
      </div>
      <div class="legend-step">
        <svg v-for="n in 6" :key="'ls' + n" viewBox="0 0 32 32"><polygon points="16,3 19.6,12 29,12.4 21.6,18.4 24.2,28 16,22.6 7.8,28 10.4,18.4 3,12.4 12.4,12" fill="#f59e0b"/></svg>
        <span>→</span>
        <svg viewBox="0 0 32 32"><path d="M21 5a11 11 0 1 0 7 18A9 9 0 1 1 21 5z" fill="#fbbf24"/></svg>
      </div>
      <div class="legend-step">
        <svg v-for="n in 6" :key="'lm' + n" viewBox="0 0 32 32"><path d="M21 5a11 11 0 1 0 7 18A9 9 0 1 1 21 5z" fill="#fbbf24"/></svg>
        <span>→</span>
        <svg viewBox="0 0 32 32"><circle cx="16" cy="16" r="7" fill="#f97316"/><g stroke="#fb923c" stroke-width="2" stroke-linecap="round"><path d="M16 3v4M16 25v4M3 16h4M25 16h4M6.5 6.5l3 3M22.5 22.5l3 3M25.5 6.5l-3 3M9.5 22.5l-3 3"/></g></svg>
      </div>
    </section>

    <MallGiftGrid
      mode="apply"
      :items="data.items || []"
      :apply-open="!!data.applyOpen"
      :redeemable-points="spendable"
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
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStudentMall, redeemMallItem } from '../../api'
import MallGiftGrid from '../../components/points/MallGiftGrid.vue'
import { mallIconSrc } from '../../constants/mallIcons'
import { growthFromPoints, parkMinutes } from '../../utils/growthLevel'

const loading = ref(false)
const applying = ref(false)
const data = ref({})
const studentName = computed(() => data.value.studentName || localStorage.getItem('realName') || '同学')
const spendable = computed(() => Number(data.value.redeemablePoints || 0))
const earned = computed(() => Number(data.value.earnedPoints || 0))
const growth = computed(() => growthFromPoints(earned.value))
const remainder = computed(() => growth.value.points % 10)
const shownSuns = computed(() => Math.min(growth.value.holdSuns, 6))
const minutes = computed(() => parkMinutes(spendable.value))

const formatTime = (v) => (v ? String(v).replace('T', ' ').slice(0, 19) : '—')

const load = async () => {
  loading.value = true
  try {
    const res = await getStudentMall()
    data.value = res.data || {}
  } catch {
    if (import.meta.env.DEV && !(data.value.items || []).length) {
      data.value = {
        studentName: localStorage.getItem('realName') || '同学',
        redeemablePoints: 15,
        earnedPoints: 15,
        applyOpen: false,
        items: [
          { id: 1, code: 'star_future_2', name: '星未来积分2', cost: 80 },
          { id: 2, code: 'snack', name: '零食1份', cost: 160 },
          { id: 3, code: 'stationery', name: '文具1份', cost: 160 },
          { id: 4, code: 'praise_letter', name: '表扬信', cost: 180 },
          { id: 5, code: 'milk_tea', name: '奶茶1份', cost: 250 },
          { id: 6, code: 'info_blindbox', name: '信息盲盒小礼物', cost: 140 }
        ],
        orders: []
      }
    }
  } finally {
    loading.value = false
  }
}

const onApply = async (item) => {
  if (data.value.preview) {
    const gap = Number(item.cost || 0) - spendable.value
    if (gap > 0) {
      ElMessage.info(`还差 ${gap} 积分就能兑换「${item.name}」。教师查看不会真正扣分。`)
    } else {
      ElMessage.info(`教师查看：学生用 ${item.cost} 积分可以兑换「${item.name}」。这里不会真正扣分。`)
    }
    return
  }
  const gap = Number(item.cost || 0) - spendable.value
  if (gap > 0) {
    ElMessage.info(`还差 ${gap} 积分就能兑换「${item.name}」。`)
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定用 ${item.cost} 积分兑换「${item.name}」？可兑积分会减少，星星和等级还在。`,
      '兑换确认',
      { type: 'warning', confirmButtonText: '确定兑换' }
    )
  } catch {
    return
  }
  applying.value = true
  try {
    await redeemMallItem(item.id)
    ElMessage.success(`兑换成功！用 ${item.cost} 积分换到「${item.name}」。`)
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
.mall-head p { margin: 0 0 16px; color: #64748b; font-size: 13px; line-height: 1.6; }
.growth {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: stretch;
  margin-bottom: 16px;
}
.balance-card {
  background: linear-gradient(135deg, #fff7ed, #ffedd5);
  border-radius: 16px;
  padding: 12px 18px;
  min-width: 120px;
}
.balance-card span { display: block; font-size: 12px; color: #9a3412; }
.balance-card strong { font-size: 28px; color: #c2410c; }
.balance-card.muted { background: #f1f5f9; }
.balance-card.muted span { color: #64748b; }
.balance-card.muted strong { color: #334155; }
.balance-card.sky { background: #eef2ff; }
.balance-card.sky span { color: #4338ca; }
.balance-card.sky strong { color: #3730a3; }
.bodies {
  flex: 1;
  min-width: 220px;
  background: #fff;
  border-radius: 16px;
  padding: 10px 14px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.mark { width: 42px; height: 42px; flex: none; }
.more { font-weight: 800; color: #f97316; }
.park-pill {
  margin-left: auto;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}
.open-tag {
  align-self: center;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 700;
  background: #fee2e2;
  color: #b91c1c;
}
.open-tag.on { background: #dcfce7; color: #15803d; }
.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 0 0 16px;
}
.legend-step {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #f8fafc;
  border-radius: 16px;
  padding: 8px 12px;
}
.legend-step b { color: #c2410c; font-size: 18px; }
.legend-step span { color: #94a3b8; font-weight: 700; }
.legend-step svg { width: 22px; height: 22px; flex: none; }
.sec-title { margin: 22px 0 10px; font-size: 16px; }
.order-item { display: inline-flex; align-items: center; gap: 8px; }
.order-thumb {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  object-fit: cover;
}
</style>
