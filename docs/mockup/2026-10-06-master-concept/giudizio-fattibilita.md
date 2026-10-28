# Giudizio di fattibilità sui concept della master (06/10)

Giudice: fattibilità (Compose + Svelte). Ho guardato i quattro `concept-<k>.png`, letto i quattro `.md` e i quattro `.css`, ricalcolato con Python ogni rapporto di contrasto citato, campionato i pixel degli screenshot e confrontato le note Android con il codice vero (`MasterDock.kt`, `SessionSheet.kt`, `CmColors.kt`, `web/src/lib/badge.ts`, `contract/README.md`).

**Vincitore: carbone.** È l'unico concept che si porta su Android senza riscrivere il tema, non tocca il badge né i colori di stato, e lo screenshot mostra esattamente i token dichiarati. Il suo punto debole è il colpo d'occhio da chiusa, che resta sottile. Si corregge con due innesti economici, senza cambiare idea.

## Punteggi (1-10)

| Criterio | carbone | carta | profondità | firma |
|---|---|---|---|---|
| Android e web con gli stessi token | 8 | 4 | 5 | 6 |
| Dimensione e rischio della modifica | 7 | 3 | 5 | 5 |
| Contrasto WCAG AA (ricalcolato) | 8 | 8 | 7 | 9 |
| Prestazioni su un telefono medio | 10 | 8 | 6 | 9 |
| Passaggio chiusa/aperta, badge, colori di stato | 7 | 5 | 6 | 3 |
| Il CSS produce quello che il concept dichiara | 9 | 8 | 5 | 8 |
| **Totale (media)** | **8,2** | **6,0** | **5,7** | **6,7** |

## Verifiche fatte

- **Contrasti.** Tutti i rapporti dichiarati tornano entro 0,03. L'unico che non torna è in firma, «cornice sulla card 4,81»: vale contro `--low` #1B1F26, ma contro `--surface` #23272E fa 4,36. Supera comunque la soglia.
- **Pixel.** carbone: linguetta #36332E, fondo #292723, chat #292723, identici ai token. carta: linguetta #E0D9C6, chat #D6CDB8, identici. firma: cornice #8F8A80, fondo #1C1B1A, linguetta #2A2927, identici. profondità: **il centro della chat aperta è #12100D, cioè 1,10:1 sul nero**.
- **Codice.** `CmColors` è un `object` di costanti, e `SessionSheet.kt` lo chiama 166 volte: un tema sostituibile a runtime oggi non esiste. In `badge.ts:43` vale `breathes = busy || awaiting`, e il contratto dice che `awaiting` è una sessione che lavora a un prompt partito dal polso.

## carbone: 8,2 (vincitore)

**Forti**
- Il meccanismo è il più pulito: dentro `.pane > .closed` e `.slide` i token freddi dell'app vengono riassegnati ai gemelli caldi. Pochissimi `!important`, e solo dove ci sono colori scritti a mano.
- Lascia invariati il badge, la pillola ambra «ti aspetta» (testo 9,5:1, «Rispondi» 6,7:1) e i passaggi falliti. Nessun colore di stato viene rubato: chroma dei grigi ≤ 0,014.
- Da aperta lo sfondo della chat coincide davvero con il fondo del blocco (#292723 campionato): la richiesta esplicita di Franz è soddisfatta.
- Niente ombre, niente gradienti. L'unico movimento è una rotazione di una maschera di 13 px. Costo nullo.
- Le note Android dicono la verità sul costo: elencano la `CmPalette` e un `LocalCmPalette` limitato ai composable della chat.

**Deboli**
- Da chiusa lo stacco dalle card è affidato alla sola temperatura. Linguetta contro card: 1,19:1. Fondo contro card: 1,11:1. Chroma 0,010. Sul PNG si vede; sul Pixel al buio può leggersi come «un'altra card grigia».
- **Errore di fatto.** La scintilla gira solo con `aria-label="busy"`, e il documento motiva la scelta dicendo che `awaiting` vuol dire «aspetta Franz». È falso: `awaiting` lavora a un prompt partito dal polso. Con la master in `awaiting` la scintilla resta ferma.
- Il bordo del campo a riposo, `--m-edge` #5E5A55, fa 2,18:1 sul fondo e 2,46:1 sull'interno: sotto i 3:1 di un confine di componente. È vero che oggi è così anche altrove, ma non è una giustificazione.
- `--b-ring` sabbia #DCCFBC contro l'ambra d'avviso #FFC46B: **1,03:1** di luminanza. L'anello neutro e quello d'avviso si distinguono solo per la tinta, su un arco di 3 px.
- La scintilla ruba circa 20 px alla riga mono, che ora finisce con un «·» appeso. Togliere «ctx» è un cambio di testo e serve l'ok di Franz.
- «Casa» diventa una pastiglia: è una forma in più che somiglia a un bottone.

## carta: 6,0

**Forti**
- È il riconoscimento più forte in assoluto: luminosità OKLCH 0,85 contro 0,27. Si riconosce in un decimo di secondo.
- Le tabelle di contrasto sono le più complete. Ha capito che i badge sulla carta non arrivano a 3:1 (giallo 1,07:1) e ha previsto l'anello d'inchiostro.
- Dichiara da sé il costo della luce: media dello schermo ×23.

**Deboli**
- **Va contro un vincolo fisso di `basi.md`**: «Tema scuro, nero di fondo». La master diventa un tema chiaro.
- Su Android è il lavoro più grosso:
  - la tavolozza completa con tutti gli stati rigirati (attesa, finito, al lavoro, errore, consiglio, opus, anelli);
  - un `MaterialTheme(lightColorScheme)` annidato;
  - `isAppearanceLightNavigationBars` da accendere e spegnere;
  - l'anello sul `SessionBadge`, e i colori di `ElsewherePill` e `MasterHome` da rifare.
- Il passaggio chiusa/aperta diventa un salto di luminanza. Va mascherato con una dissolvenza di 320 ms, che nel banco è stata spenta perché rovinava lo screenshot.
- La tinta «sera» non è mai stata resa sull'app: c'è solo `campioni-carta-sera.png`, disegnato con PIL.
- `body:has(.pane > .closed) .demo` cambia la riga di stato «relay giù» in tutta l'app. Su schermate nere diventerebbe #554D42 sul nero, illeggibile, se il pannello home resta montato.

## profondità: 5,7

**Forti**
- Usa la condizione di stato giusta, `.breathe` (busy più awaiting), e il cambio di tinta è il più misurato: chroma 0,008.
- Linguetta contro letto: 1,30:1, e 1,55:1 dalla cima. È la differenza lieve più leggibile dei quattro.
- L'ombra che spegne la card sotto la master (campionata: #16191F contro #1B1F26) dice «sta sopra» senza colore.

**Deboli**
- **Il punto chiave non regge.** Il centro della chat aperta è #12100D, 1,10:1 sul nero. Dove Franz legge, la chat torna praticamente nera, e il «panino» che il concept dice di risolvere ritorna.
- È il più costoso da portare su Android: tre gradienti più la vignettatura sulla chat, tre ombre verso l'alto da disegnare a mano (le elevazioni di Compose cadono in basso), `innerShadow` e `dropShadow`, il riflesso animato con `Animatable`, e il letto del campo con angoli e margine nuovi. Molti valori sono alfa composte (`--m-rim`, `--m-glint`, `--m-crease`), non token piatti condivisibili.
- Sul web, `box-shadow` fino a 56 px di sfocatura sopra una lista che scorre, e un riflesso che riparte a ogni apertura e chiusura.
- `.slide .chat > .bar` prende `margin-top` e raggio: cambia la geometria del campo da aperta, quindi il passaggio da chiusa ad aperta non è più lo stesso oggetto.
- «Casa», «Usa» e ↗ restano blu dentro la grafite. Bordo del campo a riposo 1,74:1. Il riflesso è quasi invisibile nel PNG.

## firma: 6,7

**Forti**
- Il riconoscimento non dipende dal colore: la prova in bianco e nero (`concept-firma-bn.png`) è reale e convincente.
- La cornice #8F8A80 fa 5,0:1 sul fondo e 4,9:1 sull'interno del campo: è l'unico bordo del campo a riposo sopra 3:1 tra i concept scuri.
- I token sono pochi e piatti, e i pixel corrispondono.

**Deboli**
- **Toglie il badge di stato** (`display: none`) e lo sostituisce con un punto da 10 px:
  - spariscono i glifi pausa, fulmine e mano, contro la regola delle icone di stato con significato fisso;
  - `idle` non ha più nessun segno;
  - il punto è posizionato a mano (`left: 31px`).
- `--opus: var(--m-ink)` dentro la master spegne il corallo del pallino Opus nella pillola del modello e la stella «al lavoro». Toglie un significato che esiste già.
- Il rientro di 6 px più la cornice di 1,5 px tolgono circa 15 px: il titolo si tronca prima («claude-mas») e la riga mono si chiude con mezzo glifo «c».
- Su Compose servono:
  - una cornice aperta in basso disegnata con un `Path`;
  - la tacca sovrapposta al bordo del campo M3, con posizione calcolata;
  - il filo sotto le risposte, con `bottom: 44px` e `size.height − 44 dp` fissi, che si rompe quando la riga delle azioni cambia altezza;
  - lo spessore di 1,5 dp, che l'`OutlinedTextField` semplice non accetta.

## Innesti nel vincitore (carbone)

1. **Da firma, la penna del campo.** `--m-edge` da #5E5A55 a #837E76: 3,70:1 sul fondo, 4,17:1 sull'interno, 3,12:1 sulla linguetta. Il campo supera i 3:1 senza aggiungere colore.
2. **Da profondità, un'ombra verso l'alto sul blocco chiuso, a uno strato solo:** `0 -10px 20px -6px rgb(0 0 0 / .7)`, con `position: relative` e senza `z-index`. Spegne la card che passa sotto e dà alla master il «sta sopra» che oggi le manca da chiusa. Su Android è un solo `drawBehind` sul contenitore di linguetta e campo, fuori dal `clip` della `Row`.
3. **Da profondità, la condizione di stato:** la scintilla gira con `busy` e con `awaiting`, cioè `.dock:has(.breathe)`, ovvero `breathes()` di `badge.ts`. Su Android la stessa regola di `SessionBadge`.
4. **Da firma e carta, «Casa» come link sottolineato** (1 px, `--m-edge`, staccato di 4-5 px) invece della pastiglia: niente seconda forma da bottone.
5. **Da firma, la verifica.** Una resa in scala di grigi di carbone, per controllare che il blocco chiuso si stacchi dalle card anche senza tinta. Se non regge, prima si prova la variante «più caldo» già prevista da carbone (#37332B / #2A2620 / #201D18), poi l'innesto 2. Nient'altro.
6. **Da profondità, la piega opzionale** tra linguetta e campo (1 px nero .5 più 1 px chiaro al 4 %). Solo se dopo l'innesto 5 la differenza lieve risulta troppo timida.

## Da correggere prima di passarlo a Franz

1. **La scintilla con `awaiting`.** Nel CSS di carbone gira solo con `busy`, e la motivazione nel documento è sbagliata: `awaiting` lavora (contratto, README righe 22-23; `badge.ts:43`). Va corretta anche la nota Android («solo con lo stato busy, non con awaiting»).
2. **L'anello neutro.** `--b-ring` sabbia sta a 1,03:1 dall'ambra d'avviso. Il neutro va tenuto distinto dall'avviso: si lascia l'anello dell'app (#8BB4F7, come in tutte le altre sessioni) oppure si usa l'avorio pieno, non la sabbia.
3. **La riga mono troncata.** Con la scintilla, «MASTER · 12:50 · Opus 5.5 ·» finisce con un punto appeso. Si chiede a Franz se togliere «ctx» dalla linguetta (c'è già nella testata aperta). Non si decide in silenzio.
4. **Il bordo del campo sotto 3:1.** Si corregge con l'innesto 1.
5. **Il colpo d'occhio da chiusa non è dimostrato.** Linguetta e card stanno a 1,19:1. Prima di dichiararlo risolto: innesto 2, resa in grigi, prova sul Pixel al buio.
6. **Il piano Android.**
   - Non fare in un colpo il passaggio di tutti i 166 `CmColors.x` di `SessionSheet.kt` a `LocalCmPalette`.
   - Primo passo: token `master*` in `CmColors`, `MasterDock` e `Composer` direttamente, `masterChat` con `MasterChatStyle.CARBONE`.
   - Poi `LocalCmPalette` solo per i composable della chat elencati: `ClaudeBubble`, `MineBubble`, `UserBubble`, `StepsCard`, `PromptBox`, `SheetHeader`. Quelli di stato (`ElsewherePill`, `SessionBadge`) restano su `CmColors`.
   - Golden Paparazzi da registrare di nuovo.
7. **Un dato da correggere nel documento.** La pillola «ti aspetta» sul carbone è #4B3D23 (ambra #FFB020 al 16 %), non #4B402F. I contrasti restano sopra soglia: testo 9,5:1, «Rispondi» 6,7:1.
