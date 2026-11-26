// Hold Button
// Holding fills the pill from the left (the label inverts exactly under the fill).
// Let go early: the fill springs back past zero and the pill shakes "no".
// Hold to the end: the pill morphs into a green circle and the check draws itself.
(() => {
  const { S, tune, step, spring, num, after } = Motion;
  const $ = id => document.getElementById(id);
  const btn = $('hold'), fill = $('fill'), top = $('topLabel'), sub = $('sub');

  const T = {
    holdSec: num('--hold-ms', 1200) / 1000, release: spring('--hold-release', '260 14'),
    morph: spring('--hold-morph', '320 20'), doneSec: num('--hold-done-ms', 1700) / 1000,
  };
  const FULL = 220, DOT = 56;

  const p = S(0, ...T.release);        // fill progress 0..1 (driven directly while holding)
  const W = S(FULL, ...T.morph);       // pill width
  const sc = S(1, 520, 20);            // press squash
  const shake = S(0, 700, 9);          // "no" wobble on early release
  let mode = 'idle', holding = false;

  function setSub(text) {
    if (!sub) return; // the caption was removed (Noah, 9 Nov 2026)
    sub.classList.add('swap');
    after(.18, () => { sub.textContent = text; sub.classList.remove('swap'); });
  }

  function start() {
    if (mode !== 'idle') return;
    holding = true; mode = 'holding';
    sc.t = .96;
  }
  function stop() {
    if (mode !== 'holding') return;
    holding = false; mode = 'idle';
    sc.t = 1;
    if (p.x < 1) {
      tune(p, ...T.release); p.t = 0;   // spring back, overshooting a hair below zero
      shake.v += 260 * Math.min(1, p.x * 2 + .3);
    }
  }
  function complete() {
    holding = false; mode = 'done';
    btn.classList.add('done');
    btn.setAttribute('aria-label', 'Published');
    tune(W, ...T.morph); W.t = DOT;
    sc.t = 1; sc.v += 2.5;
    setSub('Published to the store');
    after(T.doneSec, reset);
  }
  function reset() {
    btn.classList.remove('done');
    btn.setAttribute('aria-label', 'Hold to publish');
    tune(W, T.morph[0] * .8, T.morph[1]); W.t = FULL;
    p.x = 0; p.v = 0; p.t = 0;
    mode = 'idle';
    setSub('Motion Kit v2 · 24 components');
  }

  btn.addEventListener('pointerdown', e => { start(); try { btn.setPointerCapture(e.pointerId); } catch {} });
  btn.addEventListener('pointerup', stop);
  btn.addEventListener('pointercancel', stop);
  btn.addEventListener('lostpointercapture', stop);
  btn.addEventListener('keydown', e => { if ((e.key === ' ' || e.key === 'Enter') && !e.repeat) { e.preventDefault(); start(); } });
  btn.addEventListener('keyup', e => { if (e.key === ' ' || e.key === 'Enter') stop(); });
  btn.addEventListener('contextmenu', e => e.preventDefault());

  Motion.register({
    frame(dt) {
      if (holding) {
        // linear while held: it is a progress bar, the spring takes over on release
        p.x = Math.min(1, p.x + dt / T.holdSec); p.v = 0; p.t = p.x;
        if (p.x >= 1) complete();
      } else step(p, dt);
      step(W, dt); step(sc, dt); step(shake, dt);
    },
    render() {
      const w = W.x;
      const fw = Math.max(0, p.x) * w;
      btn.style.width = w + 'px';
      btn.style.transform = `translateX(${shake.x}px) scale(${sc.x})`;
      fill.style.width = fw + 'px';
      // the black label only shows where the white fill is
      top.style.clipPath = `inset(0 ${Math.max(0, w - fw)}px 0 0)`;
    },
  });
})();
