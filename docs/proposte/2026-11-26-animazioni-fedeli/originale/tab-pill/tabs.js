// Tab Pill
// Two springs per pill, one per edge. The edge in the direction of travel is
// stiff and the other is soft, so the pill stretches toward the new tab and
// then pulls its tail in.
(() => {
  const { S, step, tune, after } = Motion;
  const $ = id => document.getElementById(id);

  // narrow cards: shrink the whole bar to fit instead of cutting it off
  const fitBox = el => { const room = el.parentElement.clientWidth - 24, w = el.offsetWidth; el.style.transform = `translate(-50%,-50%) scale(${Math.min(1, room / w).toFixed(4)})`; };
  const pill = $('pill'), tabs = [...document.querySelectorAll('#tabs button')];
  const A = S(0), B = S(0);
  let cur = -1, clock = 0, userUntil = 0;

  function pick(i, snap) {
    const dir = i > cur ? 1 : -1;
    cur = i;
    tabs.forEach((t, k) => t.setAttribute('aria-selected', k === i));
    const el = tabs[i];
    A.t = el.offsetLeft; B.t = el.offsetLeft + el.offsetWidth;
    if (dir > 0) { tune(B, 620, 40); tune(A, 170, 19); } else { tune(A, 620, 40); tune(B, 170, 19); }
    if (snap) { A.x = A.t; B.x = B.t; }
  }
  tabs.forEach((t, i) => t.addEventListener('click', () => { userUntil = clock + 7; pick(i); }));
  pick(0, true);
  const ORDER = [1, 3, 2, 0];
  let n = 0;
  (function auto() { if (clock >= userUntil) pick(ORDER[n++ % ORDER.length]); after(1.6, auto); })();

  Motion.register({
    frame(dt) {
      fitBox($('tabs')); clock += dt; step(A, dt); step(B, dt); },
    render() {
      pill.style.transform = `translateX(${A.x.toFixed(2)}px)`;
      pill.style.width = Math.max(0, B.x - A.x).toFixed(2) + 'px';
    },
  });
})();
