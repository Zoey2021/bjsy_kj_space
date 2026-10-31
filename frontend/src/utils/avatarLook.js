const FILE = {
  'boy-hair-short': 'avatar-boy-base.png',
  'boy-hair-sport': 'avatar-boy-hair-cowlick.png',
  'boy-hair-side': 'avatar-boy-hair-sidepart.png',
  'boy-hair-wavy': 'avatar-boy-hair-wavy.png',
  'boy-top-shirt': 'avatar-boy-base.png',
  'boy-top-jersey': 'avatar-boy-outfit-jersey.png',
  'boy-top-jacket': 'avatar-boy-outfit-hoodie.png',
  'boy-top-sport': 'avatar-boy-outfit-sport.png',
  'boy-top-tech': 'avatar-boy-outfit-tech.png',
  'boy-extra-none': 'avatar-boy-base.png',
  'boy-extra-glasses': 'avatar-boy-acc-glasses.png',
  'boy-extra-cap': 'avatar-boy-acc-cap.png',
  'girl-hair-bob': 'avatar-girl-base.png',
  'girl-hair-twin': 'avatar-girl-hair-pigtails.png',
  'girl-hair-long': 'avatar-girl-hair-longstar.png',
  'girl-hair-bun': 'avatar-girl-hair-bun.png',
  'girl-top-dress': 'avatar-girl-base.png',
  'girl-top-uniform': 'avatar-girl-outfit-uniform.png',
  'girl-top-coat': 'avatar-girl-outfit-cardigan.png',
  'girl-top-sport': 'avatar-girl-outfit-sport.png',
  'girl-extra-none': 'avatar-girl-base.png',
  'girl-extra-bow': 'avatar-girl-acc-bow.png',
  'girl-extra-glasses': 'avatar-girl-acc-glasses.png'
}

const DEFAULTS = {
  boy: { hair: 'boy-hair-short', outfit: 'boy-top-shirt', extra: 'boy-extra-none' },
  girl: { hair: 'girl-hair-bob', outfit: 'girl-top-dress', extra: 'girl-extra-none' }
}

export function avatarItemSrc(id) {
  const file = FILE[id]
  return file ? `/avatar/thumbs/${file}` : ''
}

function lookSrc(id) {
  const file = FILE[id]
  return file ? `/avatar/looks/${file}` : ''
}

export function avatarLayers(figure, hair, outfit, extra) {
  const fig = figure === 'girl' ? 'girl' : 'boy'
  const base = DEFAULTS[fig]
  const h = hair || base.hair
  const o = outfit || base.outfit
  const e = extra || base.extra
  return {
    base: lookSrc(fig === 'girl' ? 'girl-hair-bob' : 'boy-hair-short'),
    outfit: o === base.outfit ? '' : lookSrc(o),
    hair: h === base.hair ? '' : lookSrc(h),
    extra: e === base.extra ? '' : lookSrc(e)
  }
}
