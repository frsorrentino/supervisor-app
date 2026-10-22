# Lavori, «In corso» per chi deve muoversi — piano

Compito `lavori-gerarchia-22-10` (segnalazione di Franz 22/10 20:50, mockup della master approvato alle 21:16:
`docs/mockup/2026-10-22-in-corso/`). Contratto 1.79 (`short`, relay 0.8.45) copiato in `f17795b3`. Ramo `tab-emoji/22-10`
dopo la pre-release `tab-emoji-2026-10-22`, oppure un ramo `lavori/in-corso` da lì. Esito: pre-release
`lavori-gerarchia-2026-10-22` (non latest) installata sul telefono.

## Cosa cambia (dal mockup e dalle indicazioni della master)

1. Tre gruppi per chi deve muoversi, al posto di attivi / in pausa / fermi su altri:
   - **AL LAVORO**: impegni non in pausa che non aspettano Franz, più quelli in pausa che aspettano una sessione (in coda
     dietro un altro compito): riga sotto = sessione e ora dell'ok, o il motivo della pausa («in coda dopo X»).
   - **ASPETTA TE**: impegni con `waits.on = person` (in pausa o no) e gli approvati che aspettano un ok: riga sotto =
     cosa serve da Franz (il motivo della pausa, o «il tuo ok a: …» da `next`); a destra da quando («dalle 20:50» oggi,
     «dal 18/10» prima). Stessa sessione e stesso motivo = una riga sola con la pastiglia «N compiti» (Albo Chiaro: 5).
   - **FERMI SU ALTRI**: le righe dell'agenda con `blocks = terzi`: riga sotto = `ref` o la prima riga di `detail`;
     a destra la data di ritorno (`until`) se c'è.
2. Ogni riga: icona del gruppo in un cerchio (play, persona, clessidra), titolo breve su una riga (`short` del contratto
   1.79, o `Lavori.shortTitle(title)`: prefisso «nome: » tolto, parole intere entro 28 caratteri, senza articolo o
   parentesi aperta in coda), riga sotto su una riga, clip senza «…» (la riga sotto può andare a due righe sul
   telefono stretto: ruling, mai puntini).
3. Via la spiegazione in testa alla sezione e la legenda nella vista Elenco.
4. Nella testata della sezione il tasto **Elenco | Tempo** che ricorda la scelta (DataStore sul telefono, localStorage
   sul web). Tempo = il Gantt dei sette giorni: etichetta breve a sinistra (120 dp), asse una volta sola in testa,
   barre come oggi (piena = al lavoro, tratteggio = in pausa o in coda, riga bianca = adesso), legenda in una riga in
   fondo; le righe dell'agenda senza barra con «dall'agenda, senza data».
5. Stessa regola sul web (`Lavori.svelte`, `lavori.ts`).
6. Provini col caso reale di stasera: 2 al lavoro, 7 aspetta te (Albo Chiaro ×5 raggruppato, Lavori leggibile, App del
   03/10), 2 fermi su altri (Chrome Web Store, chrome-bridge #2); titoli lunghi.

## Task

1. **Regola** `Lavori.kt` / `lavori.ts`: `shortTitle(title)`; `InProgress(working, you: List<Group>, others, axis)` con
   `Group(rows, short, n, meta, since)`; raggruppamento per sessione + motivo; `since` = `paused.at` o `approved_at`
   o `last_step.at`. Test prima: `LavoriTest` e `lavori.test.ts` (caso reale a mano, con e senza `short`).
2. **Android** `LavoriScreen.kt`: testata con il segmented Elenco | Tempo (stato in `prefs`), le tre liste con `WorkRow`
   (cerchio con l'icona, titolo, riga sotto, a destra da quando o la pastiglia), vista Tempo con `AxisRow` una volta,
   `GanttRow` (etichetta 120 dp + barra) e la legenda; via `Expl` di In corso e `lavori_key`. Stringhe nuove
   (`lavori_working`, `lavori_waits_you`, `lavori_others`, `lavori_n_tasks`, `lavori_since_at`, `lavori_since_on`,
   `lavori_view_list`, `lavori_view_time`, `lavori_agenda_no_date`, `lavori_your_ok_to`). Provini `LavoriScreenTest`
   `lavoriProgressList` e `lavoriProgressTime` col caso reale; `lavoriFull`/`lavoriProgressAndDone` aggiornati.
3. **Web** `Lavori.svelte`: stessi gruppi e viste, `t.ts` (it/en), icone `play`, `user`, `hourglass`.
4. **Consegna**: core e web su win, golden in CI, pre-release `lavori-gerarchia-2026-10-22`, `supervisor task done`,
   resoconto `docs/verifiche/2026-10-22-in-corso.md`.

Fuori: il blocco Per te (gli ok restano lì con il tasto Approva: In corso li elenca sotto ASPETTA TE senza tasto), la
home, la pagina Notte.
