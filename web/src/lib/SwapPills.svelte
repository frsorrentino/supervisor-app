<script lang="ts">
  import { onMount } from 'svelte'
  import { t } from './t'
  // Il segno nel bordo fra la home e le colonne, accanto a ogni sessione della home (Franz 03/11 18:22 e 04/11,
  // app-sessione-sostituisci-03-11, app-scheda-sostituisce-04-11): chiusa, + la apre accanto; già in colonna, il filo bianco
  // che la lega al riquadro è il suo meno, e al passaggio diventa «− Chiudi». Sostituire è il clic sulla scheda.
  let { shown, homeRight, onAdd, onClose }: { shown: string[]; homeRight: boolean; onAdd: (n: string) => void; onClose: (n: string) => void } = $props()
  type Spot = { name: string; y: number; x: number }
  let spots = $state<Spot[]>([])
  let hot = $state<string | null>(null)
  function measure() {
    // Con la master sotto la home (app-bordo-aggiungi-sparito-04-11) la home sta nella colonna della master; i segni stanno
    // sempre a metà della corsia, che nella pila resta fra le sessioni e la colonna (app-tablet-barre-bordi-05-11: sul bordo
    // della colonna coprivano le schede).
    const stack = document.querySelector('aside.stackcol')
    const desk = document.querySelector('.desk')
    const home = stack ? stack.querySelector('.hs-top') : desk?.querySelector(':scope > aside.home')
    const handle = desk?.querySelector(':scope > .handle')
    if (!home || !handle) { spots = []; return }
    const hr = home.getBoundingClientRect()
    const gr = handle.getBoundingClientRect()
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
    <button class="link" class:hot={hot === s.name} class:flip={homeRight} style:top="{s.y}px" style:left="{s.x}px" aria-label={t.swapClose(s.name)}
      onpointerenter={() => (hot = s.name)} onpointerleave={() => (hot = null)} onclick={() => onClose(s.name)}><i></i>{#if hot === s.name}<span class="tip">{t.swapCloseShort}</span>{/if}</button>
  {:else}
    <div class="pill" class:hot={hot === s.name} class:flip={homeRight} style:top="{s.y}px" style:left="{s.x}px" role="group" aria-label={s.name}
      onpointerenter={() => (hot = s.name)} onpointerleave={() => (hot = null)}>
      <button aria-label={t.swapAdd(s.name)} title={t.swapAdd(s.name)} onclick={() => onAdd(s.name)}>+</button>
      {#if hot === s.name}
        <span class="tip tip1">{t.swapAddShort}</span>
      {/if}
    </div>
  {/if}
{/each}

<style>
  .pill { position: fixed; z-index: 30; transform: translate(-50%, -50%); width: 30px; display: flex; flex-direction: column; border-radius: 16px; background: var(--s1); border: 1px solid var(--line); opacity: .55; transition: opacity .15s, background-color .15s; }
  .pill.hot { opacity: 1; background: var(--s2); }
  .pill button { height: 30px; padding: 0; display: grid; place-items: center; font: 500 20px/1 var(--sans, inherit); color: var(--text); }
  .pill button:hover { color: var(--accent); }
  .tip { position: absolute; left: calc(100% + 8px); white-space: nowrap; padding: 4px 10px; border-radius: 10px; background: var(--bg); border: 1px solid var(--line); font-size: 12.5px; color: var(--text); pointer-events: none; }
  .pill.flip .tip { left: auto; right: calc(100% + 8px); }
  .tip1 { top: 2px; }
  /* Il filo: dalla colonna alla scheda (13 + 28 + 13) a riposo, il tocco prende 30 x 30; al passaggio diventa la pillola del
     meno, come quella del +. */
  .link { position: fixed; z-index: 30; transform: translate(-50%, -50%); width: 30px; height: 30px; padding: 0; display: grid; place-items: center; border-radius: 16px; border: 1px solid transparent; transition: background-color .15s, border-color .15s; }
  .link i { width: 54px; flex: none; height: 2px; border-radius: 1px; background: var(--text); opacity: .8; transition: width .15s; }
  .link.hot { background: var(--s2); border-color: var(--line); }
  .link.hot i { width: 12px; opacity: 1; }
  .link .tip { top: 4px; }
  .link.flip .tip { left: auto; right: calc(100% + 8px); }
  @media (prefers-reduced-motion: reduce) { .pill, .link, .link i { transition: none; } }
</style>
