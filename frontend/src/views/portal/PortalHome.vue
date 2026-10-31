<template>
  <div class="portal">
    <header class="top">
      <div>
        <p class="kicker">中小学信息科技学习空间</p>
        <h1>请选择要进入的空间</h1>
      </div>
      <div class="who">
        <span>{{ realName }}</span>
        <button v-if="isAdmin" type="button" @click="go('/admin/users')">用户管理</button>
        <button type="button" @click="logout">退出</button>
      </div>
    </header>

    <main class="cards">
      <button type="button" class="card" @click="enterTeach">
        <img src="/portal/teach.jpg" alt="教" />
      </button>
      <button type="button" class="card" @click="enterLearn">
        <img src="/portal/learn.jpg" alt="学" />
      </button>
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const realName = localStorage.getItem('realName') || ''
const role = localStorage.getItem('role') || ''
const isAdmin = role === 'ADMIN'
const isStaff = role === 'TEACHER' || role === 'ADMIN'

const go = (path) => router.push(path)

const enterTeach = () => {
  if (!isStaff) {
    ElMessage.info('「教」是教师工作台，学生请进入「学」')
    return
  }
  go('/teacher/course-map')
}

const enterLearn = () => {
  if (role === 'STUDENT') {
    go('/student/map')
    return
  }
  go('/teacher/student-view')
}

const logout = () => {
  localStorage.clear()
  router.push('/login')
}
</script>

<style scoped>
.portal {
  min-height: 100vh;
  background:
    radial-gradient(900px 420px at 50% -10%, rgba(255, 255, 255, 0.9), transparent 70%),
    linear-gradient(180deg, #d7e9ff 0%, #eef4ff 42%, #f7f8fc 100%);
  padding: 28px 40px 48px;
}
.top {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  margin-bottom: 28px;
}
.kicker {
  margin: 0 0 6px;
  color: #5b6b88;
  font-size: 14px;
}
h1 {
  margin: 0;
  font-size: 28px;
  color: #1e3a5f;
}
.who {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #334155;
}
.who button, .card {
  font: inherit;
  cursor: pointer;
}
.who button {
  border: 0;
  background: transparent;
  color: #2563eb;
  font-weight: 700;
}
.cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px;
  max-width: 1180px;
}
.card {
  position: relative;
  border: 0;
  padding: 0;
  border-radius: 28px;
  overflow: hidden;
  background: #e8f1ff;
  box-shadow: 0 16px 40px rgba(37, 99, 235, 0.12);
  aspect-ratio: 1;
}
.card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 42%;
  transform: scale(1.12);
  display: block;
}
.card:hover {
  transform: translateY(-3px);
}
@media (max-width: 860px) {
  .portal { padding: 20px 16px 32px; }
  .top { flex-direction: column; align-items: flex-start; }
  .cards { grid-template-columns: 1fr; }
}
</style>
