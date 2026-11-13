# iPhone: la chat corta e il rimbalzo della pagina (app-ios-chat-scroll-13-11)

Segnalazione del collega dalla PWA su iPhone (web 0.2.2154, video del 13/11, non nel repo): la chat di una sessione al lavoro
«si apre con spazio in basso», e a ogni trascinamento tutta la pagina scende con la testata, lascia del nero sopra e torna su.

Cosa si vede nel video: la chat ha un turno solo, col lavoro ridotto alle ultime tre voci («Lavoro · 44 passi»), sotto «Carica i
messaggi precedenti». Il contenuto è più corto della lista: stava in cima con lo spazio vuoto sotto, e non c'era niente da
scorrere. Il dito sulla lista allora trascinava la pagina, che su iPhone rimbalza anche se non scorre; `#app` è fisso sull'area
visibile (app-ios-scatti-tastiera-12-11) e rimbalzava tutto insieme, testata compresa.

Correzione: la pagina non rimbalza (`overscroll-behavior: none` su `html` e `body`, Safari dalla 16) e la lista non passa lo
scorrimento alla pagina (`contain`); una chat corta sta in fondo, accanto al campo (`margin-top: auto` sulla prima voce).

`chat-corta.mjs` (Playwright 1.49.1, da una cartella con `npm i playwright@1.49.1`, demo su `localhost:4237`) misura nella vista
di un iPhone (390 × 844) lo spazio vuoto di una chat corta e che una chat lunga arrivi ancora in cima e in fondo.

Misura del 13/11/2026:

| Browser | chat corta, vuoto sotto: prima | dopo | vuoto sopra dopo | chat lunga arriva in cima |
| --- | --- | --- | --- | --- |
| WebKit | 272 px | 20 px (il margine della lista) | 272 px | sì |
| Chromium | 275 px | 20 px | 275 px | sì |

Limiti: il rimbalzo della pagina è di iOS e non si riproduce con WebKit su Windows (lì non c'è rimbalzo, e `overscroll-behavior`
non compare fra gli stili calcolati); la regola è controllata in `viewport.test.ts` e in Chromium (pagina `none`, lista
`contain`). La prova vera è sull'iPhone del collega, PWA e Safari.
