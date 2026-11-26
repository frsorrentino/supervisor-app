// Command Palette
// The pill's width and height spring open into the panel. Every command row
// has a position spring and a visibility spring, so filtering slides the
// matches up into place while the rest fade and drop out. Arrow keys and
// Enter work; on its own it opens, types a query, steps down and runs one.
(() => {
  const { S, step, clamp, after } = Motion;
  const $ = id => document.getElementById(id);
  const pal = $('pal'), trig = $('trig'), box = $('box'), q = $('q'), res = $('res'), hl = $('hl');
  const CMDS = [["New project", "N"], ["Search files", "F"], ["Profile settings", "P"], ["Projects", "G"], ["Invite people", "I"], ["Dark mode", "D"]];
  const rows = CMDS.map(([name, key]) => {
    const li = document.createElement('li');
    const t = document.createElement('span'); t.textContent = name;
    const k = document.createElement('kbd'); k.textContent = key;
    li.appendChild(t); li.appendChild(k); res.appendChild(li);
    return { li, name, y: S(0, 300, 26), o: S(1, 300, 28), shown: true };
  });
  const W = S(170, 300, 26), H = S(44, 240, 24), O = S(0, 300, 28), HY = S(0, 420, 30);
  let open = false, sel = 0, clock = 0, userUntil = 0, timers = [];
  const later = (s, f) => { const t = after(s, () => { timers = timers.filter(x => x !== t); f(); }); timers.push(t); };
  const fullW = () => box.offsetWidth;

  function layout() {
    const v = q.value.trim().toLowerCase();
    let k = 0;
    rows.forEach(r => { r.shown = !v || r.name.toLowerCase().includes(v); if (r.shown) { r.y.t = k * 38; r.idx = k++; } r.o.t = r.shown ? 1 : 0; });
    sel = Math.min(sel, Math.max(0, k - 1));
    mark();
  }
  function vis() { return rows.filter(r => r.shown); }
  function mark() { const v = vis(); rows.forEach(r => r.li.classList.toggle('cur', v[sel] === r)); HY.t = sel * 38; }
  function setOpen(o) {
    open = o; trig.setAttribute('aria-expanded', o); box.classList.toggle('on', o);
    W.t = o ? fullW() : 170; H.t = o ? 52 + 6 * 38 + 6 : 44; O.t = o ? 1 : 0;
    if (!o) { q.value = ''; sel = 0; layout(); } else if (clock < userUntil) q.focus();
  }
  trig.addEventListener('click', () => { userUntil = clock + 10; timers.forEach(Motion.cancel); timers = []; setOpen(true); });
  q.addEventListener('input', () => { userUntil = clock + 10; sel = 0; layout(); });
  q.addEventListener('keydown', e => {
    userUntil = clock + 10;
    const n = vis().length;
    if (e.key === 'ArrowDown') { e.preventDefault(); sel = (sel + 1) % Math.max(n, 1); mark(); }
    if (e.key === 'ArrowUp') { e.preventDefault(); sel = (sel - 1 + n) % Math.max(n, 1); mark(); }
    if (e.key === 'Enter' || e.key === 'Escape') setOpen(false);
  });
  rows.forEach(r => r.li.addEventListener('click', () => { userUntil = clock + 4; setOpen(false); }));
  layout(); rows.forEach(r => { r.y.x = r.y.t; });

  function play() {
    if (clock < userUntil) { later(2, play); return; }
    setOpen(true);
    'pro'.split('').forEach((c, i) => later(.7 + i * .16, () => { q.value += c; sel = 0; layout(); }));
    later(1.9, () => { sel = 1; mark(); });
    later(2.5, () => { sel = 2; mark(); });
    later(3.3, () => setOpen(false));
    later(5, play);
  }
  later(.8, play);

  Motion.register({
    frame(dt) { clock += dt; [W, H, O, HY].forEach(s => step(s, dt)); rows.forEach(r => { step(r.y, dt); step(r.o, dt); }); },
    render() {
      pal.style.width = W.x.toFixed(2) + 'px';
      pal.style.height = H.x.toFixed(2) + 'px';
      pal.style.borderRadius = (22 - 2 * clamp(O.x, 0, 1)).toFixed(2) + 'px';
      const o = clamp(O.x, 0, 1);
      trig.style.opacity = clamp(1 - o * 2.5, 0, 1).toFixed(3);
      box.style.opacity = clamp(o * 2 - .6, 0, 1).toFixed(3);
      rows.forEach(r => {
        const v = clamp(r.o.x, 0, 1);
        r.li.style.opacity = v.toFixed(3);
        r.li.style.transform = `translateY(${(r.y.x + (1 - v) * 10).toFixed(2)}px) scale(${(.96 + .04 * v).toFixed(4)})`;
      });
      hl.style.opacity = vis().length ? 1 : 0;
      hl.style.transform = `translateY(${HY.x.toFixed(2)}px)`;
    },
  });
})();
