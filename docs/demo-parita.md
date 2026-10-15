# Demo web e app Android: parità

Check-up chiesto da Franz il 15/10/2026 alle 20:36: «la demo deve essere affidabile e deve rispecchiare al 100% l'app».
La demo è la web app con i dati di prova: ogni correzione vale anche per la web vera.

Inventario fatto dal codice Android (`MainActivity.kt`, `ui/*.kt`, `core/rules/*.kt`, `strings.xml`). Ogni riga è
stata provata con chrome-bridge sulla demo locale: telefono a 412 px (`emulate_media`, tocco attivo), tablet a 1000 px,
desktop a 1300 px. Tocchi veri dove serve un gesto dell'utente (lettura ad alta voce); scorrimenti e trascinamenti con
eventi di scorrimento e pointer.

Regole coperte da test automatici (`web/src/lib/*.test.ts`): `demo.test.ts` (dati di prova coerenti, conti uguali fra
home, banner, avviso e coda, PC finto che risponde come FakeTransport), `swipePages.test.ts`, `queue.test.ts`,
`outside.test.ts`, `sessionsMenu.test.ts`.

| area | nell'app | nella demo | stato |
|---|---|---|---|
| dati | La demo dell'app (FakeTransport + DemoText): payments-api, storefront, blog, pricing-page, stato portato ad adesso | Stessi nomi e testi; stato, eventi, notte e recap portati ad adesso; in più la master e una conversazione per sessione | ok |
| dati | I comandi in demo cambiano lo stato con i testi del relay | `DemoTransport` porta FakeTransport: risposta, messaggio, stop, riapri, approva, segui, coda della notte, agenda, permessi, nuovo progetto, quota | ok |
| dati | Conti: «Ti aspettano» in home, banner della master, avviso in chat e coda dicono la stessa cosa | 3 nella home e nel banner, 2 nell'avviso dentro una di loro, 2 nella coda aperta da lì (test) | ok |
| dati | Niente eventi nel futuro né risposte a domande ancora aperte | Eventi spostati come lo stato, tolti quelli dopo adesso e le risposte alla domanda aperta (test) | ok |
| dati | Rifiuti del relay (modello, effort, file, aggiungi dispositivo in demo) | Stessi testi: «not available in demo», «missing or unreadable», «pair --add: no saved key» | ok |
| home | Fascia «Demo: sessioni di esempio, nessun PC collegato» | Fascia ambra in cima alla home, telefono e plancia | ok |
| home | Testata «Master · N aperte», lente, ≡ | Uguale: «Master 3 aperte», Cerca, Menu | ok |
| home | Chip «N per te» (state.attention): tocco porta alla prima voce | «3 per te»; sul telefono apre storefront, sulla plancia la mette in fondo alle colonne col fuoco nel campo | ok |
| home | Riga della quota per account sotto la testata, tocco = Utilizzo | Due righe (personal, work), il tocco apre e illumina «Utilizzo» | ok |
| home | Sezioni richiudibili ricordate: Notte, Sessioni, Utilizzo, Recap, Altro | Stesse sezioni e stesso ordine, ricordate nel browser, riassunto a sezione chiusa | ok |
| home | Banner «N ti aspettano» coi nomi | «3 ti aspettano storefront, payments-api, blog», tocco alla prima voce | ok |
| home | Gruppi «Da approvare», «Ti aspettano», «Hanno finito», «Al lavoro», «Ferme» | Stessi gruppi; le ferme per Franz (1.68) stanno in «Ti aspettano» | ok |
| home | Card sessione: badge, nome, segno permessi, ctx, 5h, testo; tocco apre | Uguale; il tocco apre la pagina con la transizione e la striscia si allinea | ok |
| home | ˅ apre la card sul posto, una alla volta | «Apri i passaggi» / «Chiudi i passaggi» | ok |
| home | Opzioni della domanda sulla card, la prima piena senza approvazioni | «1 · yes» tonale (c'è un'approvazione), risposta mandata e sessione al lavoro | ok |
| home | Chip «Prossimi» sulla card, ✓ dopo l'invio | Presenti sulle card ferme, il tocco manda e segna ✓ | ok |
| home | Riga di pulizia «aperta · ha finito il compito alle», «Chiudi nome» con conferma | «● aperta» su blog, conferma nella card | ok |
| home | Menu contestuale della card (pressione lunga / tasto destro): Segui, Fai controllare, Chiudi | Pannello con le stesse voci | ok |
| home | Card «appena chiusa» per 2,5 s dopo la chiusura | «blog chiusa alle 21:09 · ora in «Chiuse»» | ok |
| home | Da approvare: titolo, PRODUZIONE, Esce, Dove, Chiesto, Approva con nota | Uguale; Approva toglie il compito (demo) | ok |
| home | Card «La master non è aperta» con Riapri | Card con «Riapri» quando manca la master: provata chiudendo la master con /exit, «Riapri» la riporta | ok |
| home-notte | Riquadro «Notte del D1-D2 mese», conteggi, «N cose per te», Apri | «Notte del 14-15 ottobre», 2 finiti · 2 in corso · 1 fermo · 1 con domanda, «2 cose per te, prima payments-api: Staging is green. Deploy 2.8.0?» | ok |
| home-utilizzo | Pannelli quota: anello 5h, ripartenza, ritmo, settimana, Oggi | Pannelli per personal e work con campioni delle ultime tre ore e «OGGI» | ok |
| home-recap | Chip Azioni, «Aspetta te», «Può farlo Claude», «Tutto il recap» | Uguale, chiuso col riassunto «3 · 2 · 2» | ok |
| home-recap | Foglio «Mandare questa azione?» con origine e destinazione, ✓ dopo «Manda» | «Dal recap del 15/10 di storefront», «Va a storefront», chip ✓ | ok |
| home-recap | Schede agenda con segno d'ambito; Approfondisci, Fallo, Stanotte, Parlane con la master, Fatto, Rimanda, Passa, Rimuovi | Stesse azioni; le scritture cambiano l'agenda come `agenda_set` | ok |
| home-altro | «Fuori dalle sessioni»: Da sistemare, Programmati, Da riprendere (dal diario), Stanotte, Chiuse | Stesse categorie con icona e conteggio; in demo «Da riprendere» docs-site, «Stanotte», «Chiuse» | ok |
| home-altro | Righe di servizio col tasto: Handoff e riavvio, Vedi, Avvia, Ascolta, Aggiungi | Uguale; «Avvia» lancia docs-site nella demo | ok |
| home-altro | Scheda Stanotte: lavori in coda, quello in corso per primo, «DA N MIN» / «IN CODA», ultima notte e «Rapporto» | Uguale | ok |
| home-altro | «Chiuse»: i primi tre nomi, tocco = elenco con «Riapri» | Foglio «Chiuse · N», «Riapri» riporta la sessione fra le vive | ok |
| home-master | Barra della master in fondo: ▲, MASTER · ora · ctx, titolo, ▶ | Uguale; ▶ legge l'esito (provato con tocco vero) e diventa ■ | ok |
| home-master | Trascinare/toccare la barra: espande e riduce | Trascinata oltre 40 px in su espande, in giù riduce; ▲ e ⌄ fanno lo stesso col tocco | ok |
| home-master | Campo «Scrivi alla master» da chiusa: mandare apre la conversazione | Uguale; nella demo la master si mette al lavoro | ok |
| gesti | Scorrimento laterale fra home e sessioni (HorizontalPager), home sempre prima | Striscia orizzontale con aggancio: home, poi le sessioni nell'ordine della home | ok |
| gesti | Dito a sinistra sulla home apre la prima sessione | Dalla home a storefront scorrendo | ok |
| gesti | Dito a destra sulla prima sessione torna alla home | Scorrendo a sinistra si torna alla home | ok |
| gesti | Ordine delle pagine fermo durante il gesto, rifatto tornando alla home | `swipePages.stable`, stessi casi di SwipePagesTest | ok |
| gesti | Una sessione aperta da fuori (card, avviso, coda, «Vai a») porta la striscia lì | Allineamento immediato, anche a più pagine di distanza | ok |
| gesti | Gesto Indietro: dalla sessione o da una pagina torna alla home | Indietro del browser e del telefono torna alla home, poi esce | ok |
| gesti | Volo card ↔ pagina | Transizione di vista del browser sulla card e sulla pagina | ok |
| gesti | Tira per aggiornare | Lo stato arriva da solo (stream); il gesto del browser ricarica la pagina | ok |
| gesti | Pillola di lettura sopra tutte le schermate, «Torna a X» | Pillola con sessione di provenienza, pausa, velocità, stop | ok |
| menu | ≡: Lancia, Registro, Notte, Recap, Utilizzo, Cerca, Impostazioni | Stesse voci, anche nella testata della sessione sul telefono | ok |
| menu | Il nome della sessione con ▾ apre «Vai a»: home, gruppi, «Chiuse · N» | Uguale; righe «domanda · 5 m», «al lavoro · 1 m», «ferma da 1 g», ctx e ✓ | ok |
| menu | Lancia: account, progetto, recenti, primo messaggio, Lancia | Uguale; nella demo la sessione nasce al lavoro | ok |
| menu | Nuovo progetto: «Dove», Avanti, conferma | Uguale (`project_new` con conferma) | ok |
| menu | Aggiungi alla notte: schede del Recap spuntabili e progetti | Uguale; la coda della notte cresce | ok |
| menu | Ricerca nelle conversazioni, tocco apre la sessione | «staging» trova storefront e payments-api, il tocco apre | ok |
| coda | «Ti aspettano»: una pagina per voce dalla più vecchia, ← →, «N di M» | Domande e ferme per Franz (1.68) con motivo e «Apri la chat» | ok |
| coda | Dall'avviso di una chat: le sessioni che l'avviso conta | «2 sessioni ti aspettano» apre «1 di 2» | ok |
| registro | Stanotte (coda, Togli, Aggiungi), giorni, quota | Uguale; «Togli» toglie il lavoro dalla coda della demo | ok |
| notte | Pagina Notte: finestra, conteggi, «Da fare per te», «Cosa è successo», progetti, Aggiorna | Uguale; la domanda è quella aperta adesso | ok |
| notte | Righe «Da fare per te» con le opzioni, Approva, sblocco | «1 · yes», «2 · no», sblocco leggibile «svuotare il contesto (/clear)» col tasto «Manda /clear» (Franz, 15/10 22:05) | ok |
| impostazioni | Collegamento: schema dei dispositivi, dettagli al tocco | Schema con PC e dispositivi della demo | ok |
| impostazioni | Aggiungi un dispositivo | Foglio con la risposta del relay della demo | ok |
| impostazioni | Voce, Velocità di lettura | Uguale | ok |
| impostazioni | Lingua: come il telefono / Italiano / English | Come il browser / Italiano / English, vale subito | ok |
| impostazioni | Modalità demo (interruttore) | Interruttore: acceso porta alla demo, spento al collegamento | ok |
| impostazioni | Notifiche | Notifiche web push sulla strada remota; nella demo nascoste come nell'app (mai notifiche in Demo) | ok |
| impostazioni | Dettagli della sessione a destra (solo tablet) | Interruttore solo sulla plancia | ok |
| impostazioni | Privacy: l'informativa sul sito | Link alla stessa informativa | ok |
| impostazioni | Versione | Versione della web app | ok |
| menu | Modalità live nel menu ≡: notiziario in cuffia, comandi dal watch, «Recap della live» | Voce «Modalità live» nel menu ≡ con la nota «c'è solo nell'app Android: il notiziario in cuffia e i comandi dal watch» e il link alla pagina dell'app; la live vera resta nell'app (servizio del telefono e orologio) | differenza approvata da Franz («Live: voce che rimanda all'app», 15/10 21:50) |
| testata | Riga fissa: ←, nome con ▾ («Vai a»), ≡; sotto modello, quota, contesto, ⋮ | Sul telefono due righe come l'app; nella plancia nome nella testata della colonna | ok |
| testata | Note: obiettivo, bassa priorità, senza finestra | Uguale | ok |
| pillola-modello | «Modello · effort» col segno dei permessi; puntino del consiglio | Uguale | ok |
| pillola-modello | Pressione lunga sul segno: scheda con nome e spiegazione | Scheda «senza conferme · Fa tutto senza chiedere.» (pressione lunga o tasto destro) | ok |
| pillola-modello | Pannello Modello ed effort: consiglio, modelli, effort, permessi con doppio tocco | Uguale; nella demo il cambio risponde «not available in demo» come l'app | ok |
| quota | Pillola «5h N% · ora», si accorcia se manca spazio | Uguale (misurata sulla riga) | ok |
| contesto | Pillola ctx e foglio Contesto: Handoff poi /clear, Compatta | Uguale | ok |
| contesto | Proposta «Contesto al N%» con Fallo e × | Su blog al 64 % | ok |
| menu-sessione | ⋮: Segui/Non seguire, Fai controllare alla master, Apri in Claude, Chiudi la sessione | Stesse voci con le spiegazioni | ok |
| chiusura | Dialogo «Chiudere X?» con stato e conseguenze | «Chiudere blog? blog è aperta e ferma: ha finito il compito alle 20:12», «Lascia aperta», «Chiudi blog» | ok |
| chiusura | Pannello «si sta chiudendo» e «è chiusa», torna alla home in 3 s, Riapri | Uguale; nella demo /exit chiude davvero | ok |
| chat | In fondo all'apertura, segue i nuovi, ↓ «Torna all'ultimo messaggio» | Uguale | ok |
| chat | «Carica i messaggi precedenti» quando il relay ha `more` | Tasto in cima, pagina `before=` prepesa sopra | ok |
| chat | Riga «in attesa del PC da N s» / «aggiorno la conversazione» | Solo col relay; nella demo la conversazione è già lì | ok |
| chat | Stato vuoto «Conversazione nuova: ancora nessun messaggio» | Uguale, dopo /clear o su una sessione nuova | ok |
| chat | Divisore «Nuovi» | Uguale | ok |
| chat | Messaggi miei con stato (inviato, preso in carico, consegnato, non consegnato), Copia, Modifica | Uguale; i nuovi messaggi della demo seguono le regole del relay | ok |
| chat | Messaggi da altrove con l'origine (dal PC, dal telefono, dalla web app) | Uguale | ok |
| chat | Risposta di Claude con tabelle, link, riga Esito, costo del turno, Copia, ▶, segnalibro, ora | Uguale (tabelle dei numeri su blog) | ok |
| passaggi | Gruppo di passaggi con puntini, fallito in rosso, apri/chiudi | Uguale (storefront: 1 fallito) | ok |
| file | Ogni allegato a tutta larghezza con l'anteprima: immagini, video, PDF (prima pagina), testo e codice (prime righe), gli altri con una scheda larga | Uguale. Arrivano da sole: immagini fino a 10 MB, PDF fino a 10 MB, testi fino a 1 MB, video fino a 25 MB solo senza contatore; oltre, o col risparmio dati, «Mostra anteprima». Nella demo file veri: checkout-before.png, checkout-tests.pdf, edge-caching.md, bench-data.zip (Franz, 15/10 22:20) | ok |
| file | Apri, Scarica, Condividi; «Copia» solo dove copia il contenuto (testo e immagini), mai un percorso | Uguale (Franz, 15/10 22:19); nella demo i file si aprono, si scaricano e si condividono davvero | ok |
| file | Immagine a tutto schermo con zoom | L'immagine si apre in una scheda del browser, che ingrandisce | ok |
| file | Video con poster e controlli | Lettore del browser con i controlli | ok |
| domanda | Card domanda col bordo del rischio, ▶, opzioni, la prima piena | Uguale | ok |
| domanda | Rischio alto: pressione lunga per confermare | Stessa card (QuestionCard, tier high); la domanda della demo è a rischio medio, come nella fixture dell'app | ok |
| domanda | Parliamone, Consenti tutto | Uguale | ok |
| domanda | Risposta libera nel campo «Rispondi con parole tue» | Uguale | ok |
| domanda | «✓ risolta al terminale · opzione» | Uguale | ok |
| esito | Scheda quota finita con le strade, scheda turno fallito con Continua | Uguale nel codice; nella demo gli eventi non ci sono perché i conti della quota non tornerebbero (account all'11 %) | ok |
| voce | ▶ / ■ su risposte, esiti, domande sopra 120 caratteri; velocità | Uguale | ok |
| voce | Riga dal vivo: asterisco, tempo, strumento | «1 min 42 s · Run the checkout test suite after the Stripe webhook refactor» | ok |
| prossimi | Box «AZIONI · N» sopra il campo, ↗ manda, + accoda | Uguale su blog | ok |
| prossimi | Suggerimento nel campo con «Usa» | «pubblica il post · Usa» | ok |
| campo | Placeholder «Scrivi a X» / «Scrivi ad X» / «Rispondi con parole tue» | Uguale: «Scrivi a storefront», «Rispondi con parole tue» su payments-api | ok |
| campo | Invia / Ferma / Riapri | Uguale; Ferma nella demo ferma davvero il turno | ok |
| campo | Invio manda, Maiusc+Invio a capo | Uguale | ok |
| campo | Suggerimenti «/» e conferma di /clear | «/» propone /compact, /clear, /exit, /context, /cost; /exit apre il dialogo di chiusura | ok |
| campo | «@nome» nella chat della master | «@» propone @payments-api, @storefront con l'attività, @blog | ok |
| allegati | «+»: Foto dalla galleria, Scatta una foto, File; nella master Azioni ricorrenti e Salva una decisione | Uguale | ok |
| allegati | Anteprime in attesa con ×, fino a 5 | Uguale | ok |
| manda-alle | «Manda alle» con l'ora; «parte alle HH:MM» | Uguale; la demo risponde «scheduled for» | ok |
| avviso-altre | Pillola «X ti aspetta» / «N sessioni ti aspettano» + Rispondi; «X ha finito» + Apri + × | Uguale | ok |
| esito | Salva come decisione | Foglio uguale; la demo risponde «decision sent to the master» | ok |
| plancia | Da 840 di larghezza la plancia: home di lato e colonne | Uguale (provata a 1000 e 1300) | ok |
| plancia | Home a sinistra o a destra (⇄), «Colonne di nuovo uguali» | Uguale; il secondo tasto compare dopo un allargamento | ok |
| colonne | Fino a 4, le prime 3 senza scelta, tocco su una scheda aggiunge o toglie | Uguale | ok |
| colonne | Trascinare la testata scambia le colonne | Provato: storefront e payments-api si scambiano | ok |
| colonne | Trascinare il bordo allarga a dodicesimi | Provato: 176/176/176 diventano 308/132/88 | ok |
| testata-colonna | Badge, nome, ×, gruppo ed età | «TI ASPETTA · 7 M» | ok |
| cruscotto | Senza colonne: Recap e Utilizzo larghi | Uguale | ok |
| dettagli | Pannello Dettagli della prima colonna da 1200 | Uguale con la cronologia di oggi della demo | ok |
| master-metà | Master a tre misure sulla plancia: barra, metà, tutto | Uguale | ok |
| chiusura-colonna | Colonna di una sessione chiusa resta sotto il pannello, poi si toglie | Uguale | ok |
| link | Il link porta all'app o alla demo (NotPairedScreen: «Prova la demo») | Il link pubblicato su X apre la pagina di collegamento: «Prova la demo» è un tasto sotto «Inquadra il QR», pieno dentro il browser di un'app (prima era un link piccolo in fondo, sotto la barra di X) | ok |
| link | Aperta dentro il browser di un'app (X, Telegram, Instagram, LinkedIn, Facebook) | La pagina si misura sull'area visibile vera (visualViewport); una barra chiudibile: dentro X su Android dice di toccare ⋮ accanto all'indirizzo e aprire nel browser (il browser di X rifiuta i link intent://: «Nessuna app può eseguire questa azione», Franz 21:36); nelle altre app «Apri in Chrome» con le stesse istruzioni se la pagina resta lì; su iPhone due righe per Safari. Provato sul Pixel dall'app X (screenshot in `docs/screenshots/`, fuori da git): prima `demo-x-prima.png` (collegamento, «Guarda la demo» sotto il pannello di X); dopo `demo-x-dopo-collegamento.png` (barra e «Prova la demo» pieno), `demo-x-menu-apri-nel-browser.png` (la voce che la barra indica), `demo-x-dopo-demo.png` (demo aperta dentro X). Il pannello del post di X resta sopra la parte bassa: è una finestra di X, la barra porta al browser | ok |
| link | Dentro X il pannello del post copre la parte bassa della pagina | Il pannello è una finestra di X sopra la pagina e si trascina: lasciare 248 px liberi in basso rendeva la schermata troppo corta (Franz, 21:44), ritirato. La pagina resta a tutta altezza; la barra in cima spiega come aprirla nel browser, dove il pannello non c'è | ok |
| affidabilità | Nessun errore in console oltre il ResizeObserver | Nessun errore dopo i giri di prova; la transizione a pagina nascosta non lascia più promesse rifiutate | ok |
| affidabilità | Nessun tasto morto | Giro su ogni tasto di home, sessioni, pagine e plancia: tutti cambiano qualcosa | ok |

Bundle pubblicato su https://supervisor.francescosorrentino.com il 15/10/2026 alle 22:44: `index-COQhgBZS.js`.
