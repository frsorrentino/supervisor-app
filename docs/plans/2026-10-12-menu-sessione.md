# Menu ⋮ della sessione ampliato

Richiesta di Franz del 12/10, riferita dalla master. Alle 22:12 ha chiesto come ampliare il menu della sessione; alle
22:17 ha scelto le voci 1, 3 e 5, alle 22:19 ha aggiunto la 2 («Aggiungi anche Riavvia»). Mockup in
`docs/mockup/2026-10-12-menu-sessione/`, disegnati con i componenti dell'app sul suo telefono (448 dp, carattere 1,3).

## Il menu

Dall'alto: **File prodotti**, **Stato del progetto**, Segui, Fai controllare alla master, **Handoff, poi /clear**,
**Riavvia**, Apri in Claude; in fondo, separata e in rosso, Chiudi la sessione. Le due viste in sola lettura stanno in
cima perché si usano più spesso. Le due voci che fermano la sessione stanno vicino a Chiudi. Una voce nuova compare solo
se il PC annuncia la sua operazione in `state.ops`, come Stop.

- **Voce 1, Handoff, poi /clear**: sempre nel menu, non solo dal foglio del contesto. È lo stesso flusso del contratto 1.37:
   il prompt dell'handoff, poi `/clear` a turno finito. Al relay non serve niente.
- **Voce 2, Riavvia**: un dialogo che fa anche da conferma, come Chiudi la sessione. Dentro ci sono il nome, lo stato
   («sta lavorando»: il turno in corso si interrompe), Riprendi (stessa conversazione), Da zero (prima l'handoff, poi una
   conversazione nuova) e Annulla.
- **Voce 3, File prodotti**: un foglio dal basso. Mostra gli ultimi file che la sessione ha creato o modificato, i più recenti
   prima, con cartella, misura e ora. Si toccano per aprirli, e la freccia li salva nei Download, come i file della chat
   (`FileCache`, anteprime e azioni riusate).
- **Voce 5, Stato del progetto**: un foglio dal basso in sola lettura. Mostra il ramo con i commit da pubblicare, i file
   cambiati con la lettera di git, i commit locali non ancora su origin e l'esito dell'ultimo test. Si rilegge a ogni
   apertura.

## Dal relay: una richiesta, contratto 1.51

La richiesta va alla sessione supervisor, che oggi è chiusa: la legge dalla sua casella.

- `restart`, operazione nuova. `session` è il nome; `arg` è `resume` (il PC fa `supervisor restart NOME`, stessa
  conversazione) o `clean` (`supervisor restart NOME --clean`). La sessione passa da gone a idle come con `reopen`; il
  risultato del comando dice se è ripartita. «Da zero» dall'app manda prima il prompt dell'handoff e a turno finito
  `restart clean`, come oggi «Handoff, poi /clear» manda `/clear`.
- `files`, lettura nuova. `session` è il nome. Restituisce fino a 20 file creati o modificati dalla sessione: quelli
  scritti dagli strumenti Write, Edit e NotebookEdit e quelli mandati con SendUserFile. Per ogni file `path`, `size`,
  `mtime` e `mime`, i più recenti prima, solo quelli che esistono ancora. `file` (1.24) accetta anche questi percorsi,
  per aprirli e salvarli.
- `project`, lettura nuova. `session` è il nome. Restituisce `branch`, `upstream`, `ahead` e `behind`; `changed` come
  coppie `{path, status}`, al massimo 20, con il totale; `unpushed` come coppie `{sha, subject}`, al massimo 10; `test`
  come `{cmd, ok, passed, failed, at}` oppure null. `test` è l'ultimo comando di test della conversazione (npm test,
  gradlew test, pytest, cargo test) con l'esito letto dal suo output. Se la cartella non è un repo git, `branch` è null.

Fixture nuove in `contract/` per i tre risultati, come `cmd-result-sample.json`.

## App

- Le voci in `SheetHeader` (`SessionSheet.kt`), che è anche la testata delle colonne del tablet. Testi in `strings.xml`
  in italiano e in inglese.
- `FilesSheet` e `ProjectSheet` come fogli dal basso (`ModalBottomSheet`), come nei mockup. Riavvia è un dialogo accanto
  a `CloseDialog`.
- Test: unit sulla lettura dei tre risultati dalle fixture; Paparazzi del menu, dei due fogli e del dialogo.

## Ordine

1. Ok di Franz su piano e mockup.
2. La voce 1 subito, perché al relay non serve niente.
3. La richiesta di contratto alla sessione supervisor, una sola per le tre operazioni.
4. Le voci 2, 3 e 5 quando il relay alza la versione e le annuncia in `state.ops`.

La web ha un menu ⋮ simile in `Header.svelte`: le stesse voci lì, solo se Franz lo chiede.
