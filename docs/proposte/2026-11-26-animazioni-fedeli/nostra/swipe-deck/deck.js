// Swipe Deck
// The top card follows the pointer and tilts around the point you grabbed it by.
// Cards behind it rise as you drag (their depth eases by your progress).
// Throw it far or fast enough and it flies off, then recycles to the back.
(() => {
  const { S, tune, step, clamp, spring, num, after } = Motion;
  const $ = id => document.getElementById(id);
  const deck = $('deck');

  const T = { drag: spring('--deck-drag', '900 50'), back: spring('--deck-return', '280 16'), th: num('--deck-threshold', 110) };

  const ITEMS = [
    { name: 'Nova', price: '$79', meta: 'SaaS template · 24 pages', tags: ['Framer', 'Next.js'], look: ['accent', '', 'green'] },
    { name: 'Kalypso', price: '$49', meta: 'Portfolio kit · 12 pages', tags: ['Figma', 'Framer'], look: ['', 'orange', ''] },
    { name: 'Cobalt', price: '$69', meta: 'Fashion store · 18 pages', tags: ['Next.js'], look: ['', '', 'accent'] },
    { name: 'Shotreel', price: '$59', meta: 'Video studio · 9 pages', tags: ['Framer'], look: ['green', '', ''] },
    { name: 'Hale', price: '$39', meta: 'Wellness clinic · 7 pages', tags: ['Figma', 'Astro'], look: ['', 'accent', 'orange'] },
  ];

  const cards = ITEMS.map((it, i) => {
    const el = document.createElement('article');
    el.className = 'card';
    el.setAttribute('aria-label', `${it.name}, ${it.meta}, ${it.price}`);
    el.innerHTML = `
      <div class="preview cover cover-${i % 5 + 1}"></div>
      <div class="c-body">
        <div class="c-title"><b>${it.name}</b><span>${it.price}</span></div>
        <div class="c-meta">${it.meta}</div>
        <div class="c-tags">${it.tags.map(t => `<span>${t}</span>`).join('')}</div>
      </div>
      <span class="stamp save">Save</span><span class="stamp skip">Skip</span>`;
    deck.appendChild(el);
    return { el, save: el.querySelector('.stamp.save'), skip: el.querySelector('.stamp.skip'),
      x: S(0, ...T.drag), y: S(0, ...T.drag), d: S(i, 260, 22), grabSign: 1, flying: false };
  });
  let order = [...cards];

  // ---------- drag ----------
  let drag = null;
  function onDown(e) {
    const c = order[0];
    if (e.currentTarget !== c.el || c.flying) return;
    const r = c.el.getBoundingClientRect();
    drag = { c, id: e.pointerId, x0: e.clientX - c.x.x, y0: e.clientY - c.y.x, lx: e.clientX, ly: e.clientY, lt: performance.now(), vx: 0, vy: 0 };
    c.grabSign = e.clientY < r.top + r.height / 2 ? 1 : -1;   // grabbed by the top: tilts one way, by the bottom: the other
    tune(c.x, ...T.drag); tune(c.y, ...T.drag);
    c.el.classList.add('dragging');
    try { c.el.setPointerCapture(e.pointerId); } catch {}
  }
  function onMove(e) {
    if (!drag || e.pointerId !== drag.id) return;
    const now = performance.now(), dt = Math.max(1, now - drag.lt) / 1000;
    drag.vx = drag.vx * .5 + ((e.clientX - drag.lx) / dt) * .5;
    drag.vy = drag.vy * .5 + ((e.clientY - drag.ly) / dt) * .5;
    drag.lx = e.clientX; drag.ly = e.clientY; drag.lt = now;
    drag.c.x.t = e.clientX - drag.x0;
    drag.c.y.t = (e.clientY - drag.y0) * .6;
  }
  function onUp(e) {
    if (!drag || e.pointerId !== drag.id) return;
    const { c, vx, vy } = drag; drag = null;
    c.el.classList.remove('dragging');
    const x = c.x.t;
    const dir = Math.abs(x) > T.th ? Math.sign(x) : Math.abs(vx) > 700 && Math.sign(vx) === Math.sign(x || vx) ? Math.sign(vx) : 0;
    if (dir) fling(dir, vx, vy);
    else { tune(c.x, ...T.back); tune(c.y, ...T.back); c.x.t = 0; c.y.t = 0; }
  }
  cards.forEach(c => {
    c.el.addEventListener('pointerdown', onDown);
    c.el.addEventListener('pointermove', onMove);
    c.el.addEventListener('pointerup', onUp);
    c.el.addEventListener('pointercancel', onUp);
  });

  function fling(dir, vx = 0, vy = 0) {
    const c = order[0];
    if (!c || c.flying) return;
    // The cards behind had risen with the drag (render subtracts p from their
    // depth). Bake that rise into their springs now, or the moment the top card
    // leaves they drop back a level and rise again: the glitch Noah saw (9 Nov 2026).
    const p = clamp(Math.abs(c.x.x) / (T.th * 1.6), 0, 1);
    order.forEach(o => { if (o !== c) o.d.x = Math.max(0, o.d.x - p); });
    c.flying = true;
    // Damped enough that the thrown card keeps going instead of swinging back.
    tune(c.x, 70, 17); tune(c.y, 70, 17);
    c.x.v = dir * Math.max(Math.abs(vx), 1500);
    c.x.t = dir * Math.max(innerWidth / 2 + 360, Math.abs(c.x.x) + 420);   // always ahead of it, never behind
    c.y.t = c.y.x + clamp(vy * .2, -120, 120);
    // next card becomes the top right away; the thrown one recycles to the back
    order = [...order.slice(1), c];
    after(.42, () => {
      c.flying = false;
      tune(c.x, ...T.drag); tune(c.y, ...T.drag);
      c.x.x = c.x.t = c.x.v = 0; c.y.x = c.y.t = c.y.v = 0;
      c.d.x = order.length + .5;          // appear from deeper than the last card
    });
  }

  addEventListener('keydown', e => { if (e.key === 'ArrowLeft') fling(-1); if (e.key === 'ArrowRight') fling(1); });

  Motion.register({
    frame(dt) {
      order.forEach((c, i) => { c.d.t = i; });
      cards.forEach(c => { step(c.x, dt); step(c.y, dt); step(c.d, dt); });
    },
    render() {
      const top = order[0];
      const p = top && !top.flying ? clamp(Math.abs(top.x.x) / (T.th * 1.6), 0, 1) : 0;
      cards.forEach(c => {
        const isTop = c === top;
        c.el.classList.toggle('is-top', isTop);
        const depth = Math.max(0, c.d.x - (isTop || c.flying ? 0 : p));
        const s = 1 - Math.min(depth, 3) * .055;
        const y = Math.min(depth, 3) * 16;
        const rot = c.x.x * .065 * c.grabSign;
        c.el.style.transform = `translate(${c.x.x}px,${c.y.x + y}px) rotate(${rot}deg) scale(${s})`;
        c.el.style.opacity = clamp(3.6 - depth, 0, 1);
        // No shadow, so each card behind is a step darker; that is what separates them.
        c.el.style.filter = depth > .01 ? `brightness(${1 - Math.min(depth, 3) * .22})` : '';
        c.el.style.zIndex = c.flying ? 50 : 40 - Math.round(c.d.t);
        c.save.style.opacity = isTop || c.flying ? clamp(c.x.x / T.th, 0, 1) : 0;
        c.skip.style.opacity = isTop || c.flying ? clamp(-c.x.x / T.th, 0, 1) : 0;
      });
    },
  });
})();
