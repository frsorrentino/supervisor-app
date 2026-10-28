# Movimenti dell'app per il film

Chiesto dalla master il 10/10/2026 (Franz, 08:54: «anche le animazioni devono essere corrette»). I valori sono presi dal
codice del ramo `feature/tablet` al commit `5367c196`, con file e riga. Prima del file il prefisso è
`mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/`, salvo dove è scritto un altro percorso.

Le molle di Material 3 vengono da `MotionScheme.expressive()` (`ui/PhoneTheme.kt:63`), con i valori della libreria in uso
(material3 1.5.0-alpha18, letti dalla classe `ExpressiveMotionTokens`):

| Nome | Smorzamento | Rigidità |
|---|---|---|
| spaziale di base (`defaultSpatialSpec`) | 0,8 | 380 |
| spaziale veloce (`fastSpatialSpec`) | 0,6 | 800 |
| spaziale lenta | 0,8 | 200 |
| effetti di base (`defaultEffectsSpec`, dissolvenze) | 1 | 1600 |
| effetti veloci | 1 | 3800 |
| effetti lenti | 1 | 800 |

Altre costanti usate sotto:
- `CmMotion.easing` = `cubic-bezier(0.3, 0, 0.2, 1)` (`ui/PhoneTheme.kt:79`).
- `CmMotion.panel` = molla senza rimbalzo (smorzamento 1), rigidità 400 (`StiffnessMediumLow`) (`ui/PhoneTheme.kt:86-89`).
- `FLY_MS` = 320 ms (`ui/Fly.kt:27`).
- Le animazioni senza parametri di Compose (`fadeIn()`, `slideInVertically {}`, `animateContentSize()`) usano una molla
  con smorzamento 1 e rigidità 400.

Con le animazioni di sistema spente, tutto quello che segue avviene di colpo.

## 1. Volo dalla card alla sessione, e ritorno

- **Cosa si muove.** I bordi della card della home diventano i bordi della pagina della sessione: posizione e misura,
  con il contenuto rimisurato a ogni passo. Nessuna dissolvenza: il resto della pagina non si muove. (`ui/Fly.kt:32-42`,
  `sharedBounds` con `ResizeMode.RemeasureToBounds`.)
- **Durata e curva.** 320 ms con `CmMotion.easing` per i bordi (`ui/Fly.kt:39`). L'avanzamento della transizione è
  lineare in 320 ms quando parte da un tocco (`MainActivity.kt:320`).
- **Ritorno con Indietro.** Il gesto Indietro di Android guida l'avanzamento all'indietro passo per passo
  (`MainActivity.kt:324`); lasciato a metà, la pagina torna aperta (`MainActivity.kt:327`).
- **Ritorno col dito verso destra sulla prima sessione.** Lo spostamento del dito diviso la larghezza dello schermo
  guida il volo all'indietro (`MainActivity.kt:1543`). La pagina torna alla home se il dito supera un terzo della larghezza
  o se lo slancio supera 1200 px/s (`MainActivity.kt:1561`); altrimenti torna aperta.
- **Andata col dito verso sinistra sulla home.** Lo stesso al contrario, verso la prima sessione della fila, con le
  stesse soglie (`MainActivity.kt:1464`, `MainActivity.kt:1487`).
- **Senza card sullo schermo** (sessione aperta dal menu, da una notifica o dalla ricerca): la pagina entra da destra
  partendo da un terzo della larghezza, in 320 ms con `CmMotion.easing`, e appare con una dissolvenza di 320 ms. La home
  dietro si sposta a sinistra di un decimo e svanisce in 160 ms. All'uscita avviene il contrario, con la pagina che resta
  sopra (`MainActivity.kt:1430-1438`).
- **Fra una sessione e l'altra.** Scorrimento laterale del pager, una pagina accanto all'altra (`HorizontalPager` di
  Compose, movimento standard).

## 2. La master che sale dalla barra

- **Telefono (barra → tutto schermo).** Il pannello della conversazione sale dal basso, dalla quota della barra, con
  `CmMotion.panel` (molla senza rimbalzo, rigidità 400). Intanto la lista dietro arretra: dissolvenza e scala da 1 a 0,96
  in 250 ms con `CmMotion.easing`. Alla chiusura il pannello scende per la stessa strada e la lista torna avanti
  (`ui/SessionSheet.kt:536-541`).
  - La barra della home ricompare solo a pannello arrivato: dissolvenza di 150 ms con 200 ms di ritardo
    (`ui/SessionSheet.kt:564`).
  - La testata della home («Master N aperte», 🔍, ≡) rientra mentre il pannello sale: contrazione verticale sulla molla
    spaziale di base (0,8 e 380) più una dissolvenza sulla molla degli effetti (1 e 1600); torna allo stesso modo quando la
    master si richiude (`ui/SessionSheet.kt:320-324`).
  - La testata della chat della master (modello, quota, contesto) usa le stesse due molle: rientra mentre si rilegge più su
    e torna quando si è in fondo (`ui/SessionSheet.kt:334-338`).
- **Tablet (tre misure: barra, metà, tutta).** Il foglio cresce in altezza dalla barra fino al 55 % della colonna
  (`ui/SessionSheet.kt:1598`) o a tutta altezza, con una molla senza rimbalzo di rigidità 400 (`ui/SessionSheet.kt:493-499`).
  A metà, la home sopra resta ferma e si tocca.

## 3. Menu ≡ e pagine

- **Menu ≡.** Il fondo si scurisce al 55 % di nero con una dissolvenza di 150 ms. Il pannello del menu entra con una
  dissolvenza e una scala da 0,96 a 1 in 200 ms con `CmMotion.easing`, a partire dal tasto ≡; esce in 150 ms all'inverso
  (`ui/AppMenuPanel.kt:108-124`). Sul tablet è largo 380 dp, dalla parte del tasto.
- **Pagine opache** (Impostazioni, Terminale, Recap, Notte, Cerca, Coda, Registro): dal 10/10 (commit `4becb368`) entrano da
  destra partendo da un terzo della larghezza, in 320 ms con `CmMotion.easing`, insieme a una dissolvenza di 320 ms
  (`ui/PageMotion.kt`). Chiudendole, la home rientra da sinistra di un decimo della larghezza, con la stessa curva.

## 4. Card che cambia stato o sezione, lista che arretra

- **Card che cambia gruppo** (per esempio da «Al lavoro» a «Ferma»): scivola al posto nuovo con una molla senza rimbalzo
  di rigidità 400, e le altre si spostano allo stesso modo. Le card che entrano o escono hanno una dissolvenza di 150 ms
  (`ui/SummaryList.kt:162-168`).
- **Lista che arretra** quando sale la master: vedi la parte 2 (scala a 0,96 e dissolvenza in 250 ms).

## 5. Tasti che cambiano forma

- I tasti tondi della barra della master (▲, ▼, ▶): premuti, il raggio degli angoli passa da 22 a 12 dp, cioè da cerchio a
  quadrato smussato, sulla molla spaziale veloce (0,6 e 800). Lasciati, tornano cerchio (`ui/MasterDock.kt:69`).

## 6. La scintilla

- Accanto a «MASTER» nella barra: otto raggi corallo `#D97757` che ruotano da 0 a 360 gradi in 2,4 s, lineari, all'infinito,
  mentre la master lavora (`ui/MasterDock.kt:79-81`). Quando la master ti aspetta o è chiusa, al suo posto compare il tondo
  della sessione.

## 7. L'entrata della conversazione

- Mentre si carica c'è la rotella; la conversazione entra con una dissolvenza di 150 ms e la rotella esce in 100 ms
  (`ui/SessionSheet.kt:363`).

- **All'apertura di una sessione** (dal 10/10, commit `c7351654`): le ultime sei voci salgono di 12 dp con una dissolvenza in
  200 ms con `CmMotion.easing`, una dopo l'altra dall'alto, 30 ms di scarto (`ui/SessionSheet.kt`, `cascadeIn`). Le voci
  arrivate dall'ultima visita hanno sopra la riga «Nuovi» e un fondo celeste al 12 % che sfuma in 2 s dopo 600 ms (`newsTint`).
- **Sessione chiusa → home**: la pagina vola indietro fino alla card della sessione appena chiusa, in cima alle sessioni
  (corretto il 10/10, commit `4becb368`: prima la pagina spariva di colpo).

## 8. Il foglio di lancio e quello della notte

- È il foglio modale di Material 3: sale dal basso con l'animazione della libreria, che l'app non modifica, e si chiude
  trascinandolo giù o toccando fuori. Il contenuto non ha movimenti propri.

## 9. La pillola live e il controller della lettura

- **Comparsa.** Dissolvenza più salita da metà della sua altezza, con le molle predefinite di Compose (smorzamento 1,
  rigidità 400); l'uscita fa il contrario (`ui/ReadingPill.kt:165`).
- **Spostamento fra i posti** (da una pagina all'altra, sopra il campo): scivola in 220 ms con `FastOutSlowInEasing`
  (`ui/ReadingPill.kt:162`).
- **Apertura** (con la domanda e i tasti, o con le righe delle sessioni): la misura cambia con `animateContentSize`, molla
  predefinita (`ui/ReadingPill.kt:485`).
- **Pallino rosso di «LIVE».** Opacità da 1 a 0,25 e ritorno, 900 ms per verso, all'infinito; fermo in pausa
  (`ui/ReadingPill.kt:508`).
- **Le tre barrette della voce.** Ognuna sale e scende fra il 35 % e il 100 % in 520 ms con `FastOutSlowInEasing`, con
  ritardi diversi, all'infinito; ferme quando la voce tace (`ui/ReadingPill.kt:454`).

## 10. L'orologio

- **Liste.** Sono `TransformingLazyColumn` di Wear Compose Material 3 (per esempio
  `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/ui/screens/LaunchScreen.kt:40`). Scorrendo, le card in alto e in basso
  si restringono e si deformano verso il bordo tondo (`SurfaceTransformation`), con il movimento standard della libreria.
- **Tondo della sessione che lavora.** «Respira»: opacità da 1 a 0,55 e ritorno, 1500 ms per verso, all'infinito
  (`wear/src/main/kotlin/it/pixelbox/cmwatch/wear/ui/components/SessionBadge.kt:47-51`).

## Registrazioni di riferimento

Non le ho fatte. Il telefono di Franz è in uso; il tablet e l'Android del Chromebook hanno l'app collegata ai dati veri,
non in modalità demo; e l'orologio non è in rete. Per farle senza dati personali serve un dispositivo con l'app in demo.
