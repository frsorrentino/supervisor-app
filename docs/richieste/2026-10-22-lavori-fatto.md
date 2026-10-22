# Richiesta al relay (contratto 1.77): il blocco «Fatto» della pagina Lavori e le pause che aspettano la persona

Da supervisor-app, sul mockup della pagina unica «Lavori» approvato da Franz il 22/10 alle 12:43 (`docs/mockup/2026-10-22-lavori/`, nota «Cosa cambia nei dati» nel README). La pagina fonde Registro, Recap e Impegni e ha quattro blocchi: Per te, In corso, Stanotte, Fatto. Tre blocchi stanno in piedi con il contratto 1.76; qui le due cose che mancano. I nomi sono una proposta, la forma la decide la sessione supervisor. Una richiesta per volta, come da regola: questa è l'unica aperta.

## 1. Op `done`: i compiti chiusi per giorno, con la prova

A richiesta come `agenda` e `studio`, mai nello stato. `arg` = giorno ISO (`AAAA-MM-GG`), null = oggi. Risponde `text` = JSON:

```json
{"date": "2026-10-22",
 "rows": [{"task": "impegni-app-22-10", "title": "App: Impegni, Approva e aggiornamento dentro l'app", "project": "personali/supervisor-app",
           "session": "supervisor-app", "at": 1792647923, "outcome": "green",
           "proof": {"kind": "release", "value": "impegni-2026-10-22-b", "url": "https://github.com/frsorrentino/supervisor-app/releases/tag/impegni-2026-10-22-b"}}],
 "days": [{"date": "2026-10-21", "n": 6}, {"date": "2026-10-20", "n": 4}],
 "more": false}
```

- `rows`: i compiti chiusi quel giorno (`task done` verde, `auto-close`, `cancel`), dal più recente. `outcome` = `green` | `cancelled` | `failed`. `at` = epoch s della chiusura. `title` come nel registro, `project` nella forma di `sessions[].project`, `session` = la sessione che l'ha chiuso o null.
- `proof`: la prova del risultato, una sola, scelta dal relay in quest'ordine: la release o il tag se il controllo ne cita uno; il commit se il controllo o l'esito ne citano uno (`kind: commit`, `value` = hash corto, `url` se il remoto è noto); il file se l'esito cita un resoconto (`kind: file`, `value` = percorso relativo al progetto); altrimenti `kind: check`, `value` = la prima riga del comando di controllo. `url` null quando non c'è.
- `days`: i sei giorni prima di `date` che hanno almeno un compito chiuso, con il conteggio: l'app li mostra richiusi e li apre con un'altra chiamata.
- `more`: true se le righe superano 60 KB (tagliate dal fondo).
- Senza registro: `ok: false`, «no task registry». `done` entra in `ops`, in fondo dopo `studio`.

Finché l'op non c'è, l'app ricava «Fatto oggi» dagli eventi `task_check` verdi (1.57), senza prova e senza i giorni prima.

## 2. Una pausa che aspetta la persona

Oggi la pausa di Albo Chiaro dice «aspetta da Franz la casella Aruba» solo nel testo del motivo. L'app deve metterla in «Per te» con il tasto Fatto, e non può leggerlo dal testo. Chiesto:

- `task pause ID --reason R --waits person` (o un sinonimo che decide supervisor): nello stato la voce ha `waits = {"who": "<sessione>", "on": "person"}` finché è in pausa, e `paused.at` = l'inizio vero della pausa quando è noto (oggi Albo ha `at` del 22/10 alle 00:36 per una pausa dal 18/10).
- `task resume ID` toglie la pausa e riporta `waits` a chi tocca. L'app, su Fatto, manda alla master un `prompt` «Fatto: <motivo>. Riprendi <task>» (nessuna op nuova).
- Un compito in pausa che aspetta la persona non va in allarme (`stalled` resta false) e non entra in `task_stalled`, come oggi.

## Fixture

- In `cmd-result-sample.json`: un `done` riuscito con una riga per ogni `kind` di prova (release, commit, file, check) e `days` con due giorni; un `done` senza registro.
- `done` in fondo a `ops` nei tre `state-*.json`; in `state-3-stale.json` la voce in pausa di `orbit-docs-site` con `waits.on = "person"`.
- `v` resta 1. Secondo la regola di questo repo le fixture le aggiorna questa sessione dal ramo del relay, poi il relay le copia; se la sessione supervisor le scrive prima, le copio byte per byte come per la 1.76.
