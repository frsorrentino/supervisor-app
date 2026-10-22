# Tab accetta il suggerimento · niente emoji — piano

Compiti `tab-suggerimento-22-10` e `niente-emoji-22-10` (ok di Franz dal web, 22/10 13:32 e 13:36; registrati dalla
master). Ramo `tab-emoji/22-10` da `lavori/pagina-unica` (`3ded2d77`); una sola pre-release `tab-emoji-2026-10-22`.

## Task 1 — Tab e tocco accettano il suggerimento del campo

- Regola unica: `NextSteps.acceptSuggestion(draft, field)` in Kotlin e `acceptSuggestion(draft, field)` in
  `web/src/lib/composer.ts`: col campo vuoto e un suggerimento visibile torna il suggerimento, altrimenti null
  (niente cambia). Test prima (`NextStepsTest`, `composer.test.ts` con «Tab» nel nome del caso).
- Web `Composer.svelte`: Tab senza Maiusc nel campo → `preventDefault`, il suggerimento diventa testo; il focus resta
  perché l'evento non prosegue. Vale anche nel campo stretto (riga `sugrow`).
- Telefono `SessionSheet.kt` (Composer): un tocco sul testo attenuato del suggerimento lo mette nel campo e chiede
  il focus. Il segnaposto non riceve i tocchi (sta sotto il campo): una superficie trasparente sui suoi confini,
  misurati con `onGloballyPositioned`, solo col campo vuoto. «Usa» resta.
- Commit: `feat(composer): Tab on the web and a tap on the phone accept the field suggestion`.

## Task 2 — Filtro delle emoji sui testi del relay

- `rules/Emoji.kt` + `web/src/lib/emoji.ts`: `strip(text)` toglie i pittogrammi (U+1F000–1FAFF), simboli e dingbat
  (U+2600–27BF), frecce e quadrati emoji (U+2B00–2BFF), orologi e comandi (U+231A–231B, 23E9–23F3, 23F8–23FA),
  le forme con presentazione emoji (U+25AA–25AB, 25B6, 25C0, 25FB–25FE), ‼ ⁉ ℹ, VS15/VS16, ZWJ e keycap;
  poi spazi doppi, spazi e «·» ai bordi. Test prima.
- Kotlin: `Event.shareTitle`/`shareBody` passano dal filtro; `AlertEvents.green` legge `ok` (1.78) e solo in
  mancanza il «✓» del titolo grezzo. Web: `alertEvents.green` idem; gli eventi si puliscono quando arrivano
  (`App.svelte`) e la push del service worker (`sw.ts`) pulisce titolo e corpo.
- Notifiche del telefono: titoli e corpi da `shareTitle`/`shareBody` e dal filtro (esito, domanda, fermo per te).
- `NotificationTexts`/`NotificationPlan` (orologio e test): titoli senza ❓ ✓ ✗ ⚠, `person` senza il pallino.
- Commit: `feat(core): emoji filter on relay titles and bodies; notification titles without symbols`.

## Task 3 — Icona della notifica per tipo di avviso

- `rules/NotificationIcon.kt`: `of(kind, ok)` → QUESTION, DONE, CHECK_OK, CHECK_KO, BLOCK (quota), WARN (turno
  fallito, riavvio fallito, relay fermo), WEEK, HAND (ferma per te), BELL (promemoria), HOURGLASS (impegni fermi),
  NIGHT (diario, notte), CROSS (chiusa), APP. Test prima.
- Drawable Lucide a tratto 2,5 (bianco, come `ic_w_*`): nuovi `ic_n_check`, `ic_n_x`, `ic_n_ban`, `ic_n_alert`,
  `ic_n_bell`; riuso di `ic_w_outcome`, `ic_w_waiting`, `ic_w_week`, `ic_w_turn`, `ic_w_night`, `ic_notification`.
- `PhoneNotifier`: `setSmallIcon(icon(kind))` in ogni notifica.
- Commit: `feat(phone): notification icon by alert kind`.

## Task 4 — Niente glifi di stato nell'app: icone vettoriali

- `strings.xml`/`values-en`: via ✓ ✗ ❓ da `night_c_*`, `night_st_*`, `night_w_*`, `night_wc_*`, `night_gt_*`,
  `question_resolved_terminal`. Il controllo del compito è il grep vuoto su questi file e su `t.ts`.
- Mobile `ui/StateIcons.kt`: icona Material per `NightPage.Icon`, `Where`, `GoalState`; usata nelle chip dei
  conteggi (NightScreen, SummaryList, NightGanttViews), nella pillola di stato e nei punti degli obiettivi;
  `stateGlyph` e `pointMark` spariscono.
- Web `t.ts` senza ✓ ▶ ✗ ❓ ◐; `icons.ts` con `play`, `x`, `question`, `half`, `circle`; `NightPage.svelte`,
  `Home.svelte`, `Dash.svelte` con l'icona davanti al numero.
- Golden Paparazzi registrati in CI (`-f record=true`).
- Commit: `refactor(ui): status glyphs become vector icons`.

## Task 5 — Verifica e consegna

- Core e mobile su win (`run-lavori.ps1`, `run-mobile.ps1`), vitest + build + svelte-check (`run-web.ps1`).
- CI record → golden committati; CI verify + APK → pre-release `tab-emoji-2026-10-22` (non latest).
- `supervisor task done` dei due compiti; resoconto in `docs/verifiche/2026-10-22-tab-emoji.md`; handoff.

Fuori: tile e complicazioni dell'orologio (`TileTexts`, `ComplicationTexts`: testo ProtoLayout, i glifi sono le
icone per disegno), il «↻» del widget e le frecce tipografiche «→» «↳» (non sono emoji); il testo delle chat.
