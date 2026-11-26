// File Drop
// A file chip is carried in on a lazy spring and leans with its speed. When it
// comes near, the zone lifts its fill and the arrow starts to bob. On the drop
// the chip shrinks into the circle, which gulps, then a ring fills around a
// rolling percentage (it stalls in the middle like a real upload). At 100 the
// circle floods green, the check draws itself and the two text lines roll over.
// It also works for real: drop a file on it, click it, or press Enter. Nothing
// is sent anywhere, only the file's name and size are read.
(() => {
  const { S, tune, step, clamp, after } = Motion;
  const $ = id => document.getElementById(id);
  const zone = $('zone'), core = $('core'), g = $('g'), ring = $('ring'), arc = $('arc'), up = $('up'), pct = $('pct'), ok = $('ok'), tick = $('tick');
  const file = $('file'), fn = $('fn'), pick = $('pick');
  const CIRC = 2 * Math.PI * 33, DUR = 2.1, IDLE_T = 'Drop your file here', IDLE_S = 'PDF, PNG or MP4, up to 20 MB';
  arc.style.strokeDasharray = CIRC.toFixed(2);

  // two text lines that roll: the old one leaves upward, the new one rises in
  const line = id => { const el = $(id); return { el, cur: el.firstElementChild, old: null, o: S(1, 360, 30) }; };
  const T = line('t'), U = line('s');
  function say(l, text) {
    if (l.cur.textContent === text) return;
    if (l.old) l.old.remove();
    l.old = l.cur;
    l.cur = document.createElement('span');
    l.cur.textContent = text;
    l.el.appendChild(l.cur);
    l.o.x = 0; l.o.v = 0; l.o.t = 1;
  }

  const FX = S(0, 90, 15), FY = S(0, 90, 15), FS = S(1, 520, 34), FO = S(0, 500, 40);   // file chip
  const ZS = S(1, 380, 16), ZH = S(0, 300, 26), CS = S(1, 420, 14);                     // zone, hover, circle
  const UP = S(1, 420, 30), PO = S(0, 420, 30), RO = S(0, 300, 30), G = S(0, 260, 21), K = S(0, 200, 26);
  let phase = 'idle', p = 0, tUp = 0, total = 0, name = '', near = false, overAt = -9, restUntil = 1, clock = 0, userUntil = 0, run = 0;

  const fmt = b => b >= 1e6 ? (b / 1e6).toFixed(1) + ' MB' : Math.max(1, Math.round(b / 1e3)) + ' KB';
  const short = n => n.length > 20 ? n.slice(0, 10) + '…' + n.slice(-8) : n;
  // quick start, a stall around the middle, then it runs home
  const pace = x => x < .35 ? x * 1.4 : x < .6 ? .49 + (x - .35) * .4 : .59 + (x - .6) * 1.025;

  function begin(n, bytes) {
    name = short(n); total = bytes; phase = 'upload'; p = 0; tUp = 0; near = false;
    UP.t = 0; PO.t = 1; RO.t = 1; G.t = 0; K.t = 0;
    CS.x = 1.2; ZS.x = .965;
    say(T, 'Uploading ' + name); say(U, '0 KB of ' + fmt(total));
  }
  function done() {
    const me = run;
    phase = 'done'; p = 1;
    PO.t = 0; RO.t = 0; G.t = 1; K.t = 1; CS.x = .84;
    say(T, 'Upload complete'); say(U, name + ' · ' + fmt(total));
    after(2.6, () => { if (me === run) reset(); });
  }
  function reset() {
    phase = 'idle'; restUntil = clock + 1.1;
    G.t = 0; K.t = 0; UP.t = 1; PO.t = 0; RO.t = 0;
    say(T, IDLE_T); say(U, IDLE_S);
  }

  // the demo: carry a file in from the corner and drop it on the circle
  function ghost() {
    const me = ++run, live = () => me === run;
    const B = document.body, b = B.getBoundingClientRect(), c = core.getBoundingClientRect();
    const tx = c.left - b.left + c.width / 2, ty = c.top - b.top + c.height / 2;
    phase = 'carry';
    fn.textContent = 'brief.pdf';
    FX.x = B.clientWidth + 90; FY.x = ty + 190; FX.v = FY.v = 0;
    FS.x = FS.t = 1; FO.x = FO.t = 1;
    tune(FX, 90, 15); tune(FY, 90, 15);
    FX.t = tx + 78; FY.t = ty + 62;
    after(.7, () => { if (live()) near = true; });
    after(1.25, () => { if (live()) { tune(FX, 150, 18); tune(FY, 150, 18); FX.t = tx + 8; FY.t = ty + 6; } });
    after(2, () => {
      if (!live()) return;
      tune(FX, 520, 36); tune(FY, 520, 36); FX.t = tx; FY.t = ty; FS.t = 0;
      after(.16, () => { if (live()) FO.t = 0; });
      begin('brief.pdf', 2.4e6);
    });
  }
  function take(f) {
    if (!f || phase === 'upload') return;
    run++; FO.t = 0;
    begin(f.name, f.size);
  }

  zone.addEventListener('dragover', e => {
    e.preventDefault(); userUntil = clock + 8; overAt = clock;
    if (phase === 'carry') { run++; phase = 'idle'; near = false; FO.t = 0; }
  });
  zone.addEventListener('drop', e => { e.preventDefault(); userUntil = clock + 8; overAt = -9; take(e.dataTransfer && e.dataTransfer.files[0]); });
  zone.addEventListener('click', e => { if (e.isTrusted && e.target !== pick) { userUntil = clock + 8; pick.click(); } });
  zone.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); userUntil = clock + 8; pick.click(); } });
  pick.addEventListener('change', () => { userUntil = clock + 8; take(pick.files[0]); pick.value = ''; });
  zone.addEventListener('pointerdown', () => { ZS.x = .975; });

  (function auto() {
    if (phase === 'idle' && clock >= userUntil && clock >= restUntil) ghost();
    after(.5, auto);
  })();

  Motion.register({
    frame(dt) {
      clock += dt;
      if (phase === 'upload') {
        tUp += dt;
        p = clamp(pace(tUp / DUR), 0, 1);
        if (tUp >= DUR) done();
      }
      ZH.t = phase !== 'upload' && phase !== 'done' && (near || clock - overAt < .2) ? 1 : 0;
      [FX, FY, FS, FO, ZS, ZH, CS, UP, PO, RO, G, K, T.o, U.o].forEach(s => step(s, dt));
    },
    render() {
      const h = clamp(ZH.x, 0, 1);
      zone.style.setProperty('--h', h.toFixed(3));
      zone.style.transform = `translate(-50%,-50%) scale(${(ZS.x * (1 + .025 * ZH.x)).toFixed(4)})`;
      core.style.transform = `translateY(${(-5 * ZH.x).toFixed(2)}px) scale(${CS.x.toFixed(4)})`;
      const bob = -3.5 * h * (.5 + .5 * Math.sin(clock * 7));
      up.style.opacity = clamp(UP.x, 0, 1).toFixed(3);
      up.style.transform = `translateY(${(bob + (1 - UP.x) * -12).toFixed(2)}px) scale(${(.6 + .4 * UP.x).toFixed(3)})`;
      pct.style.opacity = clamp(PO.x, 0, 1).toFixed(3);
      pct.style.transform = `scale(${(.7 + .3 * PO.x).toFixed(3)})`;
      ring.style.opacity = clamp(RO.x, 0, 1).toFixed(3);
      arc.style.strokeDashoffset = (CIRC * (1 - p)).toFixed(2);
      g.style.transform = `scale(${Math.max(0, G.x).toFixed(4)})`;
      ok.style.opacity = clamp(K.x * 4, 0, 1).toFixed(3);
      tick.style.strokeDashoffset = (24 * (1 - clamp(K.x, 0, 1))).toFixed(2);
      if (phase === 'upload') {
        const n = String(Math.round(p * 100));
        if (pct.textContent !== n) pct.textContent = n;
        const s = fmt(p * total) + ' of ' + fmt(total);
        if (U.cur.textContent !== s) U.cur.textContent = s;
      }
      [T, U].forEach(l => {
        const o = clamp(l.o.x, 0, 1);
        l.cur.style.opacity = clamp((o - .35) / .65, 0, 1).toFixed(3);   // the new line waits for the old one to clear
        l.cur.style.transform = `translateY(${((1 - l.o.x) * 16).toFixed(2)}px)`;
        if (l.old) {
          if (o > .985) { l.old.remove(); l.old = null; }
          else { l.old.style.opacity = clamp(1 - o * 1.8, 0, 1).toFixed(3); l.old.style.transform = `translateY(${(-l.o.x * 16).toFixed(2)}px)`; }
        }
      });
      file.style.opacity = clamp(FO.x, 0, 1).toFixed(3);
      file.style.transform = `translate(${FX.x.toFixed(2)}px,${FY.x.toFixed(2)}px) translate(-50%,-50%) rotate(${clamp(FX.v * .04, -16, 16).toFixed(2)}deg) scale(${Math.max(0, FS.x).toFixed(4)})`;
    },
  });
})();
