# app-home-riordino-29-10 nello stile «Studio e Diretta» terza versione

Ok di Franz 30/10 09:49 (inbox b2124c, registrato dalla master): stile nuovo notte e giorno, push e pre-release del piano.
Riferimenti: docs/mockup/2026-10-30-home-unione/studio-diretta-v3-*.html (home e sistema), pwa-*-notte.html,
telefono-master-aperta-notte.html. Palette giorno: in attesa della scelta A, B o C che passa la master.
Ambito (master 10:22): solo la superficie home (lista, tabellone, barra e scheda della master, plancia web), telefono e
web, SOLO NOTTE; il giorno è un compito successivo per tutta l'app. Sessione, impostazioni e pagine restano come sono.
Plancia: restyle sì, togliere le ripetizioni solo dopo l'ok di Franz. Base: commit locale 4edb5c4a (riordino web, Kpi core).

## Web
- [ ] caratteri in web/public/fonts (Barlow Semi Condensed 500/600/700, Barlow 400/500, Geist Mono 400/500, woff2) + OFL
- [ ] token v3 notte nella classe .v3 su HomePane e Dash, con i nomi vecchi rimappati
- [ ] StateGlyph.svelte: ❓ ▶ (anello del contesto) ✓ ✗ a contorno, ferma tratteggiata
- [ ] tabellone: tre cifre (solo il 2 in ambra) con freccina, quota 5 ore neutra sotto
- [ ] titoli di sezione col binario (icona grigia 52 px, titolo Barlow SC), liste a filo, schede piene solo per le attese
- [ ] attese: binario glifo + minuti, opzioni una per riga con la consigliata a bordo chiaro e numero pieno, Fatto tonale
- [ ] suggerimento della master fuori dalle attese, lista a filo, icona grigia
- [ ] sessioni: binario, nome, strumento in mono, ctx, segno rosso con l'ora dell'ultimo rosso; Chiudi tonale
- [ ] lavori: Da riprendere, Notte su una riga, Tutti i lavori; esiti senza numeri ripetuti; Altro su quattro righe
- [ ] barra e scheda della master (chiusa, metà, aperta) e plancia nello stile v3
- [ ] data-testid home-sez-*; vitest, svelte-check e build su win; foto ?demo a 412 e 1440
- [ ] plancia web pwa2 (ok Franz 10:5x, inbox 3111cb): testata col tabellone come testa delle colonne, quattro colonne per
      argomento, master come barra in fondo e pannello a destra (420 px a metà, 720 px aperta); riferimento pwa2-*-notte.html
## Telefono
- [ ] caratteri in res/font, token v3 per la home, glifi, stessi punti in SummaryList.kt, MasterDock.kt e KpiViews.kt
- [ ] testTag home-sez-*; strings it/en
- [ ] Paparazzi golden da CI (record)
## Uscita
- [ ] commit per file, push per sha su tab-emoji/22-10, CI, pre-release con APK, deploy web, tag alla master
