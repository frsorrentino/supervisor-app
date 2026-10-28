# Concept «carbone»: la master nella temperatura di Claude

Resa sulla web app vera: `concept-carbone.png` (a sinistra chiusa, a destra aperta), dal CSS `concept-carbone.css`.

## 1. L'idea

**La master è l'unica parte dell'app in grigi caldi: aprirla è come entrare in claude.ai, e il corallo resta solo nella scintilla ✻.**

Il resto dell'app non cambia: nero con card grigio-blu. La master cambia **temperatura**, non tinta. Linguetta, fondo intorno al campo e sfondo della chat aperta sono un carbone caldo e opaco (chroma OKLCH 0,008, tono 80, cioè un grigio che pende al caldo senza diventare marrone), con testo avorio. Il corallo di Claude compare in tre punti, tutti piccoli: la scintilla a otto raggi prima di «MASTER», la stessa scintilla in fondo a ogni risposta di Claude, il cursore e il bordo del campo quando ci scrivi.

**Perché si riconosce a colpo d'occhio.**
- *Chiusa*: in fondo alla home è l'unico blocco caldo, opaco e **più chiaro delle card** (L 0,27-0,32 contro 0,24 della card `#1B1F26`), sopra il nero e accanto alle card blu. L'occhio lo legge come un materiale diverso, non come un'altra card. La scintilla corallo prima di «MASTER» è l'unico punto di colore del blocco.
- *Aperta*: cambia tutta la pagina, dalla linguetta al campo. Al posto del nero c'è il grigio caldo di claude.ai, il testo è avorio, la bolla di Franz e la pillola del modello sono grigio caldo, non più blu. Le altre sessioni aperte restano nere con le bolle grigio-blu. Chi conosce claude.ai in tema scuro riconosce quella temperatura prima di leggere qualunque cosa.

**Perché risolve quello che non andava nella terracotta B.**
- *Era marrone.* `#36221A`/`#24160F` hanno chroma 0,034/0,027 e tono 43-47, cioè il territorio del cuoio e della ruggine. Nella foto dal telefono delle 18:58 il blocco si legge come legno scuro, non come Claude. Il carbone ha un quarto di quel chroma (0,008-0,010) e un tono spostato verso il neutro (80): è caldo per temperatura, non per colore.
- *Era tutto arancio.* In B fondo, riga «MASTER» (`#E2A58C`), icone (`#F2C1A8`) e bordo del campo erano tutti della stessa famiglia arancio: il corallo era ovunque, quindi non segnava niente. Qui il corallo ha un solo ruolo, la firma di Claude, ed è la stessa scintilla che già gira nella chat quando la master lavora.
- *Aveva il colore di un avviso.* La terracotta (tono 43) sta a 4° dal corallo (39) e a 17° dal rosso «produzione» `#E5736B` (26): tutto il blocco aveva la tinta degli avvisi. Il carbone ha chroma 0,008 e nessuno stato gli si avvicina; ambra, rosso e verde degli stati restano gli unici colori pieni.
- *Era scuro come la home.* Il fondo `#24160F` (L 0,216) è più scuro della card (0,238): metà del blocco stava sotto le card invece di staccarsene. Il carbone è tutto più chiaro delle card.
- *Da aperta la chat restava nera.* Restavano due fasce marroni con un buco nero in mezzo. Qui lo sfondo della chat è lo stesso fondo del blocco.

## 2. Palette

Nove grigi caldi su un solo tono (OKLCH H 80), più avorio, pietra e sabbia per testo e icone, e il corallo. Nomi web `--m-*`, Android in §5.

| Token | Hex | OKLCH (L C H) | Ruolo |
|---|---|---|---|
| `--m-well` | `#1F1D1A` | 0,232 0,006 78 | Interno del campo, gruppi di passaggi, riquadro Prossimi: incavati |
| `--m-bed` | `#292723` | 0,273 0,008 85 | Fondo intorno al campo; sfondo della chat aperta e della sua testata |
| `--m-tab` | `#36332E` | 0,322 0,010 81 | Linguetta (un passo più chiara del fondo); pillola del modello; tasto invio a riposo |
| `--m-high` | `#413E39` | 0,365 0,009 81 | Bolle di Franz, tasto ▶ della lettura, pastiglia «Casa» |
| `--m-key` | `#4B4742` | 0,400 0,010 80 | I tasti tondi ▶ ▲/▼ sulla linguetta |
| `--m-line` | `#46433E` | 0,385 0,009 80 | Filetti: sotto la testata, bordo e righe di Prossimi |
| `--m-edge` | `#5E5A55` | 0,470 0,010 80 | Bordo del campo a riposo (1,5 px) |
| `--m-track` | `#58554F` | 0,450 0,010 80 | Binario degli anelli 5h/ctx |
| `--m-dot` | `#6C6863` | 0,520 0,010 80 | Puntini dei passaggi |
| `--m-text` | `#F6F3EC` | 0,965 0,010 87 | Avorio caldo: titoli, risposte, bolle |
| `--m-text2` | `#C7C2B9` | 0,816 0,014 82 | Pietra: riga mono della linguetta, ore, meta, segnaposto |
| `--m-text3` | `#A6A199` | 0,710 0,013 80 | Suggerimento in corsivo nel campo |
| `--m-sand` | `#DCCFBC` | 0,860 0,030 78 | Icone e link («Casa», «Usa», +, ↗, anelli) |
| `--m-primary` | `#EFEBE1` | 0,940 0,014 88 | L'unico bottone pieno (invio con testo, stop): avorio |
| `--m-on-primary` | `#24211D` | 0,250 0,008 80 | Freccia sul bottone pieno |
| `--m-spark` | `#D97757` | 0,672 0,131 39 | Corallo di Claude: solo la scintilla, il cursore, il bordo a fuoco |

Riferimento freddo: card `#1B1F26` = 0,238 0,015 **262**. Stesso ordine di chroma, tono opposto (262 contro 80): è questo che fa leggere due temperature.

### Contrasti WCAG (calcolati con Python sulla formula di luminanza relativa)

| Dove | Coppia | Testo | Fondo | Rapporto | Soglia |
|---|---|---|---|---|---|
| Chiusa | titolo della linguetta | `--m-text` #F6F3EC | `--m-tab` #36332E | 11.35:1 | 4.5 ok |
| Chiusa | riga MASTER · ora · modello | `--m-text2` #C7C2B9 | `--m-tab` #36332E | 7.09:1 | 4.5 ok |
| Chiusa | scintilla sulla linguetta | `--m-spark` #D97757 | `--m-tab` #36332E | 4.03:1 | 3.0 ok |
| Chiusa | icona ▶ ▲ sul tasto tondo | `--m-text` #F6F3EC | `--m-key` #4B4742 | 8.31:1 | 3.0 ok |
| Chiusa | tasto tondo contro la linguetta | `--m-key` #4B4742 | `--m-tab` #36332E | 1.36:1 | non testo: il tasto si riconosce dall'icona |
| Chiusa | linguetta contro fondo (la differenza lieve) | `--m-tab` #36332E | `--m-bed` #292723 | 1.19:1 | non testo, voluto (ΔE OKLab 0,049; in B era 0,059) |
| Chiusa | segnaposto «Scrivi alla master» | `--m-text2` #C7C2B9 | `--m-well` #1F1D1A | 9.48:1 | 4.5 ok |
| Chiusa/Aperta | testo scritto nel campo | `--m-text` #F6F3EC | `--m-well` #1F1D1A | 15.17:1 | 4.5 ok |
| Chiusa/Aperta | suggerimento in corsivo | `--m-text3` #A6A199 | `--m-well` #1F1D1A | 6.55:1 | 4.5 ok |
| Chiusa/Aperta | «Usa» e + (sabbia) | `--m-sand` #DCCFBC | `--m-well` #1F1D1A | 10.96:1 | 4.5 ok |
| Chiusa/Aperta | bordo del campo a riposo contro il fondo | `--m-edge` #5E5A55 | `--m-bed` #292723 | 2.18:1 | non testo (oggi: 2.03:1, `rgb(255 255 255/.25)` sul nero) |
| Chiusa/Aperta | bordo del campo a riposo contro l'interno | `--m-edge` #5E5A55 | `--m-well` #1F1D1A | 2.46:1 | non testo |
| Chiusa/Aperta | fuoco del campo (corallo) contro il fondo | `--m-spark` #D97757 | `--m-bed` #292723 | 4.77:1 | 3.0 ok |
| Chiusa/Aperta | fuoco del campo (corallo) contro l'interno | `--m-spark` #D97757 | `--m-well` #1F1D1A | 5.39:1 | 3.0 ok |
| Chiusa/Aperta | invio a riposo: freccia sul tasto | `--m-text2` #C7C2B9 | `--m-tab` #36332E | 7.09:1 | 3.0 ok |
| Chiusa/Aperta | invio pieno: freccia scura su avorio | `--m-on-primary` #24211D | `--m-primary` #EFEBE1 | 13.46:1 | 3.0 ok |
| Chiusa/Aperta | invio pieno contro il campo | `--m-primary` #EFEBE1 | `--m-well` #1F1D1A | 14.12:1 | 3.0 ok |
| Chiusa | riga «Invio manda» sotto il campo (≥ 760 px) | `--m-text2` #C7C2B9 | `--m-bed` #292723 | 8.41:1 | 4.5 ok |
| Aperta | risposta di Claude | `--m-text` #F6F3EC | `--m-bed` #292723 | 13.45:1 | 4.5 ok |
| Aperta | ore, «dal telefono», meta | `--m-text2` #C7C2B9 | `--m-bed` #292723 | 8.41:1 | 4.5 ok |
| Aperta | icone copia/segnalibro | `--m-text2` #C7C2B9 | `--m-bed` #292723 | 8.41:1 | 3.0 ok |
| Aperta | «Casa» (sabbia) su pastiglia | `--m-sand` #DCCFBC | `--m-high` #413E39 | 6.94:1 | 4.5 ok |
| Aperta | scintilla in fondo alla risposta | `--m-spark` #D97757 | `--m-bed` #292723 | 4.77:1 | 3.0 ok |
| Aperta | bolla di Franz | `--m-text` #F6F3EC | `--m-high` #413E39 | 9.61:1 | 4.5 ok |
| Aperta | ▶ lettura: icona sabbia sul tondo | `--m-sand` #DCCFBC | `--m-high` #413E39 | 6.94:1 | 3.0 ok |
| Aperta | pillola «Opus 5.5 · high» | `--m-text` #F6F3EC | `--m-tab` #36332E | 11.35:1 | 4.5 ok |
| Aperta | 5h / ctx (riga quota) | `--m-text2` #C7C2B9 | `--m-bed` #292723 | 8.41:1 | 4.5 ok |
| Aperta | anello quota (sabbia) contro il fondo | `--m-sand` #DCCFBC | `--m-bed` #292723 | 9.72:1 | 3.0 ok |
| Aperta | binario dell'anello contro il fondo | `--m-track` #58554F | `--m-bed` #292723 | 2.01:1 | non testo, solo traccia |
| Aperta | passaggi: riassunto | `--m-text2` #C7C2B9 | `--m-well` #1F1D1A | 9.48:1 | 4.5 ok |
| Aperta | passaggi: ultimo passo | `--m-text` #F6F3EC | `--m-well` #1F1D1A | 15.17:1 | 4.5 ok |
| Aperta | Prossimi: titolo mono | `--m-text2` #C7C2B9 | `--m-well` #1F1D1A | 9.48:1 | 4.5 ok |
| Aperta | Prossimi: riga | `--m-text` #F6F3EC | `--m-well` #1F1D1A | 15.17:1 | 4.5 ok |
| Aperta | Prossimi: freccia ↗ (sabbia) | `--m-sand` #DCCFBC | `--m-well` #1F1D1A | 10.96:1 | 3.0 ok |
| Aperta | pillola «ti aspetta»: testo avorio | `--m-text` #F6F3EC | ambra 16 % sul fondo = #4B402F | 9.14:1 | 4.5 ok |
| Aperta | pillola «ti aspetta»: «Rispondi» | `--b-warn` #FFC46B | #4B402F | 6.44:1 | 4.5 ok |

Il bordo del campo a riposo resta sotto 3:1, come oggi. Il campo però non si riconosce solo dal bordo: ha l'interno più scuro, il segnaposto, il + e la freccia. A fuoco il bordo diventa corallo a 4,8-5,4:1.

### Colori di stato e card grigio-blu

- **Nessun token della master ruba uno stato.** Tutti i grigi hanno chroma ≤ 0,014 e la sabbia 0,030. Ambra, verde, blu, rosso e viola degli stati stanno sopra 0,1. Grigi e sabbia hanno lo stesso tono dell'ambra (78-80 contro 75-76), ma un quindicesimo del suo chroma (0,030 contro 0,126-0,165; ΔE OKLab 0,097 da `#FFC46B` e 0,143 da `#FFB020`): si leggono come grigi, non come «ti aspetta».
- **Gli stati dentro la master restano quelli dell'app:** il badge nella linguetta, la pillola ambra «ledger-api ti aspetta», la barra ambra delle righe bloccanti di Prossimi, il fondo rosso dei passaggi falliti (`.steps.fail` resta escluso dall'override), gli anelli ambra o rossi sopra soglia. Ridefinisco solo `--b-ring`, cioè il colore *neutro* dell'anello. Nella resa la pillola ambra sul carbone resta dorata e leggibile (9,14:1 il testo, 6,44:1 «Rispondi»).
- **Il corallo è vicino al rosso «produzione»** `#E5736B` (ΔE 0,036). Per questo compare solo come forma della scintilla e mai come pallino, riempimento o testo. È lo stesso corallo che l'app usa già per la stella «al lavoro» e per Opus.
- **Convivenza con le card:** il blocco caldo non tocca nessuna card, perché `.card` e le etichette di gruppo restano fuori dal selettore. Il confronto anzi aiuta: accanto al grigio-blu il carbone appare più caldo di quanto sia, e la card accanto al carbone appare più fredda. È contrasto simultaneo, lo stesso effetto che fa sembrare «calda» claude.ai.

## 3. Specifica per elemento

**Meccanismo.** Dentro `.pane > .closed` e `.slide` ogni token freddo dell'app è ridefinito sul suo gemello caldo (`--bg → --m-bed`, `--low → --m-well`, `--surface → --m-tab`, `--high → --m-high`, `--line`, `--text`, `--text2`, `--icon → --m-sand`, `--primary`, `--on-primary`, `--stale`, `--b-second`, `--b-track`, `--b-ring`, `--steps-bg`, `--steps-dot`). Chat, bolle, testata, Prossimi e campo cambiano temperatura senza toccare i loro componenti. Gli `!important` servono solo dove c'è un colore scritto a mano (linguetta, campo della master).

### Chiusa (in fondo alla home)
| Elemento | Specifica |
|---|---|
| Blocco (`.pane > .closed`) | Fondo `--m-bed`, angoli 26 px in alto. Linguetta e campo sono un solo pezzo |
| Linguetta (`.dock`) | Fondo `--m-tab`: un passo più chiara del fondo (ΔL 0,05). Sul bordo alto un filo di luce interno, `inset 0 1px 0` avorio al 7 %, come le superfici rialzate di claude.ai |
| Riga «MASTER · ora · modello» (`.dock .mono`) | `--m-text2` pietra, non più corallo. La precede la **scintilla** ✻ a 8 raggi (13 px, `--m-spark`, margine destro 7 px): stesso disegno della stella «al lavoro» di `Feed.svelte`, come maschera SVG e non come carattere |
| Titolo (`.dock b`) | `--m-text` avorio, 16 px 600 come oggi |
| Tasti ▶ ▲ (`.dock .ib`) | Tondi 44 px, fondo `--m-key`, icona avorio |
| Badge | Invariato: è lo stato |
| Fondo intorno al campo | `--m-bed`: è il blocco stesso |
| Campo (`.field.master`) | Interno `--m-well`, incavato; bordo 1,5 px `--m-edge`. A fuoco 2 px `--m-spark`. Cursore `caret-color: --m-spark`. Segnaposto `--m-text2`, suggerimento in corsivo `--m-text3`, «Usa» e + in `--m-sand`. Invio a riposo su `--m-tab`; con testo diventa l'unico bottone pieno, avorio `--m-primary` |
| Riga sotto il campo (`.keys`, solo da 760 px) | `--m-text2` su `--m-bed` |
| Selezione del testo | Sabbia al 28 % invece del blu `#3457D5` |

### Aperta (pagina intera)
| Elemento | Specifica |
|---|---|
| Linguetta in cima (`.dock.expanded`) | La stessa della chiusa (`--m-tab`, scintilla, tasto ▼), angoli tondi in alto sul nero della home |
| Riga modello/quota (`.chat > header`) | Fondo `--m-bed`, filetto sotto `--m-line`. Pillola «Opus 5.5 · high» su `--m-tab`. Anelli: binario `--m-track`, arco neutro `--m-sand`; ambra e rosso sopra soglia invariati |
| Pillola «ti aspetta» | Invariata (ambra al 16 %): sul carbone diventa `#4B402F` e resta uno stato |
| «Casa» (`.tohome`) | Pastiglia `--m-high` con testo sabbia 500. Con solo la sabbia sembrava testo normale (giro 1) |
| Sfondo della chat (`.lines`) | `--m-bed`, lo stesso fondo del blocco. Nella casa della master la griglia di puntini diventa avorio al 6 % |
| Risposte di Claude (`.claude`, `.ctext`) | Avorio `--m-text` su carbone. In fondo a ogni risposta, prima di copia/▶/segnalibro, la **scintilla** corallo da 14 px, come la firma di claude.ai |
| Bolle di Franz (`.me .bubble`) | `--m-high`, testo avorio. Ora, «dal telefono» e icone in `--m-text2` |
| Gruppi di passaggi (`.steps`) | `--m-well`, incavati; puntini `--m-dot`. Il gruppo con errori resta rosso |
| Prossimi (`.box`) | `--m-well`, bordo e righe `--m-line`; titolo mono pietra; ↗ sabbia; barra ambra delle righe bloccanti invariata |
| Campo e suo fondo | Come da chiusa: la barra del campo sta sul `--m-bed` della chat, senza fasce diverse |

### Movimento
- **La scintilla della linguetta gira solo mentre la master lavora**: `rotate` 360° in 2,4 s lineare, lo stesso giro della stella «al lavoro» della chat. La condizione è `.dock:has(svg[role=img][aria-label="busy"])`, perché il badge porta lo stato nell'`aria-label`. Non uso `.breathe`: il badge respira anche in `awaiting`, quando la master aspetta Franz e non lavora. A riposo la scintilla è ferma. Non è un alone: è il segno di Claude che pensa, nel posto dove guardi.
- **Movimento ridotto**: la scintilla resta ferma. Lo stato «al lavoro» lo dice già il badge.
- L'apertura usa la salita che c'è già (300 ms, `cubic-bezier(.2,.8,.2,1)`). Nessuna transizione di colore: il cambio di temperatura arriva insieme alla pagina.
- Nel banco di prova la master è ferma, quindi la rotazione non si vede nel PNG.

## 4. Il CSS e la resa

- File: `docs/mockup/2026-10-06-master-concept/concept-carbone.css`, reso con `sh render.sh carbone` → `concept-carbone.png`, `shot-carbone-home.png`, `shot-carbone-open.png`.
- **Giro 1**: l'idea regge da chiusa e da aperta. Unico difetto: «Casa» in sabbia sembrava testo e non un comando.
- **Giro 2**: «Casa» diventa una pastiglia tonale. Ho campionato i pixel del PNG: linguetta (54,51,46) = `#36332E`, fondo (41,39,35) = `#292723`, campo (31,29,26) = `#1F1D1A`. I token arrivano sullo schermo identici e la differenza fra linguetta e fondo si vede.
- Il rilevatore di `impeccable` sul CSS non segnala nulla. Mi sono fermato al secondo giro.

## 5. Note per Android

**`CmColors.kt`** (ui-tokens): al posto di `MasterTab`, `MasterBed`, `MasterField`, `MasterSoft` e `MasterIcon`, privati in `MasterDock.kt`, vanno i token condivisi: `masterWell 0xFF1F1D1A`, `masterBed 0xFF292723`, `masterTab 0xFF36332E`, `masterHigh 0xFF413E39`, `masterKey 0xFF4B4742`, `masterLine 0xFF46433E`, `masterEdge 0xFF5E5A55`, `masterTrack 0xFF58554F`, `masterDot 0xFF6C6863`, `masterText 0xFFF6F3EC`, `masterText2 0xFFC7C2B9`, `masterText3 0xFFA6A199`, `masterSand 0xFFDCCFBC`, `masterPrimary 0xFFEFEBE1`, `masterOnPrimary 0xFF24211D`. Per la scintilla si riusa `modelOpus` (`0xFFD97757`).

**Il gemello caldo su Compose.** Sul web basta ridefinire le variabili CSS. In Compose i componenti leggono `CmColors` direttamente, quindi serve una tavolozza che si possa sostituire:
- in `CmColors.kt`, `data class CmPalette(bg, surfaceLow, surface, surfaceHigh, line, text, text2, actionIcon, primary, onPrimary, stale, stepsBg, stepsDot, briefTrack, briefRing, briefSecondary)` con `CmPalette.Cool` (i valori di oggi) e `CmPalette.Carbone` (i `master*`), più `val LocalCmPalette = staticCompositionLocalOf { CmPalette.Cool }`;
- in `SessionSheet.kt`, dove oggi si applica `masterChat(LocalMasterChatStyle.current, sh)`, un `CompositionLocalProvider(LocalCmPalette provides CmPalette.Carbone)` attorno alla conversazione della master;
- nei composable della chat `CmColors.x` diventa `LocalCmPalette.current.x`: `SheetHeader`, `ClaudeBubble`, `MineBubble`, `UserBubble`, `StepsCard`, `StepDots`, `ToolLine`, `FileChip`, `SmallAction`, `PromptBox`, `NextStep`, `Composer`. `ElsewherePill`, `SessionBadge` e i colori di stato (`waiting`, `gone`, `stepsFailBg`, `briefWarn`, `briefAlertRing`) restano su `CmColors`.

**`MasterDock.kt`**
- `Row(...).background(CmColors.masterTab)`; il filo d'alto si disegna con `drawBehind`: linea di 1 dp `masterText.copy(alpha = .07f)`. Da chiusa sparisce il `Box` di 1 dp `CmColors.line` sopra la linguetta.
- La riga mono: `MonoSmall.copy(color = CmColors.masterText2)`, preceduta da `MasterSpark(13.dp)`. È la `Canvas` a 8 raggi `modelOpus` della riga «al lavoro» in `SessionSheet.kt`, quella che oggi gira e pulsa accanto a «sta pensando»: va estratta in un composable condiviso `MasterSpark(size, spinning)` e usata in entrambi i posti, così la scintilla della linguetta e quella della chat sono lo stesso segno.
- I due `FilledTonalIconButton`: `containerColor = CmColors.masterKey`, `contentColor = CmColors.masterText`.
- Movimento: nella linguetta `MasterSpark` gira (0→360° in 2400 ms lineari, la stessa `tween(2400)` della riga «al lavoro») solo con lo stato `busy`, non con `awaiting`. Il movimento ridotto segue la stessa variabile `off` che ferma oggi la stella della chat.

**`SessionSheet.kt`**
- `MasterChatStyle.CARBONE -> background(CmColors.masterBed)`, che diventa lo stile predefinito della master.
- `Composer`: `bar` su `CmColors.masterBed`, come oggi su `MasterBed`.
- `OutlinedTextFieldDefaults.colors(unfocusedBorderColor = masterEdge, focusedBorderColor = modelOpus, unfocusedContainerColor = masterWell, focusedContainerColor = masterWell, cursorColor = modelOpus)`. Bordo a riposo 1,5 dp.
- Segnaposto `masterText2`, suggerimento `masterText3`, «Usa» `masterSand` al posto di `actionIcon`.
- Bottone pieno `masterPrimary`/`masterOnPrimary`.
- `LocalTextSelectionColors` con `backgroundColor = masterSand.copy(alpha = .28f)`.
- `ClaudeBubble`: la scintilla `MasterSpark(14.dp)` in testa alla riga delle azioni, solo dentro la master (`LocalCmPalette.current == CmPalette.Carbone`).
- I golden Paparazzi della master (home e foglio) vanno registrati di nuovo.

## 6. Rischi e contromosse

| Rischio | Contromossa |
|---|---|
| «È grigio, non si vede che è diversa»: sul telefono il caldo è troppo timido | Si alza il chroma sullo stesso tono 80, senza cambiare idea: variante «più caldo» linguetta `#37332B`, fondo `#2A2620`, campo `#201D18` (C 0,013; contrasti identici: avorio 11,34:1, pietra 7,09:1). Oltre 0,016 diventa marrone e si torna al problema di B |
| «Sembra fango» sul pannello OLED del Pixel, che spinge i caldi | Si scende verso il quasi-neutro di claude.ai, stessa luminosità e chroma 0,004: `#343331` / `#282725` / `#1E1D1C` (avorio 11,39:1, pietra 7,12:1). Il caldo lo fanno allora l'avorio e il contrasto con le card blu |
| La scintilla corallo letta come il rosso «produzione» | Il corallo compare solo come forma ✻, mai come pallino o testo. Se crea dubbi, la scintilla ferma diventa avorio e il corallo resta solo quando gira (al lavoro) e sul fuoco del campo |
| La scintilla ruba 20 px alla riga mono: «ctx 34 %» sparisce prima. Sul telefono era già tagliato | Dalla riga della linguetta si toglie «ctx»: sta già nella testata della master aperta. La riga diventa «✻ MASTER · 12:50 · Opus 5.5» e sta intera nei 412 dp |
| La master chiusa, più chiara delle card, pesa più della card «ti aspetta» | Il peso viene dalla luminosità, non dal colore: se disturba, il fondo scende a `#24211E` (L 0,25) e la linguetta resta `#36332E`. La differenza fra le due cresce e il blocco pesa meno |
| Da aperta tutta la pagina accesa costa più batteria del nero su OLED | Il costo è minimo: grigio molto scuro (luminanza relativa 2 %), solo mentre la master è aperta. Se Franz lo nota, si scurisce solo lo sfondo della chat (`#24211E`) lasciando invariati linguetta e campo |
| «Mi aspettavo più Claude» | Si potenzia senza aggiungere colore: le risposte della master nel serif delle risposte di claude.ai. Lo tengo fuori perché la proposta B «Editoriale» del 05/10 non è stata scelta. Si prova solo se lo chiede lui |
