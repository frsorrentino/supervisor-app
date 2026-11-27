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

## Avviso della master per il gruppo 1 (27/11 ~20:30)
Franz (20:22) ha chiesto il Rubber dock di designbookmark «agli elementi dell'app»; la master gli chiede dove con una scheda
(consiglio: la riga di icone, pillola elastica = due bordi L/R con molle diverse al posto di X/W 340/28; nome 300/26 e numeri come da piano).
- Il gruppo 1 si fa come da piano, ma il calcolo di posizione e larghezza della pillola sta in UN PUNTO SOLO (un'unica funzione o classe,
  telefono e web), così la pillola elastica si innesta senza rifare il resto.
- NIENTE golden sul movimento della pillola finché Franz non sceglie; la scelta arriva come compito a parte (app-dock-elastico-27-11).
- Codice originale e mockup: D:/cm-sessions/dalla-master/dock-elastico-2026-11-27/ (fonte-designbookmark/, riga-elastica.html).
- Molle (k/c, massa 1; dampingRatio Compose = c/(2√k)): bordo davanti 640/38 (0,751), bordo dietro 150/16 (0,653); col dito 1100/56 (0,844)
  e 320/26 (0,727); fermo, dir 0: 600/44 (0,898); salto icona 520/16 (0,351), spinta −3,2; card: larghezza 340/21 (0,569), altezza 230/21
  (0,692), chiusura 420/30 (0,732). Pillola tirata: altezza = 40·clamp(1/√(larghezza/larghezza finale), 0,66, 1,1). Oltre i bordi:
  elastico di dock.js (d=46, 0,55) e la riga che segue al 35%.

## app-dock-elastico-27-11 approvato (Franz 27/11 20:46, scelta «Riga di icone»)
- Il Rubber dock (pillola elastica a due bordi, trascinabile col dito, elastico ai bordi; con «riduci movimento» niente rimbalzo) sulla
  riga di icone, telefono e web, molle --dock-* di tokens.css (dampingRatio = c/(2√k)), colori v3, testi veri. Va DOPO il gruppo 1, sulla
  stessa pillola. Mockup dal vivo https://claude.ai/artifact/QEtxUiQUsiGTff1ywjM6sr; copia per win D:/cm-sessions/dalla-master/dock-elastico-2026-11-27/.
  Test, golden, commit col compito, build e deploy verdi, prova con immagini a 360 e 1280.
- Franz nello stesso messaggio: «io in realtà intendevo ottenere l'effetto di entrata elastica su tutti gli elementi della home quando si
  avvia l'app» → è un'altra richiesta (entrata elastica della home all'avvio): segnalata alla master, da fare solo se la master la avvia.

## Proposta del centro notifiche (dopo il gruppo 1, solo documenti; ok della master 27/11 ~21:30)
- docs/proposte/2026-11-26-centro-notifiche/ (fonti.md c'è): proposta.md, mockup animati per voce, tutte.html autonomo, 00-tutte.png, 2-4 opzioni.
- Tasti ACCANTO A OGNI MOCKUP (Franz 20:53): «Rivedi» e «Al rallentatore» subito sotto il titolo di ogni mockup, muovono solo quello;
  niente barra di comandi in cima.
- Un WebP animato per voce, come dock-elastico-2026-11-27/home-entrata.html: __frame(ms) disegna un istante, foto a 30 fps, montaggio in
  loop (la home: 55 fotogrammi, 326 KB).
- Nella scheda: prima l'HTML autonomo, poi i WebP e le immagini.
