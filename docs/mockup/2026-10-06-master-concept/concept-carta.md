# Concept «Carta»: la master è l'unico foglio chiaro dell'app

Immagine da giudicare: `concept-carta.png` (web app vera, dati di prova, 412×915 a densità 2). CSS: `concept-carta.css`.
Campioni della tinta sera, disegnati e non fotografati dall'app: `campioni-carta-sera.png`.

## 1. L'idea

**In una frase:** in un'app tutta nera e grigio-blu, la master è un foglio di carta avorio smorzata con il testo d'inchiostro. Chiusa spunta dal fondo della home come la linguetta di una cartellina, con il campo dentro. Aperta è un'intera pagina di carta.

**Perché si riconosce a colpo d'occhio.** Tutti i tentativi precedenti (lilla, alone, corallo, terracotta) hanno lavorato sulla **tinta**. Questo lavora sulla **luminosità**, che è il canale che l'occhio separa per primo, prima ancora di leggere.
- **Chiusa:** nella home la carta è l'unica superficie chiara e calda. Le card sono a luminosità OKLCH 0,27 e fredde (tinta 262); la carta va da 0,85 a 0,89 ed è calda (tinta 88). Non serve un filo, un bordo o una luce: il blocco in fondo è *un altro materiale*.
- **Aperta:** ogni altra conversazione dell'app è nera, quella della master è carta dall'alto in basso: linguetta, riga del modello, chat, Prossimi e campo. Entrare nella master è come girare pagina. Basta un decimo di secondo per capire dove si è, anche con il telefono preso al volo.

**Perché risolve quello che non andava nella terracotta B. Coi numeri:**
- La linguetta B (#36221A) ha luminosità OKLCH **0,275**. La superficie delle card (#23272E) ha **0,272**: sono alla stessa luminosità. La B si distingueva dalle card solo per la tinta, e a luminosità così basse la tinta si percepisce poco (sul telefono, nel buio, il colore «si spegne»). Il risultato, visibile negli screenshot di Franz delle 18:58, è una card marroncina, non un'altra cosa.
- Un arancio scuro poco saturo (croma 0,03) a luminosità 0,22-0,27 è per definizione marrone. Franz l'ha detto con «il colore non mi convince»: vicino al corallo delle icone e del bordo, diventa cotto e fango.
- Il campo della B (#180E0A) è *più scuro* del fondo: un buco con il bordo corallo, non un posto dove scrivere.
- La carta tiene quasi la stessa croma della terracotta (0,030 contro 0,034) ma la porta a 0,85 di luminosità. A quella luminosità la stessa poca croma si legge come avorio, cioè carta, non come marrone. E del corallo non resta niente in superficie: c'è solo nella stellina di Claude mentre lavora, che è il suo marchio.

## 2. Tavolozza

### Token

| Token | Carta | Y (lum. relativa) | OKLCH | Sera | Y | Uso |
|---|---|---|---|---|---|---|
| `--m-tab` | #E0D9C6 | 0,696 | 0.885 0.026 90 | #BEB7A5 | 0,475 | linguetta, campo, riquadro Prossimi |
| `--m-paper` | #D6CDB8 | 0,614 | 0.850 0.030 88 | #B6AD99 | 0,421 | fondo intorno al campo, sfondo della chat, riga del modello |
| `--m-well` | #CCC3AD | 0,549 | 0.818 0.031 86 | #ADA38F | 0,371 | carta in ombra: pillola del modello, passaggi, tasti tondi, menu |
| `--m-bubble` | #C5BAA4 | 0,497 | 0.793 0.033 85 | #A69B87 | 0,333 | bolla di Franz, tasto ▶ della risposta, codice in linea |
| `--m-btn` | #D1CAB8 | 0,593 | inchiostro all'8 % sulla linguetta | #B0AA99 | | tasti ▶ ▲ ▼ della linguetta |
| `--m-line` | #B7AB95 | | | #968B77 | | fili, binari degli anelli (solo decoro) |
| `--m-edge` | #787165 | | | #5E574C | | bordo del campo (≥ 3:1) |
| `--m-dot` | #9A907E | | | #807663 | | puntini dei passaggi |
| `--m-ink` | #25201A | | | #14100B | | testo, titolo, invio pieno |
| `--m-ink-2` | #554D42 | | | #3F372D | | riga MASTER, ore, meta, segnaposto, icone |
| `--m-wait` / `--m-wait-bg` | #714900 / #E8C17F | 0,568 (bg) | bg 0.83 0.095 80 | #563400 / #C4A269 | | ambra «ti aspetta» girata per la carta |
| `--m-good` / `--m-good-bg` | #206234 / #ABD8B3 | | | #004A1D / #90B697 | | verde «ha finito» |
| `--m-busy`, `--m-ring` | #3051A4, #4167AD | | | #1C398B, #2C4F94 | | blu al lavoro, anelli quota e contesto |
| `--m-alert` / `--m-fail-bg` | #9E2D26 / #E1C7C3 | | | #820C0C / #C0A7A3 | | rosso errore, passaggi falliti |
| `--m-advice` | #61459F | | | #4B2C84 | | viola consiglio |
| `--m-opus` | #A94D2C | | | #8E3510 | | stellina di Claude al lavoro (unico corallo) |

### Contrasti WCAG (calcolati con python sulle coppie usate)

| Coppia | Testo / fondo | Carta | Sera | Soglia |
|---|---|---|---|---|
| Riga «MASTER · ora · modello» (mono 12) | `ink2` su `tab` | 5.90 | 5.85 | 4.5 |
| Titolo della linguetta | `ink` su `tab` | 11.47 | 9.48 | 4.5 |
| Glifo ▶ ▲ sul tasto tonale | `ink` su `btn` | 9.88 | 8.17 | 3 |
| Anello del badge | `ink` su `tab` | 11.47 | 9.48 | 3 |
| Testo nel campo | `ink` su `tab` | 11.47 | 9.48 | 4.5 |
| Segnaposto e suggerimento in corsivo | `ink2` su `tab` | 5.90 | 5.85 | 4.5 |
| Bordo del campo sul fondo | `edge` su `paper` | 3.05 | 3.20 | 3 |
| Bordo del campo sul campo | `edge` su `tab` | 3.43 | 3.57 | 3 |
| + e invio a riposo (cerchio carta in ombra) | `ink2` su `well` | 4.74 | 4.68 | 3 |
| Invio pieno: freccia carta su inchiostro | `tab` su `ink` | 11.47 | 9.48 | 4.5 |
| Riga sotto il campo / stato relay | `ink2` su `paper` | 5.26 | 5.25 | 4.5 |
| Pillola del modello | `ink` su `well` | 9.21 | 7.59 | 4.5 |
| 5h, ctx, ora di azzeramento | `ink2` su `paper` | 5.26 | 5.25 | 4.5 |
| Anello quota e contesto | `ring` su `paper` | 3.53 | 3.55 | 3 |
| «ledger-api ti aspetta» | `ink` su `waitbg` | 9.51 | 7.86 | 4.5 |
| «Rispondi», mano, freccia | `wait` su `waitbg` | 4.66 | 4.62 | 4.5 |
| Pillola «ha finito» | `good` su `goodbg` | 4.62 | 4.67 | 4.5 |
| «Casa» | `ink` su `paper` | 10.22 | 8.50 | 4.5 |
| Risposta di Claude | `ink` su `paper` | 10.22 | 8.50 | 4.5 |
| Bolla di Franz | `ink` su `bubble` | 8.41 | 6.91 | 4.5 |
| Ora, «dal telefono», meta | `ink2` su `paper` | 5.26 | 5.25 | 4.5 |
| Tasto ▶ della risposta | `ink2` su `bubble` | 4.33 | 4.27 | 3 |
| Passaggi: riassunto e codice | `ink2` su `well` | 4.74 | 4.68 | 4.5 |
| Passaggi falliti | `alert` su `failbg` | 4.61 | 4.63 | 4.5 |
| Prossimi: titolo | `ink2` su `tab` | 5.90 | 5.85 | 4.5 |
| Prossimi: righe | `ink` su `tab` | 11.47 | 9.48 | 4.5 |
| Prossimi: lucchetto ambra | `wait` su `tab` | 5.62 | 5.57 | 3 |
| Stella di Claude al lavoro | `opus` su `paper` | 3.51 | 3.53 | 3 |
| Ambra (attesa) come testo | `wait` su `paper` | 5.00 | 5.00 | 4.5 |
| Verde (finito) | `good` su `paper` | 4.65 | 4.72 | 4.5 |
| Blu (al lavoro) | `busy` su `paper` | 4.68 | 4.70 | 4.5 |
| Rosso (errore) | `alert` su `paper` | 4.65 | 4.70 | 4.5 |
| Viola (consiglio) | `advice` su `paper` | 4.64 | 4.73 | 4.5 |

Badge sulla carta. Sulla linguetta i riempimenti dei badge non arrivano a 3:1: giallo #F4D03F 1,07, verde #2ECC71 1,49, grigio #9B9B9B 1,97, rosso #E74C3C 2,71. Per questo il badge porta un **anello d'inchiostro di 2 px**: anello sulla linguetta 11,47, e ogni riempimento contro l'anello resta tra 3,46 (viola) e 10,72 (giallo). Il colore della sessione resta lo stesso; cambia solo il contorno.

### Come evita i colori di stato

- La carta è avorio a **croma 0,030**. L'ambra di stato #FFB020 ha croma 0,165, cinque volte e mezzo tanto. Anche la tinta è diversa (88 contro 75). La carta si legge bianco caldo, non giallo.
- I colori di stato non spariscono: dentro la master tornano **gli stessi cinque significati, girati per la carta** (stessa tinta, più scuri per il testo, con fondi chiari per le pillole). La pillola «ledger-api ti aspetta» resta ambra, come un post-it giallo su un foglio.
- La master non prende nessun colore per sé. La sua firma è il materiale (carta e inchiostro), non una tinta, quindi non ruba niente a nessuno.

### Come convive con le card grigio-blu

Le card restano identiche (la home è il termine di paragone). Card e carta si oppongono su due assi insieme: luminosità (0,27 contro 0,85) e temperatura (fredda contro calda). Tra la superficie delle card e la carta il contrasto è 9,5:1, quindi non si confondono nemmeno in visione periferica. Il nero della home fa da cornice: la carta non tocca mai una card.

## 3. Specifica per elemento

Il meccanismo: dentro la master (`.pane > .closed` chiusa, `.slide` aperta) **i token dell'app vengono riassegnati** alla tavolozza carta (`--text` → `--m-ink`, `--surface` → `--m-well`, `--wait` → `--m-wait`, eccetera). Ogni componente che usa le variabili si gira da solo. Gli override puntuali servono solo dove il codice ha colori cablati (corallo della linguetta, bordo del campo, pillola con `--t` in linea).

### Chiusa (in fondo alla home)

- **Linguetta** (`.dock`): `--m-tab`, angoli 26 px in alto sul nero. È il foglio più chiaro, la «testa» della cartellina.
- **Riga MASTER** (`.dock .mono`): `--m-ink-2`, peso 500, mono 12 px come oggi. Niente corallo.
- **Titolo** (`.dock b`): `--m-ink`, 16 px, 600.
- **Tasti ▶ ▲** (`.dock .ib`): cerchio 44 px in `--m-btn` (inchiostro all'8 % sulla linguetta), glifo `--m-ink`. Al passaggio: `brightness(.95)`, scurisce invece di schiarire.
- **Badge** (`.dock svg[role=img]`): anello d'inchiostro 2 px (`box-shadow: 0 0 0 2px`), tondo o con raggio del 23 % se il badge è quadrato. Il respiro del badge resta com'è.
- **Fondo intorno al campo** (`.pane > .closed`): `--m-paper`. Prosegue senza giunture sotto la linguetta fino al bordo dello schermo: linguetta e campo sono un solo foglio.
- **Campo** (`.field.master`): fondo `--m-tab` (lo stesso foglio chiaro della linguetta, il posto dove si scrive), bordo 1,5 px `--m-edge`. Col fuoco: 2 px `--m-ink`. Testo `--m-ink`, segnaposto `--m-ink-2`. «Usa» in `--m-ink` 600. Il + in `--m-ink-2`. L'invio a riposo è un cerchio `--m-well`; pieno (l'unico bottone pieno della schermata) diventa un cerchio d'inchiostro con la freccia color carta.
- **Riga sotto il campo**: sul telefono è la riga piccola di stato che ci cade sopra («Dati di prova», e in produzione «relay non raggiungibile» / «connessione»). Va in `--m-ink-2` piena, sennò sulla carta sparirebbe. Sul desktop la riga «Invio manda · Maiusc+Invio a capo» (`.keys`) è anch'essa `--m-ink-2`.

### Aperta (pagina intera)

- **Linguetta in cima** (`.slide .dock.expanded`): stessa `--m-tab`, angoli tondi in alto che **mostrano il nero** (il fondo carta parte da `.slide > .body`, non da `.slide`: corretto al giro 2). Tasto ▼ come ▲.
- **Riga modello/quota** (`.chat > header`): su `--m-paper`, filo sotto `--m-line`. Pillola «Opus 5.5 · high» su `--m-well`, testo `--m-ink`. 5h, ctx e ora in `--m-ink-2`. Anelli in `--m-ring`, su binario `--m-line`; arancio e rosso dei livelli alti con le varianti carta di ambra e rosso.
- **Pillola «ledger-api ti aspetta»** (`.chat > .pill`): fondo `--m-wait-bg`, testo `--m-ink`, mano, «Rispondi» e freccia in `--m-wait`. Se l'avviso è «ha finito»: `--m-good-bg` / `--m-good`.
- **Casa** (`.tohome`): `--m-ink` 500, sottolineata con filo `--m-edge` a 4 px. Sulla carta un link è sottolineato, non azzurro. La casa della master (vista «Casa») va su **carta puntinata**: i puntini oggi bianchi al 7 % diventano inchiostro al 13 %.
- **Sfondo della chat** (`.slide .lines`): trasparente sopra `--m-paper`. Tutta la conversazione è su carta.
- **Risposte di Claude** (`.claude`, `.ctext`): testo `--m-ink` libero sulla carta, senza bolla, come claude.ai in chiaro. Link in inchiostro sottolineati, codice in linea su `--m-bubble`.
- **Bolle di Franz** (`.me .bubble`): `--m-bubble`, testo `--m-ink`, stessa forma di oggi. Ora e «dal telefono» in `--m-ink-2`, spunte di stato nelle varianti carta.
- **Gruppi di passaggi** (`.steps`): `--m-well`, riassunto e codice `--m-ink-2`, puntini `--m-dot`. Falliti: fondo `--m-fail-bg`, testo `--m-alert`.
- **Prossimi** (`.box`): foglio `--m-tab` con filo `--m-line`, quindi un foglietto più chiaro appoggiato sulla carta. Titolo mono `--m-ink-2`, righe `--m-ink`, frecce `--m-ink-2`, lucchetto e barretta di sblocco in `--m-wait`.
- **Campo e suo fondo**: lo stesso campo della chiusa (`--m-tab`, bordo `--m-edge`), e il suo fondo *è* la carta della chat. Il blocco della master chiusa si è disteso a pagina intera.
- **Menu, fogli, selezione:** il menu del + e i fogli (salva decisione) prendono `--m-well` dai token. La selezione del testo è inchiostro al 18 %, il cursore `--m-ink`.

### Differenza tra linguetta e resto del blocco

La linguetta è **un gradino più chiara** della carta: luminosità OKLCH +0,036, contrasto 1,12:1. Nel PNG si vede senza cercarla. È la stessa logica della B, che Franz aveva approvato (linguetta un poco più chiara del fondo), ma un filo più sottile della B (+0,058). Se al polso o in tasca risulta troppo timida, si alza solo `--m-tab` a #E4DDCB (+0,05) senza toccare altro.

### Notte: quanta luce fa

Misure fatte sugli screenshot del banco, in luminanza relativa (0 nero, 1 bianco pieno):

| | Massimo | Media della schermata |
|---|---|---|
| Base, master aperta (oggi) | 0,903 (il testo bianco) | 0,026 |
| Carta, master aperta | **0,696** (la linguetta) | 0,605 |
| Base, home | 0,903 | 0,065 |
| Carta, home | 0,903 (il testo bianco delle card) | 0,157 (carta sul 20 % dello schermo) |
| Sera, master aperta (stima dalle tinte, non resa) | **0,475** | circa 0,42 |

- **Non abbaglia** nel senso del picco: niente sulla carta è più chiaro della linguetta (0,696). Il tasto «Approva» che già c'è nella home vale 0,759 e il testo bianco dell'app 0,903. La carta resta sotto entrambi: non c'è un punto che spara.
- **Ma fa più luce, e va detto:** aperta, la media sale da 0,026 a 0,605, circa 23 volte. È il prezzo dell'idea. Al buio la pupilla si adatta in un secondo a una pagina così, ma la prima apertura si sente. Per questo l'apertura **sale in 320 ms** invece di accendersi, e c'è la tinta **sera**: stessa carta con la luce abbassata, massimo 0,475 e media circa 0,42. La sera tiene tutti i contrasti AA della tabella sopra. Si accende dall'orario (21-7) o da un'impostazione «Carta: giorno / sera / automatica», con `:root[data-carta="sera"]` sul web e `CmPalette.CartaSera` su Android.

### Movimento

Uno solo, all'apertura: la pagina di carta sale di 24 px e passa da trasparente a piena in **320 ms**, `cubic-bezier(.16, 1, .3, 1)` (uscita esponenziale). Serve a non accendere di colpo una pagina chiara nel buio: la luce sale. Con **movimento ridotto**: solo la dissolvenza, 160 ms lineare, senza spostamento (meglio di un'accensione secca, che di notte è la cosa peggiore). Alla chiusura la carta sparisce subito: diventare buio non disturba. Niente luci, aloni, respiri o filtri animati. Nel CSS del banco la regola è scritta in commento e spenta (`animation: none`): al giro 2 lo screenshot headless aveva fotografato la pagina a metà dissolvenza.

## 4. Il CSS e i giri

- `concept-carta.css` contiene i token `--m-*`, la variante `:root[data-carta="sera"]` e la riassegnazione dei token dentro `.pane > .closed, .slide`, più una ventina di override. Il rilevatore di Impeccable non ha trovato niente da segnalare (`[]`).
- **Giro 1:** l'idea regge al primo colpo. Tre difetti: gli angoli tondi della linguetta aperta mostravano carta invece del nero, la pillola ambra era troppo accesa, la carta tirava al grigio sabbia.
- **Giro 2:** fondo spostato su `.slide > .body`, ambra più quieta (#E8C17F, «Rispondi» a #714900 per restare a 4,66), carta un po' più avorio (croma da 0,024 a 0,030). Lo screenshot della pagina aperta è uscito a metà animazione.
- **Giro 3** (finale, `concept-carta.png`): animazione spenta nel banco, tutto a posto. Mi sono fermato.
- `concept-carta-sera.css` (importa la carta e abbassa i token) è pronto ma **non è stato reso**: i tre giri erano finiti. La sera si vede in `campioni-carta-sera.png`, disegnato con PIL dalle stesse tinte e dichiarato come campioni, non screenshot.

## 5. Note per Android

Oggi `CmColors` è un `object` di costanti, e `SessionSheet.kt` lo chiama 166 volte. La carta su Android è quindi una **tavolozza a scopo**, non un cambio di colori sparso:

- **`CmColors.kt`** (`ui-tokens`): una `data class CmPalette(bg, surfaceLow, surface, surfaceHigh, line, text, text2, actionIcon, primary, onPrimary, waiting, idle, busy, gone, goneDim, stale, advice, prod, stepsBg, stepsFailBg, stepsDot, modelOpus, briefRing, briefTrack, briefWarn, briefGood, briefAlert, …)` con `CmPalette.Dark` (i valori di oggi), `CmPalette.Carta` e `CmPalette.CartaSera` (i `--m-*` sopra), più `val LocalCmPalette = staticCompositionLocalOf { CmPalette.Dark }`. Token nuovi della sola master: `masterTab` (#E0D9C6), `masterPaper` (#D6CDB8), `masterBtn` (#D1CAB8), `masterEdge` (#787165), `waitingBg` (#E8C17F), `goodBg` (#ABD8B3).
- **`MasterDock.kt`**: `MasterTab`, `MasterBed`, `MasterField`, `MasterSoft`, `MasterIcon` (righe 46-50) spariscono a favore di `cm.masterTab`, `cm.masterPaper`, `cm.text2`. I due `FilledTonalIconButton` passano da `containerColor = CmColors.modelOpus.copy(alpha = 0.18f)` a `cm.masterBtn`, con icona `cm.text`. Il badge prende `Modifier.border(2.dp, cm.text, shape)` con la stessa forma (cerchio o quadrato arrotondato). Il filo `MasterThread` e la riga `CmColors.line` sotto la linguetta non servono più.
- **`SessionSheet.kt`**:
  - Intorno al blocco chiuso e alla master aperta: `CompositionLocalProvider(LocalCmPalette provides if (sera) CmPalette.CartaSera else CmPalette.Carta)`, più `MaterialTheme(colorScheme = lightColorScheme(...))` dagli stessi token, così i default M3 (cursore, ripple, `TextField`) sono quelli chiari.
  - Dentro, i riferimenti `CmColors.x` vanno a `LocalCmPalette.current.x`. È un cambio meccanico e fuori dalla master non cambia niente, perché il default è `Dark`.
  - `masterChat`: un nuovo `MasterChatStyle.CARTA -> background(cm.masterPaper)` (e via gli stili lilla).
  - `Composer`/`bar`: `unfocusedContainerColor = MasterField` / `focusedContainerColor` → `cm.masterTab`. Bordo `cm.masterEdge` 1,5 dp, col fuoco `cm.text` 2 dp. Invio pieno `containerColor = cm.text`, icona `cm.masterTab`.
- **Barra di sistema:** la carta arriva al bordo basso dello schermo (home con la master chiusa e master aperta). Lì serve `WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true`, sennò la maniglia dei gesti resta bianca sulla carta. Torna a `false` sulle schermate delle altre sessioni. La barra di stato in alto resta scura, perché sopra la master aperta c'è ancora la testata nera della home.
- **Web, quando si porta nell'app:** i token `--m-*` vanno in `web/src/app.css`. La riassegnazione va su `.closed` (`HomePane.svelte`) e su `.slide > .body`. I colori cablati da togliere sono `.dock .mono` #E2A58C e `.dock .ib` in `MasterDock.svelte`, e `.field.master` in `Composer.svelte`.
- **Goldens Paparazzi** delle schermate con la master (chiusa e aperta) da registrare di nuovo.

## 6. Rischi e cosa potrebbe non piacere a Franz

- **«Di notte è troppa luce.»** È il rischio vero: aperta, la schermata fa 23 volte la luce di oggi, anche se il picco è più basso. *Contromossa:* tinta sera automatica dalle 21. Se non basta, ultima riserva: la carta resta su linguetta, riga del modello e campo, e la chat torna notte. Però va contro la sua richiesta di allineare lo sfondo della chat, quindi solo se lo chiede lui.
- **«Sembra un tema chiaro rotto, un'altra app.»** Un'isola chiara in un'app scura può sembrare un errore, la prima volta. *Contromossa:* è coerente in tutti e due gli stati (stesso foglio, chiuso e disteso) e ha la forma della linguetta, che Franz conosce già. Un errore non ha una forma: questo sì.
- **«Beige, cartone, sporco».** Sul telefono a luminosità bassa l'avorio smorzato può scivolare verso il grigio sabbia, soprattutto la sera (vedi i campioni). *Contromossa:* si alza solo la linguetta (#E4DDCB) o la croma della carta di 0,005. È una manopola, non un rifacimento.
- **«Mi manca il colore di Claude».** Il corallo sparisce dalla superficie (Franz il 06/10 alle 09:45 l'aveva voluto sul campo). *Contromossa:* una firma sola in argilla scura #913B1A, 4,6:1 sulla carta, sulla riga «MASTER». Va bene solo lì: più argilla tornerebbe vicina all'ambra scura di «ti aspetta».
- **L'anello sul badge sembra un adesivo.** *Contromossa:* al posto dell'anello, una «presa» d'inchiostro di 28 px con il badge dentro, cioè un pezzetto di notte incastonato nella carta dove i colori di stato restano quelli tarati sul nero.
- **La pillola ambra diventa la cosa più forte della pagina.** È giusto che sia così (una sessione aspetta Franz). Se disturba, `--m-wait-bg` scende a croma 0,08.
- **Pezzi non provati al banco:** la vista «Casa» della master (`MasterHome`), i fogli di dialogo, il menu del + e la plancia desktop/tablet (una colonna di carta accanto a colonne nere) non compaiono nel PNG. I token li coprono, ma `MasterHome` può avere colori cablati da passare uno a uno.
- **Costo su Android:** il passaggio da `CmColors` costante a tavolozza a scopo è il lavoro più grosso (meccanico ma esteso: 166 riferimenti solo in `SessionSheet.kt`). In compenso è riusabile per qualunque variante futura, sera compresa.
