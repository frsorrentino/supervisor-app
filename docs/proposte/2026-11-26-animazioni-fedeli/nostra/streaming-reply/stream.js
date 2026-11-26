// Streaming Reply
// A question goes up, the answer bubble opens as a small "Ci penso" pill, then
// the reply streams in a word at a time: each word rises and sharpens on its
// own spring while the bubble grows to hold it. Then it clears and asks again.
(() => {
  const { S, step, clamp, after } = Motion;
  const $ = id => document.getElementById(id);
  const chat = $('chat'), me = $('me'), ai = $('ai'), think = $('think'), text = $('text');
  const TURNS = [
    ['La CI è verde?', 'Sì: run 45810262740 tutta verde, pre-release 0.2.2296 con i due APK. Ti mando le prove da fare quando sei al telefono.'],
    ['Cosa manca alla release?', 'Solo la tua prova sulla 0.2.2297: Annulla dopo Non ora, la pila di Per te, e una domanda al polso appena arrivata.'],
  ];
  const PAD_X = 14, PAD_Y = 9;
  const meS = S(0, 300, 22), aiS = S(0, 300, 22), all = S(1, 260, 30);   // entrances, and the whole thread for the clear
  const W = S(0, 260, 26), Hh = S(40, 260, 26), T = S(1, 300, 30);     // bubble size, "Ci penso" opacity
  let words = [], turn = 0;

  const full = () => chat.offsetWidth * .9;

  function start() {
    const [q, a] = TURNS[turn++ % TURNS.length];
    me.textContent = q;
    text.innerHTML = '';
    text.style.width = (full() - PAD_X * 2) + 'px';
    words = a.split(' ').map((w, i, list) => {
      const el = document.createElement('span');
      el.className = 'wd';
      el.textContent = w;
      el.style.opacity = 0;
      text.appendChild(el);
      if (i < list.length - 1) text.append(' ');
      return { el, s: S(0, 340, 26), on: false };
    });
    all.x = all.t = 1;
    meS.x = meS.t = 0; aiS.x = aiS.t = 0; T.x = T.t = 1;
    W.x = W.t = think.offsetWidth + PAD_X * 2;
    Hh.x = Hh.t = 40;
    after(.15, () => { meS.t = 1; });
    after(.75, () => { aiS.t = 1; });
    after(2.1, stream);
  }

  function stream() {
    T.t = 0;
    W.t = full();
    words.forEach((w, i) => after(i * .07, () => {
      w.on = true; w.s.t = 1;
      Hh.t = text.offsetTop + w.el.offsetTop + w.el.offsetHeight + PAD_Y;
    }));
    after(words.length * .07 + 2.8, () => { all.t = 0; });
    after(words.length * .07 + 3.4, start);
  }

  start();

  Motion.register({
    frame(dt) {
      [meS, aiS, all, W, Hh, T].forEach(s => step(s, dt));
      words.forEach(w => step(w.s, dt));
    },
    render() {
      chat.style.opacity = clamp(all.x, 0, 1).toFixed(3);
      me.style.opacity = clamp(meS.x, 0, 1).toFixed(3);
      me.style.transform = `translateY(${((1 - meS.x) * 14).toFixed(2)}px) scale(${(.92 + .08 * meS.x).toFixed(4)})`;
      ai.style.opacity = clamp(aiS.x, 0, 1).toFixed(3);
      ai.style.transform = `translateY(${((1 - aiS.x) * 14).toFixed(2)}px)`;
      ai.style.width = W.x.toFixed(2) + 'px';
      ai.style.height = Hh.x.toFixed(2) + 'px';
      think.style.opacity = clamp(T.x, 0, 1).toFixed(3);
      words.forEach(w => {
        if (!w.on) return;
        const v = clamp(w.s.x, 0, 1.2);
        w.el.style.opacity = clamp(v, 0, 1).toFixed(3);
        w.el.style.transform = `translateY(${((1 - w.s.x) * 6).toFixed(2)}px)`;
        w.el.style.filter = v < .98 ? `blur(${((1 - clamp(v, 0, 1)) * 4).toFixed(2)}px)` : 'none';
      });
    },
  });
})();
