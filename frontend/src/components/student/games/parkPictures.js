/** 不依赖系统表情字体，学校电脑上也能画出水果和配对图案。 */

export const FRUIT_KINDS = ['apple', 'orange', 'lemon', 'grape']

export function drawFruit(ctx, kind, x, y) {
  ctx.save()
  if (kind === 'orange') {
    ctx.fillStyle = '#f97316'
    ctx.beginPath()
    ctx.arc(x, y, 15, 0, Math.PI * 2)
    ctx.fill()
    ctx.strokeStyle = '#fdba74'
    ctx.lineWidth = 1.5
    ctx.beginPath()
    ctx.arc(x, y, 9, 0.4, 1.4)
    ctx.stroke()
    ctx.fillStyle = '#166534'
    ctx.fillRect(x - 2, y - 20, 4, 7)
  } else if (kind === 'lemon') {
    ctx.fillStyle = '#facc15'
    ctx.beginPath()
    ctx.ellipse(x, y, 16, 12, -0.4, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = '#a16207'
    ctx.beginPath()
    ctx.ellipse(x - 14, y - 4, 3, 2, 0, 0, Math.PI * 2)
    ctx.fill()
  } else if (kind === 'grape') {
    ctx.fillStyle = '#7c3aed'
    const dots = [[0, 2], [-8, 8], [8, 8], [-4, 16], [4, 16], [0, 22]]
    dots.forEach(([dx, dy]) => {
      ctx.beginPath()
      ctx.arc(x + dx, y - 8 + dy, 6, 0, Math.PI * 2)
      ctx.fill()
    })
    ctx.strokeStyle = '#166534'
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.moveTo(x, y - 10)
    ctx.quadraticCurveTo(x + 8, y - 22, x + 14, y - 16)
    ctx.stroke()
  } else {
    ctx.fillStyle = '#ef4444'
    ctx.beginPath()
    ctx.arc(x, y, 15, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = '#b91c1c'
    ctx.beginPath()
    ctx.ellipse(x - 5, y - 2, 5, 8, -0.5, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = '#166534'
    ctx.fillRect(x - 2, y - 22, 4, 9)
    ctx.fillStyle = '#22c55e'
    ctx.beginPath()
    ctx.ellipse(x + 8, y - 18, 8, 4, 0.4, 0, Math.PI * 2)
    ctx.fill()
  }
  ctx.restore()
}

export function drawBasket(ctx, x, y) {
  ctx.save()
  ctx.fillStyle = '#b45309'
  ctx.beginPath()
  ctx.moveTo(x + 8, y)
  ctx.lineTo(x + 72, y)
  ctx.lineTo(x + 64, y + 28)
  ctx.lineTo(x + 16, y + 28)
  ctx.closePath()
  ctx.fill()
  ctx.strokeStyle = '#fde68a'
  ctx.lineWidth = 2
  for (let i = 0; i < 4; i += 1) {
    const lx = x + 20 + i * 12
    ctx.beginPath()
    ctx.moveTo(lx, y + 4)
    ctx.lineTo(lx - 2, y + 24)
    ctx.stroke()
  }
  ctx.strokeStyle = '#92400e'
  ctx.lineWidth = 3
  ctx.beginPath()
  ctx.arc(x + 40, y - 2, 22, Math.PI * 1.15, Math.PI * 1.85)
  ctx.stroke()
  ctx.restore()
}

export const MEMORY_ICONS = [
  {
    id: 'apple',
    svg: '<svg viewBox="0 0 64 64"><circle cx="32" cy="38" r="16" fill="#ef4444"/><rect x="30" y="16" width="4" height="10" rx="2" fill="#166534"/><ellipse cx="40" cy="18" rx="8" ry="4" fill="#22c55e"/></svg>'
  },
  {
    id: 'star',
    svg: '<svg viewBox="0 0 64 64"><polygon points="32,8 39,26 58,26 43,38 49,56 32,45 15,56 21,38 6,26 25,26" fill="#f59e0b"/></svg>'
  },
  {
    id: 'ball',
    svg: '<svg viewBox="0 0 64 64"><circle cx="32" cy="32" r="18" fill="#fff" stroke="#0f172a" stroke-width="3"/><path d="M14 32h36M32 14c6 6 6 30 0 36M32 14c-6 6-6 30 0 36" fill="none" stroke="#0f172a" stroke-width="2"/></svg>'
  },
  {
    id: 'book',
    svg: '<svg viewBox="0 0 64 64"><path d="M12 16h18c4 0 6 2 6 6v28c-4-3-8-4-12-4H12z" fill="#2563eb"/><path d="M52 16H34c-4 0-6 2-6 6v28c4-3 8-4 12-4h12z" fill="#60a5fa"/></svg>'
  },
  {
    id: 'cat',
    svg: '<svg viewBox="0 0 64 64"><polygon points="16,28 22,12 30,26" fill="#fb923c"/><polygon points="48,28 42,12 34,26" fill="#fb923c"/><circle cx="32" cy="36" r="16" fill="#fdba74"/><circle cx="26" cy="34" r="2.5" fill="#1e293b"/><circle cx="38" cy="34" r="2.5" fill="#1e293b"/><path d="M32 38l-3 3h6z" fill="#ea580c"/></svg>'
  },
  {
    id: 'rocket',
    svg: '<svg viewBox="0 0 64 64"><path d="M32 8c10 8 12 20 12 28H20c0-8 2-20 12-28z" fill="#e2e8f0"/><circle cx="32" cy="24" r="4" fill="#38bdf8"/><path d="M20 36l-8 10 12-4zM44 36l8 10-12-4z" fill="#f97316"/></svg>'
  },
  {
    id: 'note',
    svg: '<svg viewBox="0 0 64 64"><circle cx="22" cy="44" r="8" fill="#7c3aed"/><circle cx="42" cy="38" r="8" fill="#7c3aed"/><rect x="28" y="14" width="4" height="30" fill="#6d28d9"/><rect x="48" y="10" width="4" height="28" fill="#6d28d9"/><rect x="28" y="14" width="24" height="4" fill="#6d28d9"/></svg>'
  },
  {
    id: 'flower',
    svg: '<svg viewBox="0 0 64 64"><circle cx="32" cy="18" r="7" fill="#f472b6"/><circle cx="18" cy="32" r="7" fill="#f472b6"/><circle cx="46" cy="32" r="7" fill="#f472b6"/><circle cx="24" cy="46" r="7" fill="#f472b6"/><circle cx="40" cy="46" r="7" fill="#f472b6"/><circle cx="32" cy="32" r="7" fill="#facc15"/></svg>'
  }
]
