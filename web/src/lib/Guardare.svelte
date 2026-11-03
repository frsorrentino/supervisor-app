<script lang="ts">
  import type { CmdResult, ResultItem, ResultsPage, ResultsSummary, TranscriptFile } from './contract'
  import { line, note as cleanNote, NOTE_MAX, older, pos, thumb, type Line } from './guardare'
  import { newCmd } from './transport'
  import { t } from './t'
  import CatIcon from './CatIcon.svelte'
  // «Da guardare» (app-da-guardare-02-11, mockup della master delle 08:16; contratto 1.106), porta di GuardareUi.kt: in home le
  // ultime versioni non viste con Visto e Nota; la pagina coi lavori e le versioni precedenti (anche di una sessione sola); il
  // risultato aperto col lettore della chat e la nota che va alla sessione col secondo in cui si è.
  let { run, summary, mode, session = null, openId = null, getFile, onExternal, onSession, onAll = () => {}, onOpen = () => {} }: {
    run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>; summary: ResultsSummary | null | undefined; mode: 'body' | 'page'
    session?: string | null; openId?: string | null
    getFile: (r: ResultItem, f: TranscriptFile, quiet: boolean) => Promise<Blob | null>; onExternal: (r: ResultItem) => void; onSession: (name: string) => void
    onAll?: () => void; onOpen?: (r: ResultItem) => void
  } = $props()
  let page = $state<ResultsPage | null>(null)
  let failed = $state(false)
  let opened = $state<string | null>(null)
  // Aperto dall'elenco della pagina: la sua freccia torna all'elenco; aperto dalla home, la freccia della pagina torna alla home.
  let fromList = $state(false)
  $effect(() => { opened = openId; fromList = false })
  async function load() { const r = await run(newCmd('results', null)); failed = !r?.ok; if (r?.ok) try { page = JSON.parse(r.text) as ResultsPage } catch { failed = true } }
  $effect(() => { void summary?.n_unseen; void summary?.items?.[0]?.id; void load() })
  const full = (r: ResultItem) => page?.groups.flatMap(g => g.items).find(x => x.id === r.id) ?? r
  const groups = $derived((page?.groups ?? []).filter(g => !session || g.session === session))
  const current = $derived(opened ? page?.groups.flatMap(g => g.items).find(x => x.id === opened) ?? summary?.items.find(x => x.id === opened) ?? null : null)
  let seenIds = $state(new Set<string>())
  async function seen(r: ResultItem) {
    if (seenIds.has(r.id)) return
    seenIds = new Set([...seenIds, r.id])
    const x = await run(newCmd('result_seen', null, r.id))
    if (!x?.ok) seenIds = new Set([...seenIds].filter(i => i !== r.id))
    void load()
  }
  // Le miniature e il file aperto: URL locali dal blob, rilasciati quando il componente se ne va.
  // Chiesti si segna fuori dallo stato: il render non scrive stato (state_unsafe_mutation), lo fa solo l'arrivo del file.
  let urls = $state<Record<string, string>>({})
  const asked = new Set<string>()
  const made: string[] = []
  $effect(() => () => { for (const u of made) URL.revokeObjectURL(u) })
  function urlOf(r: ResultItem, f: TranscriptFile, quiet: boolean): string | null {
    const k = `${r.id}:${f.path}`
    if (!asked.has(k)) {
      asked.add(k)
      queueMicrotask(() => void getFile(r, f, quiet).then(b => { if (b) { const u = URL.createObjectURL(b); made.push(u); urls = { ...urls, [k]: u } } }))
    }
    return urls[k] ?? null
  }
  const sub = (l: Line, withSession = true) => [withSession ? l.session : null, l.seconds != null ? `${t.watchKind[l.kind]}, ${t.watchSeconds(l.seconds)}` : t.watchKind[l.kind], hm(l.at)].filter(Boolean).join(' · ')
  const hm = (s: number) => new Date(s * 1000).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
  const titleOf = (l: Line) => (l.version != null ? t.watchTitleVersion(l.title, l.version) : l.title)
  function open(r: ResultItem) { if (mode === 'body') onOpen(full(r)); else { opened = r.id; fromList = true } }
  let olderOpen = $state<Record<string, boolean>>({})
  let text = $state(''), at = $state<number | null>(null), sent = $state<string | null>(null)
  async function sendNote(r: ResultItem) {
    const x = cleanNote(text); if (!x) return
    const res = await run(newCmd('result_note', null, r.id, x, undefined, at != null && line(r).kind === 'video' ? { pos_s: Math.round(at * 10) / 10 } : undefined))
    if (res?.ok) { text = ''; sent = r.session ?? null; void load() }
  }
</script>

{#snippet thumbOf(r: ResultItem, big = false)}
  {@const f = thumb(r)}
  {@const u = f ? urlOf(r, f, true) : null}
  <span class="th" class:big>{#if u}<img src={u} alt="" />{/if}{#if line(r).kind === 'video'}<span class="ms play" aria-hidden="true">play_circle</span>{:else if !u}<span class="doc"><CatIcon name="doc" size={28} /></span>{/if}</span>
{/snippet}
{#snippet seenNote(r: ResultItem)}
  <span class="acts">
    {#if r.seen_at == null}<button class="pill" class:done={seenIds.has(r.id)} disabled={seenIds.has(r.id)} onclick={(e) => { e.stopPropagation(); void seen(r) }}><span class="ms" aria-hidden="true">check</span>{t.watchSeen}</button>{/if}
    <button class="pill" onclick={(e) => { e.stopPropagation(); open(r) }}><CatIcon name="pencil" size={18} />{t.watchNote}</button>
  </span>
{/snippet}
{#snippet noteLine(n: { text: string; pos_s?: number | null })}
  <p class="note"><CatIcon name="pencil" size={18} />{n.pos_s != null ? t.watchYourNoteAt(pos(n.pos_s), n.text) : t.watchYourNote(n.text)}</p>
{/snippet}

{#if current}
  {@const r = current}
  {@const l = line(r)}
  {@const show = thumb(r) ?? (l.kind === 'video' ? { path: r.path, mime: r.mime, size: r.size } : null)}
  {@const u = show ? urlOf(r, show, false) : null}
  <div class="viewer" data-testid="risultato-aperto">
    <div class="vh">{#if fromList}<button class="back" aria-label={t.back} onclick={() => { opened = null; fromList = false }}><CatIcon name="back" size={24} /></button>{/if}<h2>{l.title}</h2></div>
    {#if u && l.kind === 'video'}<!-- svelte-ignore a11y_media_has_caption --><video src={u} controls playsinline ontimeupdate={(e) => (at = (e.currentTarget as HTMLVideoElement).currentTime)}></video>
    {:else if u}<img class="full" src={u} alt={l.title} />
    {:else if show}<div class="ph"></div>{/if}
    {#if l.kind !== 'video' && l.kind !== 'image'}<button class="pill" onclick={() => onExternal(r)}>{t.watchOpenFile}</button>{/if}
    <p class="vline"><b>{t.watchVersionOf(r.version, r.versions)}</b><span class="mono">{[hm(r.at), l.seconds != null ? t.watchSeconds(l.seconds) : null].filter(Boolean).join(' · ')}</span></p>
    {#if r.changed?.trim()}<p class="changed"><b>{t.watchChanged}</b> {r.changed}</p>{/if}
    {#if r.caption?.trim() && r.caption !== r.title}<p class="sub">{r.caption}</p>{/if}
    {#if r.replaced}<p class="sub warn">{t.watchReplaced}</p>{/if}
    <span class="acts">
      {#if r.seen_at == null}<button class="pill filled" class:done={seenIds.has(r.id)} disabled={seenIds.has(r.id)} onclick={() => seen(r)}><span class="ms" aria-hidden="true">check</span>{t.watchSeen}</button>{/if}
      {#if r.session}<button class="pill" onclick={() => onSession(r.session!)}><span class="ms" aria-hidden="true">chevron_right</span>{t.watchOpenSession}</button>{/if}
    </span>
    {#each r.notes ?? [] as n, i (i)}{@render noteLine(n)}{/each}
    {#if r.session}
      <form class="nform" onsubmit={(e) => { e.preventDefault(); void sendNote(r) }}>
        <input data-testid="nota-risultato" bind:value={text} maxlength={NOTE_MAX} placeholder={t.watchNoteHint(r.session)} />
        <button class="pill" disabled={!text.trim()}>{t.watchSend}{at != null && l.kind === 'video' ? ` ${t.watchAtPos(pos(at))}` : ''}</button>
      </form>
      <p class="sub">{sent ? t.watchNoteSent(sent) : t.watchNoteExplain}</p>
    {/if}
  </div>
{:else if mode === 'body'}
  <div class="list" data-testid="corpo-guardare">
    {#if !summary?.items.length}<p class="sub">{t.watchEmpty}</p>{/if}
    {#each (summary?.items ?? []).slice(0, 3).map(full) as r (r.id)}
      {@const l = line(r)}
      <div class="row" role="button" tabindex="0" data-testid="risultato-{r.id}" onclick={() => open(r)} onkeydown={(e) => e.key === 'Enter' && open(r)}>
        {@render thumbOf(r)}
        <span class="col"><b class="t">{#if l.unseen}<i class="dot"></i>{/if}{titleOf(l)}</b><span class="sub">{sub(l)}</span>{@render seenNote(r)}</span>
      </div>
    {/each}
    <button class="all" data-testid="risultati-tutti" onclick={onAll}>{t.watchAll}</button>
  </div>
{:else}
  <div class="pg" data-testid="da-guardare">
    {#if failed && !page}<p class="sub">{t.watchReadFailed}</p>{:else if !page}<p class="sub">{t.watchLoading}</p>{:else if !groups.length}<p class="sub">{t.watchEmpty}</p>{/if}
    {#each groups as g (g.title + g.session)}
      {@const k = g.title + '|' + g.session}
      <section class="grp lit">
        <h3>{g.title}</h3>
        {#each [g.items[0], ...(olderOpen[k] ? older(g) : [])].filter(Boolean) as r (r.id)}
          {@const l = line(r)}
          <div class="row" role="button" tabindex="0" onclick={() => open(r)} onkeydown={(e) => e.key === 'Enter' && open(r)}>
            {@render thumbOf(r)}
            <span class="col"><b class="t">{#if l.unseen}<i class="dot"></i>{/if}{t.watchVersion(r.version)}</b>
              <span class="sub">{[hm(r.at), r.changed?.trim() || null, l.unseen ? null : t.watchSeenWord].filter(Boolean).join(' · ')}</span>
              {#if l.unseen}{@render seenNote(r)}{/if}</span>
          </div>
          {#each r.notes ?? [] as n, i (i)}{@render noteLine(n)}{/each}
        {/each}
        {#if older(g).length}<button class="older" onclick={() => (olderOpen = { ...olderOpen, [k]: !olderOpen[k] })}><span class="ms" aria-hidden="true">{olderOpen[k] ? 'expand_less' : 'expand_more'}</span>{t.watchOlder}&nbsp;&nbsp;{older(g).length}</button>{/if}
      </section>
    {/each}
  </div>
{/if}

<style>
  .list, .pg { display: flex; flex-direction: column; gap: 4px; }
  .pg { gap: 12px; padding: 8px 16px 24px; }
  .row { display: flex; gap: 12px; padding: 8px 0; cursor: pointer; border-bottom: 1px solid var(--line, rgba(255,255,255,.08)); text-align: left; }
  .col { display: flex; flex-direction: column; gap: 4px; min-width: 0; flex: 1; }
  .t { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 600; color: var(--tx); }
  .dot { width: 9px; height: 9px; border-radius: 50%; background: var(--wait); flex: none; }
  .sub { color: var(--tx2); font-size: 13.5px; margin: 0; }
  .sub.warn { color: var(--wait); }
  .th { width: 118px; height: 74px; flex: none; border-radius: 10px; background: var(--s3); position: relative; overflow: hidden; display: grid; place-items: center; }
  .th img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; object-position: top; }
  .th .play { position: relative; font-size: 34px; color: var(--tx); }
  .th .doc { color: var(--tx2); display: grid; }
  .acts { display: flex; gap: 8px; flex-wrap: wrap; }
  .pill { min-height: 36px; padding: 0 14px; display: inline-flex; align-items: center; gap: 6px; border-radius: 18px; background: var(--s3); color: var(--tx); font-size: 14px; }
  .pill .ms { font-size: 18px; }
  .pill.filled { background: var(--act); color: var(--on-act, #000); }
  .pill.done { background: var(--good); color: #000; }
  .all, .older { align-self: center; color: var(--act); padding: 10px; font-size: 14px; display: inline-flex; align-items: center; gap: 4px; }
  .older { align-self: flex-start; color: var(--tx2); }
  .grp { background: var(--s1); border-radius: 14px; padding: 14px; display: flex; flex-direction: column; gap: 6px; }
  .grp h3 { margin: 0; font-size: 17px; color: var(--tx); border-bottom: 1px solid var(--line, rgba(255,255,255,.08)); padding-bottom: 6px; }
  .note { display: flex; gap: 8px; color: var(--tx2); font-size: 13.5px; margin: 0 0 4px; }
  .note .ms { font-size: 18px; }
  .viewer { display: flex; flex-direction: column; gap: 10px; padding: 8px 16px 24px; }
  .vh { display: flex; align-items: center; gap: 6px; }
  .vh h2 { margin: 0; font-size: 20px; color: var(--tx); }
  .back { width: 40px; height: 40px; display: grid; place-items: center; border-radius: 20px; color: var(--tx); }
  video, .full { width: 100%; border-radius: 12px; background: #000; }
  .ph { width: 100%; aspect-ratio: 16 / 9; border-radius: 12px; background: var(--s2); }
  .vline { display: flex; gap: 10px; align-items: baseline; margin: 0; color: var(--tx); }
  .mono { font-family: var(--mono, monospace); color: var(--tx2); font-size: 13px; }
  .changed { margin: 0; color: var(--tx); }
  .nform { display: flex; gap: 8px; }
  .nform input { flex: 1; min-height: 44px; border-radius: 22px; padding: 0 16px; background: transparent; border: 1px solid var(--line, rgba(255,255,255,.2)); color: var(--tx); font: inherit; }
</style>
