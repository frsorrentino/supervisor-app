// Le quattro fasi della chiusura con lo stesso testo; ogni bozza dà solo il contenitore e i tasti.
const N = 'joyconceptstore-com'
const I = {
  pc: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="12" rx="2"/><path d="M8 20h8M12 16v4"/></svg>',
  ok: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="m8 12.5 2.8 2.8L16.5 9.5"/></svg>',
  now: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="9" opacity=".3"/><path d="M12 3a9 9 0 0 1 9 9"/></svg>',
  todo: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="9"/></svg>',
  pw: (c) => `<svg class="ico" style="color:${c}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M12 3v8"/><path d="M6.3 6.8a8 8 0 1 0 11.4 0"/></svg>`,
  back: '<svg class="bk" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5M11 6l-6 6 6 6"/></svg>',
}
const F = {
  ask: {
    cap: ['Conferma', 'La finestra sul PC si dice prima di chiudere: è un\'informazione, non un divieto.'],
    body: `<div class="t">Chiudere <span class="nm">${N}?</span></div>
      <div class="st">È aperta e ferma dalle 12:39.</div>
      <div class="pc">${I.pc}<p>È aperta anche in una finestra sul PC<small>Chiudendo si chiude anche quella.</small></p></div>
      <ul class="what"><li>Se ha decisioni o lavoro in corso scrive il passaggio di consegne, poi riceve /exit</li><li>Entro 3 minuti si chiude comunque</li><li>Va in «Chiuse», da dove si riapre</li></ul>`,
  },
  closing: {
    cap: ['Si sta chiudendo', 'Stesso posto, stessa larghezza: i passi veri, il primo già fatto.'],
    body: `<div class="t"><span class="nm">${N}</span> si sta chiudendo</div>
      <div class="st">Salva e chiudi mandato alle 16:44.</div>
      <ul class="steps"><li class="ok">${I.ok}Passaggio di consegne scritto</li><li class="now">${I.now}Chiudo la finestra sul PC</li><li class="todo">${I.todo}Il PC conferma la chiusura</li></ul>
      <div class="prog"><i style="width:55%"></i></div><div class="note">Di solito ci vuole meno di un minuto.</div>`,
  },
  closed: {
    cap: ['Chiusa', 'Un solo esito, in italiano, qui. Niente snackbar sotto.'],
    body: `<div class="t"><span class="nm">${N}</span> è chiusa</div>
      <div class="st">Alle 16:45. La trovi in «Chiuse» e si può riaprire.</div>
      <div class="goodl">Passaggio di consegne salvato</div><div class="file">docs/handoff-2026-11-20.md</div>
      <div class="prog"><i style="width:40%;background:var(--tx3)"></i></div><div class="note">Torni alla home fra 2 secondi.</div>`,
  },
  big: {
    cap: ['Conferma, testo grande', 'Carattere a 1,3: nessun tasto va a capo, la finestra scorre se serve.'],
    body: null,
  },
  refused: {
    cap: ['Non chiusa', 'Se il PC dice di no: il motivo in italiano, una volta sola.'],
    body: `<div class="t"><span class="nm">${N}</span> non si è chiusa</div>
      <div class="why">Ha una domanda aperta: rispondi, poi chiudila.</div>
      <div class="st">Resta aperta com'era.</div>`,
  },
}
function phone(inner, opt, k) {
  return `<div class="ph${k === 'big' ? ' big' : ''}">
    <div class="sb"><span>16:44</span><span>67%</span></div>
    <div class="top">${I.back}<span class="dot"></span><b>${N}</b></div>
    <div class="chips"><span>Opus 5.5 · max</span><span>5h 6%</span><span>ctx 13%</span></div>
    <div class="page"><p>Ordine TNLAOGPFW di oggi alle 12:01: −15 €. Il buono era nato dall'ordine QUUHFWXES, spedito il 31/10.</p><p>Immagino che la domanda nasca dall'ordine di oggi, ma è una mia deduzione.</p><p>Se vuoi, si fa in modo che il buono si spenga da solo dopo ogni ordine.</p></div>
    ${opt.veil ? '<div class="veil"></div>' : '<div class="comp">Scrivi a ' + N + '</div>'}
    ${inner}
    <div class="gest"></div>
  </div>`
}
function build(opt) {
  const out = ['ask', 'closing', 'closed', 'refused', 'big'].map(k => {
    const f = F[k]; const body = f.body ?? F.ask.body; const acts = opt.acts[k] ?? opt.acts.ask
    const ico = k === 'closed' ? I.pw('var(--tx2)') : k === 'refused' ? I.pw('var(--amber)') : k === 'closing' ? I.pw('var(--busy)') : ''
    return `<div class="col"><div class="cap">${f.cap[0]}<small>${f.cap[1]}</small></div>${phone(opt.wrap(`<div class="phase">${body}</div>`, acts, ico, k), opt, k)}</div>`
  })
  document.getElementById('row').innerHTML = out.join('')
}
