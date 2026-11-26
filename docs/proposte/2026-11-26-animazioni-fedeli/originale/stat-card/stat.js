// Stat Card
// Twelve springs, one per chart point, drawn as a smooth curve with a filled
// area. A range change retargets them left to right, the total and the change
// chip roll to their new values and the pill slides under the new tab.
(() => {
  const { S, step, clamp, after } = Motion;
  const $ = id => document.getElementById(id);
  const stat = $('stat'), chart = $('chart'), line = $('line'), fill = $('fill'), dot = $('dot'), big = $('big'), delta = $('delta'), pill = $('pill');
  const btns = [...document.querySelectorAll('.seg button')];
  const RANGES = [
    { v: 12.8, d: 8.2, pts: [.30, .42, .36, .50, .46, .58, .52, .64, .60, .70, .66, .78] },
    { v: 48.6, d: 12.4, pts: [.22, .28, .40, .34, .48, .44, .60, .56, .66, .74, .70, .86] },
    { v: 482, d: 31.9, pts: [.10, .16, .14, .26, .34, .30, .46, .52, .58, .70, .80, .94] },
  ];
  const pts = RANGES[1].pts.map(() => S(0, 200, 17));
  const val = S(0, 160, 22), dv = S(0, 160, 22), px = S(0, 380, 30), pw = S(0, 380, 30);
  let r = -1, clock = 0, userUntil = 0;

  function pick(i) {
    r = i;
    btns.forEach((b, k) => b.classList.toggle('on', k === i));
    px.t = btns[i].offsetLeft; pw.t = btns[i].offsetWidth;
    if (!pw.x) { px.x = px.t; pw.x = pw.t; }
    val.t = RANGES[i].v; dv.t = RANGES[i].d;
    pts.forEach((p, k) => after(k * .03, () => { if (r === i) p.t = RANGES[i].pts[k]; }));
  }
  btns.forEach((b, i) => b.addEventListener('click', () => { userUntil = clock + 8; pick(i); }));
  pick(1);
  (function auto() {
    if (clock >= userUntil) pick((r + 1) % 3);
    after(3, auto);
  })();

  const money = v => '$' + (v >= 100 ? Math.round(v) : v.toFixed(1)) + 'k';

  Motion.register({
    frame(dt) {
      clock += dt;
      pts.forEach(p => step(p, dt));
      [val, dv, px, pw].forEach(s => step(s, dt));
    },
    render() {
      const W = chart.clientWidth, H = 96, n = pts.length;
      chart.setAttribute('viewBox', `0 0 ${W} ${H}`);
      const xy = pts.map((p, i) => [4 + i / (n - 1) * (W - 8), 8 + (1 - clamp(p.x, -.05, 1.05)) * (H - 16)]);
      let d = `M${xy[0][0].toFixed(2)} ${xy[0][1].toFixed(2)}`;
      for (let i = 0; i < n - 1; i++) {
        const p0 = xy[i - 1] || xy[i], p1 = xy[i], p2 = xy[i + 1], p3 = xy[i + 2] || p2;
        d += ` C${(p1[0] + (p2[0] - p0[0]) / 6).toFixed(2)} ${(p1[1] + (p2[1] - p0[1]) / 6).toFixed(2)} ${(p2[0] - (p3[0] - p1[0]) / 6).toFixed(2)} ${(p2[1] - (p3[1] - p1[1]) / 6).toFixed(2)} ${p2[0].toFixed(2)} ${p2[1].toFixed(2)}`;
      }
      line.setAttribute('d', d);
      fill.setAttribute('d', `${d} L${xy[n - 1][0].toFixed(2)} ${H} L${xy[0][0].toFixed(2)} ${H} Z`);
      const [lx, ly] = xy[n - 1];
      dot.style.transform = `translate(${(lx - 4.5).toFixed(2)}px,${(ly - 4.5).toFixed(2)}px)`;
      big.textContent = money(Math.max(0, val.x));
      delta.textContent = '+' + Math.max(0, dv.x).toFixed(1) + '%';
      pill.style.transform = `translateX(${px.x.toFixed(2)}px)`;
      pill.style.width = pw.x.toFixed(2) + 'px';
    },
  });
})();
