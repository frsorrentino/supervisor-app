<script lang="ts">
  import { onMount } from 'svelte'
  import { t } from './t'
  // La pillola + e › nel bordo fra la home e le colonne (Franz 03/11 18:22, app-sessione-sostituisci-03-11): accanto a ogni
  // sessione della home, + la apre accanto, › al posto di quella in vista. Tenue a riposo, chiara al passaggio con le due
  // scritte. La sessione già in colonna non ha la pillola: un filo bianco, centrato sulla sua riga, la lega al riquadro.
  let { shown, current, homeRight, onAdd, onSwap }: { shown: string[]; current: string | null; homeRight: boolean; onAdd: (n: string) => void; onSwap: (n: string) => void } = $props()
  type Spot = { name: string; y: number; x: number }
  let spots = $state<Spot[]>([])
  let hot = $state<string | null>(null)
  function measure() {
    const desk = document.querySelector('.desk'), home = desk?.querySelector(':scope > aside.home'), handle = desk?.querySelector(':scope > .handle')
    if (!home || !handle) { spots = []; return }
    const hr = home.getBoundingClientRect(), gr = handle.getBoundingClientRect()
    const x = gr.left + gr.width / 2
    const out: Spot[] = []
    for (const el of home.querySelectorAll<HTMLElement>('[data-sess]')) {
      const r = el.getBoundingClientRect()
      const y = r.top + Math.min(r.height / 2, 44)
      if (y > hr.top + 20 && y < hr.bottom - 20) out.push({ name: el.dataset.sess!, y, x })
    }
    if (JSON.stringify(out) !== JSON.stringify(spots)) spots = out
  }
  onMount(() => {
    measure()
    const on = () => requestAnimationFrame(measure)
    window.addEventListener('scroll', on, true); window.addEventListener('resize', on)
    const iv = setInterval(measure, 400)
    return () => { window.removeEventListener('scroll', on, true); window.removeEventListener('resize', on); clearInterval(iv) }
  })
</script>

{#each spots as s (s.name)}
  {#if shown.includes(s.name)}
    <i class="link" style:top="{s.y}px" style:left="{s.x - 14}px" aria-hidden="true"></i>
  {:else}
    <div class="pill" class:hot={hot === s.name} class:flip={homeRight} style:top="{s.y}px" style:left="{s.x}px" role="group" aria-label={s.name}
      onpointerenter={() => (hot = s.name)} onpointerleave={() => (hot = null)}>
      <button aria-label={t.swapAdd(s.name)} title={t.swapAdd(s.name)} onclick={() => onAdd(s.name)}>+</button>
      <button aria-label={current ? t.swapReplace(current) : t.swapAdd(s.name)} title={current ? t.swapReplace(current) : ''} onclick={() => onSwap(s.name)}>{homeRight ? '‹' : '›'}</button>
      {#if hot === s.name}
        <span class="tip tip1">{t.swapAddShort}</span>
        {#if current}<span class="tip tip2">{t.swapReplace(current)}</span>{/if}
      {/if}
    </div>
  {/if}
{/each}

<style>
  .pill { position: fixed; z-index: 30; transform: translate(-50%, -50%); width: 30px; display: flex; flex-direction: column; border-radius: 16px; background: var(--s1); border: 1px solid var(--line); opacity: .55; transition: opacity .15s, background-color .15s; }
  .pill.hot { opacity: 1; background: var(--s2); }
  .pill button { height: 30px; display: grid; place-items: center; font: 500 20px/1 var(--sans, inherit); color: var(--text); }
  .pill button + button { border-top: 1px solid var(--line); }
  .pill button:hover { color: var(--accent); }
  .tip { position: absolute; left: calc(100% + 8px); white-space: nowrap; padding: 4px 10px; border-radius: 10px; background: var(--bg); border: 1px solid var(--line); font-size: 13px; color: var(--text); pointer-events: none; }
  .pill.flip .tip { left: auto; right: calc(100% + 8px); }
  .tip1 { top: 2px; } .tip2 { top: 32px; }
  .link { position: fixed; z-index: 30; width: 28px; height: 2px; margin-top: -1px; background: var(--text); border-radius: 1px; opacity: .8; pointer-events: none; }
  @media (prefers-reduced-motion: reduce) { .pill { transition: none; } }
</style>
