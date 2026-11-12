# Handoff 12/11/2026 ore 16:10, win:supervisor-app (ramo tab-emoji/22-10)

Scritto perché il limite d'uso è arrivato mentre cominciavo la scheda di scelta. Sostituisce `handoff-2026-11-12.md` per l'ordine dei lavori.

## 1. Fatto in questa sessione
- Segnalazione di Franz delle 15:11 («la X per chiudere il menu della scelta delle sessioni va nel posto sbagliato»): era l'app Android, non la web. Commit `b442d8fd`, versione 0.2.2120. La × del Genie ora si posa sulla ▾ del nome per «Vai a» e sul punto toccato per gli altri menu (`Genie.kt`, `AppMenuPanel.kt`, `AppShell.kt`, test `GenieCloseTest`).
- Verifiche: GenieCloseTest rosso prima e verde dopo; `:mobile:testGithubDebugUnitTest` in locale 531 test, 0 falliti. Manca la prova di Franz sul telefono. La CI (run 43240964705) era in corso alle 16:10: controllare che la pre-release 0.2.2120 ci sia.
- Alla master ho già scritto: skill e plugin visti dopo il riavvio, «task done» di anteprime e riquadro.

## 2. Da fare, nell'ordine della master
1. `app-scheda-scelta-12-11`, davanti a tutto. Nessun codice scritto ancora.
2. `app-monitor-link-12-11`, col contratto 1.138.
3. Giro F della chat e variante D di `app-mockup-sposta-master-12-11`; poi `app-ios-scatti-tastiera-12-11` e `app-formato-unico-12-11`.

## 3. Scheda di scelta: la descrizione approvata da Franz alle 15:10
1. In chat, al posto del link `#/attesa/<id>` (o del suo id), una scheda compatta: titolo, una riga su cosa decidere, miniature delle opzioni, stato («da scegliere», «scelto B»); un tocco la apre.
2. Aperta: descrizione vera della scelta, opzioni una accanto all'altra, ognuna con immagine, una riga di pro e contro e il suo tasto; le immagini si ingrandiscono al tocco; in fondo «Nessuna, rifare» col motivo.
3. Fissa in «Per te» della sessione e in Home finché Franz non risponde; poi in archivio con ciò che ha scelto.
4. Un giro nuovo entra nella stessa scheda; i precedenti restano col motivo per cui sono stati scartati.
5. La stessa scheda per le verifiche, con le immagini di cosa guardare.
- Contiene `app-attesa-immagini-12-11`: le immagini nella scheda sono il primo passo e si possono pubblicare da sole.
- Telefono, tablet e web, giorno e notte; test; pre-release e web dalla CI; scrivere alla master cosa guardare; commit con l'id.
- Skill di design da usare: impeccable (già caricata: niente PRODUCT.md né DESIGN.md, si lavora sui token di `web/src/v3.css`; alla fine `impeccable detect --json <file cambiati>`), frontend-design, emil-design-eng, apple-design, mobile-native.

## 4. Contratto 1.137 (dalla master, 15:17; supervisor 0.8.152, additivo, `v` resta 1)
- Ogni voce di `attese` e `attesa`: `description` (testo o null); `options[]` come oggi più `description` (la riga di pro e contro) e `files`; `files` `[{path, name, mime, size, kind: image|document|other, session}]` o null (`session` null = file dichiarato, servito solo finché la richiesta è aperta); `card_id` (id del primo giro, stabile); `state` («open» nelle aperte); `answered` (null nelle aperte).
- `actions` di una scelta: dopo le opzioni `{label «Nessuna, rifare», style secondary, cmd {op: approval_redo, arg: <task>, session: null}, text_field: "text"}`; resta «Chiudi senza ok».
- Scaricare un file: op `file` con `arg` = `files[].path` esatto e `session` = `files[].session`.
- Op `attesa`: `msg.id` può essere una voce SendUserFile; senza file dichiarati e con file nel messaggio d'origine, `files` = quei file con `session` = `msg.session`; `rounds` = i giri precedenti dal più vecchio `[{id, at, text, description, options, files, state: redo|closed|replaced, reason, by, answered_at}]`, `[]` al primo giro.
- Archivio: con `archive: true` nel comando, l'id di qualsiasi giro torna la scheda anche dopo la risposta: `state` open|chosen|approved|redo|closed, `answered {by, at, choice, reason}` o null, `actions` `[]` se non è aperta. Senza `archive` come prima.
- Op nuova `approval_redo`: `arg` = il compito, `text` = il motivo, obbligatorio, fino a 300 caratteri. Errori: compito sconosciuto, motivo vuoto; un compito che non aspetta più risponde ok con `closed: false`.
- Fixture `cmd-result-sample.json` …1300, …1301 (due opzioni coi file e `rounds`), …1370 (file di un allegato), …1380-1382 (`approval_redo`), …1390 (archivio): la master la manda appena la sessione supervisor dice dove sta.

## 5. Contratto 1.138 (dalla master, 15:54; supervisor 0.8.153)
- `monitors`/`monitor`: ogni scheda ha `url`; `series[].url`; `hits[].url`; `rows[].url` nelle tabelle. `state.suggestions[]` di kind monitor: `url` accanto a `link`. Solo http e https. Fixture …1310/…1311.
- Da fare: riga della serie e avviso «Da guardare» si toccano e aprono la pagina nel browser quando il link c'è; senza link niente cambia.

## 6. Variante D di sposta-master: precisazioni di Franz, 15:50-15:51
- È lo stesso cerchio che oggi apre e chiude la master: centro = apri/chiudi, bordo = sposta.
- Le frecce stanno dentro il cerchio, rivolte in fuori; quelle possibili da quella posizione accese, le altre spente ma al loro posto. Anche diagonali.
- Mouse sopra una freccia: si accende di più, l'app si scurisce, la zona d'arrivo si accende in giallo; il clic sposta.
- Tocco: tocco corto apre e chiude come oggi; pressione lunga entra nello spostamento, si trascina verso una direzione con la zona gialla, si rilascia per confermare.
- Dove le posizioni sono solo due (Home del telefono, vista stretta, Home ridotta a riga) il tasto resta quello di oggi, solo freccia su o giù.
- La master vuole vedere: il cerchio che si allarga al passaggio o alla pressione; frecce da tastiera ed Esc per annullare. Pagina di prova con tutti e due i modi.

## 7. Cosa ho già letto per la scheda (web)
- Scheda aperta di oggi: `TiAspettaView.svelte` (carta `.wc`, tasti da `actions`, `go()` manda `cmd` e gestisce `text_field`), `TiAspetta.svelte`, `tiAspetta.ts`; tipi in `contract.ts` (`WaitItem`, `WaitOption`, `WaitAction`), decodifica in `decideDecode.ts`.
- File: `App.svelte` 1772-1861 (`loadFile`, `getFile`, `preview`, `fileAction`), cache in `mediaCache.svelte.ts` (`media`, `coming`, `missed`, `failed`), chiave `fileKey` in `media.ts` (`sessione\npercorso…`); `FileStore.keep` butta i file delle sessioni non a schermo.
- Chat: `Feed.svelte` snippet `claude` e `Md.svelte` (i link nascono lì); `App.svelte` intercetta `#/attesa/<id>`.
- Demo: `waitsDemo` in `demo.ts` 503; `demoTransport.ts` risponde ad `attesa`, `attese`, `file` (`DEMO_FILES`).
- `approval_redo` va aggiunta a `CmdOp`; il motivo è obbligatorio (oggi un campo `text` può restare vuoto).

## 8. Direzione di design pensata, non ancora costruita
- Le immagini sono il contenuto: grandi, intere su un fondo neutro di proporzione fissa, con la lettera dell'opzione.
- Telefono: immagini affiancate in una striscia, sotto le opzioni a righe intere (nome, riga di pro e contro, tasto), così il testo non va in colonne strette. Da 600 px: colonne complete affiancate.
- Niente etichetta sopra il titolo, niente pillole in serie né pallini; un solo tasto pieno; stato a parole.

## 9. Ambiente di questo PC
- Niente `gh`, niente telefono né emulatore: la CI si legge con l'API pubblica di GitHub via curl.
- Il ramo locale non ha upstream: `git pull --rebase origin tab-emoji/22-10` e `git push origin tab-emoji/22-10`.
- Paparazzi: `localToScreen` di Compose dà infinito; usare `positionInRoot` e `view.getLocationOnScreen`.
