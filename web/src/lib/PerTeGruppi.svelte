<script lang="ts">
  import type { CmdResult, WaitItem } from './contract'
  import type { newCmd } from './transport'
  import { splitBlocking, type Gruppo, type Voce as Item } from './perTe'
  import { perTeOpen, togglePerTe } from './perTeOpen.svelte'
  import { fold as foldIn } from './motion'
  import { actVoce, adviceText, agoOf, mainLabelOf, silenceOf, subOf } from './voceRow'
  import { age } from './tiAspetta'
  import { t } from './t'
  import Voce from './Voce.svelte'
  // Dalla master per sessione (app-riall-2-viste-16-11, direzione A scelta da Franz il 16/11; mockup a-master e a-largo in
  // docs/mockup/2026-11-16-chiarezza): le stesse voci e gli stessi tasti di «Per te» nella sessione, un gruppo per sessione. Il titolo
  // del gruppo porta alla sessione; sullo schermo largo il gruppo della sessione aperta accanto è acceso («questa»). In fondo le voci
  // senza sessione. Prende il posto di Decidi e Verifica: il numero della sezione sono queste righe.
  // app-per-te-ordine-23-11 (Franz 23/11 19:00 e 19:01; contratto 1.160): in alto, aperte, le carte che bloccano un compito in corso,
  // ognuna con la sua sessione; sotto i gruppi per sessione col resto, chiusi di base, col nome, quante voci e da quanto, e la stessa
  // freccia delle altre sezioni; «apri» porta alla sessione. Il rimando da una sessione apre il suo gruppo. Una risposta toglie la voce
  // subito qui (attese.svelte.ts) e sugli altri dispositivi al primo stato.
  let { run, groups, now, current = [], focus = null, onSession, onOpenWait, onOpenResult, reread, onAnswered = () => {} }: {
    run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>; groups: Gruppo[]; now: number
    /** Le sessioni aperte nelle colonne accanto. */
    current?: string[]
    /** Il rimando «anche in Dalla master» da una sessione: il suo gruppo si vede e si illumina una volta. */
    focus?: { session: string; n: number; at: number } | null
    onSession: (name: string) => void; onOpenWait: (id: string) => void; onOpenResult: (id: string, session: string | null) => void
    reread: () => void; onAnswered?: (w: WaitItem) => void
  } = $props()

  const split = $derived(splitBlocking(groups))
  const keyOf = (g: Gruppo) => g.session ?? ''
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
    perTeOpen[f.session] = true
    lit = f.session
    setTimeout(() => { if (lit === f.session) lit = null }, 1600)
  })
</script>

{#snippet row(v: Item, session: string | null, withSession: boolean)}
  <Voce group={v.group} title={v.title} sub={withSession && session ? `${session} · ${subOf(v)}` : subOf(v)} ago={agoOf(v, now)} silence={silenceOf(v, now)} advice={adviceText(v.advice)} main={mainLabelOf(v)} rec={!!v.main?.recommended}
    testid="voce-{v.key}" onOpen={() => open(v, session)} onMain={() => void actVoce(run, v, { open: (x) => open(x, session), reread, onAnswered })} />
{/snippet}

{#if groups.length}
  <div class="pg" bind:this={box} data-testid="per-te-gruppi">
    {#if split.top.length}
      <div class="grp top" data-testid="per-te-bloccanti">
        <div class="gh loose"><span class="dot wait"></span><b>{t.perTeBlocking}</b><span class="st">{t.perTeGroupN(split.top.length)}</span></div>
        {#each split.top as v (v.key)}{@render row(v, v.session, true)}{/each}
      </div>
    {/if}
    {#each split.groups as g (keyOf(g))}
      {@const here = !!g.session && current.includes(g.session)}
      {@const on = !!perTeOpen[keyOf(g)]}
      <div class="grp" class:here class:lit={!!g.session && lit === g.session} data-group={keyOf(g)}>
        <div class="gh">
          <button class="tg" aria-expanded={on} data-testid="gruppo-{g.session ?? 'senza-sessione'}" onclick={() => togglePerTe(keyOf(g))}>
            <span class="dot" class:wait={waiting(g)}></span><b>{g.session ?? t.perTeNoSession}</b>
            <span class="st">{t.perTeGroupN(g.voci.length)} · {here ? t.perTeGroupThis : age(now - oldest(g))}</span>
            <svg class="turn" class:up={on} viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"><path d="M6 9l6 6 6-6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
          </button>
          {#if g.session}{@const name = g.session}
            <button class="go" data-testid="gruppo-apri-{name}" onclick={() => onSession(name)}>{t.perTeGroupOpen}<svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true"><path d="M9 6l6 6-6 6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></button>
          {/if}
        </div>
        {#if on}<div class="fb" in:foldIn out:foldIn={{ ms: 150 }}><div class="foldin">
          {#each g.voci as v (v.key)}{@render row(v, g.session, false)}{/each}
        </div></div>{/if}
      </div>
    {/each}
  </div>
{/if}

<style>
  .pg { border-radius: 16px; background: var(--s1); border: 1px solid var(--line); overflow: hidden; }
  .grp + .grp { border-top: 1px solid var(--line); }
  .gh { display: flex; align-items: center; gap: 4px; width: 100%; min-height: 48px; padding: 0 8px 0 16px; background: var(--s2); color: var(--tx); text-align: left; }
  .gh .tg { flex: 1; min-width: 0; display: flex; align-items: center; gap: 10px; min-height: 48px; color: var(--tx); text-align: left; cursor: pointer; }
  .gh.loose { padding-right: 16px; gap: 10px; }
  .gh .go { flex: none; min-height: 40px; padding: 0 4px 0 8px; }
  .turn { flex: none; color: var(--tx2); transition: transform 200ms cubic-bezier(0.23, 1, 0.32, 1); }
  .turn.up { transform: rotate(180deg); }
  .fb { display: grid; grid-template-columns: minmax(0, 1fr); grid-template-rows: 1fr; }
  .foldin { min-height: 0; display: flex; flex-direction: column; }
  /* Le carte che bloccano un lavoro: il velo ambra di «ti aspetta» sulla testata. */
  .top .gh { background: color-mix(in srgb, var(--wait) 10%, var(--s2)); }
  .top .gh .st { flex: 1; }
  .gh b { font: 600 16px var(--display); white-space: nowrap; overflow: hidden; text-overflow: clip; min-width: 0; }
  .gh .st { flex: 1; min-width: 0; font-size: 13px; color: var(--tx2); white-space: nowrap; overflow: hidden; text-overflow: clip; }
  .gh .go { display: inline-flex; align-items: center; gap: 2px; font-size: 13px; color: var(--tx2); white-space: nowrap; }
  .gh.loose { cursor: default; }
  @media (hover: hover) and (pointer: fine) { .gh .tg:hover b, .gh .go:hover { color: var(--tx); text-decoration: underline; } }
  .dot { flex: none; width: 10px; height: 10px; border-radius: 50%; background: var(--tx3); }
  .dot.wait { background: var(--wait); }
  .here .gh { background: color-mix(in srgb, var(--wait) 12%, var(--s2)); }
  .here .gh .st { color: var(--wait); }
  /* Il gruppo raggiunto dal rimando si illumina un momento (app-riall-2-viste-16-11). */
  .gh { transition: background-color 300ms ease-out; }
  .grp.lit .gh { background: color-mix(in srgb, var(--wait) 26%, var(--s2)); }
  @media (prefers-reduced-motion: reduce) { .gh, .turn { transition: none; } }
</style>
