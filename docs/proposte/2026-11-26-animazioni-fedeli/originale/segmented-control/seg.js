// Segmented Control
// The highlight rides one spring for position; its speed squashes it (wider,
// a touch shorter) so it reads as soft. The caption swaps with a quick blur
// out and rise in.
(() => {
  const { S, step, clamp, after } = Motion;
  const $ = id => document.getElementById(id);
  const hl = $('hl'), cw = $('cw'), btns = [...document.querySelectorAll('#seg button')];
  const CAP = ['<b>6h 40m</b> focus today', '<b>38h</b> focus this week', '<b>152h</b> focus this month', '<b>1,740h</b> focus this year'];
  const X = S(0, 300, 20), C = S(1, 300, 26);
  let cur = -1, clock = 0, userUntil = 0, swap = null, first = true;

  function pick(i) {
    if (i === cur) return;
    cur = i;
    btns.forEach((b, k) => b.setAttribute('aria-selected', k === i));
    if (first) { cw.innerHTML = CAP[i]; first = false; return; }
    C.t = 0;
    swap = after(.14, () => { cw.innerHTML = CAP[cur]; C.x = -.4; C.t = 1; });
  }
  btns.forEach((b, i) => b.addEventListener('click', () => { userUntil = clock + 7; pick(i); }));
  pick(1);
  const ORDER = [2, 3, 0, 1];
  let n = 0;
  (function auto() { if (clock >= userUntil) pick(ORDER[n++ % ORDER.length]); after(1.8, auto); })();

  let init = true;
  Motion.register({
    frame(dt) {
      clock += dt;
      X.t = btns[cur].offsetLeft;
      if (init) { X.x = X.t; init = false; }
      step(X, dt); step(C, dt);
    },
    render() {
      const w = btns[cur].offsetWidth;
      const sp = clamp(Math.abs(X.v) / 1400, 0, .22);
      hl.style.width = w + 'px';
      hl.style.transform = `translateX(${X.x.toFixed(2)}px) scale(${(1 + sp).toFixed(4)},${(1 - sp * .5).toFixed(4)})`;
      const c = C.x;
      cw.style.opacity = clamp(c, 0, 1).toFixed(3);
      cw.style.transform = `translateY(${(c < 0 ? -c * 10 : (1 - clamp(c, 0, 1)) * -6).toFixed(2)}px)`;
      cw.style.filter = `blur(${((1 - clamp(c, 0, 1)) * 4).toFixed(2)}px)`;
    },
  });
})();
