# Suggerimenti in coda: tre alla volta, tipi nuovi, tasti per opzione — piano

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans (o subagent-driven-development). Passi con
> checkbox (`- [ ]`).

**Goal:** la scheda «Suggerimenti della master» (telefono e web) mostra tre voci scelte dalla coda per `rank`, con al
massimo due dello stesso tipo; usata una sale la successiva; i sei tipi nuovi hanno icona ed etichetta, un tipo
sconosciuto è «Suggerimento»; le scelte (`choice`) hanno un tasto per opzione; «Parliamone» passa dal relay come op.

**Architecture:** una regola pura per lato, uguale in `core/.../rules/Suggestions.kt` e `web/src/lib/suggestions.ts`,
con gli stessi casi di test. Le schede (`SuggestionsCard.kt`, `SuggestionsCard.svelte`) mappano `kind` su icona,
tono ed etichetta. Le azioni passano da `Repo.suggestion` e da `newCmd('suggestion', …)`.

**Tech Stack:** Kotlin + kotlinx.serialization + Compose (mobile), Svelte 5 + vitest (web), Paparazzi (goldens da CI).

**Spec:** `/home/franz/Desktop/workspaces/.claude/proposte/suggerimenti-coda-2026-10-26.md` (§8.2, §8.7, §8.8, §10.3).
Compito `suggerimenti-coda-app-27-10` (ok di Franz 27/10 20:34, inbox 0d4f50). Ramo `tab-emoji/22-10`.

## Global Constraints

- Numero del contratto: il documento dice 1.87, ma 1.87 è già uscito oggi (supervisor 0.8.69, «already gone»). Il
  numero vero è quello che manda supervisor (atteso 1.88); test e commenti usano quello.
- **Prima il contratto, poi il codice che lo legge.** Solo il Task 1 parte prima: non legge campi nuovi.
- Additivo, `v` resta 1. Campi nuovi facoltativi in `Suggestion`: `rank: Int?` (0-9, assente = 5), `source: String?`,
  `options: List<String>?` (2-4, solo `choice`). `prompt` sparisce dallo stato: in Kotlin ha già default `""`, in TS
  diventa facoltativo.
- Tipi nuovi: `choice`, `followup`, `unsaved`, `waiting`, `idea`, `proposal`. Tipo sconosciuto: etichetta
  «Suggerimento» / «Suggestion», icona e tono neutri (non più «QUOTA»).
- Testi in `strings.xml` (IT) e `values-en/strings.xml` (EN), web in `t.ts` IT ed EN. Una riga, mai «…».
- Tasti tonali: Accetta; per una `choice` con `options`, un tasto tonale per opzione al posto di Accetta, come in
  «Per te» (`Lavori.svelte:100`). Parliamone, Non ora e Mai restano testuali.
- Commit in inglese, file per nome, mai `-A`. Kotlin: suite su CI (memoria win-ram); vitest solo su win.
- Pre-release e web compresi nell'ok; l'APK lo installa la master.

## Review Focus

1. Coda tutta dello stesso tipo (es. 5 `unsaved`): la scheda mostra comunque tre voci, non due. Regola: la varietà
   sceglie prima fra le altre, poi riempie con le scartate nell'ordine. Test `fillsWhenOnlyOneKind`.
2. `rank` assente o fuori 0-9 (relay vecchio, voce a mano): vale 5, niente crash. Test `missingRankIsFive`.
3. `choice` senza `options` o con una sola: mostra Accetta come le altre, niente tasti vuoti. Test sulla regola
   `Suggestions.options(s)` = lista solo se 2-4 etichette.
4. Relay che non conosce `action: discuss` (risposta non ok): Parliamone ripiega sul prompt di oggi alla master,
   così il tasto non muore. Verifica nel codice dell'azione, non solo nel test.
5. Doppio tocco veloce su una opzione: la voce è già in `sugDone`, il secondo tocco non c'è (la voce sparisce subito).
   Nessun test nuovo: già coperto dal comportamento esistente, da ricontrollare a mano sul web.

---

### Task 1: tipo sconosciuto neutro e i sei tipi nuovi (prima del contratto)

**Files:**
- Modify: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/SuggestionsCard.kt` (blocco `when (s.kind)`)
- Modify: `mobile/src/main/res/values/strings.xml`, `mobile/src/main/res/values-en/strings.xml` (dopo `sug_kind_quota`)
- Modify: `web/src/lib/SuggestionsCard.svelte` (`tone`, `icon`, fallback), `web/src/lib/t.ts` (`sugKind`, `sugKindOther`)
- Modify: `web/src/lib/contract.ts:56` (`SuggestionKind`), `web/src/lib/suggestions.ts` (`KINDS`)
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/rules/SuggestionsTest.kt`, `web/src/lib/suggestions.test.ts`

**Interfaces:**
- Produces: `Suggestions.KINDS: List<String>` (Kotlin) e `KINDS` (TS) = `recurring, cleanup, deadline, quota, choice,
  followup, unsaved, waiting, idea, proposal`; `Suggestions.known(kind: String): Boolean`.

Etichette IT/EN: `choice` Scelta/Choice · `followup` Seguito/Follow-up · `unsaved` Da salvare/Unsaved · `waiting`
In attesa/Waiting · `idea` Idea/Idea · `proposal` Proposta/Proposal · sconosciuto Suggerimento/Suggestion.
Icone Material Rounded: `AltRoute`, `Redo`, `SaveAs`, `HourglassEmpty`, `Lightbulb`, `TipsAndUpdates`; sconosciuto
`AutoAwesome` col tono `text2`. Toni: `choice` `briefWarn`, `followup` `briefRing`, `unsaved` `idle`, `waiting`
`text2`, `idea` `advice`, `proposal` `advice`. Web: le stesse forme come path SVG 24×24
stroke, toni dalle variabili già usate (`--b-warn`, `--b-ring`, `--good`, `--text2`, `--advice`).

- [ ] **Step 1: test che fallisce** — `knownKinds`: `Suggestions.known("unsaved") == true`, `known("zzz") == false`,
  `KINDS.size == 10`; stesso caso in vitest.
- [ ] **Step 2:** CI/win fallisce su `known` non definito.
- [ ] **Step 3:** `known` + rami del `when`; `else` neutro con `sug_kind_other`. Web: `t.sugKind[s.kind] ??
  t.sugKindOther`, icona di fallback la stella (✦ come path), tono `--text2`.
- [ ] **Step 4:** test verdi (vitest su win; Kotlin in CI col push del Task 4).
- [ ] **Step 5: commit** `feat(home): suggestion kinds for the queue (choice, followup, unsaved, waiting, idea,
  proposal); an unknown kind reads «Suggerimento», not «Quota»`.

### Task 2: modello e fixture del contratto nuovo (dopo il messaggio di supervisor)

**Files:**
- Modify: `core/src/main/kotlin/it/pixelbox/cmwatch/contract/Model.kt:304` (`Suggestion`)
- Modify: `web/src/lib/contract.ts:57` (`Suggestion`)
- Fixture: `contract/state-2-idle.json`, `contract/cmd-result-sample.json`, `contract/README.md` (dal PC, identiche)
- Test: `core/src/test/.../contract/ContractTest.kt:85-86,305`, `web/src/lib/contract.test.ts:130-143`

**Interfaces:**
- Produces: `Suggestion(…, val rank: Int? = null, val source: String? = null, val options: List<String>? = null)`;
  TS `{ …; prompt?: string; rank?: number | null; source?: 'script' | 'master' | string | null; options?: string[] | null }`.

- [ ] **Step 1:** copiare le fixture del nuovo contratto; adeguare i test: `s2.suggestions` ha una voce per ogni tipo
  nuovo (conteggi presi dalla fixture), nessuna voce ha `prompt`, la `choice` ha `options` di 2-4; i risultati di
  `choose` e `discuss` ci sono (conteggi `results.size`/`!ok` aggiornati sui numeri della fixture).
- [ ] **Step 2:** i test falliscono sulle asserzioni di `rank`/`options` (la decodifica ignora i campi sconosciuti,
  quindi non è la lettura a rompersi).
- [ ] **Step 3:** campi nel modello.
- [ ] **Step 4:** verdi.
- [ ] **Step 5: commit** `test(contract): contract 1.88 fixture — suggestion rank, source, options, the six new
  kinds, no prompt in state; choose and discuss results`.

### Task 3: la regola della coda (rank, varietà, opzioni)

**Files:**
- Modify: `core/src/main/kotlin/it/pixelbox/cmwatch/rules/Suggestions.kt`, `web/src/lib/suggestions.ts`
- Test: `SuggestionsTest.kt`, `suggestions.test.ts` (stessi casi, stessi nomi)

**Interfaces:**
- Consumes: `Suggestion.rank`, `.kind`, `.options` (Task 2).
- Produces: `Suggestions.visible(state, done, now): Visible` (firma invariata); `Suggestions.options(s): List<String>`
  (vuota salvo 2-4 etichette e `kind == "choice"`); TS `visible(...)`, `options(s): string[]`.

Ordine: `rank` crescente (assente o fuori 0-9 = 5), a parità `at` decrescente (il più recente in alto, come oggi).
Scelta delle tre: si scorre la lista ordinata e si prende una voce se il suo tipo ne ha già meno di 2 fra le prese;
se dopo il giro sono meno di 3, si riempie con le scartate nell'ordine. `total` = tutte le voci filtrate.

- [ ] **Step 1: test che falliscono**
  - `rankBeforeAt`: voci (id, rank, at) a(6,50) b(2,10) c(4,40) d(null,60) → `shown` = b, c, d; `total` 4.
  - `missingRankIsFive`: d(null,60) sta fra c(4) e a(6); rank 12 vale 5.
  - `atMostTwoOfAKind`: tre `unsaved` rank 4 e una `idea` rank 6 → due `unsaved` più l'`idea`.
  - `fillsWhenOnlyOneKind`: cinque `unsaved` → tre `unsaved`, `total` 5.
  - `optionsOnlyForChoice`: `choice` con ["A","B"] → ["A","B"]; con ["A"] → []; `idea` con ["A","B"] → [].
  - il caso di oggi `threeAtMostNewestFirst` resta verde (tutte senza rank).
- [ ] **Step 2:** falliscono.
- [ ] **Step 3:** implementare in Kotlin e TS; aggiornare i commenti di testa (contratto nuovo, Franz 26/10 20:44).
- [ ] **Step 4:** verdi.
- [ ] **Step 5: commit** `feat(home): suggestions come from the queue by rank, at most two of a kind among the three`.

### Task 4: tasti per opzione e Parliamone come op

**Files:**
- Modify: `SuggestionsCard.kt` (firma `onAnswer: (Suggestion, String, String?) -> Unit`), `MainActivity.kt:902-913`
- Modify: `core/.../data/Repo.kt:216` → `suggestion(id: String, action: String, choice: String? = null)`
  (`command(...)` ha già `choice`, `Repo.kt:235`)
- Modify: `SuggestionsCard.svelte`, `App.svelte:233-241`
- Test: `core/src/test/.../data/` caso del comando (se esiste un test di `Repo.command` serializzato; altrimenti
  `ContractTest` sul JSON del cmd `suggestion` con `action: choose`, `choice: "B"`); Paparazzi: un caso nuovo in
  `SummaryListTest` con una `choice` a due opzioni e un tipo sconosciuto, golden da CI.

Comportamento:
- `choice` con `options(s)` non vuota: al posto di Accetta un tasto tonale per opzione; tocco → `sugDone += id`, op
  `suggestion {action: choose, choice: <etichetta>}`; ok → toast `sug_sent`; rifiuto → la voce torna, toast del relay.
- Parliamone: op `suggestion {action: discuss}`, la voce resta (niente `sugDone`), si apre la chat della master; se
  la risposta non è ok, ripiego sul prompt di oggi (`sug_discuss_prompt` / `t.sugDiscussPrompt`) a `MASTER`.

- [ ] **Step 1:** test del comando `choose` (JSON con `choice`) e golden nuovo (fallisce: manca il caso).
- [ ] **Step 2:** falliscono.
- [ ] **Step 3:** implementare.
- [ ] **Step 4:** vitest su win; push del ramo → CI Android verde, goldens nuovi scaricati dall'artifact e committati
  (memoria paparazzi: mai goldens da win).
- [ ] **Step 5: commit** `feat(home): one button per option on a suggested choice; «Parliamone» goes through the relay
  as the discuss action`, poi `test(home): Paparazzi goldens from CI for the suggestion queue`.

### Task 5: verifica, rilascio, resoconto

- [ ] Web: `npm run build` e deploy hosting su win come le volte scorse; controllo con chrome-bridge (`timeOrigin`,
  `script src`) che la build nuova sia quella caricata; screenshot della scheda con la fixture demo che ha i tipi nuovi
  → `docs/screenshots/suggerimenti-coda-web.png`, mandato a Franz con SendUserFile.
- [ ] Pre-release `notte-2026-10-27` (o nome libero se esiste) con l'APK del run verde (memoria github-prerelease-apk).
- [ ] Resoconto `docs/verifiche/2026-10-27-suggerimenti-coda.md`: cosa è provato (test, CI, web dal vivo) e cosa no
  (telefono: lo installa la master; «dopo un tocco sale la quarta» dal vivo solo quando `fill` gira sul PC).
- [ ] Il check del compito passa: commit con «suggeriment» su `origin/tab-emoji/22-10` dopo le 20:34 e pre-release.
- [ ] Riferire alla master con `supervisor talk master "…"`.
