export const MALL_ICONS = {
  star_future_2: '/mall/star-future.jpg',
  snack: '/mall/snack.jpg',
  stationery: '/mall/stationery.jpg',
  praise_letter: '/mall/praise-letter.jpg',
  milk_tea: '/mall/milk-tea.jpg',
  info_blindbox: '/mall/blindbox.jpg',
  星未来积分2: '/mall/star-future.jpg',
  零食1份: '/mall/snack.jpg',
  文具1份: '/mall/stationery.jpg',
  表扬信: '/mall/praise-letter.jpg',
  奶茶1份: '/mall/milk-tea.jpg',
  信息盲盒小礼物: '/mall/blindbox.jpg'
}

export function mallIconSrc(item) {
  if (!item) return ''
  return MALL_ICONS[item.code] || MALL_ICONS[item.name] || MALL_ICONS[item.itemName] || ''
}
