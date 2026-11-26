// Loading > Success
// Click: the label drops away and the pill closes into a circle with a
// spinner. When the work is done the circle opens into a green pill, the check
// draws itself and "Inviato" rises in. A moment later it goes back to Submit.
(() => {
  const { S, step, clamp, after, cancel } = Motion;
  const $ = id => document.getElementById(id);
  const sub = $('sub'), txt = $('txt'), spin = $('spin'), done = $('done'), tick = $('tick');
  const W = S(168, 300, 22), T = S(1, 400, 30), P = S(0, 300, 28), D = S(0, 220, 20), G = S(0, 200, 30), K = S(0, 120, 20);
  const len = 22; tick.style.strokeDasharray = len;
  let state = 'idle', timers = [], rot = 0, clock = 0, userUntil = 0;
  const later = (s, f) => { const t = after(s, () => { timers = timers.filter(x => x !== t); f(); }); timers.push(t); };

  function go() {
    if (state !== 'idle') return;
    state = 'load';
    T.t = 0; W.t = 56; P.t = 1;
    later(1.6, () => { state = 'done'; P.t = 0; W.t = 140; G.t = 1; D.t = 1; later(.15, () => { K.t = 1; }); sub.setAttribute('aria-label', 'Inviato'); });
    later(3.6, () => { state = 'idle'; D.t = 0; G.t = 0; K.t = 0; W.t = 168; later(.12, () => { T.t = 1; }); sub.removeAttribute('aria-label'); });
  }
  sub.addEventListener('click', () => { userUntil = clock + 6; go(); });
  (function auto() { if (clock >= userUntil) go(); after(5.2, auto); })();

  Motion.register({
    frame(dt) { clock += dt; rot += dt * 360 * 1.4; [W, T, P, D, G, K].forEach(s => step(s, dt)); },
    render() {
      sub.style.width = W.x.toFixed(2) + 'px';
      const g = clamp(G.x, 0, 1);
      sub.style.background = g > .01 ? `color-mix(in srgb, var(--green) ${(g * 100).toFixed(1)}%, var(--inverse))` : '';
      txt.style.opacity = clamp(T.x, 0, 1).toFixed(3);
      txt.style.transform = `translateY(${((1 - T.x) * 14).toFixed(2)}px)`;
      spin.style.opacity = clamp(P.x, 0, 1).toFixed(3);
      spin.style.transform = `rotate(${rot % 360}deg) scale(${(.6 + .4 * clamp(P.x, 0, 1.2)).toFixed(3)})`;
      done.style.opacity = clamp(D.x, 0, 1).toFixed(3);
      done.style.transform = `translateY(${((1 - D.x) * 12).toFixed(2)}px)`;
      tick.style.strokeDashoffset = (len * (1 - clamp(K.x, 0, 1))).toFixed(2);
    },
  });
})();
