# Turno di notte 2026-10-07 — claude-master-phone-notte

- id: `9507ba25` · account: personale · modello: - · effort: -
- max turni: 150 · durata: 1841 s · esito: rc=0
- aggiunto: 2026-10-07T02:03:11

## Prompt

Seconda esecuzione della voce d17d7b00. Alle 02:01 la prima si è fermata perché le fonti stavano fuori dal worktree e i permessi ne negavano la lettura. Ora la master le ha copiate in docs/proposte/, esclusa da git: non committarla.

Lavoro notturno non presidiato del 06-07/10/2026, chiesto da Franz: implementare il nucleo della modalità live nel modulo core. Stanotte nessuno risponde alle domande: le scelte di routine le fai tu e le scrivi nel rapporto finale.

Fonti, da leggere per prime:
- la specifica docs/proposte/2026-10-06-modalita-live-design.md (copia nel worktree), approvata da Franz il 06/10 alle 21:50 («realizziamo»);
- la storia delle decisioni in docs/proposte/2026-10-06-modalita-live-bozza.md (copia nel worktree).

Sei in un worktree del repo claude-master-phone, sul ramo notte/live-core. La sessione interattiva claude-master-phone lavora nell'altra cartella (personali/claude-master-phone): non toccarla.

Compiti, in ordine:
1. Scrivi il piano di implementazione delle sole regole pure del modulo core (fasi 1 e 2 della specifica) in docs/plans/2026-10-07-live-core.md: file, test, ordine.
2. Implementa in core, scrivendo prima i test e poi il codice:
   - SpeakableName (§5);
   - LiveFeed.plan (§5): livelli, raggruppamento, attese di 3 minuti con orologio iniettato, «Dopo» e «Salta», giro completo, frasi;
   - LiveRoute.parse (§6), senza la parte di invio;
   - DoubleConfirm (§7): la macchina a stati, con orologio iniettato.
   Dove servono, usa le fixture del contratto in contract/ (state-1-question.json, state-2-idle.json, events-sample.json). Segui lo stile dei file vicini, come Wake.kt e SpeechText.kt: commenti in italiano, nomi in inglese.
3. Limiti: niente interfaccia, niente servizio Android, niente modifiche al contratto, niente dipendenze nuove.
4. Verifica: ./gradlew :core:test deve essere verde. Se un test non passa dopo 3 tentativi, fermati e scrivi perché.
5. Fai commit piccoli e frequenti sul ramo notte/live-core, con messaggi in inglese (feat(core): …, test(core): …). Il limite è di un'ora: i commit frequenti salvano il lavoro se il tempo finisce. Niente push. I file in docs/ restano fuori da git, perché il repo è pubblico.
6. Alla fine scrivi docs/plans/2026-10-07-live-core-esito.md con: cosa è fatto e cosa no, le decisioni prese da solo, i comandi di verifica con il loro esito.

Aggiunta della master, 07/10 alle 00:50. Regola di Franz per il lavoro notturno: arrivare il più vicino possibile alla conclusione completa e alla pubblicazione. I limiti del punto 3 restano. Quindi, finiti i compiti, porta il ramo a essere pronto da pubblicare: allineato all'ultimo main, `./gradlew :core:test` verde, e nell'esito i comandi esatti per il merge e la pubblicazione. Il push resta vietato: lo fa la master dopo la verifica.

## Output

Fatto: le quattro regole della modalità live sono in `core` con i loro test, in 5 commit su `notte/live-core`. `./gradlew :core:test` è verde sull'ultimo commit `ab2ba54`: 903 test in debug e 903 in release, nessun fallimento. Il ramo però non è riallineato: `git merge` e `git rebase` chiedono un'approvazione che stanotte nessuno poteva dare. I comandi sono nell'esito.

- **`fbc3b25` `SpeakableName`:** toglie `pix-` e il dominio finale, i trattini diventano spazi, il nome non resta mai vuoto.
- **`b8290be` `DoubleConfirm`:** serve un tasto diverso tenuto 2 s, entro 10 s dal primo tocco. Si annulla con Annulla, alla scadenza, se la richiesta sparisce o se il relay rifiuta.
- **`184ec54` `LiveFeed`:** livelli, una notizia per sessione, attesa di 3 minuti, «Dopo», «Salta», modalità «solo bloccanti», frasi e giro completo. I test usano le tre fixture del contratto.
- **`c3af9ee` `LiveRoute.parse`:** riconosce istruzione, domanda sullo stato, ok o domanda per la master. Il nome si cerca per uguaglianza, poi prefisso, poi contenuto. Niente invio.
- **`ab2ba54` correzione:** l'ho trovata rileggendo. Una sessione in bassa priorità che sparisce dalla lista non dà più la notizia di chiusura. Prima ho scritto il test che falliva, poi la correzione.

Tre scelte fatte da solo da guardare (le 13 decisioni sono nell'esito):
1. **Bassa priorità:** la specifica la mette al livello 3 («si sente solo nel giro completo») e poi la esclude dal giro. Ho escluso dal giro solo le sue notizie di routine; domande e Prossimi con «!» entrano sempre.
2. **Richiesta di ok:** la frase dice «Richiesta di ok: …» senza «da *nome*», perché il contratto 1.37 non dice quale sessione la chiede.
3. **`launched` e `recap`:** non entrano in coda, come vuole la specifica, ma non si sentono neanche nel giro completo.

Piano in `docs/plans/2026-10-07-live-core.md`, esito in `docs/plans/2026-10-07-live-core-esito.md`. Tutti e due restano fuori da git, come `docs/proposte/`. Niente push.

Da te:
1. **Master:** riallineare, verificare e pubblicare con i comandi qui sotto. Uso `feature/tablet` come «main» perché `origin/master` non contiene l'app del telefono. Nessuno degli 8 file nuovi è fra quelli che `feature/tablet` ha toccato dopo la base.
   ```bash
   cd ~/Desktop/workspaces/personali/claude-master-phone-notte && git rebase feature/tablet && ./gradlew :core:test
   cd ~/Desktop/workspaces/personali/claude-master-phone && git merge --ff-only notte/live-core && git push origin feature/tablet
   ```
   Il merge va fatto con la sessione interattiva ferma.
2. **Franz:** decidere sulla scelta 1 e la 3, e se chiedere a claude-master il nome della sessione negli ok.

Se vuoi, lo deposito come esame in model-bench.

Esito: nucleo live in `core` fatto e verificato, 5 commit su `notte/live-core`; riallineamento e push restano alla master.
Prossimi: !Rebase e merge come da esito · Decidi livello 3 nel giro completo · Chiedi il nome negli ok (contratto)
