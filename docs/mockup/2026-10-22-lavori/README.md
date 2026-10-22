# Mockup 22/10/2026: la pagina unica «Lavori»

Ok di Franz dal telefono alle 07:47 («ok al mockup, non al codice»), compito `mockup-lavori-22-10`. Niente codice dell'app finché Franz non approva il mockup.

File: `genera.py` (sorgente: CSS, testi e dati), `lavori-telefono.png` (la pagina intera a 412 px; la riga tratteggiata è la fine del primo schermo), `home.png`, `menu.png`, `lavori-tablet.png` (il cruscotto, stesso contenuto in due colonne) e gli `.html` da cui nascono. Dati veri del 22/10 alle 08:00.

## Perché una pagina sola

Oggi tre pagine rispondono alla stessa domanda, «cosa resta da fare e chi lo deve fare», da tre fonti diverse: Registro (coda di stanotte, la notte, i giorni, la quota), Recap (azioni, chi deve muoversi, fatto e sospeso, dall'agenda) e la sezione Impegni della home (il registro dei compiti). «Stanotte» sta in due posti, il fatto in due posti, «chi deve muoversi» viene da due fonti, la quota anche in «Utilizzo e limiti». La pagina nuova è ordinata per domanda, non per fonte, e ogni blocco si spiega da solo in una riga.

## I quattro blocchi

1. **Per te**: solo quello che aspetta Franz, ognuno col suo tasto. Tre gruppi: «Ok da dare» (Approva), «Da fare fuori dalla chat» (Fatto), «Può farlo Claude» (Avvia). Il resto in «Altre N».
2. **In corso**: un compito per riga, con cosa, chi, da quando e il prossimo passo; sotto, la barra del mini Gantt della Notte su sette giorni (piena = al lavoro, tratteggio = in pausa, banda viola = la notte, riga bianca = adesso). Assorbe la decisione aperta «impegni nel Gantt». I compiti in pausa e quelli fermi su terzi stanno in fondo, richiusi.
3. **Stanotte**: la coda della notte, solo qui, con «Aggiungi»; una riga porta al rapporto della notte scorsa nella pagina Notte.
4. **Fatto**: per giorno, oggi aperto e i giorni prima chiusi, ogni riga con la prova accanto (release, commit, file del resoconto).

La quota non è nella pagina: resta in «Utilizzo e limiti».

## Dove finiscono Registro, Recap e Impegni

- Menu ≡: la voce **Lavori** («Per te, in corso, stanotte, fatto») sostituisce **Registro** e **Recap**. **Notte** resta: il rapporto di stanotte con il Gantt e le notti prima; la coda di stanotte si gestisce da Lavori. **Utilizzo e limiti** tiene la quota, che prima stava anche nel Registro.
- Home: le sezioni **Da approvare**, **Impegni** e **Recap** diventano una sola sezione **Lavori** con solo le righe che hanno un tasto e i conteggi; «Tutti i lavori» apre la pagina. Notte, Sessioni e Utilizzo restano come sono.
- Cerca: cerca anche nei lavori (prima «esiti e registro»).

## Dati veri usati

- Registro (`supervisor task impegni --json`, 07:50): 7 impegni aperti: `mockup-lavori-22-10` al lavoro; i cinque di Albo Chiaro (ok del 17/10 alle 22:05) e `phone-fix-0310-sera` (03/10) in pausa; la pausa di Albo aspetta da Franz la casella Aruba `albo@`.
- Ok da dare (`state.attention.approvals` del resoconto della notte): `notte-cc2-1-307-fable-22-10`, chiesto dalla master alle 05:53.
- Agenda (`~/Desktop/workspaces/.claude/agenda.tsv`): 30 righe aperte, 17 bloccano su Franz, 10 su Claude, 3 su terzi; 1 sospesa (`dev.pinsacesare.it`).
- Notte (`~/.cc-supervisor/night-report/2026-10-22.json`): «5 progetti: 1 fatti, 3 in parte, 1 fermi», finestra 00:25–06:30, 4 sessioni e 2 lavori della coda; coda di stanotte vuota.
- Fatto oggi: `impegni-app-22-10` chiuso alle 07:45 con il controllo verde (release `impegni-2026-10-22-b` installata sul telefono); Bluey chiuso in automatico alle 06:40 (`origin/main 1c04bb2`); i due lavori della notte (claude-observe 02:29, fable-director 02:17, resoconti in `docs/notte/`); relay 0.8.41 (01:29) e 0.8.40 (00:19); piano SDK `b0129a75` (00:52).
- I giorni prima sono richiusi senza conteggio: il registro oggi non espone i compiti chiusi per giorno (vedi sotto).

## Cosa cambia nei dati (nota, niente codice)

- **Fonte unica: il registro dei compiti.** Le righe dell'agenda diventano compiti: `blocca = franz` → compito «per te» con il tasto Fatto; `blocca = claude` → compito proposto, «Avvia» è l'ok di Franz che lo fa partire; `blocca = terzi` → compito in pausa «aspetta terzi». Le righe «prossimo» dei recap entrano nel registro allo stesso modo. Finché l'agenda vive, i tasti restano `agenda_set` (done, pass) e il registro la legge.
- **Una pausa che aspetta Franz** (il motivo della pausa, come per Albo Chiaro) è una riga «per te» con Fatto, che riprende il compito (`task resume`).
- **Ok da dare** = `state.approvals` più gli impegni `awaiting_ok` del contratto 1.76: niente di nuovo.
- **In corso** = `state.commitments` (1.76) con `approved_at`, `last_step`, `idle_s`, `paused`: bastano per la barra a sette giorni. Per «fermi su terzi» serve che l'agenda sia nel registro, oppure `waits.who = "third"`.
- **Stanotte** = `state.night` con `night_add` e `night_remove`: niente di nuovo.
- **Fatto** ha bisogno di una cosa nuova: i compiti chiusi per giorno con la prova (`results[].proof`: controllo, commit, release, file). Oggi l'app riceve solo gli eventi `recap` e `night_report`. Richiesta al relay, dopo l'ok di Franz: un'op a richiesta `done` (giorno → `{task, title, session, at, proof{kind, value, url}}`), oppure `commitments` che porta anche i chiusi del giorno.
- **Quota**: via dalla pagina, resta `state.quota` in Utilizzo e limiti.
- **Cosa sparisce nell'app**: DiaryScreen, RecapScreen e i gruppi del Recap, le sezioni Impegni, Da approvare e Recap della home, le voci di menu Registro e Recap. La pagina Notte resta.

## Come è arrivato a Franz

I quattro PNG mandati nella chat di questa sessione con SendUserFile (schede file sul telefono), il 22/10 alle 08:05 circa; il passo del registro lo riporta.
