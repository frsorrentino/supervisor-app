# Animazioni, terzo giro

Le idee di Franz del 26/11 alle 14:49, guardando di nuovo designbookmark.com/motion, con il consiglio della master. Compito `app-animazioni-terzo-giro-26-11`.

Ogni voce ha un mockup animato (`NN-nome.html`) e il suo PNG a 360 px. `tutte.html` è un file solo, con CSS e JS dentro e le nove voci che girano dal vivo; `00-tutte.png` le mostra tutte in un'immagine. Nessun nome di file comincia con «_», così la cartella si apre da un link. Il mockup «Copy button» non c'è: la Copia che risponde è già nell'app, voce 1 del primo giro.

## Regole

Le regole sono quelle dei primi due giri:

- le tre molle di casa e il ciclo di 1,6 s;
- rimbalzo solo su posizione e scala;
- con le animazioni ridotte, solo lo stato finale;
- niente librerie nuove;
- la lettura per blocchi resta selezionabile;
- nei testi «scheda», mai «carta».

Ogni voce dice anche cosa serve dal relay.

## 1. Citare una sessione con @

Componente *Mention picker*; mockup `01-citare.html`.

- **Dove:** telefono e web, nel campo della master e in quello di ogni sessione.
- **Cosa fa:**
  - «@» apre l'elenco delle sessioni sopra il campo; l'elenco si filtra mentre scrivi e mostra lo stato di ciascuna.
  - La sessione scelta diventa una pillola nel campo.
  - Mandato, il testo va alla sessione citata come un prompt dal telefono. Con due citazioni va a tutte e due.
  - Nella chat da cui scrivi resta una riga «→ supervisor-app».
  - Il tocco sulla pillola apre la sessione; la ✕ della pillola la toglie.
- **Tempi:** elenco 150 ms con la molla veloce; pillola con la molla di casa.
- **Animazioni ridotte:** comparsa secca.
- **Relay:** niente di nuovo, il prompt a una sessione c'è già.
- **Costo:** medio.

## 2. Codice di abbinamento a 6 caselle

Componente *OTP code*; mockup `02-codice.html`.

- **Dove:** telefono e web, nell'abbinamento di un dispositivo.
- **Cosa fa:** quando il QR non si legge, il codice si scrive in sei caselle.
  - Il fuoco passa da solo alla casella dopo e la cancellazione torna indietro.
  - L'incolla riempie tutte e sei.
  - Giusto, le caselle diventano verdi; sbagliato, scuotono una volta e si svuotano.
- **Tempi:** cifra 200 ms con la molla veloce; scossa 320 ms, una volta.
- **Animazioni ridotte:** niente scossa, resta il rosso.
- **Relay:** da chiedere. Oggi l'abbinamento passa da QR e link: serve che `relay pair --add` mostri anche un codice a 6 cifre e che il relay lo accetti.
- **Costo:** medio, più il relay.

## 3. Passi della chiusura sul filo

Componente *Agent steps*; mockup `03-chiusura.html`.

- **Dove:** telefono e web, nella schermata di chiusura di una sessione.
- **Cosa fa:** i passi che oggi sono una lista con ○ e ✓ (salvataggio, attesa che la sessione esca, chiusura) stanno sul filo delle tappe della prova. È lo stesso componente: un solo linguaggio per «sta succedendo una cosa a tappe».
- **Tempi:** filo 400 ms, spunta 250 ms, arco 1,2 s.
- **Animazioni ridotte:** tappe accese senza movimento.
- **Relay:** niente di nuovo, i passi li dice già la chiusura.
- **Costo:** piccolo, il componente c'è.

## 4. Il nostro campo di testo

Componente *Prompt box*; mockup `04-campo.html`.

- **Dove:** telefono e web, il campo di scrittura.
- **Cosa fa:** il campo di oggi con tre movimenti.
  - Cresce con il testo fino a cinque righe senza scatti, e gli angoli si fanno meno tondi quando è alto.
  - Gli allegati entrano come miniature sopra il testo.
  - L'invio passa da grigio a pieno quando c'è qualcosa da mandare; al tocco la freccia parte verso l'alto.
- **Tempi:** altezza 200 ms con la molla veloce; miniature con la molla di casa; invio 150 ms, freccia 260 ms.
- **Animazioni ridotte:** senza movimento.
- **Relay:** niente.
- **Costo:** piccolo-medio.

## 5. Velocità della voce

Componente *Jelly slider*, in versione sobria; mockup `05-velocita.html`.

- **Dove:** telefono e web, nel cursore della velocità (da 0,5× a 2×, scatto su 1×).
- **Cosa fa:** una molla sobria, non gommosa.
  - La manopola si allarga un poco mentre la tieni.
  - Il numero batte a ogni passo.
  - Vicino a 1× si aggancia con un mezzo scatto e la vibrazione leggera.
  - Niente deformazioni: si deve leggere il numero.
- **Tempi:** manopola 200 ms con la molla veloce; aggancio con la molla di casa; numero 120 ms.
- **Animazioni ridotte:** niente movimento.
- **Relay:** niente.
- **Costo:** piccolo.

## 6. File trascinati nella web

Componente *File drop*; mockup `06-trascina.html`.

- **Dove:** solo la web. Sul telefono i file arrivano dal + e dalla condivisione.
- **Cosa fa:** il trascinamento sulla chat c'è già («Lascia qui»).
  - Il velo entra in 150 ms invece di comparire, e dice quanti file e per chi.
  - Al rilascio i file volano nel campo e diventano miniature, da togliere prima di mandare se serve.
- **Tempi:** velo 150 ms con la molla veloce; volo con la molla di casa.
- **Animazioni ridotte:** velo in dissolvenza, miniature senza volo.
- **Relay:** niente.
- **Costo:** piccolo.

## 7. Ricerca come palette di comandi

Componente *Command palette*; mockup `07-palette.html`.

- **Dove:** web, con le frecce e Invio, e telefono, col tocco; in «Cerca».
- **Cosa fa:** scrivendo, sopra i risultati compaiono le azioni con il nome della sessione già dentro: Lancia, Riapri, Chiudi, Vai a. Riapri e Chiudi seguono le regole di issue #4: niente Riapri su una sessione aperta, niente Chiudi su una in uso.
- **Tempi:** si apre e si chiude spesso, quindi niente animazioni lunghe: 200 ms all'apertura e il fondo della riga scelta che scivola con la molla veloce. Il filtro non si anima.
- **Animazioni ridotte:** niente.
- **Relay:** niente di nuovo.
- **Costo:** medio.

## 8. Lettura a voce con lo scrubber

Componenti *Read along* e *Range slider*; mockup `08-scrubber.html`.

- **Dove:** telefono e web, nella pillola della lettura.
- **Cosa fa:** oggi la pillola dice cosa legge, non a che punto è.
  - Una barra con una tacca per paragrafo avanza mentre la voce legge.
  - Trascinandola, un'etichetta dice a quale paragrafo arrivi; al rilascio la lettura riparte da lì.
  - È un altro modo di scegliere il blocco da leggere: il tocco su un paragrafo in chat resta.
- **Tempi:** avanzamento lineare; manopola 200 ms con la molla veloce; etichetta 150 ms.
- **Animazioni ridotte:** manopola senza movimento.
- **Relay:** niente.
- **Costo:** medio.

## 9. Tieni premuto per Play

Componente *Hold button*; mockup `09-play.html`.

- **Dove:** telefono, web e orologio, sull'approvazione che pubblica su Google Play.
- **Cosa fa:** il tieni premuto c'è già sulle approvazioni di produzione. Qui si estende solo a ciò che non si può annullare: la pubblicazione su Play. Dove c'è l'Annulla di 5 s non si aggiunge, perché due protezioni sullo stesso gesto sarebbero di troppo.
- **Tempi:** come la voce 6 del primo giro: riempimento 600 ms lineare, ritorno 200 ms.
- **Relay:** serve un segno «play» sull'approvazione; oggi c'è solo `deploy`. Se Play è già sempre `deploy`, non serve niente.
- **Costo:** piccolo.

## Riepilogo

| # | Voce | Telefono | Web | Orologio | Relay | Costo |
|---|---|---|---|---|---|---|
| 1 | Citare una sessione con @ | sì | sì | no | niente | medio |
| 2 | Codice a 6 caselle | sì | sì | no | sì: il codice | medio + relay |
| 3 | Passi della chiusura sul filo | sì | sì | no | niente | piccolo |
| 4 | Il nostro campo | sì | sì | no | niente | piccolo-medio |
| 5 | Velocità della voce | sì | sì | no | niente | piccolo |
| 6 | File trascinati | no | sì | no | niente | piccolo |
| 7 | Palette di comandi | sì | sì | no | niente | medio |
| 8 | Scrubber della lettura | sì | sì | no | niente | medio |
| 9 | Tieni premuto per Play | sì | sì | sì | forse il segno «play» | piccolo |

## Opzioni per la scheda di scelta

- **A, i ritocchi:** 3, 4, 5 e 6. Componenti che ci sono già, resi più chiari; niente relay.
- **B, A più la lettura:** 3, 4, 5, 6 e 8. Aggiunge lo scrubber, che è un altro modo di scegliere il blocco da leggere.
- **C, B più le funzioni nuove:** 1, 3, 4, 5, 6, 7 e 8. Aggiunge la citazione con @ e la palette dei comandi; niente relay.
- **D, tutte e nove:** C più il codice a 6 caselle, che chiede il relay, e il tieni premuto per Play.

Proposta: la C. Porta le due funzioni nuove che Franz ha chiesto (citare una sessione, i comandi dalla ricerca) senza aspettare il relay; il codice a 6 caselle va chiesto prima a win-supervisor.
