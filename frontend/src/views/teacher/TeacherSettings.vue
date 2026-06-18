<template>
  <div class="settings-page">
    <div class="page-head">
      <h2>后台管理</h2>
      <p>班级、AI 与数据导出等系统配置</p>
    </div>
    <el-row :gutter="16">
      <el-col :xs="24" :md="8" v-for="block in blocks" :key="block.key">
        <el-card shadow="hover" class="block-card">
          <div class="block-icon">{{ block.icon }}</div>
          <h3>{{ block.title }}</h3>
          <ul>
            <li v-for="(f, i) in block.features" :key="i">{{ f }}</li>
          </ul>
          <el-tag size="small" type="info">即将上线</el-tag>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
const blocks = [
  {
    key: 'class',
    icon: '👥',
    title: '班级管理',
    features: ['新增/编辑班级', '批量导入学生', '绑定任课教师']
  },
  {
    key: 'ai',
    icon: '⚙️',
    title: 'AI 配置',
    features: ['评价规则与提示词', 'SOLO 层级标准', 'AI 调用额度']
  },
  {
    key: 'export',
    icon: '📤',
    title: '数据导出',
    features: ['导出学情 Excel', '导出作业提交', '导出评价报告']
  }
]
</script>

<style scoped>
.settings-page { max-width: 1000px; margin: 0 auto; }
.page-head { margin-bottom: 20px; }
.page-head h2 { margin: 0 0 4px; font-size: 22px; }
.page-head p { margin: 0; color: #64748b; font-size: 13px; }
.block-card { border-radius: 12px; margin-bottom: 16px; min-height: 220px; }
.block-icon { font-size: 36px; margin-bottom: 8px; }
.block-card h3 { margin: 0 0 12px; font-size: 16px; }
.block-card ul {
  margin: 0 0 14px;
  padding-left: 18px;
  color: #475569;
  font-size: 13px;
  line-height: 1.8;
}
</style>
