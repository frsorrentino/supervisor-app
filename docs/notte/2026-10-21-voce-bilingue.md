# Notte 21/10/2026 — voce bilingue

Ramo `notte/voce-bilingue` da `feature/tablet` (17f20cdc), pushato su origin. Richiesta: docs/proposte/2026-10-21-voce-bilingue.md.

## Esito

Fatto e verde in CI, non provato dal vivo. In `core` il testo da leggere si divide per lingua; telefono (▶ e live) e
orologio leggono le frasi e le citazioni inglesi con la voce inglese e i termini tecnici con la voce scelta.
Impostazione accesa di default su tutti e due. Ramo `notte/voce-bilingue`, ultimo commit dei golden, CI 39523548554
verde con APK e AAB.

## Cosa fa

- **Segmentazione in `core`** (`rules/SpeechLang.kt`, JVM): il testo da leggere si divide in pezzi `(testo, lingua)`.
  Si decide per frase (taglio solo a `.!?…` e a capo, così «First closed test of Supervisor: see which…» resta una
  frase sola) e, dentro la frase, per le parti tra «», “”, "" o parentesi. Una lingua prende uno spezzone solo se ha
  almeno 3 parole e almeno 2 parole funzionali a favore (articoli, preposizioni, ausiliari: liste corte, solo parole che
  esistono in una lingua sola). Le parole accentate contano come italiane; elisioni («l'app») e contrazioni («don't»)
  contano per la loro lingua; nomi di file, rami e domini («my-example-it», «feature/tablet») non contano.
- **Lettura** — telefono (`Speech.kt`, tasto ▶), modalità live (`LiveVoice.kt`) e orologio (`Speaker.kt`): ogni pezzo va
  in coda con la sua lingua; `setLanguage` si chiama solo quando la lingua cambia, perché il motore prende lingua e voce
  al momento di `speak`; alla fine si torna alla voce scelta. Gli id delle utterance non cambiano formato (evidenziazione,
  mini-controller, stop, pausa e velocità come prima). Se il motore non ha la voce inglese (`isLanguageAvailable` sotto
  `LANG_AVAILABLE`) tutto si legge con la voce di base, senza errori. Con l'app in inglese è il contrario: le frasi
  italiane vanno alla voce italiana. Lingue diverse da it/en: come prima.
- **Impostazione** accesa di default: telefono «Parti in inglese» (sottotitolo «Lette con la voce inglese; i termini
  tecnici restano nella voce scelta»), orologio «Inglese con voce inglese» in Lettura vocale (`Settings.ttsBilingual`).
  Il primo titolo del telefono, «Leggi le parti in inglese con la voce inglese», si tagliava sulla riga: accorciato.

## Scelte e perché

- Classificatore a parole funzionali, non ML Kit Language Identification: ML Kit aggiunge un modello di qualche MB per
  ogni APK (telefono e orologio, misura non fatta) e le regole su spezzoni e termini tecnici servirebbero comunque; il
  classificatore gira in JVM ed è provato con test. ML Kit resta la seconda strada se dal vivo le frasi inglesi sfuggono.
- La prima versione decideva anche dalle desinenze (parole in vocale = italiano). Provata su 2864 frasi degli handoff del
  repo: frasi di soli nomi tecnici («26 test TypeScript + 9 Python verdi») passavano all'inglese. Tolta: oggi su 2237
  frasi (codice inline tolto) cambiano lingua 33 pezzi: intestazioni inglesi («Decisions and why»), citazioni
  inglesi e una riga fatta di soli identificativi.

## Prove

- `SpeechLangTest`: 14 test (frasi reali dagli handoff e dalle note di versione: tecniche restano italiane, frasi e
  citazioni inglesi passano, sotto le 3 parole niente cambio, app in inglese, pezzi sotto il limite).
- `core` su win: 1160 test, falliscono solo i 5 `ChatLogTest` (noti, solo Windows). Telefono e orologio compilano.
- CI: record dei golden 39520933486 e 39522606410, build 39523548554 verde (unit test, Paparazzi verify, APK).
- Golden cambiati e controllati a vista: Impostazioni del telefono (`settingsDemo`, `settingsWatchPending`: la riga
  nuova) e dell'orologio (`settings`).

## Fuori richiesta, necessario

`feature/tablet` aveva la CI rossa dal 18/10 (run 39166500414): le fixture del relay 1.73 contengono l'op `studio` che
`CmdOp` non conosceva, 17 test di `core` fallivano. Commit `c496ba5c`: `CmdOp.STUDIO` (l'app non la manda, la demo la
rifiuta) e i conteggi di `ContractTest`. Senza, nessun ramo da `feature/tablet` passa la CI.

## Non provato

Dal vivo: la voce inglese sul Pixel e sull'orologio (serve Franz con il telefono). Da provare: una risposta della master
con una frase inglese e un termine tecnico italiano; la voce inglese disinstallata (deve leggere tutto in italiano).

## Prossimi

Merge su `feature/tablet` e rilascio con il processo abituale (la correzione `studio` è anche in
`notte/play-pixelfarm` come cherry-pick: stesso contenuto, il secondo merge la trova già fatta); APK della CI nell'artifact `cmwatch-apk` (un giorno).

FINE
