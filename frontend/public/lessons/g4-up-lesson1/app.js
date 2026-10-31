const params = new URLSearchParams(location.search);
const IS_EMBEDDED = params.get('embedded') === '1';
const IS_PREVIEW = params.get('preview') === '1' || localStorage.getItem('role') === 'TEACHER';
const ACTIVITY = parseInt(params.get('activityIndex') || params.get('step') || '0', 10);

const scenarios = [
  { id: 'sound', label: '场景 1', img: 'img/sound.jpeg', desc: '图书馆里，小朋友听到平板电脑中传出的故事音频。', answer: '声音数据', knowledge: '声音数据可以记录讲话、音乐、故事等声音内容。我们平时听的音频、录音，都属于声音数据。' },
  { id: 'text', label: '场景 2', img: 'img/text.jpeg', desc: '教室墙上贴着课程表，大家通过文字安排每天的学习。', answer: '文字数据', knowledge: '文字数据由字、词、句子组成，用来表达信息、记录知识和说明规则。' },
  { id: 'number', label: '场景 3', img: 'img/number.jpeg', desc: '校园里的电子温度计显示今天是 28℃。', answer: '数值数据', knowledge: '数值数据是用数字表示的信息，可以表示温度、身高、成绩和数量。' },
  { id: 'image', label: '场景 4', img: 'img/image.jpeg', desc: '平板里保存着许多照片，能看见一张张生活画面。', answer: '图像数据', knowledge: '图像数据记录画面和图形，比如照片、图画、截图等。' },
  { id: 'video', label: '场景 5', img: 'img/video.jpeg', desc: '科学课上，大家用视频观察植物实验的变化过程。', answer: '视频数据', knowledge: '视频数据是连续变化的画面，还常常带有声音，可以记录实验和活动过程。' }
];

const cats = {
  number: { name: '数值', hint: '温度、成绩、数量', mark: '数' },
  text: { name: '文字', hint: '姓名、通知、课程表', mark: '文' },
  image: { name: '图像', hint: '照片、图画、截图', mark: '图' },
  audio: { name: '声音', hint: '录音、音乐、铃声', mark: '声' },
  video: { name: '视频', hint: '录像、实验过程', mark: '视' }
};

const level2Items = [
  { text: '今天气温 28℃', type: 'number' },
  { text: '考试分数 95 分', type: 'number' },
  { text: '姓名：小明', type: 'text' },
  { text: '课程表', type: 'text' },
  { text: '校园风景照', type: 'image' },
  { text: '家庭合影', type: 'image' },
  { text: '朗读课文录音', type: 'audio' },
  { text: '上课铃声', type: 'audio' },
  { text: '运动会录像', type: 'video' },
  { text: '科学实验记录', type: 'video' }
];

function readStudent() {
  const name = localStorage.getItem('realName') || '';
  const username = localStorage.getItem('username') || '';
  const className = localStorage.getItem('className') || '';
  const userId = localStorage.getItem('userId') || '';
  const role = localStorage.getItem('role') || '';
  return { name, username, className, userId, role };
}

let student = readStudent();
let state = {
  page: 'intro',
  score: 0,
  combo: 0,
  maxCombo: 0,
  correct: 0,
  attempts: 0,
  levelProgress: { level1: 0, level2: 0 },
  startedAt: 0,
  l1Map: {},
  l1Revealed: false,
  l2Placed: 0,
  l2Current: []
};

const $ = (id) => document.getElementById(id);

function show(id) {
  document.querySelectorAll('.page').forEach((x) => x.classList.remove('active'));
  $(id).classList.add('active');
  state.page = id;
  window.scrollTo(0, 0);
  updateStats();
}

function toast(msg, good = true) {
  const t = $('toast');
  t.textContent = msg;
  t.className = 'toast ' + (good ? 'good' : 'bad') + ' show';
  setTimeout(() => t.classList.remove('show'), 1300);
}

let audioCtx = null;
function tone(ok) {
  try {
    const AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return;
    if (!audioCtx) audioCtx = new AC();
    if (audioCtx.state === 'suspended') audioCtx.resume();
    const o = audioCtx.createOscillator();
    const g = audioCtx.createGain();
    o.type = ok ? 'sine' : 'square';
    o.frequency.value = ok ? 660 : 220;
    o.connect(g);
    g.connect(audioCtx.destination);
    g.gain.value = 0.08;
    o.start();
    o.stop(audioCtx.currentTime + (ok ? 0.18 : 0.25));
  } catch (e) { /* ignore */ }
}

function updateStats() {
  ['score1', 'score2'].forEach((id) => { if ($(id)) $(id).textContent = state.score; });
  ['combo1', 'combo2'].forEach((id) => { if ($(id)) $(id).textContent = state.combo; });
  const label = state.page === 'level1' ? '环节1' : state.page === 'level2' ? '环节2' : state.page === 'result' ? '已完成' : '准备出发';
  if ($('headerLevel')) $('headerLevel').textContent = label;
}

function addScore() {
  state.correct++;
  state.attempts++;
  state.combo++;
  state.maxCombo = Math.max(state.maxCombo, state.combo);
  state.score += 10 + (state.combo >= 5 ? 5 : state.combo >= 3 ? 2 : 0);
  tone(true);
  updateStats();
}

function wrong() {
  state.attempts++;
  state.combo = 0;
  tone(false);
  updateStats();
}

function resetRun() {
  state.score = 0;
  state.combo = 0;
  state.maxCombo = 0;
  state.correct = 0;
  state.attempts = 0;
  state.levelProgress = { level1: 0, level2: 0 };
  state.startedAt = Date.now();
  state.l1Map = {};
  state.l1Revealed = false;
  state.l2Placed = 0;
  state.l2Current = [];
  hideDone();
  updateStats();
}

function confetti() {
  const box = $('confetti');
  if (!box) return;
  if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  box.innerHTML = '';
  const colors = ['#ff6b8a', '#ffb84d', '#7ad7c5', '#7d9cff', '#b89cff'];
  const n = 12;
  for (let i = 0; i < n; i++) {
    const s = document.createElement('span');
    s.style.left = Math.random() * 100 + '%';
    s.style.top = (-10 - Math.random() * 30) + 'px';
    s.style.background = colors[i % colors.length];
    s.style.animationDelay = Math.random() * 0.35 + 's';
    box.appendChild(s);
  }
  setTimeout(() => { box.innerHTML = ''; }, 1400);
}

function bindStudentHeader() {
  student = readStudent();
  const el = $('studentInfo');
  if (!el) return;
  if (IS_PREVIEW || student.role === 'TEACHER') {
    el.textContent = student.name ? ('教师 · ' + student.name) : '教师预览';
    return;
  }
  const parts = [];
  if (student.name) parts.push(student.name);
  if (student.username) parts.push('学号 ' + student.username);
  else if (student.className) parts.push(student.className);
  el.textContent = parts.length ? parts.join(' · ') : '已登录';
}

const learnSpaceNotifiedSteps = new Set();
function notifyLearnSpaceComplete(step, extra) {
  if (!window.parent || window.parent === window) return;
  if (learnSpaceNotifiedSteps.has(step)) return;
  learnSpaceNotifiedSteps.add(step);
  const idx = step;
  window.parent.postMessage({
    type: 'LEARN_SPACE_SUBMIT',
    payload: Object.assign({
      activityIndex: idx,
      step,
      stepCompleted: true,
      source: 'g4-up-lesson1',
      score: state.score,
      studentName: student.name,
      username: student.username
    }, extra || {})
  }, '*');
}

function hideDone() {
  const b = $('doneBanner');
  if (b) b.hidden = true;
}

function showDone(title, desc) {
  const b = $('doneBanner');
  if (!b) return;
  $('doneTitle').textContent = title;
  $('doneDesc').textContent = desc;
  b.hidden = false;
}

function enterLevel1() {
  resetRun();
  buildL1();
  show('level1');
}

function enterLevel2() {
  resetRun();
  buildL2();
  show('level2');
}

function buildL1() {
  state.l1Map = {};
  state.l1Revealed = false;
  hideDone();
  $('l1Submit').disabled = true;
  $('l1Count').textContent = '0/5';
  $('l1Progress').style.width = '0%';
  const grid = $('sceneGrid');
  const bank = $('chipBank');
  grid.innerHTML = '';
  bank.innerHTML = '';
  scenarios.forEach((s) => {
    const c = document.createElement('div');
    c.className = 'scene-card';
    c.dataset.id = s.id;
    c.innerHTML = '<div class="scene-media"><img src="' + s.img + '" alt="' + s.label + '" width="720" height="405" decoding="async"><span class="badge">' + s.label + '</span></div>'
      + '<div class="desc">' + s.desc + '</div>'
      + '<div class="slot" data-slot="' + s.id + '">把数据类型放到这里</div>'
      + '<div class="feedback" data-feed="' + s.id + '"></div>';
    grid.appendChild(c);
  });
  scenarios.map((s) => s.answer).sort(() => Math.random() - 0.5).forEach((type) => {
    const ch = document.createElement('div');
    ch.className = 'chip';
    ch.draggable = true;
    ch.dataset.type = type;
    ch.textContent = type;
    bank.appendChild(ch);
    bindChip(ch);
  });
  document.querySelectorAll('.slot').forEach(bindSlot);
  updateStats();
}

let selectedChip = null;
function bindChip(ch) {
  ch.addEventListener('click', () => {
    if (state.l1Revealed) return;
    document.querySelectorAll('.chip').forEach((x) => x.classList.remove('selected'));
    selectedChip = selectedChip === ch ? null : ch;
    if (selectedChip) ch.classList.add('selected');
  });
  ch.addEventListener('dragstart', (e) => {
    if (state.l1Revealed) {
      e.preventDefault();
      return;
    }
    e.dataTransfer.setData('text/plain', ch.dataset.type);
    selectedChip = ch;
  });
}

function bindSlot(slot) {
  slot.addEventListener('dragover', (e) => {
    e.preventDefault();
    slot.closest('.scene-card').classList.add('drag-over');
  });
  slot.addEventListener('dragleave', () => slot.closest('.scene-card').classList.remove('drag-over'));
  slot.addEventListener('drop', (e) => {
    e.preventDefault();
    slot.closest('.scene-card').classList.remove('drag-over');
    placeL1(e.dataTransfer.getData('text/plain'), slot.dataset.slot);
  });
  slot.addEventListener('click', () => {
    if (selectedChip) placeL1(selectedChip.dataset.type, slot.dataset.slot);
  });
}

function placeL1(type, id) {
  if (state.l1Revealed || !type) return;
  Object.keys(state.l1Map).forEach((k) => {
    if (state.l1Map[k] === type) delete state.l1Map[k];
  });
  state.l1Map[id] = type;
  selectedChip = null;
  document.querySelectorAll('.chip').forEach((x) => x.classList.remove('selected'));
  scenarios.forEach((s) => {
    const sl = document.querySelector('[data-slot="' + s.id + '"]');
    sl.textContent = state.l1Map[s.id] || '把数据类型放到这里';
    sl.classList.toggle('filled', !!state.l1Map[s.id]);
  });
  const n = Object.keys(state.l1Map).length;
  $('l1Count').textContent = n + '/5';
  $('l1Progress').style.width = (n * 20) + '%';
  $('l1Submit').disabled = n !== 5;
}

function onL1Submit() {
  if (Object.keys(state.l1Map).length !== 5) return;
  state.l1Revealed = true;
  let right = 0;
  scenarios.forEach((s) => {
    const card = document.querySelector('[data-id="' + s.id + '"]');
    const feed = document.querySelector('[data-feed="' + s.id + '"]');
    const ok = state.l1Map[s.id] === s.answer;
    card.classList.remove('correct', 'wrong');
    feed.className = 'feedback show ' + (ok ? 'good' : 'bad');
    if (ok) {
      right++;
      card.classList.add('correct');
      feed.textContent = '答对啦：这是' + s.answer + '。';
      addScore();
    } else {
      card.classList.add('wrong');
      feed.textContent = '正确答案是“' + s.answer + '”。' + s.knowledge;
      wrong();
    }
  });
  state.levelProgress.level1 = right;
  toast(right === 5 ? '环节1全对，太棒了！' : '环节1完成，继续观察知识点！', right === 5);
  $('l1Submit').disabled = true;
  confetti();
  notifyLearnSpaceComplete(1, { level1Correct: right, total: 5 });
  if (IS_EMBEDDED) {
    showDone('环节1已完成', '正在为你打开环节2：分类岛屿。');
  } else {
    setTimeout(() => enterLevel2(), 1400);
  }
}

function buildL2() {
  state.l2Placed = 0;
  hideDone();
  state.l2Current = level2Items.map((x) => Object.assign({}, x, { id: 'd' + Math.random().toString(36).slice(2) }))
    .sort(() => Math.random() - 0.5);
  $('l2Count').textContent = '0/10';
  $('l2Progress').style.width = '0%';
  const area = $('dragArea');
  const grid = $('typeGrid');
  area.innerHTML = '';
  grid.innerHTML = '';
  state.l2Current.forEach((item) => {
    const el = document.createElement('div');
    el.className = 'drag-card';
    el.draggable = true;
    el.dataset.type = item.type;
    el.dataset.id = item.id;
    el.textContent = item.text;
    area.appendChild(el);
    bindDrag2(el);
  });
  Object.entries(cats).forEach(([key, c]) => {
    const z = document.createElement('div');
    z.className = 'type-zone';
    z.dataset.type = key;
    z.innerHTML = '<div class="type-head"><div class="type-dot">' + c.mark + '</div><div><b>' + c.name + '</b><small>' + c.hint + '</small></div></div><div class="zone-items"></div>';
    grid.appendChild(z);
    bindZone2(z);
  });
  updateStats();
}

let selected2 = null;
function bindDrag2(el) {
  el.onclick = () => {
    if (el.classList.contains('placed')) return;
    document.querySelectorAll('.drag-card').forEach((x) => { x.style.outline = ''; });
    selected2 = selected2 === el ? null : el;
    if (selected2) el.style.outline = '4px solid #ffd27c';
  };
  el.ondragstart = (e) => {
    e.dataTransfer.setData('text/plain', el.dataset.id);
    selected2 = el;
  };
}

function bindZone2(z) {
  z.ondragover = (e) => { e.preventDefault(); z.classList.add('drag-over'); };
  z.ondragleave = () => z.classList.remove('drag-over');
  z.ondrop = (e) => {
    e.preventDefault();
    z.classList.remove('drag-over');
    const el = document.querySelector('[data-id="' + e.dataTransfer.getData('text/plain') + '"]');
    drop2(el, z);
  };
  z.onclick = () => { if (selected2) drop2(selected2, z); };
}

function drop2(el, z) {
  if (!el || el.classList.contains('placed')) return;
  const ok = el.dataset.type === z.dataset.type;
  if (ok) {
    el.classList.add('correct', 'placed');
    z.classList.add('correct');
    z.querySelector('.zone-items').insertAdjacentHTML('beforeend', '<span class="mini">' + el.textContent + '</span>');
    state.l2Placed++;
    state.levelProgress.level2 = state.l2Placed;
    addScore();
    toast(state.combo >= 3 ? '连击 ' + state.combo + '！' : '分类正确！', true);
    setTimeout(() => z.classList.remove('correct'), 500);
    $('l2Count').textContent = state.l2Placed + '/10';
    $('l2Progress').style.width = (state.l2Placed * 10) + '%';
    if (state.l2Placed === 10) {
      confetti();
      notifyLearnSpaceComplete(2, { level2Correct: 10, total: 10 });
      setTimeout(showResult, 900);
    }
  } else {
    el.classList.add('wrong');
    wrong();
    toast('再想想：它属于' + cats[el.dataset.type].name + '数据', false);
    setTimeout(() => el.classList.remove('wrong'), 500);
  }
}

function formatTime(s) {
  s = Math.max(0, Math.floor(s));
  return Math.floor(s / 60) + ':' + String(s % 60).padStart(2, '0');
}

function showResult() {
  const elapsed = Math.max(1, Math.floor((Date.now() - state.startedAt) / 1000));
  const accuracy = state.attempts ? Math.round(state.correct / state.attempts * 100) : 0;
  const stars = accuracy >= 90 ? 3 : accuracy >= 70 ? 2 : accuracy >= 50 ? 1 : 0;
  $('finalScore').textContent = state.score;
  $('finalAccuracy').textContent = accuracy + '%';
  $('finalTime').textContent = formatTime(elapsed);
  $('finalCombo').textContent = state.maxCombo;
  $('stars').innerHTML = [0, 1, 2].map((i) => '<span class="star ' + (i < stars ? 'on' : '') + '">★</span>').join('');
  $('resultTitle').textContent = stars === 3 ? '数据探险大达人！' : stars === 2 ? '表现很不错！' : stars === 1 ? '继续努力，再来一次！' : '加油，数据小侦探！';
  const who = student.name ? student.name + '，' : '';
  $('resultDesc').textContent = who + '你已经完成两个环节：看场景匹配类型，再把数据分到对应岛屿。';
  $('saveStatus').textContent = IS_EMBEDDED ? '本课进度已记录到学习空间。' : '本课进度已保存在当前浏览器。';
  show('result');
  confetti();
}

function boot() {
  bindStudentHeader();
  if (IS_EMBEDDED) document.documentElement.classList.add('embedded');

  const startBtn = $('startBtn');
  if (startBtn) startBtn.onclick = enterLevel1;
  $('l1Submit').onclick = onL1Submit;
  $('l1Reset').onclick = buildL1;
  $('l2Reset').onclick = buildL2;
  const againBtn = $('againBtn');
  if (againBtn) {
    againBtn.onclick = () => {
      if (IS_EMBEDDED && ACTIVITY === 2) enterLevel2();
      else enterLevel1();
    };
  }

  if (IS_EMBEDDED) {
    if (ACTIVITY === 2) enterLevel2();
    else enterLevel1();
  } else {
    show('intro');
  }
}

boot();
