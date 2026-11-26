# Animazioni, seconda proposta

Franz il 26/11 alle 11:50, sulla carta della prima proposta: «ok tutte, proponi altre». Compito `app-animazioni-altre-26-11`.

Otto voci nuove, diverse dalle otto già scelte (copia, lettura, al lavoro, cifra, passi, tieni premuto, anello, ok al polso). Vengono dai componenti di designbookmark.com/motion rimasti fuori e dalle parti dell'app che oggi non si muovono: la chat, la barra della master, «Per te», il visore delle immagini, il campo di scrittura e l'orologio.

Valgono le stesse regole della prima proposta (`docs/proposte/2026-11-26-animazioni/proposta.md`):

- le tre molle di casa e il ciclo di 1,6 s;
- rimbalzo solo su posizione e scala;
- con le animazioni ridotte, solo lo stato finale;
- niente librerie nuove;
- l'orologio solo dove regge la batteria;
- la lettura per blocchi resta selezionabile (vincolo di Franz delle 11:35); nessuna voce la tocca.

I file:

- un mockup animato per voce (`NN-nome.html`) e il suo PNG a 360 px;
- `tutte.html`, un file solo con CSS e JS dentro e gli otto mockup che girano dal vivo;
- `00-tutte.png`, le otto voci in un'immagine.

## 1. Messaggio che entra

Componenti *Streaming reply* e *Notification stack*; mockup `01-messaggio.html`.

- **Dove:** telefono e web, nella chat aperta.
- **Cosa fa:** il messaggio nuovo sale di 8 px e compare, una volta sola e solo lui; la conversazione non si muove. Se stai leggendo più in alto la chat non scorre da sola: compare la pillola «1 nuovo ↓», che ti porta giù.
- **Tempi:** entrata 220 ms in uscita; pillola con la molla di casa.
- **Animazioni ridotte:** dissolvenza di 120 ms, pillola senza scala.
- **Costo:** piccolo-medio: serve sapere se chi legge è in fondo alla chat.

## 2. Barra della master che si allunga

Componente *Morph island*; mockup `02-barra-master.html`.

- **Dove:** telefono e web, sulla barra della master in fondo (`MasterDock`).
- **Cosa fa:** a pannello chiuso, quando arriva una risposta della master, la barra si allunga per 3 s e mostra la prima riga col punto ambra; poi torna pillola e il punto resta. Il tocco sulla barra allungata apre la risposta. È un oggetto che cambia forma, non un avviso in più.
- **Tempi:** forma con la molla di casa (circa 5 %), 620 ms; testo dopo 120 ms, in 150 ms.
- **Animazioni ridotte:** cambio secco, i 3 s restano.
- **Costo:** medio. La barra oggi ha una misura sola; va misurata la prima riga.

## 3. Per te come una pila

Componenti *Fan cards* e *Notification stack*; mockup `03-pila.html`.

- **Dove:** telefono e web, in «Per te» ridotto, nella sessione e in home.
- **Cosa fa:** sotto la prima voce si vedono i bordi delle altre, al massimo due, come carte impilate; al tocco le voci scendono al loro posto. Si capisce che ce ne sono altre senza leggere il numero.
- **Tempi:** apertura e chiusura con la molla di casa, 620 ms.
- **Animazioni ridotte:** apertura secca; i bordi restano, perché dicono qualcosa.
- **Costo:** piccolo-medio.

## 4. Annulla col tempo

Componenti *Undo button* e *Swipe deck*; mockup `04-annulla.html`.

- **Dove:** telefono e web, dopo i gesti che tolgono qualcosa: Non ora e Mai su un suggerimento, Visto su un risultato, chiudere una sessione senza salvare.
- **Cosa fa:** la riga esce e si chiude, poi compare «Annulla» con un anello che si svuota in 5 s; finito l'anello, l'azione parte davvero.
- **Tempi:** uscita della riga 160 ms, chiusura dello spazio 200 ms; avviso 150 ms più la molla veloce; anello 5 s lineare.
- **Animazioni ridotte:** anello fermo, ma i 5 s restano.
- **Costo:** medio. Cambia quando parte il comando al PC: 5 s dopo, non subito. Da decidere con Franz per «chiudi senza salvare».

## 5. Puntini del visore

Componente *Pagination dots*; mockup `05-puntini.html`.

- **Dove:** telefono e web, nel visore delle immagini di una carta o di un invio.
- **Cosa fa:** sotto l'immagine una fila di puntini, quello di adesso allungato a pillola. Si capisce che si scorre di lato e quante immagini restano. Completa `app-carta-immagini-26-11`, che ora mostra tutte le immagini. Oltre 7 immagini si vedono i 7 puntini attorno a quella di adesso.
- **Tempi:** pillola 200 ms con la molla veloce; la pagina segue il dito come oggi.
- **Animazioni ridotte:** la pillola salta senza allungarsi.
- **Costo:** piccolo.

## 6. Suggerimento nel campo

Componente *Ghost suggestion*; mockup `06-suggerimento.html`.

- **Dove:** telefono e web, nel campo della chat quando è vuoto.
- **Cosa fa:** il primo dei passi suggeriti («Prossimi») compare in grigio nel campo. Un tocco sulla freccia, o Tab sulla web, lo scrive nel campo, da correggere prima di mandarlo. Basta una lettera e il grigio sparisce. Non manda niente da solo, e i tasti dei passi restano.
- **Tempi:** grigio 150 ms; testo che si scopre in 180 ms in uscita.
- **Animazioni ridotte:** il testo compare intero.
- **Costo:** piccolo-medio.

## 7. Limite ad anello

Componente *Letter limit*; mockup `07-limite.html`.

- **Dove:** telefono e web, nei campi col limite: il motivo di «Nessuna, rifare» (300 caratteri) e la nota a un risultato (2000).
- **Cosa fa:** un anello piccolo si riempie mentre scrivi. Negli ultimi 20 caratteri diventa ambra e mostra il numero; oltre il limite diventa rosso. Non blocca la scrittura. Prende il posto di «restano N caratteri».
- **Tempi:** anello 120 ms lineare, dietro al testo; ambra 200 ms con la molla veloce.
- **Animazioni ridotte:** anello senza scatti.
- **Costo:** piccolo.

## 8. Domanda che arriva al polso

Componente *Notification stack*; mockup `08-polso.html`.

- **Dove:** orologio, sulla domanda aperta dalla notifica.
- **Cosa fa:** la domanda sale dal basso e si posa con la molla: prima chi chiede, poi la domanda, poi i tasti, a 40 ms l'uno dall'altro, insieme alla vibrazione che c'è già. Una volta sola all'arrivo, mai a schermo spento.
- **Tempi:** salita con la molla di casa, 620 ms; scarto di 40 ms.
- **Animazioni ridotte:** dissolvenza di 120 ms. Nell'ambiente a basso consumo nessuna animazione.
- **Costo:** piccolo.

## Riepilogo

| # | Voce | Telefono | Web | Orologio | Costo |
|---|---|---|---|---|---|
| 1 | Messaggio che entra | sì | sì | no | piccolo-medio |
| 2 | Barra della master che si allunga | sì | sì | no | medio |
| 3 | Per te come una pila | sì | sì | no | piccolo-medio |
| 4 | Annulla col tempo | sì | sì | no | medio, cambia quando parte il comando |
| 5 | Puntini del visore | sì | sì | no | piccolo |
| 6 | Suggerimento nel campo | sì | sì | no | piccolo-medio |
| 7 | Limite ad anello | sì | sì | no | piccolo |
| 8 | Domanda che arriva al polso | no | no | sì | piccolo |

Nessun ciclo infinito nuovo: sono tutte animazioni da un colpo solo, legate a un arrivo o a un tocco. L'anello dell'Annulla dura 5 s e si ferma.

## Opzioni per la carta di scelta

- **A, si capisce che c'è altro:** 3, 5 e 7. Pila, puntini e anello dicono «ce ne sono altri» e «quanto manca»; tutte piccole.
- **B, A più gli arrivi:** 1, 3, 5, 7 e 8. Aggiunge il messaggio che entra e la domanda al polso: quello che arriva si vede arrivare.
- **C, B più barra e suggerimento:** 1, 2, 3, 5, 6, 7 e 8. Tutto tranne l'Annulla.
- **D, tutte e otto:** C più l'Annulla, che cambia quando parte il comando al PC.

Proposta: la C. L'Annulla merita una scelta a parte, perché ritarda di 5 s azioni che oggi partono subito.
