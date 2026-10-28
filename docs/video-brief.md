# Brief del film di supervisor: la parte app

Contenuto per la regia del film (sessione mgv su win), scritto il 09/10/2026 su richiesta di Franz (21:54). Copre telefono,
tablet, orologio, Recap, live, notte e Utilizzo. Il film mostra solo cose che l'app fa oggi.

## 1. La promessa

**«Le sessioni di Claude Code che lavorano sul tuo PC ti seguono fuori dalla scrivania: le vedi, rispondi e le rimetti in
moto dal telefono, dal tablet o dal polso.»**

Pubblico: chi tiene al lavoro più sessioni di Claude Code in parallelo, come sviluppatori freelance, piccoli team e agenzie,
e perde tempo quando una sessione si ferma ad aspettare una risposta mentre è lontano dal PC.

## 2. I momenti da mostrare

1. **Una domanda arriva al polso.** Una sessione si ferma e chiede «Deploy now?». Dall'orologio basta un tocco su «1 · yes»
   e il lavoro riparte. Convince perché fa vedere il giro completo senza tirare fuori il telefono.
   Schermata: orologio, domanda.
2. **Il colpo d'occhio sul telefono.** La home divide le sessioni in «Ti aspetta», «Al lavoro» e «Ferma». Per ognuna mostra
   l'ultimo esito, l'obiettivo, il contesto e la quota, e la domanda aperta ha già i tasti per rispondere. Convince perché lo
   stato di tutto il lavoro si legge in una schermata.
   Schermata: home del telefono.
3. **Rispondere con parole tue.** Nella sessione la domanda ha le opzioni e un campo per una risposta libera; ▶ la legge ad
   alta voce. Convince perché non costringe a scegliere fra risposte già pronte.
   Schermata: sessione con la domanda.
4. **Il tablet come plancia.** La home a sinistra e fino a 4 sessioni in colonna accanto, ognuna con la sua conversazione.
   Senza colonne aperte il tablet mostra sessioni, Recap e Utilizzo affiancati. Convince perché è il PC di regia senza il PC.
   Schermate: plancia con le colonne; plancia con Recap e Utilizzo.
5. **Live.** Il telefono racconta a voce cosa fanno le sessioni; al polso arriva l'esito, con le mosse successive da
   toccare. Convince perché si segue il lavoro mentre si fa altro.
   Schermata: orologio, live con l'esito.
6. **La notte.** Si lascia un lavoro per stanotte. La mattina il resoconto dice cosa è successo, sessione per sessione
   (commit, test, prompt), e cosa aspetta te, con i tasti per rispondere e approvare. Convince perché il PC lavora anche
   quando dormi e la mattina sai da dove ripartire.
   Schermata: resoconto della notte.
7. **Recap e Utilizzo.** Il recap del giorno propone le azioni da far partire e l'agenda divisa fra quello che aspetta te e
   quello che può fare Claude, ognuna col suo menu «Azioni». Utilizzo mostra la quota di 5 ore con la previsione al ritmo di
   adesso e la settimana. Convince perché aiuta a decidere cosa fare dopo, non solo a guardare.
   Schermate: pagina Recap; plancia con Recap e Utilizzo.

## 3. Le schermate

Golden di Paparazzi, rigenerati dalla CI a ogni modifica. Contengono solo dati di prova: i progetti ledger-api, atlas-shop,
field-notes e orbit-docs, e gli account «personal» e «work». Non ci sono nomi di clienti.

| # | Momento | File |
|---|---|---|
| 1 | Domanda al polso | `wear/src/test/snapshots/images/it.pixelbox.cmwatch.wear_ScreensSnapshotTest_question.png` |
| 2 | Sessioni al polso | `wear/src/test/snapshots/images/it.pixelbox.cmwatch.wear_ScreensSnapshotTest_sessions.png` |
| 3 | Live al polso | `wear/src/test/snapshots/images/it.pixelbox.cmwatch.wear_LiveScreenSnapshotTest_liveOutcome.png` |
| 4 | Home del telefono | `mobile/src/test/snapshots/images/it.pixelbox.cmwatch.mobile_SummaryListTest_summaryOpen.png` |
| 5 | Sessione con domanda | `mobile/src/test/snapshots/images/it.pixelbox.cmwatch.mobile_SessionSheetTest_sheetQuestion.png` |
| 6 | La master a tutto schermo | `mobile/src/test/snapshots/images/it.pixelbox.cmwatch.mobile_SessionSheetTest_masterScreenPhone.png` |
| 7 | Plancia con le colonne | `mobile/src/test/snapshots/images/it.pixelbox.cmwatch.mobile_TabletShellTest_tabletDesk.png` |
| 8 | Plancia con Recap e Utilizzo | `mobile/src/test/snapshots/images/it.pixelbox.cmwatch.mobile_TabletShellTest_tabletDeskEmpty.png` |
| 9 | Resoconto della notte | `mobile/src/test/snapshots/images/it.pixelbox.cmwatch.mobile_NightScreenTest_anOpenCard.png` |
| 10 | Pagina Recap | `mobile/src/test/snapshots/images/it.pixelbox.cmwatch.mobile_RecapScreenTest_recapPage.png` |

Da sapere prima di montare:
- **Risoluzione.** Paparazzi salva al massimo 1000 px sul lato lungo: telefono 461×1000, tablet 1000×625, orologio 456×456.
  Per inquadrature grandi servono rese apposite.
- **Lingua.** Le schermate dell'orologio sono in inglese («Sessions», «low priority»), quelle di telefono e tablet in
  italiano. I testi delle sessioni sono in inglese in tutte.
- **Numeri nelle schermate.** Valori come «48 passed», «9 %» o «ctx 62 %» sono dati di prova. Il film non li presenta
  come risultati.

## 4. Cosa non dire e non mostrare

**Cose non vere:**
- Non è un prodotto di Anthropic: usa Claude Code, ma l'app e il relay sono di Franz.
- L'app non esegue Claude. Le sessioni girano sul PC; telefono, tablet, orologio e web le guardano e le comandano.
  A PC spento non lavora niente.
- Solo Android e Wear OS, più la web nel browser. Niente iPhone, iPad o Apple Watch.
- L'app non ha un microfono suo e non chiede il permesso del microfono. Si scrive, o si detta con la dettatura del
  sistema (la tastiera del telefono, l'input vocale dell'orologio). La voce dell'app è in uscita: la lettura ad alta voce.
- Cifratura: si può dire che stato, comandi e risposte viaggiano cifrati con AES-256-GCM fra PC e dispositivi. Non si
  dice «nessuno vede niente» né «zero knowledge»: le notifiche push non sono state verificate per questo.
- Nessuna misura di tempo («in due secondi», «risparmi un'ora»): nessuna è stata misurata.

**Ancora da provare dal vivo:**
- Le schede del Recap mandate alla notte con «Stanotte», fatte dalla master: il relay 0.8.4 sta uscendo stasera.
- Gli allegati fino a 50 MB, attivi dal relay 0.8.3 di stasera.

**Dati privati:**
- Non usare le foto dei dispositivi veri (`docs/segnalazioni/`, `docs/screenshots/`): contengono nomi di progetti e clienti
  veri e il nome dell'account nella barra di stato.
- Nessuna ripresa dell'agenda vera (`.claude/agenda.tsv`: siti di clienti), dei QR di accoppiamento, dell'indirizzo del
  progetto Firebase, di chiavi o token.

## 5. Numeri che si possono dire

| Numero | Cosa | Dove si verifica |
|---|---|---|
| 4 | sessioni affiancate al massimo sul tablet | `Tablet.MAX_COLUMNS` in `core/src/main/kotlin/it/pixelbox/cmwatch/rules/Tablet.kt` |
| 50 MB | il file più grande che si manda dal telefono | contratto 1.48, `max_parts_bytes` = 52428800 nello stato del relay |
| 8 | lavori al massimo nella coda di una notte | contratto 1.17 (`contract/README.md`), `night.max_queued` |
| 158 | schermate controllate a ogni modifica (119 telefono e tablet, 39 orologio) | `mobile/src/test/snapshots/images`, `wear/src/test/snapshots/images` |
| 1035 + 418 | test automatici del nucleo e della web al 09/10 | `./gradlew :core:testDebugUnitTest`, `npx vitest run` in `web/` |
