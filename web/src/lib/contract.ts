// Il contratto con il PC (contract/README.md), lo stesso JSON che legge l'app Android: qui solo i tipi e la lettura.
// I test leggono le fixture di ../contract, come quelli Kotlin.

export type SessionState = 'waiting' | 'busy' | 'idle' | 'awaiting' | 'gone'
/** Contratto 1.53: la riga che spiega l'opzione e l'opzione consigliata. */
export type Option = { n: number; label: string; description?: string | null; recommended?: boolean }
export type Question = { id: string; kind: 'permission' | 'ask' | 'plan'; text: string; options: Option[]; tier: 'low' | 'medium' | 'high'; asked_at: number
  /** Contratto 1.56: il testo senza segreti, solo quando diverso da `text`. */
  preview?: string | null }
/** Contratto 1.55: una domanda risolta al terminale, nello stato per 5 minuti. */
/** Contratto 1.66: question | turn_failed | quota_limit | task_red. */
export type Attention = { kind: string; session?: string | null; ref?: string | null; title: string; since: number }
export type Resolved = { id: string; text: string; at: number; by: string }
/** Contratto 1.60: `preview` = short e full senza segreti, solo quando il relay ha nascosto qualcosa. */
export type Outcome = { short: string; full: string; at: number; preview?: { short: string; full: string } | null }
export type Model = { id: string; label?: string | null }
export type Goal = { text: string; since: number; met?: boolean | null }

export type Session = {
  /** Contratto 1.54 e 1.55. */
  permission_mode?: string | null; resolved?: Resolved | null
  /** Contratto 1.62: attaccata e con un tasto premuto al PC negli ultimi 2 minuti. */
  at_pc?: boolean
  /** Contratto 1.68: `supervisor needs-you` e il turno finito con una voce bloccante, finché la sessione è ferma. */
  needs_you?: { reason: string; at: number } | null; turn_blocking?: { text: string; at: number } | null
  /** Contratto 1.69: la sessione può arrivare a bypassPermissions. */
  permission_bypass?: boolean
  /** Contratto 1.72: i conteggi delle modifiche della cartella; null fuori da git, senza modifiche o con git fermo. */
  changes?: ChangesCount | null
  id: string; name: string; account: string; project: string; state: SessionState; since: number
  turn_started?: number | null; tool?: string | null; link?: string; tool_note?: string | null
  attached?: boolean; followed?: boolean; question?: Question | null; outcome?: Outcome | null
  next?: string | null; next_at?: number | null; color?: string | null; icon?: string | null; account_kind?: string | null
  model?: Model | null; effort?: string | null; context?: number | null; low_priority?: string | null
  goal?: Goal | null; suggestion?: string | null
  /** Contratto 1.51: il comando slash dell'handoff; null = nessuno, assente = relay vecchio (fable-director). */
  handoff?: string | null
  /** Contratto 1.37: assenti nello stato quando non valorizzati; la lettura mette null / false / null. */
  advice?: Advice | null; finished?: boolean; duplicate_of?: string | null
  /** Contratto 1.38: i Prossimi dell'ultimo esito, col «!» tolto; `blocking` = sblocca un lavoro fermo. */
  next_steps?: NextStep[] | null
}

export type NextStep = { text: string; blocking: boolean }

/** Contratto 1.37: il consiglio di fable-director; `when` "now" o "next_task", `differs` = il puntino sul tasto. */
export type Advice = { model: string; effort: string; reason: string; switch_cost_tokens: number; at: number; source: string; when: 'now' | 'next_task'; differs: boolean }
/** Contratto 1.37: un compito che aspetta l'ok; `deploy` = esce in produzione. */
export type Approval = { task: string; title: string; what: string; where: string; deploy: boolean; requested_at: number }
/** Contratto 1.76: una voce della vista «Impegni» del registro; `waits` = chi aspetta chi (`person` = l'utente). */
export type Commitment = {
  task: string; title: string; project?: string | null; session?: string | null
  state: 'proposed' | 'assigned' | 'running' | 'awaiting_ok' | 'approved' | string; lane?: string | null; plan?: string | null
  approved: boolean; approved_at?: number | null; approved_by?: string | null
  age_s: number; last_step?: { at: number; kind: string; text: string } | null; idle_s: number
  next?: string | null; waits?: { who: string; on: string } | null
  expected?: string | null; outcome_kind?: string | null
  check?: { outcome: string; at: number } | null; paused?: { reason: string; at: number; by?: string | null } | null; stalled: boolean
}

export type QuotaAccount = { h5?: number | null; w7?: number | null; reset_w7?: number | null; reset_h5?: number | null; stale?: boolean; kind?: string | null }
export type Device = { uid: string; name: string; kind?: string | null; seen?: number | null }
export type Recurring = { id: string; label: string; prompt: string; param?: boolean }
/** Contratto 1.75: un host remoto confermato, dall'ultima lettura del poller; con `up` false i numeri sono null e `error` dice perché.
 *  `cpu` fra 0 e 1, RAM e disco in byte (il disco della radice delle sessioni), `sessions` = i nomi delle sessioni vive lì. */
export type HostStatus = {
  name: string; up: boolean; at: number | null; error: string | null; cpu: number | null
  ram_used: number | null; ram_total: number | null; disk_free: number | null; disk_total: number | null; sessions: string[]
}
/** Contratto 1.75: una pubblicazione di oggi, la più nuova prima; `url` null per un remoto non GitHub. */
export type Published = { at: number; kind: 'release' | 'push' | 'deploy' | string; project: string; text: string; url: string | null }

export type Project = { path: string; name: string; account: string; last_used?: number | null }
export type NightItem = { id: string; dir: string; name: string; prompt: string; added: number; started?: number | null }
/** `report` (contratto 1.44): l'ultimo rapporto della notte; assente senza rapporto o con un relay precedente. */
export type NightReportRef = { date: string; generated_at: number }
/** Contratto 1.49: `master` = `night_add` accetta la master (`NIGHT_TARGET`). */
export type Night = { queued: number; running?: string | null; items?: NightItem[] | null; report?: NightReportRef | null; master?: boolean }
/** Contratto 1.46: una riga dell'agenda, tutta testo libero; le righe nell'ordine del file, `more` = tagliate sopra i 60 KB. */
export type AgendaRow = {
  state: string; scope: string; blocks: string; title: string; ref: string
  /** Contratto 1.47: il dettaglio con gli a capo (null = non scritto) e `AAAA-MM-GG` fino a cui è sospesa. */
  detail?: string | null; until?: string | null
  /** Contratto 1.47: la chiave della riga, scelta dal relay; cambia a ogni modifica. */
  key?: string
}
/** `owner` (contratto 1.47): il valore di `blocks` che indica l'utente. */
export type AgendaPage = { rows: AgendaRow[]; more: boolean; owner?: string | null }
export type RecapItem = { project: string; done: string; next?: string | null }
export type Recap = { date: string; items: RecapItem[] }
export type EventKind = 'question' | 'answered' | 'outcome' | 'gone' | 'launched' | 'quota' | 'resumed' | 'recap' | 'night_report' | 'restart_failed' | 'relay_stale' | 'task_check' | 'quota_limit' | 'turn_failed' | 'week_unused' | 'week_pace' | 'needs_you' | 'turn_blocking' | 'attention_reminder' | 'task_stalled'
export type Event = { key: string; kind: EventKind; session?: string | null; account?: string | null; ts: number; title: string; body?: string; ref?: string | null
  /** Contratto 1.56: titolo e corpo senza segreti. */
  preview?: { title: string; body?: string } | null
  /** Contratto 1.58, `quota_limit`. */
  reset_at?: number | null; window?: string | null; other_account?: string | null; actions?: string[] | null
  /** Contratto 1.61 (`turn_failed`) e 1.62 (`quiet`: senza suono, Franz è al PC). */
  reason?: string | null; continue_text?: string | null; quiet?: boolean
  /** Contratto 1.68, `attention_reminder`: il kind della voce che aspetta ancora. */
  attention_kind?: string | null
  /** Relay 0.8.30: il `turn_failed` chiuso, riscritto senza `continue_text` e con l'ora della chiusura. */
  settled_at?: number | null }

// Trascrizione (contratto 1.22): `in` comprende la cache; `queued` = scritta a turno in corso e non ancora presa; `cut` = accorciata dal PC.
export type TranscriptTurn = { started?: number | null; ended?: number | null; in?: number | null; out?: number | null }
export type TranscriptFile = { path: string; mime?: string | null; size?: number | null }
export type TranscriptEntry = {
  id: string; role: string; text?: string | null; at?: number | null; tool?: string | null; note?: string | null
  error?: boolean | null; cut?: boolean; turn?: TranscriptTurn | null; files?: TranscriptFile[] | null
  origin?: string | null; queued?: boolean
  /** Contratto 1.72: il diff del passaggio, sulle voci tool di Edit, MultiEdit, Write e NotebookEdit. */
  diff?: StepDiff | null
  /** Contratto 1.78: sui `task_check` l'esito (true = verde), perché il titolo non porta più ✓ o ✗; null altrove e con un relay precedente. */
  ok?: boolean | null
}
export type TranscriptPage = { entries: TranscriptEntry[]; more?: boolean }
// Contratto 1.72, il diff dal telefono: un pezzo come in `git diff -U3`, le righe che cominciano per «-», «+» o spazio.
export type Hunk = { old_start: number; old_lines: number; new_start: number; new_lines: number; lines: string[] }
/** Il diff di un passaggio di modifica; `kind` edit | create | overwrite; `cut` = oltre 200 righe, il resto con l'op `diff`. */
export type StepDiff = { path: string; kind: string; added: number; removed: number; hunks: Hunk[]; cut: boolean }
/** I conteggi delle modifiche della cartella, per la pillola della testata. */
export type ChangesCount = { files: number; added: number; removed: number; at: number }
/** Un file della vista «Modifiche»: `path` relativo alla radice, `status` modified | added | deleted | renamed | binary. */
export type ChangedFile = { path: string; status: string; added: number; removed: number; size: number | null; hunks: Hunk[]; cut: boolean }
/** Il risultato dell'op `changes`: `source` git | transcript, `base` null in un repo senza commit, `cut` oltre 50 file o 2000 righe. */
export type ChangesView = { source: string; repo: string | null; base: string | null; files: ChangedFile[]; totals: { files: number; added: number; removed: number }; cut: boolean; at: number }

/** Il `text` dell'op `changes`, coi valori di default dove mancano. */
export function decodeChanges(raw: string): ChangesView {
  const v = JSON.parse(raw)
  if (typeof v?.source !== 'string' || !Array.isArray(v?.files)) throw new Error('not a changes view')
  return {
    source: v.source, repo: v.repo ?? null, base: v.base ?? null, cut: !!v.cut, at: v.at ?? 0,
    totals: { files: v.totals?.files ?? 0, added: v.totals?.added ?? 0, removed: v.totals?.removed ?? 0 },
    files: v.files.map((f: Partial<ChangedFile> & { path: string; status: string }) => ({
      path: f.path, status: f.status, added: f.added ?? 0, removed: f.removed ?? 0, size: f.size ?? null, hunks: f.hunks ?? [], cut: !!f.cut,
    })),
  }
}

/** Il `text` dell'op `diff`: un passaggio intero, nella forma di `diff`. */
export function decodeDiff(raw: string): StepDiff {
  const d = JSON.parse(raw)
  if (typeof d?.path !== 'string') throw new Error('not a diff')
  return { path: d.path, kind: d.kind ?? 'edit', added: d.added ?? 0, removed: d.removed ?? 0, hunks: d.hunks ?? [], cut: !!d.cut }
}
/** Contratto 1.59: `/tail/<id>`, le ultime voci di una sessione viva, riscritte dal relay al massimo ogni 10 s. */
export type Tail = { session: string; id: string; tx?: string; at: number; entries: TranscriptEntry[]; more?: boolean }
/** La chiave del nodo: l'id della sessione con . # $ [ ] / cambiati in _. */
export const tailKey = (sessionId: string) => sessionId.replace(/[.#$[\]/]/g, '_')
/** Le voci del tail sulla copia: agganciate in coda se la prima c'è già, altrimenti al posto della copia troppo vecchia. */
export function withTail(old: TranscriptEntry[], tail: Tail): TranscriptEntry[] {
  const first = tail.entries[0]?.id
  if (!first) return old
  if (!old.some(e => e.id === first)) return tail.entries
  const byId = new Map(tail.entries.map(e => [e.id, e]))
  const seen = new Set(old.map(e => e.id))
  return [...old.map(e => byId.get(e.id) ?? e), ...tail.entries.filter(e => !seen.has(e.id))]
}

// Ricerca (contratto 1.27): `match` = [inizio, fine] dentro `snippet`.
export type SearchHit = { session: string; live: boolean; project?: string | null; entry?: string | null; role?: string | null; at?: number | null; snippet: string; match?: number[] }
export type SearchPage = { hits?: SearchHit[]; more?: boolean }

/** Contratto 1.29: la cronologia di oggi; `live` distingue la stessa cartella viva e chiusa. */
export type TimelineEvent = { at: number; kind: string; text: string; ok?: boolean | null; ref?: string | null }
export type TimelineSession = { session: string; live: boolean; project?: string | null; events: TimelineEvent[] }
export type TimelinePage = { since: number; sessions: TimelineSession[]; more?: boolean }

export type State = {
  v: number; ts: number; host: string
  /** Contratto 1.64: le cartelle madri per un progetto nuovo. */
  project_parents?: string[] | null
  /** Contratto 1.66: quello che serve a Franz, dalla voce più vecchia; assente quando non c'è niente. */
  attention?: Attention[] | null
  /** 1.43: epoch s della pubblicazione (decimali ammessi); assente con un relay precedente. */
  published_at?: number | null
  sessions: Session[]; quota: Record<string, QuotaAccount>
  projects: Project[]; night: Night; recap: Recap
  ops?: string[] | null; slash?: string[] | null
  devices?: Device[] | null; recurring?: Recurring[] | null
  choices?: { models: Model[]; efforts: string[] } | null
  /** Contratto 1.48: `parts` = il relay accetta il file a pezzi fino a `max_parts_bytes`. */
  /** Contratto 1.50: `multi` = quanti allegati in un `report` solo, id separati da virgola. */
  share?: { max_bytes: number; any?: boolean; parts?: boolean; max_parts_bytes?: number; multi?: number } | null
  /** Fase 4 del piano web: la chiave pubblica VAPID del relay (base64url); assente = il relay non manda web push. */
  push?: { vapid: string } | null
  approvals?: Approval[]
  /** Contratto 1.76: null o assente con un relay precedente, [] senza impegni. */
  commitments?: Commitment[] | null
  /** Contratto 1.75: gli host remoti e le pubblicazioni di oggi; [] con un relay precedente. Firebase ne porta al massimo 5. */
  hosts_status: HostStatus[]; published_today: Published[]
}

export type CmdOp = 'answer' | 'prompt' | 'launch' | 'follow' | 'unfollow' | 'resume' | 'screen' | 'allow_all' | 'last' | 'reopen'
  | 'model' | 'effort' | 'night_add' | 'night_remove' | 'report' | 'interrupt' | 'transcript' | 'file' | 'slash' | 'projects'
  | 'search' | 'timeline' | 'pair_add' | 'approve' | 'decision' | 'unpair' | 'night' | 'agenda' | 'agenda_set' | 'quota_route' | 'project_new'
  /** Contratto 1.69: arg = default | acceptEdits | plan | auto | bypassPermissions. */
  | 'permission_mode'
  /** Contratto 1.72: la vista «Modifiche» (arg null o file=<percorso>) e il passaggio intero (arg = id della voce). */
  | 'changes' | 'diff'
export type Cmd = {
  id: string; op: CmdOp; session?: string | null; arg?: string | null; issued: number; by: string; text?: string; device?: string; parts?: boolean
  /** Contratto 1.52: `answer` e `allow_all`, l'id della domanda a cui si risponde. */
  question?: string
  /** Contratto 1.63: `prompt` da consegnare a quest'ora (epoch s). Contratto 1.64: `project_new`, conferma e primo messaggio. */
  at?: number; confirm?: boolean; prompt?: string
  /** Contratto 1.65: a una sessione chiusa, che il relay riapre e poi le consegna. */
  reopen?: boolean
  /** Contratto 1.47, `agenda_set`: done | snooze | remove | pass, il giorno di uno snooze, a chi passa. */
  action?: string; until?: string; blocks?: string
}
export type CmdResult = { id: string; ok: boolean; text: string; at: number; session?: string | null; job?: string | null }

/** Lo stato come arriva: i campi che mancano prendono i valori di default del contratto. */
export function decodeState(raw: string): State {
  const s = JSON.parse(raw)
  if (typeof s?.v !== 'number' || typeof s?.ts !== 'number' || typeof s?.host !== 'string') throw new Error('not a state')
  const sessions = (s.sessions ?? []).map((x: Session) => ({ ...x, advice: x.advice ?? null, finished: x.finished ?? false, duplicate_of: x.duplicate_of ?? null }))
  return { ...s, sessions, approvals: s.approvals ?? [], quota: s.quota ?? {}, projects: s.projects ?? [], hosts_status: s.hosts_status ?? [], published_today: s.published_today ?? [], night: { queued: 0, ...s.night }, recap: { date: '', items: [], ...s.recap } }
}

/** Lo stato prima del primo /state del relay: vuoto, così i dati di prova non si mescolano mai a quelli veri. */
export const blankState = (ts: number): State => decodeState(JSON.stringify({ v: 1, ts, host: '' }))
