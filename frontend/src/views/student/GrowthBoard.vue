<template>
  <div class="board">
    <p class="preview-note">预览页：用来看换算和权益。还没有替换现在的积分商城，也不会改学生已有积分。</p>

    <ol class="flow">
      <li>任务</li>
      <li class="arrow">→</li>
      <li>积分</li>
      <li class="arrow">→</li>
      <li>等级</li>
      <li class="arrow">→</li>
      <li>权益</li>
    </ol>

    <div class="cols">
      <aside class="rules">
        <section>
          <h2>积分怎么来</h2>
          <p>完成老师发布的每节课任务，就能获得积分。</p>
        </section>
        <section>
          <h2>等级怎么升</h2>
          <p>10 积分换成 1 颗星星。集齐 6 颗星星换成 1 个月亮，集齐 6 个月亮换成 1 个太阳。</p>
          <p>等级就是一共得到的星星数。换礼物只花可兑积分，星星不会被收回。</p>
        </section>
        <section>
          <h2>积分能做什么</h2>
          <ul>
            <li v-for="item in rules" :key="item">{{ item }}</li>
          </ul>
        </section>
      </aside>

      <section class="level-card">
        <header>
          <h2>成长等级</h2>
          <span>{{ levelTitle }}</span>
        </header>
        <div class="who">
          <label>
            姓名
            <input v-model="name" maxlength="12" />
          </label>
          <div class="score">
            <span>累计积分</span>
            <strong>{{ growth.points }}</strong>
          </div>
          <div class="score soft">
            <span>可兑积分</span>
            <strong>{{ spendable }}</strong>
          </div>
        </div>

        <div class="bodies" aria-label="手里的星星、月亮和太阳">
          <div>
            <span>星星</span>
            <div class="icons">
              <svg v-for="n in growth.holdStars" :key="'s' + n" viewBox="0 0 32 32"><polygon points="16,3 19.5,12 29,12.5 21.5,18.5 24,28 16,22.5 8,28 10.5,18.5 3,12.5 12.5,12" fill="#f59e0b"/></svg>
              <em v-if="!growth.holdStars">正好换成月亮了</em>
            </div>
          </div>
          <div>
            <span>月亮</span>
            <div class="icons">
              <svg v-for="n in growth.holdMoons" :key="'m' + n" viewBox="0 0 32 32"><path d="M20 4a12 12 0 1 0 8 20 10 10 0 1 1-8-20z" fill="#fbbf24"/></svg>
              <em v-if="!growth.holdMoons">{{ growth.moons ? '正好换成太阳了' : '再攒星星' }}</em>
            </div>
          </div>
          <div>
            <span>太阳</span>
            <div class="icons">
              <svg v-for="n in shownSuns" :key="'u' + n" viewBox="0 0 32 32"><circle cx="16" cy="16" r="7" fill="#f97316"/><g stroke="#fb923c" stroke-width="2" stroke-linecap="round"><path d="M16 3v4M16 25v4M3 16h4M25 16h4M6.5 6.5l3 3M22.5 22.5l3 3M25.5 6.5l-3 3M9.5 22.5l-3 3"/></g></svg>
              <em v-if="growth.holdSuns > shownSuns">+{{ growth.holdSuns - shownSuns }}</em>
              <em v-if="!growth.holdSuns">继续向前</em>
            </div>
          </div>
        </div>

        <p class="count-line">一共 {{ growth.stars }} 颗星星，等级 {{ growth.level }}。<span v-if="growth.holdMoons || growth.holdStars">手里还有 <template v-if="growth.holdMoons">{{ growth.holdMoons }} 个月亮</template><template v-if="growth.holdMoons && growth.holdStars">、</template><template v-if="growth.holdStars">{{ growth.holdStars }} 颗星星</template>。</span><span v-if="growth.suns">已经换成 {{ growth.suns }} 个太阳。</span></p>

        <div class="mile">
          <span>下一个里程碑</span>
          <strong>{{ mile.title }}</strong>
          <p>还差 {{ mile.gapPoints }} 积分，也就是还差 {{ mile.gapLevels }} 级</p>
        </div>

        <div class="try">
          <button type="button" @click="addTask">完成本节课任务 +10</button>
          <button v-for="n in samples" :key="n" type="button" class="ghost" @click="setSample(n)">{{ n }} 分</button>
        </div>
        <p v-if="cheer" class="cheer">{{ cheer }}</p>
      </section>

      <aside class="perks">
        <header>权益</header>
        <h3>达到等级就有</h3>
        <ul>
          <li v-for="item in unlocks" :key="item.id" :class="{ on: item.on }">
            <div>
              <b>{{ item.title }}</b>
              <small>{{ item.level }} 级</small>
            </div>
            <span>{{ item.on ? '已解锁' : `还差 ${item.level - growth.level} 级` }}</span>
          </li>
        </ul>
        <h3>用积分兑换</h3>
        <ul>
          <li v-for="item in redeems" :key="item.id">
            <div>
              <b>{{ item.title }}</b>
              <small>{{ item.cost }} 积分 · {{ item.hint }}</small>
            </div>
            <button type="button" :disabled="!item.ok" @click="redeem(item)">{{ item.ok ? '兑换' : '还不行' }}</button>
          </li>
        </ul>
        <p class="mystery">神秘大奖：到 31 级解锁。具体奖品和要不要扣分，等老师来定。</p>
        <h3 v-if="records.length">已兑换</h3>
        <ul v-if="records.length" class="logs">
          <li v-for="(row, i) in records" :key="i">{{ row }}</li>
        </ul>
      </aside>
    </div>

    <pre class="card">{{ card }}</pre>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import {
  UNLOCK_BENEFITS,
  cheerForGain,
  formatStatusCard,
  growthFromPoints,
  nextMilestone,
  redeemStatus
} from '../../utils/growthLevel'

const rules = [
  '10 积分 = 1 颗星星，等级 = 星星数',
  '6 颗星星 = 1 个月亮，6 个月亮 = 1 个太阳',
  '6 级：游学乐园 5 分钟',
  '11 级：游学乐园 10 分钟，可以换小礼物',
  '21 级：游学乐园 15 分钟，优先选机位',
  '31 级：游学乐园 30 分钟，神秘大奖',
  '小礼物：精美文具 30 分，小玩具 80 分，奖状 100 分'
]

const samples = [0, 50, 200, 360, 2160]
const name = ref('小明')
const earned = ref(200)
const spendable = ref(200)
const cheer = ref('200 积分正好是 20 颗星星：3 个月亮，手里还剩 2 颗星星。')
const records = ref([])

const growth = computed(() => growthFromPoints(earned.value))
const mile = computed(() => nextMilestone(earned.value))
const shownSuns = computed(() => Math.min(growth.value.holdSuns, 6))
const unlocks = computed(() => UNLOCK_BENEFITS.map((item) => ({
  ...item,
  on: growth.value.level >= item.level
})))
const redeems = computed(() => redeemStatus(earned.value, spendable.value))
const levelTitle = computed(() => {
  const g = growth.value
  if (g.suns >= 1) return `已经有 ${g.suns} 个太阳`
  if (g.moons >= 1) return `已经有 ${g.moons} 个月亮`
  if (g.stars >= 1) return `已经有 ${g.stars} 颗星星`
  return '从第一颗星星开始'
})
const card = computed(() => formatStatusCard({
  name: name.value.trim() || '同学',
  earned: earned.value,
  spendable: spendable.value,
  delta: 0,
  taskName: ''
}))

const addTask = () => {
  const before = earned.value
  earned.value += 10
  spendable.value += 10
  cheer.value = cheerForGain(before, earned.value) + ' 这 10 分来自「完成本节课任务」。'
}

const setSample = (points) => {
  const before = earned.value
  earned.value = points
  spendable.value = points
  records.value = []
  cheer.value = points === before
    ? '积分没有变化。'
    : cheerForGain(before, points)
}

const redeem = (item) => {
  if (!item.ok) return
  spendable.value -= item.cost
  records.value.unshift(`${item.title} · 用了 ${item.cost} 积分`)
  cheer.value = `兑换成功！用 ${item.cost} 积分换到「${item.title}」。星星、月亮和等级都还在。`
}
</script>

<style scoped>
.board {
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 20px 48px;
  color: #1e293b;
  background: #f4f7fb;
  min-height: 100vh;
  box-sizing: border-box;
}
.preview-note {
  margin: 0 0 16px;
  color: #64748b;
  font-size: 13px;
}
.flow {
  display: flex;
  align-items: center;
  gap: 8px;
  list-style: none;
  margin: 0 0 18px;
  padding: 0;
}
.flow li {
  background: #e8eef6;
  color: #334155;
  border-radius: 12px;
  padding: 12px 28px;
  font-weight: 700;
}
.flow .arrow {
  background: transparent;
  padding: 0 2px;
  color: #94a3b8;
}
.cols {
  display: grid;
  grid-template-columns: 240px 1fr 280px;
  gap: 16px;
  align-items: start;
}
.rules section + section { margin-top: 22px; }
.rules h2, .perks h3 {
  margin: 0 0 6px;
  padding-left: 8px;
  border-left: 3px solid #93c5fd;
  font-size: 16px;
}
.rules p, .rules li { margin: 0; color: #475569; font-size: 14px; line-height: 1.6; }
.rules ul { margin: 0; padding-left: 18px; }
.level-card, .perks {
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}
.level-card header, .perks header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-radius: 18px 18px 0 0;
  background: linear-gradient(90deg, #5b5bd6, #7c6cf0);
  color: #fff;
}
.level-card header h2, .perks header { margin: 0; font-size: 16px; }
.who, .bodies, .mile, .try { padding: 0 16px; }
.who { display: flex; gap: 12px; align-items: end; margin-top: 14px; }
.who label { font-size: 12px; color: #64748b; display: grid; gap: 4px; }
.who input {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 6px 8px;
  font-size: 16px;
  width: 120px;
}
.score span { display: block; font-size: 12px; color: #64748b; }
.score strong { font-size: 28px; color: #4f46e5; }
.score.soft strong { color: #c2410c; font-size: 22px; }
.bodies { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-top: 12px; }
.bodies span { font-size: 12px; color: #64748b; }
.icons { min-height: 36px; display: flex; flex-wrap: wrap; gap: 4px; align-items: center; }
.icons svg { width: 28px; height: 28px; }
.icons em { font-style: normal; font-size: 12px; color: #94a3b8; }
.count-line { margin: 10px 16px; color: #475569; font-size: 14px; }
.mile {
  margin-top: 4px;
  background: #f8fafc;
  border-radius: 12px;
  padding: 12px 16px;
}
.mile span { font-size: 12px; color: #64748b; }
.mile strong { display: block; margin: 2px 0; }
.mile p { margin: 0; color: #334155; }
.try { display: flex; flex-wrap: wrap; gap: 8px; margin: 14px 0; }
.try button, .perks button {
  border: 0;
  border-radius: 999px;
  padding: 8px 12px;
  background: #4f46e5;
  color: #fff;
  cursor: pointer;
}
.try .ghost { background: #e0e7ff; color: #3730a3; }
.perks button:disabled { background: #e2e8f0; color: #94a3b8; cursor: default; }
.cheer { margin: 0 16px 16px; color: #b45309; font-weight: 700; }
.perks { padding-bottom: 12px; }
.perks h3 { margin: 14px 16px 8px; }
.perks ul { list-style: none; margin: 0; padding: 0 12px; }
.perks li {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
  padding: 8px;
  border-radius: 10px;
}
.perks li.on { background: #eef2ff; }
.perks b { display: block; font-size: 14px; }
.perks small, .mystery, .logs { color: #64748b; font-size: 12px; }
.mystery { margin: 8px 16px; }
.card {
  margin: 16px 0 0;
  padding: 16px;
  white-space: pre-wrap;
  background: #fff;
  border-radius: 16px;
  font-family: inherit;
  line-height: 1.7;
}
@media (max-width: 980px) {
  .cols { grid-template-columns: 1fr; }
}
</style>
