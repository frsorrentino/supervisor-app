# Pagina «Notte»: Gantt per progetto e per obiettivo (21/10/2026)

Esito: fixture del contratto 1.74 su `origin/notte/gantt` alle 12:23, così R0 del relay passa. Gantt A e B con lo switch «Progetti» / «Obiettivi» e le notti precedenti nella pagina Notte del telefono e del tablet, sul ramo `notte/gantt` (pushato). L'APK arriva insieme alla release di supervisor con il rapporto v2.

## Fixture 1.74 (prima di tutto, per R0)

I file, in `contract/`, sono byte per byte uguali a `tests/fixtures/relay/` di supervisor sul ramo `fix/notte-riepilogo` (54e9cad):

- `night-report-sample.json`;
- `cmd-result-sample.json`;
- `state-1-question.json`;
- `state-2-idle.json`;
- `state-3-stale.json`.

Commit `8a77be10`, con la voce 1.74 in `contract/README.md` (`3c02f2b5`). R0 confronta con il ramo remoto che ha l'ultimo commit su `contract/`, cioè `origin/notte/gantt`. La master è stata avvisata alle 12:23.

**In più:** `feature/tablet` era rosso in CI dal 18/10. Il Kotlin non conosceva l'op `studio` delle fixture 1.73, e ogni test che decodifica i comandi di `cmd-result-sample.json` falliva. Ora `CmdOp` ha `STUDIO` (l'app non lo manda) e `NIGHTS`. Ho aggiornato anche il conteggio dei risultati (73) e il prompt web, che non è più unico.

## Cosa c'è nell'app

- **Core.**
  - `NightReport` legge la v2: `title`, `end_source`, `projects[].spans`, `goals`, `milestones`, `where`.
  - `ContractJson.decodeNights` legge l'op `nights`; `NIGHTS` è una lettura passiva.
  - La demo risponde a `nights` con la notte della fixture.
  - `rules/NightGantt` calcola tutto il Gantt: l'asse dall'ora intera prima all'ora intera dopo, le posizioni, le larghezze (con una larghezza minima), gli stati, i totali e l'ordine dei progetti, dal primo partito.
- **Pagina Notte, per un rapporto v2.**
  - In testa le pillole «dove è arrivato» (arrivati, in corso, in parte, fermi).
  - Poi il segmented button di Material 3. Parte da «Progetti» e ricorda l'ultima scelta (`night_by_goal`).
  - **A.** Barre = intervalli di lavoro; rombi = push e release; linea tratteggiata = «ora» (rapporto fatto di notte) o «fine» (primo messaggio del mattino). Accanto al nome i punti fatti sul totale, sotto «dove è arrivato ogni progetto».
  - **B.** In testa «3 su 4» con la barra divisa per stato. Poi, per progetto, **un solo segmento per obiettivo**: pieno fino al FINE se fatto, a righe se in corso, tratteggiato se in parte o fermo, nessuna barra con «non fatto». I punti sono elencati dentro, con il segno dello stato e senza ora: il relay non la conosce. Il titolo dell'obiettivo ha una riga sua, perché tagliato a 116 dp si leggeva «Registro dei».
  - Sotto restano «Da fare per te» e «Cosa è successo» (chiuso quando c'è il Gantt). «Progetti · parti» c'è solo per i rapporti v1, perché lo sostituisce la vista A.
  - In fondo le notti precedenti, dall'op `nights`, chiesta solo se lo stato la elenca in `ops`. Toccandone una, il suo rapporto (`night` con la data) prende il posto di quello mostrato.
- **Stati non solo col colore:** ogni stato ha l'icona (✓ ▶ ◐ ✗ ○), una luminosità sua e, nella vista B, il tratto (pieno, a righe, tratteggiato). Testi in `strings.xml`, anche in `values-en`.
- Nella riga «dove è arrivato» il prefisso del relay («domanda:», «!:») non c'è: lo dice già la pillola. «/clear» diventa la frase dell'app.

## Prove

- Test core su win: 1163 test. Falliscono solo i 5 `ChatLogTest` noti, che passano in CI su Linux. I nuovi sono `NightGanttTest` (14 test: asse, linea, ordine, barre, rombi, punti, stati, totali, segmenti, obiettivo non partito, stato sconosciuto) e `NightReportTest` (v2, v1 senza campi, op `nights`).
- `NightPageTest` e `web/src/lib/night.test.ts` seguono la timeline v2, con `win-orbit-docs` e l'ordine nuovo.
- vitest su win: 598 su 599. Fallisce `zip.test.ts`, «nota è.md» letto da `unzip` di Windows: è la codifica della macchina, non tocca la notte.
- Paparazzi: ho registrato e guardato su win `ganttByProject` e `ganttByGoal`, poi ho corretto l'interlinea in A e i titoli in B. I golden veri vengono dalla CI (run di registrazione 39584655266): li ho committati in `a233e146`, insieme a quelli della pagina Notte e del riepilogo cambiati dalla fixture v2.
- **CI verde** sul ramo (run 39586223317, finito alle 12:46): test JVM, verifica Paparazzi del telefono e dell'orologio, APK.

## Da sapere

- **La web non ha il Gantt.** Ho aggiornato solo i suoi test sulla fixture. La proposta parla dell'app; se Franz lo vuole anche nella PWA, è un lavoro a parte.
- Nella lista «Cosa è successo» una sessione con esito `unknown` (nella fixture `win-orbit-docs`) compare come «✗ fermo», per la regola del 07/10 «altri valori si mostrano come fermo». Il Gantt dice invece «◐ in parte», che è il `where` del progetto. Da decidere se `unknown` debba avere un segno suo.
- Sul telefono il Gantt si vede solo con un rapporto v2. Il file vero del 21/10 è ancora v1, quindi va rigenerato dopo la release di supervisor (punto 3 del resoconto di `fix/notte-riepilogo`).

FINE
