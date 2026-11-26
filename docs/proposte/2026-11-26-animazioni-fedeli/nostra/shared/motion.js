// Tiny spring engine shared by every component.
// Springs, sim-time timers (so slow-mo slows them too), one rAF loop, and token readers.
window.Motion = (() => {
  // theme: shared by every page through localStorage, live-updated by the gallery
  const setTheme = t => { document.documentElement.dataset.theme = t === 'light' ? 'light' : 'dark'; };
  try { setTheme(localStorage.getItem('ms-theme')); } catch { setTheme('dark'); }
  addEventListener('storage', e => { if (e.key === 'ms-theme') setTheme(e.newValue); });

  const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;
  const S = (x, k = 400, c = 30) => ({ x, v: 0, t: x, k, c });
  const tune = (s, k, c) => { s.k = k; s.c = reduce ? 2 * Math.sqrt(k) : c; };
  const step = (s, dt) => { const a = -s.k * (s.x - s.t) - s.c * s.v; s.v += a * dt; s.x += s.v * dt; };
  const clamp = (v, a, b) => Math.max(a, Math.min(b, v));

  // tokens from shared/tokens.css
  const token = (name, fallback = '') =>
    getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;
  const num = (name, fallback) => parseFloat(token(name, String(fallback)));
  const spring = (name, fallback) => token(name, fallback).split(/\s+/).map(Number);

  let timeScale = 1;
  const timers = [];
  const after = (sec, fn) => { const t = { t: sec, fn }; timers.push(t); return t; };
  const cancel = t => { const i = timers.indexOf(t); if (i > -1) timers.splice(i, 1); };

  // slow motion, synced with a parent page when embedded
  const slowListeners = [];
  function setSlow(v, fromParent = false) {
    timeScale = v ? .25 : 1;
    slowListeners.forEach(f => f(v));
    if (!fromParent && parent !== window) parent.postMessage({ type: 'motion:slow-changed', on: v }, '*');
  }
  addEventListener('message', e => {
    if (e.data && e.data.type === 'motion:slow') setSlow(!!e.data.on, true);
    if (e.data && e.data.type === 'motion:theme') setTheme(e.data.theme);
  });

  const comps = [];
  let last = performance.now();
  function loop(now) {
    const dt = Math.min((now - last) / 1000, 1 / 30) * timeScale; last = now;
    for (let i = timers.length - 1; i >= 0; i--) {
      if ((timers[i].t -= dt) <= 0) timers.splice(i, 1)[0].fn();
    }
    const sub = 4;
    for (let k = 0; k < sub; k++) comps.forEach(c => c.frame(dt / sub));
    comps.forEach(c => c.render());
    requestAnimationFrame(loop);
  }
  requestAnimationFrame(loop);

  return {
    S, tune, step, clamp, token, num, spring, after, cancel,
    register: c => comps.push(c),
    get timeScale() { return timeScale; },
    get slow() { return timeScale !== 1; },
    setSlow, onSlow: f => slowListeners.push(f),
    svg: (p, sw = 1.8) => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="${sw}" stroke-linecap="round" stroke-linejoin="round">${p}</svg>`,
  };
})();
