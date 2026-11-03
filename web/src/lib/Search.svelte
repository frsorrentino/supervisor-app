<script lang="ts">
  import type { Event, SearchPage, Session } from './contract'
  import type { Sent } from './chatRules'
  import { find, withConversations, type Hit } from './search'
  import { dayStart, FILTER, groups, remember, whenOf, type Filter, type Recent } from './searchView'
  import { badge as badgeOf, GREY } from './badge'
  import Icon from './Icon.svelte'
  import { t } from './t'

  // Cerca (app-cerca-pagina-03-11, mockup del 03/11, scelta «Così» di Franz). Prima di scrivere: le ricerche recenti e i filtri
  // (tutte o una sessione aperta, Oggi o 7 giorni, anche le chiuse). Con il relay (contratto 1.27) cerca nelle conversazioni di
  // tutte le sessioni degli ultimi 7 giorni, partendo da sola quando smetti di scrivere; i risultati stanno raggruppati per
  // sessione col passaggio trovato evidenziato, e un tocco apre la conversazione lì. Un evento senza sessione apre il Registro.
  let { sent, events, remote, onQuery, page = null, loading = false, known, onOpen, timeZone, sessions = [], now = Math.floor(Date.now() / 1000) }: {
    sent: Sent[]; events: Event[]; remote: boolean; onQuery: (q: string) => void; page?: SearchPage | null; loading?: boolean
    known: Set<string>; onOpen: (session: string | null, entry?: string | null) => void; timeZone?: string; sessions?: Session[]; now?: number
  } = $props()

  const KEY = 'cm.search_recent'
  const load = (): Recent[] => { try { const v = JSON.parse(localStorage.getItem(KEY) ?? '[]'); return Array.isArray(v) ? v.filter(x => typeof x?.q === 'string') : [] } catch { return [] } }
  let recent = $state<Recent[]>(load())
  const save = (r: Recent[]) => { recent = r; try { localStorage.setItem(KEY, JSON.stringify(r)) } catch { /* senza memoria resta in pagina */ } }

  let query = $state('')
  let filter = $state<Filter>({ ...FILTER })
  const local = $derived(find(query, sent, events))
  const hits = $derived<Hit[]>(remote && page ? withConversations(local, page) : local)
  const shown = $derived(groups(hits, filter, dayStart(now, timeZone)))
  const total = $derived(shown.reduce((n, g) => n + g.hits.length, 0))
  const open = $derived(sessions.filter(s => s.state !== 'gone'))
  const color = (name: string | null) => {
    const s = sessions.find(x => x.name === name)
    const f = s ? badgeOf(s.account, s.color, s.state, s.icon, s.account_kind).fill : GREY
    return f === GREY ? 'var(--text2)' : f
  }
  let field: HTMLInputElement | undefined = $state()
  $effect(() => { field?.focus() })
  // Quando smetti di scrivere: ogni ricerca costa al relay da mezzo secondo a dieci. Partita, la ricerca va fra le recenti.
  $effect(() => {
    const q = query.trim()
    if (q.length < 2) return
    const id = setTimeout(() => { if (remote) onQuery(q); save(remember(recent, { q, session: filter.session })) }, 700)
    return () => clearTimeout(id)
  })
  const when = (at: number) => {
    const w = whenOf(at, now, timeZone)
    return w.kind === 'time' ? w.hhmm : w.kind === 'yesterday' ? t.searchYesterday : w.kind === 'weekday' ? t.searchWeekdays[w.day - 1] : w.ddmm
  }
  const canOpen = (h: Hit) => h.kind !== 'conversation' || h.live === true || (h.session != null && known.has(h.session))
  function useRecent(r: Recent) { filter = { ...filter, session: r.session && open.some(s => s.name === r.session) ? r.session : null }; query = r.q; field?.focus() }
</script>

<div class="search">
  <label class="field">
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round"><path d="m21 21-4.34-4.34" /><circle cx="11" cy="11" r="8" /></svg>
    <input bind:this={field} bind:value={query} placeholder={remote ? t.searchHintAll : t.searchHint} aria-label={t.search} />
    {#if query}<button class="clr" aria-label={t.searchClear} title={t.searchClear} onclick={() => { query = ''; field?.focus() }}><Icon name="x" size={20} /></button>{/if}
  </label>
  <div class="pills" role="group">
    <button class="pill" class:on={filter.session == null} aria-pressed={filter.session == null} onclick={() => (filter = { ...filter, session: null })}>{t.searchAll}</button>
    {#each open as s (s.name)}
      <button class="pill" class:on={filter.session === s.name} aria-pressed={filter.session === s.name} onclick={() => (filter = { ...filter, session: filter.session === s.name ? null : s.name })}><i class="dot" style:background={color(s.name)}></i>{s.name}</button>
    {/each}
  </div>
  <div class="pills" role="group">
    <button class="pill" class:on={filter.today} aria-pressed={filter.today} onclick={() => (filter = { ...filter, today: true })}>{t.searchToday}</button>
    <button class="pill" class:on={!filter.today} aria-pressed={!filter.today} onclick={() => (filter = { ...filter, today: false })}>{t.searchWeek}</button>
    <span class="sep"></span>
    <button class="pill" class:on={filter.closed} aria-pressed={filter.closed} onclick={() => (filter = { ...filter, closed: !filter.closed })}>
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"><rect x="3" y="4" width="18" height="4" rx="1" /><path d="M5 8v11a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V8M10 12h4" /></svg>{t.searchClosedToo}
    </button>
  </div>
  {#if loading}<div class="bar"><i></i></div>{/if}

  {#if !query.trim()}
    {#if recent.length}
      <p class="lbl">{t.searchRecent}</p>
      <div class="recent">
        {#each recent as r (r.q)}
          <div class="row">
            <button class="go" onclick={() => useRecent(r)}><Icon name="clock" size={20} color="var(--text2)" /><span class="t">{r.q}{#if r.session}<small>{r.session}</small>{/if}</span></button>
            <button class="x" aria-label={t.searchForget(r.q)} title={t.searchForget(r.q)} onclick={() => save(recent.filter(x => x !== r))}><Icon name="x" size={20} /></button>
          </div>
        {/each}
      </div>
    {/if}
    <p class="help">{t.searchHelp}</p>
  {:else}
    {#if !shown.length && !loading}<p class="none">{t.searchNone}</p>{/if}
    {#each shown as g (`${g.session}-${g.live}`)}
      <section class="grp">
        <header class="gh"><i class="dot" style:background={g.session == null ? 'var(--text2)' : g.live === false ? 'var(--text3, var(--text2))' : color(g.session)}></i><b>{g.session ?? t.lavoriTitle}</b><span class="n">{g.hits.length}</span>{#if g.live != null}<span class="st">{g.live ? t.searchOpenOne : t.searchClosed}</span>{/if}</header>
        {#each g.hits as h (h.ref)}
          {@const ok = canOpen(h)}
          <svelte:element this={ok ? 'button' : 'div'} class="hit" class:open={ok} role={ok ? undefined : 'group'} onclick={ok ? () => onOpen(h.session, h.entry ?? null) : undefined}>
            <span class="at">{when(h.at)}</span>
            <span class="tx">{h.line.slice(0, h.start)}<mark>{h.line.slice(h.start, h.end)}</mark>{h.line.slice(h.end)}</span>
            {#if ok}<svg class="chev" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="m9 6 6 6-6 6" /></svg>{/if}
          </svelte:element>
        {/each}
      </section>
    {/each}
    {#if remote && page?.more}<p class="more">{t.searchMore}</p>{/if}
    {#if shown.length}<p class="foot">{t.searchFoot(total, shown.length, filter.today)}</p>{/if}
  {/if}
</div>

<style>
  .search { display: flex; flex-direction: column; min-height: 100%; max-width: 760px; padding-bottom: 16px; }
  .field { display: flex; align-items: center; gap: 12px; margin: 12px 16px; padding: 0 8px 0 18px; min-height: 52px; border: 1px solid rgb(255 255 255 / .25); border-radius: 26px; background: var(--surface); }
  .field:focus-within { border-color: var(--text2); border-width: 2px; padding: 0 7px 0 17px; }
  input { flex: 1; min-width: 0; font: inherit; font-size: 16px; color: var(--text); background: none; border: 0; outline: 0; }
  .clr { width: 44px; height: 44px; display: grid; place-items: center; color: var(--text2); border-radius: 22px; }
  .pills { display: flex; gap: 8px; padding: 0 16px 8px; overflow-x: auto; scrollbar-width: none; }
  .pill { display: inline-flex; align-items: center; gap: 6px; height: 34px; padding: 0 14px; border-radius: 17px; border: 1px solid rgb(255 255 255 / .14); font-size: 14px; font-weight: 500; color: var(--text2); flex: none; white-space: nowrap; }
  .pill.on { background: var(--text); color: var(--bg, #0B0C0E); border-color: var(--text); }
  .dot { display: inline-block; width: 8px; height: 8px; border-radius: 4px; flex: none; }
  .sep { width: 1px; height: 22px; background: rgb(255 255 255 / .14); align-self: center; flex: none; }
  .bar { height: 4px; margin: 4px 20px 0; border-radius: 2px; background: var(--b-track); overflow: hidden; }
  .bar i { display: block; height: 100%; width: 35%; background: var(--icon); animation: run 1.2s ease-in-out infinite; }
  @keyframes run { from { translate: -100% 0; } to { translate: 300% 0; } }
  .lbl { font-size: 13px; font-weight: 600; letter-spacing: .08em; text-transform: uppercase; color: var(--text2); padding: 14px 20px 6px; margin: 0; }
  .row { display: flex; align-items: center; min-height: 52px; border-top: 1px solid var(--line); padding-right: 8px; }
  .row:first-child { border-top: 0; }
  .go { flex: 1; min-width: 0; display: flex; align-items: center; gap: 14px; min-height: 52px; padding: 0 0 0 20px; text-align: left; color: var(--text); }
  .go .t { flex: 1; min-width: 0; font-size: 16px; white-space: nowrap; overflow: hidden; }
  .go small { color: var(--text2); font-size: 13px; margin-left: 8px; }
  .x { width: 44px; height: 44px; display: grid; place-items: center; color: var(--text2); border-radius: 22px; }
  .help { margin: 18px 20px 0; color: var(--text2); font-size: 14px; line-height: 1.4; }
  .none { color: var(--text2); padding: 8px 20px; }
  .grp { margin: 12px 12px 0; background: var(--surface); border-radius: 16px; overflow: hidden; }
  .gh { display: flex; align-items: center; gap: 10px; padding: 12px 14px 8px; }
  .gh .dot { width: 10px; height: 10px; border-radius: 5px; }
  .gh b { font-size: 17px; font-weight: 600; white-space: nowrap; overflow: hidden; }
  .gh .n { font-size: 13px; color: var(--text2); font-variant-numeric: tabular-nums; }
  .gh .st { margin-left: auto; font-size: 12px; color: var(--text2); }
  .hit { display: flex; gap: 12px; width: 100%; padding: 10px 10px 10px 14px; border-top: 1px solid var(--line); align-items: flex-start; text-align: left; }
  .hit.open:hover { background: rgb(255 255 255 / .04); }
  .hit .at { font-size: 12px; line-height: 20px; color: var(--text2); width: 42px; flex: none; font-variant-numeric: tabular-nums; }
  .hit .tx { flex: 1; min-width: 0; font-size: 15px; line-height: 20px; color: var(--text2); display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
  .hit mark { background: rgb(255 178 63 / .26); color: var(--text); border-radius: 3px; padding: 0 2px; }
  .chev { flex: none; color: var(--text2); margin-top: 0; }
  .more { font-size: 13px; color: var(--text2); padding: 12px 20px 0; }
  .foot { margin: 16px 20px 0; color: var(--text2); font-size: 13px; }
  @media (prefers-reduced-motion: reduce) { .bar i { animation: none; } }
</style>
