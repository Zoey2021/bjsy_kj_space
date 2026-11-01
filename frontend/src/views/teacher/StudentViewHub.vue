<template>
  <div class="tech-world">
    <header class="tw-header">
      <div class="header-main">
        <h1><span class="title-icon">🌐</span> 科技世界</h1>
        <p>以教师身份预览学生看到的商城与乐园。预览不会提交作业，也不会扣学生积分。</p>
      </div>
    </header>

    <div class="tw-body">
      <div class="cards">
        <button type="button" class="entry-card" @click="router.push('/teacher/tech-world/mall')">
          <span class="entry-icon">🎁</span>
          <b>积分商城</b>
          <span>星星、等级和奖品，按演示积分预览兑换流程。</span>
        </button>
        <button type="button" class="entry-card" @click="router.push('/teacher/tech-world/park')">
          <span class="entry-icon">🎮</span>
          <b>游学乐园</b>
          <span>打开游戏看看效果，预览模式不扣积分。</span>
        </button>
      </div>
      <p class="note">个人中心、学习记录和积分榜可在打开某一课后，用课时内快捷入口查看。</p>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.tech-world {
  min-height: calc(100vh - 56px);
  background: #f5f3ff;
}
.tw-header {
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
  max-width: 720px;
}
.tw-body {
  padding: 16px 20px 28px;
  max-width: 920px;
}
.cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.entry-card {
  text-align: left;
  border: 1px solid #e9e5ff;
  background: #fff;
  border-radius: 14px;
  padding: 18px 16px;
  cursor: pointer;
  box-shadow: 0 2px 12px rgba(91, 79, 199, 0.06);
  transition: border-color 0.15s ease, transform 0.15s ease;
}
.entry-card:hover {
  border-color: #7c3aed;
  transform: translateY(-1px);
}
.entry-icon {
  display: inline-block;
  font-size: 22px;
  margin-bottom: 8px;
}
.entry-card b {
  display: block;
  font-size: 16px;
  color: #312e81;
  margin-bottom: 6px;
}
.entry-card span:last-child {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}
.note {
  margin: 16px 0 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
}
@media (max-width: 800px) {
  .cards { grid-template-columns: 1fr; }
}
</style>
