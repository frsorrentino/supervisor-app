# iPhone: il salto della trascrizione (app-ios-scatti-tastiera-12-11)

`salto.mjs` sale una trascrizione di 160 voci (una su tre lunga) a passi di 300 px e misura, per ogni passo, quanto la voce più in
alto sullo schermo si sposta oltre lo scorrimento. Si lancia da una cartella con `npm i playwright@1.49.1` e
`npx playwright install webkit`: `node salto.mjs`.

Misura del 13/11/2026 (Playwright 1.49.1, WebKit senza `overflow-anchor` come Safari fino a iOS 26):

| Browser | content-visibility | salto peggiore | salto totale |
| --- | --- | --- | --- |
| WebKit | sì (prima) | 792 px | 30 096 px |
| WebKit | no (ora, senza ancoraggio) | 0 | 0 |
| Chromium | sì (resta: ha l'ancoraggio) | 792 px | 11 880 px |
| Chromium | no | 0 | 0 |

Nella demo su WebKit `html.anchor` manca e le righe della home hanno `content-visibility: visible`; su Chromium `anchor` c'è e
restano `auto`, come prima. Il passo programmato del test aggira in parte l'ancoraggio di Chromium: con le dita e la rotella
Chrome tiene fermo il punto di lettura, e Android e desktop restano come prima.

La tastiera della PWA non si prova qui: WebKit su Windows non ha la tastiera di iOS. Le regole stanno in `web/src/lib/viewport.ts`
coi test in `viewport.test.ts`; la prova vera è su un iPhone, PWA e Safari.
