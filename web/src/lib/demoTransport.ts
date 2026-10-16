import changesRaw from '../../../contract/changes-sample.json?raw'
import { decodeChanges, type ChangesView, type Cmd, type CmdResult, type Event, type Session, type State, type TranscriptEntry } from './contract'
import type { FileBlob, Transport } from './transport'
import { demoSearch, world, type World } from './demo'
import { NIGHT_TARGET } from './recapAgenda'

// La demo che risponde, porta di FakeTransport.kt (Franz, 15/10 20:36: «la demo deve rispecchiare al 100% l'app»): i
// comandi cambiano lo stato come li cambierebbe il PC, con i testi del relay, di successo e di rifiuto. Niente rete.
const MODES = new Set(['default', 'acceptEdits', 'plan', 'auto', 'bypassPermissions'])
const VALID_NAME = /^[A-Za-z0-9._-]{1,64}$/
const DEMO_FILES: Record<string, string> = { 'checkout-before.png': 'image/png', 'checkout-tests.pdf': 'application/pdf', 'edge-caching.md': 'text/markdown', 'bench-data.zip': 'application/zip' }

export class DemoTransport implements Transport {
  readonly world: World
  private st: State
  private listeners = new Set<(s: State) => void>()
  private shared = new Set<string>()
  private results = new Map<string, CmdResult>()
  private agendaSeq = 7

  constructor(private readonly clock: () => number = () => Math.floor(Date.now() / 1000)) {
    this.world = world(clock())
    this.st = this.world.state
  }

  get state(): State { return this.st }
  private now() { return this.clock() }
  private set(s: State) { this.st = s; for (const f of this.listeners) f(s) }
  private replace(x: Session) { this.set({ ...this.st, ts: this.now(), sessions: this.st.sessions.map(s => (s.id === x.id ? x : s)) }) }

  async fetchState(): Promise<State> { return this.st }
  subscribe(onState: (s: State) => void): () => void {
    this.listeners.add(onState)
    queueMicrotask(() => onState(this.st))
    // Come FakeTransport.TICK: senza lavoro lo stato rinfresca solo l'ora, così non risulta mai vecchio.
    const tick = setInterval(() => this.set({ ...this.st, ts: this.now(), published_at: this.now() }), 30_000)
    return () => { this.listeners.delete(onState); clearInterval(tick) }
  }
  async fetchEvents(since = 0): Promise<Event[]> { return this.world.events.filter(e => e.ts > since) }
  async share(id: string, _mime: string, data: Uint8Array): Promise<void> {
    const max = this.st.share?.parts ? this.st.share.max_parts_bytes ?? 0 : this.st.share?.max_bytes ?? 0
    if (max && data.length > max) throw new Error(`file too large: ${data.length} bytes, at most ${max}`)
    this.shared.add(id)
  }
  // I file della demo (Franz, 15/10 22:20): veri, serviti dal sito in /demo-files, così anteprime, apri, scarica e condividi
  // funzionano come col PC. `file` li prepara, `/api/file/<id>` li dà.
  private files = new Map<string, string>()
  async fetchFile(id: string): Promise<FileBlob | null> {
    const name = this.files.get(id)
    if (!name) return null
    const r = await fetch(`/demo-files/${name}`).catch(() => null)
    if (!r?.ok) return null
    return { data: new Uint8Array(await r.arrayBuffer()), mime: DEMO_FILES[name], name }
  }

  /** Le voci di prova di una conversazione, come le darebbe `transcript`. */
  private transcript(name: string, arg: string | null | undefined): CmdResult['text'] | null {
    const all: TranscriptEntry[] = this.world.transcripts[name] ?? []
    const after = arg?.match(/after=(.+)$/)?.[1]
    if (after) { const i = all.findIndex(e => e.id === after); return i < 0 ? null : JSON.stringify({ entries: all.slice(i + 1), more: false }) }
    return JSON.stringify({ entries: all.slice(-(Number(arg) || 50)), more: false })
  }

  async send(cmd: Cmd): Promise<CmdResult> {
    const seen = this.results.get(cmd.id)
    if (seen) return seen
    const s = this.st
    const ses = s.sessions.find(x => x.name === cmd.session)
    const t = this.now()
    const ok = (text: string, extra: Partial<CmdResult> = {}): CmdResult => ({ id: cmd.id, ok: true, text, at: t, ...extra })
    const ko = (text: string): CmdResult => ({ id: cmd.id, ok: false, text, at: t })
    const r = ((): CmdResult => {
      switch (cmd.op) {
        case 'answer': {
          const q = ses?.question
          if (!ses) return ko(`no session ${cmd.session}`)
          if (!q) return ko(`${ses.name} has no question`)
          // Contratto 1.10: «Type something.» e «Chat about this», con i testi del relay.
          if (cmd.arg?.startsWith('text:')) {
            const x = cmd.arg.slice(5)
            if (!x.trim()) return ko('empty text')
            this.replace({ ...ses, question: null, state: 'busy', since: t, turn_started: t, tool: 'Bash', tool_note: x })
            return ok(`answered ${q.options.length + 1}. ${x}`)
          }
          if (cmd.arg === 'chat') { this.replace({ ...ses, question: null, state: 'idle', since: t }); return ok(`answered ${q.options.length + 2}. Chat about this`) }
          const n = Number(cmd.arg)
          if (!Number.isInteger(n)) return ko(`answer ${cmd.arg}: expected a number, text:<text> or chat`)
          const opt = q.options.find(o => o.n === n)
          // Dopo la risposta la sessione dice cosa sta facendo, non «turno in corso» (Franz, 18/09 07:39).
          this.replace({ ...ses, question: null, state: 'busy', since: t, turn_started: t, tool: 'Bash', tool_note: n === 1 ? 'Deploying 2.8.0 to production' : opt?.description ?? opt?.label ?? null })
          this.set({ ...this.st, attention: (this.st.attention ?? []).filter(a => !(a.kind === 'question' && a.session === ses.name)) })
          return ok(`answered ${cmd.arg}. ${opt?.label ?? cmd.arg}`)
        }
        // Contratto 1.63: con `at` il prompt aspetta nella coda della sessione; nella demo non parte.
        case 'prompt': {
          if (!ses) return ko(`no session ${cmd.session}`)
          if (cmd.at != null) {
            const hm = new Date(cmd.at * 1000).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })
            return ok(`scheduled for ${hm}: ${ses.name} gets it then (at its first stop after, if it is working)`)
          }
          // Contratto 1.65: a una sessione chiusa il relay la riapre e poi consegna.
          if (ses.state === 'gone' && !cmd.reopen) return ko(`no session ${cmd.session}`)
          this.replace({ ...ses, state: 'busy', question: null, turn_started: t, since: t, tool: 'Bash', tool_note: cmd.text ?? ses.tool_note ?? null, needs_you: null, turn_blocking: null })
          this.set({ ...this.st, attention: (this.st.attention ?? []).filter(a => a.session !== ses.name || a.kind === 'question') })
          return ok('delivered')
        }
        case 'report': {
          const ids = (cmd.arg ?? '').split(',').map(x => x.trim()).filter(Boolean)
          const saved = (i: string) => `docs/segnalazioni/demo-${i.slice(0, 8)}.jpg`
          if (!ses || ses.state === 'gone') return ko(`no session ${cmd.session}`)
          if ((cmd.text?.length ?? 0) > 4000) return ko('text too long: at most 4000 characters')
          if (ids.length > 5) return ko('too many attachments: at most 5')
          if (ids.some(i => !this.shared.has(i))) return ko('image missing or unreadable')
          if (!ids.length && !cmd.text?.trim()) return ko('empty report: nothing to send')
          this.replace({ ...ses, state: 'busy', turn_started: t, since: t, tool: 'Read', tool_note: cmd.text || null })
          return ok(ids.length ? `sent to ${ses.name}: ${ids.length > 1 ? 'images saved as' : 'image saved as'} ${ids.map(saved).join(', ')}` : `sent to ${ses.name}`)
        }
        case 'launch': {
          const p = s.projects.find(x => x.path === cmd.arg)
          if (!p) return ko('unknown project')
          const there = s.sessions.find(x => x.name === p.name)
          const fresh: Session = { id: `demo-${p.name}`, name: p.name, account: p.account, account_kind: p.account, project: p.path.split('/').slice(-2).join('/'), state: 'busy', since: t,
            turn_started: t, tool: 'Bash', tool_note: cmd.text ?? null, model: { id: 'claude-opus-5[1m]', label: 'Opus 5' }, effort: 'medium', context: 3 }
          this.set({ ...s, ts: t, sessions: there ? s.sessions.map(x => (x.name === p.name ? { ...x, ...fresh, id: x.id, question: null } : x)) : [...s.sessions, fresh],
            projects: s.projects.map(x => (x.path === p.path ? { ...x, last_used: t } : x)) })
          return ok(`launched ${p.name} (${p.account})`, { session: p.name })
        }
        case 'follow': if (!ses) return ko(`no session ${cmd.session}`); this.replace({ ...ses, followed: true }); return ok(`following ${ses.name}`)
        case 'unfollow': if (!ses) return ko(`no session ${cmd.session}`); this.replace({ ...ses, followed: false }); return ok('unfollowed')
        case 'resume': return ses?.state === 'gone' ? ko(`${ses.name} is gone: use launch`) : ok('resumed')
        // Contratto 1.9: i testi del relay; riaperta, la sessione torna fra le vive.
        case 'reopen':
          if (!ses) return ko(`${cmd.session} is not a closed session`)
          if (ses.state !== 'gone') return ko(`${ses.name} is already running`)
          this.replace({ ...ses, state: 'idle', since: t })
          return ok(`reopened ${ses.name} (${ses.account}): last conversation in the folder`)
        case 'allow_all': return ko("no «don't ask again» option on this question")
        case 'last': return ses?.outcome?.full ? ok(ses.outcome.full) : ko(`${cmd.session}: nessun messaggio da leggere`)
        // Contratto 1.12: la demo non cambia modello né effort, lo dice come farebbe il relay con una sessione occupata.
        case 'model': case 'effort': return ko(`${cmd.session}: not available in demo`)
        case 'transcript': { const x = ses ? this.transcript(ses.name, cmd.arg) : null; return x == null ? ko(`${cmd.session}: no transcript`) : ok(x) }
        case 'file': {
          const name = (cmd.arg ?? '').split('/').pop() ?? ''
          if (!ses || !DEMO_FILES[name]) return ko('missing or unreadable')
          this.files.set(cmd.id, name)
          return ok(`ready: ${name}`)
        }
        case 'slash': {
          if (!ses) return ko(`no session ${cmd.session}`)
          // /exit chiude davvero, così il pannello di chiusura arriva in fondo; /clear svuota la conversazione di prova.
          if (cmd.arg === 'exit') {
            this.replace({ ...ses, state: 'gone', since: t, question: null, needs_you: null, turn_blocking: null })
            // Chiusa, non aspetta più nessuno: il relay la toglie da `attention` (15/10: il banner la contava ancora).
            this.set({ ...this.st, attention: (this.st.attention ?? []).filter(a => a.session !== ses.name) })
          }
          if (cmd.arg === 'clear') { this.world.transcripts[ses.name] = []; this.replace({ ...ses, context: 0 }) }
          return ok(`sent /${cmd.arg} to ${cmd.session}`)
        }
        case 'interrupt':
          if (!ses || ses.state === 'gone') return ko(`${cmd.session} is not running`)
          if (ses.state !== 'busy' && ses.state !== 'awaiting') return ko(`${ses.name}: nothing to stop`)
          this.replace({ ...ses, state: 'idle', turn_started: null })
          return ok(`${ses.name}: stopped`)
        case 'projects': return ok(JSON.stringify({ projects: [...s.projects].sort((a, b) => (b.last_used ?? 0) - (a.last_used ?? 0) || a.name.localeCompare(b.name)) }))
        case 'search': {
          const q = (cmd.arg ?? '').trim().replace(/\s+/g, ' ')
          return !q || q.length > 200 ? ko('bad query: from 1 to 200 characters') : ok(JSON.stringify(demoSearch({ state: s, transcripts: this.world.transcripts }, q)))
        }
        case 'timeline': return ok(JSON.stringify(cmd.session ? { ...this.world.timeline, sessions: this.world.timeline.sessions.filter(x => x.session === cmd.session) } : this.world.timeline))
        case 'night_add': {
          const p = s.projects.find(x => x.path === cmd.arg)
          const items = s.night.items ?? []
          const master = cmd.arg === NIGHT_TARGET
          if (!p && !master) return ko(`${cmd.arg} is not a published project`)
          if (!cmd.text?.trim()) return ko('empty prompt: nothing to queue')
          if (items.length >= 8) return ko("tonight's queue is full (8 jobs): remove one first")
          const id = Math.abs([...(cmd.id + t)].reduce((h, c) => (h * 31 + c.charCodeAt(0)) | 0, 0)).toString(16).padStart(8, '0').slice(0, 8)
          const one = cmd.text.split('\n').join(' ').trim()
          const prompt = one.length <= 160 ? one : one.slice(0, one.lastIndexOf(' ', 160))
          const name = p?.name ?? 'master', dir = p?.path ?? '/home/demo/workspaces/personal/control-room'
          const next = [...items, { id, dir, name, prompt, added: t, started: null }]
          this.set({ ...s, ts: t, night: { ...s.night, queued: next.length, items: next } })
          return ok(`queued for tonight: ${name} (${p?.account ?? 'personal'}), job ${id}`, { job: id })
        }
        case 'night_remove': {
          const items = s.night.items ?? []
          const it = items.find(i => i.id === cmd.arg)
          if (!it) return ko(`no job ${cmd.arg} in tonight's queue`)
          if (it.started != null) return ko(`job ${it.id} has already started: it cannot be removed`)
          const next = items.filter(i => i !== it)
          this.set({ ...s, ts: t, night: { ...s.night, queued: next.length, items: next } })
          return ok(`removed from tonight's queue: ${it.id}`)
        }
        // Contratto 1.31: la demo non ha un PC con una chiave a cui aggiungere un dispositivo; risponde come il relay.
        case 'pair_add': return ko('pair --add: no saved key, nothing to add to — use `relay pair`')
        case 'unpair': {
          const dv = s.devices?.find(x => x.uid === cmd.arg)
          if (!dv) return ko(`unknown device ${cmd.arg}`)
          this.set({ ...s, devices: s.devices!.filter(x => x.uid !== dv.uid) })
          return ok(`removed ${dv.name}`)
        }
        // Contratto 1.37: la demo toglie il compito approvato dalla lista.
        case 'approve':
          if (!s.approvals.some(a => a.task === cmd.arg)) return ko(`${cmd.arg}: no task waiting for an ok`)
          this.set({ ...s, approvals: s.approvals.filter(a => a.task !== cmd.arg) })
          return ok(`approved ${cmd.arg}`)
        case 'decision': return cmd.text?.trim() ? ok('decision sent to the master') : ko('empty decision')
        case 'night': return ok(JSON.stringify(this.world.night))
        case 'agenda': return ok(JSON.stringify(this.world.agenda))
        case 'agenda_set': {
          const rows = this.world.agenda.rows
          const i = rows.findIndex(x => x.key === cmd.arg)
          if (i < 0) return ko('agenda row changed')
          const row = rows[i]
          const nu = cmd.action === 'done' ? { ...row, state: 'fatto' }
            : cmd.action === 'snooze' ? (/^\d{4}-\d{2}-\d{2}$/.test(cmd.until ?? '') ? { ...row, state: 'sospeso', until: cmd.until } : null)
            : cmd.action === 'remove' ? { ...row, state: 'scartato' }
            : cmd.action === 'pass' ? (cmd.blocks === 'claude' || cmd.blocks === 'you' ? { ...row, blocks: cmd.blocks } : null)
            : undefined
          if (nu === undefined) return ko(`bad agenda action: ${cmd.action}`)
          if (nu === null) return ko(cmd.action === 'snooze' ? `bad until date: ${cmd.until}` : `bad blocks: ${cmd.blocks}`)
          const key = `d${++this.agendaSeq}`
          this.world.agenda = { ...this.world.agenda, rows: rows.map((x, k) => (k === i ? { ...nu, key } : x)) }
          return ok(JSON.stringify({ key }))
        }
        // Contratto 1.69: i testi del relay; nella demo la modalità cambia subito nello stato.
        case 'permission_mode':
          if (!ses || ses.state === 'gone') return ko(`no session ${cmd.session}`)
          if (!MODES.has(cmd.arg ?? '')) return ko(`unknown permission mode: ${cmd.arg}`)
          if (cmd.arg === 'bypassPermissions' && !ses.permission_bypass) return ko(`${ses.name}: bypassPermissions is not in this session's Shift+Tab cycle, left on ${ses.permission_mode ?? 'default'}`)
          this.replace({ ...ses, permission_mode: cmd.arg })
          return ok(`${ses.name}: permissions now ${cmd.arg}`)
        // Contratto 1.64: la cartella si crea al secondo tocco, con `confirm`; nella demo nasce anche la sessione.
        case 'project_new': {
          const name = cmd.text ?? ''
          const path = `${(cmd.arg ?? '').replace(/\/+$/, '')}/${name}`
          if (!VALID_NAME.test(name)) return ko(`bad project name: ${name} (letters, digits, . _ -, at most 64)`)
          if (!cmd.confirm) return ko(`confirm: create ${path}`)
          this.set({ ...s, ts: t, projects: [...s.projects, { path, name, account: 'personal', last_used: t }],
            sessions: [...s.sessions, { id: `demo-${name}`, name, account: 'personal', account_kind: 'personal', project: `personal/${name}`, state: 'busy', since: t, turn_started: t, tool: 'Bash', tool_note: cmd.prompt ?? null, context: 2 }] })
          return ok(`created ${path} and launched it (personal)`, { session: name })
        }
        // Contratto 1.58: le tre strade dopo una quota finita, con i testi del relay.
        case 'quota_route': {
          const name = cmd.session ?? '', arg = cmd.arg ?? ''
          if (!s.sessions.some(x => x.name === name)) return ko(`${name} is not running`)
          if (arg === 'other_account') return ok(`${name}-2 continues on work from the handoff`, { session: `${name}-2` })
          if (arg === 'at_reset') return ok(`${name} resumes by itself at 18:00, when the quota resets`)
          if (arg.startsWith('pass:')) { const to = arg.slice(5); return s.sessions.some(x => x.name === to) ? ok(`passed to ${to} with the handoff`) : ko(`${to} is not running`) }
          return ko(`quota_route ${arg}: expected other_account, at_reset or pass:<session>`)
        }
        // Contratto 1.72 (FakeTransport): la sessione con `changes` nello stato (storefront) ha le modifiche della fixture del
        // contratto, le altre sono pulite; `file=` dà quel file solo, coi suoi conteggi.
        case 'changes': {
          if (!ses) return ko(`no session ${cmd.session}`)
          const all: ChangesView = ses.changes ? { ...decodeChanges(changesRaw), at: t }
            : { source: 'git', repo: `/home/demo/workspaces/${ses.project}`, base: 'f00ba47', files: [], totals: { files: 0, added: 0, removed: 0 }, cut: false, at: t }
          const file = cmd.arg?.startsWith('file=') ? cmd.arg.slice(5) : null
          if (file == null) return ok(JSON.stringify(all))
          const fs = all.files.filter(f => f.path === file)
          return ok(JSON.stringify({ ...all, files: fs, cut: false, totals: { files: fs.length, added: fs.reduce((n, f) => n + f.added, 0), removed: fs.reduce((n, f) => n + f.removed, 0) } }))
        }
        // Contratto 1.72: il passaggio intero di una voce; quello tagliato di blog ha la sua versione lunga nel mondo della demo.
        case 'diff': {
          const e = (this.world.transcripts[cmd.session ?? ''] ?? []).find(x => x.id === cmd.arg)
          if (!e) return ko(`no entry ${cmd.arg} in the transcript`)
          const d = this.world.fullDiffs[`${cmd.session}/${e.id}`] ?? e.diff
          return d ? ok(JSON.stringify(d)) : ko(`no diff for ${cmd.arg}`)
        }
        case 'screen': return ses ? ok(['⏺ Reading the checklist', 'Read(RELEASE.md)', 'Edit app/admin.py', 'Read app/seed.py'].join('\n')) : ko(`no session ${cmd.session}`)
        default: return ko(`${cmd.op}: not available in demo`)
      }
    })()
    this.results.set(cmd.id, r)
    return r
  }
}
