<script lang="ts">
  import type { NightPage } from './night'
  import type { GlyphKind } from './rail'
  import StateGlyph from './StateGlyph.svelte'
  import { locale } from './lang'
  import { t } from './t'
  // «La notte scorsa» nella colonna Esiti della plancia (pwa2, Franz 30/10): una riga di registro per lavoro col suo glifo.
  // Un lavoro fermo su una domanda qui resta solo una riga grigia: la domanda, coi suoi comandi, sta in «Ti aspettano».
  let { night, loading = false, onNight }: { night: NightPage | null; loading?: boolean; onNight: () => void } = $props()
  const KIND: Record<string, GlyphKind> = { ok: 'done', running: 'busy', stopped: 'red', question: 'still', partial: 'still' }
  const MAX = 6
  const hm = (s: number) => new Intl.DateTimeFormat(locale, { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date(s * 1000))
</script>

<div class="vsec"><span class="ic"><span class="ms" aria-hidden="true">bedtime</span></span>{t.deskLastNight}{#if night}<span class="rt">{hm(night.start)}-{hm(night.end)}</span>{/if}</div>
<div class="vlist">
  {#if night}
    {#each night.cards.slice(0, MAX) as c (c.id)}
      <button class="vrow" onclick={onNight}>
        <span class="ic"><StateGlyph kind={KIND[c.icon] ?? 'still'} /></span>
        <span class="tx"><b>{c.title}</b>{#if c.folder || c.end}<small class="mono">{[c.folder, c.end ? t.deskNightEnd(hm(c.end)) : null].filter(Boolean).join('  ')}</small>{/if}</span>
      </button>
    {/each}
    {#if night.cards.length > MAX || !night.cards.length}
      <button class="vrow" onclick={onNight}><span class="ic"></span><span class="tx"><b>{t.deskNightAll}</b></span><span class="mono">{night.cards.length}</span><span class="ms" aria-hidden="true" style="color:var(--tx3)">chevron_right</span></button>
    {/if}
  {:else}
    <div class="vrow"><span class="ic"></span><span class="tx"><small>{loading ? t.nightLoading : t.dashNightLater}</small></span></div>
  {/if}
</div>
