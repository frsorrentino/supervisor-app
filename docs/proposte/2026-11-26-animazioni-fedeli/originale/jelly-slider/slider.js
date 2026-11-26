// Jelly Slider
// The thumb follows the pointer through a spring and stretches with its speed.
// While dragging, a value drop rises out of the thumb; both sit in one goo layer,
// so the drop peels off like liquid and melts back in on release.
// Dragging past either end meets rubber resistance and snaps back.
// The thumb travels inside the track: its centre runs from R to width - R, so it
// sits flush with the track at 0 and 100 and the tick labels line up with it.
(() => {
  const { S, tune, step, clamp, spring, num, svg } = Motion;
  const $ = id => document.getElementById(id);
  const rail = $('rail'), fill = $('fill'), thumb = $('thumb'), drop = $('drop');
  // the mute button and the big value number were taken off on 10 Nov 2026 (Noah)
  const dropLabel = $('dropLabel'), hit = $('hit');

  // scale under the track: a mark every 5, major every 25; passed ones light up
  const marks = [];
  for (let v = 0; v <= 100; v += 5) {
    const m = document.createElement('i');
    m.style.setProperty('--p', v / 100);
    if (v % 25 === 0) m.className = 'major';
    $('marks').appendChild(m); marks.push(m);
  }
  const nums = [...document.querySelectorAll('.nums span')];
  let litValue = -1;
  const lightScale = v => {
    if (v === litValue) return; litValue = v;
    marks.forEach((m, i) => m.classList.toggle('on', i * 5 <= v && v > 0));
    nums.forEach((n, i) => n.classList.toggle('on', i * 25 <= v && v > 0));
  };

  const T = {
    follow: spring('--slider-follow', '900 50'), bubble: spring('--slider-bubble', '420 22'),
    release: spring('--slider-release', '300 14'), glide: spring('--slider-glide', '420 34'),
    blur: num('--slider-blur', 8),
  };
  $('gooBlur').setAttribute('stdDeviation', T.blur);


  // R thumb radius, D drop radius, CY track centre inside the rail, OX/OY goo layer offset
  // TR is the drawn thumb's half size: the goo filter eats about 2px off its edge, so it is drawn at 28 to read as 24
  const R = 12, TR = 14, D = 18, OX = 40, OY = 80, CY = 16, RISE = -38, REST_DROP = .5;
  // width is 0 while the page sits in a hidden iframe, so keep the value as a ratio
  let Wd = rail.clientWidth || 0, ratio = .4, value = 40, last = 40;
  const span = () => Math.max(Wd - 2 * R, 1);
  const toX = v => R + v / 100 * span();                    // value => thumb centre
  const toV = x => clamp((x - R) / span(), 0, 1) * 100;     // thumb centre => value

  const X = S(toX(40), ...T.follow);     // thumb center x
  const DX = S(toX(40), 260, 18);        // drop x lags behind => it leans
  const DY = S(CY, ...T.bubble);         // drop center y
  const DS = S(REST_DROP, ...T.bubble);  // drop scale
  const PS = S(1, 520, 16);              // thumb hover / press scale

  const rubber = (x, min, max, d = 18) =>
    x < min ? min - (1 - 1 / ((min - x) * .55 / d + 1)) * d :
    x > max ? max + (1 - 1 / ((x - max) * .55 / d + 1)) * d : x;
  const localX = e => e.clientX - rail.getBoundingClientRect().left;

  let dragging = false, hover = false, lifted = false;
  function sync() {
    const up = dragging || lifted;
    DY.t = up ? CY + RISE : CY;
    DS.t = up ? 1 : REST_DROP;
    PS.t = dragging ? 1.16 : hover ? 1.08 : 1;
  }
  // glide to a value without a jump: used by the keyboard
  function goTo(v) { tune(X, ...T.glide); X.t = toX(v); }

  rail.addEventListener('pointerdown', e => {
    dragging = true; try { rail.setPointerCapture(e.pointerId); } catch {}
    tune(X, ...T.follow);
    X.t = rubber(localX(e), R, Wd - R);
    sync(); hit.focus({ preventScroll: true });
  });
  rail.addEventListener('pointermove', e => { if (dragging) X.t = rubber(localX(e), R, Wd - R); });
  const end = () => {
    if (!dragging) return;
    dragging = false;
    tune(X, ...T.release); X.t = clamp(X.t, R, Wd - R);
    const v = Math.round(toV(X.t)); if (v > 0) last = v;
    sync();
  };
  rail.addEventListener('pointerup', end);
  rail.addEventListener('pointercancel', end);
  rail.addEventListener('pointerenter', e => { if (e.pointerType === 'mouse') { hover = true; sync(); } });
  rail.addEventListener('pointerleave', () => { hover = false; sync(); });

  // keyboard: the drop stays up while the thumb has keyboard focus, so the value is its focus state
  hit.addEventListener('keydown', e => {
    const d = { ArrowRight: 5, ArrowUp: 5, ArrowLeft: -5, ArrowDown: -5, PageUp: 10, PageDown: -10 }[e.key];
    const abs = { Home: 0, End: 100 }[e.key];
    if (d === undefined && abs === undefined) return;
    e.preventDefault();
    const next = abs !== undefined ? abs : clamp(Math.round(toV(X.t)) + d, 0, 100);
    if (next > 0) last = next;
    goTo(next);
    lifted = true; sync();
  });
  hit.addEventListener('focus', () => { if (hit.matches(':focus-visible')) { lifted = true; sync(); } });
  hit.addEventListener('blur', () => { lifted = false; sync(); });

  new ResizeObserver(() => {
    const w = rail.clientWidth; if (!w || w === Wd) return;
    Wd = w; X.x = X.t = DX.x = DX.t = toX(ratio * 100); X.v = DX.v = 0;
  }).observe(rail);

  Motion.register({
    frame(dt) {
      DX.t = X.x;
      if (Wd > 2 * R) ratio = toV(X.t) / 100;
      step(X, dt); step(DX, dt); step(DY, dt); step(DS, dt); step(PS, dt);
    },
    render() {
      const x = X.x;
      value = Math.round(toV(x));

      // thumb: stretch along the track with speed, area preserving
      const st = 1 + Math.min(Math.abs(X.v) / 2400, .4);
      thumb.style.transform = `translate(${x - TR + OX}px,${CY - TR + OY}px) scale(${st * PS.x},${PS.x / st})`;

      // drop: rises, leans the way it is being dragged from
      const tilt = clamp((DX.x - X.x) * .9, -28, 28);
      const dTr = `translate(${DX.x - D}px,${DY.x - D}px) rotate(${tilt}deg) scale(${DS.x})`;
      drop.style.transform = `translate(${OX}px,${OY}px) ` + dTr;
      dropLabel.style.transform = dTr;
      dropLabel.style.opacity = clamp((DS.x - .7) / .25, 0, 1);
      dropLabel.textContent = value;

      // the fill ends under the thumb centre, and is gone by the time the thumb sits at 0
      fill.style.width = clamp(x, 0, Math.max(Wd, 0)) * clamp((x - R) / R, 0, 1) + 'px';
      hit.style.left = x - R + 'px';
      hit.setAttribute('aria-valuenow', value);
      lightScale(value);
    },
  });
})();
