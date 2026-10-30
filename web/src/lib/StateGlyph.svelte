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
</script>

<svg width={size} height={size} viewBox="0 0 20 20" role={label ? 'img' : undefined} aria-label={label} aria-hidden={label ? undefined : 'true'}>
  {#if kind === 'busy'}
    <path d={shape} fill="none" stroke={col} stroke-opacity=".28" stroke-width="2" />
    {#if pct > 0}<path d={shape} fill="none" stroke={col} stroke-width="2" stroke-linecap="round" pathLength="100" stroke-dasharray="{pct} 100" />{/if}
    <path d="M8.3 6.7 13.6 10 8.3 13.3Z" fill={col} stroke={col} stroke-width="1.2" stroke-linejoin="round" />
  {:else if kind === 'still'}
    <path d={shape} fill="none" stroke={col} stroke-width="1.8" stroke-dasharray="2.6 2.4" />
  {:else}
    <path d={shape} fill="none" stroke={col} stroke-width="1.8" />
    {#if kind === 'waiting'}
      <path d="M7.7 8a2.4 2.4 0 1 1 3.3 2.2c-.7.3-1 .8-1 1.5v.4" fill="none" stroke={col} stroke-width="1.7" stroke-linecap="round" />
      <circle cx="10" cy="14.7" r="1.05" fill={col} />
    {:else if kind === 'done'}
      <path d="M6.4 10.2l2.4 2.4 4.8-5" fill="none" stroke={col} stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
    {:else}
      <path d="M7.3 7.3l5.4 5.4M12.7 7.3l-5.4 5.4" stroke={col} stroke-width="1.8" stroke-linecap="round" />
    {/if}
  {/if}
</svg>

<style>
  svg { flex: none; display: block; }
</style>
