# Righe e nomi della home (02/11/2026)

Compiti `app-home-nomi-02-11` (Franz 21:28, passo 2 cambiato alle 21:41) e `app-richiede-te-righe-02-11` (Franz 20:52, mockup
`.claude/proposte/2026-11-02-richiede-te/richiede-te.png`, punti 1, 3 e la regola in fondo). Si fanno insieme: toccano le stesse
righe. Telefono, tablet, PWA stretta e larga, demo.

## Fase A: nomi, fusione, ordine, conteggi

1. Nomi: `you` Tocca a te (icona nuova, non la mano), `work` In corso, `out` Esiti, `tools` Risorse. `strings.xml` (+ `values-en`),
   `t.catName` it/en. Restano «Esiti di oggi» e i nomi delle sezioni.
2. Ti aspettano si fonde con Sessioni: niente più blocco «Ti aspettano»; le sessioni ferme stanno in cima alla lista di Sessioni,
   in ambra; la riga Sessioni (chiusa o aperta) si accende in ambra quando ce n'è una, e Sessioni si apre da sola alla prima
   ferma nuova solo se Franz non l'ha chiusa. Gli ok ai compiti senza sessione (`looseOk`) stanno sotto le ferme, nello stesso
   blocco. Il login resta dov'è.
3. Ordine: categorie Tocca a te, In corso, Esiti, Risorse; dentro, Da decidere, Da provare, Suggerimenti (poi Prima raccolta);
   Utilizzo, Sistema (con Plugin e skill). Una volta sull'ordine salvato: marcatore `rev:2` in `home_order`; senza marcatore
   `HomeCats.decode` rimette categorie e sezioni di Tocca a te e Risorse nell'ordine nuovo, `encode` scrive sempre il marcatore.
   Test Kotlin e TS sugli stessi casi.
4. Riga chiusa: le voci nell'ordine della lista aperta (`catBits`/`catSignal` seguono `c.sections`).
5. Primo riquadro: «N sessioni ferme, la prima da M min» (plurale), non più «ti aspettano».
6. Conteggi: «N aperte» in testata e «Sessioni N» contano le stesse sessioni (non chiuse, ferme comprese); «rossi» anche
   nella riga Esiti (`cat_sig_failed`).

## Fase B: righe e ambra

1. Tocca a te: chiusa, la riga ambra dice le voci che la accendono (Da decidere, Da provare, Suggerimenti con voci); aperta,
   ambra sull'etichetta con l'icona e sulle voci con qualcosa per Franz: numero in pillola ambra e filo ambra a sinistra; le vuote
   grigie. Niente si sposta da solo.
2. Ogni riga della home ha la forma di Sessioni: icona, nome, numero accanto, a destra lo stato in parti separate da « · » (lo
   zero non si scrive), freccia. Ambra solo per quel che chiede Franz.
3. Dentro le sezioni le voci sono righe come le sessioni: pallino di stato, nome, dato breve a destra, sotto una riga che dice
   cosa c'è; il tocco apre la scheda coi tasti come oggi. Famiglia: Da decidere, Da provare, Suggerimenti, Da seguire, Lavori,
   Prima raccolta. Tabella della famiglia nel rapporto.

## Verifica

Test `HomeCatsTest` e `homeCats.test.ts` (migrazione e ordine), Paparazzi della home (telefono, tablet, 320 dp con testo 1,3),
demo web provata con chrome-bridge; pre-release e web online, poi prova di Franz.
