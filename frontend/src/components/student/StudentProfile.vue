<template>
  <div class="profile" v-loading="loading">
    <div class="stage">
      <template v-if="!figure">
        <button type="button" class="pick" @click="pick('boy')">
          <DigitalAvatar figure="boy" hair="boy-hair-short" outfit="boy-top-shirt" extra="boy-extra-none" />
          <span>男生形象</span>
        </button>
        <button type="button" class="pick" @click="pick('girl')">
          <DigitalAvatar figure="girl" hair="girl-hair-bob" outfit="girl-top-dress" extra="girl-extra-none" />
          <span>女生形象</span>
        </button>
      </template>
      <template v-else>
        <DigitalAvatar :figure="figure" :hair="look.hair" :outfit="look.outfit" :extra="look.extra" />
        <p v-if="trying" class="try-tip">正在试穿「{{ trying.name }}」</p>
        <div class="switch">
          <button type="button" :class="{ on: figure === 'boy' }" @click="pick('boy')">男生</button>
          <button type="button" :class="{ on: figure === 'girl' }" @click="pick('girl')">女生</button>
        </div>
      </template>
    </div>

    <section class="id-card">
      <h3>身份证信息</h3>
      <div class="id-row"><span>班级</span><strong>{{ className || '—' }}</strong></div>
      <div class="id-row"><span>学号</span><strong>{{ studentNo || '—' }}</strong></div>
      <div class="id-row"><span>姓名</span><strong>{{ studentName || '—' }}</strong></div>
      <p>这三项不能修改。</p>
      <p v-if="preview" class="preview-note">教师查看。换装只在这一页看看，不会扣积分，也不会保存。</p>
    </section>

    <section v-if="figure" class="shop">
      <h3>装扮 · 可兑 {{ points }} 积分</h3>
      <p class="shop-hint">点一下先试穿；还没拥有的装扮会询问是否兑换，确认后才扣积分。</p>
      <div v-for="group in groups" :key="group.slot" class="slot">
        <div class="slot-name">{{ group.label }}</div>
        <button
          v-for="item in group.items"
          :key="item.id"
          type="button"
          class="item"
          :class="{
            on: equipped[group.slot] === item.id,
            try: trying && trying.id === item.id
          }"
          :disabled="busy"
          @click="wear(item)"
        >
          <img :src="itemSrc(item.id)" alt="" width="64" height="84" decoding="async" />
          <b>{{ item.name }}</b>
          <small>{{ labelOf(item) }}</small>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { chooseAvatarFigure, equipAvatarItem, getStudentAvatar } from '../../api'
import { avatarItemSrc } from '../../utils/avatarLook'
import DigitalAvatar from './DigitalAvatar.vue'

const props = defineProps({
  className: { type: String, default: '' },
  studentNo: { type: String, default: '' },
  studentName: { type: String, default: '' }
})
const emit = defineEmits(['points'])

const loading = ref(false)
const busy = ref(false)
const figure = ref('')
const equipped = ref({ hair: '', outfit: '', extra: '' })
const look = ref({ hair: '', outfit: '', extra: '' })
const trying = ref(null)
const catalog = ref([])
const owned = ref([])
const points = ref(0)
const identity = ref({})
const preview = ref(false)
const itemSrc = avatarItemSrc

const className = computed(() => identity.value.className || props.className)
const studentNo = computed(() => identity.value.username || props.studentNo)
const studentName = computed(() => identity.value.realName || props.studentName)

const slotLabel = { hair: '发型', outfit: '衣服', extra: '配饰' }
const groups = computed(() => ['hair', 'outfit', 'extra'].map((slot) => ({
  slot,
  label: slotLabel[slot],
  items: catalog.value.filter((item) => item.figure === figure.value && item.slot === slot)
})))

const isOwned = (item) => !!(item.owned || owned.value.includes(item.id) || !item.cost)

const labelOf = (item) => {
  if (trying.value?.id === item.id) return '试穿中'
  if (equipped.value[item.slot] === item.id) return '穿着中'
  if (isOwned(item)) return '换上'
  if (points.value < item.cost) return `还差 ${item.cost - points.value} 分`
  return `${item.cost} 积分 · 可试穿`
}

const syncLook = (slots) => {
  look.value = { hair: '', outfit: '', extra: '', ...(slots || {}) }
  trying.value = null
}

const apply = (data) => {
  if (!data) return
  identity.value = {
    className: data.className,
    username: data.username,
    realName: data.realName
  }
  figure.value = data.figure || ''
  equipped.value = { hair: '', outfit: '', extra: '', ...(data.equipped || {}) }
  syncLook(equipped.value)
  catalog.value = data.catalog || []
  owned.value = data.owned || []
  points.value = Number(data.redeemablePoints || 0)
  preview.value = !!data.preview
  emit('points', points.value)
}

const load = async () => {
  loading.value = true
  try {
    const res = await getStudentAvatar()
    apply(res.data)
  } finally {
    loading.value = false
  }
}

const defaultsFor = (next) => (
  next === 'girl'
    ? { hair: 'girl-hair-bob', outfit: 'girl-top-dress', extra: 'girl-extra-none' }
    : { hair: 'boy-hair-short', outfit: 'boy-top-shirt', extra: 'boy-extra-none' }
)

const pick = async (next) => {
  if (busy.value || next === figure.value) return
  if (preview.value) {
    figure.value = next
    equipped.value = defaultsFor(next)
    syncLook(equipped.value)
    return
  }
  busy.value = true
  try {
    const res = await chooseAvatarFigure(next)
    apply(res.data)
  } finally {
    busy.value = false
  }
}

const tryOn = (item) => {
  look.value = { ...look.value, [item.slot]: item.id }
  trying.value = item
}

const revertTryOn = () => {
  syncLook(equipped.value)
}

const confirmRedeem = async (item) => {
  try {
    await ElMessageBox.confirm(
      `正在试穿「${item.name}」。确定用 ${item.cost} 积分兑换并穿上吗？可兑积分会减少，星星和等级还在。`,
      '兑换确认',
      { type: 'warning', confirmButtonText: '确定兑换', cancelButtonText: '先不换' }
    )
    return true
  } catch {
    return false
  }
}

const wear = async (item) => {
  if (busy.value) return
  if (preview.value) {
    look.value = { ...look.value, [item.slot]: item.id }
    equipped.value = { ...equipped.value, [item.slot]: item.id }
    trying.value = null
    ElMessage.success(`教师查看：已换上「${item.name}」，不会扣积分。`)
    return
  }

  // 点回当前穿着：取消试穿，恢复已保存形象
  if (equipped.value[item.slot] === item.id && isOwned(item)) {
    if (trying.value) revertTryOn()
    return
  }

  // 先试穿到形象上
  tryOn(item)

  const have = isOwned(item)
  if (!have) {
    if (points.value < item.cost) {
      ElMessage.info(`还差 ${item.cost - points.value} 积分才能兑换「${item.name}」。可以先看看试穿效果。`)
      return
    }
    const ok = await confirmRedeem(item)
    if (!ok) {
      revertTryOn()
      return
    }
  }

  busy.value = true
  try {
    const res = await equipAvatarItem(item.id)
    apply(res.data)
    if (res.data?.message) ElMessage.success(res.data.message)
  } catch {
    revertTryOn()
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.profile {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 18px;
  padding: 20px 24px 32px;
  align-items: start;
}
.preview-note { margin: 8px 0 0; color: #0f766e; }
.try-tip {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  color: #b45309;
}
.shop-hint {
  margin: -4px 0 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}
.stage {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.pick {
  width: 100%;
  border: 1px solid #e2e8f0;
  background: #fff;
  border-radius: 16px;
  cursor: pointer;
  padding: 4px 0 8px;
}
.pick span, .switch button { font-weight: 700; color: #334155; }
.switch { display: flex; gap: 8px; }
.switch button, .item {
  border: 1px solid #e2e8f0;
  background: #fff;
  border-radius: 999px;
  padding: 6px 12px;
  cursor: pointer;
}
.switch button.on, .item.on { background: #eef2ff; border-color: #818cf8; color: #3730a3; }
.item.try {
  background: #fff7ed;
  border-color: #fb923c;
  color: #9a3412;
  box-shadow: 0 0 0 2px rgba(251, 146, 60, 0.25);
}
.item.try small { color: #c2410c; }
.id-card, .shop {
  background: #f8fafc;
  border-radius: 16px;
  padding: 14px 16px;
}
.id-card h3, .shop h3 { margin: 0 0 10px; font-size: 16px; }
.id-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #e2e8f0;
}
.id-row span { color: #64748b; }
.id-card p { margin: 8px 0 0; color: #94a3b8; font-size: 12px; }
.shop { grid-column: 1 / -1; }
.slot { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-top: 10px; }
.slot-name { width: 42px; color: #64748b; font-size: 13px; }
.item { display: flex; flex-direction: column; align-items: center; border-radius: 12px; min-width: 92px; }
.item img { width: 64px; height: 84px; object-fit: contain; }
.item b { font-size: 14px; }
.item small { color: #c2410c; }
.item.on small { color: #4338ca; }
@media (max-width: 720px) {
  .profile { grid-template-columns: 1fr; }
}
</style>
