// Letter Limit
// The box is a real textarea with a hard limit. Whatever would take it past
// the limit is cut off the end and thrown out as letters: each one starts at
// the end of the text, hops, falls with gravity and spin, bounces once and
// comes to rest on the pile under the card (the floor rises where letters
// already lie). The ring fills as you type, goes orange near the limit and
// red at it, and every refused letter shakes it. Delete and the pile shrinks,
// newest first, one letter per freed place. It writes by itself when idle.
(() => {
  const { S, tune, step, clamp, spring, after } = Motion;
  const $ = id => document.getElementById(id);
  const card = $('card'), ta = $('ta'), mirror = $('mirror'), fill = $('fill'), count = $('count'), post = $('post'), pileEl = $('pile');

  const LIMIT = 60, NEAR = 10, BIN = 10, MAXP = 46;
  const T = { pop: spring('--spring-pop', '520 16'), settle: spring('--spring-settle', '280 22') };
  const F = S(0, ...T.settle);           // ring fill
  const CP = S(1, ...T.pop);             // count pop
  const SH = S(0, 700, 12);              // the shake of its head on a refused letter
  const PP = S(1, ...T.pop);             // post press
  [[F, T.settle], [CP, T.pop], [SH, [700, 12]], [PP, T.pop]].forEach(([s, t]) => tune(s, ...t));

  const LINES = [
    'Le immagini 02 e 03 non arrivano: rifai la scheda con un invio per file e metti prima l'HTML.',
    'Il motivo: il tasto copia deve restare e la lettura per blocchi pure, sennò la rifaccio.',
  ];
  let letters = [], bins = {}, len = 0, clock = 0, userUntil = 0, floorY = 0, li = 0, gi = 0, gt = .6, phase = 'type', erase = 0, W = 0;

  function measure() {
    const b = document.body.getBoundingClientRect(), c = card.getBoundingClientRect();
    W = b.width; floorY = Math.min(c.bottom - b.top + 46, b.height - 14);
  }
  measure();
  addEventListener('resize', measure);

  function endOfText() {                 // where the next letter would have gone
    mirror.textContent = ta.value;
    const m = document.createElement('span'); m.textContent = '\u200b'; mirror.appendChild(m);   // a zero-width space marks the spot
    const b = document.body.getBoundingClientRect(), r = m.getBoundingClientRect();
    return [r.left - b.left, r.top - b.top + 12];
  }
  function drop(chars) {
    const [x, y] = endOfText();
    [...chars].forEach((ch, i) => {
      if (!ch.trim()) return;
      const el = document.createElement('span'); el.textContent = ch; pileEl.appendChild(el);
      letters.push({ el, x: x + 4 + i * 9, y, vx: -40 + Math.random() * 150, vy: -150 - Math.random() * 110, r: 0, vr: (Math.random() - .5) * 720, rest: false, out: 0, s: 1, bin: null, hits: 0 });
    });
    while (letters.filter(l => !l.out).length > MAXP) { const l = letters.find(l => !l.out); leave(l); }
    SH.v += 34; CP.x = 1.25;
  }
  function leave(l) {                    // a letter is cleared from the pile
    if (l.out) return;
    l.out = .001; l.vy = -230; l.rest = false;
    if (l.bin != null) bins[l.bin] = Math.max(0, (bins[l.bin] || 0) - 1);
  }
  function sync(grown) {
    let v = ta.value;
    if (v.length > LIMIT) {              // cut what does not fit, and throw it out
      const caret = ta.selectionStart, extra = v.slice(LIMIT);
      ta.value = v = v.slice(0, LIMIT);
      if (document.activeElement === ta) ta.setSelectionRange(Math.min(caret, LIMIT), Math.min(caret, LIMIT));
      drop(extra);
    }
    const was = len; len = v.length;
    if (len < was) {                     // room was made: the pile gives back as many
      const live = letters.filter(l => !l.out);
      for (let i = 0; i < Math.min(was - len, live.length); i++) leave(live[live.length - 1 - i]);
    }
    if (len !== was) CP.x = Math.max(CP.x, 1.12);
    const left = LIMIT - len;
    count.textContent = left;
    card.classList.toggle('is-near', left <= NEAR && left > 0);
    card.classList.toggle('is-full', left <= 0);
    F.t = len / LIMIT;
  }
  ta.addEventListener('input', () => { userUntil = clock + 12; sync(); });
  ['pointerdown', 'keydown', 'focus'].forEach(e => ta.addEventListener(e, () => { userUntil = clock + 12; }));
  post.addEventListener('click', () => { userUntil = clock + 12; PP.x = .9; ta.value = ''; sync(); });

  // idle: write a line that runs over, think, delete some, clear, next line
  function ghost(dt) {
    if (clock < userUntil || document.activeElement === ta) { phase = 'wait'; gt = 1.2; return; }
    if ((gt -= dt) > 0) return;
    const line = LINES[li];
    if (phase === 'wait') { ta.value = ''; sync(); gi = 0; phase = 'type'; gt = .5; }
    else if (phase === 'type') {
      if (gi < line.length) { ta.value += line[gi++]; sync(); gt = .035 + Math.random() * .05 + (line[gi - 1] === ' ' ? .03 : 0); }
      else { phase = 'erase'; erase = 22; gt = 1.3; }
    } else if (phase === 'erase') {
      if (erase-- > 0) { ta.value = ta.value.slice(0, -1); sync(); gt = .045; }
      else { phase = 'clear'; gt = 1; }
    } else { ta.value = ''; sync(); li = (li + 1) % LINES.length; gi = 0; phase = 'type'; gt = .9; }
  }

  Motion.register({
    frame(dt) {
      clock += dt; ghost(dt);
      [F, CP, SH, PP].forEach(s => step(s, dt));
      letters.forEach(l => {
        if (l.out) { l.out += dt; l.vy += 900 * dt; l.y += l.vy * dt; l.s = Math.max(0, 1 - l.out / .32); return; }
        if (l.rest) return;
        l.vy += 1900 * dt; l.x += l.vx * dt; l.y += l.vy * dt; l.r += l.vr * dt;
        if (l.x < 14) { l.x = 14; l.vx = Math.abs(l.vx) * .5; } else if (l.x > W - 14) { l.x = W - 14; l.vx = -Math.abs(l.vx) * .5; }
        const b = Math.round(l.x / BIN), ground = floorY - (bins[b] || 0) * 9;
        if (l.y >= ground && l.vy > 0) {
          l.y = ground;
          if (l.hits++ < 1 && l.vy > 260) { l.vy *= -.3; l.vx *= .6; l.vr *= .5; }
          else {                           // it stays: lie down on the nearest side, and raise the floor here
            l.rest = true; l.bin = b; bins[b] = Math.min(3, (bins[b] || 0) + 1);
            l.r = Math.round(l.r / 90) * 90 + (Math.random() - .5) * 34;
            l.el.classList.add('is-down');
          }
        }
      });
      letters = letters.filter(l => { if (l.out > .34) { l.el.remove(); return false; } return true; });
    },
    render() {
      fill.style.strokeDashoffset = (1 - clamp(F.x, 0, 1)).toFixed(4);
      const sh = SH.x * .5;
      count.style.transform = `translateX(${sh.toFixed(2)}px) scale(${CP.x.toFixed(4)})`;
      fill.parentNode.style.transform = `translateX(${sh.toFixed(2)}px) rotate(-90deg)`;
      post.style.transform = `scale(${PP.x.toFixed(4)})`;
      letters.forEach(l => { l.el.style.transform = `translate(${l.x.toFixed(1)}px,${l.y.toFixed(1)}px) rotate(${l.r.toFixed(1)}deg) scale(${l.s.toFixed(3)})`; });
    },
  });
  sync();
})();
