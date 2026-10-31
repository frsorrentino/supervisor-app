# Animazioni dell'app: proposta

Compito `app-animazioni-proposta-31-10`. Solo proposta: nessun codice. Franz sceglie l'elenco, la costruzione è un compito a parte.

Riferimento: `video-supervisor/docs/plans/micro-prove/stile-scuro-lavori.mp4`, da 5,33 s a 10 s. Dopo l'ok una barra blu avanza sul bordo basso della card; a lavoro finito la card diventa verde, la pagina scorre e la card atterra in Done.

Criterio per ogni riga: quante volte al giorno si vede, a cosa serve, se resta sotto 300 ms, se aiuta chi legge. L'app si apre molte volte al giorno: nel dubbio, niente movimento. Le durate sono punti di partenza, da tarare al polso e al telefono.

## Proposte

Tempi e curve: «uscita» è `cubic-bezier(0.23, 1, 0.32, 1)`, in Compose `CubicBezierEasing`; «molla» è senza rimbalzo, `Spring.DampingRatioNoBouncy`, come `moving` in `SummaryList.kt`. Costo su telefono e web insieme: S mezza giornata, M una giornata, L più giorni.

| # | Azione | Dove | Cosa si muove | Durata e curva | Perché serve | Costo |
|---|---|---|---|---|---|---|
| 1 | Lavoro in corso dopo l'ok | Card di un compito approvato (`WorkCard`, `SummaryList.kt:1188`; `workCard` in `Home.svelte`) | Sul bordo basso una linea blu sottile, indeterminata, che scorre senza riempirsi; sotto, il tempo passato | Ciclo 1,6 s lineare, entrata 200 ms in dissolvenza | Il lavoro partito si vede senza aprirlo. **Onesta:** non avanza come una percentuale, perché il relay non la dà | M |
| 2 | Lavoro finito | Stessa card | Bordo e icona passano a verde; la riga scivola al posto nuovo solo se chi guarda è fermo (regola sotto) | Colore 250 ms uscita, posto con molla | La fine si coglie a colpo d'occhio e si vede dove è finita la voce | M |
| 3 | Invio con Riprova o allegato | Voce mia in chat (`StatusMark`, `SessionSheet.kt:1823`) | Icona di stato che cambia con dissolvenza e scala da 0,9 a 1 | 180 ms uscita | Risponde al difetto del doppio invio: si vede che il tocco è stato preso | S |
| 4 | Stato «in corso» di Aggiorna tutto e tasti che lavorano | `PluginsSection.kt`, tasti delle op di Sistema | Il tasto si restringe nel proprio indicatore, poi diventa l'esito | 200 ms uscita | Niente salto fra tasto, attesa ed esito | S |
| 5 | Apertura e chiusura di una sezione | Testate e righe della home (`V3Section`, `V3Row`; `.sec` in `Home.svelte`) | Il contenuto si apre in altezza con dissolvenza, la freccina ruota | 200 ms uscita, chiusura 150 ms | Oggi il contenuto compare di colpo e la lista salta | M |
| 6 | Pressione sui tasti | Bottoni e pillole (`V3Pill`, `.tonal`, `.filled`) | Scala a 0,97 alla pressione, non al rilascio | 100 ms al tocco, 160 ms al rilascio | Conferma che il tocco è arrivato. Quasi invisibile, quindi regge l'uso frequente | S |
| 7 | Comparsa e sparizione di una domanda o richiesta | «Ti aspettano» | Entra da altezza 0 con dissolvenza; alla risposta esce e le altre salgono | Entrata 220 ms uscita, sparizione 160 ms | Evita che le card saltino sotto il dito, soprattutto mentre si sta per toccare | M |
| 8 | Rilascio del trascinamento | Home, dopo la pressione lunga | La copia del titolo vola al posto finale e la sezione torna piena; una lieve vibrazione a ogni scambio | Molla, circa 250 ms | Oggi la copia sparisce di colpo: si perde dove è finita la voce | S |
| 9 | Foglio della master, tutto aperto e tutto chiuso | `MasterDock` | Segue il dito 1:1 e al rilascio prosegue alla velocità del dito | Molla, risposta 0,3-0,4 s | È il gesto più fisico dell'app. Da guardare prima: può darsi che basti poco | M |
| 10 | Prima apertura e «tutto fatto» | Home vuota: nessuno ti aspetta, lavori finiti | Una spunta verde che si disegna, una volta sola | 400 ms uscita, solo alla transizione | Lo spazio per il piacere è nei momenti rari, non nei frequenti | S |

## Le due del film, dette onestamente

**Barra sul bordo (1).** Nel film avanza come se sapesse quanto manca. Il relay non dà una percentuale: dà al più l'ora di inizio (`turn_started` per le sessioni, `started` per i lavori di notte), e una barra che si riempie sarebbe un dato inventato: quello che il relay non sa non si mostra. Per questo la proposta è una linea indeterminata col tempo passato scritto accanto. Se poi serve una barra piena davvero, bisogna che il relay mandi i passi fatti e quelli totali: sarebbe una richiesta sul contratto, non ora.

Legame col codice: `LavoriScreen.kt:496` ha già un `LinearWavyProgressIndicator` senza valore, quindi indeterminato, sotto il nome del lavoro notturno in corso. È la stessa onestà con un altro aspetto: un'onda a tutta riga dentro la card, in `Cm.busy`. Propongo una sola forma in tutta l'app: la linea sul bordo nelle card, e l'onda di `LavoriScreen.kt:496` portata alla stessa forma. Il web non ha un indicatore equivalente: va fatto con una `transition` su `transform`.

**Scorrimento finale (2).** Non deve mai rubare lo scorrimento. Scorre da solo solo se tutte queste sono vere:
- nessun dito è sulla lista e la lista è ferma da almeno 2 s;
- la voce finita è sullo schermo, o lo era quando Franz ha dato l'ok;
- nessun campo di scrittura ha il fuoco e nessun foglio è aperto;
- l'atterraggio è entro uno schermo di distanza.

Altrimenti la card diventa verde dove si trova e basta. Se Franz tocca la lista mentre scorre, lo scorrimento si ferma subito e non riparte. Più voci che finiscono insieme non fanno più scorrimenti: al massimo uno ogni 10 s.

## Cosa NON animare

- **I dati che arrivano a ogni push:** cifre del tabellone, quota, carico del PC, «ctx», tempi che passano, riassunti delle righe. Cambiano di continuo e li si legge: si aggiornano e basta. Niente conteggi che scorrono.
- **Le azioni ripetute:** apertura della ricerca, invio di un messaggio normale, cambio di scheda frequente, tastiera, apertura di una sessione più volte al minuto. Solo la pressione (6), mai una transizione che fa aspettare.
- **Grafici di Utilizzo:** niente disegno progressivo, sono da leggere.
- **I testi della chat in arrivo:** niente comparsa a cascata oltre a quella che c'è già all'apertura (`cascadeIn`, `SessionSheet.kt:1582`).
- **Rimbalzi** su ciò che non è stato lanciato dal dito, e **ritardi** che bloccano un tocco: ogni animazione si interrompe e il tocco vale subito.
- **I colori di stato** non diventano decorazione: restano ambra, blu, verde, rosso con i loro significati.

## Riduzione delle animazioni

- **Telefono:** esiste già `animationsOff()` in `PhoneTheme.kt:108`, che legge `ANIMATOR_DURATION_SCALE` e vale anche per le immagini di prova (`LocalStill`). Ogni proposta passa da lì. Con le animazioni spente restano i cambi di colore e lo stato finale; niente movimento, niente scorrimento automatico. La linea della proposta 1 diventa fissa, così il «in corso» si legge ancora.
- **Web:** `@media (prefers-reduced-motion: reduce)` come già in `Desk.svelte:150` e `CloseSplash.svelte:73`: dissolvenze brevi al posto degli spostamenti.
- **Prove:** le immagini Paparazzi girano con `still = true`, quindi i golden non cambiano per colpa di un'animazione. Il movimento vero si può giudicare solo sul telefono, e da lì lo giudica Franz.

## Limiti

- Solo proprietà leggere dove si può: `transform` e `opacity` sul web, `graphicsLayer` in Compose. L'apertura delle sezioni (5) cambia l'altezza: va provata su una home lunga prima di tenerla.
- **Wear OS fuori da questo giro.** Nessuna proposta vale l'eccezione: sull'orologio contano la batteria e la lettura al volo.
- Le durate e il «sentirsi bene» non si decidono dal codice: ogni riga scelta si prova su un telefono vero.

## Scartate

- Cifre del tabellone che contano fino al valore: dati letti di continuo.
- Passaggi tra pagine più elaborati: `PageMotion.kt` e il volo di `Fly.kt` ci sono già.
- Coriandoli o animazioni a ogni esito verde: 50 esiti al giorno.

## Le tre che farei per prime

1. **La 3, stato sull'invio:** poco costo e chiude il difetto appena segnalato.
2. **La 1, linea sul bordo:** la richiesta di Franz, in forma onesta, e unifica l'indicatore esistente.
3. **La 2, fine del lavoro col verde e lo scorrimento prudente:** completa la 1 e dà il momento del film.

Le altre dopo averle viste al polso e al telefono.
