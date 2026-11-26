// Ghost Suggestion
// A sentence is typed, the AI offers the rest of it in a dim colour with a Tab
// key beside it, and Tab accepts: each suggested letter turns bright with a
// small hop, left to right, and the caret jumps to the end. Then the next note.
// Focus the card and press Tab to accept before it does.
(() => {
  const { S, step, clamp, after, cancel } = Motion;
  const $ = id => document.getElementById(id);
  const ed = $('ed'), body = $('body'), typed = $('typed'), caret = $('caret'), ghost = $('ghost'), tab = $('tab');
  const NOTES = [
    ['The new onboarding cuts setup', ' from ten minutes to two.'],
    ['Ship the beta to ten teams', ' first, then open the waitlist.'],
    ['Move the pricing link into', ' the header, where people look for it.'],
  ];
  const tabS = S(0, 520, 24), fade = S(1, 300, 30);
  let letters = [], n = 0, offered = false, timers = [], clock = 0, typing = false;
  const later = (s, f) => { const t = after(s, () => { timers = timers.filter(x => x !== t); f(); }); timers.push(t); };

  function next() {
    const [start, rest] = NOTES[n++ % NOTES.length];
    typed.textContent = ''; ghost.innerHTML = ''; letters = []; offered = false;
    body.insertBefore(caret, ghost);
    fade.x = fade.t = 1; tabS.x = tabS.t = 0;
    typing = true;
    [...start].forEach((_, i) => later(.3 + i * .045, () => { typed.textContent = start.slice(0, i + 1); }));
    later(.3 + start.length * .045 + .45, () => { typing = false; offer(rest); });
  }

  function offer(rest) {
    // Letters sit inside one unbreakable span per word, so a line only ever
    // wraps between words, never in the middle of one.
    letters = [];
    rest.split(/( )/).forEach(part => {
      if (part === '') return;
      if (part === ' ') { ghost.append(' '); return; }
      const word = document.createElement('span');
      word.style.whiteSpace = 'nowrap';
      ghost.appendChild(word);
      [...part].forEach(ch => {
        const el = document.createElement('span');
        el.className = 'g';
        el.textContent = ch;
        el.style.opacity = 0;
        word.appendChild(el);
        letters.push({ el, o: S(0, 300, 30), y: S(0, 520, 16) });
      });
    });
    letters.forEach((l, i) => later(i * .008, () => { l.o.t = 1; }));
    tabS.t = 1; offered = true;
    later(1.5, accept);
  }

  function accept() {
    if (!offered) return;
    offered = false;
    timers.forEach(cancel); timers = [];
    tabS.v = -9; tabS.t = 0;                      // the key goes down and away
    letters.forEach((l, i) => later(i * .016, () => { l.el.classList.add('ok'); l.y.v = -70; l.o.t = 1; }));
    later(letters.length * .016, () => body.insertBefore(caret, tab));
    later(letters.length * .016 + 1.7, () => { fade.t = 0; });
    later(letters.length * .016 + 2.2, next);
  }

  ed.addEventListener('keydown', e => { if (e.key === 'Tab' && offered) { e.preventDefault(); accept(); } });
  ed.addEventListener('click', accept);
  next();

  Motion.register({
    frame(dt) {
      clock += dt;
      step(tabS, dt); step(fade, dt);
      letters.forEach(l => { step(l.o, dt); step(l.y, dt); });
    },
    render() {
      body.style.opacity = clamp(fade.x, 0, 1).toFixed(3);
      const t = clamp(tabS.x, -.2, 1.2);
      tab.style.opacity = clamp(t, 0, 1).toFixed(3);
      tab.style.transform = `scale(${(.6 + .4 * clamp(t, 0, 1.2)).toFixed(4)})`;
      letters.forEach(l => {
        l.el.style.opacity = clamp(l.o.x, 0, 1).toFixed(3);
        l.el.style.transform = `translateY(${l.y.x.toFixed(2)}px)`;
      });
      caret.style.opacity = typing || (clock * 1.8) % 2 < 1 ? 1 : 0;
    },
  });
})();
