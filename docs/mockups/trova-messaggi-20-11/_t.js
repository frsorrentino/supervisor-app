// La conversazione della master com'era la sera del 20/11, e i pezzi comuni delle tre bozze.
const I = {
  back: '<svg class="ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5M11 6l-6 6 6 6"/></svg>',
  mine: '<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M4 5h16v10H9l-5 4z"/><path d="M8 9h8M8 12h5"/></svg>',
  search: '<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="6.5"/><path d="m16 16 4 4"/></svg>',
  up: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 15 6-6 6 6"/></svg>',
  down: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 9 6 6 6-6"/></svg>',
  x: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M6 6l12 12M18 6 6 18"/></svg>',
  menu: '<svg class="ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M4 7h16M4 12h16M4 17h16"/></svg>',
}
// I messaggi: [chi, testo, ora, nota]
const MSG = {
  m1: ['me', 'Va bene la C, ma non voglio il tasto bianco.', '19:08'],
  b1: ['bot', 'Ricevuto: la carta della chiusura senza tasto bianco, l\'azione principale col velo ambra. Lo passo a win:supervisor-app.', '19:09'],
  m2: ['me', 'Ci sono ancora incoerenze: dopo un\'approvazione in un punto, in altri risultano da approvare.', '20:38'],
  b2: ['bot', 'Le carte delle prove nella chat leggevano la prova una volta sola. Ho aperto app-carte-stato-vivo-20-11.', '20:41'],
  m3: ['me', 'Su Pixel 11 e Pixel Tablet si può installare Claude, visto che hanno il terminale Linux?', '20:44'],
}
const esc = s => s
function bubble(k, o = {}) {
  const [who, text, at] = MSG[k]
  const t = o.mark ? text.replace(o.mark, m => `<mark class="${o.now ? 'now' : ''}">${m}</mark>`) : text
  if (who === 'me') return `<div class="me ${o.hit ? 'hit' : ''} ${o.dim ? 'dim' : ''}">${t}<small>${o.warn ? '<span class="warn">non arrivato al PC · </span>' : ''}${at}</small></div>`
  return `<div class="bot ${o.dim ? 'dim' : ''}">${t}<small>${at}</small></div>`
}
function phone({ head = '', feed = '', bottom = '', over = '', find = '', title = 'Master' }) {
  return `<div class="ph">
    <div class="sb"><span>20:58</span><span>44%</span></div>
    ${head || `<div class="top">${I.back}<b>${title}</b>${find}${I.menu}</div>`}
    <div class="chips"><span>Opus 5.5 · max</span><span>5h 16%</span><span>ctx 58%</span></div>
    <div class="feed">${feed}</div>
    ${bottom || '<div class="comp">Scrivi alla master</div>'}
    ${over}
    <div class="gest"></div>
  </div>`
}
function frames(list) {
  document.getElementById('row').innerHTML = list.map(f => `<div class="col"><div class="cap">${f.cap}<small>${f.sub}</small></div>${phone(f)}</div>`).join('')
}
