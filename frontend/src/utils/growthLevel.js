/** 10 积分 = 1 星，6 星 = 1 月，6 月 = 1 日。等级 = 星星总数。 */

export const POINTS_PER_STAR = 10
export const STARS_PER_MOON = 6
export const MOONS_PER_SUN = 6
export const PARK_POINTS_PER_MINUTE = 2

/** 达到等级即可享受，不扣积分。 */
export const UNLOCK_BENEFITS = [
  { id: 'park5', level: 6, title: '游学乐园 5 分钟' },
  { id: 'park10', level: 11, title: '游学乐园 10 分钟' },
  { id: 'park15', level: 21, title: '游学乐园 15 分钟' },
  { id: 'seat', level: 21, title: '优先选机位' },
  { id: 'park30', level: 31, title: '游学乐园 30 分钟' },
  { id: 'mystery', level: 31, title: '神秘大奖' }
]

/**
 * 消耗可兑积分。到 11 级后才能换小礼物。
 * 神秘大奖没有写明扣多少分，不放进兑换清单。
 */
export const REDEEM_BENEFITS = [
  { id: 'stationery', title: '精美文具', cost: 30, minLevel: 11 },
  { id: 'toy', title: '小玩具', cost: 80, minLevel: 11 },
  { id: 'certificate', title: '奖状', cost: 100, minLevel: 11 }
]

export function growthFromPoints(points) {
  const safe = Math.max(0, Math.floor(Number(points) || 0))
  const stars = Math.floor(safe / POINTS_PER_STAR)
  const moons = Math.floor(stars / STARS_PER_MOON)
  const suns = Math.floor(moons / MOONS_PER_SUN)
  return {
    points: safe,
    stars,
    moons,
    suns,
    level: stars,
    holdStars: stars % STARS_PER_MOON,
    holdMoons: moons % MOONS_PER_SUN,
    holdSuns: suns
  }
}

export function parkMinutes(spendablePoints) {
  return Math.floor(Math.max(0, Math.floor(Number(spendablePoints) || 0)) / PARK_POINTS_PER_MINUTE)
}

/** 下一颗星星；手里已有 5 颗时，下一档是月亮。 */
export function nextShine(earnedPoints) {
  const g = growthFromPoints(earnedPoints)
  const rest = g.points % POINTS_PER_STAR
  const gapPoints = rest === 0 ? POINTS_PER_STAR : POINTS_PER_STAR - rest
  if (g.holdStars === STARS_PER_MOON - 1 && g.holdMoons === MOONS_PER_SUN - 1) {
    return { gapPoints, title: '1 个太阳' }
  }
  if (g.holdStars === STARS_PER_MOON - 1) {
    return { gapPoints, title: '1 个月亮' }
  }
  return { gapPoints, title: '1 颗星星' }
}

export function nextMilestone(earnedPoints) {
  const g = growthFromPoints(earnedPoints)
  const upcoming = UNLOCK_BENEFITS.find((item) => item.level > g.level)
  if (upcoming) {
    const needPoints = upcoming.level * POINTS_PER_STAR
    return {
      title: upcoming.title,
      gapPoints: needPoints - g.points,
      gapLevels: upcoming.level - g.level
    }
  }
  const nextSunPoints = (g.suns + 1) * MOONS_PER_SUN * STARS_PER_MOON * POINTS_PER_STAR
  return {
    title: '再集齐 1 个太阳',
    gapPoints: nextSunPoints - g.points,
    gapLevels: Math.ceil((nextSunPoints - g.points) / POINTS_PER_STAR)
  }
}

export function cheerForGain(beforePoints, afterPoints) {
  const before = growthFromPoints(beforePoints)
  const after = growthFromPoints(afterPoints)
  if (after.suns > before.suns) {
    const n = after.suns - before.suns
    return n === 1
      ? '你集齐了 6 个月亮，换到 1 个太阳啦！'
      : `你一下集齐了 ${n} 个太阳，太厉害啦！`
  }
  if (after.moons > before.moons) {
    const n = after.moons - before.moons
    return n === 1
      ? '你又集齐了 6 颗星星，换到 1 个月亮啦！'
      : `你又换到了 ${n} 个月亮，继续保持！`
  }
  if (after.stars > before.stars) {
    const n = after.stars - before.stars
    return n === 1 ? '你又点亮了 1 颗星星！' : `你又点亮了 ${n} 颗星星！`
  }
  const rest = POINTS_PER_STAR - (after.points % POINTS_PER_STAR || POINTS_PER_STAR)
  if (after.points % POINTS_PER_STAR === 0) return '积分已经记下啦。'
  return `积分已经记下啦，再拿 ${rest} 分就能点亮下一颗星星。`
}

export function unlockedBenefits(earnedPoints) {
  const level = growthFromPoints(earnedPoints).level
  return UNLOCK_BENEFITS.filter((item) => level >= item.level)
}

export function redeemStatus(earnedPoints, spendablePoints) {
  const level = growthFromPoints(earnedPoints).level
  const spendable = Math.max(0, Math.floor(Number(spendablePoints) || 0))
  return REDEEM_BENEFITS.map((item) => {
    if (level < item.minLevel) {
      return {
        ...item,
        ok: false,
        hint: `还差 ${item.minLevel - level} 级`
      }
    }
    if (spendable < item.cost) {
      return {
        ...item,
        ok: false,
        hint: `还差 ${item.cost - spendable} 积分`
      }
    }
    return { ...item, ok: true, hint: `${item.cost} 积分可换` }
  })
}

export function formatStatusCard({ name, earned, spendable, delta, taskName }) {
  const g = growthFromPoints(earned)
  const mile = nextMilestone(earned)
  const unlocked = unlockedBenefits(earned)
  const redeems = redeemStatus(earned, spendable)
  const gain = delta > 0 && taskName
    ? `（本笔 +${delta} 来自「${taskName}」）`
    : ''
  const perkText = unlocked.length
    ? unlocked.map((item) => item.title).join('、')
    : '还没有解锁的权益，完成课堂任务就能向前一步'
  const redeemText = redeems
    .map((item) => (item.ok ? `${item.title}（${item.cost} 积分）` : `${item.title}（${item.hint}）`))
    .join('、')
  return [
    `[学生] ${name || '同学'}`,
    `积分：可兑 ${Math.max(0, Math.floor(spendable || 0))}，累计 ${g.points}${gain}`,
    `⭐星星 ×${g.holdStars}（一共 ${g.stars}）｜ 🌙月亮 ×${g.holdMoons} ｜ ☀️太阳 ×${g.holdSuns} ｜ 等级：${g.level}`,
    `距下一个里程碑：还差 ${mile.gapPoints} 积分（还差 ${mile.gapLevels} 级）解锁「${mile.title}」`,
    `当前可享权益：${perkText}`,
    `可兑换：${redeemText}`
  ].join('\n')
}
