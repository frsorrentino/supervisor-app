// Mention Picker
// The line is a list of pieces: text, chips, and while you are mentioning,
// the query ("@no"). The menu sits under the query. Every row has a y spring
// (its slot in the filtered list) and an opacity/scale spring, so rows that
// stop matching fold away and the rest close up instead of jumping. Menu
// height and the highlight are springs too; the highlight has a lead edge
// and a trail edge so it stretches when it moves. Enter or a click turns the
// query into a chip that pops (scale .6 to 1 on an overshooting spring).
(() => {
  const { S, tune, step, clamp, spring, after } = Motion;
  const $ = id => document.getElementById(id);
  const card = $('card'), ed = $('ed'), menu = $('menu'), hl = $('hl'), send = $('send');
  const T = { pop: spring('--spring-pop', '520 16'), fade: spring('--spring-fade', '420 32'), soft: spring('--spring-soft', '260 24'), lead: spring('--spring-lead', '620 36'), trail: spring('--spring-trail', '190 19') };
  const PEOPLE = [['Nour Adel', 'nour', 3], ['Noah Kim', 'noah', 6], ['Omar Said', 'omar', 9], ['Maya Lin', 'maya', 1], ['Sami Karim', 'sami', 4]];
  const RH = 40;

  let parts = [], query = null, list = [], act = 0, clock = 0, userUntil = 0, open = false;
  const MH = S(0), MO = S(0), HA = S(4), HB = S(4), MX = S(0), MY = S(0);
  tune(MH, ...T.soft); tune(MO, ...T.fade); tune(HA, ...T.lead); tune(HB, ...T.trail); tune(MX, 900, 50); tune(MY, 900, 50);

  const rows = PEOPLE.map(([name, h, av], i) => {
    const b = document.createElement('button'); b.className = 'row'; b.setAttribute('role', 'option'); b.tabIndex = -1;
    b.innerHTML = `<span class="av" data-av="${av}"></span>${name} <em>@${h}</em>`; menu.appendChild(b);
    const r = { b, name, h, Y: S(4 + i * RH), O: S(0), on: false };
    tune(r.Y, ...T.soft); tune(r.O, ...T.fade);
    b.addEventListener('pointerdown', e => e.preventDefault());
    b.addEventListener('click', () => { userUntil = clock + 8; act = list.indexOf(r); pickNow(); ed.focus(); });
    return r;
  });

  function draw() {
    ed.innerHTML = ''; query_el = null; /* closing menu stays where it was */
    parts.forEach(p => {
      if (p.t === 'text') { const s = document.createElement('span'); s.textContent = p.s; ed.appendChild(s); }
      else { const c = document.createElement('span'); c.className = 'chip'; c.textContent = '@' + p.name.split(' ')[0]; ed.appendChild(c); p.el = c; }
    });
    if (query != null) { const q = document.createElement('span'); q.className = 'q'; q.textContent = '@' + query; ed.appendChild(q); query_el = q; }
    const k = document.createElement('span'); k.className = 'caret'; ed.appendChild(k);
    if (!parts.length && query == null) ed.innerHTML = '';
    send.classList.toggle('ready', parts.length > 0 || query != null);
  }
  let query_el = null, fresh = false, up = false;
  function filter() {
    const q = (query || '').toLowerCase();
    list = rows.filter(r => !q || r.name.toLowerCase().split(' ').some(w => w.startsWith(q)) || r.h.startsWith(q));
    rows.forEach(r => { const i = list.indexOf(r); r.on = i > -1; r.O.t = r.on ? 1 : 0; if (r.on) r.Y.t = 4 + i * RH; r.b.setAttribute('aria-selected', i === act); });
    act = clamp(act, 0, Math.max(0, list.length - 1));
    setOpen(query != null && list.length > 0);
    place();
  }
  function setOpen(v) {
    if (v && !open) { fresh = true; MO.x = 0; rows.forEach(r => { if (r.on) { r.Y.x = r.Y.t - 8; r.O.x = 0; } }); HA.x = HB.x = HA.t; }
    open = v; menu.classList.toggle('is-open', v); MO.t = v ? 1 : 0;
    MH.t = v ? list.length * RH + 8 : 0;
  }
  function place() {
    if (!query_el) return;
    const b = document.body.getBoundingClientRect(), r = query_el.getBoundingClientRect();
    const h = list.length * RH + 8, below = r.bottom - b.top + 8;
    MX.t = clamp(r.left - b.left - 10, 12, b.width - 224);
    /* card sits centred, so a long list opens above the @ when it would run off the stage */
    if (fresh) up = below + PEOPLE.length * RH + 8 > b.height - 8; /* side is picked once per open */
    MY.t = up ? Math.max(8, r.top - b.top - 8 - h) : below;
    if (fresh || MO.x < .05) { MX.x = MX.t; MY.x = MY.t; fresh = false; }
  }
  function hlTo(i) { const y = 4 + i * RH; HA.t = y; HB.t = y; }
  function last() { return parts[parts.length - 1]; }
  function addText(s) { const l = last(); if (l && l.t === 'text') l.s += s; else parts.push({ t: 'text', s }); }
  function type(ch) {
    if (query != null) {
      if (ch === ' ' && !list.length) { addText('@' + query + ' '); query = null; }
      else query += ch;
    } else if (ch === '@') { query = ''; act = 0; }
    else addText(ch);
    draw(); filter(); if (open) hlTo(act);
  }
  function back() {
    if (query != null) { if (query.length) query = query.slice(0, -1); else query = null; }
    else { const l = last(); if (!l) return; if (l.t === 'text') { l.s = l.s.slice(0, -1); if (!l.s) parts.pop(); } else parts.pop(); }
    draw(); filter();
  }
  function pickNow() {
    const r = list[act]; if (!r) return;
    const p = { t: 'chip', name: r.name, K: S(.6) }; tune(p.K, ...T.pop); p.K.t = 1;
    parts.push(p); query = null; addText(' '); draw(); filter();
  }
  function move(d) { if (!list.length) return; act = (act + d + list.length) % list.length; hlTo(act); rows.forEach(r => r.b.setAttribute('aria-selected', list.indexOf(r) === act)); }
  const arrow = send.querySelector('i');
  function fly() {
    if (!send.classList.contains('ready')) return;
    arrow.animate([{ transform: 'translateY(0)', opacity: 1, filter: 'blur(0)' }, { transform: 'translateY(-26px)', opacity: 0, filter: 'blur(2px)' }, { transform: 'translateY(22px)', opacity: 0, filter: 'blur(2px)', offset: .5 }, { transform: 'translateY(0)', opacity: 1, filter: 'blur(0)' }], { duration: 560, easing: 'cubic-bezier(.3,.7,.2,1)' });
    send.animate([{ scale: '1' }, { scale: '.94' }, { scale: '1' }], { duration: 260 });
  }
  function clear() { parts = []; query = null; draw(); filter(); }

  ed.addEventListener('keydown', e => {
    userUntil = clock + 10;
    if (open && (e.key === 'ArrowDown' || e.key === 'ArrowUp')) { e.preventDefault(); move(e.key === 'ArrowDown' ? 1 : -1); return; }
    if (open && (e.key === 'Enter' || e.key === 'Tab')) { e.preventDefault(); pickNow(); return; }
    if (e.key === 'Escape' && query != null) { addText('@' + query); query = null; draw(); filter(); return; }
    if (e.key === 'Backspace') { e.preventDefault(); back(); return; }
    if (e.key.length === 1 && !e.metaKey && !e.ctrlKey) { e.preventDefault(); type(e.key); }
  });
  ed.addEventListener('focus', () => { ed.classList.remove('is-quiet'); userUntil = clock + 10; });
  ed.addEventListener('blur', () => { if (clock < userUntil) ed.classList.add('is-quiet'); });
  ed.addEventListener('pointerdown', () => { userUntil = clock + 10; });
  send.addEventListener('click', () => { userUntil = clock + 8; fly(); clear(); });
  addEventListener('resize', place);

  // idle: write a comment with two mentions, send it, again
  const SCRIPT = ['Thanks ', '@', 'n', 'o', 300, '↓', 400, '↑', 300, '⏎', 'for the review, ', '@', 's', 600, '⏎', 'can you check copy?', 1800, 'send', 1200];
  let si = 0, ci = 0;
  function tick() {
    if (clock < userUntil || document.activeElement === ed) { after(1, tick); return; }
    const a = SCRIPT[si];
    if (typeof a === 'number') { si++; after(a / 1000, tick); return; }
    if (a === '⏎') { pickNow(); si++; after(.5, tick); return; }
    if (a === '↓' || a === '↑') { move(a === '↓' ? 1 : -1); si++; after(.2, tick); return; }
    if (a === 'send') { fly(); clear(); si = (si + 1) % SCRIPT.length; after(.2, () => { si = 0; tick(); }); return; }
    type(a[ci++]);
    if (ci >= a.length) { ci = 0; si++; }
    after(a === '@' ? .35 : .05 + Math.random() * .06, tick);
  }
  ed.classList.add('is-quiet'); draw(); filter(); after(.8, tick);

  Motion.register({
    frame(dt) {
      clock += dt;
      [MH, MO, HA, HB, MX, MY].forEach(s => step(s, dt));
      rows.forEach(r => { step(r.Y, dt); step(r.O, dt); });
      parts.forEach(p => p.K && step(p.K, dt));
    },
    render() {
      const o = clamp(MO.x, 0, 1);
      menu.style.height = Math.max(0, MH.x).toFixed(1) + 'px';
      menu.style.opacity = o.toFixed(3);
      menu.style.transform = `translate(${MX.x.toFixed(1)}px,${(MY.x + (1 - o) * -6).toFixed(1)}px)`;
      const top = Math.min(HA.x, HB.x);
      hl.style.transform = `translateY(${top.toFixed(1)}px)`; hl.style.height = (Math.abs(HA.x - HB.x) + RH).toFixed(1) + 'px';
      hl.style.opacity = list.length ? 1 : 0;
      rows.forEach(r => { const v = clamp(r.O.x, 0, 1); r.b.style.opacity = v.toFixed(3); r.b.style.transform = `translateY(${r.Y.x.toFixed(1)}px) scale(${(.92 + .08 * v).toFixed(3)})`; r.b.style.visibility = v < .01 ? 'hidden' : ''; });
      parts.forEach(p => { if (p.K && p.el) p.el.style.transform = `scale(${p.K.x.toFixed(3)})`; });
    },
  });
})();
