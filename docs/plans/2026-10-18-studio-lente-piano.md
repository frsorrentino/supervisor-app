# Pagina Studio con la master come lente — piano d'implementazione

> **Per chi esegue:** sub-skill richiesta: superpowers:executing-plans (metodo scelto: native) o
> superpowers:subagent-driven-development. I passi usano le caselle (`- [ ]`) per il tracciamento.

**Obiettivo:** una pagina Studio nella web app (PWA `web/`) con scadenziario, pratiche, prossimi e recap delle 8
pratiche, e sopra il dock della master che manda i prompt alla master generale con lo scope `[studio]`.

**Architettura:** il relay espone un op a richiesta `studio` (come `agenda`, mai nello stato) con pratiche, scadenze ed
esiti per pratica, e accetta `scope: "studio"` sul `prompt`, anteponendo un blocco `[studio]…[/studio]` col contesto.
Nella web una funzione pura `studioModel` costruisce le quattro sezioni; la pagina riusa `HomePane` e `MasterDock` così
com'è sulla home, con il campo della master che manda con lo scope. Nessuna sessione nuova.

**Stack:** Svelte 5 (runes), TypeScript, Vitest, Vite; relay Python (lo cambia la sessione `supervisor`, non questa).

**Specifica:** `/home/franz/Desktop/workspaces/.claude/proposte/2026-10-18-studio-pagina-master-lente.md`

## Vincoli globali

- Niente master dedicata: un solo master, due viste (home e Studio) sulla stessa sessione `master`.
- Chat = dock onnipresente in basso, espandibile, stesso componente della home (`HomePane` + `MasterDock`).
- Layout a 4 sezioni, in quest'ordine: Scadenziario, Pratiche, Prossimi, Recap Studio; dock sopra a tutto.
- Nessun duplicato di quota, ricorrenti, segnalazioni: restano sulla home. Nessuna funzione Studio rimossa.
- Il relay lo cambia solo la sessione `supervisor`, su richiesta di questa; da qui mai scrivere in `personali/supervisor/`.
- Testi visibili in `web/src/lib/t.ts`, `it` ed `en`; mai «…» nei testi; una riga logica su una riga fisica.
- Vitest, `npm run check`, `npm run build` solo su win (`ssh win`, `D:\workspaces\team-supervisor-app`), mai sul Chromebook.
- Commit in inglese, file per nome, mai `git add -A` o di cartella.
- Lavoro nel worktree `personali/supervisor-app-studio`, ramo `supervisor-app-studio`, nato da `feature/tablet`;
  a fine lavoro il ramo torna in `feature/tablet` (merge --no-ff), che è il ramo di lavoro reale del repo.
- Push su `origin` (pubblico) e deploy dell'hosting approvati con questo piano. Eccezione dichiarata: niente push sul
  remote `archivio` né sui suoi workflow.

## Review Focus

1. Quattro pratiche puntano alla stessa cartella (`personali/francescosorrentino.com`): una sessione accesa lì deve
   comparire su tutte e quattro, e un esito di quella cartella non deve finire quattro volte nel Recap Studio.
2. Relay vecchio senza `studio` in `ops`: la voce di menu c'è, la pagina dice di aggiornare il relay, il dock funziona
   e manda senza `scope` (nessun prompt perso).
3. Calendario spento o irraggiungibile (`ok: false` o `error` non vuoto): la pagina mostra l'errore in una riga e le
   pratiche comunque, senza scadenze; mai pagina vuota senza spiegazione.
4. Titolo di scadenza lungo: il chip dei Prossimi resta entro 40 caratteri, tagliato a fine parola, senza «…».
5. Una voce utente arrivata da un altro dispositivo porta il blocco `[studio]…[/studio]` nel testo: in chat si vede solo
   il messaggio, mai il blocco.

---

### Task 1: Richiesta di contratto al relay

**File:** nessuno in questo repo. Messaggio alla sessione `supervisor`.

**Interfacce prodotte (le usano tutti i task dopo):**

- Op `studio`, `session` e `arg` null, a richiesta, mai nello stato; `studio` entra in `ops`. Risposta `ok: true`,
  `text` = JSON `StudioPage`:

```json
{
  "cases": [{ "id": "c_3f1fa7a00ddc", "name": "Rino: procedimento penale", "project": "personali/rino" }],
  "deadlines": [{ "uid": "d_1", "summary": "Deposito memoria", "date": "2026-10-21", "days_left": 3, "due": true, "case": "c_3f1fa7a00ddc" }],
  "outcomes": [{ "project": "personali/rino", "session": "rino", "at": 1760790000, "short": "Bozza della memoria pronta" }],
  "error": ""
}
```

  `cases`: tutte le voci di `calendar.cases`, nell'ordine del file; `name` da un nuovo valore oggetto
  `{"dir": …, "name": …}` (resta valido il valore stringa: allora `name` = ultimo pezzo della cartella); `project` nella
  stessa forma di `sessions[].project`. `deadlines`: tutte le future (`days_left` ≥ 0) fino a 60 giorni, la più vicina
  prima, `due` = nel periodo di avviso; anche quelle senza pratica (`case` = ""). `outcomes`: per ogni cartella delle
  pratiche, gli ultimi 3 esiti delle sue sessioni (anche chiuse), il più nuovo prima, una volta sola per cartella.
  `error`: il testo di `cm-calendar` se il feed non risponde (le scadenze allora dalla copia in cache o vuote).
  Calendario spento o irraggiungibile: `ok: true` con `cases` pieno, `deadlines` vuoto ed `error` = «calendar off» o il
  testo dell'errore (addendum del 18/10 dopo la revisione finale: la pagina mostra le pratiche anche senza scadenze).
- `prompt` accetta `scope: "studio"`: il relay antepone al testo

```text
[studio]
Pratiche: <name> → <cartella>   (una riga per pratica)
Scadenze: <gg/mm> <summary> · <name>   (una riga per scadenza dei prossimi 60 giorni)
Regola: a ogni passo concluso di una pratica, ~/Desktop/workspaces/personali/console-personale/bin/studio-push evento "<parte del nome>" "<cosa è successo>" [protocollo]
[/studio]
```

  poi una riga vuota e il testo mandato. Uno scope sconosciuto si ignora. La voce in trascrizione finisce col testo
  mandato, come oggi con le istruzioni del relay.
- Fixture: in `cmd-result-sample.json` un `studio` riuscito e uno «calendar off»; `studio` in `ops` nei tre `state-*.json`.

- [ ] **Passo 1: manda la richiesta**

```bash
supervisor talk supervisor "Richiesta contratto dall'app (spec Studio approvata da Franz il 18/10): op studio + prompt.scope. Testo completo in docs/plans/2026-10-18-studio-lente-piano.md del worktree supervisor-app-studio, Task 1. Rispondi con versione e commit quando è pubblicato; le fixture le aggiorni tu."
```

- [ ] **Passo 2:** non aspettare: i task 2-7 usano dati scritti nei test; il Task 8 si fa quando arriva la risposta.

### Task 2: Tipi del contratto e `newCmd` con lo scope

**File:**
- Modifica: `web/src/lib/contract.ts` (tipi `Studio*`, `CmdOp`, `Cmd.scope`)
- Test: `web/src/lib/studio.test.ts` (creato qui, cresce nel Task 3)

**Interfacce:**
- Produce:

```ts
export type StudioCase = { id: string; name: string; project: string }
export type StudioDeadline = { uid: string; summary: string; date: string; days_left: number; due: boolean; case: string }
export type StudioOutcome = { project: string; session: string; at: number; short: string }
export type StudioPage = { cases: StudioCase[]; deadlines: StudioDeadline[]; outcomes: StudioOutcome[]; error: string }
// CmdOp: + 'studio'.  Cmd: + scope?: 'studio'
```

- [ ] **Passo 1: test che fallisce** — in `web/src/lib/studio.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { newCmd } from './transport'
import type { Cmd } from './contract'

describe('studio: comando', () => {
  it('lo scope viaggia sul prompt', () => {
    const c: Cmd = { ...newCmd('prompt', 'master', 'ciao'), scope: 'studio' }
    expect(c.scope).toBe('studio')
    expect(newCmd('studio', null).op).toBe('studio')
  })
})
```

- [ ] **Passo 2:** su win `npx vitest run src/lib/studio.test.ts` e `npm run check` — atteso: errore di tipo su `'studio'`.
- [ ] **Passo 3:** in `contract.ts` aggiungi i quattro tipi sopra, `| 'studio'` in fondo a `CmdOp` con il commento
  `/** Contratto 1.7x: la pagina Studio (op a richiesta) e lo scope del prompt. */`, e in `Cmd`
  `scope?: 'studio'` con lo stesso commento.
- [ ] **Passo 4:** test e check verdi su win.
- [ ] **Passo 5:** `git add web/src/lib/contract.ts web/src/lib/studio.test.ts && git commit -m "feat(web): studio op and prompt scope in the contract types"`

### Task 3: `studioModel`, chip dei Prossimi, `stripScope`

**File:**
- Crea: `web/src/lib/studio.ts`
- Test: `web/src/lib/studio.test.ts`

**Interfacce:**
- Consuma: i tipi del Task 2, `Session` di `contract.ts`.
- Produce:

```ts
export type CaseView = { id: string; name: string; project: string; next: StudioDeadline | null; session: Session | null; last: StudioOutcome | null; open: boolean }
export type StudioModel = { deadlines: (StudioDeadline & { caseName: string | null })[]; open: CaseView[]; rest: CaseView[]; steps: StudioStep[]; recap: (StudioOutcome & { names: string[] })[] }
export type StudioStep = { label: string; send: string }
export const RECENT_S = 7 * 86400
export function studioModel(page: StudioPage, sessions: Session[], now: number, step: (d: StudioDeadline, caseName: string | null) => StudioStep): StudioModel
export function chip(prefix: string, title: string, max?: number): string   // max = 40
export function stripScope(text: string): string
```

Regole: `next` = la prima scadenza della pratica; `session` = la sessione non chiusa con `project` uguale (la più
recente per `since`); `last` = il primo esito con lo stesso `project`; `open` = `next?.due` o `session` o
`now - last.at < RECENT_S`. `open` e `rest` mantengono l'ordine di `cases`, ma `open` mette prima chi ha la scadenza
più vicina. `steps`: una per scadenza `due`, al massimo 3 (`MAX` di `nextSteps.ts`). `recap`: gli esiti ordinati per
`at` decrescente, uno per (`project`, `at`), con `names` = i nomi delle pratiche di quella cartella.

- [ ] **Passo 1: test che falliscono** — aggiungi a `studio.test.ts`:

```ts
import { chip, stripScope, studioModel, RECENT_S } from './studio'
import type { Session, StudioPage } from './contract'

const now = 1760800000
const page: StudioPage = {
  cases: [
    { id: 'a', name: 'Accordo Idemedia', project: 'personali/francescosorrentino.com' },
    { id: 'b', name: 'Permessi 104', project: 'personali/francescosorrentino.com' },
    { id: 'r', name: 'Rino: procedimento penale', project: 'personali/rino' },
    { id: 't', name: 'Termocamino Antonietta', project: 'personali/termocamino' },
  ],
  deadlines: [
    { uid: 'd1', summary: 'Deposito memoria', date: '2026-10-21', days_left: 3, due: true, case: 'r' },
    { uid: 'd2', summary: 'Rata 4 accordo', date: '2026-11-30', days_left: 43, due: false, case: '' },
  ],
  outcomes: [
    { project: 'personali/francescosorrentino.com', session: 'fs', at: now - 3600, short: 'Lettera pronta' },
    { project: 'personali/termocamino', session: 'tc', at: now - RECENT_S - 10, short: 'Vecchio' },
  ],
  error: '',
}
const live = { name: 'fs', project: 'personali/francescosorrentino.com', state: 'idle', since: now - 60 } as Session
const step = (d: { summary: string }) => ({ label: chip('Prepara', d.summary), send: `Prepara: ${d.summary}` })

describe('studioModel', () => {
  it('scadenziario con il nome della pratica, la più vicina prima', () => {
    const m = studioModel(page, [live], now, step)
    expect(m.deadlines.map(d => [d.summary, d.caseName])).toEqual([['Deposito memoria', 'Rino: procedimento penale'], ['Rata 4 accordo', null]])
  })
  it('una cartella condivisa: la sessione su tutte le pratiche, l\'esito una volta sola nel recap', () => {
    const m = studioModel(page, [live], now, step)
    expect(m.open.map(c => c.id)).toEqual(['r', 'a', 'b'])
    expect(m.open.find(c => c.id === 'b')?.session?.name).toBe('fs')
    expect(m.recap.filter(o => o.short === 'Lettera pronta').map(o => o.names)).toEqual([['Accordo Idemedia', 'Permessi 104']])
  })
  it('collassate le pratiche senza scadenza in avviso, sessione o attività recente', () => {
    expect(studioModel({ ...page, outcomes: [] }, [], now, step).rest.map(c => c.id)).toEqual(['a', 'b', 't'])
    expect(studioModel(page, [], now, step).rest.map(c => c.id)).toEqual(['t'])
  })
  it('una sessione chiusa non conta come accesa', () => {
    expect(studioModel(page, [{ ...live, state: 'gone' } as Session], now, step).open.find(c => c.id === 'a')?.session).toBe(null)
  })
  it('i Prossimi solo dalle scadenze in avviso, al massimo 3', () => {
    const many = { ...page, deadlines: Array.from({ length: 5 }, (_, i) => ({ ...page.deadlines[0], uid: `x${i}` })) }
    expect(studioModel(page, [], now, step).steps.map(s => s.label)).toEqual(['Prepara Deposito memoria'])
    expect(studioModel(many, [], now, step).steps).toHaveLength(3)
  })
})

describe('chip', () => {
  it('entro 40 caratteri, a fine parola, senza puntini', () => {
    const c = chip('Prepara', 'Ricorso al tribunale amministrativo regionale della Calabria')
    expect(c.length).toBeLessThanOrEqual(40)
    expect(c).toBe('Prepara Ricorso al tribunale')
    expect(c).not.toMatch(/…|\.\.\./)
  })
})

describe('stripScope', () => {
  it('toglie il blocco [studio] del relay e la riga vuota dopo', () => {
    expect(stripScope('[studio]\nPratiche: x → y\n[/studio]\n\nChe cosa scade?')).toBe('Che cosa scade?')
    expect(stripScope('Che cosa scade?')).toBe('Che cosa scade?')
    expect(stripScope('[studio] senza chiusura')).toBe('[studio] senza chiusura')
  })
})
```

- [ ] **Passo 2:** su win `npx vitest run src/lib/studio.test.ts` — atteso FAIL, «Failed to resolve import "./studio"».
- [ ] **Passo 3: implementazione** — `web/src/lib/studio.ts`:

```ts
// La pagina Studio (spec del 18/10, approvata da Franz): le pratiche dello scadenziario come lente sulla master, con lo
// stesso dock della home. Il relay dà pratiche, scadenze ed esiti con l'op `studio`; qui le quattro sezioni.
import type { Session, StudioDeadline, StudioOutcome, StudioPage } from './contract'
import { MAX } from './nextSteps'

export type CaseView = { id: string; name: string; project: string; next: StudioDeadline | null; session: Session | null; last: StudioOutcome | null; open: boolean }
export type StudioStep = { label: string; send: string }
export type StudioModel = { deadlines: (StudioDeadline & { caseName: string | null })[]; open: CaseView[]; rest: CaseView[]; steps: StudioStep[]; recap: (StudioOutcome & { names: string[] })[] }

/** Un esito più vecchio di una settimana non tiene la pratica in evidenza. */
export const RECENT_S = 7 * 86400

/** «Prepara Ricorso al tribunale»: entro `max` caratteri, tagliato a fine parola, mai coi puntini. */
export function chip(prefix: string, title: string, max = 40): string {
  const full = `${prefix} ${title}`.trim()
  if (full.length <= max) return full
  const cut = full.slice(0, max + 1)
  const space = cut.lastIndexOf(' ')
  return (space > prefix.length ? cut.slice(0, space) : full.slice(0, max)).trimEnd()
}

/** Il blocco `[studio]…[/studio]` che il relay antepone al prompt non si mostra in chat. */
export function stripScope(text: string): string {
  const m = /^\[studio\]\n[\s\S]*?\n\[\/studio\]\n+/.exec(text)
  return m ? text.slice(m[0].length) : text
}

export function studioModel(page: StudioPage, sessions: Session[], now: number, step: (d: StudioDeadline, caseName: string | null) => StudioStep): StudioModel {
  const nameOf = new Map(page.cases.map(c => [c.id, c.name]))
  const deadlines = [...page.deadlines].sort((a, b) => a.days_left - b.days_left).map(d => ({ ...d, caseName: nameOf.get(d.case) ?? null }))
  const views: CaseView[] = page.cases.map(c => {
    const next = deadlines.find(d => d.case === c.id) ?? null
    const session = sessions.filter(s => s.project === c.project && s.state !== 'gone').sort((a, b) => b.since - a.since)[0] ?? null
    const last = page.outcomes.filter(o => o.project === c.project).sort((a, b) => b.at - a.at)[0] ?? null
    return { ...c, next, session, last, open: !!next?.due || !!session || (!!last && now - last.at < RECENT_S) }
  })
  const left = (v: CaseView) => (v.next?.due ? v.next.days_left : Number.MAX_SAFE_INTEGER)
  const open = views.filter(v => v.open).map((v, i) => [v, i] as const).sort((a, b) => left(a[0]) - left(b[0]) || a[1] - b[1]).map(x => x[0])
  const steps = deadlines.filter(d => d.due).slice(0, MAX).map(d => step(d, d.caseName))
  const seen = new Set<string>()
  const recap = [...page.outcomes].sort((a, b) => b.at - a.at).filter(o => {
    const k = `${o.project}\n${o.at}`
    if (seen.has(k)) return false
    seen.add(k)
    return true
  }).map(o => ({ ...o, names: page.cases.filter(c => c.project === o.project).map(c => c.name) }))
  return { deadlines, open, rest: views.filter(v => !v.open), steps, recap }
}
```

- [ ] **Passo 4:** su win `npx vitest run src/lib/studio.test.ts` — atteso PASS (tutti). Se il test dell'ordine di
  `open` fallisce, correggi l'ordinamento, non il test: `r` ha la scadenza in avviso e va prima.
- [ ] **Passo 5:** `git add web/src/lib/studio.ts web/src/lib/studio.test.ts && git commit -m "feat(web): studio model — deadlines, cases with shared folders, next chips, scope header stripped"`

### Task 4: Il blocco `[studio]` non si vede in chat

**File:**
- Modifica: `web/src/lib/chatFeed.ts` (in `merge`, il ramo `type: 'user'` senza corrispondenza)
- Test: `web/src/lib/chatFeed.test.ts`

**Interfacce:** consuma `stripScope` del Task 3. Nessuna nuova.

- [ ] **Passo 1: test che fallisce** — in fondo al `describe` di `merge` in `chatFeed.test.ts`:

```ts
it('una voce Studio da un altro dispositivo si legge senza il blocco del relay', () => {
  const e = { role: 'user', text: '[studio]\nPratiche: x → y\n[/studio]\n\nChe cosa scade?', at: 100, origin: 'phone' }
  const items = merge([e as TranscriptEntry], [])
  expect(items[0]).toMatchObject({ type: 'user', entry: { text: 'Che cosa scade?' } })
})
```

  (usa gli import già presenti nel file per `merge` e `TranscriptEntry`; se `TranscriptEntry` non è importato, aggiungi
  `import type { TranscriptEntry } from './contract'`.)
- [ ] **Passo 2:** su win `npx vitest run src/lib/chatFeed.test.ts` — atteso FAIL sul testo.
- [ ] **Passo 3:** in `merge`, sostituisci `return { type: 'user', entry: e }` con
  `return { type: 'user', entry: e.text ? { ...e, text: stripScope(e.text) } : e }` e importa
  `import { stripScope } from './studio'`. Le voci `mine` restano come sono: mostrano già il testo mandato.
- [ ] **Passo 4:** `npx vitest run src/lib/chatFeed.test.ts src/lib/chatRules.test.ts` verdi su win.
- [ ] **Passo 5:** `git add web/src/lib/chatFeed.ts web/src/lib/chatFeed.test.ts && git commit -m "feat(web): the relay's [studio] block is hidden in the chat"`

### Task 5: Testi e demo

**File:**
- Modifica: `web/src/lib/t.ts` (blocco `it` e blocco `en`)
- Modifica: `web/src/lib/demo.ts` (campo `studio: StudioPage` in `World`)
- Modifica: `web/src/lib/demoTransport.ts` (`case 'studio'`)
- Test: `web/src/lib/demo.test.ts`

**Interfacce:**
- Produce le chiavi di `t`: `menuStudio`, `menuStudioSub`, `studioTitle`, `studioDeadlines`, `studioCases`,
  `studioMore(n)`, `studioNext`, `studioRecap`, `studioNone`, `studioOld`, `studioOff`, `studioLaunch`, `studioOpen`,
  `studioPrepare`, `studioPrepareSend(summary, date, name)`, `studioWhen(days)`, `studioLive`, `studioIdle`.
- Produce `world(now).studio: StudioPage` e la risposta dell'op `studio` nella demo.

- [ ] **Passo 1: test che fallisce** — in `demo.test.ts`:

```ts
it('la demo risponde a studio con pratiche e scadenze', async () => {
  const d = new DemoTransport(() => 1760800000)
  const r = await d.send(newCmd('studio', null))
  const p = JSON.parse(r.text)
  expect(r.ok).toBe(true)
  expect(p.cases.length).toBeGreaterThan(2)
  expect(p.deadlines.some((x: { due: boolean }) => x.due)).toBe(true)
})
```

  (usa la costruzione di `DemoTransport` che il file usa già; se il costruttore prende altri argomenti, copiali dal test
  vicino.)
- [ ] **Passo 2:** su win il test — atteso FAIL (op sconosciuto).
- [ ] **Passo 3:** testi in `t.ts`, `it` (accanto a `menuNight`):

```ts
  menuStudio: 'Studio', menuStudioSub: 'Scadenze e pratiche, con la master',
  studioTitle: 'Studio', studioDeadlines: 'Scadenziario', studioCases: 'Pratiche', studioMore: (n: number) => `Altre pratiche · ${n}`,
  studioNext: 'Prossimi', studioRecap: 'Recap Studio', studioNone: 'Nessuna scadenza nei prossimi 60 giorni',
  studioOld: 'Il relay sul PC non conosce ancora la pagina Studio: aggiorna supervisor', studioOff: 'Calendario di Studio non collegato sul PC',
  studioLaunch: 'Lancia', studioOpen: 'Apri', studioPrepare: 'Prepara', studioLive: 'sessione accesa', studioIdle: 'nessuna sessione',
  studioPrepareSend: (s: string, d: string, n: string | null) => `Prepara la scadenza «${s}» del ${d}${n ? ` (pratica ${n})` : ''}: cosa serve e cosa manca.`,
  studioWhen: (n: number) => (n === 0 ? 'oggi' : n === 1 ? 'domani' : `tra ${n} giorni`),
```

  ed `en`:

```ts
  menuStudio: 'Studio', menuStudioSub: 'Deadlines and cases, with the master',
  studioTitle: 'Studio', studioDeadlines: 'Deadlines', studioCases: 'Cases', studioMore: (n: number) => `Other cases · ${n}`,
  studioNext: 'Next', studioRecap: 'Studio recap', studioNone: 'No deadlines in the next 60 days',
  studioOld: "The relay on the PC doesn't know the Studio page yet: update supervisor", studioOff: "Studio's calendar is not linked on the PC",
  studioLaunch: 'Launch', studioOpen: 'Open', studioPrepare: 'Prepare', studioLive: 'session on', studioIdle: 'no session',
  studioPrepareSend: (s: string, d: string, n: string | null) => `Prepare the deadline «${s}» on ${d}${n ? ` (case ${n})` : ''}: what it needs and what is missing.`,
  studioWhen: (n: number) => (n === 0 ? 'today' : n === 1 ? 'tomorrow' : `in ${n} days`),
```

- [ ] **Passo 4:** demo: in `demo.ts` aggiungi `studio: StudioPage` al tipo `World` e in `world(now)` (con `en` come
  le altre voci) tre pratiche inventate su progetti già nella demo (`personal/atlas-shop`, `personal/control-room`,
  `personal/docs-site` o quelli presenti), due scadenze (`days_left` 2 con `due: true`, 20 con `due: false`), due
  esiti (`at: now - 3600`, `now - 86400 * 3`), `error: ''`. In `demoTransport.ts`, accanto a `case 'agenda'`:
  `case 'studio': return ok(JSON.stringify(this.world.studio))`; e `'studio'` nelle `ops` dello stato della demo, se
  la demo le elenca.
- [ ] **Passo 5:** su win `npx vitest run` intero e `npm run check` — verdi (il demo test e i test di `t` se ci sono).
- [ ] **Passo 6:** `git add web/src/lib/t.ts web/src/lib/demo.ts web/src/lib/demoTransport.ts web/src/lib/demo.test.ts && git commit -m "feat(web): studio strings (it, en) and demo answer"`

### Task 6: `StudioPage.svelte`, le quattro sezioni

**File:**
- Crea: `web/src/lib/StudioPage.svelte`

**Interfacce:**
- Consuma: `StudioModel`, `CaseView` (Task 3), `t` (Task 5).
- Produce il componente:

```ts
let { model, error, loading, old, onStep, onSession, onLaunch, onSpeak }: {
  model: StudioModel | null; error: string | null; loading: boolean; old: boolean
  onStep: (s: StudioStep) => void; onSession: (name: string) => void; onLaunch: (c: CaseView) => void; onSpeak: (text: string) => void
} = $props()
```

Contenuto, dall'alto: (1) **Scadenziario**: una riga per scadenza, `gg/mm · studioWhen · summary · nome pratica`, le
`due` col colore di avviso (`var(--b-warn)`), `studioNone` se vuoto. (2) **Pratiche**: `model.open` come righe
(nome, stato `studioLive`/`studioIdle`, prossima scadenza, ultimo esito con l'ora), tocco = `onSession` se c'è la
sessione, altrimenti la riga si apre e mostra il tasto `studioLaunch` (`onLaunch`); sotto un `<details>` chiuso
`studioMore(rest.length)` con le altre. (3) **Prossimi**: i chip di `model.steps`, tocco = `onStep`; sezione assente se
vuota. (4) **Recap Studio**: `model.recap`, una riga per esito, `hh:mm` o `gg/mm`, nomi delle pratiche, `short`, ▶
(`onSpeak`) se `short` ≥ 120 caratteri. In cima, se `old`, una riga `studioOld`; se `error`, una riga con l'errore.
Nessun box di quota, ricorrenti o segnalazioni. Stile: copia le classi delle sezioni di `RecapPage.svelte` (titolo
di sezione, righe, chip) per restare nel tema; righe intere, testo su una riga con `text-overflow: clip` solo dove serve.

- [ ] **Passo 1:** scrivi il componente con il markup sopra, usando `<section>` per sezione e `<h2>` col titolo.
- [ ] **Passo 2:** su win `npm run check` — 0 errori.
- [ ] **Passo 3:** `git add web/src/lib/StudioPage.svelte && git commit -m "feat(web): Studio page sections — deadlines, cases, next, recap"`

### Task 7: La pagina nell'app, col dock della master in scope

**File:**
- Modifica: `web/src/lib/AppBar.svelte` (tipo `Page` + voce di menu)
- Modifica: `web/src/App.svelte` (`sendTo` con lo scope, caricamento `studio`, `pageView`, `?page=studio`)
- Modifica: `web/src/lib/Page.svelte` (prop `fill` per il dock a tutta altezza)

**Interfacce:**
- Consuma: `studioModel`, `chip` (Task 3), `StudioPage.svelte` (Task 6), `t.*` (Task 5), `HomePane` com'è.
- Produce `sendTo(name: string, text: string, scope?: 'studio')`.

- [ ] **Passo 1: `sendTo`** — in `App.svelte` cambia la firma in `async function sendTo(name: string, text: string, scope?: 'studio')`
  e il comando in `{ ...newCmd('prompt', name, text), id: m.id, ...(closed ? { reopen: true } : {}), ...(scope && st.ops?.includes('studio') ? { scope } : {}) }`.
  Con un relay vecchio lo scope non parte e il prompt arriva lo stesso (Review Focus 2).
- [ ] **Passo 2: menu** — in `AppBar.svelte` aggiungi `'studio'` al tipo `Page` e la voce dopo `recap`:
  `{ page: 'studio', title: t.menuStudio, sub: t.menuStudioSub, d: ['M4 7h16v13H4z', 'M9 7V4h6v3', 'M4 12h16'] }`.
- [ ] **Passo 3: dati** — in `App.svelte`, accanto all'agenda:

```ts
  // La pagina Studio (spec del 18/10): pratiche, scadenze ed esiti con l'op `studio`, chiesti all'apertura della pagina.
  let studio = $state<{ page: StudioPageData | null; error: string | null; loading: boolean }>({ page: null, error: null, loading: false })
  const canStudio = $derived(!!st.ops?.includes('studio'))
  async function loadStudio() {
    if (!tr) return
    studio = { ...studio, loading: true, error: null }
    try {
      const r = await tr.send(newCmd('studio', null, null))
      if (r.ok) { const p: StudioPageData = JSON.parse(r.text); studio = { page: p, error: p.error || null, loading: false } }
      else studio = { page: studio.page, error: r.text.startsWith('calendar off') ? t.studioOff : r.text, loading: false }
    } catch { studio = { ...studio, error: t.noAnswer, loading: false } }
  }
  $effect(() => { if (page === 'studio' && canStudio) untrack(() => loadStudio()) })
  const studioView = $derived(studio.page ? studioModel(studio.page, st.sessions, now, (d, n) => ({ label: chip(t.studioPrepare, d.summary),
    send: t.studioPrepareSend(d.summary, `${d.date.slice(8, 10)}/${d.date.slice(5, 7)}`, n) })) : null)
```

  con `import type { StudioPage as StudioPageData } from './lib/contract'`, `import { chip, studioModel } from './lib/studio'`,
  `import StudioPage from './lib/StudioPage.svelte'`. Aggiungi `'studio'` alla lista di `asked` e
  `studio: t.studioTitle` a `pageTitle`.
- [ ] **Passo 4: la pagina col dock** — in `pageView`, prima di `{:else}`:

```svelte
    {:else if p === 'studio'}
      <HomePane {master} entries={transcripts[MASTER] ?? []} open={masterOpen} onToggle={(o) => smooth(() => { masterOpen = o; masterHalf = o && wide })} onSpeak={(x) => toggle(x, MASTER)}
        halfStops={wide} half={masterHalf} onHalf={(h) => (masterHalf = h)}>
        {#snippet quick()}{@render masterQuick('studio')}{/snippet}
        {#snippet list()}<div class="list"><StudioPage model={studioView} error={studio.error} loading={studio.loading} old={!canStudio}
          onStep={(s) => { sendTo(MASTER, s.send, 'studio'); smooth(() => { masterOpen = true }) }} onSession={(n) => { openPage(null); pick(n) }}
          onLaunch={(c) => cmd(c.project.split('/').filter(Boolean).at(-1) ?? c.name)('launch', c.project)} onSpeak={(x) => toggle(x)} /></div>{/snippet}
        {#snippet chat()}{@render chatOf(MASTER, true)}{/snippet}
      </HomePane>
```

  e rendi `masterQuick` parametrico: `{#snippet masterQuick(scope?: 'studio')}` con `onSend={(x) => { sendTo(MASTER, x, scope); smooth(() => { masterOpen = true }) }}`.
  Il `Page` attorno resta (← e titolo): il suo `.body` scorre, ma `HomePane` vuole altezza piena: dai a `Page` una prop
  `fill = false` che, vera, mette `.body { overflow: hidden; display: flex; flex-direction: column }`, e passala per `studio`.
  Prima di scrivere `onLaunch`, controlla in `Launch.svelte` quale `session` e `arg` manda l'op `launch` (`cmd(pr.name)('launch', pr.path, …)`):
  se `pr.path` non è nella forma di `sessions[].project`, prendi il `Project` di `st.projects` con lo stesso nome di cartella e usa il suo `path`.
- [ ] **Passo 5:** su win `npx vitest run`, `npm run check`, `npm run build` — verdi.
- [ ] **Passo 6: prova a vista** — su win `npx vite preview --host` o build copiata e servita qui con `python3 -m http.server`
  dalla `dist`; con chrome-bridge apri `?page=studio` in demo a 412×915 e a 1280×800: quattro sezioni nell'ordine, il dock
  in fondo, tocco sul dock = conversazione, invio da un chip = messaggio in chat della master. Screenshot in
  `docs/screenshots/studio-pagina-telefono.png` e `docs/screenshots/studio-pagina-plancia.png`.
- [ ] **Passo 7:** `git add web/src/App.svelte web/src/lib/AppBar.svelte web/src/lib/Page.svelte docs/screenshots/studio-pagina-telefono.png docs/screenshots/studio-pagina-plancia.png && git commit -m "feat(web): Studio page in the menu, with the master dock sending with the studio scope"`

### Task 8: Fixture del relay e chiusura

**File:**
- Modifica: `contract/*.json`, `contract/README.md` (arrivano dalla sessione `supervisor`, copiati identici)
- Modifica: `web/src/lib/contract.test.ts`

- [ ] **Passo 1:** alla risposta di `supervisor`, copia le fixture e il paragrafo del README dalla sua versione; se la
  forma differisce dal Task 1, adegua i tipi del Task 2 e i test del Task 3, non il contrario.
- [ ] **Passo 2: test** — in `contract.test.ts`:

```ts
it('studio: il risultato della fixture si legge come StudioPage', () => {
  const root = JSON.parse(fixture('cmd-result-sample.json'))
  const ok = root.result.find((r: { text: string }) => r.text.startsWith('{"cases"'))
  const p: StudioPage = JSON.parse(ok.text)
  expect(p.cases.every(c => c.id && c.name && c.project)).toBe(true)
  expect(p.deadlines.every(d => /^\d{4}-\d{2}-\d{2}$/.test(d.date) && d.days_left >= 0)).toBe(true)
  for (const n of ['state-1-question.json', 'state-2-idle.json', 'state-3-stale.json']) expect(decodeState(fixture(n)).ops).toContain('studio')
})
```

- [ ] **Passo 3:** su win suite intera, check, build — verdi. `git add` dei file per nome, commit `test: studio fixtures from the relay (contract 1.7x)`.
- [ ] **Passo 4: unione e uscita** — `git -C ../supervisor-app merge --no-ff --no-edit supervisor-app-studio` (sul ramo
  `feature/tablet`, solo se il working tree principale non ha modifiche ai file toccati), push di `feature/tablet` su
  `origin`, build su win, `dist` riportata qui, `firebase deploy --only hosting --project claude-master-relay-3761 --non-interactive`
  dalla cartella `web` in background. Poi a Franz: Ctrl+R sulla PWA.
- [ ] **Passo 5: prova dal vivo** — dopo il relay nuovo: dalla PWA vera apri Studio, manda «Che cosa scade questa
  settimana?» dal dock; in `~/.cc-supervisor/relay/relay.log` la riga del prompt con lo scope; in chat il messaggio
  senza blocco; la master risponde con le pratiche giuste.

Se la risposta del relay non arriva entro la fine del lavoro: i Task 2-7 vanno comunque uniti e pubblicati (la pagina
dice `studioOld` e il dock manda senza scope); il Task 8 resta aperto e lo si dice nel report alla master.
