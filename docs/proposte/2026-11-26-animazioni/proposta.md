# Animazioni per lo stile

Richiesta di Franz, 26/11 alle 11:15, dalla web app: studiare le animazioni di https://designbookmark.com/motion e proporre dove usarle nell'app, coerenti con tutta l'app e senza appesantirla («serve dare maggior stile all'app»). Compito `app-animazioni-stile-26-11`.

Questa è una proposta: nessun codice dell'app è cambiato. Ogni voce ha un mockup animato (`NN-nome.html`, si apre nel browser, con «Riproduci» e «Animazioni ridotte») e un PNG a grandezza reale, 360 px di larghezza (`NN-nome.png`). `index.html` li mostra tutti insieme.

## Da dove vengono

La pagina mostra 89 componenti animati dal vivo; il testo non descrive né tempi né curve, e i prompt «Copy for Claude» non sono leggibili senza aprirli uno per uno. Le voci qui sotto prendono dalla pagina l'idea di ciascun componente, indicato per nome, e la rifanno con il movimento che l'app ha già. Nessuna libreria nuova: Compose sul telefono e sull'orologio, CSS sulla web.

Sono rimasti fuori i componenti che portano un aspetto non nostro, come Goo, Glass lens, Siri orb, Thinking sphere, Parallax ticket, Like burst e Dust delete. Sono rimasti fuori anche quelli che rifanno cose già risolte: Toggle switch, Pull to refresh, Command palette e Expand card, che è la voce 5 del 31/10.

## Il movimento che l'app ha già

Il piano del 31/10 (`docs/plans/2026-10-31-animazioni-app.md`) ha portato 16 voci; lì c'è cosa fa ognuna. Fra queste:

- la pressione al 97 %;
- la linea blu del lavoro in corso;
- le sezioni che si aprono in altezza;
- le domande che entrano ed escono senza far saltare la lista;
- l'ok che diventa conferma;
- la spunta di «tutto fatto»;
- i menu e i fogli che crescono dal punto premuto;
- le cifre a larghezza fissa.

Nel codice: sul telefono 49 `Animatable`, 23 `AnimatedVisibility`, 19 cicli infiniti e 13 `AnimatedContent`; sulla web 64 regole per le animazioni ridotte. Tutto passa da tre valori condivisi, `CmMotion` sul telefono e le variabili `--spring-*` in `app.css`:

| Nome | Telefono | Web | Per cosa |
|---|---|---|---|
| veloce | `CmMotion.fast`, molla senza rimbalzo | `--spring-fast`, 200 ms | pressione, tasti, scambi d'icona |
| rimbalzo | `CmMotion.bounce`, circa 5 % | `--spring-bounce`, 620 ms | solo posizione e scala |
| lento | `CmMotion.slow` | `--spring-slow`, 500 ms | colori e fondi |
| ciclo | linea della voce 1 | 1,6 s lineare | ciò che lavora e non sa quando finisce |
| uscita | `CmMotion.easing`, cubic-bezier(.3, 0, .2, 1) | stessa | dissolvenze da 150-250 ms |

Le proposte qui usano solo questi valori, più un arco che gira in 1,2 s. Restano valide le regole del 31/10:

- niente conteggi che scorrono sui dati dei push;
- niente animazioni sulle azioni ripetute;
- rimbalzo solo su posizione e scala;
- con le animazioni ridotte (`animationsOff()` sul telefono, `prefers-reduced-motion` sulla web) niente movimento, solo lo stato finale.

## 1. Copia che risponde

Il componente è *Copy button*; mockup `01-copia.html`.

- **Dove:** telefono e web. Il tasto Copia del blocco `` ```text `` (la novità di ieri) e la Copia sotto ogni risposta. L'orologio non copia.
- **Cosa fa:** oggi Copia è muta. Al tocco l'icona diventa una spunta verde e la parola «Copiato»; dopo 1,6 s torna com'era.
- **Tempi:** pressione 100 ms; scambio con dissolvenza di 180 ms e scala da 0,9 con la molla veloce.
- **Animazioni ridotte:** scambio secco, la spunta resta 1,6 s.
- **Costo:** piccolo. Un componente condiviso: `SmallAction` sul telefono, il tasto `.cp` sulla web.

## 2. Lettura che segue la voce

Il componente è *Read along*; mockup `02-lettura.html`.

- **Dove:** telefono e web, nella chat durante la lettura a voce. L'orologio resta al paragrafo: schermo piccolo e batteria.
- **Cosa fa:** oggi si accende il paragrafo intero. Qui un fondo leggero segue anche la parola detta, e le parole già lette passano in chiaro. La posizione la dà il motore vocale, nessuna stima a tempo: `onRangeStart` su Android dalla 8, l'evento `boundary` nel browser.
- **Tempi:** passo di parola 140 ms con la molla veloce; colore 120 ms.
- **Animazioni ridotte:** il fondo salta di parola in parola senza scorrere.
- **Costo:** medio. Va mappato il pezzo letto (`SpeechText.chunks`) sulle parole del paragrafo. Dove il motore non dà i confini, resta il paragrafo.

## 3. Al lavoro, vivo

Il componente è *Thinking text*; mockup `03-al-lavoro.html`.

- **Dove:** telefono e web, nella parola «al lavoro» delle righe delle sessioni. L'orologio no, per la batteria.
- **Cosa fa:** un riflesso attraversa la parola blu con lo stesso ciclo della linea della voce 1. Chi lavora si distingue a colpo d'occhio da chi è fermo, senza icone in più. Si muove solo quella parola, solo sulle righe al lavoro.
- **Tempi:** ciclo di 1,6 s lineare.
- **Animazioni ridotte:** parola blu ferma, come oggi.
- **Costo:** piccolo. Sulla web un gradiente sul testo; sul telefono un `Brush` animato sullo stile del testo, con un solo `rememberInfiniteTransition` per tutta la lista.

## 4. Cifra che scorre

I componenti sono *Word roll* e *Stat card*; mockup `04-cifra.html`.

- **Dove:** telefono e web, nel numero «ti aspettano» del tabellone.
- **Cosa fa:** la cifra scorre solo quando cambia per un tuo gesto. Rispondi a una domanda e «3» diventa «2» davanti a te: la vecchia sale e sfuma, la nuova arriva da sotto. Quando cambia per un aggiornamento dal PC si aggiorna e basta, come oggi. Il mockup mostra i due casi.
- **Tempi:** scorrimento 220 ms con la molla veloce, dissolvenza 160 ms, senza rimbalzo perché è un dato.
- **Animazioni ridotte:** cambio secco.
- **Costo:** piccolo-medio. Serve sapere che il cambio viene da un tuo gesto. L'app lo sa già, perché la domanda esce con la voce 7.

## 5. Passi della prova

I componenti sono *Agent steps* e *Step progress*; mockup `05-passi.html`.

- **Dove:** telefono e web, nella card di una sessione in verifica.
- **Cosa fa:** le tappe della prova su un filo: test, CI, pre-release, la tua prova, le stesse dei contratti della master. Quando una tappa passa, il filo si riempie fino a lei e il pallino si disegna in spunta; quella in corso ha un arco che gira. Una tappa fallita diventa rossa con la sua ✗.
- **Tempi:** filo 400 ms in uscita; spunta disegnata in 250 ms con la scala della molla veloce; arco 1,2 s lineare.
- **Animazioni ridotte:** tappe accese senza movimento, arco fermo.
- **Costo:** grande. È l'unica voce che chiede dati nuovi: il relay deve mandare le tappe e il loro stato, cioè una richiesta a claude-master e un campo nel contratto. Senza quei dati non si fa.

## 6. Tieni premuto per la produzione

I componenti sono *Hold to publish* e *Tool approval*; mockup `06-tieni-premuto.html`.

- **Dove:** telefono, web e orologio (la pressione lunga c'è già al polso). Solo per le approvazioni segnate come produzione, quelle rosse.
- **Cosa fa:** il tasto si riempie mentre tieni premuto e l'ok parte quando è pieno. Lasciato prima, torna vuoto e non succede niente. Un tocco per sbaglio, in tasca o scorrendo, non pubblica. Le altre approvazioni restano a un tocco. Pieno, diventa la conferma della voce 11.
- **Tempi:** riempimento 600 ms lineare, perché è un tempo e si deve leggere; ritorno 200 ms in uscita.
- **Animazioni ridotte:** niente riempimento, ma il tempo da tenere resta, e la conferma pure.
- **Costo:** medio. Cambia un gesto, non solo l'aspetto: è da decidere prima se Franz lo vuole.

## 7. Anello che si sposta

I componenti sono *Tab pill* e *Segmented control*; mockup `07-anello.html`.

- **Dove:** telefono e web, sul tabellone. L'anello della casella scelta è quello rifatto ieri (`app-tabellone-filtro-25-11`).
- **Cosa fa:** cambiando filtro l'anello scivola da una casella all'altra invece di spegnersi e riaccendersi: si vede da dove vieni e dove sei. Sulla web la plancia ha già una pillola che scorre (`Desk.svelte`).
- **Tempi:** posizione e larghezza con la molla col rimbalzo, 620 ms, circa 5 %: è posizione, quindi il rimbalzo è ammesso. Comparsa e sparizione 150 ms.
- **Animazioni ridotte:** l'anello appare al posto, senza viaggio.
- **Costo:** piccolo. Un solo anello disegnato sopra il tabellone al posto di tre bordi.

## 8. Ok al polso

Il componente è *Loading to success*; mockup `08-orologio.html`.

- **Dove:** orologio, nelle domande e nelle approvazioni.
- **Cosa fa:** i due momenti oggi ci sono già, a parole e con la vibrazione: «Inviato» con un tick, «confermato» con due. Qui li dice il tasto stesso: dopo il Sì diventa un arco che gira finché il PC non conferma, poi una spunta verde, sugli stessi tick; il No sfuma. Una volta sola, niente cicli lunghi.
- **Tempi:** pressione 100 ms; arco 1,2 s lineare finché serve; spunta 250 ms.
- **Animazioni ridotte:** arco fermo, spunta senza tratto. Nell'ambiente a basso consumo non si anima niente.
- **Costo:** piccolo-medio. Lo stato inviato e confermato c'è già in `Notifier.kt`: va portato anche sullo schermo della domanda.

## Riepilogo

| # | Voce | Telefono | Web | Orologio | Costo |
|---|---|---|---|---|---|
| 1 | Copia che risponde | sì | sì | no | piccolo |
| 2 | Lettura che segue la voce | sì | sì | resta al paragrafo | medio |
| 3 | Al lavoro, vivo | sì | sì | no | piccolo |
| 4 | Cifra che scorre | sì | sì | no | piccolo-medio |
| 5 | Passi della prova | sì | sì | no | grande, chiede il relay |
| 6 | Tieni premuto per la produzione | sì | sì | sì | medio, cambia un gesto |
| 7 | Anello che si sposta | sì | sì | no | piccolo |
| 8 | Ok al polso | no | no | sì | piccolo-medio |

Peso: nessuna voce aggiunge librerie. I cicli infiniti nuovi sono due, il riflesso della 3 e l'arco della 5, e girano solo mentre qualcosa lavora davvero. Tutte le altre sono animazioni da un colpo solo, legate a un tocco.

## Opzioni per la carta di scelta

- **A, ritocchi dei gesti:** 1, 3 e 7. Le tre piccole: ogni tocco risponde e chi lavora si vede. Nessun gesto nuovo, nessun dato nuovo.
- **B, A più voce e polso:** 1, 2, 3, 7 e 8. Aggiunge la lettura che segue la parola e l'ok animato al polso: lo stile arriva anche alla voce e all'orologio.
- **C, B più il tabellone che risponde:** 1, 2, 3, 4, 7 e 8. Aggiunge la cifra che scorre per i tuoi gesti: tutto quello che si fa senza cambiare gesti né contratto.
- **D, tutte e otto:** C più la 6 (un gesto nuovo per la produzione) e la 5 (le tappe, che chiedono prima una richiesta al relay).

Proposta: la C. Dà lo stile in ogni punto toccato spesso, con un solo linguaggio di movimento, e lascia fuori solo le due voci che vanno decise a parte. La 6 cambia come si approva, la 5 dipende dal relay.
