# «Supervisor isn't responding» nell'emulatore di win, 18/11/2026 (app-blocco-anr-18-11)

**Esito: è l'emulatore, non l'app.** Nessuna correzione nel codice.

Visto preparando le schermate per Play (app-play-aggiornamento-18-11) su un AVD Android 15 x86_64, grafica software
(swiftshader), 3 GB, su win con 16 GB: quattro ANR dell'app e due di System UI.

## Le tracce (`/data/anr`, ore dell'emulatore in UTC)

| Ora (CET) | Processo | Thread principale | APK |
|---|---|---|---|
| 12:24 | supervisor | compone la pagina (`SnapshotStateObserver.recordRead`), utm 3,3 s, stm 8,3 s | debug, schermo 2560×1600 |
| 12:24 | supervisor | fotogramma di `InfiniteTransition` | debug, 2560×1600 |
| 12:35 | supervisor | inserisce nodi (`LayoutNode.attach`), utm 3,6 s, stm 9,9 s | debug, animazioni a 0 |
| 12:48 | supervisor | **aspetta il RenderThread** (`HardwareRenderer.nSyncAndDrawFrame`) | release |
| 13:33 | **com.android.systemui** | «failed to complete startup» | — |
| 13:48 | **com.android.systemui** | «failed to complete startup» | — |

- Il tempo di sistema è il doppio o il triplo di quello utente: l'emulatore pagina, non c'è un ciclo nel codice.
- Il RenderThread fermo è la GPU software che non tiene il passo.
- A non rispondere è anche System UI, che non ha niente dell'app.
- Win alle 12:41 era in Out of memory, alle 13:48 PowerShell non partiva («file di paging troppo piccolo»): oltre
  all'emulatore da 3-4 GB c'erano WSL (1,7 GB) e due Claude.

## La prova

Emulatore riavviato con 2 GB e win con 4,7 GB liberi, APK di release della pre-release 0.2.2229
(`auto-2026-11-18-1132-14736b2d`), demo:

- home ferma 10 s: **0 fotogrammi** disegnati, quindi nessuna animazione che giri di continuo;
- 10 minuti d'uso (28 giri: apri una sessione e torna, scorri la home, menu e «Utilizzo e limiti», scorri, torna):
  **0 ANR**, nessun crash.

I fotogrammi lenti (90° percentile 101 ms) sono della grafica software dell'emulatore a 1080×2160.

## Per il futuro

Su win un emulatore alla volta con 2 GB, chiuso fra un giro e l'altro; niente build Gradle mentre gira.
