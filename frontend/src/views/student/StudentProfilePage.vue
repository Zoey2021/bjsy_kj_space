<template>
  <div class="page">
    <header class="head">
      <h1>个人中心</h1>
      <p>查看身份信息，挑选自己的数字形象与装扮。</p>
    </header>
    <StudentProfile
      :class-name="className"
      :student-no="studentNo"
      :student-name="studentName"
    />
  </div>
</template>

<script setup>
import StudentProfile from '../../components/student/StudentProfile.vue'

const className = localStorage.getItem('className') || ''
const studentNo = localStorage.getItem('username') || ''
const studentName = localStorage.getItem('realName') || ''
</script>

<style scoped>
.page {
  max-width: 960px;
  margin: 0 auto;
}
.head {
  margin-bottom: 16px;
}
.head h1 {
  margin: 0 0 6px;
  font-size: 22px;
  color: #1e293b;
}
.head p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}
</style>
