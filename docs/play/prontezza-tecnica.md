# Supervisor su Play: prontezza tecnica (21/10/2026)

Pacchetto `com.francescosorrentino.cmaster`, telefono e orologio nella stessa scheda.

## Build

- Catena: GitHub Actions, `.github/workflows/build-android.yml`, passo «Release APK»: `bundleRelease` di `:mobile` e
  `:wear`, firmati con la chiave di upload dei secret (`RELEASE_KEYSTORE_BASE64`, `KEYSTORE_PASS`, `KEY_ALIAS`,
  `KEY_PASS`, la stessa del watchface). Gli AAB escono nell'artifact `cmwatch-apk`, tenuto un giorno.
- Il workflow parte da solo sui push di `main`, `master` e `feature/**`. I rami `notte/**` non ci sono: lì la build si
  avvia a mano con `gh workflow run build-android.yml --ref <ramo>`.
- Firma dell'app: Play App Signing, già attiva dal primo caricamento del 13/10. Con il trasferimento la chiave di firma
  di Google segue l'app; la chiave di upload resta quella dei secret.

## Versioni

| | telefono | orologio |
|---|---|---|
| Già caricate (13/10, test chiuso) | 21 · 0.2 | 22 · 0.2 |
| Ramo `notte/play-pixelfarm` | **31 · 0.3** | **32 · 0.3** |

Regola nel codice: `versionCode = release × 10 + fattore di forma` (1 telefono, 2 orologio), così i due AAB della
stessa release sono unici e l'orologio sta sempre sopra il telefono. Ogni caricamento su Play consuma il codice: la
release dopo sarà 41/42.

## SDK

- `targetSdk = 36` (Android 16) su telefono e orologio, `minSdk = 33`. Le regole di Play chiedono un target recente
  per le app nuove e gli aggiornamenti; il test chiuso con 36 è stato accettato il 13/10 e la Console non segnala
  problemi di livello API. La soglia in vigore si legge in Console, «Livello API target» nelle norme dell'app.
- `compileSdk = 36`. L'orologio dichiara `com.google.android.wearable.standalone` come nel test chiuso (canale
  «Solo Wear OS»).

## Permessi e perché servono

| Permesso | Dove | Perché |
|---|---|---|
| `INTERNET` | telefono, orologio | Parla con il Firebase dell'utente (Realtime Database, FCM). |
| `POST_NOTIFICATIONS` | telefono, orologio | Avvisa quando una sessione fa una domanda o finisce. |
| `VIBRATE` | telefono, orologio | Vibrazioni per domanda, esito, sessione chiusa (si spengono nelle impostazioni). |
| `WAKE_LOCK` | orologio | Dichiarato nel manifest, ma il nostro codice non apre wake lock: lo usano Firebase Messaging e WorkManager per consegnare i push. Non va dichiarato su Play. |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | telefono (`LiveService`), orologio (`SpeakService`) | La modalità live legge in cuffia gli aggiornamenti delle sessioni a schermo spento; l'orologio legge una risposta a voce dopo il tocco su ▶ nella notifica. È audio parlato che l'utente avvia e può fermare dalla notifica. |

Niente fotocamera (il QR lo legge lo scanner dei Google Play services), niente microfono, niente posizione, niente
contatti.

**Da fare:** la dichiarazione «Autorizzazioni per i servizi in primo piano» in Contenuti app è in ritardo (scadenza
31/01/2024 passata) e chiede un link a un video. Senza, Play blocca i prossimi aggiornamenti. Testo pronto in
`dichiarazioni-bozza.md`, video da girare: vedi `da-fare-franz.md`.

## Prove

- Test di `core` su win il 21/10: 1160, falliscono solo i 5 `ChatLogTest` che falliscono solo su Windows (in CI passano).
- Prima di questo ramo `feature/tablet` aveva la CI rossa dal 18/10 (run 39166500414): le fixture 1.73 del relay hanno
  l'op `studio` che il Kotlin non conosceva. Corretto nel commit `fix(core): read the relay's 1.73 fixtures again`,
  presente sia qui sia in `notte/voce-bilingue`.
