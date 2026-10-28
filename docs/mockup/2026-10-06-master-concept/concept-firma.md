# Concept «firma»: la master si riconosce dal segno, non dal colore

Resa sulla web app vera, 412×915 a densità 2: `concept-firma.png` (chiusa e aperta affiancate), `shot-firma-home.png`, `shot-firma-open.png`; prova in bianco e nero: `concept-firma-bn.png`. CSS: `concept-firma.css`.

## 1. L'idea

**La master è un foglio grafite con una sola cornice e la scintilla ✻ di Claude come firma: lo stesso oggetto da chiusa (spunta dal fondo della home) e da aperta (tirato su a tutta pagina).**

### Perché si riconosce a colpo d'occhio

- **Chiusa.** Nella home ogni sessione è una card piena grigio-blu senza bordo, con un badge colorato pieno (tondo o quadrato). La master è l'unica cosa **disegnata a contorno**: un cassetto rientrato di 6 px dai bordi dello schermo, cornice di 1,5 px sopra e ai lati, aperto in basso. Al posto del badge c'è un **segno a tratto**, la scintilla ad otto raggi. Una forma diversa e un glifo diverso: l'occhio li separa prima ancora di leggere la parola MASTER.
- **Aperta.** Le altre sessioni aperte occupano lo schermo da bordo a bordo su fondo nero. La master resta un **foglio dentro lo schermo**: la stessa cornice scende lungo i due lati fino in fondo, il nero si vede ai margini. Lo sfondo della chat è il fondo del foglio, lo stesso intorno al campo: non c'è più il nero che a metà spezzava l'oggetto. Le risposte di Claude sono appese a una scintilla, sullo stesso asse verticale della scintilla della linguetta (x = 26), e il testo parte dalla stessa colonna del titolo della linguetta (x = 50). Si vede una griglia propria.
- **In bianco e nero regge.** Nella variante B il fondo terracotta e una card grigio-blu, ridotti a grigio, differiscono di **1,06:1**: in pratica sono lo stesso grigio. Qui la cornice sta a **4,81:1** sulla card e la scintilla a **12,99:1** sulla linguetta. In `concept-firma-bn.png` la master si distingue anche senza colore.

### Perché risolve quello che non andava nella terracotta B

1. **Era un colore che sembrava uno stato.** Il corallo #D97757 (tinta 15°) e la terracotta #36221A (17°) stanno fra il rosso di errore e produzione (#E5736B, 4°) e l'ambra di «ti aspetta» (#FFB020, 39°). Una barra calda in fondo alla home sembrava un avviso; la riga MASTER in #E2A58C (17°) aveva la tinta di un'etichetta d'errore attenuata. Qui le superfici della master hanno saturazione del 3-5%: non hanno la tinta di nessuno stato, quindi ambra, blu e rosso dentro la master significano solo lo stato.
2. **Marrone su nero, accanto al grigio-blu.** Una superficie scura e satura (35-41% in HSL) su OLED si legge come ruggine o marrone «sporco». Messa accanto alle card blu-grigie (tinta 218°, quasi complementare) sembrava una toppa di un'altra app. La grafite qui ha la stessa luminosità delle card (L* 9,8 contro 11,6) ma niente tinta: è un altro materiale, non un'altra temperatura.
3. **L'identità stava tutta nel colore.** Basta che il colore non piaccia e la master sparisce, ed è quello che è successo. Qui il colore è quasi neutro e il riconoscimento lo fanno la forma (la cornice), il segno (la scintilla) e la griglia: si può ritoccare la tinta senza perdere l'identità.
4. **Tutta la pagina in terracotta sarebbe stata pesante.** Allargare il marrone allo sfondo della chat, come chiede Franz, voleva dire leggere testi lunghi su una pagina marrone, con la pillola ambra «ti aspetta» nella stessa famiglia di tinte. La grafite regge la lettura lunga (avorio su foglio a 15,37:1) e lascia l'ambra da sola.

## 2. Palette

Token nuovi, tutti `--m-*`. Dentro la master i token dell'app vengono rimappati su questi: blu e grigio-blu non entrano nel foglio.

| Token | Hex | L* | Ruolo |
|---|---|---|---|
| `--m-tab` | `#2A2927` | 16,6 | linguetta (un gradino sopra il foglio) |
| `--m-bed` | `#1C1B1A` | 9,8 | il foglio: fondo del blocco, della chat e intorno al campo |
| `--m-sunk` | `#121211` | 5,4 | incassi: campo, gruppi di passaggi, Prossimi |
| `--m-raise` | `#33322F` | 20,8 | rilievi: bolle di Franz, pillola del modello, tasto invia spento |
| `--m-key` | `#403E3A` | 26,3 | i due tasti tondi sulla linguetta |
| `--m-rule` | `#3D3B37` | 24,9 | filetti: separatori, il filo sotto la scintilla (decorativo) |
| `--m-frame` | `#8F8A80` | 57,6 | la cornice: blocco e campo, la stessa penna |
| `--m-ink` | `#F5F2EA` | 95,5 | inchiostro avorio: testo, scintilla, icone |
| `--m-ink2` | `#B9B4A8` | 73,4 | inchiostro secondario: riga MASTER, ore, segnaposto |

Fuori dalla tabella: `#6E6A62` per i puntini dei passaggi e `#4A4842` per la traccia degli anelli di quota. Sono decorativi e restano sotto la soglia di contrasto di proposito.

### Contrasti WCAG (calcolati con Python, formula WCAG 2.x)

| Coppia | Testo / segno | Fondo | Rapporto | Soglia |
|---|---|---|---|---|
| titolo della linguetta | `#F5F2EA` | `#2A2927` | **12,99:1** | 4,5 |
| riga «MASTER · ora · modello» (mono 12) | `#B9B4A8` | `#2A2927` | **7,03:1** | 4,5 |
| scintilla sulla linguetta | `#F5F2EA` | `#2A2927` | **12,99:1** | 3 |
| icone ▶ ▲ ▼ sui tasti | `#F5F2EA` | `#403E3A` | **9,54:1** | 3 |
| risposte di Claude, «Casa», scintilla delle risposte | `#F5F2EA` | `#1C1B1A` | **15,37:1** | 4,5 |
| ore, «dal telefono», meta | `#B9B4A8` | `#1C1B1A` | **8,32:1** | 4,5 |
| testo nel campo, righe di Prossimi | `#F5F2EA` | `#121211` | **16,76:1** | 4,5 |
| segnaposto, «PROSSIMI · 2» | `#B9B4A8` | `#121211` | **9,07:1** | 4,5 |
| bolle di Franz, pillola del modello | `#F5F2EA` | `#33322F` | **11,46:1** | 4,5 |
| freccia del tasto invia (spento) | `#B9B4A8` | `#33322F` | **6,20:1** | 3 |
| tasto invia pieno (l'unico bottone pieno) | `#1C1B1A` | `#F5F2EA` | **15,37:1** | 4,5 |
| anello quota, tono neutro | `#B9B4A8` | `#1C1B1A` | **8,32:1** | 3 |
| cornice del campo, verso l'interno | `#8F8A80` | `#121211` | **5,46:1** | 3 |
| cornice del campo, verso il foglio | `#8F8A80` | `#1C1B1A` | **5,01:1** | 3 |
| cornice, sulla linguetta | `#8F8A80` | `#2A2927` | **4,23:1** | 3 |
| cornice, verso il nero fuori | `#8F8A80` | `#000000` | **6,12:1** | 3 |
| punto di stato ambra (ti aspetta) | `#FFB020` | `#2A2927` | **7,95:1** | 3 |
| punto di stato blu (al lavoro) | `#7FA1FF` | `#2A2927` | **5,84:1** | 3 |
| punto di stato rosso (chiusa) | `#FF453A` | `#2A2927` | **4,27:1** | 3 |
| pillola «ti aspetta»: testo | `#F5F2EA` | `#40331B` (ambra 16% sul foglio) | **10,99:1** | 4,5 |
| pillola «ti aspetta»: «Rispondi» | `#FFB020` | `#40331B` | **6,73:1** | 4,5 |

Gradini fra superfici, cioè la differenza lieve:

- linguetta e foglio: **1,18:1**, ΔL\* 6,8 (la B aveva ΔL\* 6,6: stesso gradino, senza tinta);
- foglio e campo: 1,09:1, ΔL\* 4,4;
- foglio e nero: 1,22:1, come card e nero (1,27:1).

### Colori di stato e card grigio-blu

- **Nessun colore di stato rubato.** La master usa solo grafite (saturazione 3-5%) e avorio. Ambra, blu e rosso compaiono dentro la master solo come stato: la pillola «ti aspetta», il filetto ambra della riga bloccante in Prossimi, il punto sulla scintilla, i gruppi di passaggi falliti (`--steps-fail` resta rossastro). Su un fondo senza tinta si leggono meglio che sulla terracotta.
- **Il corallo esce dalla master.** Anche la scintilla «sta lavorando» del feed (`--opus`) dentro la master diventa avorio. Il corallo resta il colore del modello Opus fuori dalla master.
- **Convivenza con le card.** Il foglio ha la luminosità delle card (L\* 9,8 contro 11,6), quindi la home non diventa a macchie. La differenza sta nella forma: card piene senza bordo con badge colorato da una parte, un foglio a contorno con un segno a tratto dall'altra. Le card non si toccano.

## 3. Specifica per elemento

Misure in px web = dp Android.

### Chiusa (in fondo alla home)

| Elemento | Specifica |
|---|---|
| **Blocco** (`.pane > .closed`) | Fondo `--m-bed`. Rientrato di 6 dai bordi dello schermo. Cornice 1,5 `--m-frame` sopra e ai lati, niente sotto: il foglio scende oltre il bordo. Raggio 26 in alto. |
| **Linguetta** (`.dock`) | Fondo `--m-tab`, raggio interno 24,5 (26 − 1,5), niente filetto in alto, padding 10 / 10 / 9 / 14. |
| **Badge → scintilla** | Il badge di stato sparisce. Al suo posto la scintilla a otto raggi, 24 × 24, avorio. È la stessa geometria della stellina che già gira nel feed quando Claude lavora (raggi da 0,25 a 1 del raggio, tratto 2 su 18, punte tonde). |
| **Stato sulla scintilla** | Un punto di 10 px con bordo di 2 px in `--m-tab`, in basso a destra della scintilla: ambra `--wait` se ti aspetta, blu `--busy` se lavora, rosso `--gone` se è chiusa (con la scintilla al 50%). Da ferma, nessun punto: la scintilla da sola vuol dire «calma». Il CSS legge lo stato dall'`aria-label` del badge nascosto. |
| **Riga MASTER** | Mono 12, `--m-ink2` (7,03:1). Niente più corallo. |
| **Titolo** | 16 / 600, `--m-ink` (12,99:1). |
| **Tasti ▶ ▲** | Tondi da 44, fondo `--m-key`, icona `--m-ink` (9,54:1). Tonali, non pieni. |
| **Fondo intorno al campo** | `--m-bed`, lo stesso del blocco. Barra a 12 / 10 / 12. |
| **Campo** (`.field.master`) | Fondo `--m-sunk` (incassato nel foglio), cornice 1,5 `--m-frame`: è la stessa penna della cornice del blocco. Con lo stato attivo la cornice diventa `--m-ink`. Sul tratto dritto del bordo superiore c'è una **tacca** di 22 × 15 nel colore del foglio con dentro una scintilla da 13: è l'etichetta del campo come nei campi Material a contorno, centrata sulla colonna del testo (x = 50). Segnaposto in `--m-ink2` (9,07:1), «+» e «Usa» in avorio. |
| **Riga sotto il campo** | «Invio manda · Maiusc+Invio a capo» (solo da scrivania) in `--m-ink2` tramite `--text2`. Sul telefono non c'è, come oggi. |

### Aperta (pagina intera)

| Elemento | Specifica |
|---|---|
| **Foglio** (`.slide`) | La stessa forma della chiusa: rientro di 6, cornice 1,5 sopra e ai lati fino in fondo, raggio 26, fondo `--m-bed`. |
| **Linguetta in cima** | Identica alla chiusa, con ▼. Il gradino linguetta/foglio (1,18:1) è l'unica differenza di fondo in tutta la pagina. |
| **Riga modello / quota** (`.chat > header`) | Senza fondo (si vede il foglio), filetto sotto in `--m-rule`. Pillola del modello in `--m-raise`. Anelli di quota col tono neutro in `--m-ink2` su traccia `#4A4842`; avviso e allarme restano ambra e rosso. |
| **Pillola «ti aspetta»** | Invariata: è uno stato e resta ambra (testo 10,99:1, «Rispondi» 6,73:1). |
| **Sfondo della chat** (`.lines`) | `--m-bed`: lo stesso fondo del campo. Dalla linguetta al bordo inferiore è un foglio solo. |
| **Risposte di Claude** (`.claude`) | **Appese a una scintilla.** Scintilla avorio da 15 sull'asse x = 26, lo stesso della scintilla della linguetta. Sotto, un filo di 1 px in `--m-rule` scende fino alla fine del testo, esclusa la riga delle azioni. Il testo parte da x = 50, la colonna del titolo della linguetta. Voce propria: `--m-ink`, interlinea 1,6 (oggi 1,5), spaziatura 0,012 em (oggi 0,03), `text-wrap: pretty`. Le azioni (copia, ▶, segnalibro, ora) sono allineate al testo. |
| **Bolle di Franz** | Stessa forma (20 / 20 / 6 / 20), fondo `--m-raise`, testo `--m-ink` (11,46:1). |
| **Gruppi di passaggi** | Fondo `--m-sunk`, puntini `#6E6A62`. I gruppi falliti restano rossastri. |
| **Prossimi** (`.box`) | Fondo `--m-sunk`, bordo e separatori `--m-rule`, testo avorio (16,76:1). Il filetto ambra della riga bloccante resta. |
| **Casa** | Avorio con sottolineatura di 1 px in `--m-frame`, staccata di 5 px. Senza il blu serviva un segno che dica «si tocca». |
| **Campo e suo fondo** | Come da chiusa: barra sul foglio, campo incassato a cornice, scintilla nella tacca. Quando c'è qualcosa da mandare il tasto invia è pieno avorio con freccia grafite: resta l'unico bottone pieno della schermata. |

### Movimento

- **Scatto della firma.** Quando la linguetta compare (si apre o si chiude la master), la scintilla ruota da −45° a 0° in **420 ms**, `cubic-bezier(.2, .8, .2, 1)`. A otto raggi, 45° riportano la stessa figura: si percepisce un piccolo scatto, come un sigillo girato, senza cambiare forma. Si somma al movimento di salita che c'è già (300 ms).
- **Al lavoro.** La scintilla della linguetta gira piano, **un giro ogni 12 s**, lineare, finché la master lavora. È un'eco calma della stellina del feed (2,4 s per giro), che resta sulla risposta in scrittura.
- **Movimento ridotto** (`prefers-reduced-motion`, e su Android la scala delle animazioni a 0): niente scatto e niente rotazione. Lo stato resta leggibile dal punto colorato, che c'è comunque.
- Nient'altro si muove: niente aloni, sfocature o sfumature animate.

## 4. CSS e resa

- `concept-firma.css` per la web app vera, coi selettori di `basi.md`. Il dettaglio tecnico da notare: la master è uno **scope di token**. Su `.pane > .closed` e `.slide` vengono ridefiniti `--text`, `--text2`, `--icon`, `--bg`, `--low`, `--surface`, `--high`, `--line`, `--steps-bg`, `--primary`, `--opus` e gli altri, così Feed, Composer, PromptBox e Header cambiano senza toccarne gli stili. La scintilla è un SVG inline: una maschera sulla linguetta, uno sfondo nelle risposte e nella tacca.
- Tre giri di resa, il massimo previsto:
  1. struttura (foglio, cornice, scintilla, scope di token);
  2. grafite un gradino più scura, scintille delle risposte sull'asse della linguetta, testo sulla colonna del titolo, sottolineatura di «Casa»;
  3. tacca spostata sul tratto dritto del bordo, perché sulla curva sembrava un angolo rotto.
- Verifiche fatte su questa resa: colori dei pixel campionati (la tacca coincide col foglio `#1C1B1A`, il campo è `#121211`, la linguetta `#2A2927`); prova in scala di grigi in `concept-firma-bn.png`; rilevatore di impeccable sul CSS: nessun rilievo.
- **Cosa la resa non mostra.** La master di prova è ferma, quindi il punto di stato e la rotazione non si vedono. Lo scatto è un movimento e non compare in un'immagine ferma.

## 5. Note per Android

Token in `ui-tokens/.../CmColors.kt` (nomi speculari a quelli web):
`masterTab #2A2927`, `masterBed #1C1B1A`, `masterSunk #121211`, `masterRaise #33322F`, `masterKey #403E3A`, `masterRule #3D3B37`, `masterFrame #8F8A80`, `masterInk #F5F2EA`, `masterInk2 #B9B4A8`, più `masterStepsDot #6E6A62` e `masterTrack #4A4842`. Spariscono `MasterTab`, `MasterBed`, `MasterField`, `MasterSoft` e `MasterIcon` (in `MasterDock.kt`) e `MasterNight` e `MasterLilac` (in `SessionSheet.kt`).

- **`ClaudeSpark(size: Dp, color: Color, rotation: Float = 0f)`**, composable nuovo e condiviso. Si estrae dal `Canvas` della riga «sta lavorando» in `SessionSheet.kt` (8 `drawLine`, raggi da 0,25r a r, punte tonde) e la usano linguetta, risposte e campo.
- **`MasterDock.kt`**
  - `SessionBadge(master, 20.dp)` (e il ramo `look.signature` con l'anello `MasterGradient`) → `Box(28.dp) { ClaudeSpark(24.dp, masterInk, rot) ; punto di stato 10.dp con bordo 2.dp masterTab, allineato in basso a destra }`. Colore dal `master.state`: waiting → `CmColors.waiting`; busy e awaiting → `CmColors.busy`; gone → `CmColors.gone`, con la scintilla ad alpha 0,5.
  - Rotazione: `rememberInfiniteTransition`, `tween(12_000, LinearEasing)`, solo se busy e se le animazioni sono attive (la stessa condizione `off` della riga «sta lavorando»).
  - Scatto: `Animatable(-45f).animateTo(0f, tween(420, CmMotion…))` in un `LaunchedEffect(expanded)`.
  - Etichetta in `masterInk2`, titolo in `masterInk`, `IconButton` con `containerColor = masterKey` e `contentColor = masterInk`. Forma `RoundedCornerShape(topStart = 24.5.dp, topEnd = 24.5.dp)`.
- **Il foglio** (closed e open), un helper `Modifier.masterSheet()` in `SessionSheet.kt`: `padding(horizontal = 6.dp)`, poi `clip(RoundedCornerShape(26.dp, 26.dp, 0.dp, 0.dp))`, `background(masterBed)` e una cornice **aperta in basso**. `Modifier.border` disegna tutti e quattro i lati, quindi serve `drawWithContent { drawContent(); drawPath(path, masterFrame, style = Stroke(1.5.dp.toPx())) }`, con un `Path` fatto di due archi in alto e dei due lati.
  - Si usa intorno alla colonna dock + `Composer` nella home.
  - In `masterChat`: un nuovo `MasterChatStyle.SHEET`, che diventa il default di `LocalMasterChatStyle`. Oggi il pannello usa `RoundedCornerShape(topStart = 24.dp…)`: va portato a 26 e rientrato di 6. Gli stili BLACK, FRAME, EDGE, VIOLET, AURORA e FRAME_FIELD si tolgono quando Franz approva.
- **`Composer`** in `SessionSheet.kt`
  - `bar`: `background(if (toMaster) masterBed …)`, padding orizzontale 10 da chiusa e 12 da aperta.
  - `OutlinedTextField`: `unfocusedBorderColor = masterFrame`, `focusedBorderColor = masterInk`, `container = masterSunk`, spessore 1,5 con `OutlinedTextFieldDefaults.Container(unfocusedBorderThickness = 1.5.dp, focusedBorderThickness = 1.5.dp)`. L'`OutlinedTextField` semplice non accetta lo spessore: serve `BasicTextField` con `OutlinedTextFieldDefaults.DecorationBox(container = { Container(...) })`, oppure si tengono 1 e 2 dp di serie.
  - Tacca: lo slot `label` di M3 non va bene, perché l'etichetta sale nella tacca solo con il campo attivo o pieno. Serve una sovrapposizione: `Box(Modifier.offset(y = (-8).dp).size(22.dp, 15.dp).background(masterBed)) { ClaudeSpark(13.dp, masterInk) }` sul tratto dritto, centrata a 50 dp dal bordo interno del foglio.
  - Segnaposto in `masterInk2`, «Usa» in `masterInk`, tasto invia pieno con `masterInk` e `masterBed`.
- **`ClaudeBubble`**: il ramo `voice` (barra lilla da 3 dp) → `padding(start = 26.dp)` più `drawBehind` con la scintilla da 15 dp a y 9 e il filo da 1 dp in `masterRule`, da 32 dp a `size.height − 44 dp`. Testo con `color = masterInk`, `lineHeight = 1.6.em`, `letterSpacing = 0.012.em`. `LocalVoiceAccent` diventa `LocalMasterVoice`, a true nella chat della master.
- **Il resto della chat della master**:
  - `MineBubble` e `UserBubble`: contenitore `masterRaise`;
  - `StepsCard`: `stepsBg` → `masterSunk` e puntini `masterStepsDot`, solo se non è fallito;
  - `PromptBox`: `masterSunk` con bordo `masterRule`;
  - testata modello / quota: senza fondo, divisore `masterRule`, anello neutro `masterInk2` su `masterTrack`;
  - «Casa»: sottolineatura disegnata in `masterFrame`.
  - Via più corta: un `if (inMaster)` per composable. Via pulita, come lo scope di token del CSS: una `LocalCmPalette` che la master sovrascrive. Conviene la prima per provarla sul telefono e la seconda se viene approvata.
- **Paparazzi**: le golden della home con la master e della master aperta vanno registrate di nuovo.

## 6. Rischi, e la contromossa

| Rischio | Contromossa |
|---|---|
| **«È grigio, sembra spenta o disattivata.»** La grafite può sembrare un pannello inattivo. | Si alza la penna, non il fondo: cornice da `#8F8A80` all'avorio pieno `#F5F2EA` a 1,5 px. Il segnale cresce senza aggiungere colore. Il testo è già a 13-16:1, quindi non sembra spento da vicino. |
| **«È ancora marroncino.»** La grafite ha una punta calda (tinta 30-45°, saturazione 3-5%), presa dalla «carta» di claude.ai. | Variante neutra pura pronta, contrasti verificati: `#2A2A2A` / `#1C1C1C` / `#121212` / `#333333`, cornice `#8C8C8C`, inchiostro `#F2F2F2` / `#B5B5B5`. Avorio sulla linguetta 12,82:1, secondario sul campo 9,14:1, cornice sul foglio 5,07:1, gradino linguetta/foglio 1,19:1. |
| **«Troppe linee»**: cornice del foglio, cornice del campo, filo sotto ogni risposta. | Si tolgono in quest'ordine: prima i fili sotto le risposte (la scintilla resta), poi la tacca. La cornice del foglio è l'idea e resta. |
| **«L'abbiamo già vista»**: `MasterLook` del 05/10 aveva firma, filo e voce. | Allora erano anelli e barre colorati in corallo-lilla, aggiunti a una master nera. Qui non c'è colore: la scintilla **sostituisce** il badge invece di circondarlo, la cornice è il contorno dell'oggetto e non una riga sotto la barra, la voce è una griglia tipografica. Basta mostrare `concept-firma-bn.png`. |
| **Si perde il glifo di stato** (pausa, fulmine, mano) del badge. | Oggi lo stato è un punto colorato più il moto. Se a Franz manca, il punto prende il glifo Lucide a 8 px su fondo pieno. Da ferma si può mostrare anche il punto verde `--good`: è uno stato vero, non è rubato. |
| **Il rientro di 6 px toglie 12 px**: il titolo della linguetta si tronca un po' prima. | Rientro a 4 px, o rientro solo da aperta. La cornice si vede comunque contro il nero. |
| **La scintilla nella tacca sembra un indicatore** (allegato, IA, «scintilla magica» delle app). | Si toglie: il campo appartiene già al foglio per la cornice uguale e per l'incasso. La firma resta sulla linguetta e sulle risposte. |
| **La rotazione in home distrae.** | 12 s per giro è lento, ma se dà fastidio la rotazione si toglie e resta il punto blu. È già il comportamento a movimento ridotto. |
| **Lavoro su Compose**: cornice aperta in basso, spessore 1,5 del campo M3, tacca sovrapposta. | Tre pezzi piccoli e locali (`masterSheet()`, `Container(...Thickness)`, una `Box` in `offset`), senza shader né sfocature: costano come la B. |
