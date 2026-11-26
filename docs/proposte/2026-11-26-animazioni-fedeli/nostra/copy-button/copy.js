// Copy Button
// Click: the copy icon shrinks and turns away while a check pops in, the word
// rolls up from Copy to Copied and the button's width springs to fit it.
// After a moment it rolls back.
(() => {
  const { S, step, clamp, after, cancel } = Motion;
  const $ = id => document.getElementById(id);
  const cp = $('cp'), iCopy = $('iCopy'), iOk = $('iOk'), roll = $('roll'), w = $('w');
  const [a, b] = w.children;
  const R = S(0, 260, 20), I = S(0, 420, 18), RW = S(0, 300, 24);
  let on = false, timer = null, clock = 0, userUntil = 0;
  const widths = () => [a.offsetWidth, b.offsetWidth];

  function copy() {
    on = true; R.t = 1; I.t = 1; RW.t = widths()[1];
    if (timer) cancel(timer);
    timer = after(1.8, () => { on = false; R.t = 0; I.t = 0; RW.t = widths()[0]; });
  }
  cp.addEventListener('click', e => {
    userUntil = clock + 6; copy();
    if (!e.isTrusted) return;
    // only a real click touches the clipboard, never the autoplay
    try { navigator.clipboard.writeText('git push origin tab-emoji/22-10').catch(() => {}); } catch (e) {}
  });
  (function auto() { if (clock >= userUntil && !on) copy(); after(3.4, auto); })();

  Motion.register({
    frame(dt) {
      clock += dt;
      if (!RW.x) { RW.x = RW.t = widths()[0]; }
      [R, I, RW].forEach(s => step(s, dt));
    },
    render() {
      roll.style.width = RW.x.toFixed(2) + 'px';
      w.style.transform = `translateY(${(-18 * R.x).toFixed(2)}px)`;
      const i = clamp(I.x, -.2, 1.2);
      iCopy.style.opacity = clamp(1 - i * 1.6, 0, 1).toFixed(3);
      iCopy.style.transform = `scale(${(1 - .5 * clamp(i, 0, 1)).toFixed(3)}) rotate(${(-40 * clamp(i, 0, 1)).toFixed(1)}deg)`;
      iOk.style.opacity = clamp(i * 1.6 - .4, 0, 1).toFixed(3);
      iOk.style.transform = `scale(${(.4 + .6 * i).toFixed(3)})`;
    },
  });
})();
