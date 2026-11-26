// Prompt Box
// Types a prompt by itself, sends it, thinks, and says the draft is ready, on a
// loop. Click into the field to take over: type, Enter sends, Shift+Enter is a
// new line, and the button stops an answer halfway.
(() => {
  const { S, step, clamp, after, cancel } = Motion;
  const $ = id => document.getElementById(id);
  const field = $('field'), send = $('send'), status = $('status'), label = $('statusText');
  const arrow = send.querySelector('.arrow'), stop = send.querySelector('.stop');
  const PROMPTS = [
    'Write a launch post for our new motion kit, short and friendly',
    'Turn these meeting notes into three clear next steps',
    'Suggest five names for a calm habit tracker',
  ];
  const H = S(22, 420, 34);     // field height
  const SH = S(0, 380, 30);     // status line, 0 closed, 1 open
  const M = S(0, 520, 30);      // button, 0 arrow, 1 stop
  let busy = false, user = false, p = 0, timers = [];
  const later = (s, f) => { const t = after(s, () => { timers = timers.filter(x => x !== t); f(); }); timers.push(t); };
  const clear = () => { timers.forEach(cancel); timers = []; };

  function fit() {
    field.style.height = 'auto';
    H.t = Math.min(field.scrollHeight, 88);
    field.style.height = H.x + 'px';
    send.classList.toggle('ready', busy || !!field.value.trim());
  }

  function say(kind, text) { status.className = 'status ' + kind; label.textContent = text; }

  function submit() {
    if (busy || !field.value.trim()) return;
    busy = true; M.t = 1; SH.t = 1;
    say('think', 'Thinking');
    send.setAttribute('aria-label', 'Stop');
    later(.3, () => { field.value = ''; fit(); });
    later(2.3, () => finish('Draft ready'));
  }

  function finish(text) {
    busy = false; M.t = 0;
    say('done', text);
    send.setAttribute('aria-label', 'Send');
    fit();
    later(1.6, () => { SH.t = 0; if (!user) later(.7, auto); });
  }

  function auto() {
    const text = PROMPTS[p++ % PROMPTS.length];
    [...text].forEach((_, i) => later(i * .035, () => { field.value = text.slice(0, i + 1); fit(); }));
    later(text.length * .035 + .7, submit);
  }

  send.addEventListener('click', () => {
    if (busy) { clear(); finish('Stopped'); }
    else submit();
  });
  field.addEventListener('focus', () => {
    if (user) return;
    user = true;
    if (!busy) { clear(); field.value = ''; SH.t = 0; fit(); }
  });
  field.addEventListener('input', fit);
  field.addEventListener('keydown', e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); submit(); } });

  fit();
  later(.6, auto);

  Motion.register({
    frame(dt) { [H, SH, M].forEach(s => step(s, dt)); },
    render() {
      field.style.height = H.x.toFixed(2) + 'px';
      const o = clamp(SH.x, 0, 1);
      status.style.height = (Math.max(0, SH.x) * 30).toFixed(2) + 'px';
      status.style.opacity = o.toFixed(3);
      const m = clamp(M.x, 0, 1.2);
      arrow.style.transform = `scale(${Math.max(0, 1 - m)}) rotate(${m * 90}deg)`;
      stop.style.transform = `scale(${Math.max(0, m)})`;
    },
  });
})();
