// Read Along
// The sentence is read out word by word: a pill slides behind the word being
// read, words already read turn bright. The pill's edges are two springs, the
// one in front fast and the one behind slow, so it stretches as it travels.
// At a line break it closes up and opens again on the next line instead of
// sliding across the text. Click any word to read from there.
(() => {
  const { S, tune, step, clamp } = Motion;
  const read = document.getElementById('read'), pill = document.getElementById('pill');
  const TEXT = 'Make the thing, then make it simple, then make it yours.';
  const PAD = .16;   // em the pill reaches past each side of a word

  const words = TEXT.split(' ').map((w, i, all) => {
    const el = document.createElement('span');
    el.className = 'w';
    el.textContent = w;
    el.setAttribute('aria-hidden', 'true');
    read.appendChild(el);
    if (i < all.length - 1) read.append(' ');
    el.addEventListener('click', () => { at = i; wait = dwell(i); show(); });
    return el;
  });

  const L = S(0, 520, 40), R = S(0, 520, 40), Y = S(0, 600, 48), O = S(0, 300, 30);
  let at = -1, wait = 1, lineTop = null;
  const dwell = i => .2 + words[i].textContent.length * .035;
  const em = () => parseFloat(getComputedStyle(read).fontSize);

  function box(i) {
    const el = words[i], p = em() * PAD;
    return { l: el.offsetLeft - p, r: el.offsetLeft + el.offsetWidth + p, top: el.offsetTop + (el.offsetHeight - em() * 1.3) / 2 };
  }

  function show() {
    words.forEach((w, k) => {
      w.classList.toggle('past', k < at);
      w.classList.toggle('now', k === at);
    });
    if (at < 0) { O.t = 0; return; }
    const b = box(at);
    O.t = 1;
    const forward = b.l >= L.t;
    if (lineTop === null || Math.abs(b.top - lineTop) > 2 || O.x < .05) {
      // New line (or first word): open up from the word's left edge, no slide.
      L.x = L.t = b.l; R.x = b.l; Y.x = Y.t = b.top;
    }
    lineTop = b.top;
    tune(forward ? R : L, 520, 40);    // the edge in front of the move is quick
    tune(forward ? L : R, 210, 26);    // the edge behind it lags, so the pill stretches
    L.t = b.l; R.t = b.r; Y.t = b.top;
  }

  Motion.register({
    frame(dt) {
      if ((wait -= dt) <= 0) {
        if (at === words.length - 1) { at = -1; lineTop = null; wait = 1.1; }
        else { at++; wait = at === words.length - 1 ? 1.8 : dwell(at); }
        show();
      }
      [L, R, Y, O].forEach(s => step(s, dt));
    },
    render() {
      const w = Math.max(0, R.x - L.x);
      pill.style.transform = `translate(${L.x.toFixed(2)}px,${Y.x.toFixed(2)}px)`;
      pill.style.width = w.toFixed(2) + 'px';
      pill.style.opacity = clamp(O.x, 0, 1).toFixed(3);
    },
  });
})();
