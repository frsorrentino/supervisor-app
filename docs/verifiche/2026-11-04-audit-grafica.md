# Audit di coerenza grafica, 04/11/2026

Compito `app-audit-grafica-animazioni-04-11`, parte 1. Franz, 04/11 00:22: «serve audit di coerenza grafica». Fonte dei numeri:
script di inventario su `web/src` (95 componenti Svelte e i due fogli `app.css`, `v3.css`) e su `mobile/src/main/kotlin`
(68 file UI), più la lettura degli stili calcolati nella demo web (0.2.1863, larghezza 500 px). Ogni riga «prima» è un conteggio
di dichiarazioni nel sorgente, non una stima.

## Tabella per famiglia, prima

| Famiglia | Telefono (Compose) | Web (Svelte) | Differenza | Regola |
|---|---|---|---|---|
| **Colori di superficie e stato** | Un punto solo: `V3` in `HomeV3.kt`, `Cm` punta a `V3` | Un punto solo: `v3.css` su `.v3, dialog`, i nomi vecchi di `app.css` puntano ai nuovi | 7 variabili usate e mai definite: `--accent` (3), `--onact`, `--text3`, `--brief-warn`, `--brief-warn-ink`, `--b-warn-ink`, `--b-axis`. Dove manca il ripiego il colore sparisce; dove c'è, vince il ripiego della palette vecchia: la fascia «dati vecchi» in `AppBar.svelte` è marrone e giallo `#5a4300`/`#ffd77a`, non l'ambra v3; il bollino «fermo» in `Home.svelte` è `#FFB020` su `#1d1300`. Telefono: 6 colori fuori palette (`0xFF3A4A66` in Recap, `0x42FFB23F` nella ricerca, che di giorno resta l'ambra della notte; `0xFFFFC867` e gli aloni delle luci ripetuti a mano) | Ogni colore viene da un token di stato o di superficie; nessun ripiego verso la palette vecchia; gli effetti delle luci hanno il loro token, giorno e notte |
| **Testi** | 6 stili `V3` + 12 ruoli Material (mappati in `V3Theme`, tranne `labelSmall`, `headlineMedium`, `displaySmall` che restano Roboto) + 19 misure scritte a mano (12, 15, 11, 13, 10, 13,5, 14, 22, 16, 17, 20, 11,5, 28, 18, 15,5, 12,5, 38, 42, 19 sp) | 27 misure diverse: 14 (157), 16 (52), 13 (63), 12,5 (50), 15 (44), 14,5 (25), 13,5 (18), 12 (57), 11 (24), 10 (9), 10,5, 11,5, 15,5, 16,5, 17, 18, 19, 20, 22, 24, 28, 34, 42, 44 px | Il corpo è 15,5 sul telefono e 15 sul web; i tasti 14 (`labelLarge`) sul telefono e 15 sul web; i riassunti oscillano fra 13 e 14,5 nella stessa pagina. Nella demo, su una sola schermata, 6 misure di corpo in Barlow (13, 13,5, 14, 14,5, 15, 16) | Una scala sola, uguale sui due lati: Display 42, Titolo 22, Sezione 18, Nome 17, Tasto 15 (Barlow Semi Condensed); Corpo 15,5, Piccolo 14 (Barlow); Misura 12,5 e Minimo 11 (Geist Mono). Le misure fuori scala vanno al gradino più vicino; i campi di testo del web restano a 16 (sotto, Safari ingrandisce la pagina) |
| **Forme: card e liste** | `V3.Shape` 14 (54) e 20 dp (22: domanda MCP, Plugin, Consiglio del Sistema, card di SummaryList, Lavori, Decidi, HomeBox, Seguiti) | 14 px (42) e 16 (Kpi, Utilizzo di oggi, quota, ricerca) e 20 (domanda MCP, Lavori, HomeBox, Seguiti, Prima raccolta) | Tre raggi per la stessa cosa: una card a 20 accanto a una lista a 14 | Card, liste e pannelli: 14 |
| **Forme: fogli** | Foglio modale 14 in alto (`Buttons.kt`), conversazione 24, master 26, categorie 18 | 28 (6), 26 (4), 18 (4) | Quattro raggi per i fogli dal basso | Fogli dal basso: 28 in alto, come Material 3 |
| **Forme: elementi interni** | 12 (immagini, note tinte, chip di stato), 10, 8, 6 | 12 (30), 8, 10, 6 | Abbastanza coerente | Riquadri dentro una card: 12; barre e tacche di grafico restano 2-3 |
| **Tasti** | 7 componenti per lo stesso tasto: `V3Pill`, `MiniButton`, `ElicitButton`, `NeedButton`, `ConfirmButton`, `OptionButton`, e i tasti Material | 9 varianti calcolate nella demo: altezze 33, 34, 36, 40, 44 px; peso 500 Barlow o 600 Semi Condensed; 13, 14, 15 px | La forma è la stessa (pillola); cambiano carattere, peso e altezza | Tasto: pillola alta 40, Tasto 15/600 Semi Condensed, 16 di lato; tonale su `s3`, pieno su `act` |
| **Icone** | 18 dp (38), 22 (27), 20 (24), 16 (17), 24 (17) | Material Symbols 20 (30), 24 (4), 18 (3), 16 | Il telefono usa 18 dove il web usa 20 | 24 barra in alto, 22 binario delle sezioni, 20 righe e tasti, 16 dentro il testo |
| **Spazi** | Margine di lista 12, interno card 14 o 16 | Margine di lista 12, interno card 12-14 o 14 | Interno card 14 contro 16 | Margine 12, interno card 14 |

## Cosa va bene e resta

- Una palette sola per notte e giorno, con i nomi vecchi che puntano ai nuovi su tutti e due i lati.
- Il colore appartiene allo stato: nessuna decorazione colorata trovata fuori da ambra, blu, verde, rosso.
- Cifre a larghezza fissa ovunque (`tabular-nums` sul web, `tnum` sul telefono).
- La riduzione delle animazioni è rispettata da un punto solo (`animationsOff()`, `prefers-reduced-motion`).

## Correzioni

Una regola per famiglia, applicata in un passo su tutti e due i lati, in un commit con l'id del compito. Lo stato «dopo» è nella sezione sotto.

## Dopo

Commit 6cb47311, web 0.2.1864. Conteggi rifatti con lo stesso metodo.

| Famiglia | Telefono | Web | Cosa resta |
|---|---|---|---|
| Colori | `RecapSection` e `SearchScreen` sui token (`V3.Tx` su `V3.S3`, `V3.Wait` al 26%): di giorno la ricerca usa l'ambra del giorno | Le 7 variabili inesistenti puntano a token v3; nuovi `--accent` (= `--busy`, come `Cm.accent`), `--banner` e `--banner-ink` (= `V3.Banner`/`BannerInk`); `ReadingPill` su `--s3` | Gli aloni delle luci (`#FFC867`, ambra al 30% e al 10%) restano scritti a mano sui due lati, come approvati nel mockup luci2: sono solo della notte |
| Testi | `cmType` in `PhoneTheme.kt` porta i ruoli Material sulla scala (bodyLarge 15,5, bodySmall 12,5, labelLarge 15, labelMedium 12,5, titleMedium 17, titleSmall 15); `labelSmall`, `headlineMedium` e `displaySmall` passano in Barlow; 61 misure scritte a mano portate al gradino (10 e 11,5 → 11; 12 e 13 → 12,5; 13,5 → 14; 16 → 15,5 o 15) | 240 dichiarazioni portate al gradino; corpo della pagina 15,5. Nella demo il testo in Barlow passa da 6 misure (13, 13,5, 14, 14,5, 15, 16) a 3 (12,5, 14, 15,5) | Due misure relative in `em` (10,4 e 12,9 px calcolati) e i campi di testo a 16 di proposito |
| Card | Tutte a 14 (22 da 20) | 26 regole da 16 o 20 a 14; immagini e QR a 12 | — |
| Fogli | 28 in alto (foglio modale da 14, conversazione da 24, master da 26); dialoghi 14 come sul web | 28 (da 26 in master, conversazione incorporata, chat) | Le testate delle colonne della plancia a 18 non sono fogli e restano |
| Tasti | Tutti su `labelLarge`, ora Tasto 15 Semi Condensed 600 come `V3Pill` | Regola unica in `v3.css` per `.tonal`, `.filled`, `.approve`, `.vbtn` | Le opzioni della domanda MCP e alcune pillole piccole sono `button` senza classe e restano 500 Barlow 15,5 e 12,5 |
| Icone | Non cambiate | Non cambiate | Il telefono usa 18 dp dove il web usa 20 px nelle righe: da vedere schermata per schermata, non con uno script |
| Spazi | Non cambiati | Non cambiati | Interno card 14 contro 16 in Lavori e HomeBox |

Golden: le misure cambiano quasi ogni schermata del telefono; i nuovi golden vengono dal run CI con la registrazione, non da win.

## Morphing: dove una forma diventa un'altra

| Punto | Telefono | Web | Stato |
|---|---|---|---|
| Card della sessione → pagina della sessione | `fly` con `sharedBounds` (`Fly.kt`) | Plancia: `page-fly`; telefono stretto: fila che scorre | C'era |
| Barra della master → pannello | `MasterDock`, molla `CmMotion.panel` | `master-morph` sulla plancia | C'era |
| Tasto → menu, foglio, dialogo | `growFromPress` | `growFromPress`, `installDialogGrow` | C'era (voce 16) |
| Trascinamento della master → posto nuovo | — | `landingMorph.ts` | C'era (03/11) |
| Tasto scelto → conferma nella card | `ActionPass`, pillola con la spunta | `pass` | C'era (voce 11) |
| **Voce premuta → pagina, e ritorno** | **Nuovo**, `PageIn` in `PageMotion.kt`: la pagina si scopre da un riquadro attorno al punto premuto, 380 ms. Solo l'andata: tornando la home rientra da sinistra come prima | **Nuovo**, `morphStart` in `motion.ts` con le View Transitions: la voce (casella, riga, voce di menu, Cerca) diventa la pagina e la pagina torna nella voce, anche se la home è stata ridisegnata | Fatto: web 0.2.1866, telefono nella prossima pre-release |

Prova sul web (0.2.1866, demo, Chrome): aprendo Cerca e tornando, il gruppo `morph` ha vecchia e nuova immagine nei due versi e nessun nome resta appeso dopo. Fotogramma a 150 ms: `docs/mockup/2026-11-04-effetti/morph-cerca-150ms.png`. Sul telefono non c'è un emulatore Wear né un telefono collegato: il movimento lo giudica Franz sulla pre-release.
