# Giudizio di design sui quattro concept della master (06/10/2026)

**Vince «carbone».** È l'unico che risponde alla domanda di Franz senza cambiargli la domanda: tiene la struttura che ha chiesto e approvato (linguetta un passo più chiara, fondo che prosegue intorno al campo, sfondo della chat dello stesso fondo) e cambia solo la cosa che non lo convinceva, il colore. Il marrone diventa il grigio caldo di claude.ai, e il corallo si riduce a una firma. Gli altri tre o cambiano materiale in modo che Franz quasi certamente rifiuterà (carta), o si appoggiano a effetti che nella resa non arrivano (profondità), o spostano l'identità sulla forma, pagando con il badge di stato e con il rumore delle linee (firma).

Immagini guardate: `concept-base.png`, i due JPG dal telefono di Franz in `docs/segnalazioni/`, i quattro `concept-<k>.png`, `concept-firma-bn.png`, `campioni-carta-sera.png`, più un confronto affiancato delle cinque basi chiuse e delle cinque pagine aperte, ricavato dagli `shot-*.png`. Colori campionati sui pixel delle rese (sotto).

| Resa | Sfondo chat al centro | Linguetta | Fondo intorno al campo | Pillola «ti aspetta» |
|---|---|---|---|---|
| base (B terracotta) | (0,0,0) nero | (54,34,26) | (36,22,15) | (41,32,17) |
| carbone | (41,39,35) | (54,51,46) | (41,39,35) | (75,64,46) |
| carta | (214,205,184) | (224,217,198) | (214,205,184) | (232,193,127) |
| profondità | **(17,15,13)**, quasi nero | (64,59,56) | (31,29,26) | (71,60,43) |
| firma | (28,27,26), ai lati (0,0,0) | (42,41,39) | (28,27,26) | (64,54,38) |

## Punteggi (1-10)

Criteri: R = riconoscibile a colpo d'occhio, chiusa e aperta · B = bellezza, gusto, misura · C = coerenza con l'app e con i colori di stato · L = leggibilità · A = probabilità che Franz lo accetti.

| Concept | R | B | C | L | A | Totale |
|---|---|---|---|---|---|---|
| **carbone** | 7 | 8 | 8 | 9 | 7 | **7,8** |
| firma | 8 | 6 | 5 | 8 | 5 | 6,4 |
| profondità | 5 | 6 | 6 | 8 | 5 | 6,0 |
| carta | 10 | 6 | 4 | 7 | 2 | 5,8 |

### carbone: 7,8

- **Forte.** Da aperta è la differenza più netta tra quelle credibili: tutta la pagina passa dal nero a un grigio caldo uniforme, quindi lo sfondo della chat è allineato davvero e non solo sui bordi. Il problema del «panino» della B (marrone sopra e sotto, nero in mezzo) è risolto in modo pulito.
- **Forte.** Il gradino tra linguetta e fondo si vede nella resa: (54,51,46) contro (41,39,35). È proprio la «differenza lieve ma visibile» che Franz ha chiesto.
- **Forte.** È l'unico concept in cui il corallo ha un solo ruolo. Non compete con «PRODUZIONE», col rosso di «Chiudi» o con l'ambra.
- **Forte.** Su Android il costo è il più basso dei quattro: colori piatti, nessuna ombra disegnata, nessun gradiente.
- **Debole.** Da chiusa il riconoscimento è il punto più fragile. È un blocco a tutta larghezza più chiaro e più caldo delle card, ma sul telefono, con la luminosità bassa, può leggersi come «un'altra card grigia». La scintilla da 13 px è troppo piccola per reggere l'identità da sola.
- **Debole.** La pastiglia «Casa» (`--m-high`) ha lo stesso colore e la stessa forma arrotondata della bolla di Franz: sembra un messaggio o un chip, non un link.
- **Debole.** La scintilla in fondo a ogni risposta, in testa alla riga copia / ▶ / segnalibro, si legge come un'altra azione da toccare.
- **Debole.** La riga mono della linguetta finisce con «Opus 5.5 ·», con un punto appeso. Viola la regola «una riga logica occupa la riga fisica».

### firma: 6,4

- **Forte.** È l'identità più robusta in assoluto. La prova in bianco e nero regge, e la cornice a contorno è una forma che nessun'altra parte dell'app usa.
- **Forte.** Da aperta, il foglio rientrato con i margini neri si riconosce subito. Le risposte appese all'asse della scintilla hanno una griglia tipografica vera.
- **Debole.** Toglie il badge di stato, e il suo glifo con lui (pausa, fulmine, mano). In tutta l'app il badge è la lingua dello stato. Sostituirlo con una scintilla avorio da 24 px più un punto che da ferma sparisce è una regressione funzionale, non una scelta di stile.
- **Debole.** Ci sono troppe linee: la cornice del foglio, la cornice del campo, il filetto sotto ogni risposta e la tacca. La tacca con la scintilla sul bordo del campo sembra un bordo rotto, oppure la «scintilla magica» delle app di IA. È la cosa meno elegante delle quattro proposte.
- **Debole.** Il rientro di 6 px costa larghezza a un titolo già tagliato: «claude-mas» invece di «claude-maste». Nella riga mono compare una «(» isolata.
- **Debole.** La grafite quasi neutra (28,27,26) sta alla stessa luminosità delle card. Senza la cornice non si distinguerebbe, e Franz ha già detto di volere un fondo, non un contorno: il «filo corallo» è stato superato proprio per questo.

### profondità: 6,0

- **Forte.** È l'unico concept che risponde alla frase di Franz «mi aspettavo un effetto di luce sulla tab»: un filo di luce sul bordo alto, con un riflesso che entra una volta sola. È un'idea giusta.
- **Forte.** La linguetta (64,59,56) è la più chiara e la più «sopra» delle quattro scure.
- **Debole.** Da aperta il centro della chat è (17,15,13). A colpo d'occhio è nero come le altre sessioni, e la vignettatura lo rende anche torbido. Manca proprio il punto che Franz ha chiesto oggi, lo sfondo della chat allineato.
- **Debole.** L'ombra verso l'alto spegne il testo verde «Obiettivo…» della card sotto: c'è un'informazione di stato resa illeggibile per un effetto.
- **Debole.** Gradienti, vignettatura, ombre interne, ombre disegnate verso l'alto e un letto del campo staccato a labbro sono tanto lavoro su Compose per un risultato che nel PNG quasi non si vede. Il documento stesso dice «va giudicato sul telefono», che è un segnale d'allarme. Su OLED il gradiente da L 0,17 a 0,25 rischia il banding.
- **Debole.** Mescola le temperature: superfici calde con «Casa», ▶ e frecce rimasti blu.

### carta: 5,8

- **Forte.** Il riconoscimento è da 10: non si sbaglia mai, né chiusa né aperta. L'analisi è la migliore del gruppo (la luminosità come canale primario) e va tenuta come principio.
- **Debole.** Franz ha rifiutato il corallo pieno perché «troppo acceso». Una pagina intera che fa 23 volte la luce di oggi, in un'app che usa di sera, è lo stesso rifiuto moltiplicato. La variante sera (`campioni-carta-sera.png`) è color cartone o cachi: peggio, non meglio.
- **Debole.** Per stare sulla carta deve rifare tutti i colori di stato (ambra, verde, blu e rosso scuriti) e mettere un anello d'inchiostro ai badge. La master diventa l'unico posto dell'app dove gli stati hanno un altro aspetto, e questo rompe la coerenza che i vincoli chiedono di proteggere. La pillola ambra diventa un post-it giallo acceso, la cosa più forte della pagina.
- **Debole.** Sembra un tema chiaro rimasto acceso per errore. Va aggiunto il costo della barra di sistema chiara e del passaggio di tutta la UI a tavolozza a scopo.

## Innesti sul vincitore (carbone)

1. **Da profondità: la luce sulla linguetta, ma ferma e piatta.** Il filo interno in alto passa dal 7 % al 14-16 % di avorio, lungo la curva degli angoli, così la linguetta ha un bordo di luce che si vede anche sul telefono. Facoltativo: il riflesso che entra una volta sola (700 ms, solo quando la linguetta compare, fermo al centro col movimento ridotto). È la risposta diretta al «mi aspettavo un effetto di luce sulla tab», senza alone e senza stato. Niente gradiente radiale, niente vignettatura.
2. **Da profondità: un'ombra sola verso l'alto sul blocco chiuso**, `0 -10px 24px -8px` nero al 60 %, non tre livelli. Basta a far sembrare il blocco sopra la lista quando le card ci scorrono sotto, senza spegnere il testo della card visibile. È anche la leva che manca a carbone per il riconoscimento da chiusa.
3. **Da firma: la scintilla delle risposte sull'asse della scintilla della linguetta**, una sola per risposta, in testa al testo e non nella riga delle azioni, senza il filetto verticale. Toglie l'ambiguità «è un tasto?» e dà alla chat della master la sua griglia.
4. **Da firma: «Casa» come link avorio o sabbia, sottolineato di 1 px a 4-5 px di distanza**, con area di tocco da 44 px. Così non ha più la forma della bolla di Franz.
5. **Da firma: la prova in bianco e nero.** Va fatta anche su carbone, perché è la verifica più onesta del riconoscimento da chiusa. Se in grigi il blocco non si stacca dalle card, si applica l'innesto 2 e, se serve, il 6.
6. **Da carta: il principio della luminosità.** Se al polso o sul telefono la master chiusa risulta timida, si alza la linguetta di un gradino di L (verso `#3C3833`, L circa 0,345) lasciando il fondo dov'è, invece di aggiungere tinta. Il gradino tra linguetta e fondo cresce insieme alla distanza dalle card.
7. **Da firma: la scintilla della linguetta un po' più grande**, da 13 a 15-16 px, sempre in corallo. Non i 24 px avorio al posto del badge: il badge resta.

## Da correggere prima di mostrarlo a Franz

1. **Riga mono della linguetta.** Togliere «ctx» dalla linguetta chiusa e aperta (sta già nella testata della master aperta): «✻ MASTER · 12:50 · Opus 5.5» deve stare intera nei 412 dp, senza punto appeso. Carbone lo propone come rischio: va fatto, non è facoltativo.
2. **La scintilla nella riga delle azioni va spostata** (innesto 3) o tolta. Oggi sembra un quarto tasto.
3. **«Casa»** va staccato dalla forma della bolla (innesto 4).
4. **Va verificata la pillola ambra sul carbone** (75,64,46). Nella resa è un oro oliva un po' torbido ed è la cosa più vicina al vecchio marrone che resti in pagina. Va guardata sul telefono. Se si legge come «marroncino», si porta l'ambra della pillola dal 16 % a una base più fredda o si alza il testo «Rispondi», senza toccare lo stato.
5. **La prova vera è sul Pixel, non sul PNG.** Il grigio caldo con chroma 0,008 su OLED può andare in due direzioni: «fango» oppure «grigio spento». Vanno preparate già pronte le due varianti del documento («più caldo» C 0,013 e «quasi neutro» C 0,004), da cambiare con un solo token, per non rifare il giro se Franz dice «troppo grigio» o «ancora marrone».
6. **Il corallo sul campo.** Il 06/10 alle 09:45 Franz aveva voluto il corallo sul campo, e carbone lo mostra solo a fuoco. Va detto esplicitamente nel messaggio a Franz, perché non lo scopra come «mi manca il colore di Claude».
7. **La rotazione della scintilla solo in `busy`**, mai in `awaiting`, e ferma col movimento ridotto, sia sul web sia su Android (`MasterSpark` condivisa con la riga «sta pensando»).
8. **La tavolozza a scopo su Android** (`LocalCmPalette`) è il pezzo grosso del lavoro: una dozzina di composable in `SessionSheet.kt`. Va pianificata come passo a sé, con i golden Paparazzi della home e della master aperta da registrare di nuovo.
9. **La barra di navigazione Android** sotto il blocco chiuso e la master aperta va nel colore `--m-bed`, non nero, sennò il blocco unico si spezza al bordo dello schermo (nella foto delle 18:58 della B si vede la fascia).

## In una riga per Franz

Carbone: la master diventa il grigio caldo di claude.ai, linguetta un passo più chiara, chat dello stesso fondo, corallo solo nella scintilla. Le alternative più spinte (carta chiara, cornice, profondità) si riconoscono di più o di meno, ma ognuna paga un prezzo che lui ha già rifiutato.
