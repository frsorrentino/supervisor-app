// Range Slider
// Each thumb follows its value on a spring, and its bubble tilts and
// stretches with the thumb's speed. Closer than a bubble's width, a merge
// spring fades the two bubbles into one centred bubble with the full range.
// Drag either thumb; on its own it glides between a few ranges.
(() => {
  const { S, step, clamp, after } = Motion;
  const $ = id => document.getElementById(id);
  const area = $('area'), fill = $('fill'), A = $('a'), B = $('b'), ta = $('ta'), tb = $('tb'), tm = $('tm');
  const MAX = 1000;
  // thumbs never get closer than 34px, whatever the track width
  const gapV = () => 34 / Math.max(1, area.clientWidth) * MAX;
  const va = S(240, 260, 24), vb = S(640, 260, 24), M = S(0, 300, 26);
  let clock = 0, userUntil = 0, drag = null;
  const W = () => area.clientWidth;
  const money = v => '$' + Math.round(v / 10) * 10;

  function bind(el, s, isA) {
    el.addEventListener('pointerdown', e => { drag = { s, isA }; el.setPointerCapture(e.pointerId); userUntil = clock + 1e9; });
    el.addEventListener('pointermove', e => {
      if (!drag || drag.s !== s) return;
      const r = area.getBoundingClientRect();
      let v = clamp((e.clientX - r.left) / r.width * MAX, 0, MAX);
      v = isA ? Math.min(v, vb.t - gapV()) : Math.max(v, va.t + gapV());
      s.t = v;
    });
    const end = () => { if (drag && drag.s === s) { drag = null; userUntil = clock + 5; } };
    el.addEventListener('pointerup', end); el.addEventListener('pointercancel', end);
    el.addEventListener('keydown', e => {
      const d = e.key === 'ArrowRight' ? 20 : e.key === 'ArrowLeft' ? -20 : 0; if (!d) return;
      e.preventDefault(); userUntil = clock + 6;
      s.t = isA ? clamp(s.t + d, 0, vb.t - gapV()) : clamp(s.t + d, va.t + gapV(), MAX);
    });
  }
  bind(A, va, true); bind(B, vb, false);
  const PLAN = [[180, 820], [380, 520], [420, 540], [120, 460], [240, 640]];
  let n = 0;
  (function auto() { if (clock >= userUntil) { const [a, b] = PLAN[n++ % PLAN.length]; const g = gapV(), m = (a + b) / 2; va.t = Math.min(a, m - g / 2); vb.t = Math.max(b, m + g / 2); } after(1.6, auto); })();

  Motion.register({
    frame(dt) { clock += dt; [va, vb, M].forEach(s => step(s, dt)); },
    render() {
      const w = W(), xa = va.x / MAX * w, xb = vb.x / MAX * w;
      A.style.transform = `translateX(${xa.toFixed(2)}px)`;
      B.style.transform = `translateX(${xb.toFixed(2)}px)`;
      A.setAttribute('aria-valuenow', Math.round(va.x)); B.setAttribute('aria-valuenow', Math.round(vb.x));
      fill.style.left = xa.toFixed(2) + 'px'; fill.style.width = Math.max(0, xb - xa).toFixed(2) + 'px';
      ta.textContent = money(va.x); tb.textContent = money(vb.x); tm.textContent = money(va.x) + ' – ' + money(vb.x);
      const need = (ta.offsetWidth + tb.offsetWidth) / 2 + 8;
      M.t = xb - xa < need ? 1 : 0;
      const m = clamp(M.x, 0, 1);
      const tipAt = (el, x, vel, o, sc) => {
        const tilt = clamp(vel / MAX * w * -.02, -12, 12);
        el.style.opacity = o.toFixed(3);
        el.style.transform = `translateX(${(x - el.offsetWidth / 2).toFixed(2)}px) rotate(${tilt.toFixed(2)}deg) scale(${sc.toFixed(3)})`;
      };
      // separate bubbles slide toward the middle as they fade into the merged one
      const mid = (xa + xb) / 2;
      tipAt(ta, xa + (mid - xa) * m, va.v, 1 - m, 1 - .2 * m);
      tipAt(tb, xb + (mid - xb) * m, vb.v, 1 - m, 1 - .2 * m);
      tipAt(tm, mid, (va.v + vb.v) / 2, m, .8 + .2 * m);
    },
  });
})();
