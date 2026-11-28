<script lang="ts">
  import ProvaCards from './ProvaCards.svelte'
  import { DAY } from './durations'
  import { popOnChange } from './motion'
  import { arriva, entra } from './molla'
  import { arrivalGate, OPENING_MS } from './arrivo'
  import CopyButton from './CopyButton.svelte'
  import { parts as citeParts } from './citazione'
  import QuoteBlock from './QuoteBlock.svelte'
  import { locale } from './lang'
  import type { Session, StepDiff, TranscriptEntry, TranscriptFile } from './contract'
  import type { Load } from './diff'
  import StepDiffRow from './StepDiffRow.svelte'
  import ImageViewer from './ImageViewer.svelte'
  import { sepMemory, sepReason, type Item } from './chatFeed'
  import { entryIds, failedCount, foldable, rowOf, shownWork, stepDetail, stepLine, toolCount, turns, type Request, type Row } from './chatTurns'
  import { copyText, rich, sections, stripTipo } from './answer'
  import { reducedMotion } from './motion'
  import type { Status } from './chatRules'
  import { blocks, cards, compact, type Table } from './markdown'
  import { kind } from './toolText'
  import { outcomeForPhone } from './links'
  import { stripSessionLinks, stripWaitLinks } from './sessionLinks'
  import SceltaCards from './SceltaCards.svelte'
  import { parseSteps } from './nextSteps'
  import { speakFrom, speech, toggle } from './speech.svelte'
  import { readBlocks } from './speechRules'
  import ReadAlong from './ReadAlong.svelte'
  import type { IconName } from './icons'
  import { t } from './t'
  import { canCopy, canShare, pageDoc, type FileAct } from './fileActions'
  import PageFrame from './PageFrame.svelte'
  import { autoPreview, canPreview, fileKey, isImage, isVideo, pageHeight, previewKind } from './media'
  import AnteprimaAnimata from './AnteprimaAnimata.svelte'
  import PaginaIntera from './PaginaIntera.svelte'
  import { draftIn, previewOf } from './anteprimeAnimate'
  import { movimento } from './riduciMovimento.svelte'
  // Franz 24/10 00:40 (anteprime in app): il riquadro di una pagina è alto il 60% della finestra; «Schermo intero» la apre a tutto schermo.
  let vh = $state(typeof window !== 'undefined' ? window.innerHeight : 800)
  $effect(() => { const on = () => (vh = window.innerHeight); window.addEventListener('resize', on); return () => window.removeEventListener('resize', on) })
  let full = $state<{ key: string; name: string; path: string } | null>(null)
  // L'immagine aperta nel visore con lo zoom (app-chat-zoom-02-11); prima si apriva in una scheda nuova.
  let zoomed = $state<{ src: string; name: string } | null>(null)
  // Da quanto un'anteprima è in arrivo, in secondi, dopo i primi COMING_SHOW_S: il relay può tenerla in fila oltre un minuto (12/11).
  const COMING_SHOW_S = 10
  let comingNow = $state(Date.now())
  $effect(() => {
    if (!Object.keys(coming).length) return
    comingNow = Date.now()
    const i = setInterval(() => (comingNow = Date.now()), 1000)
    return () => clearInterval(i)
  })
  import { onDestroy } from 'svelte'
  import { firstNew, latest, readSeen, saveSeen } from './chatNews'
  import { coming, failed, media, missed, moving, pages, texts } from './mediaCache.svelte'
  // Le pagine mostrate come sorgente col tasto «Codice», per chiave.
  let code = $state<Record<string, boolean>>({})
  import Md from './Md.svelte'
  import { turnFiles, type Bozze } from './bozzeLinks'
  import { replaceNames, splitLabels, taskNames } from './chatLabels'
  import { outcomeText, splitOutcome } from './decide'
  import { outcomeType, type OutcomeType } from './kpi'
  import Icon from './Icon.svelte'
  const ESITO_ICON: Record<OutcomeType, 'x' | 'hand' | 'upload' | 'check' | 'message' | 'circle' | 'run'> = { problem: 'x', waiting: 'hand', shipped: 'upload', done: 'check', answer: 'message', none: 'circle', running: 'run' }

  // La conversazione della scheda (SessionSheet.kt): i messaggi a destra con lo stato, Claude a tutta larghezza con Copia
  // e ▶, i passaggi raccolti in una card, i file come chip, il costo del turno, e in fondo la riga dal vivo.
  let { flash = null, focus = null, onStep = null, steps: showSteps = true, s, items, now, onFile = () => {}, onFiles = () => {}, onDiff = null, sessions = [], onSession = () => {}, commitments = null, onTask = null }: { /** app-riall-3-doppioni-16-11: i PROSSIMI nel turno; in una sessione stanno solo sopra il campo. */ steps?: boolean; flash?: string | null
    /** La voce che Cerca sta portando in vista: il lavoro che la contiene si apre. */
    focus?: string | null
    /** Una voce di «Prossimi» toccata nel piede: va nel campo. */
    onStep?: ((text: string) => void) | null
    s: Session; items: Item[]; now: number; onFile?: (path: string, act: FileAct | 'prepare' | 'preview' | 'seen', key: string) => void; onFiles?: (fs: { path: string; key: string }[], act: 'save' | 'share' | 'prepare') => void
    /** Contratto 1.72: «Mostra tutto» sotto un passaggio, l'op `diff`; null = il relay non la conosce. */
    onDiff?: ((id: string) => Promise<Load<StepDiff>>) | null
    /** Le sessioni dello stato e l'apertura di una: il link di una di loro nel testo diventa il tasto «Apri in supervisor» (Franz, 21/10 19:04). */
    sessions?: Session[]; onSession?: (name: string) => void; commitments?: { task: string; short?: string | null }[] | null; onTask?: ((code: string) => void) | null } = $props()

  // Un file: sotto il nome i tasti apri, scarica, copia e condividi (condividi solo dove il browser lo sa fare; Franz, 07/10
  // 15:48). Il file si chiede al PC appena il puntatore ci arriva, così il tocco trova i byte pronti.
  const fileActs: [FileAct, string, IconName][] = [['open', t.fileOpen, 'external'], ['download', t.fileDownload, 'download'], ['copy', t.fileCopy, 'copy'], ...(canShare() ? [['share', t.fileShare, 'share'] as [FileAct, string, IconName]] : [])]

  // A + B (Franz, 10/10 16:01), come sul telefono: all'apertura le ultime sei voci salgono a cascata dall'alto, 30 ms l'una
  // dall'altra, nel primo secondo; una voce che arriva dopo entra da sola. Le voci arrivate dall'ultima visita hanno sopra la
  // riga «Nuovi» e un fondo che sfuma; l'ultima visita si ricorda lasciando la chat (Chat rimonta il Feed a ogni sessione).
  // La scheda unica (app-chat-scheda-unica-13-11): le voci divise in turni, la richiesta a fumetto e sotto una scheda sola.
  const rows = $derived(turns(items))
  // app-bozze-link-19-11: le bozze citate in una risposta aprono le immagini o gli HTML mandati nello stesso turno.
  const entryList = $derived(items.flatMap(i => ('entry' in i && i.entry ? [i.entry] : [])))
  function bozzeOf(e: TranscriptEntry): Bozze | null {
    const fs = turnFiles(entryList, entryList.indexOf(e))
    if (!fs.length) return null
    return { files: fs.map(x => x.file.path), open: (i: number) => {
      const { file, entry } = fs[i]
      const key = fileKey(s.name, file, entry), url = media[key]
      if (url && (isImage(file.mime) || /\.(png|jpe?g|webp|gif)$/i.test(file.path))) { zoomed = { src: url, name: file.path.split('/').pop() ?? '' }; onFile(file.path, 'seen', key) }
      else onFile(file.path, 'open', key)
    } }
  }
  const rowKey = (r: Row) => (r.type === 'turn' ? r.key : `x-${r.entry.id}`)
  // svelte-ignore state_referenced_locally
  const seenAt = readSeen(s.name)
  const newsFrom = $derived.by(() => { const i = firstNew(items, seenAt); if (i == null) return null; const r = rowOf(rows, items[i]); return r < 0 ? null : r })
  // All'apertura niente onda (app-sessione-apertura-06-11); sale solo il turno che arriva dopo.
  let born: Set<string> | null = null
  // Voce 1 (app-animazioni-fedeli-26-11): scorre parola per parola solo la risposta arrivata dopo l'apertura, una volta sola.
  let bornAns: Set<string> | null = null
  // app-apertura-master-scatti-26-11 (Franz 26/11 23:27): all'apertura la lista può arrivare a pezzi. Franz 27/11 15:17: arriva con
  // un'animazione solo quello che nasce a chat aperta e ferma (arrivo.ts, come StreamGate sul telefono): finita l'apertura, e della
  // sessione solo da un turno visto aperto; quello che compare prima si vede intero e fermo.
  const gate = arrivalGate()
  $effect(() => {
    const id = setTimeout(() => { gate.settle(); gate.working(s.state, s.turn_started) }, OPENING_MS)
    return () => clearTimeout(id)
  })
  $effect(() => { gate.working(s.state, s.turn_started) })
  function liveAnswer(id: string, at?: number | null): boolean {
    if (!bornAns) { bornAns = new Set(rows.flatMap((x) => (x.type === 'turn' && x.answer ? [x.answer.id] : []))); gate.prime(bornAns) }
    return gate.claim(id, at)
  }
  // Deciso una volta per riga, al suo primo disegno: entra un messaggio mandato da qui, o quello della sessione quando la sessione lavora.
  const rise = new Map<string, string>()
  function motion(r: Row): string {
    if (!born) born = new Set(rows.map(rowKey))
    const k = rowKey(r)
    if (born.has(k)) return ''
    let m = rise.get(k)
    if (m == null) { m = gate.settled && ((r.type === 'turn' && r.request?.type === 'mine') || gate.live) ? 'rise' : ''; rise.set(k, m) }
    return m
  }
  onDestroy(() => saveSeen(s.name, latest(items)))

  // Immagini e video nascono già in anteprima, come sul telefono (Franz, 10/10 15:24): il file si chiede al PC appena la
  // card è a schermo, se `autoPreview` lo vuole; col risparmio dati del browser i video aspettano il tocco.
  function preview(_node: HTMLElement, { f, key }: { f: TranscriptFile; key: string }) {
    const saveData = (navigator as Navigator & { connection?: { saveData?: boolean } }).connection?.saveData === true
    if (!media[key] && !texts[key] && autoPreview(f.mime, f.size, !saveData, f.path)) onFile(f.path, 'preview', key)
  }
  // app-anteprime-webp-27-11: un WebP o una GIF con la sua bozza HTML nello stesso invio (lo stesso nome) aprono la bozza dal vivo a
  // schermo intero, chiesta al PC se non c'è ancora; l'HTML che ha accanto la sua anteprima animata non apre il riquadro dal vivo in
  // chat: la sua anteprima leggera è l'immagine che gira.
  function draftOfFile(f: TranscriptFile, fs: { f: TranscriptFile; entry: string }[]) {
    const d = draftIn(f, fs.map(x => x.f))
    const x = d ? fs.find(y => y.f === d) : null
    return x ? { path: x.f.path, key: fileKey(s.name, x.f, x.entry), name: x.f.path.split('/').pop() ?? '' } : null
  }
  const lightOf = (f: TranscriptFile, fs: { f: TranscriptFile; entry: string }[]) => previewOf(f.path, fs.map(x => x.f)) != null
  function openDraft(d: { path: string; key: string; name: string }) {
    full = d
    if (!pages[d.key]) onFile(d.path, 'preview', d.key)
  }

  const hm = (at: number) => new Date(at * 1000).toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })
  // Il testo che legge il ▶ sotto la scheda: senza il tipo e senza i consigli.
  const spokenOf = (e: TranscriptEntry) => parseSteps(stripTipo(e.text ?? '', t.outcomeLabel).text).text
  // Contratto 1.126 (app-barra-conversazioni-05-11, SeparatorBar nell'app): motivo e ora, la data se non è oggi.
  function sepHead(e: TranscriptEntry): string | null {
    const r = sepReason(e)
    if (!r) return null
    if (e.at == null) return t.sepReason[r]
    const d = new Date(e.at * 1000)
    const today = new Date().toDateString() === d.toDateString()
    return `${t.sepReason[r]} · ${today ? '' : `${d.getDate()}/${d.getMonth() + 1} `}${hm(e.at)}`
  }
  const toolIcon = (e: TranscriptEntry): IconName => (e.error ? 'error' : kind(e.tool))
  const fileIcon = (f: TranscriptFile): IconName =>
    f.mime?.startsWith('image/') ? 'image' : f.mime === 'application/pdf' ? 'pdf' : f.mime?.startsWith('video/') ? 'film' : f.mime?.startsWith('audio/') ? 'audio' : 'file'
  const size = (b: number) => (b >= 1_000_000 ? `${(b / 1_000_000).toFixed(1).replace('.', ',')} MB` : b >= 1000 ? `${Math.floor(b / 1000)} KB` : `${b} B`)
  const since = (d: number) => (d < 3600 ? `${Math.floor(d / 60)} m` : d < 86400 ? `${Math.floor(d / 3600)} h ${String(Math.floor((d % 3600) / 60)).padStart(2, '0')}` : `${Math.floor(d / 86400)} ${DAY}`)
  function cost(e: TranscriptEntry): string | null {
    const tr = e.turn
    if (!tr) return null
    const secs = (tr.ended ?? 0) - (tr.started ?? 0)
    const parts = [tr.started != null && tr.ended != null && secs > 0 ? (secs < 60 ? `${secs} s` : since(secs)) : null, tr.out != null ? t.turnTokens(tr.out) : null]
    return parts.filter(Boolean).join(' · ') || null
  }
  const status: Record<Status, [IconName, string]> = {
    scheduled: ['alarm', 'var(--wait)'], offline: ['offline', 'var(--stale)'], uploading: ['upload', 'var(--text2)'], sending: ['clock', 'var(--text2)'],
    sent: ['check', 'var(--text2)'], uncertain: ['hourglass', 'var(--wait)'], failed: ['error', 'var(--gone)'], delivered: ['checks', 'var(--text2)'],
    queued: ['checks', 'var(--stale)'], working: ['checks', 'var(--busy)'], done: ['checks', 'var(--icon)'],
  }
  let open = $state<Record<string, boolean>>({})
  // Il messaggio letto una volta: senza le righe Tipo e Prossimi, coi link delle sessioni e delle richieste tolti, l'esito a parte.
  function read(e: TranscriptEntry) {
    const tp = stripTipo(e.text ?? '', t.outcomeLabel)
    const ps = parseSteps(tp.text)
    const sl = stripSessionLinks(outcomeForPhone(ps.text, t.outcomeLabel), sessions)
    const wl = stripWaitLinks(sl.text)
    return { text: ps.text, tipo: tp.tipo, steps: ps.steps, buttons: sl.buttons, wl, typed: outcomeType(e.outcome_type), es: splitOutcome(wl.text, t.outcomeLabel) }
  }
  type Read = ReturnType<typeof read>
  const reqText = (q: Request) => (q.type === 'mine' ? q.sent.text : q.entry.text ?? '')
  const reqAt = (q: Request) => (q.type === 'mine' ? (q.status === 'scheduled' ? null : q.sent.sentAt) : q.entry.at ?? null)
  const reqMeta = (q: Request) => { const at = reqAt(q); return [t.youLabel, q.type === 'user' && q.entry.origin ? t.origin[q.entry.origin] : null, at ? hm(at) : null].filter(Boolean).join(' · ') }
  // L'indice della risposta: il tocco porta al titolo.
  function jump(ev: MouseEvent, i: number) {
    (ev.currentTarget as HTMLElement).closest('.za')?.querySelectorAll('.sec')[i]?.scrollIntoView({ block: 'start', behavior: reducedMotion() ? 'auto' : 'smooth' })
  }

  // Il tempo del turno al secondo, mentre la sessione lavora; fermo altrimenti.
  // Il contatore parte dall'ora dello stato (`now`) e avanza col tempo passato da quando la chat è aperta.
  const opened = Date.now()
  let tick = $state(0)
  const live = $derived(s.state === 'busy' || s.state === 'awaiting')
  $effect(() => { if (!live) return; const id = setInterval(() => (tick = Math.round((Date.now() - opened) / 1000)), 1000); return () => clearInterval(id) })
  const elapsed = (x: number) => (x < 60 ? `${x} s` : x < 3600 ? `${Math.floor(x / 60)} min ${x % 60} s` : `${Math.floor(x / 3600)} h ${Math.floor((x % 3600) / 60)} min`)
  const liveText = $derived([s.turn_started != null ? elapsed(Math.max(0, now + tick - s.turn_started)) : null, s.tool_note?.trim() || s.tool?.trim() || t.thinking].filter(Boolean).join(' · '))
</script>

{#snippet actions(text: string, at: number | null | undefined)}
  {@const reading = speech.text === text}
  <div class="acts">
    <CopyButton {text} />
    <button class="play" aria-label={reading ? t.stopReading : t.readAloud} onclick={() => toggle(text, s.name)}>
      {#if reading}<svg viewBox="0 0 24 24" width="18" height="18"><rect x="7" y="7" width="10" height="10" rx="1.5" fill="currentColor" /></svg>
      {:else}<svg viewBox="0 0 24 24" width="18" height="18"><path d="M8 5.5v13l10.5-6.5z" fill="currentColor" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" /></svg>{/if}
    </button>
    {#if at}<span class="time">{hm(at)}</span>{/if}
  </div>
{/snippet}

{#snippet table(tb: Table)}
  {#if compact(tb)}
    <div class="grid" style="--n:{tb.header.length}">
      {#each tb.header as h}<span class="th"><Md text={h} /></span>{/each}
      {#each tb.rows as r}{#each r as c}<span class="td"><Md text={c} /></span>{/each}{/each}
    </div>
  {:else}
    <div class="cards">
      {#each cards(tb) as c}
        <div class="tcard">
          {#if c.title.trim()}<b><Md text={c.title} /></b>{/if}
          {#each c.lines as line}{@const i = line.indexOf(': ')}<span><span class="k">{`${i < 0 ? line : line.slice(0, i)}: `}</span><Md text={i < 0 ? '' : line.slice(i + 2)} /></span>{/each}
        </div>
      {/each}
    </div>
  {/if}
{/snippet}

<!-- Il testo della sessione a pezzi: paragrafi, voci d'elenco col loro segno, comandi con «Copia». -->
{#snippet richText(text: string, bz: Bozze | null = null)}
  {#each rich(text) as b}
    {#if b.type === 'p'}<p><Md text={b.text} {onTask} bozze={bz} /></p>
    {:else if b.type === 'li'}<div class="li" style="--d:{b.depth}"><span class="mk" class:n={b.mark !== '•'}>{b.mark}</span><p><Md text={b.text} {onTask} bozze={bz} /></p></div>
    {:else if b.type === 'quote'}
      <!-- Un testo da mandare a qualcuno (app-chat-testo-da-inviare-13-11): in un box suo, senza «>», e «Copia» lo copia pulito. -->
      <div class="sq" data-testid="chat-citazione"><div class="md">{@render richText(b.text, bz)}</div><CopyButton text={copyText(b.text)} words /></div>
    {:else}<div class="cmd"><Icon name="run" size={18} color="var(--tx3)" /><code>{b.text}</code><CopyButton text={b.text} words /></div>{/if}
  {/each}
{/snippet}

{#snippet prose(text: string, bz: Bozze | null = null)}
  {#each splitLabels(replaceNames(text, taskNames(commitments))) as part}
    {#if part.kind === 'you'}
      <div class="fyou"><div class="fyh"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M18 11V6a2 2 0 0 0-4 0M14 10V4a2 2 0 0 0-4 0v2M10 10.5V6a2 2 0 0 0-4 0v8" /><path d="M18 8a2 2 0 1 1 4 0v6a8 8 0 0 1-8 8h-2c-2.8 0-4.5-.86-5.99-2.34l-3.6-3.6a2 2 0 0 1 2.83-2.82L7 15" /></svg><b>{part.title}</b></div>{@render richText(part.body, bz)}</div>
    {:else if part.kind === 'untested'}
      <div class="funt"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="12" r="9" /><path d="M12 11v5M12 8h.01" /></svg><p><Md text={part.text} {onTask} bozze={bz} /></p></div>
    {:else}
      {#each blocks(part.text) as b}
        {#if b.type === 'text'}{@render richText(b.text, bz)}{:else}{@render table(b)}{/if}
      {/each}
    {/if}
  {/each}
{/snippet}

<!-- Il testo diviso ai titoli della sessione; `index` = l'indice dei titoli in cima, nella risposta. -->
{#snippet doc(text: string, index: boolean, bz: Bozze | null = null)}
  {@const d = sections(text)}
  {#if d.lead}<div class="md lead" class:big={index && d.secs.length > 0}>{@render prose(d.lead, bz)}</div>{/if}
  {#if index && d.secs.length > 1}
    <div class="idx">{#each d.secs as sc, i}<button onclick={(ev) => jump(ev, i)}><svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true"><path d="M12 5v14M6 13l6 6 6-6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg><span><Md text={sc.title} /></span></button>{/each}</div>
  {/if}
  {#each d.secs as sc}<div class="sec" class:first={!d.lead}><h4><Md text={sc.title} {onTask} /></h4><div class="md">{@render prose(sc.body, bz)}</div></div>{/each}
{/snippet}

{#snippet extras(e: TranscriptEntry, r: Read)}
  <!-- Un tasto per sessione linkata: il clic apre la sua chat qui; è un link vero, così il tasto destro o la pressione
       lunga danno il link com'era (Copia, Apri in una scheda nuova). Copia e ▶ tengono il testo intero. -->
  {#each r.buttons as b}
    <a class="sbtn" href={b.url} target="_blank" rel="noopener" title={t.openInSupervisorHint} onclick={(ev) => { ev.preventDefault(); onSession(b.session) }}><span class="sn">{b.session}</span><span class="sl">{t.openInSupervisor}</span></a>
  {/each}
  <!-- Le prove citate come schede compatte coi loro tasti (app-prova-scheda-in-chat-11-11). -->
  <ProvaCards text={r.wl.text} />
  <!-- Le richieste citate come schede compatte, al posto del loro link (app-scheda-scelta-12-11). -->
  <SceltaCards ids={r.wl.ids} />
  {#if e.cut}<span class="meta">{t.textCut}</span>{/if}
{/snippet}

<!-- Una frase della sessione fra un passo e l'altro, al suo posto nella linea del lavoro. -->
{#snippet say(e: TranscriptEntry)}
  {@const r = read(e)}
  <div class="nd say" class:found={flash === e.id} data-entry={e.id}>
    <span class="ic"></span>
    <div class="saytext" data-quote={r.text} data-quote-at={e.at ?? ''}>
      {@render doc(r.es.body, false, bozzeOf(e))}
      {#if r.es.outcome}<div class="fesito"><span class="gl t-{r.typed}"><Icon name={ESITO_ICON[r.typed]} size={18} /></span><p><Md text={outcomeText(r.es.outcome, t.outcomeLabel)} {onTask} /></p></div>{/if}
      {@render extras(e, r)}
    </div>
  </div>
{/snippet}

<!-- La risposta: il documento coi suoi titoli e, sotto, il piede con Esito, Tipo e Prossimi; fra i due cambia solo il fondo. -->
{#snippet answer(e: TranscriptEntry)}
  {@const r = read(e)}
  {@const sp = spokenOf(e)}
  <!-- app-messaggio-master-effetto-27-11 (opzione E): la risposta nata a chat aperta e ferma compare, sale con la lista e accende l'esito (arriva, molla.ts). -->
  <div class="ans" class:found={flash === e.id} data-entry={e.id} data-quote={r.text} data-quote-at={e.at ?? ''} use:arriva={{ on: liveAnswer(e.id, e.at) }}>
    {#if r.es.body.trim() || r.buttons.length || r.wl.ids.length || e.cut}
      <section class="z za">
        {#if speech.text === sp}{@render reading(sp)}{:else}{@render doc(r.es.body, true, bozzeOf(e))}{/if}
        {@render extras(e, r)}
      </section>
    {/if}
    {#if r.es.outcome || r.tipo || (showSteps && r.steps.length)}
      <section class="z zf z-{r.typed}">
        {#if r.es.outcome}<span class="k">{t.outcomeLabel}</span><div class="fesito" data-testid="chat-esito"><span class="gl t-{r.typed}"><Icon name={ESITO_ICON[r.typed]} size={20} /></span><p><Md text={outcomeText(r.es.outcome, t.outcomeLabel)} {onTask} /></p></div>{/if}
        {#if r.tipo}<span class="k">{t.typeLabel}</span><span class="tipo t-{r.typed}">{r.tipo}</span>{/if}
        {#if showSteps && r.steps.length}<span class="k">{t.nextLabel}</span><div class="nx">{#each r.steps as x}<button disabled={!onStep} onclick={() => onStep?.(x)}>{x}</button>{/each}</div>{/if}
      </section>
    {/if}
  </div>
{/snippet}

<!-- Sotto la scheda: Copia e ▶ a sinistra, quanto è durato il turno e l'ora a destra. -->
<!-- app-web-parola-lettura-27-11 (Franz 27/11 11:50): durante la lettura a voce la risposta va a paragrafi come le schede del telefono
     (AnswerZones): quello letto in evidenza e dentro la parola detta con la pillola di Read along; il tocco su un paragrafo riparte da lì.
     Con le voci che non mandano `boundary` resta il paragrafo. -->
{#snippet reading(text: string)}
  <div class="rd" data-testid="lettura-paragrafi">
    {#each readBlocks(text, t.outcomeLabel) as b, i (i)}
      {@const shown = b.text.replaceAll('`', '')}
      <button class="rp {b.kind}" class:cur={i === speech.block} onclick={() => speakFrom(text, i, s.name)}>
        {#if i === speech.block && speech.word != null}<ReadAlong text={shown} word={speech.word} />{:else}{shown}{/if}
      </button>
    {/each}
  </div>
{/snippet}

{#snippet tail(e: TranscriptEntry)}
  {@const text = spokenOf(e)}
  <div class="tm">{@render actions(text, null)}<span class="tmr">{[cost(e), e.at ? hm(e.at) : null].filter(Boolean).join(' · ')}</span></div>
{/snippet}

<!-- Ogni file con la sua voce: senza la misura è la voce a dire se è lo stesso file (fileKey). -->
{#snippet files(fs: { f: TranscriptFile; entry: string }[])}
  <div class="files">
    <!-- Due o più file di seguito (Franz, 16/10 16:01): una riga di gruppo per salvarli o condividerli insieme; le card restano sotto. -->
    {#if fs.length > 1}
      {@const all = fs.map(({ f, entry }) => ({ path: f.path, key: fileKey(s.name, f, entry) }))}
      <div class="fgroup" role="group" aria-label={t.filesGroup(fs.length)} onpointerenter={() => onFiles(all, 'prepare')} onfocusin={() => onFiles(all, 'prepare')}>
        <Icon name="file" color="var(--icon)" /><span class="fgname">{t.filesGroup(fs.length)}</span>
        <button class="fgbtn" onclick={() => onFiles(all, 'save')}><Icon name="download" size={18} />{t.saveAll}</button>
        {#if canShare()}<button class="fgbtn" onclick={() => onFiles(all, 'share')}><Icon name="share" size={18} />{t.shareAll}</button>{/if}
      </div>
    {/if}
    {#each fs as { f, entry }}
      {@const key = fileKey(s.name, f, entry)}
      {@const url = media[key]}
      {@const kind = previewKind(f.mime, f.path)}
      {@const fname = f.path.split('/').pop() ?? ''}
      {@const dr = kind === 'image' ? draftOfFile(f, fs) : null}
      <div class="fcard" role="group" aria-label={f.path.split('/').pop()} use:preview={{ f, key }} onpointerenter={() => onFile(f.path, 'prepare', key)} onfocusin={() => onFile(f.path, 'prepare', key)}>
        <button class="fchip" title={f.path} onclick={() => onFile(f.path, 'open', key)}><Icon name={fileIcon(f)} color="var(--icon)" /><span class="fname">{f.path.split('/').pop()}</span>{#if f.size != null}<span class="fsize">{size(f.size)}</span>{/if}</button>
        <!-- L'anteprima tra il nome e i tasti, come nell'app (Franz, 13/10 07:16). Un WebP o una GIF animati girano finché sono a schermo,
             fermi col ▶ con le animazioni ridotte (app-anteprime-webp-27-11); con la loro bozza accanto il tocco la apre dal vivo. -->
        {#if url && (isImage(f.mime) || kind === 'pdf')}<button class="fthumb" class:pdf={kind === 'pdf'} aria-label={kind === 'pdf' ? t.fileOpen : dr ? t.animDraft(fname) : moving[key] && movimento.ridotto ? t.animPlay(fname) : t.fileOpen} onclick={() => { if (kind === 'pdf') onFile(f.path, 'open', key); else if (dr) { openDraft(dr); onFile(f.path, 'seen', key) } else { zoomed = { src: url, name: fname }; onFile(f.path, 'seen', key) } }}>{#if kind === 'pdf'}<img src={url} alt="" />{:else}<AnteprimaAnimata src={url} blob={moving[key]} fill={false} fit="cover" />{/if}</button>{/if}
        {#if pages[key] && !code[key]}
          <!-- Franz, 16/10 11:18: HTML e SVG come pagina, in un riquadro isolato; da app-visore-web-script-28-11 sul sito delle anteprime,
               dove gli script della pagina girano (PageFrame). -->
          {#if !(kind === 'page' && lightOf(f, fs))}<PageFrame doc={pageDoc(pages[key].mime, pages[key].text)} title={f.path.split('/').pop() ?? ''} height={pageHeight(vh)} />{/if}
        {:else if texts[key]}<button class="ftext" class:cut={texts[key].more} aria-label={t.fileOpen} onclick={() => onFile(f.path, 'open', key)}><pre>{texts[key].lines}</pre></button>{/if}
        {#if pages[key]}<span class="fbtns"><button class="fshow" aria-pressed={!!code[key]} onclick={() => (code[key] = !code[key])}>{code[key] ? t.filePage : t.fileCode}</button><button class="fshow" onclick={() => (full = { key, name: fname, path: f.path })}>{t.filePageFull}</button></span>{/if}
        {#if !url && !texts[key] && !failed[key] && !coming[key] && missed[key]}
          <!-- Non arrivata dal PC entro il limite, o rifiutata (Franz, 12/11: «se va in time out può segnalarlo con tasto che la richiede nuovamente?»). -->
          <span class="fmiss"><span>{t.previewMissed}</span><button class="fshow" onclick={() => onFile(f.path, 'preview', key)}>{t.previewRetry}</button></span>
        {:else if !url && !texts[key] && !failed[key] && !coming[key] && canPreview(f.mime, f.path) && !autoPreview(f.mime, f.size, !((navigator as Navigator & { connection?: { saveData?: boolean } }).connection?.saveData === true), f.path)}
          <!-- Oltre i limiti o col risparmio dati l'anteprima aspetta il tocco. -->
          <button class="fshow" onclick={() => onFile(f.path, 'preview', key)}>{t.showPreview}</button>
        {/if}
        {#if coming[key] && !url && !texts[key]}{@const waited = Math.floor((comingNow - coming[key]) / 1000)}<span class="fcoming">{waited >= COMING_SHOW_S ? t.previewComingFor(waited) : t.previewComing}</span>{/if}
        {#if url && isVideo(f.mime)}<!-- svelte-ignore a11y_media_has_caption --><video class="fvideo" src={url} controls preload="metadata" playsinline></video>{/if}
        <div class="acts">{#each fileActs.filter(([a]) => a !== 'copy' || canCopy(f.mime ?? '')) as [a, label, icon]}<button class="sm" aria-label={label} title={label} onclick={() => onFile(f.path, a, key)}><Icon name={icon} /></button>{/each}</div>
      </div>
    {/each}
  </div>
{/snippet}

<!-- Un passo del lavoro, a una riga: l'icona del tipo sulla linea, nome e argomento, la pillola del risultato; il tocco lo apre. -->
{#snippet tool(e: TranscriptEntry)}
  {@const ln = stepLine(e)}
  {@const dt = stepDetail(e)}
  {@const can = !!(dt.note || dt.text || e.diff)}
  <div class="nd tool" class:bad={e.error} class:found={flash === e.id} data-entry={e.id}>
    <span class="ic"><Icon name={toolIcon(e)} size={17} color={e.error ? 'var(--gone)' : 'var(--good)'} label={e.tool ?? undefined} /></span>
    <button class="tc" disabled={!can} aria-expanded={can ? !!open[e.id] : undefined} onclick={() => (open[e.id] = !open[e.id])}>
      <span class="tn">{ln.name}</span>{#if ln.prose}<span class="ta">{ln.arg}</span>{:else}<code>{ln.arg}</code>{/if}
      {#if e.error}<span class="pill bad">{t.stepFailed}</span>{:else if e.diff}<span class="pill"><span class="a">+{e.diff.added}</span> <span class="d">−{e.diff.removed}</span></span>{/if}
      {#if can}<svg class="chev" viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"><path d={open[e.id] ? 'M7 14l5-5 5 5' : 'M7 10l5 5 5-5'} fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>{/if}
    </button>
    {#if open[e.id] && can}
      <div class="td">
        {#if dt.note}<p>{dt.note}</p>{/if}
        {#if dt.text}<pre>{dt.text}</pre>{/if}
        <!-- Contratto 1.72: il diff del passo, con «Mostra tutto». -->
        {#if e.diff}<StepDiffRow id={e.id} diff={e.diff} {onDiff} startOpen />{/if}
      </div>
    {/if}
    {#if e.files?.length}{@render files(e.files.map(f => ({ f, entry: e.id })))}{/if}
  </div>
{/snippet}

<!-- La richiesta: il tuo fumetto a destra. data-quote: il testo che «Cita tutto» mette sopra il campo, con l'ora. -->
{#snippet request(q: Request)}
  {@const text = reqText(q)}
  {@const cps = citeParts(text)}
  <!-- data-mine (app-trova-messaggi-20-11): la fermata del salto fra i miei messaggi, la voce o il mandato se non è ancora arrivato. -->
  <div class="me" class:found={flash != null && (q.entry?.id === flash || (q.type === 'mine' && q.sent.id === flash))} data-entry={q.entry?.id} data-mine={q.entry?.id ?? (q.type === 'mine' ? q.sent.id : undefined)}>
    {#if q.type === 'mine'}
      {#if q.sent.attachment}<span class="att"><Icon name="file" size={16} />{q.sent.attachment}</span>{/if}
      {#each q.sent.attachments ?? [] as a}<span class="att"><Icon name="file" size={16} />{a}</span>{/each}
    {/if}
    {#if text}
      <div class="bubble" class:low={q.type === 'user'} data-quote={text} data-quote-at={reqAt(q) ?? ''}>
        <div class="m">{reqMeta(q)}</div>
        <!-- app-citazioni-estetica-26-11: le righe con «>» sono un blocco di citazione, il resto la risposta. -->
        {#each cps as cp, ci (ci)}
          {#if cp.quote}<QuoteBlock text={cp.text} />
          {:else}<p class="t" class:long={cp.text.length > 140 || cp.text.includes('\n') || cps.length > 1}><Md text={cp.text} /></p>{/if}
        {/each}
      </div>
    {/if}
    <div class="mrow">
      {#if q.type === 'mine'}
        {@const [ic, tone] = status[q.status]}
        <span class="mark" style="color:{tone}"><span class="pop" use:popOnChange={q.status}><Icon name={ic} size={16} color={tone} /></span>{q.status === 'scheduled' ? t.chatStatus.scheduled(hm(q.sent.scheduledFor ?? q.sent.sentAt)) : t.chatStatus[q.status]('')}</span>
      {:else if q.entry.queued}<span class="meta stale">{t.chatStatus.queued('')}</span>{/if}
      <CopyButton {text} />
    </div>
  </div>
{/snippet}

{#each rows as r, i (rowKey(r))}
  <div class="it" use:entra={{ on: motion(r) === 'rise', me: r.type === 'turn' && r.request != null }} class:turn={r.type === 'turn'} class:fresh={newsFrom != null && i >= newsFrom}>
  {#if i === newsFrom}<div class="news"><span>{t.chatNews}</span></div>{/if}
  {#if r.type === 'separator'}
    {@const head = sepHead(r.entry)}
    {@const mem = sepMemory(r.entry)}
    {@const light = r.entry.reason === 'compact'}
    <div class="sep" class:light data-entry={r.entry.id}>
      <div class="sepbar"><span>{light ? [head ?? r.entry.text, mem ? t.sepMemory[mem] : null].filter(Boolean).join(' · ') : head ?? r.entry.text ?? ''}</span></div>
      {#if !light && head && mem}<p class="sepmem">{t.sepMemory[mem]}</p>{/if}
    </div>
  {:else}
    {#if r.request}{@render request(r.request)}{/if}
    <!-- Un turno partito senza richiesta (un avviso automatico, una notifica): una testata piccola con l'ora al posto del fumetto. -->
    {#if r.auto}{@const at = r.work[0]?.entry.at ?? r.answer?.at}<div class="autoh" data-testid="chat-avviso-automatico">{[t.autoTurn, at ? hm(at) : null].filter(Boolean).join(' · ')}</div>{/if}
    {#if r.work.length || r.answer}
      {@const n = toolCount(r.work)}
      {@const bad = failedCount(r.work)}
      {@const fold = foldable(r.work)}
      {@const all = !!open[`w-${r.key}`] || (focus != null && entryIds(r).includes(focus))}
      <!-- Tutto quello che scrive la sessione in una scheda sola, a strati: il lavoro, la risposta, il piede. -->
      <div class="scard" data-testid="chat-scheda">
        {#if r.work.length}
          <section class="z zw">
            {#if n}
              <button class="zh" disabled={!fold} aria-expanded={fold ? all : undefined} onclick={() => (open[`w-${r.key}`] = !all)}>
                <span>{t.workSteps(n)}{#if bad}{' · '}<b class="sfail">{t.stepsFailed(bad)}</b>{/if}</span>
                {#if fold}<svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path d={all ? 'M7 14l5-5 5 5' : 'M7 10l5 5 5-5'} fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>{/if}
              </button>
            {/if}
            <div class="tl">{#each shownWork(r.work, all) as w (w.entry.id)}{#if w.kind === 'say'}{@render say(w.entry)}{:else}{@render tool(w.entry)}{/if}{/each}</div>
          </section>
        {/if}
        {#if r.answer}{@render answer(r.answer)}{/if}
      </div>
      {#if r.answer}{@render tail(r.answer)}{/if}
    {/if}
  {/if}
  </div>
{/each}

{#if live}
  <div class="live">
    <svg class="star" viewBox="0 0 18 18" width="18" height="18" aria-hidden="true">
      {#each Array(8) as _, i}{@const a = (i * Math.PI) / 4}<line x1={9 + Math.cos(a) * 2.2} y1={9 + Math.sin(a) * 2.2} x2={9 + Math.cos(a) * 8.5} y2={9 + Math.sin(a) * 8.5} />{/each}
    </svg>
    <span>{liveText}</span>
  </div>
{/if}

{#if zoomed}<ImageViewer src={zoomed.src} name={zoomed.name} onClose={() => (zoomed = null)} />{/if}
<!-- Una pagina a schermo intero (PaginaIntera): «Schermo intero» di un HTML, o la bozza dal vivo di un'anteprima animata, che dice
     se sta ancora arrivando dal PC (app-anteprime-webp-27-11). -->
{#if full}{@const fl = full}<PaginaIntera name={fl.name} page={pages[fl.key]} missed={!!missed[fl.key] || !!failed[fl.key]} onRetry={() => onFile(fl.path, 'preview', fl.key)} onClose={() => (full = null)} />{/if}

<style>
  .fbtns { display: flex; gap: 6px; flex-wrap: wrap; }
  p { white-space: pre-wrap; overflow-wrap: anywhere; }
  .it { display: flex; flex-direction: column; border-radius: 14px; }
  /* Il passaggio trovato da Cerca: un lampo giallo che si spegne (app-cerca-pagina-03-11). */
  .found { animation: found 2.2s ease-out; border-radius: 12px; }
  @keyframes found { from { box-shadow: inset 0 0 0 100vmax rgba(255, 178, 63, .22); } to { box-shadow: inset 0 0 0 100vmax transparent; } }
  @media (prefers-reduced-motion: reduce) { .found { animation: none; box-shadow: inset 0 0 0 100vmax rgba(255, 178, 63, .14); } }
  .fresh { animation: fresh 2s ease-out 600ms both; }
  /* Il bagliore dei messaggi nuovi in grigio, senza l'azzurro di prima (app-audit-superfici-luci-04-11). */
  @keyframes fresh { from { background: rgb(255 255 255 / .08); box-shadow: 0 0 0 6px rgb(255 255 255 / .08); } to { background: transparent; box-shadow: 0 0 0 6px transparent; } }
  /* Le voci fuori schermo non si impaginano né si disegnano (app-fluidita-06-11): il browser le salta finché non entrano in vista,
     ricordando la loro ultima altezza; nelle colonne con chat lunghe è la lista virtualizzata. */
  /* Solo col ancoraggio dello scorrimento (html.anchor, main.ts): senza, su iPhone fino a iOS 26 la trascrizione saltava salendo. */
  :global(html.anchor) .it { content-visibility: auto; contain-intrinsic-size: auto 120px; }
  .news { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; color: var(--icon); font-size: 12.5px; font-weight: 500; }
  .news::before, .news::after { content: ''; flex: 1; height: 1px; background: currentColor; opacity: .45; }
  /* La barra fra due conversazioni della sessione; per la compattazione una riga sola, più leggera. */
  .sep { padding: 12px 0; display: flex; flex-direction: column; align-items: center; gap: 4px; color: var(--text2); }
  .sepbar { width: 100%; display: flex; align-items: center; gap: 8px; font-size: 12.5px; font-weight: 600; }
  .sepbar::before, .sepbar::after { content: ''; flex: 1; height: 1px; background: var(--line); }
  .sepbar > span { max-width: 80%; text-align: center; }
  .sepmem { margin: 0; font-size: 12.5px; text-align: center; }
  .sep.light { padding: 6px 0; opacity: .7; }
  .sep.light .sepbar { font-size: 11.5px; font-weight: 500; }
  .sep.light .sepbar::before, .sep.light .sepbar::after { opacity: .5; }
  @media (prefers-reduced-motion: reduce) { .fresh { animation: none; } }
  .me { align-self: stretch; display: flex; flex-direction: column; align-items: flex-end; }
  .att { display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; margin-bottom: 4px; border-radius: 14px; background: var(--surface); color: var(--text2); font-size: 14px; overflow-wrap: anywhere; }
  .mrow { display: flex; align-items: center; gap: 2px; }
  .pop { display: inline-flex; }
  .mark { display: flex; align-items: center; gap: 4px; font-size: 12.5px; font-weight: 500; }
  .time { font-size: 12.5px; color: var(--text2); padding: 0 4px; font-weight: 500; }
  .meta { font-size: 12.5px; color: var(--text2); font-weight: 500; }
  .stale { color: var(--stale); }
  /* Voce 1: lo scambio Copia ↔ spunta entra da 0,9 con la molla veloce e sfuma; con le animazioni ridotte il tempo è 1 ms. */
  .sm { width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center; color: var(--text2); }
  .sm:hover { background: var(--surface); }
  .sbtn { align-self: flex-start; display: flex; flex-direction: column; gap: 1px; background: var(--high); color: var(--text); border-radius: 16px; padding: 8px 18px; text-decoration: none; min-height: 44px; justify-content: center; }
  .sbtn:hover { filter: brightness(1.1); }
  /* In evidenza la sessione che si apre, sotto «Apri in supervisor» (Franz 02/11 13:48: «la gerarchia deve essere invertita»). */
  .sbtn .sn { font-size: 15px; font-weight: 500; letter-spacing: .01em; }
  .sbtn .sl { font-size: 12.5px; color: var(--text2); }
  .acts { display: flex; align-items: center; gap: 2px; }
  .play { width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center; background: var(--high); color: var(--icon); margin-left: 2px; }
  .play:hover, .fchip:hover { filter: brightness(1.15); }
  .grid { display: grid; grid-template-columns: repeat(var(--n), 1fr); }
  .th, .td { padding-right: 12px; }
  .th { font-size: 14px; font-weight: 600; color: var(--text2); padding: 6px 0; }
  .td { font-size: 14px; padding: 8px 0; border-top: 1px solid var(--line); }
  .cards { display: flex; flex-direction: column; gap: 8px; }
  .tcard { background: var(--surface); border-radius: 12px; padding: 10px 14px; display: flex; flex-direction: column; gap: 3px; font-size: 14px; }
  .tcard b { font-size: 15.5px; font-weight: 600; }
  .k { color: var(--text2); }
  /* Ogni allegato a tutta larghezza, con la sua anteprima (Franz, 15/10 22:20). */
  .files { display: flex; flex-direction: column; gap: 10px; }
  .fcard { display: flex; flex-direction: column; align-items: stretch; gap: 6px; width: 100%; container-type: inline-size; }
  .fchip { display: flex; align-items: center; gap: 10px; background: var(--surface); border-radius: 12px; padding: 12px 14px; text-align: left; width: 100%; }
  .fchip .fname { flex: 1; min-width: 0; }
  /* La riga del gruppo: il conto a sinistra, i due tasti tonali a destra; su un telefono stretto i tasti vanno sotto insieme. */
  .fgroup { display: flex; flex-wrap: wrap; align-items: center; gap: 8px 10px; padding: 4px 0; }
  .fgname { flex: 1 0 auto; font-weight: 500; }
  .fgbtn { display: inline-flex; align-items: center; gap: 6px; padding: 8px 14px; border-radius: 20px; background: var(--high); color: var(--text); font-weight: 500; font-size: 14px; white-space: nowrap; }
  .fgbtn:hover { filter: brightness(1.12); }
  .fthumb.pdf { background: #fff; }
  .fthumb.pdf img { object-fit: contain; object-position: top; max-height: 120cqw; }
  .ftext { position: relative; display: block; width: 100%; text-align: left; background: var(--low); border: 1px solid var(--line); border-radius: 14px; padding: 12px 14px; overflow: hidden; }
  .ftext pre { margin: 0; font: 12.5px/1.5 var(--mono); color: var(--text); white-space: pre-wrap; word-break: break-word; }
  /* Un testo più lungo delle righe mostrate sfuma in fondo, senza puntini. */
  .ftext.cut pre { mask-image: linear-gradient(#000 70%, transparent); -webkit-mask-image: linear-gradient(#000 70%, transparent); }
  .fcoming { align-self: flex-start; padding: 2px 2px 4px; font-size: 13.5px; color: var(--text2); font-variant-numeric: tabular-nums; }
  .fmiss { display: flex; flex-wrap: wrap; align-items: center; gap: 6px 12px; font-size: 13.5px; color: var(--text2); }
  .fshow { align-self: flex-start; padding: 8px 14px; border-radius: 999px; background: var(--high); color: var(--icon); font-size: 14px; font-weight: 500; }
  /* --am-max: l'altezza massima dell'immagine dentro AnteprimaAnimata, come quella di prima (app-anteprime-webp-27-11). */
  .fthumb { padding: 0; border-radius: 14px; overflow: hidden; background: none; --am-max: 200cqw; }
  /* Con l'anteprima la scheda prende tutta la larghezza della chat, come nell'app (Franz, 12/10 12:27: «anteprime con width
     100%»); un verticale è alto al massimo il doppio della larghezza. Nome, anteprima e tasti stanno a 6 px, come nell'app. */
  .fcard:has(> .fthumb, > .fvideo) { width: 100%; container-type: inline-size; gap: 6px; }
  .fcard > .fthumb { width: 100%; }
  .fthumb img { display: block; width: 100%; height: auto; max-height: 200cqw; object-fit: cover; }
  .fvideo { display: block; width: 100%; max-height: 200cqw; border-radius: 14px; background: #000; }
  .fname { font-size: 14px; font-weight: 500; overflow-wrap: anywhere; }
  .fsize { font-size: 12.5px; color: var(--text2); white-space: nowrap; }
  .sfail { color: var(--b-alert, var(--gone)); font-weight: 600; }
  .live { display: flex; align-items: center; gap: 10px; padding: 4px 0; font-size: 14px; color: var(--text2); }
  .star { stroke: var(--opus); stroke-width: 2; stroke-linecap: round; animation: spin 2.4s linear infinite, pulse 1.4s ease-in-out infinite alternate; }
  @keyframes spin { to { rotate: 360deg; } }
  @keyframes pulse { from { scale: .75; opacity: .6; } to { scale: 1.1; opacity: 1; } }
  @media (prefers-reduced-motion: reduce) { .star { animation: pulse 1.2s ease-in-out infinite alternate; } }
  .fyou { background: var(--s2); border-radius: 14px; padding: 12px; margin: 2px 0; }
  .fyh { display: flex; align-items: center; gap: 8px; color: var(--wait); font-weight: 600; margin-bottom: 8px; }
  .funt { display: flex; gap: 8px; color: var(--tx2); }
  .funt svg { flex: none; margin-top: 3px; color: var(--tx3); }
  /* La scheda unica (app-chat-scheda-unica-13-11, mockup F3 su base E1). I veli stanno sotto il 12 %: la richiesta prende il blu di
     «al lavoro», il piede il colore del tipo dell'esito, il resto sono i grigi della scala v3. Fra gli strati cambia solo il fondo. */
  .turn { width: 100%; max-width: 780px; align-self: center; font-size: 16px; line-height: 25px; color: var(--tx);
          --fyou: color-mix(in srgb, var(--busy) 13%, var(--bg)); --fok: color-mix(in srgb, var(--good) 10%, var(--bg)); --fseam: #131313; --fpaper: var(--s2); --ftile: var(--bg); }
  :global(html.theme-day) .turn { --fyou: color-mix(in srgb, var(--busy) 9%, #FFFFFF); --fok: color-mix(in srgb, var(--good) 9%, #FFFFFF); --fseam: #F7F7F7; --fpaper: #FFFFFF; --ftile: #FFFFFF; }
  .bubble { background: var(--fyou); width: fit-content; max-width: 86%; margin-left: auto; border-radius: 20px 20px 6px 20px; padding: 10px 16px 12px; }
  .bubble .m { font-size: 12.5px; line-height: 16px; color: var(--tx2); margin-bottom: 4px; }
  .bubble .t { font-family: var(--display); font-weight: 600; font-size: 19px; line-height: 25px; }
  /* Una richiesta lunga si legge come testo, non come titolo. */
  .bubble .t.long { font-family: inherit; font-weight: 400; font-size: 16px; line-height: 25px; }
  .me + .scard { margin-top: 10px; }
  .autoh { margin: 0 6px 8px; font-size: 12.5px; line-height: 16px; color: var(--tx3); }
  .scard { border-radius: 22px; overflow: hidden; }
  :global(html.theme-day) .scard { box-shadow: 0 0 0 1px var(--line); margin: 1px; }
  .z { background: var(--zbg); }
  .zw { --zbg: var(--fseam); padding: 18px; }
  .za { --zbg: var(--fpaper); padding: 20px 22px 18px; display: flex; flex-direction: column; gap: 10px; }
  .rd { display: flex; flex-direction: column; gap: 6px; }
  .rp { display: block; width: 100%; padding: 4px 6px; border: 0; border-radius: 8px; background: none; color: var(--tx2); font: inherit; font-size: 15.5px; line-height: 21px; text-align: left; white-space: pre-wrap; overflow-wrap: anywhere; cursor: pointer; }
  .rp.cur { background: var(--s3); color: var(--tx); }
  .rp.heading { font-weight: 600; }
  .rp.code, .rp.text { font-family: var(--mono); font-size: 13px; line-height: 19px; }
  .zf { --zbg: color-mix(in srgb, var(--tx) 4%, var(--fpaper)); padding: 14px 22px 16px; display: grid; grid-template-columns: 96px minmax(0, 1fr); gap: 12px; align-items: baseline; }
  .zf.z-done, .zf.z-shipped { --zbg: var(--fok); }
  .zf.z-problem { --zbg: color-mix(in srgb, var(--gone) 10%, var(--fpaper)); }
  .zf.z-waiting { --zbg: color-mix(in srgb, var(--wait) 10%, var(--fpaper)); }
  .zf.z-running { --zbg: color-mix(in srgb, var(--busy) 10%, var(--fpaper)); }
  .zh { display: flex; align-items: center; gap: 6px; width: 100%; text-align: left; margin: 0 0 12px; font: 500 11.5px/16px var(--mono); letter-spacing: .12em; text-transform: uppercase; color: var(--tx3); }
  .zh span { flex: 1; }
  .zh .sfail { letter-spacing: .12em; }
  /* Il lavoro: una linea del tempo, un nodo per voce. */
  .tl { position: relative; padding-left: 40px; }
  .tl::before { content: ''; position: absolute; left: 14px; top: 12px; bottom: 16px; width: 2px; border-radius: 1px; background: var(--line); }
  .nd { position: relative; margin-bottom: 12px; display: flex; flex-direction: column; gap: 8px; min-width: 0; }
  .nd:last-child { margin-bottom: 0; }
  .nd > .ic { position: absolute; left: -40px; top: 0; width: 30px; height: 30px; border-radius: 50%; display: grid; place-items: center; background: var(--fseam); border: 1.5px solid var(--line); }
  .nd.say > .ic::before { content: ''; width: 8px; height: 8px; border-radius: 50%; background: var(--tx3); }
  .nd.tool > .ic { border-color: color-mix(in srgb, var(--good) 50%, var(--line)); }
  .nd.tool.bad > .ic { border-color: color-mix(in srgb, var(--gone) 60%, var(--line)); }
  .saytext { display: flex; flex-direction: column; gap: 8px; color: var(--tx2); font-size: 15.5px; line-height: 24px; padding-top: 3px; min-width: 0; }
  .tc { display: grid; grid-template-columns: auto minmax(0, 1fr) auto auto; align-items: center; column-gap: 10px; min-height: 32px; width: 100%; text-align: left; padding: 4px 8px 4px 12px; border-radius: 12px; background: var(--ftile); border: 1px solid var(--line); color: var(--tx); }
  .tc:not(:disabled):hover { border-color: var(--tx3); }
  .tc .tn { font-family: var(--display); font-weight: 600; font-size: 15.5px; }
  .tc code { font: 13px/20px var(--mono); color: var(--tx2); min-width: 0; white-space: nowrap; overflow: hidden; }
  /* La descrizione di un comando è una frase: su una riga, coi puntini se è lunga (app-chat-bash-descrizione-13-11). */
  .tc .ta { font-size: 15px; line-height: 20px; color: var(--tx2); min-width: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .tc .chev { color: var(--tx3); }
  .pill { font-size: 13px; line-height: 19px; padding: 1px 9px; border-radius: 999px; background: var(--s3); color: var(--tx2); white-space: nowrap; font-variant-numeric: tabular-nums; }
  .pill.bad { background: color-mix(in srgb, var(--gone) 16%, transparent); color: var(--gone); }
  .pill .a { color: var(--good); } .pill .d { color: var(--gone); }
  .td { display: flex; flex-direction: column; gap: 6px; padding: 0 4px; font-size: 14px; line-height: 21px; color: var(--tx2); min-width: 0; }
  .td pre { margin: 0; font: 12.5px/19px var(--mono); color: var(--tx); white-space: pre-wrap; overflow-wrap: anywhere; }
  /* La risposta come documento. */
  .md { display: flex; flex-direction: column; gap: 10px; min-width: 0; }
  .lead.big > p:first-child { font-size: 17.5px; line-height: 27px; }
  .idx { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 4px; }
  .idx button { display: inline-flex; align-items: center; gap: 4px; font-size: 14px; line-height: 20px; padding: 4px 12px 4px 10px; border-radius: 999px; background: var(--s3); color: var(--tx2); text-align: left; }
  .idx button:hover { color: var(--tx); }
  .idx svg { flex: none; color: var(--tx3); }
  .sec { padding-top: 16px; margin-top: 6px; border-top: 1px solid var(--line); scroll-margin-top: 12px; }
  .sec.first:first-child { padding-top: 0; margin-top: 0; border-top: 0; }
  .sec h4 { font-family: var(--display); font-weight: 700; font-size: 19px; line-height: 24px; margin: 0 0 8px; }
  .saytext .sec { padding-top: 0; margin-top: 0; border-top: 0; }
  .saytext .sec h4 { font-size: 16.5px; line-height: 22px; margin-bottom: 4px; color: var(--tx); }
  .li { display: grid; grid-template-columns: 22px minmax(0, 1fr); margin: -4px 0 0 calc(var(--d) * 18px); }
  .li .mk { color: var(--tx3); }
  .li .mk.n { font-family: var(--display); font-weight: 600; color: var(--tx2); }
  .cmd { display: flex; align-items: center; gap: 10px; padding: 8px 8px 8px 12px; border-radius: 12px; background: var(--s2); border: 1px solid var(--line); }
  .za .cmd { background: var(--bg); }
  .cmd :global(svg) { flex: none; }
  .cmd code { flex: 1; min-width: 0; font: 13.5px/21px var(--mono); color: var(--tx); white-space: pre-wrap; overflow-wrap: anywhere; }
  /* app-citazioni-estetica-26-11: anche nelle risposte la citazione ha la barra a sinistra e il testo tenue; resta «Copia». */
  .sq { display: flex; gap: 10px; padding: 4px 0 4px 12px; border-left: 3px solid color-mix(in srgb, var(--tx) 28%, transparent); color: var(--tx2); }
  .za .sq { background: var(--bg); }
  .sq > .md { flex: 1; }
  /* Il piede: Esito, Tipo e Prossimi, le parole della sessione. */
  .zf .k { font: 500 11.5px/18px var(--mono); letter-spacing: .12em; text-transform: uppercase; color: var(--tx3); }
  /* L'esito: il colore del tipo solo sull'icona, centrata sulla prima riga (mockup scelto il 02/11). */
  .fesito { display: flex; gap: 8px; align-items: flex-start; min-width: 0; }
  .fesito p { flex: 1; min-width: 0; color: var(--text); }
  .fesito .gl { flex: none; display: flex; align-items: center; height: 1lh; }
  .zf .fesito p { font-weight: 600; font-size: 16.5px; line-height: 24px; }
  .tipo { justify-self: start; font-family: var(--display); font-weight: 600; font-size: 14px; line-height: 20px; padding: 1px 11px; border-radius: 999px; background: color-mix(in srgb, currentColor 18%, transparent); }
  .nx { display: flex; flex-wrap: wrap; gap: 8px; }
  .nx button { font-size: 14.5px; line-height: 20px; padding: 6px 13px; border-radius: 999px; border: 1px solid var(--line); background: var(--fpaper); color: var(--tx); text-align: left; }
  .nx button:not(:disabled):hover { border-color: var(--tx3); }
  .tm { display: flex; align-items: center; margin: 6px 6px 0; }
  .tmr { margin-left: auto; font-size: 12.5px; color: var(--tx3); font-variant-numeric: tabular-nums; }
  .t-problem { color: var(--gone); } .t-waiting { color: var(--wait); } .t-shipped, .t-done { color: var(--good); } .t-running { color: var(--busy); } .t-answer, .t-none { color: var(--tx2); }
  .fyou { background: var(--s3); border-radius: 14px; padding: 12px; margin: 2px 0; display: flex; flex-direction: column; gap: 10px; }
  .fyh { display: flex; align-items: center; gap: 8px; color: var(--wait); font-weight: 600; }
  .funt { display: flex; gap: 8px; color: var(--tx2); }
  .funt svg { flex: none; margin-top: 3px; color: var(--tx3); }
  /* In una colonna stretta e sul telefono: margini stretti, il piede su una colonna. */
  @container (max-width: 520px) {
    .turn { font-size: 15.5px; line-height: 24px; }
    .zw { padding: 12px 8px 12px 6px; }
    .za { padding: 16px; } .zf { padding: 12px 16px 14px; grid-template-columns: 1fr; gap: 6px; }
    .zf .k { margin-top: 6px; } .zf .k:first-child { margin-top: 0; }
    .tl { padding-left: 34px; } .tl::before { left: 13px; }
    .nd > .ic { left: -34px; width: 28px; height: 28px; }
    .tc { padding-left: 10px; column-gap: 8px; }
    .lead.big > p:first-child { font-size: 17px; line-height: 26px; }
    .bubble { max-width: 92%; }
  }
</style>
