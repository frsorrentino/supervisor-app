# Concept «Profondità»: la master è un foglio sollevato

Immagine da giudicare: `concept-profondita.png` (sinistra chiusa, destra aperta; scatti singoli `shot-profondita-home.png`, `shot-profondita-open.png`). CSS sulla web app vera: `concept-profondita.css`.

## 1. L'idea

**La master non ha un colore diverso: è fatta di un'altra materia e sta più in alto.** È un foglio di grafite calda, quasi neutra, appoggiato sopra la home: getta l'ombra sulla lista che gli passa sotto, sul bordo alto prende un filo di luce che al centro si accende, e la conversazione aperta è una vasca in penombra tra due labbri illuminati. Il colore si riduce a un solo segno: la scintilla di Claude, 11 px, in `--opus`, prima di «MASTER».

**Perché si riconosce chiusa.** Nella home tutto è piatto: card grigio-blu su nero, nessuna ombra, nessuna luce. La master è l'unica cosa che ha spessore: è più chiara delle card (in dark mode più chiaro vuol dire più alto), ha un riflesso sul bordo e oscura la card che le scorre sotto (nella resa la riga «Obiettivo…» di atlas-shop sfuma nell'ombra). L'occhio legge prima la profondità e poi il colore: la differenza si vede anche con la luminosità al minimo o con gli occhiali da sole, dove una tinta scura si perde.

**Perché si riconosce aperta.** Tutte le altre conversazioni stanno sul nero piatto `#000`. La master non tocca mai il nero: dall'alto in basso il fondo passa dalla grafite della testata a una penombra calda e risale verso il letto del campo, con una vignettatura leggera ai lati. La linguetta in cima e il letto del campo in fondo sono lo stesso foglio di quando è chiusa, ognuno col suo filo di luce: aprendola il foglio si divide, la linguetta sale e il letto resta giù, e in mezzo si vede la conversazione.

**Cosa risolve della terracotta B** (guardando `docs/segnalazioni/2026-10-06-*.jpg`):

- **Il marrone.** `#36221A` e `#24160F` hanno croma 0,034 e 0,027 a tinta 43-47° (OKLCH): un arancio così scuro diventa caffè o ruggine, e su OLED sembra un filtro seppia più che una sessione diversa. Qui la croma scende a 0,008, meno di quella delle card (0,014): la master è un grigio caldo, non un colore.
- **La famiglia di tinte sbagliata.** Riga e icone corallo (`#E2A58C`, `#F2C1A8`) e la cornice del campo al 55% stanno a 13° dal rosso di produzione ed errore (`#E5736B`, tinta 26° contro 39°) e vicino all'ambra di «ti aspetta». Nella master aperta la pillola ambra «ledger-api ti aspetta» sta subito sotto la linguetta marrone: due fasce calde, una di stato e una d'identità, che si confondono. Nella home il corallo rima con la targhetta «PRODUZIONE» e con l'icona rossa di «Chiudi». Qui il calore resta sotto la soglia del colore e l'unico corallo è una scintilla di 11 px con una forma propria.
- **Il panino.** Aperta, la B colora solo i bordi: linguetta marrone in alto, letto marrone in basso, nero in mezzo, proprio dove Franz legge. Qui la materia della master riempie tutta la pagina.
- **Il doppio anello del campo.** Cornice corallo di 2 px su fondo `#180E0A` dentro un letto marrone: il bordo del campo era la cosa più forte del blocco. Qui il campo è un incavo (ombra interna in alto, bordo caldo di 1 px), e la cornice si accende solo col fuoco.
- **Solo tinta, niente spessore.** La B diceva «questa barra è marrone», non «questa sta sopra le altre». Qui la differenza è di livello, che è quello che la master è: la sessione che coordina le altre.

## 2. Palette

Tutti i grigi della master hanno la stessa tinta (OKLCH h 70, un grigio pietra appena caldo) e croma tra 0,005 e 0,010; cambia solo la luminosità, che fa il lavoro.

| Token | Hex | OKLCH | Ruolo |
|---|---|---|---|
| `--m-lip` | `#433F3B` | .37 .009 70 | cima della linguetta, dove batte la luce |
| `--m-tab` | `#373330` | .325 .008 70 | linguetta |
| `--m-disc` | `#4D4843` | .405 .010 70 | dischi dei tasti ▶ ▲ |
| `--m-raise` | `#302D2A` | .30 .008 70 | bolle di Franz, pillola del modello, Prossimi, invia spento |
| `--m-sheet` | `#23201D` | .245 .007 70 | foglio: fondo intorno al campo, testata aperta |
| `--m-sheet-low` | `#1D1B18` | .222 .006 70 | foglio, in basso |
| `--m-steps` | `#1A1815` | .21 .006 70 | gruppi di passaggi |
| `--m-dusk` | `#191714` | .205 .006 70 | chat, fascia alta e bassa |
| `--m-deep` | `#110F0D` | .17 .005 70 | chat, punto più profondo |
| `--m-well` | `#100E0C` | .165 .005 70 | campo, l'incavo |
| `--m-ink` | `#F7F3EE` | .965 .007 70 | testo |
| `--m-ink2` | `#D0C9C2` | .84 .013 70 | testo secondario, bordo del campo a fuoco |
| `--m-ink3` | `#AFA8A1` | .735 .013 70 | righe piccole attenuate |
| `--m-rim` | `rgb(255 238 220 / .16)` | | filo di luce lungo tutto il bordo curvo |
| `--m-glint` | `rgb(255 242 228 / .9)` | | il punto vivo del filo, al centro |
| `--m-crease` | `rgb(0 0 0 / .55)` | | la piega tra linguetta e foglio |
| `--m-line` | `rgb(255 238 220 / .2)` | | bordo del campo |
| `--m-accent` | `var(--opus)` = `#D97757` | .672 .131 39 | la scintilla; nessun colore nuovo |

Contrasti WCAG calcolati con Python sui colori composti (le trasparenze mescolate sul fondo reale):

| Coppia | Colori | Rapporto |
|---|---|---|
| Titolo linguetta (16 px, 600) | `#F7F3EE` su `#373330` | 11,32:1 |
| Titolo, cima della linguetta | `#F7F3EE` su `#433F3B` | 9,44:1 |
| Riga «MASTER · ora · modello» (12 px) | `#D0C9C2` su `#373330` | 7,63:1 |
| Riga MASTER, cima | `#D0C9C2` su `#433F3B` | 6,37:1 |
| Scintilla (icona, soglia 3:1) | `#D97757` su `#373330` | 4,01:1 |
| Scintilla, cima | `#D97757` su `#433F3B` | 3,34:1 |
| Icone ▶ ▲ sui dischi | `#F7F3EE` su `#4D4843` | 8,18:1 |
| Testo scritto nel campo | `#F7F3EE` su `#100E0C` | 17,44:1 |
| Segnaposto «Scrivi alla master» | `#D0C9C2` su `#100E0C` | 11,76:1 |
| Suggerimento in corsivo | `#98A2B3` su `#100E0C` | 7,48:1 |
| + e «Usa» | `#A8C7FA` su `#100E0C` | 11,20:1 |
| Invia, disco spento | `#D0C9C2` su `#302D2A` | 8,35:1 |
| Riga sotto il campo (12 px) | `#AFA8A1` su `#1D1B18` | 7,31:1 |
| Testata aperta | `#F7F3EE` su `#23201D` | 14,67:1 |
| 5h 11% · ctx 34% | `#D0C9C2` su `#23201D` | 9,89:1 |
| Pillola modello, bolla di Franz, voci Prossimi | `#F7F3EE` su `#302D2A` | 12,39:1 |
| «PROSSIMI · 2» | `#D0C9C2` su `#302D2A` | 8,35:1 |
| Frecce di Prossimi, ▶ sotto la risposta | `#A8C7FA` su `#302D2A` | 7,96:1 |
| «Casa» | `#A8C7FA` su `#23201D` | 9,43:1 |
| Risposta di Claude, fascia alta | `#F7F3EE` su `#191714` | 16,19:1 |
| Risposta di Claude, punto profondo | `#F7F3EE` su `#110F0D` | 17,31:1 |
| Ora e meta (12,5 px) | `#D0C9C2` su `#110F0D` | 11,67:1 |
| Passaggi | `#F7F3EE` su `#1A1815` | 16,04:1 |
| Riassunto dei passaggi | `#D0C9C2` su `#1A1815` | 10,81:1 |
| «ledger-api ti aspetta» | `#F2F4F7` su `#46371D` | 10,45:1 |
| «Rispondi» ambra | `#FFB020` su `#46371D` | 6,29:1 |
| Bordo del campo a fuoco (non testo) | `#D0C9C2` su `#100E0C` | 11,76:1 |
| Bordo del campo a riposo (non testo) | `#403B36` su `#100E0C` | 1,74:1 |

Tutto il testo passa AA con margine, l'icona più debole (la scintilla in cima alla linguetta) sta a 3,34:1. Il bordo del campo a riposo è sotto 3:1, come oggi negli altri campi (bianco al 25% sul nero, 2,03:1): il campo si riconosce dall'incavo scuro, dal + e dal segnaposto. Se al sole non basta, `--m-line` a .30 dà 2,47:1, a .36 dà 3,06:1.

**Come evita i colori di stato.** Nessuna superficie della master ha una tinta leggibile: la croma più alta è 0,013, sotto qualunque colore di stato (l'ambra ha 0,165). L'unico colore pieno è `--opus`, che l'app usa già per la stella di Claude al lavoro; vive solo in una forma a otto raggi di 11 px, mai come riga, fondo o cornice, quindi non si scambia con il rosso di produzione né con l'ambra. Le pillole di stato dentro la master (ambra «ti aspetta», il filo ambra delle voci di Prossimi che sbloccano) restano intatte e, sulla grafite, si leggono anzi meglio che sul marrone.

**Come convive con le card grigio-blu.** Le card sono un grigio freddo (h 262, croma 0,014, L .272); la master è un grigio caldo meno saturo e più chiaro (h 70, croma 0,008, L .325 la linguetta). Due grigi di temperatura opposta non litigano: si leggono come due materiali, ardesia e pietra. La linguetta più chiara delle card la mette sopra di loro; il letto (L .245) è appena più scuro delle card, così il campo non compete con loro.

## 3. Specifica per elemento

### Chiusa (in fondo alla home)

- **Blocco** (`.pane > .closed`): foglio `--m-sheet` → `--m-sheet-low` dall'alto in basso, angoli 26 px in alto come oggi. Ombra verso l'alto in tre strati: `0 -2px 3px` nero .7 (il contatto), `0 -14px 28px -6px` nero .8, `0 -34px 56px -18px` nero .6. Sul nero non si vede; si vede sulla card che scorre sotto, che si spegne negli ultimi 30 px. `position: relative` senza `z-index`: basta per dipingere l'ombra sopra la lista e lascia sopra la scritta fissa «Dati di prova» / «relay giù».
- **Linguetta** (`.dock`): gradiente `--m-lip` → `--m-tab` (fino al 62%) più una luce radiale bianco-calda al 9% centrata sul bordo alto. Bordo: `inset 0 1px 0 --m-rim` che segue la curva degli angoli; sopra, al centro, il riflesso: una linea di 1 px dal 18% all'82% della larghezza, trasparente → `--m-glint` → trasparente. Sotto, la **piega**: `inset 0 -1px 0 --m-crease`.
- **Riga MASTER** (`.dock .mono`): `--m-ink2`, preceduta dalla scintilla 11×11 px in `--m-accent` (maschera SVG, la stessa forma a otto raggi della stella di Claude nel feed), 7 px di spazio.
- **Titolo** (`.dock b`): `--m-ink`, 16 px semibold come oggi.
- **Tasti ▶ ▲** (`.dock .ib`): dischi 44 px `--m-disc`, icona `--m-ink`, un filo di luce in alto (`inset 0 1px 0` bianco-caldo .12) e un'ombra di contatto `0 1px 2px` nero .55: bottoni dello stesso materiale, in rilievo. Nessun corallo.
- **Badge** (`.dock svg[role=img]`): invariato; il suo colore è uno stato.
- **Fondo intorno al campo** (`.mquick`): il foglio, con un filo chiaro al 4,5% sotto la piega, così la piega ha un lato scuro e uno chiaro come una vera piegatura.
- **Campo** (`.field.master`): incavo `--m-well`, bordo 1 px `--m-line`, ombra interna `inset 0 2px 6px` nero .6 e un labbro chiaro esterno `0 1px 0` al 5%. Testo `--m-ink`, segnaposto `--m-ink2`, cursore `--m-accent`. A fuoco: bordo 2 px `--m-ink2` (niente corallo, il fuoco è luce). Invia spento: disco `--m-raise`, icona `--m-ink2`; acceso resta il bottone pieno `--primary` dell'app (l'unico pieno della schermata).
- **Riga sotto il campo** (`.keys`, visibile sopra i 760 px): `--m-ink3`.

**Linguetta contro resto del blocco**: linguetta L .325-.37, letto L .222-.245, separati dalla piega (1 px scuro + 1 px chiaro). Il rapporto linguetta/letto è 1,30:1 (cima/letto 1,55:1): più della B (linguetta/letto 1,17:1), e con la piega la differenza ha un bordo netto invece di una sfumatura.

### Aperta (pagina intera)

- **Linguetta in cima** (`.slide .dock.expanded`): identica alla chiusa, col riflesso; in più getta un'ombra corta verso il basso (`0 8px 16px -8px` nero .8) sulla testata: è il labbro alto della vasca.
- **Riga modello/quota** (`.slide .chat > header`): fondo `--m-sheet`, filo nero .5 sotto con un filo chiaro al 4% (una seconda piega, più leggera). Pillola «Opus 5.5 · high» `--m-raise` con luce in alto, testo `--m-ink`; percentuali e ora `--m-ink2`; gli anelli restano quelli dell'app. Il puntino viola dei consigli tiene il bordo nel colore del foglio.
- **Pillola «ledger-api ti aspetta»** (`.slide .pill` di ElsewherePill): invariata, ambra al 16% sul foglio. È uno stato e resta tale.
- **Sfondo della chat** (`.slide .chat`, `.lines` trasparente): tre strati. Gradiente verticale `--m-sheet` 0% → `--m-dusk` 26% → `--m-deep` 68% → `--m-dusk` 100%; luce radiale bianco-calda al 5% che scende dalla testata (120%×45%); vignettatura ellittica che scurisce i lati fino al 40% di nero. Lo sfondo non scorre col testo (è del contenitore): la vasca resta ferma, le parole ci passano dentro.
- **Risposte di Claude** (`.ctext`): testo `--m-ink`, nessun fondo. Il ▶ sotto la risposta su disco `--m-raise`, icona blu dell'app (le azioni restano blu ovunque).
- **Bolle di Franz** (`.me .bubble`): `--m-raise` con luce in alto e ombra corta (`0 4px 10px -4px` nero .6): fogli più piccoli appoggiati nella vasca. Ora e «dal telefono» `--m-ink2`.
- **Gruppi di passaggi** (`.steps`): `--m-steps` con ombra interna leggera, incassati come il campo; riassunto `--m-ink2`; il fondo rosso dei passaggi falliti resta quello dell'app.
- **Prossimi** (`.box`): `--m-raise` senza bordo, luce in alto, ombra `0 8px 18px -8px`; separatori nero .4 con un filo chiaro al 3%; «PROSSIMI · 2» `--m-ink2`; il filo ambra delle voci che sbloccano resta.
- **Casa** (`.tohome`): blu dell'app, al passaggio `--m-raise`.
- **Campo e suo fondo** (`.slide .chat > .bar`): il letto del campo è un labbro che sale dal basso: angoli 26 px in alto, `--m-sheet` → `--m-sheet-low`, filo di luce sul bordo curvo col suo riflesso al 70%, ombra verso l'alto `0 -12px 24px -10px` sulla vasca. Il campo è lo stesso incavo della chiusa.

### Movimento

- **Il riflesso scorre** (un solo momento): quando la linguetta compare (home caricata, master aperta o richiusa) il riflesso entra da sinistra e si ferma al centro: 700 ms, ritardo 120 ms, `cubic-bezier(.16, 1, .3, 1)`, da `translate -55%` e opacità 0. Aperta, lo stesso sul letto del campo. È «l'effetto di luce sulla tab» che Franz si aspettava da Luce, ma legato al gesto invece che allo stato.
- **Pressione**: la linguetta scende di 1 px in 90 ms e risale in 180 ms (`cubic-bezier(.2, .8, .2, 1)`).
- **Al lavoro**: la scintilla ruota piano, 2,4 s lineare, come la stella di Claude nel feed (`.dock:has(.breathe)`). Ferma, la scintilla è ferma.
- La salita della pagina resta quella di oggi (300 ms, `cubic-bezier(.2, .8, .2, 1)`).
- **Movimento ridotto**: riflesso fermo al centro dall'inizio, scintilla ferma, nessuno spostamento alla pressione; la salita della pagina è già spenta dall'app.

## 4. Resa sulla web app vera

`sh render.sh profondita` dalla cartella dei concept, tre giri, uno alla volta (aspettando gli altri render in corso):

1. Materia e ombra a posto, l'ombra spegne la card atlas-shop. Il riflesso al centro quasi invisibile (.7 sopra un filo .16): la «luce sulla tab» non si leggeva.
2. Cima della linguetta più chiara (L .36 → .37), riflesso a .9 e più largo, luce radiale .06 → .09, riflesso anche sul letto del campo da aperta, vignettatura più decisa. Il riflesso ora si vede al primo sguardo.
3. Tolti gli `z-index`: al giro 2 il blocco con `z-index: 2` copriva la scritta fissa «Dati di prova», che nell'uso vero diventa «relay giù» o «connessione». Con solo `position: relative` l'ombra resta sopra la lista e la scritta torna visibile (verificato nel PNG finale).

Il rilevatore di Impeccable sul CSS non trova difetti (`[]`). La grana fine è stata scartata di proposito: con `feTurbulence` o un rumore in PNG sembra sporco su OLED a densità 3 e costa una texture a schermo intero sul telefono.

## 5. Note per Android

- **`CmColors.kt`**: nuovi token `masterLip`, `masterTab`, `masterDisc`, `masterRaise`, `masterSheet`, `masterSheetLow`, `masterSteps`, `masterDusk`, `masterDeep`, `masterWell`, `masterInk`, `masterInk2`, `masterInk3` coi valori della tabella; `masterRim = Color(0x29FFEEDC)`, `masterGlint = Color(0xE6FFF2E4)`, `masterCrease = Color(0x8C000000)`, `masterLine = Color(0x33FFEEDC)`. L'accento è `modelOpus`, che c'è già. Vanno tolti `MasterTab`, `MasterBed`, `MasterField`, `MasterSoft`, `MasterIcon` in testa a `MasterDock.kt`.
- **`MasterDock.kt`**: la `Row` passa da `.background(MasterTab)` a `.background(Brush.verticalGradient(0f to masterLip, 0.62f to masterTab))` più `drawBehind` con la luce radiale (`Brush.radialGradient`, centro in alto, 9%). Il filo sul bordo curvo: `.border(BorderStroke(1.dp, Brush.verticalGradient(0f to masterRim, 0.05f to Color.Transparent)), shape)`, un bordo che esiste solo in cima e quindi segue gli angoli. Il riflesso: `drawWithContent { drawContent(); drawLine(Brush.horizontalGradient(listOf(Transparent, masterGlint, Transparent), startX = w*.18f, endX = w*.82f), …) }` con l'offset X animato da `Animatable` (700 ms, `CubicBezierEasing(.16f, 1f, .3f, 1f)`, ritardo 120 ms; con l'animazione di sistema spenta parte già al centro). La piega: una riga 1 dp `masterCrease` sotto la `Row` al posto del `Box` con `CmColors.line`, più 1 dp chiaro al 4,5% sopra il campo. Riga MASTER `MonoSmall.copy(color = masterInk2)` preceduta da un'`Icon` 11 dp della scintilla (lo stesso disegno a otto raggi del feed) in `modelOpus`, che ruota con `rememberInfiniteTransition` quando la sessione lavora. Tasti: `filledTonalIconButtonColors(containerColor = masterDisc)`, `tint = masterInk`. Pressione: `graphicsLayer { translationY = if (pressed) 1.dp.toPx() else 0f }` con `animateFloatAsState`.
- **Ombra verso l'alto del blocco chiuso**: le ombre di elevazione di Android cadono verso il basso, quindi va disegnata: `drawBehind` sul contenitore di linguetta + campo, tre rettangoli in `Brush.verticalGradient(Transparent → Black.copy(.6-.8f))` sopra il bordo (y negativo, 3, 28 e 56 dp), senza `clip` sul genitore e con `zIndex(1f)` rispetto alla lista. In alternativa `Modifier.dropShadow(shape, Shadow(radius, offset = DpOffset(0.dp, (-14).dp)))`, disponibile in Compose UI 1.9+ (il BOM 2026.06.01 la include).
- **`SessionSheet.kt`, `Composer`**: `bar` da `background(if (toMaster) MasterBed else CmColors.bg)` a un `Brush.verticalGradient(masterSheet, masterSheetLow)` con angoli 26 dp in alto, il filo e il riflesso come la linguetta, e l'ombra verso l'alto sulla chat (solo da aperta). Il campo: `OutlinedTextFieldDefaults.colors(unfocusedBorderColor = masterLine, focusedBorderColor = masterInk2, unfocusedContainerColor = masterWell, focusedContainerColor = masterWell, cursorColor = modelOpus)` e l'incavo con `Modifier.innerShadow(shape, Shadow(6.dp, Color.Black.copy(.6f), offset = DpOffset(0.dp, 2.dp)))`. Segnaposto `masterInk2`.
- **`SessionSheet.kt`, `masterChat`**: un nuovo valore `MasterChatStyle.DEPTH` (o al posto di `BLACK` come predefinito) che disegna `Brush.verticalGradient(0f to masterSheet, .26f to masterDusk, .68f to masterDeep, 1f to masterDusk)`, poi in `drawBehind` la luce radiale in alto (5%) e la vignettatura (`Brush.radialGradient` con centro a metà larghezza e 45% d'altezza, trasparente fino al 50%, nero .4 ai lati). Gli stili vecchi (`FRAME`, `EDGE`, `VIOLET`, `AURORA`, lilla) si possono togliere. Dentro la master, via un `CompositionLocal` (o il `LocalMasterChatStyle` che c'è già), bolle e Prossimi su `masterRaise`, passaggi su `masterSteps`, testi secondari `masterInk2`, pillola del modello `masterRaise`.
- Niente blur, niente shader: gradienti e ombre disegnate una volta, nessun ridisegno mentre la chat scorre (lo sfondo è del contenitore, non della lista).

## 6. Rischi e contromosse

- **«È grigio, è spento.»** Il rischio maggiore: dopo il marrone Franz può leggere la grafite come assenza di scelta. Contromossa: il giudizio va fatto sul telefono e non sul PNG, perché ombra e riflesso su OLED rendono più che in uno screenshot ridotto. Se resta spento, la leva è la luce, non la tinta: riflesso più lungo (dal 10% al 90%) e luce radiale al 12%, oppure la scintilla da 11 a 14 px. Non si torna a colorare il fondo.
- **«Le card e la master si somigliano.»** Linguetta e card hanno contrasto 1,20:1 tra loro: le distingue la temperatura, l'ombra e il riflesso, non la luminosità. Se a colpo d'occhio non basta, si alza la linguetta a L .35 (`#3D3A36`) lasciando il letto dov'è, così la differenza linguetta/letto cresce insieme.
- **La scintilla scambiata per «sta lavorando».** È la stessa forma della stella del feed. Contromossa già nel concept: ferma quando la master è ferma, ruota solo quando lavora, cioè dice la stessa cosa della stella; se confonde, la scintilla diventa un punto pieno di 6 px in `--opus` e la rotazione sparisce.
- **La vasca troppo scura in fondo.** Il punto più profondo (`#110F0D`) contro il nero ha 1,10:1: da solo non si vede, si vede la sfumatura. Se su schermo appare nera come le altre, si alza `--m-deep` a L .19 (`#151311`, 1,13:1 sul nero) e il gradiente diventa più piatto ma sempre diverso dal nero.
- **Il bordo del campo a riposo debole (1,74:1).** Come oggi negli altri campi; se al sole non si trova, `--m-line` a .36 porta il bordo a 3,06:1.
- **Il riflesso che scorre visto come decorazione.** Parte solo quando la linguetta compare, dura 700 ms e poi sta fermo; se dà fastidio si tiene fermo al centro, che è anche la versione a movimento ridotto.
- **Righe troncate senza segno.** «MASTER · 12:50 · Opus 5.5 ·» finisce col punto perché la scintilla ruba 19 px alla riga e oggi la riga si taglia senza «…» (regola di Franz). Contromossa: togliere «ctx» dalla riga della linguetta quando non ci sta, invece di tagliarla a metà; è un cambio di testo e va chiesto.
