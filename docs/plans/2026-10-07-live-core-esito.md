# Nucleo live in core — esito (notte 06-07/10/2026, seconda esecuzione)

Stato alle 03:48 del 07/10: **i quattro pezzi sono fatti, con test e 5 commit su `notte/live-core`.** Verifica
finale in fondo. Il ramo **non è riallineato**: `git merge` e `git rebase` chiedono un'approvazione che di notte nessuno può
dare. I comandi per allinearlo sono più sotto.

Piano: `docs/plans/2026-10-07-live-core.md`. Fonti: `docs/proposte/2026-10-06-modalita-live-design.md` (§5-§7, §9) e la
bozza nella stessa cartella.

## Fatto

| Commit | Pezzo | Test |
|---|---|---|
| `fbc3b25` | `rules/SpeakableName.kt` — nomi pronunciabili (§5) | `SpeakableNameTest`, 5 |
| `b8290be` | `rules/DoubleConfirm.kt` — macchina a stati con orologio iniettato in ms (§7) | `DoubleConfirmTest`, 12 |
| `184ec54` | `rules/LiveFeed.kt` — `plan`, `next`, `spoken`, `later`, `skip`, `round`, frasi (§5) | `LiveFeedTest`, 23 |
| `c3af9ee` | `rules/LiveRoute.kt` — `parse` → `Route` (§6), senza invio | `LiveRouteTest`, 8 tabelle |
| `ab2ba54` | correzione in `LiveFeed`: una sessione in bassa priorità che sparisce dalla lista non dà la notizia di chiusura, come quando passa a `gone` (trovata rileggendo) | un caso in più in `LiveFeedTest` |

I test usano `state-1-question.json`, `state-2-idle.json` ed `events-sample.json` attraverso `Fixtures`. Nessuna
dipendenza nuova, nessuna modifica al contratto, all'interfaccia o ai servizi.

## Non fatto

- **Allineamento del ramo:** vedi «Allineamento e pubblicazione».
- **Livello 3 nel giro completo:** `launched` e `recap` non entrano in coda (come dice la specifica) ma non si sentono
  neanche nel giro completo, perché per farlo il `Feed` dovrebbe conservarli. Va deciso con Franz insieme alla
  contraddizione al punto 3 delle decisioni.
- **Fase 3 e invio dell'ok:** la lettura della risposta della master (`last`, livello 1) e il comando `approve` con
  `via`/`confirmations` chiedono il contratto 1.42, fuori dai limiti di stanotte. `DoubleConfirm` arriva a
  `confirming`: il comando lo costruisce il servizio.
- **Le frasi in `strings.xml`:** le regole ricevono le frasi in `LiveFeed.Labels`, come `SpeechText.question` riceve
  il formato dell'opzione. Le stringhe italiane per ora stanno solo nel test (`LiveFeedTest.IT`); in `mobile`
  arriveranno con `LiveService`.

## Decisioni prese da solo

1. **Unità di tempo:** `LiveFeed` in secondi epoch come il contratto; `DoubleConfirm` in millisecondi (`nowMs`),
   perché misura una pressione di 2 secondi.
2. **La coda è un valore:** `LiveFeed.plan(feed, prev, cur, events, now, labels, zone)` riceve e restituisce il `Feed`.
   La firma della specifica (`prev, cur, events, now`) non basta per «Dopo», «Salta», `answered` e l'attesa di 3
   minuti. «Ripeti» non tocca la coda.
3. **Bassa priorità attiva:** §5 la mette al livello 3 («si sente solo nel giro completo») e poi la esclude dal giro
   completo. Ho scelto così: le sue notizie di routine (esiti senza «!», chiusure) non entrano in coda e non compaiono
   nel giro; quello che serve a Franz (domanda, Prossimo con «!») entra sempre, in coda e nel giro, perché il livello 1
   «non ha limiti».
4. **Primo stato della live** (`prev` null): si annuncia solo il livello 1 aperto (domande, «!», ok, quota), non gli
   esiti vecchi di tutte le sessioni.
5. **Domanda, esito e chiusura** si leggono dallo stato, che ha sempre il più recente; degli eventi contano `answered`
   (toglie la domanda anche prima dello stato nuovo) e `restart_failed`. Gli eventi `quota` sono ignorati: la quota si
   legge dallo stato.
6. **Chiavi della coda:** `s:<sessione>` per domanda, esito e chiusura (una notizia per sessione); `restart:<sessione>`
   a parte, perché un esito successivo non deve cancellare un riavvio fallito (livello 1).
7. **«Dopo» e aggiornamenti:** se la sessione cambia mentre la notizia aspetta i 5 minuti, vale la notizia nuova, subito
   (resta l'attesa di 3 minuti per il livello 2).
8. **Quota:** soglia `>= 95`, la stessa di `Wake.plan` e dell'avviso del relay («sopra il 95%» letto così). Si legge la
   finestra più piena; l'ora è quella del suo azzeramento nel fuso passato dal chiamante. La notizia esce dalla coda
   se la quota scende.
9. **Richiesta di ok senza nome:** il contratto 1.37 non dice quale sessione chiede l'ok, quindi la frase è «Richiesta
   di ok: *title*, *what*, su *where*. Serve la doppia conferma.». Per avere «da *nome*» serve un campo nel contratto
   (richiesta a claude-master, non fatta).
10. **Domanda oltre 300 caratteri:** il limite vale sulla frase intera (nome, testo, opzioni); si taglia all'ultima fine
    di frase entro 300, altrimenti all'ultimo spazio, e si aggiunge «Continua sul watch.». Mai «…».
11. **Nomi pronunciabili:** separano le parole anche `_` e `.` (una cartella `shop.example.it` direbbe «punto»); il
    nome non resta mai vuoto (`pix-it` → «it»).
12. **Smistamento:** «ad» accettato accanto ad «a» («chiedi ad atlas»); prefisso e contenuto solo con parole di almeno
    3 lettere («la» troverebbe «atlas shop»); le istruzioni vanno solo a sessioni non chiuse, lo stato si chiede di
    tutte; se dopo «come va» non c'è una sessione riconosciuta, la regola dello stato non si applica e la frase va alla
    master («come va la quota?»). Per l'ok si toglie l'articolo («approva la release 2.4») e «approva» da solo prende
    l'unica richiesta aperta.
13. **Doppia conferma:** la scadenza dei 10 secondi è stretta: conta l'istante in cui la pressione arriva a 2 secondi,
    che deve cadere entro la scadenza (un tick in ritardo non annulla una conferma già completa). La richiesta sparita
    annulla solo in `armed`: in `confirming` sparisce proprio perché l'ok è arrivato. Un rifiuto del relay porta in
    `cancelled` (`Why.REFUSED`), perché la specifica non ha uno stato d'errore.

## Verifica

- Esecuzione di partenza (`./gradlew :core:testDebugUnitTest`, appena dopo le 03:17): fallita alla compilazione dei
  test perché il primo file di test nuovo era già sul disco (`Unresolved reference 'SpeakableName'`), non per un
  difetto del ramo.
- Rosso, con segnaposto al posto del codice:
  `./gradlew :core:testDebugUnitTest --tests '*SpeakableNameTest' --tests '*LiveFeedTest' --tests '*LiveRouteTest' --tests '*DoubleConfirmTest'`
  → `48 tests completed, 41 failed`. I 7 verdi erano controlli negativi che col segnaposto passavano a vuoto: ho
  aggiunto le loro precondizioni prima di scrivere il codice.
- Verde per pezzo:
  - `--tests '*SpeakableNameTest' --tests '*DoubleConfirmTest'` → `BUILD SUCCESSFUL`;
  - `--tests '*LiveFeedTest'` → 23 test, 0 errori;
  - `--tests '*LiveRouteTest'` → 8 test, 0 errori.
- Suite completa su `c3af9ee`: `./gradlew :core:test` → `BUILD SUCCESSFUL in 5m 10s`; 903 test in debug e 903 in
  release (48 nuovi per variante), nessun file di risultato con fallimenti.
- Correzione `ab2ba54`: `--tests '*LiveFeedTest'` rosso prima (`23 tests completed, 1 failed`,
  `AssertionError at LiveFeedTest.kt:217`), verde dopo (23 test, 0 errori).
- Suite completa sulla punta `ab2ba54` (03:47): `./gradlew :core:test` → `BUILD SUCCESSFUL in 1m 46s`, uscita 0; 903
  test in debug e 903 in release, nessun file di risultato con fallimenti o errori
  (`grep -L 'failures="0"'` e `grep -L 'errors="0"'` su `core/build/test-results/test{Debug,Release}UnitTest/` →
  nessun file).
- Ramo: `git status --short` pulito, a parte i file in `docs/` che restano fuori da git.

## Allineamento e pubblicazione

Il ramo nasce da `d97a253`. Il lavoro dell'app vive su `feature/tablet` (la sessione interattiva), che dopo `d97a253`
ha 9 commit fino a `2983527`; `origin/master` non contiene l'app del telefono (279 commit dietro), quindi «ultimo main»
qui vuol dire `feature/tablet`. I miei 8 file sono tutti nuovi, e nessuno è fra quelli toccati da `feature/tablet`
dopo la base: il riallineamento non ha conflitti di file. Fra le fixture, `feature/tablet` cambia solo il testo del
`restart_failed` in `events-sample.json`, che i test non leggono (usano `ref`).

Comandi per la master, nell'ordine:

```bash
cd ~/Desktop/workspaces/personali/claude-master-phone-notte
git rebase feature/tablet
./gradlew :core:test
cd ~/Desktop/workspaces/personali/claude-master-phone
git merge --ff-only notte/live-core
git push origin feature/tablet
```

Se `feature/tablet` è avanzato ancora, il `rebase` lo prende. Il `merge` va fatto nella cartella della sessione
interattiva, dove `feature/tablet` è aperto: meglio a sessione ferma. Il push va solo su `origin`, mai su `archivio`.
Dopo il merge, il ramo `notte/live-core` e questo worktree si possono togliere (`git worktree remove`
`../claude-master-phone-notte`, poi `git branch -d notte/live-core`).
