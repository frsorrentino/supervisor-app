<script lang="ts">
  import type { Snippet } from 'svelte'
  import IconRow from './IconRow.svelte'
  import { altDigit, type IconTab } from './iconRow'
  import { countOf, TABS, toneOf, type SessionTab, type TabCounts } from './sessionTabs'
  import { barStep, GLYPH, GLYPH_GAP, ITEM_GAP, ITEM_GAP_SMALL, modelText, quotaText, quotaTone, ringTone, TEXT, TEXT_SMALL, type BarTone } from './statusBar'
  import { t } from './t'

  // La barra della sessione (app-barra-ibrida-28-11, H1 di dalla-master/barra-ibrida-28-11.html; prima la barra C sottile): sotto il titolo,
  // e nel foglio aperto della master al posto dell'ultimo esito, la capsula della riga di icone di prima (IconRow, alta 50 con la pillola
  // elastica del Rubber dock e il nome sulla scheda aperta) con Per te, Risultati e Modifiche e i loro numeri: un tocco fa scendere la loro
  // tendina, un altro sull'aperta la richiude. Nella stessa capsula, dopo un separatore, lo stato: modello · effort, la quota delle 5 ore
  // (un orologio e «5h 11%», quanto è usato, al posto della batteria) e l'anellino del contesto: un tocco apre Dettagli. Con poco spazio lo
  // stato si riduce a passi (statusBar.barStep); le icone coi numeri restano. Alt+1…5 come prima (Chat · Per te · Risultati · Modifiche ·
  // Dettagli), Esc richiude.
  let { counts, active, onPick, onEsc, model, effort, quota, ctx, label, sheet = false, more = null, keyScope = null }: {
    counts: TabCounts
    /** La scheda aperta; `chat` quando la tendina è chiusa. */
    active: SessionTab
    /** Un tocco su un'icona o sullo stato (`dettagli`), anche su quella aperta: decide chi chiama. */
    onPick: (key: SessionTab) => void
    onEsc?: () => void
    /** Il modello corto («Opus 5») e l'effort, come la testata li mostra, con la scelta appena fatta. */
    model: string | null
    effort: string | null
    /** La quota delle 5 ore dell'account: quanto è usato; null senza quota. */
    quota: { h5: number; stale?: boolean } | null
    ctx: number | null
    label: string
    /** Nel foglio della master: lo sfondo del foglio. */
    sheet?: boolean
    /** Il ⋮ della sessione quando nessuna testata lo prende. */
    more?: Snippet | null
    keyScope?: HTMLElement | null
  } = $props()

  const LEFT: SessionTab[] = ['perte', 'risultati', 'modifiche']
  const ICONS: Record<string, string> = { perte: 'svg:hand', risultati: 'svg:eye', modifiche: 'svg:diff' }
  const NAMES = $derived<Record<string, string>>({ perte: t.forYou, risultati: t.watchResults, modifiche: t.changesTitle })
  const tabs = $derived<IconTab[]>(LEFT.map((k) => ({ key: k, icon: ICONS[k], label: NAMES[k], count: countOf(k, counts), tone: toneOf(k) })))
  const TONE: Record<BarTone, string> = { neutral: 'var(--tx2)', warn: 'var(--wait)', alert: 'var(--red)' }

  let width = $state(0)
  let moreW = $state(0)
  let rest = $state(0)
  const used = $derived(quota ? Math.round(quota.h5) : null)
  const parts = $derived({ model, effort, used, ctx })
  // Prima della misura tutto; misurate la riga e le schede a riposo, il primo passo che ci sta.
  // Il ⋮ conta solo finché sta nella riga: preso dalla testata della colonna, la sua ultima misura resterebbe.
  const step = $derived(width > 0 && rest > 0 ? barStep(width - (more ? moreW : 0), rest, parts) : 'full')
  const small = $derived(step !== 'full' && step !== 'noEffort')
  const pcts = $derived(step !== 'noPct')
  const mtext = $derived(modelText(model, effort, step))
  const quotaC = $derived(used == null ? '' : TONE[quotaTone(used)])
  const ringC = $derived(ctx == null ? '' : TONE[ringTone(ctx)])
  const stateLabel = $derived(t.barState([model, effort].filter(Boolean).join(' · '), used, ctx))

  /** Alt+1…5 ed Esc col fuoco dentro `keyScope` (la sessione), come la riga di icone di prima; mai da un dialogo aperto. */
  function pageKey(e: KeyboardEvent) {
    if (e.defaultPrevented || e.isComposing || e.ctrlKey || e.metaKey) return
    if (e.target instanceof Element && e.target.closest('dialog[open]')) return
    if (e.key === 'Escape') {
      if (!onEsc || e.altKey || e.shiftKey) return
      e.preventDefault()
      onEsc()
      return
    }
    if (!e.altKey || e.repeat) return
    const m = /^Digit(\d)$/.exec(e.code)
    const i = m ? altDigit(Number(m[1]), TABS.length) : null
    if (i == null) return
    e.preventDefault()
    onPick(TABS[i])
  }
  $effect(() => {
    const el: HTMLElement | Window = keyScope ?? window
    const on = (e: Event) => pageKey(e as KeyboardEvent)
    el.addEventListener('keydown', on)
    return () => el.removeEventListener('keydown', on)
  })
</script>

<div class="sbar line" class:sheet data-step={step} data-testid="barra-stato" bind:clientWidth={width}
  style:--t="{small ? TEXT_SMALL : TEXT}px" style:--gap="{small ? ITEM_GAP_SMALL : ITEM_GAP}px" style:--glyph="{GLYPH}px" style:--ggap="{GLYPH_GAP}px">
  <IconRow {tabs} {active} onPick={(k) => onPick(k as SessionTab)} {label} keys={false} bind:rest trailing={status} />
  {#if more}<span class="mr" bind:offsetWidth={moreW}>{@render more()}</span>{/if}
</div>

{#snippet status()}
  <button class="st" class:on={active === 'dettagli'} type="button" role="tab" aria-selected={active === 'dettagli'} aria-label={stateLabel} title={stateLabel}
    data-testid="barra-dettagli" onclick={() => onPick('dettagli')}>
    {#if mtext}<span class="mdl">{mtext}</span>{/if}
    {#if used != null}
      <span class="gly" class:stale={quota?.stale} style:color={quotaC} data-testid="barra-quota">
        <svg viewBox="0 0 24 24" width={GLYPH} height={GLYPH} aria-hidden="true"><circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="2.4" /><path d="M12 7v5l3.2 2" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" /></svg>
        {#if pcts}<span>{quotaText(used)}</span>{/if}
      </span>
    {/if}
    {#if ctx != null}
      <span class="gly" style:color={ringC} data-testid="barra-contesto">
        <svg viewBox="0 0 12 12" width={GLYPH} height={GLYPH} aria-hidden="true">
          <circle cx="6" cy="6" r="4.6" class="trk" stroke-width="2.2" />
          <circle cx="6" cy="6" r="4.6" pathLength="100" stroke-dasharray="{Math.max(0, Math.min(100, ctx))} 100" stroke-width="2.2" transform="rotate(-90 6 6)" />
        </svg>
        {#if pcts}<span>{ctx}%</span>{/if}
      </span>
    {/if}
  </button>
{/snippet}

<style>
  /* La riga sotto il titolo: 6 sopra, la capsula da 50, 8 sotto e la riga sottile; la capsula al centro, a 12 dai lati. */
  .line { flex: none; display: flex; align-items: center; justify-content: center; gap: 8px; min-width: 0; padding: 6px 12px 8px; background: var(--bg);
    border-bottom: 1px solid var(--line); }
  .line.sheet { background: var(--s1); }
  /* Lo stato, nella capsula dopo un separatore: 4 di margine, la riga da 1, 10 dopo, 6 in fondo (statusBar.SEP, END). */
  .st { display: flex; align-items: center; gap: var(--gap); height: 28px; margin-left: 4px; padding: 0 6px 0 10px; border-left: 1px solid var(--line);
    color: var(--tx2); font: 500 var(--t)/1 var(--mono); white-space: nowrap; border-radius: 0 999px 999px 0; }
  .st.on, .st.on .mdl { color: var(--tx); }
  .st:focus-visible { outline: 2px solid var(--tx2); outline-offset: -2px; }
  .gly { display: inline-flex; align-items: center; gap: var(--ggap); }
  .gly.stale { opacity: .55; }
  .gly svg { flex: none; }
  .gly circle { fill: none; stroke: currentColor; }
  .gly circle.trk { stroke: var(--s3); }
  .mr { flex: none; display: flex; align-items: center; }
</style>
