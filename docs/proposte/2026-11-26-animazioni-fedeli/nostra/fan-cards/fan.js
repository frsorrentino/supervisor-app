// Fan Cards
// One fan spring spreads the stack (offset + rotation around the bottom edge);
// each card has its own lift and scale spring for hover. Fans and browses by
// itself until the pointer comes in.
(() => {
  const { S, step, after } = Motion;
  const fan = document.getElementById('fan');
  const cards = [...document.querySelectorAll('.c')].map((el, i) => ({ el, i, lift: S(0, 420, 20), sc: S(1, 420, 22) }));
  const f = S(0, 220, 18);
  let hot = -1, clock = 0, userUntil = 0, inside = false;

  function setHot(i) {
    hot = i;
    cards.forEach(c => {
      c.lift.t = c.i === hot ? -22 : 0;
      c.sc.t = c.i === hot ? 1.06 : 1;
      c.el.style.zIndex = c.i === hot ? 5 : c.i === 1 ? 3 : 2;
    });
  }
  setHot(-1);

  fan.addEventListener('pointerenter', () => { inside = true; userUntil = Infinity; f.t = 1; });
  fan.addEventListener('pointerleave', () => { inside = false; userUntil = clock + 2; f.t = 0; setHot(-1); });
  cards.forEach(c => {
    c.el.addEventListener('pointerenter', () => { if (inside) setHot(c.i); });
    c.el.addEventListener('click', () => { c.lift.v = -260; });
  });

  const SEQ = [() => { f.t = 1; }, () => setHot(0), () => setHot(1), () => setHot(2), () => setHot(-1), () => { f.t = 0; }];
  let n = 0;
  (function auto() {
    if (clock >= userUntil) SEQ[n++ % SEQ.length]();
    after(n % SEQ.length === 0 ? 1.4 : .75, auto);
  })();

  Motion.register({
    frame(dt) {
      clock += dt;
      step(f, dt);
      cards.forEach(c => { step(c.lift, dt); step(c.sc, dt); });
    },
    render() {
      // the open fan is about 380px wide; shrink the whole hand in narrow cards
      const room = Math.min(innerWidth, document.body.offsetWidth || innerWidth);
      fan.style.transform = `scale(${Math.min(1, (room - 24) / 380).toFixed(4)})`;
      const spread = 78;
      cards.forEach(c => {
        const d = c.i - 1, x = d * (6 + f.x * (spread - 6)), r = d * (3 + f.x * 9);
        const y = Math.abs(d) * f.x * 12 + c.lift.x;
        c.el.style.transform = `translate(${x.toFixed(2)}px,${y.toFixed(2)}px) rotate(${r.toFixed(3)}deg) scale(${c.sc.x.toFixed(4)})`;
      });
    },
  });
})();
