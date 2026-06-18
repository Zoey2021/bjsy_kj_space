<template>
  <div class="points-page">
    <div class="page-head">
      <h2>课堂积分</h2>
      <p>积分自动发放、排行榜与积分商城</p>
    </div>
    <el-tabs v-model="tab">
      <el-tab-pane label="积分排行榜" name="ranking">
        <Ranking embedded />
      </el-tab-pane>
      <el-tab-pane label="积分规则" name="rules">
        <el-card shadow="never">
          <p>开启「提交任务自动加分」：系统配置 <code>points_per_task</code>（当前默认每任务 5 分）</p>
          <el-alert type="info" show-icon :closable="false" style="margin-top:12px">
            完整规则配置界面即将上线，可在后台管理统一调整
          </el-alert>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="积分商城" name="shop">
        <ModulePlaceholder
          title="积分商城"
          description="学生使用积分兑换课堂奖励"
          icon="🎁"
          :features="['上架/下架奖励', '设置兑换所需积分', '查看兑换记录']"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Ranking from './Ranking.vue'
import ModulePlaceholder from '../../components/teacher/ModulePlaceholder.vue'

const route = useRoute()
const router = useRouter()
const tab = ref(route.query.tab || 'ranking')

watch(tab, (v) => {
  router.replace({ query: { tab: v } })
})
watch(() => route.query.tab, (v) => {
  if (v) tab.value = v
})
</script>

<style scoped>
.points-page { max-width: 1100px; margin: 0 auto; }
.page-head { margin-bottom: 16px; }
.page-head h2 { margin: 0 0 4px; font-size: 22px; }
.page-head p { margin: 0; font-size: 13px; color: #64748b; }
</style>
