# Piano: la pagina unica «Lavori» (Registro + Recap + Impegni)

> **Per chi esegue:** `superpowers:executing-plans` in questa sessione, un task alla volta; test prima del codice; i passi hanno le caselle.

**Obiettivo:** una sola pagina «Lavori», ordinata per domanda (Per te, In corso, Stanotte, Fatto), al posto di Registro, Recap e della sezione Impegni della home, su Android e web, con la stessa regola in Kotlin e TypeScript.

**Architettura:** una regola nuova in `core` (`Lavori.kt`, porta `lavori.ts`) ricava i quattro blocchi dallo stato del contratto 1.76, dall'agenda (op `agenda`) e dagli eventi; le schermate riusano le card che esistono (`CommitmentCard`, `ApprovalCard`, `AgendaRowCard`, la coda della notte del Registro). Il blocco Fatto nasce dagli eventi `task_check` verdi e passa all'op `done` quando arriva il contratto 1.77 (`docs/richieste/2026-10-22-lavori-fatto.md`).

**Stack:** Kotlin, Compose Material 3, Paparazzi; Svelte 5, vitest; CI GitHub Actions per APK e golden; Gradle solo su win con RAM libera ≥ 6 GB.

**Spec:** il mockup approvato `docs/mockup/2026-10-22-lavori/` (README con la nota sui dati) e il brief della master del 22/10 alle 07:47.

## Vincoli

- Niente campi inventati: i dati vengono da `state.commitments`, `state.approvals`, `state.night`, op `agenda`, eventi `task_check`; la prova e i giorni prima solo con la 1.77.
- Ogni blocco si spiega da solo in una riga (`strings.xml` e `t.ts`, it e en); mai testi cablati nel Kotlin.
- Un solo bottone pieno per schermata: «Approva» (o «Aggiungi» se non ci sono ok); gli altri tonali.
- La quota esce dalla pagina: resta in «Utilizzo e limiti».
- Ramo `lavori/pagina-unica` da `feature/tablet`; commit in inglese; push e pre-release coperti dall'ok del 22/10 12:43, con il compito registrato dalla master; nessun deploy web senza ok.
- Gradle e vitest su win o in CI, mai sul Chromebook; `gradlew --stop` dopo ogni giro su win.

## Punti da sorvegliare (test nel task che li possiede)

1. Relay vecchio senza `commitments` e senza agenda: la pagina mostra Stanotte e Fatto, Per te e In corso vuoti con la riga «Aggiorna supervisor sul PC» (Task 2, `oldRelayHasNoRows`).
2. Un ok del registro collegato a un impegno non deve comparire due volte (Per te «Ok da dare» e «Da approvare» della home): Task 2 `anOkIsNeverListedTwice`.
3. Un impegno approvato prima dell'asse dei sette giorni: la barra parte dal bordo sinistro, non va a sinistra dello zero (Task 2 `aBarOlderThanTheAxisStartsAtTheEdge`).
4. Una pausa senza `waits.on = person` (relay 1.76) resta in «In corso · in pausa», non in Per te (Task 2 `aPauseWithoutPersonStaysInProgress`).
5. Due `task_check` dello stesso compito nello stesso giorno (rosso poi verde): una riga sola, l'ultima (Task 2 `doneKeepsTheLastCheckOfATask`).

---

### Task 1: il contratto, richiesta 1.77

**File:** `docs/richieste/2026-10-22-lavori-fatto.md` (scritta il 22/10), `contract/README.md` quando le fixture arrivano.

- [x] Richiesta scritta e passata alla master (`supervisor talk master`), che la gira alla sessione supervisor.
- [ ] All'arrivo del ramo del relay: copiare le sei fixture byte per byte in `contract/`, aggiungere il paragrafo «Contratto 1.77» al README, far passare i test che contano le op (`RepoTest`, `FakeTransportTest`).

### Task 2: la regola `Lavori` in core

**File:** crea `core/src/main/kotlin/it/pixelbox/cmwatch/rules/Lavori.kt`, `core/src/test/kotlin/it/pixelbox/cmwatch/rules/LavoriTest.kt`.

**Interfacce (le usano i task 3, 4, 6):**

```kotlin
object Lavori {
    enum class Kind { OK, OUTSIDE, CLAUDE }
    /** Una riga di «Per te». `who` = chi chiede o chi si sblocca; `at` = quando (epoch s) o null. */
    data class ForYou(val kind: Kind, val title: String, val who: String?, val at: Long?, val approval: Approval? = null, val commitment: Commitment? = null, val agenda: AgendaRow? = null)
    data class ForYouGroups(val ok: List<ForYou>, val outside: List<ForYou>, val claude: List<ForYou>) { val total get() = ok.size + outside.size + claude.size }
    fun forYou(state: State, agenda: AgendaPage?, now: Long, today: LocalDate): ForYouGroups

    /** Frazioni 0..1 dell'asse; `dashed` = in pausa. */
    data class Bar(val from: Double, val to: Double, val dashed: Boolean)
    data class Axis(val start: Long, val end: Long, val days: List<Pair<String, Double>>, val now: Double)
    fun axis(now: Long, zone: ZoneId, days: Int = 7): Axis
    fun bars(c: Commitment, axis: Axis, now: Long): List<Bar>
    data class Work(val row: Commitments.Row?, val agenda: AgendaRow?, val bars: List<Bar>)
    data class InProgress(val active: List<Work>, val paused: List<Work>, val others: List<Work>, val axis: Axis)
    fun inProgress(state: State, agenda: AgendaPage?, now: Long, zone: ZoneId, today: LocalDate): InProgress

    /** `proof` = null finché non c'è la 1.77: la UI scrive «controllo verde». */
    data class Done(val task: String, val title: String, val session: String?, val at: Long, val proof: String?)
    data class DoneDay(val date: LocalDate, val rows: List<Done>)
    fun done(events: List<Event>, zone: ZoneId, today: LocalDate, days: Int = 7): List<DoneDay>
    const val MIN_BAR = 0.01
}
```

Regole:
- `forYou.ok` = le righe di `Commitments.rows(state, now)` con `yourTurn` e non in pausa (title = `c.title`, who = `c.session`, at = `approval?.requestedAt ?: c.lastStep?.at`), più gli `state.approvals` non collegati a un impegno (who = `a.where`, at = `a.requestedAt`); ordine per `at` crescente, null in fondo.
- `forYou.outside` = `RecapAgenda.of(agenda, null, today).you` (title = `row.title`, who = `row.ref`), poi gli impegni con `paused != null && waits?.on == Commitments.PERSON` (title = `paused.reason`, who = `c.session`, at = `paused.at`).
- `forYou.claude` = `RecapAgenda.of(agenda, null, today).claude`.
- `axis`: start = mezzanotte locale di `today − (days − 1)`, end = mezzanotte di domani; `days` = etichette «16», «17», … con la frazione di inizio; `now` = frazione di `now`.
- `bars`: inizio = `approvedAt ?: now − ageS`, tagliato a `axis.start`; in pausa con `paused.at` ≥ inizio: piena [inizio, paused.at] più tratteggiata [paused.at, now]; in pausa con `paused.at` < inizio: tutta tratteggiata; altrimenti piena [inizio, now]; larghezza minima `MIN_BAR`.
- `inProgress.active` = righe di `Commitments.rows` non in pausa e non `yourTurn`; `paused` = in pausa (anche quelle che aspettano la persona: restano visibili qui, richiuse); `others` = `RecapAgenda.of(...).other` (terzi) senza barre.
- `done`: eventi `TASK_CHECK` con `title` che inizia con «✓», una riga per `ref` (l'ultima per `ts`), raggruppati per giorno locale, oggi primo, al massimo `days` giorni; `title` = prima riga di `body`.

- [ ] **Passo 1: i test** (`LavoriTest.kt`): `theFixturesGiveTheFourBlocks` (state-2-idle + agenda …0386 + night-report commitments + un `Approval("ledger-release-14", …)`: ok = 1, outside = le righe `you` dell'agenda, claude = le righe `claude`, active = atlas-seo-pages e ledger-migrations-011, done = `atlas-release-2-4` con proof null), `anOkIsNeverListedTwice`, `aPauseWithoutPersonStaysInProgress` (state-3-stale), `aPauseThatWaitsThePersonIsForYou` (copy con `waits = CommitmentWaits("orbit-docs", "person")`), `aBarOlderThanTheAxisStartsAtTheEdge` (approvedAt = axis.start − 86400 → from = 0.0), `aPausedBarIsSolidThenDashed`, `doneKeepsTheLastCheckOfATask` (events-sample + un secondo task_check verde di ledger-migrations-011 più tardi), `oldRelayHasNoRows` (commitments = null, agenda = null → gruppi vuoti, done dagli eventi).
- [ ] **Passo 2:** eseguire su win `.\gradlew.bat :core:test --tests "*LavoriTest*"` (RAM ≥ 6 GB, poi `--stop`): rosso per classe mancante.
- [ ] **Passo 3:** `Lavori.kt` come sopra.
- [ ] **Passo 4:** test verdi; `git commit -m "feat(core): Lavori — the four blocks of the single page (Per te, In corso with bars, Fatto from task_check) from the 1.76 state, the agenda and the events"`.

### Task 3: la porta TypeScript

**File:** crea `web/src/lib/lavori.ts`, `web/src/lib/lavori.test.ts`.

- [ ] Stessi tipi e funzioni di Task 2 (`forYou`, `axis`, `bars`, `inProgress`, `done`), stessi otto casi di test sulle stesse fixture (`commitments.test.ts` e `recapAgenda.test.ts` mostrano come caricarle). `npx vitest run src/lib/lavori.test.ts` su win.
- [ ] Commit `test: lavori.ts mirrors LavoriTest`.

### Task 4: `LavoriScreen` su Android

**File:** crea `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/LavoriScreen.kt`, `mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/LavoriScreenTest.kt`; modifica `SummaryList.kt` (rendi `internal` `ApprovalCard` e `CommitmentCard`), `DiaryScreen.kt` (sposta `Tonight` e `RegLabel` in `LavoriScreen.kt` come `internal`), `res/values/strings.xml` e `values-en/strings.xml`.

Firma:

```kotlin
@Composable fun LavoriScreen(
    state: State, agenda: AgendaPage?, events: List<Event>, now: Long,
    onBack: () -> Unit, onApprove: (task: String, note: String) -> Unit, onAgendaEdit: (RecapAgenda.Edit) -> Unit,
    onStart: (RecapActions.Action) -> Unit, onDoneForPerson: (Commitment) -> Unit,
    onAddNight: () -> Unit, onRemoveNight: (String) -> Unit, onNight: () -> Unit, onSession: (String) -> Unit,
    topBar: Boolean = true, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault(),
)
```

Struttura (`LazyColumn`, chiavi `sec-foryou`, `fy-<kind>-<id>`, `sec-progress`, `wk-<id>`, `sec-tonight`, `sec-done`, `dn-<task>`): testata «Lavori · N per te»; ogni blocco con un'intestazione come `HomeSection`, la riga che lo spiega (`lavori_foryou_expl`, …), i gruppi di Per te con i tre tasti (`Approva` pieno via `ApprovalCard`, `Fatto` tonale: agenda → `onAgendaEdit(Edit(DONE, row))`, pausa → `onDoneForPerson(c)`; `Avvia` tonale → `onStart(RecapAgenda.doIt(row))`), «Altre N» richiuse (`rememberSaveable`); In corso con l'asse (`Row` di etichette) e `LavoriBar` (`Canvas`: pista, tratti pieni/tratteggiati con i colori di `NightGanttViews` RUNNING e UNKNOWN, riga di adesso); Stanotte = `Tonight` del Registro più la riga «La notte del <data>» → `onNight`; Fatto = giorni, oggi aperto, con `hhmmOf` e la prova (`lavori_proof_check` finché null).

Stringhe nuove: `menu_lavori` «Lavori», `menu_lavori_sub` «Per te, in corso, stanotte, fatto», `lavori_title`, `lavori_foryou`, `lavori_foryou_expl`, `lavori_ok`, `lavori_outside`, `lavori_claude`, `lavori_more` «Altre %1$d», `lavori_start` «Avvia», `lavori_progress`, `lavori_progress_expl`, `lavori_axis_today` «oggi», `lavori_key`, `lavori_paused_n`, `lavori_others_n`, `lavori_tonight_expl`, `lavori_last_night` «La notte del %1$s», `lavori_done`, `lavori_done_expl`, `lavori_today` «Oggi %1$s · %2$d», `lavori_proof_check` «controllo verde», `lavori_quota_note`, `lavori_old_relay`. Via: `menu_register*`, `menu_recap*`, `home_sec_recap`, `home_sec_commitments` (Task 7).

- [ ] **Passo 1:** `LavoriScreenTest` con Paparazzi: `lavoriFull` (state-2-idle + agenda + eventi + un ok), `lavoriEmptyOldRelay`, `lavoriTablet` (topBar = false, 840 dp). Goldens registrati in CI (`gh workflow run build-android.yml --ref lavori/pagina-unica -f record=true`, artefatto `paparazzi-snapshots`), poi committati.
- [ ] **Passo 2:** la schermata; compilazione su win (`:mobile:compileReleaseKotlin`) o in CI.
- [ ] **Passo 3:** commit `feat(phone): the single «Lavori» page — Per te with a button per row, In corso with the seven-day bars, Stanotte, Fatto per day`.

### Task 5: il cablaggio Android (rotte, menu, home, tablet)

**File:** `core/.../rules/StartRoute.kt` (`Tab.DIARY` → `Tab.LAVORI`), `mobile/.../MainActivity.kt` (le 21 occorrenze di `Tab.DIARY`; `diaryPage()` monta `LavoriScreen`; `recapPage` e `RecapScreen` via; `onRegister`/`onRecap` → la pagina Lavori; `onDoneForPerson` manda alla master `CmdOp.PROMPT` «Fatto: <motivo>. Riprendi <task>»), `AppMenuPanel.kt` (voce «Lavori» al posto di Registro e Recap; Notte con il sottotitolo «Il rapporto di stanotte e le notti prima»), `SummaryList.kt` (via `sec-commitments`, `sec-recap` e «Da approvare»: una sezione `sec-lavori` con `Lavori.forYou(...).ok + outside.take(2)` e la riga «Tutti i lavori · in corso N · stanotte …» → `onLavori`), `TabletShell.kt` (le colonne del cruscotto che montavano `RecapScreen(part = you/claude)` montano `LavoriScreen(topBar = false)` in due colonne come il mockup tablet), `SearchScreen.kt` (etichetta `menu_lavori`), `SummaryListTest.kt` e `TabletShellTest.kt` (goldens).

- [ ] Rinomina, cablaggio, goldens in CI, commit `feat(phone): Lavori replaces Registro, Recap and the home's Impegni section — menu, home section, tablet columns`.

### Task 6: la web app

**File:** crea `web/src/lib/Lavori.svelte`; modifica `App.svelte` (page `'lavori'`; `'diary'` e `'recap'` nel link `?p=` aprono `'lavori'`; `loadAgenda` anche per `'lavori'`; le snippet `recapYou`/`recapClaude` del cruscotto diventano `lavoriLeft`/`lavoriRight`), `AppBar.svelte` (tipo `Page`, voce di menu), `Home.svelte` (sezioni `commitments`, `recap` e «Da approvare» → `lavori`), `Desk.svelte`/`tablet.ts` (colonne), `t.ts` (it ed en, gli stessi nomi di Task 4 in camelCase), `Search.svelte` (`t.menuLavori`).

- [ ] `Lavori.svelte` con `lavori.ts`; `npm run build` e `npx vitest run` su win; svelte-check non deve aggiungere errori ai 5 noti.
- [ ] Commit `feat(web): the single «Lavori» page and its place in menu, home and desk`.

### Task 7: via Registro e Recap

**File:** elimina `mobile/.../ui/DiaryScreen.kt`, `RecapSection.kt` solo la parte `RecapScreen` (restano `AgendaRowCard`, `RecapActionChip`, `RecapSendSheet`), `DiaryScreenTest.kt`, `RecapScreenTest.kt` e i loro goldens, `core/.../rules/PhoneDiary.kt` se non più usato, `web/src/lib/Diary.svelte`, `RecapPage.svelte`, `diary.ts`, `diary.test.ts`; le stringhe `reg_*`, `diary_*`, `recap_filter_*`/`recap_scope_*` se orfane (`grep -rn R.string.<nome>` prima di togliere).

- [ ] Compilazione, test, commit `refactor: Registro and Recap pages removed, Lavori carries them`.

### Task 8: CI, pre-release, telefono, resoconto

- [ ] CI verde sul ramo; goldens aggiornati; `git push origin lavori/pagina-unica`; fast-forward di `feature/tablet`.
- [ ] Pre-release `lavori-2026-10-22` (`--prerelease --latest=false`, APK dal run verde, `apksigner verify`): il telefono la vede con l'aggiornamento dentro l'app (P6).
- [ ] Resoconto `docs/verifiche/2026-10-22-lavori.md`: cosa c'è, cosa aspetta la 1.77, checklist dal vivo (Approva, Fatto su una riga dell'agenda, Avvia, Aggiungi alla notte, i giorni di Fatto).
- [ ] `supervisor task step` a ogni traguardo; `task done` con il controllo (release installata).

### Task 9: il blocco Fatto con la 1.77 (quando il relay la pubblica)

**File:** `core/.../contract/Model.kt` (`DonePage`, `DoneRow`, `DoneProof`, `DoneDayRef`; `CmdOp.DONE`), `ContractJson.kt` (`decodeDone`), `Lavori.kt` (`done(page: DonePage, …)` accanto a quella dagli eventi, con la prova e i giorni), `lavori.ts`, `LavoriScreen.kt` e `Lavori.svelte` (la prova come link, i giorni richiusi che si aprono con un'altra chiamata), `web/src/lib/contract.ts`; test su `cmd-result-sample.json`.

- [ ] Fixture copiate (Task 1), tipi, regola, UI, test; pre-release `lavori-2026-10-22-b`.
