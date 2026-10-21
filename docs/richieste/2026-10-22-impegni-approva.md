# Richiesta al relay (contratto 1.76): Impegni sempre visibili e «Approva» che risveglia

Da supervisor-app, sulle proposte P2 e P4 di `2026-10-21-compiti-sempre-visibili.md`, approvate da Franz il 21/10 alle 23:58 («ok per le proposte, prosegui con implementazione»; registro `impegni-app-22-10`, approvazione 393) e sul suo «procedi in autonomia» del 22/10 alle 00:53. La master ha chiesto all'app una sezione fissa «Impegni» in home sopra le sessioni (età, responsabile, prossimo passo, chi aspetta chi) e il tasto Approva che sblocca subito la sessione che aspetta. Qui quello che all'app serve dal relay; i nomi sono una proposta, la forma la decide la sessione supervisor.

## Cosa serve dal relay

1. **`state.commitments[]`**, la vista «Impegni» del registro: tutto quello che è approvato e non compiuto, dalla voce più vecchia. Ogni voce:
   - `id` (quello del registro, es. `impegni-app-22-10`), `title` (≤ 120 caratteri, con le parole dell'ok), `expected` (l'esito atteso di P1, se c'è);
   - `approved_at` (epoch s), `approved_by` («Franz», «Pixel 11 Pro XL»), `approval_n` (il numero nel registro);
   - `session` (chi lo lavora, o null), `owner` («supervisor-app», «master», «Franz»): il responsabile del prossimo passo;
   - `next_step` (una riga: cosa manca), `next_owner` (chi lo deve fare);
   - `waiting_on` (null, o `{who, what, since}`: «Franz», «ok al push», epoch): chi aspetta chi;
   - `state` ∈ `approved` | `running` | `awaiting_ok` | `paused` | `blocked`; `paused_reason` (null o il motivo, «salvo altre priorità» esplicito);
   - `last_step_at` (epoch dell'ultimo passo registrato): l'età dell'attesa la calcola l'app da qui e da `approved_at`;
   - `release` (null o `{tag, published_at}`) e `installed_at` (null o epoch): per gli impegni dell'app, la pre-release e l'installazione sul telefono, così la home sa dire «APK non installato».
   Una voce esce solo quando il controllo dell'esito è verde o quando Franz la sposta o la cancella (P2). `state.commitments` assente = relay vecchio: l'app non mostra la sezione.
2. **Eventi** (in `events`, `kind` nuovi): `commitment_added` (una voce nuova approvata), `commitment_step` (un passo registrato, `body` = il passo), `commitment_done`, `commitment_stale` (P3: senza passi da più di `stale_after` ore, default 2, `body` = il prossimo passo e chi lo deve fare); `ref` = l'`id`. L'avviso forte dell'app per `commitment_stale`, come per `needs_you`.
3. **«Approva» che risveglia (P4).** L'op `approve` di oggi (1.37) resta; in più:
   - il relay, registrato l'ok, manda subito il via alla sessione che aspetta (`talk` o canale nativo) con il testo dell'ok e la nota del telefono, e toglie all'istante la voce di quella sessione da `state.attention` (`needs_you` / `turn_blocking`) senza aspettare il prossimo prompt;
   - avvisa la master con un evento `approved` (`ref` = il compito, `body` = chi, quando, la nota);
   - `cmd-result` di `approve` dice cosa ha fatto: `woke` (true/false), `session`, `commitment` (l'`id` dell'impegno collegato, se c'è);
   - un ok dato a un compito copre i rami che contengono lo stesso lavoro: il registro lo **propone** in `approvals[].covers[]` (gli `id` dei compiti vicini) e l'app lo mostra come «vale anche per …», senza inventare.
4. **Op nuove**, solo per l'app: `commitment_pause` (`arg` = `{id, reason}`), `commitment_resume` (`arg` = `{id}`), `commitment_step` (`arg` = `{id, text}`: un passo scritto dal telefono, es. «installato»). Niente cancellazione dal telefono: quella resta a Franz nel registro.

## Contratto

- Versione 1.76, solo aggiunte, `v` resta 1. Fixture: in `state-1-question.json` due impegni (uno `awaiting_ok` con `waiting_on` su Franz da 9 ore, uno `running` con `release` e `installed_at` null); in `events-sample.json` un evento per `kind` nuovo; in `cmd-result-sample.json` un `approve` con `woke: true` e un `commitment_pause`.
- Quando arriverà l'ospite SDK (piano `docs/proposte/2026-10-22-sdk-piano.md`, fase 6): ogni voce in `awaiting_ok` porterà anche `tool_use_id` e `session_id`, e `approve` risolverà direttamente la richiesta di permesso in sospeso. Non serve ora.

## Cosa fa l'app

Sezione fissa «Impegni» in cima alla home (telefono, tablet, web), sopra le sessioni: una riga per impegno con titolo, «approvato alle 12:01 · fermo da 9 h», responsabile e prossimo passo, «aspetta Franz: ok al push» quando c'è; «Approva» sulla riga quando `waiting_on.who` è Franz e c'è un'`approvals[]` collegata; «Pausa» con motivo e «Riprendi»; l'età in ambra oltre le 2 ore, in rosso oltre le 8. Test come per il resto: regole in `core` e porta web con le stesse fixture, screenshot test della home.
