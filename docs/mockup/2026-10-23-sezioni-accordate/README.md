# Sessioni e Lavori accordati: oggi, strada A, strada B (23/10/2026)

Segnalazione di Franz delle 22:50 (via master, due schermate della home del telefono): «le 2 sezioni si accordano poco tra loro, in particolare lavori ha un aspetto un po' sciatto e disordinato, puoi propormi un miglioramento grafico o concettuale?». Oggi le schede di Sessioni hanno badge di stato, nome, meta in mono, una riga di esito e la barra del contesto; quelle di Lavori hanno il titolo lungo in grassetto su quattro righe, un cerchio o un quadrato vuoto, percorso e account in piccolo e il tasto Fatto o Avvia.

Tela con le sei tavole e le tre note: https://claude.ai/artifact/G5gAMdiKgw6Hd5JGxaQutL (artefatto privato dell'account di Franz). Qui i PNG a grandezza reale, resi con chromium headless a scala 2: `telefono-oggi.png`, `telefono-a-grafica.png`, `telefono-b-concettuale.png` (390 di larghezza, pagina intera), `web-oggi.png`, `web-a-grafica.png`, `web-b-concettuale.png` (1200 di larghezza). I dati sono quelli veri delle schermate di Franz; in più un impegno del registro inventato, «Nome del pacchetto», legato a win:bluey, per mostrare come si comporta un lavoro con una sessione.

## Strada A, grafica

Stessa scheda delle sessioni anche per i lavori.

1. Badge della stessa famiglia: cerchio = account personale, quadrato = agenzia, come per le sessioni; il glifo dice cosa chiede: mano = aspetta te, triangolo = può partire. Un lavoro legato a una sessione prende il colore della sessione.
2. Titolo breve dal campo `short` del contratto 1.79, in grassetto, al posto del titolo lungo.
3. A destra il conto in mono, come «ctx 9%» delle sessioni.
4. Sotto, il titolo intero in grigio su due righe, come la riga di esito.
5. In fondo il riferimento in mono e il tasto allineato a destra, come il Fatto delle sessioni ferme: tonale per l'agenda, pieno solo per quello che sblocca una sessione.
6. Gruppi con intestazione colorata come le sessioni: DA FARE · 3, DA AVVIARE · 1, e OK DA DARE quando c'è un ok senza sessione.

## Strada B, concettuale

Un lavoro legato a una sessione sta nella scheda di quella sessione, come Approva: una riga con icona, titolo breve, «da te · chiede alle 22:31» e il tasto. In Lavori restano solo i lavori senza sessione, in una lista compatta: una riga per lavoro, titolo breve, riferimento in mono, tasto piccolo. La sezione si accorcia e la scheda della sessione dice tutto quello che aspetta da te.

## Cosa serve

- A: niente dal relay, `short` c'è già (contratto 1.79). Solo app e web.
- B: il legame lavoro → sessione. Gli impegni del registro hanno già `session`; le righe dell'agenda no: o il relay aggiunge `session` (o `project`) alle righe dell'agenda, o l'app lega per cartella del progetto.
- Le due strade si sommano: B usa la scheda di A per i lavori senza sessione.

## Scelta di Franz e seguito

Franz ha scelto la strada B alle 23:08 («b», inbox della master `04eec6`): compito `lavori-sessioni-23-10`. Alle 23:24 ha chiesto in più «quella grafica per le schede che rimangono separate, es sessioni e stanotte»: quattro tavole nella terza riga della tela e la nota arancione; i PNG stanno in `docs/mockup/2026-10-23-schede-separate/` (`telefono-oggi.png`, `telefono-grammatica.png`, `web-oggi.png`, `web-grammatica.png`).

## Le schede che restano a sé: una grammatica sola

Riga 1: badge della famiglia (cerchio o quadrato, un glifo), nome breve in grassetto, a destra la meta in mono, il chevron. Riga 2: una riga di testo in grigio, due al massimo. Riga 3, solo quando serve: a sinistra la meta in mono, a destra il tasto (pieno se sblocca una sessione, tonale altrimenti). In fondo, solo nelle sessioni, la barra del contesto con il valore in mono alla sua destra.

- Notte: da riquadro grande con titolo e tasto pieno a scheda come le altre: luna, «Stanotte», «23:30–06:30 · 5», la riga di testo, «Apri» tonale.
- Sessioni: la meta in alto perde «ctx N%», che sta sulla barra; il corpo è grigio per tutte, bianco solo per chi ti aspetta; le intestazioni dei gruppi restano.
- Altro: ogni voce è una scheda con la stessa riga 1 (glifo della categoria: triangolo per «da sistemare», orologio per i programmati, archivio per le chiuse) e il suo tasto in riga 3.
- Utilizzo resta com'è.

Solo proposta; codice dopo la scelta di Franz.
