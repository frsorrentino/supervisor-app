<script lang="ts">
  import type { Snippet } from 'svelte'
  import CatIcon from './CatIcon.svelte'
  import { altDigit, spoken } from './iconRow'
  import { countOf, TABS, toneOf, type SessionTab, type TabCounts } from './sessionTabs'
  import {
    barStep, batteryTone, BAT, BAT_IN, GLYPH_GAP, ICON, ICON_GAP, IN_TEXT, ITEM_GAP, modelText, NUM, NUM_GAP, PAD, PAD_SHEET, remaining, RING, RING_IN,
    ringTone, SIDES_GAP, TEXT, TEXT_SMALL, type BarTone,
  } from './statusBar'
  import { t } from './t'

  // La barra di stato (app-barra-stato-28-11, variante C): sotto il titolo della sessione, e nel foglio aperto della master, al posto della
  // riga di icone. A sinistra Per te, Risultati e Modifiche col loro numero (a zero solo l'icona): un tocco fa scendere la loro tendina,
  // un altro sull'icona aperta la richiude. A destra modello · effort, la quota delle 5 ore come batteria (quanto resta) e il contesto come
  // anellino: un tocco apre Dettagli. Con poco spazio il passo lo sceglie statusBar.barStep dalla larghezza della barra: prima il testo
  // più piccolo, poi il modello senza effort, poi le percentuali dentro i glifi, infine solo i glifi; i numeri delle icone restano.
  // Alt+1…5 sceglie come prima (Chat · Per te · Risultati · Modifiche · Dettagli), Esc richiude.
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
  const ICONS: Record<string, string> = { perte: 'hand', risultati: 'eye', modifiche: 'diff' }
  const NAMES = $derived<Record<string, string>>({ perte: t.forYou, risultati: t.watchResults, modifiche: t.changesTitle })
  const TONE: Record<BarTone, string> = { neutral: 'var(--tx2)', warn: 'var(--wait)', alert: 'var(--red)' }

  let width = $state(0)
  const left = $derived(quota ? remaining(quota.h5) : null)
  const parts = $derived({ counts: LEFT.map((k) => countOf(k, counts)), model, effort, left, ctx })
  // Nel foglio della master i margini sono 16, come le sue schede (app-foglio-master-testata-28-11).
  const pad = $derived(sheet ? PAD_SHEET : PAD)
  // Prima della misura la barra intera; misurata, il primo passo che ci sta.
  const step = $derived(width > 0 ? barStep(width, parts, 1, pad) : 'full')
  const text = $derived(step === 'full' ? TEXT : TEXT_SMALL)
  const mtext = $derived(modelText(model, effort, step))
  const inside = $derived(step === 'inside')
  const pcts = $derived(step === 'full' || step === 'small' || step === 'noEffort')
  const batC = $derived(left == null ? '' : TONE[batteryTone(left)])
  const ringC = $derived(ctx == null ? '' : TONE[ringTone(ctx)])
  const stateLabel = $derived(t.barState([model, effort].filter(Boolean).join(' · '), left, ctx))

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

<div class="sbar" class:sheet data-step={step} data-testid="barra-stato"
  style:--pad="{pad}px" style:--sides="{SIDES_GAP}px" style:--icon="{ICON}px" style:--igap="{ICON_GAP}px" style:--ngap="{NUM_GAP}px" style:--num="{NUM}px"
  style:--t="{text}px" style:--item="{ITEM_GAP}px" style:--ggap="{GLYPH_GAP}px" style:--in="{IN_TEXT}px">
  <div class="tl" role="tablist" aria-label={label} bind:clientWidth={width}>
    <span class="l">
      {#each LEFT as k (k)}
        {@const n = countOf(k, counts)}
        <button class="nt {toneOf(k)}" class:on={active === k} type="button" role="tab" aria-selected={active === k} aria-label={spoken(NAMES[k], n)}
          title={NAMES[k]} data-testid="barra-{k}" onclick={() => onPick(k)}>
          <CatIcon name={ICONS[k]} size={ICON} />{#if n > 0}<b>{n}</b>{/if}
        </button>
      {/each}
    </span>
    <button class="r" class:on={active === 'dettagli'} type="button" role="tab" aria-selected={active === 'dettagli'} aria-label={stateLabel} title={stateLabel}
      data-testid="barra-dettagli" onclick={() => onPick('dettagli')}>
      {#if mtext}<span class="mdl">{mtext}</span>{/if}
      {#if left != null}
        <span class="gly" class:stale={quota?.stale} style:color={batC} data-testid="barra-batteria">
          <span class="bat" class:in={inside} style:width="{(inside ? BAT_IN : BAT) - 3.5}px"><i style:width="calc((100% - 2.6px) * {left / 100})"></i>{#if inside}<b>{left}</b>{/if}</span>
          {#if pcts}<span>{left}%</span>{/if}
        </span>
      {/if}
      {#if ctx != null}
        {@const d = inside ? RING_IN : RING}
        <span class="gly" style:color={ringC} data-testid="barra-contesto">
          <span class="rng" style:width="{d}px" style:height="{d}px">
            <svg viewBox="0 0 {d} {d}" width={d} height={d} aria-hidden="true">
              <circle cx={d / 2} cy={d / 2} r={d / 2 - (inside ? 1 : 1.4)} class="trk" stroke-width={inside ? 1.6 : 2.2} />
              <circle cx={d / 2} cy={d / 2} r={d / 2 - (inside ? 1 : 1.4)} pathLength="100" stroke-dasharray="{Math.max(0, Math.min(100, ctx))} 100"
                stroke-width={inside ? 1.6 : 2.2} transform="rotate(-90 {d / 2} {d / 2})" />
            </svg>
            {#if inside}<b>{ctx}</b>{/if}
          </span>
          {#if pcts}<span>{ctx}%</span>{/if}
        </span>
      {/if}
    </button>
  </div>
  {#if more}<span class="mr">{@render more()}</span>{/if}
</div>

<style>
  /* Le misure di statusBar.ts, perché la regola dei passi e il disegno restino gli stessi. */
  .sbar { flex: none; display: flex; align-items: center; height: 28px; box-sizing: content-box; background: var(--bg); border-bottom: 1px solid var(--line);
    overflow: hidden; user-select: none; -webkit-user-select: none; -webkit-tap-highlight-color: transparent; }
  .sbar.sheet { background: var(--s1); }
  .tl { flex: 1; min-width: 0; height: 28px; display: flex; align-items: center; justify-content: space-between; gap: var(--sides); padding: 0 var(--pad); }
  .l { display: flex; align-items: center; gap: var(--igap); flex: none; }
  /* Il tocco prende anche metà dello spazio accanto: la riga resta quella del mockup. */
  .nt { display: inline-flex; align-items: center; gap: var(--ngap); height: 28px; padding: 0 5px; margin: 0 -5px; color: var(--tx2); white-space: nowrap; transition: color .2s; }
  .nt.on, .r.on { color: var(--tx); }
  .nt b { font: 500 var(--num)/1 var(--mono); font-variant-numeric: tabular-nums; }
  .nt.wait b { color: var(--wait); }
  .nt.new b { color: var(--good); }
  .nt.count b { color: var(--tx2); }
  .nt:focus-visible, .r:focus-visible { outline: 2px solid var(--tx2); outline-offset: -2px; border-radius: 6px; }
  .r { display: flex; align-items: center; gap: var(--item); flex: none; height: 28px; padding: 0 6px; margin-right: -6px; color: var(--tx2); white-space: nowrap; }
  .mdl { font: 500 var(--t)/1 var(--mono); letter-spacing: 0; color: var(--tx2); }
  .r.on .mdl { color: var(--tx); }
  .gly { display: inline-flex; align-items: center; gap: var(--ggap); font: 500 var(--t)/1 var(--mono); }
  .gly.stale { opacity: .55; }
  /* La batteria: il corpo col bordo, la punta fuori (3,5), il pieno quanto resta; col numero dentro il pieno si fa leggero. */
  .bat { position: relative; display: inline-grid; place-items: center; box-sizing: border-box; height: 10px; margin-right: 3.5px; border: 1.3px solid currentColor; border-radius: 2.5px; }
  .bat::after { content: ''; position: absolute; right: -3.7px; top: 50%; width: 2px; height: 3.6px; transform: translateY(-50%); border-radius: 0 1px 1px 0; background: currentColor; }
  .bat i { position: absolute; left: 1.3px; top: 1.3px; bottom: 1.3px; border-radius: 1px; background: currentColor; }
  .bat.in { height: 12px; }
  .bat.in i { opacity: .3; }
  .bat b, .rng b { position: relative; font: 600 var(--in)/1 var(--mono); letter-spacing: -.02em; color: currentColor; }
  .rng { position: relative; display: inline-grid; place-items: center; flex: none; }
  .rng svg { position: absolute; inset: 0; }
  .rng circle { fill: none; stroke: currentColor; }
  .rng circle.trk { stroke: var(--s3); }
  .sheet .rng circle.trk { stroke: var(--line); }
  .mr { flex: none; display: flex; align-items: center; margin-right: 4px; }
  @media (prefers-reduced-motion: reduce) { .nt { transition: none; } }
</style>
