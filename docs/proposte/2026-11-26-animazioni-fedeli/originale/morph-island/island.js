// Morph Island
// One black pill whose width, height and radius are springs. Width is stiffer than
// height, so every change lands sideways first and then drops into place.
// Content cross-fades with blur while the shape morphs. Autoplays until touched.
(() => {
  const { S, tune, step, clamp, spring, num, after, cancel } = Motion;
  const $ = id => document.getElementById(id);
  const island = $('island');
  const contents = [...island.querySelectorAll('.ic')];
  const ctls = [...document.querySelectorAll('.ctl')];

  const T = {
    w: spring('--island-w', '380 24'), h: spring('--island-h', '300 22'),
    radius: num('--island-radius', 32), cycle: num('--island-cycle-ms', 2800) / 1000,
  };
  const SIZE = { idle: [126, 36], timer: [230, 36], upload: [290, 56], call: [340, 84] };

  const W = S(SIZE.idle[0], ...T.w), H = S(SIZE.idle[1], ...T.h), press = S(1, 520, 18);
  let state = 'idle', auto = true, autoTimer = null, clock = 0;
  let timerLeft = 299, upload = 0;

  function go(next, fromUser = false) {
    if (fromUser) stopAuto();
    if (next === state) { press.v -= 2; return; }
    const [w, h] = SIZE[next];
    const grow = w * h > W.t * H.t;
    tune(W, ...T.w); tune(H, T.h[0] * (grow ? 1 : 1.3), T.h[1]);
    W.t = w; H.t = h;
    press.v -= grow ? 1.6 : 1;
    state = next;
    contents.forEach(c => c.classList.toggle('on', c.dataset.s === next));
    ctls.forEach(b => b.setAttribute('aria-pressed', b.dataset.go === next));
    if (next === 'timer') timerLeft = 299;
    if (next === 'upload') upload = 0;
  }

  // autoplay: a little story on loop until the viewer touches anything
  const story = ['timer', 'upload', 'call', 'idle'];
  let storyIdx = 0;
  function tickAuto() {
    if (!auto) return;
    go(story[storyIdx++ % story.length]);
    autoTimer = after(T.cycle, tickAuto);
  }
  function stopAuto() { auto = false; if (autoTimer) cancel(autoTimer); }

  ctls.forEach(b => b.addEventListener('click', () => go(b.dataset.go, true)));
  $('accept').addEventListener('click', e => { e.stopPropagation(); go('idle', true); });
  $('decline').addEventListener('click', e => { e.stopPropagation(); go('idle', true); });
  island.addEventListener('pointerdown', () => { press.t = .96; });
  addEventListener('pointerup', () => { press.t = 1; });
  // tapping the compact island expands it to the next richer state
  island.addEventListener('click', () => {
    stopAuto();
    if (state === 'idle') go('timer'); else if (state === 'timer') go('upload'); else if (state === 'upload') go('call'); else if (state === 'call') go('idle');
  });

  const fmt = s => `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(Math.floor(s % 60)).padStart(2, '0')}`;
  const ring = $('ring'), pct = $('pct'), timerText = $('timerText');

  Motion.register({
    frame(dt) {
      clock += dt;
      step(W, dt); step(H, dt); step(press, dt);
      if (state === 'timer') timerLeft = Math.max(0, timerLeft - dt);
      if (state === 'upload') upload = Math.min(1, upload + dt / 2.4);
    },
    render() {
      const h = Math.max(H.x, 20);
      // volume: while height is moving fast the width gives back a little
      const give = clamp(-H.v / 7000, -.05, .05);
      island.style.width = W.x * (1 + give) + 'px';
      island.style.height = h + 'px';
      island.style.borderRadius = Math.min(h / 2, T.radius) + 'px';
      island.style.transform = `translateX(-50%) scale(${press.x})`;
      timerText.textContent = fmt(timerLeft);
      ring.style.strokeDashoffset = 81.7 * (1 - upload);
      pct.textContent = Math.round(upload * 100) + '%';
    },
  });

  contents.forEach(c => c.classList.toggle('on', c.dataset.s === 'idle'));
  ctls.forEach(b => b.setAttribute('aria-pressed', b.dataset.go === 'idle'));
  after(.6, tickAuto);
})();
