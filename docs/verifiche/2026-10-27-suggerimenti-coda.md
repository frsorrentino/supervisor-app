# Suggerimenti in coda — verifica (27/10/2026)

Compito `suggerimenti-coda-app-27-10`, contratto 1.88 (supervisor 0.8.70). Piano: `docs/plans/2026-10-27-suggerimenti-coda-piano.md`.

## Provato

- Regola della coda, uguale in `Suggestions.kt` e `suggestions.ts`: ordine per `rank` (assente o fuori 0-9 = 5) poi dalla
  più recente; al massimo due dello stesso tipo fra le tre, ma una coda di un tipo solo ne mostra comunque tre; tasti solo
  sulle `choice` con 2-4 opzioni. Test nuovi rossi prima, verdi dopo (vitest su win; Kotlin in CI).
- Fixture 1.88 identiche a quelle di supervisor: state-2 (tre voci con rank e source, senza prompt), state-3 (una voce per
  tipo nuovo), cmd-result-sample (choose rifiutato, discuss, choose accettato).
- Trasporto demo (Kotlin e web) come il relay: choose fuori dalle opzioni rifiutato e la voce resta; discuss la lascia;
  choose la toglie. Test Kotlin `suggestionChooseAndDiscussLikeTheRelay`; il lato web non ha un test del trasporto demo.
- Golden Paparazzi `summaryWithSuggestionQueue` (da CI): scelta con «Breve» e «Completa» al posto di Accetta, «Da salvare»,
  tipo sconosciuto «Suggerimento» neutro. I golden esistenti non sono cambiati.
- CI Android 40577293084 su `4df0acc2`: test e verifica Paparazzi verdi.
- Web pubblicata: il sito serve `main-IPNISshG.js`, lo stesso della build.

## Non provato

- Sul telefono: l'APK lo installa la master.
- Dal vivo «usata una, sale la quarta» e i tipi nuovi: arrivano nello stato solo quando supervisor mette la riga oraria
  di `suggest fill` in crontab, dopo l'installazione.
- Parliamone come op contro il relay vero: provato solo sul trasporto demo; se il relay rifiuta, l'app ripiega sul prompt
  di prima.
- Vitest su win: `zip.test.ts` («unzip lo apre e trova i file com'erano») fallisce; i file di questo compito non lo
  toccano, ma non ho verificato se fallisse già prima.
