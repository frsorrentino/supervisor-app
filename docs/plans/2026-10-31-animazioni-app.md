# Animazioni dell'app

Tutte approvate: Franz, 31/10 21:34, dopo aver letto le 15 voci: «Approvate tutte». Compiti `app-animazioni-31-10` e `app-animazioni-resto-31-10`.

Riferimento: `video-supervisor/docs/plans/micro-prove/stile-scuro-lavori.mp4`, da 5,33 s a 10 s. Dopo l'ok una barra blu avanza sul bordo basso della card; a lavoro finito la card diventa verde, la pagina scorre e la card atterra in Done.

Criterio per ogni riga: quante volte al giorno si vede, a cosa serve, se resta sotto 300 ms, se aiuta chi legge. L'app si apre molte volte al giorno: nel dubbio, niente movimento. Le durate sono punti di partenza, da tarare al polso e al telefono.

## Proposte

Tempi e curve: «uscita» è `cubic-bezier(0.23, 1, 0.32, 1)`, in Compose `CubicBezierEasing`; «molla» è `CmMotion.bounce` su telefono (dampingRatio 0,7, `StiffnessMediumLow`, circa 5% di sforamento) e `--spring-bounce` sul web, su posizione e scala: Franz, 31/10 21:15: «ok per le animazioni, aggiungiamo anche un po' di rimbalzo sui movimenti?». Mai su colori, opacità, linea blu, scorrimento automatico, dati dei push. Il pannello della master resta senza rimbalzo: `CmMotion.panel` e `--spring` non si toccano (Franz, 03/10 17:12). Costo su telefono e web insieme: S mezza giornata, M una giornata, L più giorni.

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
| 11 | Ok nella card | Card con la richiesta (`ApprovalCard`, `SummaryList.kt`; `workCard` in `Home.svelte`) | Il tasto scelto si allarga e diventa la conferma, gli altri sfumano, la card si adatta in altezza | Tasto con molla, dissolvenza 150 ms | È il momento decisivo dell'app: si vede quale scelta è stata presa, e i tasti non spariscono di colpo | M |
| 12 | Risposta a un suggerimento | `SuggestionsCard.kt`; `SuggestionsCard.svelte` | La card esce nel verso della scelta: avanti su Accetta, in basso su Non ora e Mai. La successiva sale da 0,95 a 1 | Uscita 160 ms, entrata con molla | Si vede che la risposta è presa e che ne arriva un'altra; nelle risposte in fila la mano resta al suo posto | M |
| 13 | Vibrazione di conferma | Telefono: ok nella card, Accetta, «Ferma il ciclo» | Nessun movimento: `HapticFeedbackConstants.CONFIRM`, una volta, alla conferma | Subito | Le azioni decisive si sentono. Sul telefono la vibrazione c'è in `ReadingPill.kt`, `SessionSheet.kt` e `SummaryList.kt`, mai di conferma. Il web resta fuori | S |
| 14 | Tre valori condivisi | `CmMotion` in `PhoneTheme.kt`; `app.css` accanto a `--spring` | Nessun movimento nuovo. Veloce per pressione e tasti, senza rimbalzo; spaziale per posizione e scala, con `CmMotion.bounce`; lento per colori e passaggi di sfondo, senza sforamento | Molle, sopra `MotionScheme.expressive()` e non accanto | Tutte le animazioni parlano la stessa lingua e si tarano in un punto solo | S |
| 15 | Cifre a larghezza fissa | Numeri di tabellone, quota e carico. Sul web oggi `tabular-nums` è solo in `Kpi7.svelte` e `QuotaPanel.svelte` | Nessun movimento: `tabular-nums` sul web, `fontFeatureSettings = "tnum"` in Compose, da provare sui caratteri dell'app | Nessuna | Le cifre non tremolano quando cambiano, senza animare i dati | S |

## Cosa fa davvero ogni voce

Costruite tutte il 31/10 e l'1/11. Dove il comportamento si scosta dalla tabella vale questo; la 9 e le altre scelte sono state accettate dalla master.

- **1:** linea blu indeterminata sul bordo basso delle sessioni con un compito approvato al lavoro e nelle righe «al lavoro» di Lavori; con le animazioni spente resta ferma. Il tempo passato è quello già scritto nella riga.
- **2:** bordo verde per 6 s, la riga scivola al posto nuovo con `CmMotion.bounce`; l'icona è quella dello stato. Scorrimento solo con tutte le condizioni sopra; sul telefono un foglio o un dialogo si riconosce dalla finestra della home senza fuoco.
- **3:** icona di stato con scala da 0,9 col rimbalzo e dissolvenza di 180 ms.
- **4:** un solo passaggio, dissolvenza e scala da 0,96 in 200 ms, per Aggiorna tutto e per i tasti delle op di Sistema (`ActionPass`; `pass` sul web). Il tasto non si restringe nell'indicatore.
- **5:** telefono: la lista fa posto con la molla, il contenuto compare dopo 170 ms ed esce in 80 ms, così le sezioni non gli passano sopra; la freccina ruota. Web: apertura in altezza con `grid-template-rows` da 0fr a 1fr, 200 ms e 150 in chiusura; freccina che ruota; con la riduzione solo dissolvenza.
- **6:** scala a 0,97 alla pressione, senza rimbalzo.
- **7:** telefono: entra da altezza 0 in 220 ms ed esce in dissolvenza di 80 ms, le altre salgono con la molla. Web: 220 e 160 ms.
- **8:** la copia vola al posto con il rimbalzo; vibrazione a ogni scambio solo sul telefono. Se il posto finale è fuori schermo la copia sparisce.
- **9:** rifatta l'1/11 dopo la prova di Franz sulla 0.2.1636 («il drag riguarda solo la barra e il contenuto rimane fermo»). Ora la barra non si muove mai da sola: col dito si sposta tutto il pannello della conversazione, rigido, con la barra in cima, sopra la home che arretra; il campo resta fermo. Sul telefono una frazione guida sia il gesto sia il tocco (molla `CmMotion.panelFloat`, uguale a `panel`, con la velocità del dito), e sul tablet il foglio si allunga col dito. Sul web lo stesso gesto nella colonna unica e nel foglio della plancia; sulla plancia, da chiusa, il gesto apre al rilascio senza muovere nulla, e nel pannello laterale non sposta nulla. Con le animazioni ridotte niente inseguimento: il cambio avviene al rilascio.
- **10:** spunta verde quando chi aspetta passa da qualcuno a nessuno, per 2,5 s; non guarda i lavori finiti.
- **11:** il tasto scelto diventa una pillola con la spunta che si allarga, gli altri sfumano in 150 ms; se il PC non risponde i tasti tornano dopo 8 s.
- **12:** la voce esce in 160 ms, avanti su Accetta, una scelta e Parliamone, in basso su Non ora e Mai; la risposta parte dopo quei 160 ms. La successiva sale da 0,95 col rimbalzo.
- **13:** `HapticFeedbackConstants.CONFIRM` su ok, Accetta o una scelta, e «Ferma il ciclo»; solo telefono.
- **14:** `CmMotion.fast`, `bounce`, `slow` e le loro varianti; `--spring-fast`, `--spring-bounce`, `--spring-slow` con la durata accanto.
- **15:** `tnum` su cifre grandi e tabellone del telefono; `tabular-nums` su tutta la pagina web.

- **16, cambio forma dei menu (Franz, 01/11, rivista dopo la prova: «l'animazione deve riguardare anche il contenuto»):** pannello e contenuto crescono insieme dal punto premuto, come un solo oggetto, da un quinto della grandezza e mai da zero; niente rimbalzo. Vale per tutta la famiglia. Telefono: `growFromPress` in `PanelShell` (≡, Vai a, sessione, Modello ed effort), in `GrowPopup` (il +, la scheda dei permessi) e in `ModalBottomSheet` di `Buttons.kt`, per tutti i fogli; apre con `CmMotion.panelFloat`, chiude con `CmMotion.fast`, inverte a metà; spento con `animationsOff()`. Un foglio trascinato dal dito non cambia misura e va via col dito. Senza un tocco nell'ultimo secondo, per esempio dalla finestra di un altro menu, il foglio cresce dal fondo. Sul tablet il foglio è una scheda: cresce, ma si chiude di colpo. Web: `growFromPress` sui menu in `{#if}` (320 ms in apertura, 160 in chiusura) e `installDialogGrow` su tutti i `<dialog>`, che ora escono allo stesso modo anche con Esc; restano fuori gli avvisi al centro.

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
- **Rimbalzi** fuori da posizione e scala. Il rimbalzo su ciò che non è lanciato dal dito è una scelta di Franz (31/10 21:15: «aggiungiamo anche un po' di rimbalzo sui movimenti»), con un solo stile e con la riduzione delle animazioni che lo toglie.
- **Ritardi** che bloccano un tocco: ogni animazione si interrompe e il tocco vale subito.
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

## Secondo giro: le proposte da 11 a 15

Aggiunte il 31/10 alle 21:27, da un prompt di un'altra IA valutato lo stesso giorno. Franz: «Aggiungi le cinque all'elenco». Tutte approvate (Franz, 31/10 21:34: «Approvate tutte»): si scelgono dopo la prova delle prime tre. La 14 viene per prima, perché la 11 e la 12 ne usano i valori.

**Revisione del 01/11, dopo la prova sul telefono (Franz: «troppo lineari… un piccolo rimbalzo», «una leggera sfocatura mentre appaiono, non saprei»).** Voce 16: in apertura menu, fogli, scheda dei permessi e dialog del web usano la molla col rimbalzo e una sfocatura di 6 px che sparisce a poco più di metà corsa; la chiusura resta rapida e senza rimbalzo. Telefono: anche l'entrata delle pagine usa la molla. Restano senza: il pannello della master (scelta del 03/10, e sforando lascerebbe un vuoto sotto il campo), il volo della card (bordi rimisurati, non provabile senza telefono), colori e opacità. La sfocatura è in prova, da togliere se non convince; sotto Android 12 non c'è.
