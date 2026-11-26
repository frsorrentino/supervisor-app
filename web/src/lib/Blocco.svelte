<script lang="ts">
  import type { Snippet } from 'svelte'
  import { t } from './t'
  import { ferma, molla, motore, ridotto } from './molla'
  // Un blocco della chat ridotto a una riga (app-formato-unico-12-11, variante B scelta da Franz il 12/11 alle 07:29, tavola 6 del
  // mockup): icona, nome, quante voci e la prima; il tocco lo apre sul posto, con le voci sotto la stessa riga, e lo richiude. Lo
  // stesso per «Per te», «Azioni» e «Risultati». `amber`: il blocco di ciò che aspetta la persona. `extra`: un tasto a destra, prima
  // della freccia (il tasto principale della prima voce, la × dei risultati). La prima voce sta su una riga, tagliata senza «…».
  // `pill` (app-riquadri-chiusi-16-11): chiuso è una pillola con icona, nome e numero, nella riga dei riquadri della sessione;
  // aperto torna il blocco intero, a tutta larghezza.
  let { icon, title, n, first = null, hint = null, open, amber = false, pill = false, stack = 0, fan = [], cls = '', testid = undefined, onToggle, extra = null, children }: {
    icon: 'hand' | 'bolt' | 'eye' | 'warn' | 'layers' | 'flag'; title: string; n: number | string; first?: string | null; hint?: string | null; open: boolean
    amber?: boolean; pill?: boolean
    /** Voce 3 di app-animazioni-seconde-26-11 (Per te a pila): chiuso, quanti bordi di voci si vedono sotto, 0-2. */
    stack?: number
    /** Voce 3 (Fan cards, Franz 26/11 20:38): i titoli delle voci sotto, per il ventaglio al passaggio del mouse. */
    fan?: string[]; cls?: string; testid?: string; onToggle: () => void; extra?: Snippet | null; children: Snippet
  } = $props()
  // Voce 3, la parte Fan cards dell'originale (app-animazioni-fedeli-26-11): col mouse sopra la pila chiusa le carte si aprono a
  // ventaglio dal bordo in basso: di lato 78 px per livello, 12° di rotazione, le esterne 12 px più giù (molla 220 18); quella sotto il
  // puntatore sale di 22 px e scala a 1,06 (420 20, 420 22) e passa davanti. Uscendo tornano nella pila. Moto ridotto: niente ventaglio.
  const F = molla(0, 220, 18)
  const L = [molla(0, 420, 20), molla(0, 420, 20), molla(0, 420, 20)]
  const S = [molla(1, 420, 22), molla(1, 420, 22), molla(1, 420, 22)]
  let hot = $state(-1)
  let fanning = $state(false)
  let pillEl = $state<HTMLButtonElement>()
  const dOf = (i: number) => (i === 1 ? -1 : i === 2 ? 1 : 0)
  function paint() {
    if (!pillEl) return
    const x = F.x
    fanning = x > 0.001 || !ferma(F)
    pillEl.style.transform = `translateY(${L[0].x.toFixed(2)}px) scale(${S[0].x.toFixed(4)})`
    pillEl.querySelectorAll<HTMLElement>('.fc').forEach((el) => {
      const k = Number(el.dataset.k), d = dOf(k)
      const y = (1 - x) * 10 * k + x * Math.abs(d) * 12 + L[k].x
      const sc = ((1 - x) * (1 - 0.05 * k) + x) * S[k].x
      el.style.transform = `translate(${(d * x * 78).toFixed(2)}px, ${y.toFixed(2)}px) rotate(${(d * x * 12).toFixed(2)}deg) scale(${sc.toFixed(4)})`
    })
  }
  const fm = motore([F, ...L, ...S], paint)
  function setHot(i: number) { if (i === hot) return; hot = i; L.forEach((l, k) => (l.t = k === i ? -22 : 0)); S.forEach((q, k) => (q.t = k === i ? 1.06 : 1)); fm.vai() }
  function fanIn(e: PointerEvent) { if (e.pointerType !== 'mouse' || !fan.length || ridotto()) return; F.t = 1; fm.vai() }
  function fanMove(e: PointerEvent) {
    if (e.pointerType !== 'mouse' || F.t !== 1 || !pillEl) return
    const r = pillEl.getBoundingClientRect(), dx = (e.clientX - (r.left + r.width / 2)) / 78
    let best = 0
    for (let k = 0; k <= Math.min(2, fan.length); k++) if (Math.abs(dOf(k) - dx) < Math.abs(dOf(best) - dx)) best = k
    setHot(best)
  }
  function fanOut() { if (F.t === 0) return; setHot(-1); F.t = 0; fm.vai() }
  $effect(() => () => fm.stop())
  const key = (e: KeyboardEvent) => { if (e.target === e.currentTarget && (e.key === 'Enter' || e.key === ' ')) { e.preventDefault(); onToggle() } }
</script>

{#snippet glyph()}
  <svg class="ic" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
    {#if icon === 'hand'}<path d="M18 11V6a2 2 0 0 0-4 0M14 10V4a2 2 0 0 0-4 0v2M10 10.5V6a2 2 0 0 0-4 0v8" /><path d="M18 8a2 2 0 1 1 4 0v6a8 8 0 0 1-8 8h-2c-2.8 0-4.5-.86-5.99-2.34l-3.6-3.6a2 2 0 0 1 2.83-2.82L7 15" />
    {:else if icon === 'bolt'}<path d="M13 2.5 4.5 13.5H11l-1 8 8.5-11H12z" />
    {:else if icon === 'warn'}<path d="M12 4 3 20h18L12 4z" /><path d="M12 10v5M12 17.5h.01" />
    {:else if icon === 'layers'}<path d="M12 3 2.5 8 12 13l9.5-5L12 3z" /><path d="M2.5 16 12 21l9.5-5M2.5 12 12 17l9.5-5" />
    {:else if icon === 'flag'}<path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z" /><path d="M4 22v-7" />
    {:else}<path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z" /><circle cx="12" cy="12" r="3" />{/if}
  </svg>
{/snippet}

{#if pill && !open}
  <button class="pill {cls}" bind:this={pillEl} class:amber class:s1={stack === 1} class:s2={stack >= 2} class:fanning data-testid={testid} aria-expanded="false" aria-label={t.boxOpen(title)} onclick={onToggle} onpointerenter={fanIn} onpointermove={fanMove} onpointerleave={fanOut}>
    {#each fan.slice(0, Math.min(2, stack)) as ft, i}<span class="fc" class:hot={hot === i + 1} data-k={i + 1} aria-hidden="true">{ft}</span>{/each}{#if stack > 0}<span class="face" aria-hidden="true"></span>{/if}
    {@render glyph()}<b>{title}</b><span class="n">{n}</span>
  </button>
{:else}
<div class="blk {cls}" class:amber class:open class:s1={!open && stack === 1} class:s2={!open && stack >= 2} data-testid={testid}>
  <div class="strip" role="button" tabindex="0" aria-expanded={open} aria-label={open ? t.boxClose(title) : t.boxOpen(title)} onclick={onToggle} onkeydown={key}>
    {@render glyph()}
    <b>{title}</b><span class="n">{n}</span>
    {#if open}{#if hint}<span class="first">{hint}</span>{:else}<span class="sp"></span>{/if}{:else}<span class="first">{first ?? ''}</span>{/if}
    {#if extra}{@render extra()}{/if}
    <svg class="chev" viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"><path d="M7 10l5 5 5-5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
  </div>
  {#if open}{@render children()}{/if}
</div>
{/if}

<style>
  .blk { margin: 0 12px 8px; border-radius: 14px; background: var(--s1); border: 1px solid var(--line); color: var(--tx); overflow: hidden; flex: none; }
  .blk.amber { background: color-mix(in srgb, var(--wait) 9%, var(--s1)); border-color: color-mix(in srgb, var(--wait) 38%, var(--line)); }
  /* Aperto, il blocco di ciò che aspetta torna una scheda neutra: l'ambra resta sulla voce che aspetta una risposta. */
  .blk.amber.open { background: var(--s1); border-color: var(--line); }
  .strip { display: flex; align-items: center; gap: 8px; min-height: 44px; padding: 4px 10px 4px 12px; cursor: pointer; }
  .ic { flex: none; color: var(--tx2); }
  .amber .ic, .amber .n { color: var(--wait); }
  b { font: 600 15.5px var(--display); white-space: nowrap; }
  .n { font-size: 13px; color: var(--tx2); }
  .first, .sp { flex: 1; min-width: 0; }
  .first { font-size: 14px; color: var(--tx2); white-space: nowrap; overflow: hidden; text-overflow: clip; }
  .chev { flex: none; color: var(--tx3); transition: transform .2s; }
  .open .chev { transform: rotate(180deg); }
  @media (prefers-reduced-motion: reduce) { .chev { transition: none; } }
  .strip:focus-visible { outline: 2px solid var(--wait); outline-offset: -2px; border-radius: 14px; }
  /* La pillola: icona, nome e numero; 40 px d'altezza per il dito. */
  .pill { display: inline-flex; align-items: center; gap: 6px; flex: none; min-height: 40px; padding: 4px 13px 4px 10px; border-radius: 999px;
    background: var(--s1); border: 1px solid var(--line); color: var(--tx); white-space: nowrap; cursor: pointer;
    transition: background-color 150ms, transform var(--spring-bounce-t) var(--spring-bounce); }
  .pill.amber { background: color-mix(in srgb, var(--wait) 9%, var(--s1)); border-color: color-mix(in srgb, var(--wait) 38%, var(--line)); }
  .pill b { font: 600 14.5px var(--display); }
  /* Voce 3, fedele all'originale (Notification stack; app-animazioni-fedeli-26-11): sotto la pillola le altre voci come carte
     impilate, al massimo due, allineate in basso, ognuna 10 px più giù e scalata del 5 % per livello (0,95, 0,90). */
  .pill.s1, .pill.s2 { position: relative; isolation: isolate; }
  .pill.s1 { margin-bottom: 10px; }
  .pill.s2 { margin-bottom: 20px; }
  .pill.s1::before, .pill.s2::before, .pill.s2::after { content: ''; position: absolute; inset: -1px; border-radius: inherit; pointer-events: none; transform-origin: 50% 100%;
    background: var(--s1); border: 1px solid var(--line); }
  .pill.amber.s1::before, .pill.amber.s2::before, .pill.amber.s2::after { background: color-mix(in srgb, var(--wait) 6%, var(--s1)); border-color: color-mix(in srgb, var(--wait) 25%, var(--line)); }
  /* Dentro la pillola isolata il fondo suo dipingerebbe sotto le carte: lo porta `.face`, sopra le carte e sotto il testo. */
  .pill.s1, .pill.s2 { background: transparent; border-color: transparent; }
  .face { position: absolute; inset: -1px; z-index: -1; border-radius: inherit; background: var(--s1); border: 1px solid var(--line); pointer-events: none; }
  .pill.amber .face { background: color-mix(in srgb, var(--wait) 9%, var(--s1)); border-color: color-mix(in srgb, var(--wait) 38%, var(--line)); }
  .pill.s1::before, .pill.s2::before { z-index: -2; transform: translateY(10px) scale(.95); }
  .pill.s2::after { z-index: -3; transform: translateY(20px) scale(.9); }
  .pill.fanning::before, .pill.fanning::after { visibility: hidden; }
  .fc { position: absolute; inset: -1px; z-index: -2; display: flex; align-items: center; padding: 0 13px; border-radius: 999px; transform-origin: 50% 100%; pointer-events: none; overflow: hidden;
    font-size: 13px; color: var(--tx2); background: color-mix(in srgb, var(--wait) 6%, var(--s1)); border: 1px solid color-mix(in srgb, var(--wait) 25%, var(--line)); visibility: hidden; }
  .pill.fanning .fc { visibility: visible; }
  .fc[data-k='2'] { z-index: -3; }
  .fc.hot { z-index: 5; }
  /* Voce 3: sotto il riquadro chiuso i bordi delle altre voci, come carte impilate; aperto, le voci scendono con la molla. */
  .blk.s1 { box-shadow: 0 4px 0 -1px color-mix(in srgb, var(--wait) 14%, var(--s1)), 0 4px 0 0 color-mix(in srgb, var(--wait) 30%, var(--line)); margin-bottom: 12px; }
  .blk.s2 { box-shadow: 0 4px 0 -1px color-mix(in srgb, var(--wait) 14%, var(--s1)), 0 4px 0 0 color-mix(in srgb, var(--wait) 30%, var(--line)), 0 8px 0 -2px color-mix(in srgb, var(--wait) 8%, var(--s1)), 0 8px 0 -1px color-mix(in srgb, var(--wait) 18%, var(--line)); margin-bottom: 16px; }
  .blk.open > :global(*:not(.strip)) { animation: blk-down var(--spring-bounce-t) var(--spring-bounce) both; }
  @keyframes blk-down { from { transform: translateY(-10px); opacity: 0; } }
  .pill:active { transform: scale(.97); }
  @media (hover: hover) and (pointer: fine) { .pill:hover { background: var(--s2); } .pill.amber:hover { background: color-mix(in srgb, var(--wait) 14%, var(--s1)); } }
  .pill:focus-visible { outline: 2px solid var(--wait); outline-offset: 2px; }
  @media (prefers-reduced-motion: reduce) { .pill { transition: background-color 150ms; } .pill:active { transform: none; } }
</style>
