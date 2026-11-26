// Pagination Dots
// The strip slides on one spring. The active pill has two edge springs:
// whichever edge faces the way it is going is stiff, the other soft, so the
// pill stretches across the gap and then gathers up on the new dot.
(() => {
  const { S, step, tune, after } = Motion;
  const $ = id => document.getElementById(id);
  const strip = $('strip'), dots = $('dots'), act = $('act');
  const n = strip.children.length;
  const btns = [...Array(n)].map((_, i) => { const b = document.createElement('button'); b.setAttribute('aria-label', 'Slide ' + (i + 1)); dots.appendChild(b); return b; });
  const P = S(0, 170, 22), A = S(0), B = S(0);
  let cur = 0, clock = 0, userUntil = 0, ready = false;

  function go(i) {
    const dir = i > cur ? 1 : -1; cur = i;
    btns.forEach((b, k) => b.setAttribute('aria-selected', k === i));
    P.t = i;
    // pill 20 wide over dots 22 apart (8 + 14 gap): 8px of air to each neighbour
    const b = btns[i], w = 20;
    const c = b.offsetLeft + b.offsetWidth / 2;
    A.t = c - w / 2; B.t = c + w / 2;
    if (dir > 0) { tune(B, 520, 34); tune(A, 140, 18); } else { tune(A, 520, 34); tune(B, 140, 18); }
  }
  btns.forEach((b, i) => b.addEventListener('click', () => { userUntil = clock + 7; go(i); }));
  (function auto() { if (ready && clock >= userUntil) go((cur + 1) % n); after(1.8, auto); })();

  Motion.register({
    frame(dt) {
      clock += dt;
      if (!ready && btns[0].offsetWidth) { go(0); A.x = A.t; B.x = B.t; ready = true; }
      [P, A, B].forEach(s => step(s, dt));
    },
    render() {
      strip.style.transform = `translateX(${(-P.x * 100).toFixed(3)}%)`;
      act.style.transform = `translateX(${A.x.toFixed(2)}px)`;
      act.style.width = Math.max(8, B.x - A.x).toFixed(2) + 'px';
    },
  });
})();
