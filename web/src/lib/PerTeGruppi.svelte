<script lang="ts">
  import type { CmdResult, WaitItem } from './contract'
  import type { newCmd } from './transport'
  import type { Gruppo, Voce as Item } from './perTe'
  import { actVoce, adviceText, agoOf, mainLabelOf, subOf } from './voceRow'
  import { age } from './tiAspetta'
  import { t } from './t'
  import Voce from './Voce.svelte'
  // Dalla master per sessione (app-riall-2-viste-16-11, direzione A scelta da Franz il 16/11; mockup a-master e a-largo in
  // docs/mockup/2026-11-16-chiarezza): le stesse voci e gli stessi tasti di «Per te» nella sessione, un gruppo per sessione. Il titolo
  // del gruppo porta alla sessione; sullo schermo largo il gruppo della sessione aperta accanto è acceso («questa»). In fondo le voci
  // senza sessione. Prende il posto di Decidi e Verifica: il numero della sezione sono queste righe.
  let { run, groups, now, current = [], focus = null, onSession, onOpenWait, onOpenResult, reread, onAnswered = () => {} }: {
    run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>; groups: Gruppo[]; now: number
    /** Le sessioni aperte nelle colonne accanto. */
    current?: string[]
    /** Il rimando «anche in Dalla master» da una sessione: il suo gruppo si vede e si illumina una volta. */
    focus?: { session: string; n: number; at: number } | null
    onSession: (name: string) => void; onOpenWait: (id: string) => void; onOpenResult: (id: string, session: string | null) => void
    reread: () => void; onAnswered?: (w: WaitItem) => void
  } = $props()

  const open = (v: Item, s: string | null) => (v.result ? onOpenResult(v.result.id, s) : onOpenWait(v.key))
  const waiting = (g: Gruppo) => g.voci.some(v => v.group !== 'verifica')
  const oldest = (g: Gruppo) => Math.min(...g.voci.map(v => v.since))

  let box = $state<HTMLElement | null>(null)
  let lit = $state<string | null>(null)
  // Il rimando si segue una volta, e solo da un contenitore che si vede (l'app può tenerne uno fuori dallo schermo).
  let handled = 0
  $effect(() => {
    const f = focus
    if (!f || !box || f.n <= handled || Date.now() - f.at > 1500 || !box.isConnected || !box.getClientRects().length) return
    handled = f.n
    const el = box.querySelector<HTMLElement>(`[data-group="${CSS.escape(f.session)}"]`)
    if (!el) return
    // La categoria che si apre e la striscia del telefono che torna alla home cambiano l'altezza per qualche istante: si ripete, e
    // `nearest` non muove niente quando il gruppo è già in vista.
    const go = () => { if (el.isConnected) el.scrollIntoView({ block: 'nearest', behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' }) }
    requestAnimationFrame(go); setTimeout(go, 400); setTimeout(go, 800)
    lit = f.session
    setTimeout(() => { if (lit === f.session) lit = null }, 1600)
  })
</script>

{#if groups.length}
  <div class="pg" bind:this={box} data-testid="per-te-gruppi">
    {#each groups as g (g.session ?? '')}
      {@const here = !!g.session && current.includes(g.session)}
      <div class="grp" class:here class:lit={!!g.session && lit === g.session} data-group={g.session ?? ''}>
        {#if g.session}
          {@const name = g.session}
          <button class="gh" data-testid="gruppo-{name}" onclick={() => onSession(name)}>
            <span class="dot" class:wait={waiting(g)}></span><b>{name}</b>
            <span class="st">{here ? t.perTeGroupThis : age(now - oldest(g))}</span>
            <span class="go">{t.perTeGroupOpen}<svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true"><path d="M9 6l6 6-6 6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></span>
          </button>
        {:else}
          <div class="gh loose" data-testid="gruppo-senza-sessione"><span class="dot"></span><b>{t.perTeNoSession}</b></div>
        {/if}
        {#each g.voci as v (v.key)}
          <Voce group={v.group} title={v.title} sub={subOf(v)} ago={agoOf(v, now)} advice={adviceText(v.advice)} main={mainLabelOf(v)} rec={!!v.main?.recommended}
            testid="voce-{v.key}" onOpen={() => open(v, g.session)} onMain={() => void actVoce(run, v, { open: (x) => open(x, g.session), reread, onAnswered })} />
        {/each}
      </div>
    {/each}
  </div>
{/if}

<style>
  .pg { border-radius: 16px; background: var(--s1); border: 1px solid var(--line); overflow: hidden; }
  .grp + .grp { border-top: 1px solid var(--line); }
  .gh { display: flex; align-items: center; gap: 10px; width: 100%; min-height: 44px; padding: 0 12px 0 16px; background: var(--s2); color: var(--tx); text-align: left; }
  .gh b { font: 600 16px var(--display); white-space: nowrap; overflow: hidden; text-overflow: clip; min-width: 0; }
  .gh .st { flex: 1; min-width: 0; font-size: 13px; color: var(--tx2); white-space: nowrap; overflow: hidden; text-overflow: clip; }
  .gh .go { display: inline-flex; align-items: center; gap: 2px; font-size: 13px; color: var(--tx2); white-space: nowrap; }
  .gh.loose { cursor: default; }
  button.gh:hover { background: var(--s3); }
  .dot { flex: none; width: 10px; height: 10px; border-radius: 50%; background: var(--tx3); }
  .dot.wait { background: var(--wait); }
  .here .gh { background: color-mix(in srgb, var(--wait) 12%, var(--s2)); }
  .here .gh .st { color: var(--wait); }
  /* Il gruppo raggiunto dal rimando si illumina un momento (app-riall-2-viste-16-11). */
  .gh { transition: background-color 300ms ease-out; }
  .grp.lit .gh { background: color-mix(in srgb, var(--wait) 26%, var(--s2)); }
  @media (prefers-reduced-motion: reduce) { .gh { transition: none; } }
</style>
