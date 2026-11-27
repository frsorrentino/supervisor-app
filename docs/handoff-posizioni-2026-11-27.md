# app-posizioni-sistema-27-11 (B Foglio) + app-posizioni-basso-27-11 (variante consigliata) — piano approvato dalla master ~18:55

Materiale: D:/cm-sessions/dalla-master/posizioni-2026-11-27/ (proposta.md: «La riga di icone», «B. Foglio», «Cosa sta in basso»;
mockup m/b-foglio.html, m/riga-icone.html, m/a-regia.html; analisi.md). Ordine generale: play → casella → Bacheca → gruppi 1-6.

Risposte della master:
- Tendina: per ora il contenuto di oggi delle sezioni con i tempi del centro (360 ms FAST_OUT_SLOW_IN, 32 ms fra le righe);
  contenuto e contenitore SEPARATI (app-centro-notifiche-26-11 è ancora proposta: cambierà il contenuto, non la posizione).
- Il foglio alla misura «riga» prende del tutto il posto della barra della master (MasterDock), anche nella home del telefono.
- Prove: una dopo il gruppo 3 (riga di icone + sessione + basso), la finale dopo il gruppo 6; immagini a 360 e 1280, anche alla master.

Gruppi:
- [ ] 1 Riga di icone (IconRow) telefono+web: icona+numero sempre, nome sulla scheda aperta; colori ambra/verde/grigio, zero = niente
      numero; molle nome spring(0.75,300), pillola spring(0.76,340), salto spring(0.31,520) da 0,72 v 6; ridotte: critico, niente salto;
      Alt+1…5, Esc. Test regole + screenshot 3 stati.
- [ ] 2 Sessione: testata Chat · Per te · Risultati · Modifiche · Dettagli; tendina sopra la chat fino al campo; Avvisi in Per te;
      Modifiche col numero dei file; Dettagli = modello, effort, quota 5h, obiettivo, priorità, finestra; ⋮ resta. Via le due righe di
      pillole e la riga dei dati in testata.
- [ ] 3 In basso: vicino al campo Invia/Stop, +, microfono, «Usa», «Manda alle…», citazioni, mini-controller lettura; Azioni = chip
      sopra il campo nascosti scrivendo; Contesto = anellino nel campo, chip «Contesto 72% · Handoff» da 70%; via «Invio manda…».
      → PROVA a Franz (360 e 1280, anche alla master).
- [ ] 4 Foglio della master: riga ~170 / metà / intera; trascinamento con proiezione e scatto; tocco riga → metà; Esc/↓ → riga.
      Telefono: foglio a tutta larghezza, sotto la home con la riga Per te · Sessioni · Risultati · Lavori; sessioni da destra.
      Web: foglio sopra la colonna home a destra (400, 440 da 1680), pagina ferma; sessioni 2 da ~420 a 1280, 3 a 1920. Una preferenza.
      Base: HomePane/DockEmbed (web), AnchoredDraggable (telefono). Il foglio sostituisce MasterDock.
- [ ] 5 Via le posizioni: web App.svelte blocco posizioni (~430+110 CSS), masterLanding.ts, landingMorph.ts, masterKey.ts,
      MasterKey.svelte, homeRow.ts, HomeRow.svelte, morphBlob.ts + test, 6 preferenze cm.master_*/home_row; telefono core MasterPlace,
      MasterMove, MasterLanding, HomeRow (+test), ui HomeRowUi.kt, MasterMoveKeyUi.kt, MasterFrame; restano molle MasterGestures.
      Home in riga → numeri sulle icone. Demo allineata.
- [ ] 6 Tablet e largo, immagini 360/1280/1920, PROVA finale.
