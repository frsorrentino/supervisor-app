# Impegni nell'app: aggiornamento dentro l'app (P6), script ADB, Impegni e Approva (P2, P4) (22/10/2026)

Esito: l'app si aggiorna da sola dalle release di GitHub (P6: card «Aggiornamento disponibile · Installa» in cima alla home, download verificato col digest, installer di Android); lo script ADB usa l'adb giusto e installa anche sul telefono; con il contratto 1.76 la home ha la sezione fissa «Impegni» sopra le sessioni (P2) e «Approva» sulla riga che aspetta Franz (P4), Android e web. Due pre-release: `impegni-2026-10-22` (P6, installata via ADB alle 00:48) e `impegni-2026-10-22-b` (Impegni e Approva), che il telefono deve proporre da solo. `feature/tablet` in fast-forward. Niente deploy web. In parallelo il piano SDK per Franz in `docs/proposte/2026-10-22-sdk-piano.md`, non committato finché non lo approva.

Ordine: la master, 22/10 alle 00:01, sulle proposte di `2026-10-21-compiti-sempre-visibili.md` approvate da Franz il 21/10 alle 23:58 («ok per le proposte, prosegui con implementazione», registro `impegni-app-22-10`, approvazione 393). Per questa sessione: P6 subito, lo script ADB corretto, P2 e P4 lato app quando il contratto 1.76 li definisce; alla fine la pre-release `impegni-2026-10-22` e l'APK installato sul telefono.

## P6: l'aggiornamento dentro l'app

Com'è fatto, le scelte lasciate a me:

- **Dove guarda.** `GET https://api.github.com/repos/frsorrentino/supervisor-app/releases?per_page=10`, anonimo (il repo è pubblico; 60 richieste l'ora per IP, l'app ne fa al più due l'ora). Contano le release pubblicate (non bozze), pre-release comprese, che hanno l'asset `mobile-release.apk`: la più recente per `published_at`. La CI mette lo stesso nome all'APK del telefono, quindi la pre-release della notte basta com'è.
- **Quando lo propone.** La release è un aggiornamento se è uscita dopo l'installazione di adesso (`lastUpdateTime` del pacchetto) e il suo APK non è quello installato: il confronto è sul digest sha256 che GitHub pubblica per ogni asset (`assets[].digest`) contro il digest dell'APK installato (`applicationInfo.sourceDir`, calcolato una volta per installazione e ricordato). Così la release messa sul telefono via ADB non si ripropone, e una build di sviluppo più nuova di ogni release non torna indietro. «Non ora» scarta quel tag finché non ne esce uno nuovo o non si controlla a mano. Regole pure in `core/rules/AppUpdate.kt`, con `AppUpdateTest` (9 test).
- **Quando controlla.** All'apertura dell'app (`onStart`), non più di una volta ogni mezz'ora; in Impostazioni la riga «Aggiornamenti» dice l'esito dell'ultimo controllo («Nessuna novità · controllato alle 00:40», «Disponibile: …», «GitHub non raggiungibile: …») e il tocco controlla subito. Niente controllo in sottofondo: l'APK arriva quando Franz apre l'app, che è il momento in cui lo installa.
- **La card.** In cima alla home, sopra il banner «Ti aspettano»: «Aggiornamento disponibile», sotto il nome della release, la misura e da quanto è pubblicata, i tasti «Installa» e «Non ora». «Installa» scarica `mobile-release.apk` nella cache dell'app (`cache/updates/<tag>.apk`) con la barra di avanzamento, calcolando il sha256 mentre scarica; il file vale solo se digest e misura sono quelli della release (altrimenti «Non scaricata: il file non corrisponde al digest della release» e «Riprova»). Una release senza digest non si scarica. In cache resta solo il file del tag proposto.
- **L'installer.** Il file verificato passa all'installer di Android con un `FileProvider` (`cache-path updates/`) e `ACTION_VIEW` di tipo `application/vnd.android.package-archive`; permesso `REQUEST_INSTALL_PACKAGES` nel manifest. La prima volta Android chiede di consentire a Supervisor di installare app: la card lo dice, si apre la schermata del sistema e al ritorno l'installer parte da solo. L'installer accetta solo un APK con la stessa firma di quello installato (la chiave di release della CI): è la verifica della firma, in più al digest.
- **Cosa non fa.** Solo il telefono (e il tablet, stessa app): l'orologio resta via ADB o Play. Niente notifica: la card nella home basta, come chiesto. Niente aggiornamento automatico senza tocco.

## Lo script ADB

`scripts/install-watch.sh` usava `~/android-sdk/platform-tools/adb`, che dal 07/10 è un link a `/usr/bin/adb` (Debian, aarch64): quell'adb connette e installa ma non sa accoppiare, e il 21/10 ha bruciato il primo codice di Franz. Ora usa `adb.x86_64` sotto `qemu-x86_64-static`, avvia il server con `kill-server` (di tutti e due gli adb) e `nohup qemu-x86_64-static adb.x86_64 server nodaemon &`, e ha quattro forme: `pair IP:PORTA CODICE`, `devices` (con il modello), `IP:PORTA [file.apk]` per l'orologio, `phone IP:PORTA file.apk|TAG` per il telefono. Con un tag scarica `mobile-release.apk` della release con `gh`, controlla il sha256 contro il digest della release e solo poi installa. Prima di installare legge `ro.product.model` e rifiuta il dispositivo sbagliato (stesso `applicationId` su telefono e orologio: un APK del telefono sull'orologio lo sostituirebbe). Dopo l'installazione stampa `lastUpdateTime`, lo stesso che legge il controllo del compito.

## P2 e P4 lato app

Il contratto 1.76 è arrivato dalla sessione supervisor alle 01:35 (ramo `feat/contratto-1.76`, 3a71d2f, release 0.8.40): fixture copiate byte per byte in `contract/` (commit 57190468) con il paragrafo nel README. Forma: `state.commitments[]` (task, title, project, session, state, age_s, last_step, idle_s, next, waits {who, on}, expected, check, paused, stalled), evento `task_stalled`, `commitments` nel rapporto della notte v2; `approve` invariato ma dopo l'ok il relay risveglia la sessione e toglie il «ferma per te». La richiesta dell'app (`docs/richieste/2026-10-22-impegni-approva.md`) chiedeva di più (release/installed_at, eventi per impegno, covers, op pausa/riprendi dal telefono): rimandato a un'eventuale 1.77.

Nell'app (commit b318e7fb, fc16ccfe, 9ac63f47):
- **Modello**: `Commitment` e `State.commitments` (null con un relay vecchio), `EventKind.TASK_STALLED` fra gli avvisi forti con la sua notifica sul telefono, `commitments` in `NightReport`; porta web in `contract.ts`.
- **Regole** `Commitments` (core e `commitments.ts`): l'età e il fermo di adesso (quelli del relay più il tempo dalla pubblicazione, `published_at`), colore ambra da 2 ore ferme e rosso da 8 (niente per gli impegni in pausa, che vanno in fondo), a chi tocca (`waits.on`, `person` = Franz), l'ok del registro con lo stesso `task` collegato alla riga. Test: `CommitmentsTest` (5) e `commitments.test.ts` (5) sulle fixture 1.76; i test che contavano i 17 eventi della fixture passano a 18.
- **Home** (P2): sezione fissa «Impegni» sopra le sessioni, richiudibile come le altre, con una card per impegno: titolo, «fermo» quando il registro lo segna, «approvato alle 12:01 · fermo da 9 h» (o «aspetta il tuo ok», o «in pausa: motivo»), «tocca a te» o «tocca a ledger-api», «Prossimo: …», «ultimo controllo rosso».
- **Approva** (P4): sulla riga dell'impegno quando tocca a Franz e c'è l'ok collegato, con la stessa finestra di «Da approvare» (nota facoltativa) e la stessa op `approve`: il relay 0.8.40 risveglia la sessione con un `talk`, avvisa la master e toglie il «ferma per te». Un ok già sulla riga del suo impegno non si ripete in «Da approvare»: una sola attesa, un solo tasto (la stessa regola della master fuori dall'elenco).
- **Screenshot test** `summaryWithCommitments` (state-2 più un `awaiting_ok` del rapporto della notte con l'ok collegato).
- Non fatto: il Gantt degli impegni (la master lo propone a Franz) e pausa/riprendi dal telefono (non nel contratto).

## Prove

- `:core:testDebugUnitTest`: `AppUpdateTest` 9/9 su win (release dal JSON di GitHub dalla più recente, bozze e release senza l'APK del telefono saltate, digest sha256 o niente, proposta solo se più nuova dell'installazione e con digest diverso, «Non ora», file buono solo con digest e misura giusti, controllo ogni mezz'ora, etichette).
- `:mobile:compileDebugKotlin` ok; `SummaryListTest.summaryWithUpdate` eseguito su win e PNG guardato: la card in cima alla home sopra «Sessioni», «Notte 22/10 · 68 MB · pubblicata da 2 h 05», tasti «Installa» e «Non ora». Il golden lo registra la CI.
- Dopo le prove su win `gradlew --stop` (richiesta della master, 00:19: i daemon di Gradle e Kotlin toglievano memoria alla prova di Bluey); le prossime prove Kotlin in CI.
- `bash -n scripts/install-watch.sh` ok; lo script non l'ho provato su un dispositivo (il debug wireless lo accende Franz): la master lo usa per l'installazione di stanotte.

## CI e pre-release

- Run record (golden nuovo di `summaryWithUpdate`): [39685811048](https://github.com/frsorrentino/supervisor-app/actions/runs/39685811048), verde; golden nel commit c35f65b4.
- Run normale (unit core e mobile, Paparazzi, APK debug e release): [39686925850](https://github.com/frsorrentino/supervisor-app/actions/runs/39686925850), verde.
- `feature/tablet` in fast-forward a c35f65b4 (push `notte/link-sessioni:feature/tablet`), ramo `notte/link-sessioni` su origin.
- Pre-release `impegni-2026-10-22` (pre-release, non latest) con `mobile-release.apk` della run 39686925850, firmata (CN=Francesco Sorrentino), digest GitHub uguale allo sha256 locale `67e74732…`: https://github.com/frsorrentino/supervisor-app/releases/tag/impegni-2026-10-22 · APK diretto: https://github.com/frsorrentino/supervisor-app/releases/download/impegni-2026-10-22/mobile-release.apk . Il controllo del compito verifica che il telefono abbia un `lastUpdateTime` successivo alla pubblicazione: l'installazione la fa la master via ADB (`./scripts/install-watch.sh phone IP:PORTA impegni-2026-10-22`).
- Installata sul telefono dalla master con lo script alle 00:48:13 del 22/10 (digest verificato).
- Seconda build con P2 e P4: CI record [39696280817](https://github.com/frsorrentino/supervisor-app/actions/runs/39696280817) (golden `summaryWithCommitments`, commit 304ad730), run normale [39697064343](https://github.com/frsorrentino/supervisor-app/actions/runs/39697064343) verde; `feature/tablet` in fast-forward a 304ad730; pre-release `impegni-2026-10-22-b` (pre-release, non latest, il controllo del compito è su questo tag) con `mobile-release.apk` della run: https://github.com/frsorrentino/supervisor-app/releases/tag/impegni-2026-10-22-b · APK diretto: https://github.com/frsorrentino/supervisor-app/releases/download/impegni-2026-10-22-b/mobile-release.apk . È la prova vera di P6: con la `impegni-2026-10-22` installata, il telefono deve mostrare da solo «Aggiornamento disponibile» e installare da lì; se la card non compare, la master installa via ADB e io cerco il perché.

## Cosa provare sul telefono

Dalla pre-release `impegni-2026-10-22`, sul Pixel 11 Pro XL:

1. Dopo l'installazione via ADB: Impostazioni › «Aggiornamenti» dice «Nessuna novità · controllato alle …» (l'APK installato ha lo stesso digest della release); il tocco ricontrolla.
2. Alla prossima pre-release: in cima alla home la card «Aggiornamento disponibile» con nome, misura e da quanto è pubblicata; «Installa» mostra la barra, poi la prima volta Android chiede di consentire a Supervisor di installare app: al ritorno l'installer si apre da solo; «Non ora» nasconde quel tag finché non ne esce un altro.
3. Se il download non torna col digest: «Non scaricata: il file non corrisponde al digest della release» e «Riprova».
4. La card non compare per l'APK già installato, né per una build di sviluppo messa via ADB dopo l'ultima release.

FINE
