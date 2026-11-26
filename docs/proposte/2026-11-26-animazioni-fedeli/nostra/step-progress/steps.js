// Step Progress
// The green line's length is a spring toward the current step. Each step has
// a fill spring (green when done), a check that draws itself, and a pop
// when it becomes current. Back and Next drive it; it also walks itself.
(() => {
  const { S, step, clamp, after } = Motion;
  const $ = id => document.getElementById(id);
  const bar = $('bar'), dotsEl = $('dots'), namesEl = $('names'), next = $('next'), back = $('back');
  const NAMES = ['Test', 'CI', 'Pre-release', 'Prova'];
  const N = NAMES.length;
  const steps = NAMES.map((name, i) => {
    const d = document.createElement('span'); d.className = 'dot'; d.style.left = (i / (N - 1) * 100) + '%';
    const num = document.createElement('b'); num.textContent = i + 1; d.appendChild(num);
    d.insertAdjacentHTML('beforeend', '<svg viewBox="0 0 24 24"><path d="M5 12.5l4.5 4.5L19 7.5"/></svg>');
    const path = d.querySelector('path'); path.style.strokeDasharray = 22;
    dotsEl.appendChild(d);
    const nm = document.createElement('span'); nm.textContent = name; nm.style.left = (i / (N - 1) * 100) + '%'; namesEl.appendChild(nm);
    return { d, num, path, nm, f: S(0, 260, 24), c: S(0, 140, 20), p: S(1, 500, 14), on: S(0, 300, 26) };
  });
  const L = S(0, 160, 22);
  let cur = -1, clock = 0, userUntil = 0;

  function go(i) {
    i = clamp(i, 0, N - 1); if (i === cur) return; cur = i;
    L.t = i / (N - 1);
    steps.forEach((s, k) => {
      const done = k < i || (i === N - 1 && k === N - 1);
      s.f.t = done ? 1 : 0;
      s.on.t = k === i && !done ? 1 : 0;
      s.nm.classList.toggle('on', k <= i);
      if (done) after(.12, () => { s.c.t = 1; }); else s.c.t = 0;
    });
    steps[i].p.x = .7; steps[i].p.v = 6;
    next.textContent = i === N - 1 ? 'Da capo' : 'Avanti';
  }
  next.addEventListener('click', () => { userUntil = clock + 8; go(cur === N - 1 ? 0 : cur + 1); });
  back.addEventListener('click', () => { userUntil = clock + 8; go(cur - 1); });
  go(0);
  (function auto() { if (clock >= userUntil) go(cur === N - 1 ? 0 : cur + 1); after(cur === N - 1 ? 2.4 : 1.4, auto); })();

  Motion.register({
    frame(dt) { clock += dt; step(L, dt); steps.forEach(s => { step(s.f, dt); step(s.c, dt); step(s.p, dt); step(s.on, dt); }); },
    render() {
      bar.style.width = (clamp(L.x, 0, 1) * 100).toFixed(3) + '%';
      steps.forEach(s => {
        const f = clamp(s.f.x, 0, 1), o = clamp(s.on.x, 0, 1);
        s.d.style.background = f > .01 ? `color-mix(in srgb, var(--green) ${(f * 100).toFixed(1)}%, var(--surface))` : o > .01 ? `color-mix(in srgb, var(--inverse) ${(o * 100).toFixed(1)}%, var(--surface))` : '';
        s.d.style.color = o > .5 ? 'var(--on-inverse)' : '';
        s.num.style.opacity = (1 - f).toFixed(3);
        s.path.style.strokeDashoffset = (22 * (1 - clamp(s.c.x, 0, 1))).toFixed(2);
        s.d.style.transform = `scale(${s.p.x.toFixed(4)})`;
      });
    },
  });
})();
