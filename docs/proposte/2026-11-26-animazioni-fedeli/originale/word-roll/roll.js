// Word Roll
// The last word rolls up and out letter by letter while the next one rolls in
// from below, and the space it sits in stretches to the new width on a spring.
// Turns by itself; click (or Enter) to turn now.
(() => {
  const { S, step, clamp, after, cancel } = Motion;
  const $ = id => document.getElementById(id);
  const roll = $('roll'), slot = $('slot');
  const WORDS = ['designers', 'founders', 'makers', 'teams'];
  const width = S(0, 320, 30);
  let live = [], i = 0, timer = null;

  function make(word) {
    const el = document.createElement('span');
    el.className = 'word';
    const letters = [...word].map(ch => {
      const l = document.createElement('span');
      l.className = 'ch';
      l.textContent = ch;
      el.appendChild(l);
      return { el: l, s: S(1, 420, 26) };   // 1 = below the slot, 0 = in place, -1 = above
    });
    slot.appendChild(el);
    return { el, letters, out: false };
  }

  function show(word, first) {
    live.forEach(w => {
      if (w.out) return;
      w.out = true;
      w.letters.forEach((l, k) => after(k * .022, () => { l.s.t = -1; }));
      after(1, () => { w.el.remove(); live = live.filter(x => x !== w); });
    });
    const w = make(word);
    w.letters.forEach((l, k) => {
      if (first) l.s.x = l.s.t = 0;
      else after(.06 + k * .028, () => { l.s.t = 0; });
    });
    live.push(w);
    width.t = w.el.offsetWidth;
    if (first) width.x = width.t;
    roll.setAttribute('aria-label', `Built for ${word}`);
  }

  function schedule() { if (timer) cancel(timer); timer = after(2.4, next); }
  function next() { i = (i + 1) % WORDS.length; show(WORDS[i]); schedule(); }

  roll.addEventListener('click', next);
  roll.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); next(); } });
  show(WORDS[0], true);
  schedule();
  // The first width was measured in the fallback font.
  if (document.fonts) document.fonts.ready.then(() => { const w = live[live.length - 1]; width.t = w.el.offsetWidth; });

  Motion.register({
    frame(dt) {
      step(width, dt);
      live.forEach(w => w.letters.forEach(l => step(l.s, dt)));
    },
    render() {
      slot.style.width = width.x + 'px';
      live.forEach(w => w.letters.forEach(l => {
        const y = l.s.x;
        l.el.style.transform = `translateY(${y * 105}%)`;
        l.el.style.opacity = clamp(1 - Math.abs(y) * .9, 0, 1);
      }));
    },
  });
})();
