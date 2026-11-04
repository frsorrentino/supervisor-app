<script lang="ts">
  import type { GlyphKind } from './rail'
  // I glifi di stato dello stile «Studio e Diretta» (terza versione, 30/10): a contorno, con i significati di Telegram.
  // ❓ aspetta te, ▶ lavora con l'anello del contesto usato, ✓ fatto, ✗ rosso, il tratteggio per chi è fermo. Il quadrato
  // al posto del cerchio è l'account di lavoro, come nel badge di sempre (badge.ts).
  let { kind, ctx = null, size = 20, square = false, label }: { kind: GlyphKind; ctx?: number | null; size?: number; square?: boolean; label?: string } = $props()
  const col = $derived({ waiting: 'var(--wait)', busy: 'var(--busy)', done: 'var(--good)', red: 'var(--red)', still: 'var(--tx3)' }[kind])
  // Il contorno parte dall'alto e gira in senso orario, così l'arco del contesto comincia a mezzogiorno.
  const shape = $derived(square
    ? 'M10 1.8H13.7A4.5 4.5 0 0 1 18.2 6.3V13.7A4.5 4.5 0 0 1 13.7 18.2H6.3A4.5 4.5 0 0 1 1.8 13.7V6.3A4.5 4.5 0 0 1 6.3 1.8Z'
    : 'M10 1.8A8.2 8.2 0 1 1 10 18.2A8.2 8.2 0 1 1 10 1.8Z')
  const pct = $derived(ctx == null ? 0 : Math.max(0, Math.min(100, ctx)))
  // Effetto A (app-audit-grafica-animazioni-04-11, scelto da Franz il 04/11): ▶, ✓ e ✗ sono lo stesso tracciato con gli stessi
  // comandi (una spezzata di quattro punti e un tratto), così il browser interpola `d` e il ▶ si piega nella spunta o nella ✗.
  // I punti che avanzano cadono su un vertice già disegnato: con le punte tonde un tratto lungo zero sarebbe un puntino.
  const MARK: Record<string, string> = {
    busy: 'M8.3 6.7L13.6 10L8.3 13.3L8.3 6.7M8.3 6.7L8.3 6.7',
    done: 'M6.4 10.2L8.8 12.6L13.6 7.6L13.6 7.6M13.6 7.6L13.6 7.6',
    red: 'M7.3 7.3L12.7 12.7L12.7 12.7L12.7 12.7M12.7 7.3L7.3 12.7',
  }
  const mark = $derived(MARK[kind])
</script>

<svg width={size} height={size} viewBox="0 0 20 20" role={label ? 'img' : undefined} aria-label={label} aria-hidden={label ? undefined : 'true'}>
  {#if kind === 'still'}
    <path d={shape} fill="none" stroke={col} stroke-width="1.8" stroke-dasharray="2.6 2.4" />
  {:else}
    <path class="ring" d={shape} fill="none" stroke={col} stroke-opacity={kind === 'busy' ? 0.28 : 1} stroke-width={kind === 'busy' ? 2 : 1.8} />
    {#if kind === 'busy' && pct > 0}<path d={shape} fill="none" stroke={col} stroke-width="2" stroke-linecap="round" pathLength="100" stroke-dasharray="{pct} 100" />{/if}
    {#if kind === 'waiting'}
      <path d="M7.7 8a2.4 2.4 0 1 1 3.3 2.2c-.7.3-1 .8-1 1.5v.4" fill="none" stroke={col} stroke-width="1.7" stroke-linecap="round" />
      <circle cx="10" cy="14.7" r="1.05" fill={col} />
    {:else}
      <!-- Il ▶ pieno con un bordo di 1,2 come prima; spunta e ✗ solo tratto. -->
      <path class="mark" style:d="path('{mark}')" d={mark} fill={col} fill-opacity={kind === 'busy' ? 1 : 0} stroke={col} stroke-width={kind === 'busy' ? 1.2 : 1.8} stroke-linecap="round" stroke-linejoin="round" />
    {/if}
  {/if}
</svg>

<style>
  svg { flex: none; display: block; }
  .mark { transition: d 420ms var(--spring-bounce), stroke 300ms var(--ease-out, cubic-bezier(0.23, 1, 0.32, 1)), fill-opacity 150ms linear, stroke-width 150ms linear; }
  .ring { transition: stroke 300ms cubic-bezier(0.23, 1, 0.32, 1), stroke-opacity 300ms linear; }
  @media (prefers-reduced-motion: reduce) { .mark, .ring { transition: none; } }
</style>
