# Brief del film: la master e le funzioni dell'app

Aggiunta del 10/10/2026 a `docs/video-brief.md`, chiesta dalla master dopo il giudizio di Franz sul primo montaggio (06:32:
«vorrei si mostrasse l'interfaccia, che è più colorata; riproduzioni di interfaccia fedeli e più dinamiche e colorate;
dobbiamo far capire l'uso della master e le varie funzionalità nel menu o da home»). Tutto quello che segue c'è nell'app di
oggi. I golden sono in `mobile/src/test/snapshots/images/`, col prefisso `it.pixelbox.cmwatch.mobile_`.

## 1. La master dall'app, passo per passo

1. **La barra in fondo alla home.** Mostra «MASTER · ora · ctx», il titolo dell'ultimo esito, ▶ per ascoltarlo e ▲ per aprire.
   Sotto c'è sempre il campo «Scrivi alla master».
   Golden: `SummaryListTest_dockMaster` (la barra), `SessionSheetTest_masterPlusMenu` (il campo, in basso).
2. **Ascoltare senza aprire.** ▶ sulla barra legge l'ultimo esito ad alta voce; mentre legge, diventa ■ e compare il
   controller della lettura, con velocità e voce.
   Golden: `ReadingPillTest_rateAndVoice` (il controller), `SummaryListTest_dockMaster`.
3. **Aprirla.** Un tocco su ▲, o la barra trascinata in su: la conversazione sale dal basso a tutto schermo, con una
   testata sola (⌄ per richiuderla a sinistra, menu ≡ a destra). Sul tablet si ferma a metà della colonna o arriva a tutta
   altezza.
   Golden: `SessionSheetTest_masterScreenPhone`, `SessionSheetTest_masterHalfTablet`, `SessionSheetTest_masterFullTablet`.
4. **Le Azioni dopo ogni risposta.** Sotto l'ultima risposta il box «Azioni» raccoglie i Prossimi. Il tocco su una riga la
   porta nel campo; ↗ la manda subito; tenendola premuta la togli.
   Golden: `SessionSheetTest_sheetNextSteps`, `SessionSheetTest_sheetQuickPhrases`.
5. **Il + del campo.** Foto dalla galleria, Scatta una foto, File e Azioni ricorrenti: le richieste che si fanno spesso,
   pronte da mandare con ↗ o da completare con +.
   Golden: `SessionSheetTest_masterPlusMenu`, `SessionSheetTest_masterRecurring`.
6. **Per te.** Quello che aspetta proprio te: le domande delle sessioni con i tasti di risposta, gli esiti con i Prossimi
   come pillole.
   Golden: `SessionSheetTest_forYouOpenRows`.
7. **Il Recap passa dalla master.** Nel Recap, «Fallo» manda una scheda alla master, e «Parlane con la master» apre il suo
   campo con la scheda citata.
   Golden: `RecapScreenTest_recapHome`, `RecapScreenTest_recapPage`.

## 2. Menu ≡ e home

**Menu ≡**, nell'ordine dell'app. In cima il pallino verde e «Collegato a <PC> · aggiornato ora».

| Voce | Cosa fa | Golden |
|---|---|---|
| Lancia una sessione | un progetto e il primo messaggio | `LaunchSheetTest_launchSheet` |
| Registro | stanotte, i giorni, la coda della notte | `DiaryScreenTest_diaryFull` |
| Notte | i lavori e le sessioni della notte, il resoconto | `NightScreenTest_closedCards`, `NightScreenTest_anOpenCard` |
| Recap | le azioni del giorno, cosa aspetta te, cosa può fare Claude | `RecapScreenTest_recapPage` |
| Utilizzo e limiti | finestre di 5 ore e settimana, il ritmo | `TabletShellTest_tabletDeskEmpty` (colonna Utilizzo) |
| Cerca nelle conversazioni | messaggi, esiti e registro | `SearchScreenTest_searchResults` |
| Modalità live | notiziario in cuffia, comandi dal watch | `ReadingPillTest_liveOpen`, `ReadingPillTest_liveSpeaking` |
| Impostazioni | dispositivi collegati, voce e velocità di lettura | `SettingsScreenTest_settingsDemo` |

Il golden del menu (`ShellScreensTest_menuOpen`) non ha le voci Notte e Recap: il test non le accende.

**Home**, dall'alto in basso:

| Parte | Cosa fa | Golden |
|---|---|---|
| Testata | «Master N aperte», 🔍 e ≡ | `ShellScreensTest_menuOpen` (dietro il menu) |
| Riga della quota | 5 ore in uso e quando si azzera; il tocco porta a Utilizzo | `SessionSheetTest_sheetHeaderQuota` (stessa quota nella testata di una sessione) |
| Sessioni | Ti aspetta, Al lavoro, Ferma, Altro; si risponde dalla card | `SummaryListTest_summaryOpen` |
| Recap | azioni del giorno e schede dell'agenda con «Azioni» | `RecapScreenTest_recapHome` |
| Utilizzo | anello della quota, previsione al ritmo di adesso, settimana, eventi di oggi | `TabletShellTest_tabletDeskEmpty` |
| Stanotte | la coda della notte e il resoconto del mattino | `SummaryListTest_summaryWithNight` |
| Barra e campo della master | vedi la parte 1 | `SummaryListTest_dockMaster` |
| Dal tocco sulla card | la sessione si apre «volando» dalla card | `SessionSheetTest_sheetQuestion` (la pagina d'arrivo) |

## 3. Golden da aggiungere ai 52 già copiati

`SummaryListTest_dockMaster`, `SummaryListTest_summaryOpen`, `SummaryListTest_summaryWithNight`, `ShellScreensTest_menuOpen`,
`SearchScreenTest_searchResults`, `TabletShellTest_tabletDeskEmpty`, `NightScreenTest_anOpenCard`,
`RecapScreenTest_recapPage`, `SessionSheetTest_sheetQuestion`, `ReadingPillTest_rateAndVoice`. Per l'orologio, in `wear/src/test/snapshots/images/` col
prefisso `it.pixelbox.cmwatch.wear_`: `ScreensSnapshotTest_question`, `LiveScreenSnapshotTest_liveOutcome`.

`SessionSheetTest_masterHomeA` è una casa della master di una versione precedente (con «Ultimo esito» in cima): non usarla.
Lo stesso vale per i `masterChat*` e i `masterLook*`, che sono le varianti di stile proposte il 04-05/10, non tutte adottate.

## 4. Colori veri

Valori dai token dell'app (`ui-tokens/.../CmColors.kt` e `MasterDock.kt`). Tema scuro, fondo nero.

| Uso | Colore |
|---|---|
| Fondo | `#000000` |
| Superfici (dal basso all'alto) | `#1B1F26`, `#23272E`, `#292F3A`; linee `#2A2E35` |
| Testo; testo secondario | `#F2F4F7`; `#98A2B3` |
| Tasto pieno (uno per schermata) e il suo testo | `#D3E3FD` su `#0A2050` |
| Icone delle azioni, link | `#A8C7FA` |
| Etichetta «Ti aspetta» (ambra) | `#FFB020` |
| Etichetta «Al lavoro» (blu) | `#7FA1FF` |
| «aperta» e il pallino del PC collegato (verde) | `#34C759` |
| Chiusa (rosso) | `#FF453A` |
| Consiglio (viola); seguita (giallo) | `#C58AF9`; `#FFE08A` |
| La master: barra, chat, contenitore, fondo | `#283549`, `#1D2B3E`, `#122033`, `#0E1C2E` |
| La master: tasti tondi | `#3B475C` |
| La firma della master: dal corallo di Claude al lilla | `#D97757` → `#CDB8FF` |
| Modelli: Opus, Sonnet, Haiku | `#D97757`, `#8BB4F7`, `#65C581` |
| Utilizzo: anello e binario | `#8BB4F7` su `#455165` |
| Utilizzo: bene, attenzione, allarme, settimana | `#65C581`, `#FFC46B`, `#E5736B`, `#B9A6F5` |

Il tondo di ogni sessione ha il colore scelto per quella sessione, non quello del suo stato: lo stato lo dice il simbolo
bianco o nero dentro (✋ ti aspetta, ⚡ al lavoro, ‖ ferma, ✕ chiusa). Il colore di un elemento preciso si prende dal suo
golden; i valori servono per rifarlo nitido e a grandezza piena.

## 5. Movimenti tipici

- **Volo dalla card.** Il tocco su una card della home la allarga fino a diventare la pagina della sessione, in 320 ms. Il
  ritorno (Indietro, o il dito verso destra sulla prima sessione) la richiude nella card, e segue il dito mentre lo trascini.
  Dalla home, il dito verso sinistra apre allo stesso modo la prima sessione.
- **Fra sessioni.** Scorrimento laterale, una pagina accanto all'altra.
- **Senza card in vista** (menu, notifica, ricerca). La pagina entra da destra, partendo da un terzo della larghezza, con
  una dissolvenza, ed esce dallo stesso lato.
- **La master che sale.** Il pannello sale dal basso, dalla sua barra, con una molla morbida e senza rimbalzo; la lista
  dietro arretra in 250 ms (dissolvenza e scala a 0,96). Chiudendo, scende per la stessa strada. Sul tablet il foglio cresce
  dalla barra fino al 55 % della colonna o a tutta altezza, con la stessa molla.
- **Tasti tondi.** Premuti passano da cerchio a quadrato smussato (raggio da 22 a 12 dp) con una molla veloce, come il
  Material 3 Expressive.
- **La scintilla della master.** Accanto al nome nella barra gira, un giro ogni 2,4 s, mentre la master lavora; quando
  ti aspetta o è chiusa, al suo posto compare il tondo con lo stato.
- **Testata della chat della master.** Rientra mentre rileggi più su e torna quando sei in fondo.
- **La conversazione.** Entra in dissolvenza in 150 ms invece di comparire a pezzi.
- **Curva di base.** Le transizioni di 250 ms usano `cubic-bezier(0.3, 0, 0.2, 1)`.
