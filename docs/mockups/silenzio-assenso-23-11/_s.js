// I pezzi comuni delle tre bozze: una prova e un ok veri di oggi, la lista «Per te» e la cronologia dei compiti chiusi.
const I = {
  back: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 5l-7 7 7 7"/></svg>',
  clock: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/></svg>',
  prova: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="m3 6.5 1.8 1.8L8 5M3 16.5l1.8 1.8L8 15M12 7h9M12 17h9"/></svg>',
  ok: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="m14 13-8.4 8.4a2 2 0 0 1-2.8-2.8L11.2 10.2M16 16l6-6M8 8l6-6M9 7l8 8M21 11l-8-8"/></svg>',
  check: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="m5 12.5 4.5 4.5L19 7.5"/></svg>',
  chev: '<svg class="chev" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l6 6-6 6"/></svg>',
}
const DUE = 'giovedì 26/11 alle 13:30'
const DUE_S = 'gio 26/11'

// La carta della prova: [testa, corpo, tasti, sotto i tasti].
function prova({ head = '', pills, after = '' }) {
  return `<div class="wc">
    <p class="k">Prova 1412${head}</p>
    <p class="ttl">La scheda di chiusura in basso</p>
    <p class="q">Chiudi una sessione dalla home: la scheda sale dal basso, senza velo, col tasto principale ambra.</p>
    <dl><dt>Dove</dt><dd>telefono, app 0.2.2290</dd><dt>Chiesta</dt><dd>oggi 13:30</dd></dl>
    <div class="pills">${pills ?? `<span class="pill main">Va</span><span class="pill">Non va</span>`}</div>${after}
  </div>`
}
// La carta di un ok di un cliente (non una release, non un invio).
function ok({ head = '', pills, after = '' }) {
  return `<div class="wc">
    <p class="k">Ok del cliente${head}</p>
    <p class="ttl">Bozza della pagina Chi siamo</p>
    <p class="q">Il testo nuovo della pagina Chi siamo di atlas-shop, con le due foto del negozio.</p>
    <dl><dt>Chi</dt><dd>atlas-shop, il cliente</dd><dt>Chiesto</dt><dd>oggi 13:30</dd></dl>
    <div class="pills">${pills ?? `<span class="pill main">Approva</span><span class="pill">Rifai</span>`}</div>${after}
  </div>`
}
// «Per te» con le due carte: la data in cui si chiude da sola sta nella riga di contesto, in blu.
function perTe(mark = true) {
  const sub = (s) => mark ? `${s} · <span class="tacit">si chiude da sola ${DUE_S}</span>` : `${s} · 2 m`
  return `<div class="list"><h3>Per te · 3</h3>
    <div class="voce"><span class="ic">${I.prova}</span><span class="tx"><span class="ti">La scheda di chiusura in basso</span><span class="cx">${sub('prova 1412')}</span></span><span class="mini">Va</span></div>
    <div class="voce"><span class="ic">${I.ok}</span><span class="tx"><span class="ti">Bozza della pagina Chi siamo</span><span class="cx">${sub('ok del cliente')}</span></span><span class="mini">Approva</span></div>
    <div class="voce"><span class="ic">${I.ok}</span><span class="tx"><span class="ti">Quale icona per il quadrante</span><span class="cx">scelta fra 3 · 1 h</span></span>${I.chev}</div>
  </div>`
}
// La cronologia: il compito chiuso per silenzio assenso ha la sua etichetta, mai il verde dell'ok di Franz.
function storia() {
  return `<div class="list"><h3>Chiusi questa settimana</h3>
    <div class="voce"><span class="ic good">${I.check}</span><span class="tx"><span class="ti">app-trova-messaggi-20-11</span><span class="cx good">ok di Franz · 20/11 21:40</span></span>${I.chev}</div>
    <div class="voce"><span class="ic tacit">${I.clock}</span><span class="tx"><span class="ti">Bozza della pagina Chi siamo</span><span class="cx"><span class="lab">${I.clock}silenzio assenso</span> · 26/11 13:30</span></span>${I.chev}</div>
    <div class="voce"><span class="ic good">${I.check}</span><span class="tx"><span class="ti">app-carte-stato-vivo-20-11</span><span class="cx good">ok di Franz · 20/11 22:05</span></span>${I.chev}</div>
  </div>`
}
function phone({ title = 'Ti aspetta', body, over = '' }) {
  return `<div class="ph"><div class="sb"><span>13:31</span><span>62%</span></div>
    <div class="top">${I.back}<b>${title}</b></div>
    <div class="body">${body}</div>${over}<div class="gest"></div></div>`
}
function frames(list) {
  document.getElementById('row').innerHTML = list.map(f => `<div class="col"><p class="cap">${f.cap}<small>${f.sub}</small></p>${phone(f)}</div>`).join('')
}
