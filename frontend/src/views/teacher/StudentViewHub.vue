<template>
  <div class="hub">
    <h2>学生页面</h2>
    <p>用教师账号直接查看学生看到的页面。查看不会提交作业，也不会扣学生积分。</p>
    <div class="cards">
      <button type="button" @click="router.push('/teacher/student-view/mall')">
        <b>积分商城</b>
        <span>星星、等级和奖品，按 15 积分演示。</span>
      </button>
      <button type="button" @click="router.push('/teacher/student-view/park')">
        <b>游学乐园</b>
        <span>可以打开游戏看看，不扣积分。</span>
      </button>
    </div>
    <p class="note">个人中心、学习记录和积分榜在打开某一课之后，用左侧那一排按钮查看。</p>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.hub { max-width: 880px; margin: 0 auto; padding: 28px 20px 40px; }
h2 { margin: 0 0 8px; color: #312e81; }
p { color: #475569; line-height: 1.7; }
.cards { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; margin: 18px 0; }
button {
  text-align: left;
  border: 1px solid #ddd6fe;
  background: #fff;
  border-radius: 16px;
  padding: 18px 16px;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(91, 79, 199, 0.06);
}
button:hover { border-color: #7c6cf0; }
b { display: block; font-size: 18px; color: #4338ca; margin-bottom: 6px; }
span { color: #64748b; font-size: 13px; line-height: 1.6; }
.note { font-size: 13px; }
@media (max-width: 800px) {
  .cards { grid-template-columns: 1fr; }
}
</style>
