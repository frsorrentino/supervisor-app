// OTP Code
// A hidden input takes the keystrokes; six boxes show them. A new digit drops
// in with a bounce and the highlighted box moves on. When all six are in,
// the gaps close, the boxes run into one pill that turns green and says
// Verified, then it resets. It types a code by itself until you click in.
(() => {
  const { S, step, clamp, after } = Motion;
  const $ = id => document.getElementById(id);
  const boxes = $('boxes'), inp = $('inp');
  const N = 6;
  const bx = [...Array(N)].map(() => {
    const el = document.createElement('div'); el.className = 'bx';
    const d = document.createElement('i'); el.appendChild(d);
    const car = document.createElement('span'); car.className = 'car'; el.appendChild(car);
    boxes.insertBefore(el, inp);
    return { el, d, car, y: S(0, 520, 16), o: S(0, 400, 28), a: S(0, 300, 26) };
  });
  const ok = document.createElement('div'); ok.className = 'ok';
  ok.innerHTML = '<svg viewBox="0 0 24 24"><path d="M5 12.5l4.5 4.5L19 7.5"/></svg><span>Verified</span>';
  boxes.appendChild(ok);
  const M = S(0, 260, 24), G = S(0, 220, 28);
  let val = '', clock = 0, userUntil = 0, done = false, timers = [];
  const later = (s, f) => { const t = after(s, () => { timers = timers.filter(x => x !== t); f(); }); timers.push(t); };

  function set(v) {
    v = v.replace(/\D/g, '').slice(0, N);
    for (let i = 0; i < N; i++) {
      const c = v[i] || '';
      if (c && bx[i].d.textContent !== c) { bx[i].d.textContent = c; bx[i].y.x = -14; bx[i].y.v = 0; bx[i].o.x = 0; bx[i].o.t = 1; }
      if (!c) { bx[i].d.textContent = ''; bx[i].o.t = 0; }
    }
    val = v; inp.value = v;
    if (v.length === N && !done) { done = true; later(.25, () => { M.t = 1; }); later(.55, () => { G.t = 1; }); later(2.4, reset); }
  }
  function reset() { done = false; G.t = 0; M.t = 0; later(.35, () => set('')); }
  inp.addEventListener('input', () => { userUntil = clock + 10; timers.forEach(Motion.cancel); timers = []; if (done) { done = false; M.t = 0; G.t = 0; } set(inp.value); });
  inp.addEventListener('focus', () => { userUntil = clock + 10; });

  function play() {
    if (clock < userUntil || done) { later(1.5, play); return; }
    '482916'.split('').forEach((c, i) => later(.35 + i * .22, () => set(val + c)));
    later(6, play);
  }
  later(.6, play);

  Motion.register({
    frame(dt) {
      clock += dt;
      bx.forEach((b, i) => { b.a.t = !done && i === Math.min(val.length, N - 1) ? 1 : 0; step(b.y, dt); step(b.o, dt); step(b.a, dt); });
      step(M, dt); step(G, dt);
    },
    render() {
      const W = boxes.parentElement.offsetWidth, gap0 = 8, bw = Math.min(48, (W - gap0 * (N - 1)) / N);
      const m = clamp(M.x, 0, 1.1), gap = gap0 * (1 - clamp(m, 0, 1));
      const total = bw * N + gap * (N - 1);
      const left0 = (W - total) / 2;
      bx.forEach((b, i) => {
        b.el.style.left = (left0 + i * (bw + gap)).toFixed(2) + 'px';
        b.el.style.width = (bw + (i < N - 1 ? gap0 * clamp(m, 0, 1) + 1 : 0)).toFixed(2) + 'px';
        // inner corners square off as they join; outer corners stay round
        const r = 14 * (1 - clamp(m, 0, 1));
        b.el.style.borderRadius = i === 0 ? `14px ${r}px ${r}px 14px` : i === N - 1 ? `${r}px 14px 14px ${r}px` : `${r}px`;
        const a = clamp(b.a.x, 0, 1), g = clamp(G.x, 0, 1);
        b.el.style.background = g > .01 ? `color-mix(in srgb, var(--green) ${(g * 100).toFixed(1)}%, var(--surface))` : a > .01 ? `color-mix(in srgb, var(--fill-3) ${(a * 100).toFixed(1)}%, var(--surface))` : '';
        b.d.style.opacity = (clamp(b.o.x, 0, 1) * (1 - g)).toFixed(3);
        b.d.style.transform = `translateY(${b.y.x.toFixed(2)}px)`;
        b.car.style.opacity = a > .5 && !val[i] && (clock * 1.8) % 2 < 1.2 ? 1 : 0;
      });
      boxes.style.width = W + 'px';
      ok.style.opacity = clamp(G.x * 1.6 - .5, 0, 1).toFixed(3);
      ok.style.transform = `translateY(${((1 - clamp(G.x, 0, 1)) * 8).toFixed(2)}px)`;
    },
  });
})();
