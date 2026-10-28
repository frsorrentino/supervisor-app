# Modalità live, fase 3 — esito (07/10/2026)

Piano: `docs/plans/2026-10-07-live-fase-3.md`. Ramo `live/fase-3` da `390f524`.

## Fatto

| Commit | Pezzo | Verifica |
|---|---|---|
| `9dbebc8` | contratto 1.42: `Cmd.voice`, `via`, `confirmations`; 3 fixture in coda a `cmd-result-sample.json`; README | `ContractTest` verde; relay 1.42 in team-supervisor `9e9b6f0` (R35 4/4) |
| `3d94938` | `LiveFeed`: `launched` e `recap` conservati in `Feed.info` (5, ultime 2 ore) e letti nel giro completo; `heard`, `status`, `waiting`, `quotaAll` | `LiveFeedTest` 26/26 |
| `8f85bc1` | `LiveCard`, `LiveTap`, `LiveWire` (`/cmwatch/live/card`, `/cmwatch/live/tap`) | `LiveCardTest` 3/3 |
| `56a19cd` | `LiveDesk`, la regia pura: notizie, tocchi, dettato, domande alla master, doppia conferma, collegamento, pausa | `LiveDeskTest` 19/19 |
| `0aa5197` | telefono: `LiveService`, `LiveVoice`, `LiveLabels`, frasi in strings.xml it/en, voce «Modalità live» nel menu, tocchi dal watch | `LiveStringsTest` 1/1; `:mobile:compileDebugKotlin` ok |
| `7178d17` | watch: `LiveLink`, `LiveActivity`, `LiveScreen`, notifica fissa, test Paparazzi (domanda, esito, conferma) | `:wear:compileDebugUnitTestKotlin` ok; golden da registrare in CI |

Suite del core sulla punta `7178d17`: 932 test, 0 falliti.

## Decisioni prese qui

1. Contratto 1.42 come da specifica §4; testo del rifiuto «<task>: live approval needs a double confirmation».
2. La live si accende dal telefono (menu o notifica): Android 14 non lascia partire un servizio in primo piano da un
   messaggio del watch in background. Il watch riceve la scheda e mostra una notifica fissa che apre `LiveActivity`.
3. La regia sta tutta nel `core` (`LiveDesk`), il servizio esegue solo gli effetti; il watch non ha regole.
4. Dopo la lettura una scheda con tasti (domanda, ok, esito con Prossimi) resta 20 s, le altre 4 s; un livello 1 passa
   davanti. Una notizia interrotta da una risposta a Franz resta in coda e si rilegge.
5. Il suono d'attesa della master suona solo a voce ferma; l'esito della master che risponde a una domanda a voce non
   diventa una notizia d'esito: si legge la risposta intera di `last`, prima della coda.
6. Tasti degli auricolari: play/pausa = pausa, avanti = Salta, indietro = Ripeti. La pressione lunga resta al sistema.
7. Seconda conferma: «Tieni premuto 2 s» sulla scheda, diverso da «Approva»; il watch manda pressione e rilascio, il
   telefono conta il tempo (tick da 250 ms) e il watch vibra a conferma.
8. Fine chiamata: il focus torna quando `AudioManager.mode` è di nuovo normale.
9. Avvisi del servizio (live accesa, batteria) passano da `LiveDesk.notice`, come una risposta.

## Non fatto o da provare dal vivo

- La prova sul campo (20 minuti a piedi, criteri §1): serve Franz.
- I tasti degli auricolari con MediaSession: dipende da chi tiene il media button; da provare con i suoi auricolari.
- I suoni di inizio e fine dettatura sono quelli del sistema.
- I test sono passati al primo giro dopo la scrittura: il «rosso» è stato quello di compilazione (classi assenti), non
  un rosso di comportamento.

## Pubblicazione (07/10 mattina)

- `feature/tablet` su origin fino ai golden (live sul watch `8147f3f`, menu del telefono); CI 37588346293 verde: unit
  core e mobile, Paparazzi wear e mobile, APK release. Un primo giro (37583541287) era rosso solo per il golden del menu
  del telefono, che ora ha la voce «Modalità live».
- `origin/master`: `f7c07ea`, la 1.42 portata su master per R0 di relay-verify (ContractTest su master 25/25).
- Telefono Pixel 11 Pro XL: APK release installato alle 09:54. Tablet e orologio non installati: rete passata a
  192.168.1.x, tablet senza debug wireless annunciato, orologio annunciato ma `adb connect` rifiutato (da riaccoppiare).
