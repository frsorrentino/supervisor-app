<script lang="ts">
  import { DAY } from './durations'
  import { locale } from './lang'
  import type { Session, StepDiff, TranscriptEntry, TranscriptFile } from './contract'
  import type { Load } from './diff'
  import StepDiffRow from './StepDiffRow.svelte'
  import { counts, type Item } from './chatFeed'
  import type { Status } from './chatRules'
  import { blocks, cards, compact, type Table } from './markdown'
  import { kind, row } from './toolText'
  import { outcomeForPhone } from './links'
  import { stripSessionLinks } from './sessionLinks'
  import { parseSteps } from './nextSteps'
  import { speech, toggle } from './speech.svelte'
  import type { IconName } from './icons'
  import { t } from './t'
  import Icon from './Icon.svelte'
  import { canCopy, canShare, pageDoc, PAGE_SANDBOX, type FileAct } from './fileActions'
  import { autoPreview, canPreview, fileKey, isImage, isVideo, previewKind } from './media'
  import { onDestroy } from 'svelte'
  import { firstNew, latest, rank, readSeen, saveSeen } from './chatNews'
  import { failed, media, pages, texts } from './mediaCache.svelte'
  // Le pagine mostrate come sorgente col tasto «Codice», per chiave.
  let code = $state<Record<string, boolean>>({})
  import Md from './Md.svelte'

  // La conversazione della scheda (SessionSheet.kt): i messaggi a destra con lo stato, Claude a tutta larghezza con Copia
  // e ▶, i passaggi raccolti in una card, i file come chip, il costo del turno, e in fondo la riga dal vivo.
  let { s, items, now, onFile = () => {}, onFiles = () => {}, onDecision, onDiff = null, sessions = [], onSession = () => {} }: { s: Session; items: Item[]; now: number; onFile?: (path: string, act: FileAct | 'prepare' | 'preview', key: string) => void; onFiles?: (fs: { path: string; key: string }[], act: 'save' | 'share' | 'prepare') => void; onDecision?: (text: string) => void
    /** Contratto 1.72: «Mostra tutto» sotto un passaggio, l'op `diff`; null = il relay non la conosce. */
    onDiff?: ((id: string) => Promise<Load<StepDiff>>) | null
    /** Le sessioni dello stato e l'apertura di una: il link di una di loro nel testo diventa il tasto «Apri in supervisor» (Franz, 21/10 19:04). */
    sessions?: Session[]; onSession?: (name: string) => void } = $props()

  // Un file: sotto il nome i tasti apri, scarica, copia e condividi (condividi solo dove il browser lo sa fare; Franz, 07/10
  // 15:48). Il file si chiede al PC appena il puntatore ci arriva, così il tocco trova i byte pronti.
  const fileActs: [FileAct, string, IconName][] = [['open', t.fileOpen, 'external'], ['download', t.fileDownload, 'download'], ['copy', t.fileCopy, 'copy'], ...(canShare() ? [['share', t.fileShare, 'share'] as [FileAct, string, IconName]] : [])]

  // A + B (Franz, 10/10 16:01), come sul telefono: all'apertura le ultime sei voci salgono a cascata dall'alto, 30 ms l'una
  // dall'altra, nel primo secondo; una voce che arriva dopo entra da sola. Le voci arrivate dall'ultima visita hanno sopra la
  // riga «Nuovi» e un fondo che sfuma; l'ultima visita si ricorda lasciando la chat (Chat rimonta il Feed a ogni sessione).
  const itemKey = (it: Item) => (it.type === 'steps' ? `s-${it.entries[0].id}` : it.type === 'mine' ? `m-${it.sent.id}` : `${it.type[0]}-${it.entry.id}`)
  // svelte-ignore state_referenced_locally
  const seenAt = readSeen(s.name)
  const newsFrom = $derived(firstNew(items, seenAt))
  let born: Set<string> | null = null
  let openedAt = 0
  function motion(it: Item, i: number, n: number): { cls: string; rank: number } {
    if (!born) { born = new Set(items.map(itemKey)); openedAt = performance.now() }
    if (!born.has(itemKey(it))) return { cls: 'rise', rank: 0 }
    const r = rank(i, n)
    return r != null && performance.now() - openedAt < 1000 ? { cls: 'rise', rank: r } : { cls: '', rank: 0 }
  }
  onDestroy(() => saveSeen(s.name, latest(items)))

  // Immagini e video nascono già in anteprima, come sul telefono (Franz, 10/10 15:24): il file si chiede al PC appena la
  // card è a schermo, se `autoPreview` lo vuole; col risparmio dati del browser i video aspettano il tocco.
  function preview(_node: HTMLElement, { f, key }: { f: TranscriptFile; key: string }) {
    const saveData = (navigator as Navigator & { connection?: { saveData?: boolean } }).connection?.saveData === true
    if (!media[key] && !texts[key] && autoPreview(f.mime, f.size, !saveData, f.path)) onFile(f.path, 'preview', key)
  }

  const hm = (at: number) => new Date(at * 1000).toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })
  const copy = (x: string) => navigator.clipboard?.writeText(x)
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
    <button class="sm" aria-label={t.copy} title={t.copy} onclick={() => copy(text)}><Icon name="copy" /></button>
    <button class="play" aria-label={reading ? t.stopReading : t.readAloud} onclick={() => toggle(text, s.name)}>
      {#if reading}<svg viewBox="0 0 24 24" width="18" height="18"><rect x="7" y="7" width="10" height="10" rx="1.5" fill="currentColor" /></svg>
      {:else}<svg viewBox="0 0 24 24" width="18" height="18"><path d="M8 5.5v13l10.5-6.5z" fill="currentColor" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" /></svg>{/if}
    </button>
    {#if onDecision}<button class="sm" aria-label={t.decisionSave} title={t.decisionSave} onclick={() => onDecision(text)}><svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z" /></svg></button>{/if}
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

{#snippet claude(e: TranscriptEntry)}
  {@const text = parseSteps(e.text ?? '').text}
  {@const sl = stripSessionLinks(outcomeForPhone(text, t.outcomeLabel), sessions)}
  {@const c = cost(e)}
  <div class="claude">
    <div class="ctext">
      {#each blocks(sl.text) as b}
        {#if b.type === 'text'}<p><Md text={b.text} /></p>{:else}{@render table(b)}{/if}
      {/each}
      <!-- Un tasto per sessione linkata: il clic apre la sua chat qui; è un link vero, così il tasto destro o la pressione
           lunga danno il link com'era (Copia, Apri in una scheda nuova). Copia e ▶ tengono il testo intero. -->
      {#each sl.buttons as b}
        <a class="sbtn" href={b.url} target="_blank" rel="noopener" title={t.openInSupervisorHint} onclick={(ev) => { ev.preventDefault(); onSession(b.session) }}><span class="sl">{t.openInSupervisor}</span><span class="sn">{b.session}</span></a>
      {/each}
      {#if e.cut}<span class="meta">{t.textCut}</span>{/if}
      {#if c}<span class="cost">{c}</span>{/if}
    </div>
    {@render actions(text, e.at)}
  </div>
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
      <div class="fcard" role="group" aria-label={f.path.split('/').pop()} use:preview={{ f, key }} onpointerenter={() => onFile(f.path, 'prepare', key)} onfocusin={() => onFile(f.path, 'prepare', key)}>
        <button class="fchip" title={f.path} onclick={() => onFile(f.path, 'open', key)}><Icon name={fileIcon(f)} color="var(--icon)" /><span class="fname">{f.path.split('/').pop()}</span>{#if f.size != null}<span class="fsize">{size(f.size)}</span>{/if}</button>
        <!-- L'anteprima tra il nome e i tasti, come nell'app (Franz, 13/10 07:16). -->
        {#if url && (isImage(f.mime) || kind === 'pdf')}<button class="fthumb" class:pdf={kind === 'pdf'} aria-label={t.fileOpen} onclick={() => onFile(f.path, 'open', key)}><img src={url} alt="" /></button>{/if}
        {#if pages[key] && !code[key]}
          <!-- Franz, 16/10 11:18: HTML e SVG come pagina, in un riquadro isolato (PAGE_SANDBOX: origine opaca, niente token). -->
          <iframe class="fpage" title={f.path.split('/').pop()} sandbox={PAGE_SANDBOX} referrerpolicy="no-referrer" srcdoc={pageDoc(pages[key].mime, pages[key].text)}></iframe>
        {:else if texts[key]}<button class="ftext" class:cut={texts[key].more} aria-label={t.fileOpen} onclick={() => onFile(f.path, 'open', key)}><pre>{texts[key].lines}</pre></button>{/if}
        {#if pages[key]}<button class="fshow" aria-pressed={!!code[key]} onclick={() => (code[key] = !code[key])}>{code[key] ? t.filePage : t.fileCode}</button>{/if}
        {#if !url && !texts[key] && !failed[key] && canPreview(f.mime, f.path) && !autoPreview(f.mime, f.size, !((navigator as Navigator & { connection?: { saveData?: boolean } }).connection?.saveData === true), f.path)}
          <!-- Oltre i limiti, o col risparmio dati, l'anteprima aspetta il tocco. -->
          <button class="fshow" onclick={() => onFile(f.path, 'preview', key)}>{t.showPreview}</button>
        {/if}
        {#if url && isVideo(f.mime)}<!-- svelte-ignore a11y_media_has_caption --><video class="fvideo" src={url} controls preload="metadata" playsinline></video>{/if}
        <div class="acts">{#each fileActs.filter(([a]) => a !== 'copy' || canCopy(f.mime ?? '')) as [a, label, icon]}<button class="sm" aria-label={label} title={label} onclick={() => onFile(f.path, a, key)}><Icon name={icon} /></button>{/each}</div>
      </div>
    {/each}
  </div>
{/snippet}

{#snippet tool(e: TranscriptEntry, withFiles: boolean)}
  {@const [main, detail] = row(e.tool, e.text, e.note)}
  <div class="tool">
    <div class="trow">
      <Icon name={toolIcon(e)} color={e.error ? 'var(--gone)' : 'var(--text2)'} label={e.tool ?? undefined} />
      <span class="tcol"><span class:bad={e.error}>{main}</span>{#if detail}<code>{detail}</code>{/if}</span>
    </div>
    {#if withFiles && e.files?.length}<div class="indent">{@render files(e.files.map(f => ({ f, entry: e.id })))}</div>{/if}
    <!-- Contratto 1.72: la riga chiusa del diff sotto il passaggio; in un gruppo le righe stanno in fondo, come i file. -->
    {#if withFiles && e.diff}<div class="indent"><StepDiffRow id={e.id} diff={e.diff} {onDiff} /></div>{/if}
  </div>
{/snippet}

{#snippet steps(g: Extract<Item, { type: 'steps' }>)}
  {@const id = g.entries[0].id}
  {@const failed = g.entries.filter(e => e.error).length}
  {@const cs = counts(g)}
  {@const last = g.entries[g.entries.length - 1]}
  {@const fs = g.entries.flatMap(e => (e.files ?? []).map(f => ({ f, entry: e.id })))}
  <div class="steps" class:fail={failed > 0}>
    <button class="shead" aria-expanded={!!open[id]} onclick={() => (open[id] = !open[id])}>
      <span class="ssum">{[t.stepsCount(g.entries.length), cs[0] ? `${cs[0][1]} ${cs[0][0]}` : null, cs.length > 1 ? `+${cs.length - 1}` : null].filter(Boolean).join(' · ')}{#if failed}{' · '}<b class="sfail">{t.stepsFailed(failed)}</b>{/if}</span>
      <span class="dots">{#each g.entries.slice(0, 12) as e}<i class:bad={e.error}></i>{/each}{#if g.entries.length > 12}<span>+{g.entries.length - 12}</span>{/if}</span>
      <svg viewBox="0 0 24 24" width="22" height="22"><path d={open[id] ? 'M7 14l5-5 5 5' : 'M7 10l5 5 5-5'} fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
    </button>
    {#if open[id]}
      {#each g.entries as e (e.id)}{@render tool(e, false)}{/each}
    {:else}
      {@const [main] = row(last.tool, last.text, last.note)}
      <div class="slast"><Icon name={toolIcon(last)} size={16} color={last.error ? 'var(--gone)' : 'var(--text2)'} /><span class:bad={last.error}>{main}</span></div>
    {/if}
    {#if fs.length}{@render files(fs)}{/if}
    {#each g.entries as e (e.id)}{#if e.diff}<StepDiffRow id={e.id} diff={e.diff} {onDiff} />{/if}{/each}
  </div>
{/snippet}

{#snippet bubble(text: string, low: boolean)}
  <div class="bubble" class:low><p><Md {text} /></p></div>
{/snippet}

{#each items as it, i (itemKey(it))}
  {@const m = motion(it, i, items.length)}
  <div class="it {m.cls}" class:fresh={newsFrom != null && i >= newsFrom && it.type !== 'mine'} style="--rank:{m.rank}">
  {#if i === newsFrom}<div class="news"><span>{t.chatNews}</span></div>{/if}
  {#if it.type === 'mine'}
    {@const [ic, tone] = status[it.status]}
    <div class="me">
      {#if it.sent.attachment}<span class="att"><Icon name="file" size={16} />{it.sent.attachment}</span>{/if}
      {#each it.sent.attachments ?? [] as a}<span class="att"><Icon name="file" size={16} />{a}</span>{/each}
      {#if it.sent.text}{@render bubble(it.sent.text, false)}{/if}
      <div class="mrow">
        <span class="mark" style="color:{tone}"><Icon name={ic} size={16} color={tone} />{it.status === 'scheduled' ? t.chatStatus.scheduled(hm(it.sent.scheduledFor ?? it.sent.sentAt)) : t.chatStatus[it.status]('')}</span>
        {#if it.status !== 'scheduled'}<span class="time">{hm(it.sent.sentAt)}</span>{/if}
        <button class="sm" aria-label={t.copy} title={t.copy} onclick={() => copy(it.sent.text)}><Icon name="copy" /></button>
      </div>
    </div>
  {:else if it.type === 'user'}
    <div class="me">
      {@render bubble(it.entry.text ?? '', true)}
      <div class="mrow">
        {#if it.entry.origin && t.origin[it.entry.origin]}<span class="meta">{t.origin[it.entry.origin]}</span>{/if}
        {#if it.entry.queued}<span class="meta stale">· {t.chatStatus.queued('')}</span>{/if}
        {#if it.entry.at}<span class="time">{hm(it.entry.at)}</span>{/if}
        <button class="sm" aria-label={t.copy} title={t.copy} onclick={() => copy(it.entry.text ?? '')}><Icon name="copy" /></button>
      </div>
    </div>
  {:else if it.type === 'claude'}
    {@render claude(it.entry)}
  {:else if it.type === 'tool'}
    {@render tool(it.entry, true)}
  {:else}
    {@render steps(it)}
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

<style>
  p { white-space: pre-wrap; overflow-wrap: anywhere; }
  .it { display: flex; flex-direction: column; border-radius: 14px; }
  .rise { animation: rise 200ms cubic-bezier(0.3, 0, 0.2, 1) both; animation-delay: calc(var(--rank) * 30ms); }
  @keyframes rise { from { opacity: 0; transform: translateY(12px); } }
  .fresh { animation: fresh 2s ease-out 600ms both; }
  .rise.fresh { animation: rise 200ms cubic-bezier(0.3, 0, 0.2, 1) both, fresh 2s ease-out 600ms both; animation-delay: calc(var(--rank) * 30ms), 600ms; }
  @keyframes fresh { from { background: rgb(168 199 250 / .12); box-shadow: 0 0 0 6px rgb(168 199 250 / .12); } to { background: transparent; box-shadow: 0 0 0 6px transparent; } }
  .news { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; color: var(--icon); font-size: 12px; font-weight: 500; }
  .news::before, .news::after { content: ''; flex: 1; height: 1px; background: currentColor; opacity: .45; }
  @media (prefers-reduced-motion: reduce) { .rise, .fresh, .rise.fresh { animation: none; } }
  .me { align-self: stretch; padding-left: 40px; display: flex; flex-direction: column; align-items: flex-end; }
  .att { display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; margin-bottom: 4px; border-radius: 14px; background: var(--surface); color: var(--text2); font-size: 14px; overflow-wrap: anywhere; }
  .bubble { background: var(--high); border-radius: 20px 20px 6px 20px; padding: 12px 16px; font-size: 16px; letter-spacing: .03em; max-width: 760px; }
  .bubble.low { background: var(--surface); }
  .mrow { display: flex; align-items: center; gap: 2px; }
  .mark { display: flex; align-items: center; gap: 4px; font-size: 12.5px; font-weight: 500; }
  .time { font-size: 12.5px; color: var(--text2); padding: 0 4px; font-weight: 500; }
  .meta { font-size: 12.5px; color: var(--text2); font-weight: 500; }
  .stale { color: var(--stale); }
  .sm { width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center; color: var(--text2); }
  .sm:hover { background: var(--surface); }
  .claude { display: flex; flex-direction: column; max-width: 760px; }
  .ctext { padding: 4px; display: flex; flex-direction: column; gap: 10px; font-size: 16px; line-height: 1.5; letter-spacing: .03em; }
  .cost { font-size: 12.5px; font-weight: 500; color: var(--b-second); }
  .sbtn { align-self: flex-start; display: flex; flex-direction: column; gap: 1px; background: var(--high); color: var(--text); border-radius: 16px; padding: 8px 18px; text-decoration: none; min-height: 44px; justify-content: center; }
  .sbtn:hover { filter: brightness(1.1); }
  .sbtn .sl { font-size: 15px; font-weight: 500; letter-spacing: .01em; }
  .sbtn .sn { font-size: 12.5px; color: var(--text2); }
  .acts { display: flex; align-items: center; gap: 2px; }
  .play { width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center; background: var(--high); color: var(--icon); margin-left: 2px; }
  .play:hover, .fchip:hover { filter: brightness(1.15); }
  .grid { display: grid; grid-template-columns: repeat(var(--n), 1fr); }
  .th, .td { padding-right: 12px; }
  .th { font-size: 14px; font-weight: 600; color: var(--text2); padding: 6px 0; }
  .td { font-size: 14.5px; padding: 8px 0; border-top: 1px solid var(--line); }
  .cards { display: flex; flex-direction: column; gap: 8px; }
  .tcard { background: var(--surface); border-radius: 12px; padding: 10px 14px; display: flex; flex-direction: column; gap: 3px; font-size: 14.5px; }
  .tcard b { font-size: 16px; font-weight: 600; }
  .k { color: var(--text2); }
  .tool { display: flex; flex-direction: column; gap: 6px; padding-left: 4px; }
  .trow { display: flex; gap: 10px; align-items: flex-start; }
  .trow :global(svg) { margin-top: 2px; }
  .tcol { display: flex; flex-direction: column; gap: 2px; font-size: 14.5px; min-width: 0; }
  .bad { color: var(--gone-dim); }
  code { font: 12.5px/1.4 var(--mono); color: var(--stale); white-space: pre-wrap; overflow-wrap: anywhere; display: -webkit-box; -webkit-line-clamp: 3; line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
  .indent { padding-left: 28px; }
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
  .ftext pre { margin: 0; font: 13px/1.5 var(--mono); color: var(--text); white-space: pre-wrap; word-break: break-word; }
  /* Un testo più lungo delle righe mostrate sfuma in fondo, senza puntini. */
  .ftext.cut pre { mask-image: linear-gradient(#000 70%, transparent); -webkit-mask-image: linear-gradient(#000 70%, transparent); }
  .fpage { display: block; width: 100%; height: 360px; border: 1px solid var(--line); border-radius: 14px; background: #fff; }
  .fshow { align-self: flex-start; padding: 8px 14px; border-radius: 999px; background: var(--high); color: var(--icon); font-size: 14px; font-weight: 500; }
  .fthumb { padding: 0; border-radius: 14px; overflow: hidden; background: none; }
  /* Con l'anteprima la scheda prende tutta la larghezza della chat, come nell'app (Franz, 12/10 12:27: «anteprime con width
     100%»); un verticale è alto al massimo il doppio della larghezza. Nome, anteprima e tasti stanno a 6 px, come nell'app. */
  .fcard:has(> .fthumb, > .fvideo) { width: 100%; container-type: inline-size; gap: 6px; }
  .fcard > .fthumb { width: 100%; }
  .fthumb img { display: block; width: 100%; height: auto; max-height: 200cqw; object-fit: cover; }
  .fvideo { display: block; width: 100%; max-height: 200cqw; border-radius: 14px; background: #000; }
  .steps .fchip { background: rgb(255 255 255 / .06); }
  .fname { font-size: 14px; font-weight: 500; overflow-wrap: anywhere; }
  .fsize { font-size: 12.5px; color: var(--text2); white-space: nowrap; }
  .steps { background: var(--steps-bg); border-radius: 14px; padding: 6px 4px 8px 12px; display: flex; flex-direction: column; gap: 4px; }
  .steps.fail { background: var(--steps-fail); }
  .shead { display: flex; align-items: center; width: 100%; text-align: left; border-radius: 8px; }
  .ssum { flex: 1; min-width: 0; font-size: 14px; font-weight: 500; color: var(--text2); white-space: nowrap; overflow: hidden; }
  .sfail { color: var(--b-alert); font-weight: 600; }
  .dots { display: flex; gap: 3px; align-items: center; padding: 0 4px 0 8px; font-size: 11px; color: var(--text2); }
  .dots i { width: 5px; height: 5px; border-radius: 50%; background: var(--steps-dot); }
  .dots i.bad { background: var(--b-alert); }
  .slast { display: flex; align-items: center; gap: 8px; font-size: 14.5px; white-space: nowrap; overflow: hidden; }
  .steps .tool { padding: 4px 0; }
  .live { display: flex; align-items: center; gap: 10px; padding: 4px 0; font-size: 14.5px; color: var(--text2); }
  .star { stroke: var(--opus); stroke-width: 2; stroke-linecap: round; animation: spin 2.4s linear infinite, pulse 1.4s ease-in-out infinite alternate; }
  @keyframes spin { to { rotate: 360deg; } }
  @keyframes pulse { from { scale: .75; opacity: .6; } to { scale: 1.1; opacity: 1; } }
  @media (prefers-reduced-motion: reduce) { .star { animation: pulse 1.2s ease-in-out infinite alternate; } }
</style>
