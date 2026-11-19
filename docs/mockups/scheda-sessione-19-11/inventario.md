# Scheda aperta di una sessione: cosa mostra oggi e dove va nella J

Dal codice del 19/11 (`mobile/.../ui/SummaryList.kt` `SummaryCard`, `KpiViews.kt`, `KpiDataViews.kt`; sulla web `Home.svelte`),
compito app-scheda-sessione-semplice-19-11.

| # | Cosa mostra oggi | Nel codice | Nella J |
|---|---|---|---|
| 1 | Glifo dello stato, favicon con l'anello | `SessionRailIcon` | Testata: avatar con l'anello dello stato |
| 2 | Nome della sessione | `namePart` | Testata |
| 3 | Tag «doppione» | `cleanup_dup_tag` | Testata, accanto al nome |
| 4 | ⊗ e ora dell'ultimo fallimento | `lastRed` | Testata a destra, in rosso, al posto di ✓ e ora dell'esito |
| 5 | «ctx 32%» nella riga chiusa | `ctx_short` | Riga Contesto in fondo |
| 6 | Conto di Per te sulla carta | `PerTeCardChip` | Prossimo › Ti aspetta, una riga per voce |
| 7 | Chip Decidi e Verifica (compatta) | `TodoChip` | Prossimo › Ti aspetta |
| 8 | «Apri» (schermo largo) | `desk_open` | Resta sulla carta chiusa |
| 9 | Freccia apri e chiudi, menu della sessione | `ExpandLess`, `SessionMenu` | Testata |
| 10 | Obiettivo | `goalLine` | Adesso › Compito (riga sotto) |
| 11 | Strumento in corso | `toolLine` | Adesso › «Sta facendo», solo mentre lavora |
| 12 | Esito, o domanda per intero | `body` | Esito in Adesso (con l'ora); domanda in Prossimo › Ti aspetta |
| 13 | «Chiesto da … per …» | `asked_for` | Sotto Ti aspetta, piccolo |
| 14 | «Sta facendo» e «Ha detto alle» | `detail.doing`, `detail.said` | Adesso |
| 15 | Approvazioni con «Approva» | `ApprovalLine` | Prossimo › Ti aspetta, col tasto |
| 16 | Avviso heads-up | `HeadsUpNoteRow` | Prossimo, sopra Ti aspetta |
| 17 | «Ok» e «Fatto» con la scadenza | `attention_ok`, `attention_done` | Prossimo › Ti aspetta, scadenza sotto il testo |
| 18 | Opzioni della domanda | `QuestionOptions` | Prossimo › Ti aspetta; la consigliata è l'unico tasto pieno |
| 19 | Prossimi passi (chip) | `parsed.steps` | Prossimo › Passo: il primo per esteso, gli altri come chip tonali |
| 20 | «Finestra aperta sul PC: si chiude da lì», «ha finito alle», «doppione di», «Chiudi» con conferma | `MasterService.cleanup` | Riga di stato Finestra in fondo, «Chiudi» tonale |
| 21 | Chiusura in corso, armata o rifiutata | `ClosingChip`, `ArmedChip`, `RefusedRow` | La stessa riga di stato, al posto di Finestra |
| 22 | Priorità bassa | `details()` | Testata, nella riga del modello |
| 23 | Modello, effort, account | `details()` | Testata, sotto il nome |
| 24 | «Oggi»: barra della giornata, ore al lavoro | `Kpi11` | Tolta: il totale va in «Prima · 6 turni oggi, 57 min»; la barra oraria nella pagina Utilizzo |
| 25 | «Turni di oggi»: turni, minuti, falliti, token per turno | `Kpi10` | Prima: ultimi 3 turni con ✓ o ✗; token per turno nella pagina delle quote |
| 26 | Barra del contesto con la tacca all'80% e «oggi Y%» | `kpi-5` | Riga Contesto; oltre la soglia il consiglio e «Fallo»; «oggi Y%» nella pagina delle quote |
| 27 | Attività per ora | `Kpi6` | Tolta: pagina Utilizzo |
| 28 | Voci di Per te della sessione (ok, prova, scelta, screenshot) | Per te | Prossimo › Ti aspetta, una riga ciascuna col suo tasto |
| 29 | Risultati: file, immagini, link, release | Risultati | Gruppo Risultati: il numero di oggi, miniature e pillole, il tocco apre |
| 30 | Consiglio del contesto (passaggio di consegne e /clear) | Contesto | Riga Contesto, «Fallo» tonale |
