# Tablet come la PWA larga: differenze (app-tablet-come-pwa-03-11)

Riferimento: la PWA da 840 px in su (`web/src/App.svelte`, `Plancia.svelte`, `Desk.svelte`, `MasterDock.svelte`,
`masterLanding.ts`) com'è dopo app-pwa-coerenza-03-11 e app-master-trascina-03-11. Tablet: l'APK `mobile` sopra
840 dp (`rules/Tablet.kt`, `MainActivity.kt`, `ui/TabletShell.kt`).

Pacchetto sul tablet: lo stesso APK del telefono (stesso applicationId). Al 28/10 il tablet aveva ancora la vecchia app;
non è collegato ad adb ora, la versione installata la vede Franz in Impostazioni.

## Prima (04/11 01:50, sha 168d6a16)

| # | Parte | PWA larga | Tablet |
|---|-------|-----------|--------|
| 1 | Senza sessioni aperte | Plancia: testata fuori dalla griglia, fino a 4 colonne per categoria nell'ordine della home (Richiede te, Attività e code, Esiti, Numeri e strumenti), 320 px l'una, quelle in più in fondo all'ultima; ogni colonna con la sua testata e la cifra | Variante B: la home in una colonna e a destra Recap e Utilizzo (`TabletDashboard`) |
| 2 | In cima | Fascia a tutta larghezza sopra le colonne, alta al massimo 40 % | Dentro la lista della home, come sul telefono |
| 3 | Superfici delle colonne | Pannelli staccati sullo sfondo illuminato; dentro niente riquadri, schede piene solo per azioni | Lista del telefono a tutta colonna |
| 4 | Master chiusa | Barra in fondo alla plancia con la maniglia, il campo e ▲ | Barra in fondo alla home (`MasterDock`) |
| 5 | Master aperta | Pannello di lato alto quanto lo schermo, 420 px o 720 px, oppure larghezza libera dal bordo (scatti a colonna e a metà schermo, 360 px - 60 %) | Foglio dal basso: metà, poi tutto schermo |
| 6 | Lato della master | Destra o sinistra, ricordato (`cm.master_left`); col pannello a sinistra le colonne si stringono dall'altra parte | Nessuno |
| 7 | Master sotto la home | Con sessioni aperte: home sopra e master sotto nella stessa colonna, altezze colonna, metà, compatta, riga (`cm.master_stack_h`) | Nessuno |
| 8 | Trascinamento della master | Testata: cambia lato; bordo verso le sessioni: larghezza; bordo superiore: altezza a scatti; durante il gesto i punti di atterraggio, quello di arrivo grande e con l'etichetta | Solo il trascinamento verticale della barra, che apre e chiude |
| 9 | Menu della master | Tasti compatti nel menu ⋮ e il tasto Posizione (lato, metà schermo, misura, Chiudi) | Tasti ▲ ▼ della barra |
| 10 | Con sessioni aperte | Home accanto alla master, sul lato della master; colonne con testata, trascinabili | Home a sinistra o a destra (`tablet_home_right`), colonne con testata trascinabili |
| 11 | Pillole + e › | Accanto a ogni sessione della home, verso le colonne: + la apre accanto, › al posto di quella in vista; linea bianca su quella già in colonna (app-sessione-sostituisci-03-11) | Nessuna: il tocco apre o chiude la colonna |

Uguali già oggi: soglia 840, massimo 4 colonne, larghezze in dodicesimi, scambio delle colonne, chiusura con lo splash, ispettore da 1200.

## Ordine del porting

1. Regole in `core`: `MasterLanding` (zone, scatti, altezze) da `masterLanding.ts`, `Plancia.columns` (quante e dove scendono). Test JVM sugli stessi casi dei test web.
2. Plancia senza sessioni (righe 1-3), al posto della variante B.
3. Master: barra in fondo, pannello di lato con lato e misura, sotto la home (righe 4-7, 9).
4. Gesti con i punti di atterraggio (riga 8).
5. Home sul lato della master e pillole + e › (righe 10-11).

Ogni passo: golden Paparazzi a 1280×800 dp accanto allo screenshot della PWA alla stessa larghezza.

## Dopo (04/11 03:20, sha 59238b76)

| # | Tablet adesso | Resta diverso |
|---|---------------|---------------|
| 1 | Plancia per categorie (`Plancia`, `DeskHead`), ordine della home, colonne da 320 dp, quelle in più in fondo all'ultima nello stesso scorrimento; testata ferma in alto | - |
| 2 | Come la PWA, che oggi non passa la fascia alla plancia (`top={null}`): le voci in cima stanno in testa alla loro colonna | - |
| 3 | Pannelli staccati col bordo illuminato; sezioni sempre aperte | Le testate di sezione nelle colonne hanno ancora la freccia (nella PWA sono ferme, senza) |
| 4 | Barra della master in fondo, con la maniglia | Nella barra manca il campo per scrivere alla master |
| 5 | Pannello di lato 420 / 720 o misura libera col bordo (scatti a colonna e metà schermo) | - |
| 6 | Destra o sinistra, ricordato (`ui/master_place`) | - |
| 7 | Sotto la home, compatta o a metà, con la maniglia che alterna | Compatta mostra solo la barra della master; nella PWA anche l'ultimo messaggio e il campo |
| 8 | Maniglia del lato, bordo della larghezza, bordo superiore e barra tirata su; segni con calamita ed etichetta; annullato torna com'era | Regole provate con test JVM; il gesto sullo schermo non ha un test automatico: va provato sul tablet |
| 9 | Tasto Posizione con lati, sotto la home (solo con sessioni aperte), Allarga/Stringi, Chiudi | - |
| 10 | Home dal lato della master | - |
| 11 | Pillole + e › nel filo, filo bianco su quella già in colonna | Sul tablet niente passaggio del mouse: le due scritte della PWA non compaiono |

In più resta diversa la colonna Risorse: sul tablet PC Windows (stato e RAM) e Ricorrenti; la PWA mostra anche le sessioni di
ogni host, le pubblicazioni di oggi (`published_today`, che il modello Kotlin non legge ancora) e il lancio rapido.

Golden: `TabletShellTest` `planciaEmpty`, `planciaMasterOpen`, `deskMasterLeft`, `deskMasterUnder`, `deskSwapPills` (dalla CI).
