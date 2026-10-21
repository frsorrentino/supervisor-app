# Link alle sessioni: «Apri in supervisor» e cmwatch://session; la master fuori dalla lista (21/10/2026)

Esito: nella chat di Android e della web un link `https://claude.ai/code/session_…` di una sessione che il relay conosce diventa il tasto «Apri in supervisor» col nome della sessione sotto; `cmwatch://session/<nome>` sul telefono e `?s=<nome>` sul web aprono la chat di quella sessione. Sulla stessa notte, in un commit a parte: la master non compare più fra le sessioni della home («Ti aspettano», «Hanno finito») e il contatore «N per te» non conta le sue voci. Ramo `notte/link-sessioni`, su origin. Nessun campo nuovo nel contratto. Niente release e niente deploy.

Segnalazione: Franz dal telefono, 21/10 alle 19:04, «piuttosto che mandare il link all'app claude possiamo mandare link o tasto che apra sessioni in app supervisor?», sullo screenshot della master che dava il link della sessione legnaia. Seconda segnalazione alle 21:15, «perché vedo anche la master tra le altre sessioni? non mi pare che dovrebbe essere presente lì»: nella home «Ti aspettano · 2» con supervisor-app (ok push) e la master («Ok push link-sessioni»), e in testata «2 per te» per lo stesso ok.

## Cosa c'è

1. **Il tasto nella chat** (Android e web).
   - Un link `claude.ai/code/session_<id>` nel testo di una risposta viene confrontato con `sessions[].link` dello stato. Se è il link di una sessione nota, esce dal testo e sotto la risposta compare il tasto «Apri in supervisor» con il nome della sessione sulla seconda riga. Un tasto per sessione, nell'ordine in cui i link compaiono.
   - Il tocco apre la chat di quella sessione nell'app: sul telefono come pagina, sul tablet in colonna, sul web in colonna o come pagina; la master a casa sua, come per ogni altra strada.
   - Il link com'era resta: su Android con la pressione lunga sul tasto (si apre nel browser o nell'app Claude); sul web il tasto è un link vero, quindi tasto destro o pressione lunga danno il menu del browser (apri in una scheda nuova, copia il link). Copia e ▶ usano il testo intero, link compreso.
   - Il testo intorno si sistema: la riga che senza il link è solo un'etichetta («• **Link:**», anche quando il link sta sulla riga dopo) sparisce; le parentesi rimaste vuote anche («ledger-api (link) · AskUserQuestion» diventa «ledger-api · AskUserQuestion»); i due punti rimasti in coda a una frase se ne vanno; un link con testo `[legnaia](url)` lascia il suo testo. Punteggiatura attaccata al link («url.») va via con lui.
   - Un link di sessione che lo stato non conosce resta un link normale, come un link qualunque. Una sessione chiusa ma ancora nello stato è nota: il tasto apre la sua pagina chiusa, con «Riapri».
   - Il link di una sessione appena lanciata arriva nel messaggio prima che lo stato la elenchi: finché lo stato non la ha, è un link; al primo stato con la sessione diventa il tasto, senza fare niente.
2. **Il deep link.** `cmwatch://session/<nome>` (nome codificato come in un URL, `field%20notes`) apre la chat di quella sessione sul telefono, `master` compresa; app chiusa o aperta, stessa strada del widget. Sul web `?s=<nome>` fa lo stesso ed è un alias di `#nome`, che c'era già. Entrambi sono nel README dell'app, sezione «How it connects».
3. **Contratto.** Nessun campo manca: `sessions[].link` è già il link Remote Control. Vale per le sessioni di cui il relay pubblica `link`; una sessione senza Remote Control ha `link` vuoto e il suo link nel testo, se mai ci fosse, resta un link.
4. **La master fuori dalla lista** (commit a parte). La lista delle sessioni della home non ha mai la master: né fra chi ti aspetta (domanda, `needs_you`, voce bloccante), né fra chi ha finito, né al lavoro o ferma. Quello che la master chiede resta dove già stava: nella sua barra in basso e nel campo di scrittura col tasto «Usa». Il contatore «N per te» in testata conta le voci di `state.attention` senza quelle della master, come già faceva il banner «N ti aspettano» (contratto 1.68); il tocco porta alla prima voce delle altre sessioni. Nello screenshot delle 21:15 la home mostra quindi «Ti aspettano · 1» (supervisor-app) e «1 per te», come il banner. La coda «Ti aspettano» delle domande (pagina a parte) e la card «Per te» nella pagina della master non le ho toccate: elencano le domande, non le sessioni.

## Da decidere lato relay (supervisor)

- Per le notifiche Android del relay (FCM) e per qualunque Intent: `cmwatch://session/<nome>`.
- Per Telegram: Telegram non rende toccabile uno schema `cmwatch://` e i tasti inline accettano solo `http(s)`. Il link utile lì è `https://supervisor.francescosorrentino.com/?s=<nome>`, che apre la PWA o il browser sulla chat di quella sessione. Se si vuole che su Android quel link apra l'app nativa, servono gli App Links (`assetlinks.json` sul sito e l'`intent-filter` `https` con `autoVerify`): è un lavoro a parte, non l'ho fatto.
- La master può smettere di scrivere il link in chiaro: il tasto lo sostituisce. Se lo scrive ancora, il testo intorno si pulisce da solo.

## Codice

- `core/.../rules/SessionLinks.kt`: `sessionOf`, `strip`, `deepLink`, `deepLinkTo`; `SessionLinksTest` (10 test, fixture `state-1-question.json`). In `Links.kt` un `*` in coda non fa più parte del link (`**…url**`).
- `web/src/lib/sessionLinks.ts`: la porta con gli stessi casi in `sessionLinks.test.ts`, più `askedSession` per `?s=` e `#`.
- Android: `SessionSheet.kt` (`SheetActions.openSession`, `ClaudeBubble` con `sessions` e `onOpenSession`, `SessionButton`), `MainActivity.kt` (`openSession`, `route` col deep link), `AndroidManifest.xml` (host `session`), stringhe it/en `open_in_supervisor`, screenshot test `sheetSessionLink`.
- Web: `Feed.svelte` (tasto `.sbtn`), `Chat.svelte` (passa `st.sessions` e `onPick`), `App.svelte` (`askedSession`), `t.ts`, `demo.ts` (il messaggio della master nella demo ha il link di payments-api: il tasto si vede con `?demo`).
- La master fuori dalla lista: `Summary.kt` e `summary.ts` (le righe «ti aspetta» dalle altre sessioni, `others`, come già «ha finito», «al lavoro» e «ferme»); `MainActivity.kt` e `AppBar.svelte` + `App.svelte` (il contatore e il tocco su `StrongAlert.forMaster`); test `SummaryTest.theMasterIsNeverListed` e «la master mai in lista» in `rules.test.ts`; nota nel README del contratto (1.66).

## Prove

Su win (Gradle e vitest non girano sul Chromebook):

- `:core:testDebugUnitTest`: LinksTest 4/4, MarkdownTest 8/8, SessionLinksTest 10/10, SummaryTest 12/12 (con `theMasterIsNeverListed`), StrongAlertTest 7/7.
- `:mobile:compileDebugKotlin` ok; `SessionSheetTest.sheetSessionLink` eseguito e PNG guardato: la riga «• **Link:**» sparita, il link sconosciuto resta un link, il tasto «Apri in supervisor / atlas-shop» sotto il testo. Il golden lo registra la CI (sul PC Windows i golden differiscono per locale).
- vitest: 614 su 615, l'unico rosso è `zip.test.ts`, che fallisce solo su Windows da prima; `npm run check`: gli stessi 5 errori di prima (approvals, pdfPreview), nessuno nei file toccati; `npm run build` ok.
- Web demo dal vivo (build servita su localhost, chrome-bridge): sotto il messaggio della master il tasto «Apri in supervisor / payments-api»; il clic apre la chat di payments-api nella stessa scheda.
- Storia del ramo controllata prima del push (`git log origin/main..notte/link-sessioni -p`, grep e anonymizer sulle righe aggiunte): niente dati personali né segreti; i domini veri nei test (`a.it`, `b.it`) sostituiti con `example.com`/`example.org`.

## CI

- Run record (golden nuovo di `sheetSessionLink`): [39661335970](https://github.com/frsorrentino/supervisor-app/actions/runs/39661335970), verde; il golden è nel commit 292aebb1.
- Run normale (unit test core e mobile, verifica Paparazzi, APK debug e release): [39663273391](https://github.com/frsorrentino/supervisor-app/actions/runs/39663273391), verde.
- `feature/tablet` portato in fast-forward al tip del ramo (17f20cdc → 292aebb1), come chiesto dalla master: la release del Gantt approvata da Franz alle 12:01 (approvazione 379) non era mai stata fatta.
- Pre-release GitHub `notte-2026-10-21` (pre-release, non latest) con `mobile-release.apk` della run 39663273391: https://github.com/frsorrentino/supervisor-app/releases/tag/notte-2026-10-21 · APK diretto: https://github.com/frsorrentino/supervisor-app/releases/download/notte-2026-10-21/mobile-release.apk. Ok di Franz: approvazione 390 (dal telefono, 20:38) e 391 (inbox c2ce3e, 21:22, «Va installata sul telefono») sul compito `link-sessioni-app-21-10`. Niente deploy web.

## Cosa provare sul telefono

Dalla pre-release `notte-2026-10-21` (`mobile-release.apk`), sul Pixel 11 Pro XL:

1. Nella chat della master, un messaggio con `https://claude.ai/code/session_…` di una sessione aperta: al posto del link, sotto il testo, il tasto «Apri in supervisor» col nome della sessione. Tocco: si apre quella chat. Pressione lunga: il link di prima, nel browser o nell'app Claude. Un link di una sessione che il relay non conosce resta un link.
2. Deep link: `adb shell am start -a android.intent.action.VIEW -d cmwatch://session/master` (o un altro nome) apre quella chat, app chiusa o aperta.
3. Home: la master non sta mai fra le sessioni, neanche quando chiede un ok; quello che chiede sta nella sua barra in basso e nel campo di scrittura. In testata «N per te» conta solo le altre sessioni, come il banner «N ti aspettano».
4. Notte: il Gantt per progetti e per obiettivi, con le notti precedenti (era già sul ramo, dal merge di `notte/gantt`).
5. Web (senza deploy, solo in locale o alla prossima pubblicazione): `?s=<nome>` apre quella chat come `#nome`; con `?demo` il messaggio della master ha il tasto per payments-api; tasto destro sul tasto dà il menu del browser col link.

FINE
