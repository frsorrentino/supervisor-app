<script lang="ts">
  import type { Snippet } from 'svelte'
  import { t } from './t'
  // Un blocco della chat ridotto a una riga (app-formato-unico-12-11, variante B scelta da Franz il 12/11 alle 07:29, tavola 6 del
  // mockup): icona, nome, quante voci e la prima; il tocco lo apre sul posto, con le voci sotto la stessa riga, e lo richiude. Lo
  // stesso per «Per te», «Azioni» e «Risultati». `amber`: il blocco di ciò che aspetta la persona. `extra`: un tasto a destra, prima
  // della freccia (il tasto principale della prima voce, la × dei risultati). La prima voce sta su una riga, tagliata senza «…».
  // `pill` (app-riquadri-chiusi-16-11): chiuso è una pillola con icona, nome e numero, nella riga dei riquadri della sessione;
  // aperto torna il blocco intero, a tutta larghezza.
  let { icon, title, n, first = null, hint = null, open, amber = false, pill = false, cls = '', testid = undefined, onToggle, extra = null, children }: {
    icon: 'hand' | 'bolt' | 'eye' | 'warn' | 'layers' | 'flag'; title: string; n: number | string; first?: string | null; hint?: string | null; open: boolean
    amber?: boolean; pill?: boolean; cls?: string; testid?: string; onToggle: () => void; extra?: Snippet | null; children: Snippet
  } = $props()
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
  <button class="pill {cls}" class:amber data-testid={testid} aria-expanded="false" aria-label={t.boxOpen(title)} onclick={onToggle}>
    {@render glyph()}<b>{title}</b><span class="n">{n}</span>
  </button>
{:else}
<div class="blk {cls}" class:amber class:open data-testid={testid}>
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
  .pill:active { transform: scale(.97); }
  @media (hover: hover) and (pointer: fine) { .pill:hover { background: var(--s2); } .pill.amber:hover { background: color-mix(in srgb, var(--wait) 14%, var(--s1)); } }
  .pill:focus-visible { outline: 2px solid var(--wait); outline-offset: 2px; }
  @media (prefers-reduced-motion: reduce) { .pill { transition: background-color 150ms; } .pill:active { transform: none; } }
</style>
