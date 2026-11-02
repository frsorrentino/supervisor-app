# Spazio in cima alla home e categorie chiudibili: design e piano

> **Per chi esegue:** superpowers:executing-plans. Passi con `- [ ]`. Compito `app-home-in-cima-02-11`; chiude anche
> `app-categorie-trascina-02-11` e `app-esiti-spostabili-02-11`.

## Intento

Franz (02/11 19:02 e 19:18) vuole mettere ai primi posti voci di categorie diverse: Sessioni sopra Da decidere, Esiti di oggi in alto,
senza smontare le categorie. Sceglie le «voci fissate in cima». Vuole anche che una categoria aperta o chiusa resti come l'ha lasciata.
Criterio delle 16:04: un'interfaccia intuitiva.

## Cosa si costruisce

1. **Spazio in cima**, sopra le categorie e sotto tabellone e «Ti aspettano». Ci si trascina qualsiasi sezione di qualsiasi categoria.
   Lì le voci si ordinano liberamente. Trascinata giù, oltre il bordo dello spazio, la voce torna nella sua categoria.
   Una voce in cima **esce dalla sua categoria**, anche dal segnale della riga chiusa (Franz 19:26).
2. **Categorie chiudibili**: il tocco sulla testata apre e chiude e lo stato si salva (`shut:`). Tocco, riquadri del tabellone e
   interruttore di Riordina sono la stessa impostazione. Sparisce l'apertura «solo per questa visita» (`peek`).
3. **Categorie spostabili** fra loro con la pressione lunga sulla testata (lavoro del ramo `wip/categorie-trascina-02-11`).
4. **Riordina** mostra lo spazio in cima come prima card: ordine con le maniglie, «Togli dalla cima» su ogni voce, «Metti in cima» su
   ogni sezione delle categorie.
5. Telefono, tablet, PWA stretta e larga; demo allineata.

## Forma (scelta di supervisor-app, da dire nel rapporto)

- Telefono e PWA stretta: **nessuna testata**. Le voci in cima sono le prime righe; sotto, lo stacco della prima categoria.
- Spazio vuoto: non si vede. Durante il trascinamento di una sezione compare una striscia tratteggiata «Trascina qui per tenerla in
  cima»; con voci in cima la striscia sta sotto l'ultima.
- PWA larga (plancia a colonne): una fascia a tutta larghezza sopra le colonne, con le voci in cima affiancate.

## Formato salvato (relay invariato, contratto 1.97: ≤ 16 chiavi, ≤ 32 caratteri)

`home_order` = `top:<chiave>` per ogni voce in cima, nell'ordine; poi le chiavi di sezione delle categorie; poi `shut:`; poi `pin:`.
La voce in cima **sostituisce** la sua chiave semplice, quindi il conto resta 13 + 1 + 1 = 15.
Un'app di ieri ignora `top:k` (chiave sconosciuta) e rimette `k` nella sua categoria: nessuna perdita.
Limite noto: se tutte le sezioni disegnate *e* non disegnate di una categoria vanno in cima, la categoria perde il suo posto e torna a
quello predefinito. Non succede con le categorie di oggi: ognuna ha almeno una chiave che non si disegna o più di tre sezioni.

## Modello (`HomeCats.kt` = `homeCats.ts`, stessi casi di test)

- `EDGE = "edge:top"`: il bordo dello spazio in cima nella lista piatta del trascinamento.
- `top(saved): List<String>`: le voci in cima.
- `decode(saved)`: le categorie senza le voci in cima.
- `encode(cats, pin, top = [])`: `null` quando tutto è predefinito e la cima è vuota.
- `flat(top, cats) = top + EDGE + sezioni delle categorie`: l'ordine mostrato e trascinato.
- `settle(cats, flat): Pair<top, cats>`: dopo il rilascio, le voci prima di `EDGE` vanno in cima; le altre tornano nella loro categoria
  nell'ordine del dito. Le chiavi non disegnate tengono il loro posto.
- `dragTarget(order, held, y, mids, present)`: una sezione scavalca le voci in cima, il bordo e le sezioni della sua categoria. Le
  sezioni di altre categorie non contano.
- `toTop(cats, top, key)` e `fromTop(cats, top, key)`: per Riordina. `fromTop` rimette la voce in fondo alla sua categoria.

## Passi

- [ ] 1. Modello e test, Kotlin e TS (TDD): `top`, `decode`, `encode`, `flat`, `settle`, `dragTarget`, `toTop`, `fromTop`, più i
      test del ramo wip (`catKeys`, `catOrder`). Verde su win (`:core:testDebugUnitTest`) e vitest.
- [ ] 2. Telefono, `SummaryList.kt`: voci in cima disegnate prima delle categorie; il bordo come zona di trascinamento (striscia
      durante il gesto); rilascio con `settle`; tocco sulla testata che salva `toggle`; riquadri che aprono salvando; via `peek`.
      Categorie trascinabili dal ramo wip.
- [ ] 3. Telefono, `HomeOrderScreen.kt`: la card «In cima» e le azioni «Metti in cima» e «Togli dalla cima».
- [ ] 4. Web, `Home.svelte`, `HomeOrder.svelte`, plancia larga: le stesse cose.
- [ ] 5. Demo (`demo.ts`) con una voce in cima; `demoAllineata.test.ts`.
- [ ] 6. Test UI: Paparazzi della home con una voce in cima e con la striscia; test web di Home con il trascinamento in cima.
- [ ] 7. Tabella della famiglia (ogni categoria e sezione: pressione lunga, tocco, apertura) nel rapporto; CI golden; pre-release;
      web online; `rimasto` della demo.

## Punti critici da provare

1. Un ordine salvato ieri (senza `top:`) si apre identico.
2. Esiti di oggi, unica sezione disegnata della sua categoria, sale in cima e torna giù.
3. Una voce in cima agganciata allo slot del tabellone: sta nello slot, e quando si sgancia torna in cima.
4. Una sezione trascinata oltre una categoria che non è la sua non ci entra.
5. Chiusa e riaperta dalla testata, una categoria resta così dopo il riavvio dell'app e sull'altro dispositivo.

## Modifica del 02/11 alle 20:45: il tabellone a quattro voci (`app-tabellone-quattro-02-11`)

Franz, sul mockup `richiede-te.png` punto 2: «lascerei il rilascio al tabellone e toglierei l'altro, col rilascio la voce resta
aperta, metterei fino a 4 voci ordinabili».

- La striscia sparisce: l'unico punto di rilascio è il tabellone (`BOARD_MAX = 4`). Il bordo `EDGE` resta, invisibile, e lo
  attraversano solo le voci già sul tabellone, per tornare nella loro categoria (`dragTarget(..., fromBoard)`).
- Le voci sul tabellone stanno subito sotto i tre riquadri, con l'etichetta «In cima» e la puntina, aperte dopo il rilascio.
- Lo slot di una voce sola non si scrive più: un `pin:k` salvato prima, senza voci `top:`, diventa la prima voce del tabellone.
- Con quattro voci il tabellone dice «Tabellone pieno: 4 voci» e il rilascio non cambia niente; in Riordina la freccia su sparisce.
- L'allarme della settimana resta com'era: Utilizzo entra nel riquadro dello slot. Se Utilizzo è già una delle quattro voci, sale
  nel riquadro e sotto restano le altre tre; se non lo è, le quattro voci restano tutte sotto il riquadro. Con l'allarme Utilizzo
  non si trascina.
