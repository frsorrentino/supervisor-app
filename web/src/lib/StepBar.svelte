<script lang="ts">
  import type { ProofSteps } from './contract'
  import { molla, motore, clamp, type Molla } from './molla'
  import { line, look } from './passiProva'
  import { t } from './t'
  // Le tappe della prova come barra a 4 segmenti col nome dentro (app-scheda-sessione-a-26-11, proposta A approvata da Franz): sotto il
  // compito che verificano; passata verde con ✓, fallita rossa con ✕, quella in corso blu, da fare grigia. Sotto, il motivo a parole.
  // Il moto fedele dei passi della prova (Step progress e Agent steps; app-animazioni-fedeli-26-11, opzione a della master del 26/11):
  // la tappa passata si riempie di verde da sinistra (molla 260 24) e la spunta si disegna 120 ms dopo (140 20); il segmento che diventa
  // quello in corso si accende (il filo dell'originale, 160 22) e il suo nome fa il pop da 0,7 (500 14, spinta 6); in corso la rotella
  // (un giro in 0,8 s) e l'onda sul nome (50 ms per lettera). Moto ridotto: al posto.
  // Voce 3 del terzo giro (passi della chiusura, opzione a): `names` false toglie i nomi dai segmenti, `caption` sostituisce la riga sotto.
  let { steps, names = true, caption = null }: { steps: ProofSteps; names?: boolean; caption?: string | null } = $props()
  const l = $derived(line(steps))
  const say = $derived(!l ? '' : l.say === 'running' ? t.proofRunning(l.label) : l.say === 'failed' ? t.proofFailed(l.label) : l.say === 'todo' ? t.proofTodo(l.label) : t.proofAllDone)

  type S = { f: Molla; c: Molla; l: Molla; p: Molla; look: string; timer?: ReturnType<typeof setTimeout> }
  const springs = new Map<string, S>()
  let root = $state<HTMLElement>()
  function render() {
    if (!root) return
    for (const el of root.querySelectorAll<HTMLElement>('.seg')) {
      const s = springs.get(el.dataset.k ?? '')
      if (!s) continue
      el.style.setProperty('--f', clamp(s.f.x, 0, 1).toFixed(4))
      el.style.setProperty('--l', (clamp(s.l.x, 0, 1) * 100).toFixed(1) + '%')
      el.style.setProperty('--c', clamp(s.c.x, 0, 1).toFixed(4))
      el.style.setProperty('--p', s.p.x.toFixed(4))
    }
  }
  let cur: { vai: () => void; stop: () => void } | null = null
  $effect(() => {
    const list = steps.items.map((s) => ({ k: s.key, look: look(s) }))
    for (const { k, look: lk } of list) {
      let s = springs.get(k)
      const done = lk === 'done' ? 1 : 0
      const lit = lk === 'now' ? 1 : 0
      if (!s) {
        s = { f: molla(done, 260, 24), c: molla(done, 140, 20), l: molla(lit, 160, 22), p: molla(1, 500, 14), look: lk }
        springs.set(k, s)
        continue
      }
      if (s.look === lk) continue
      const was = s.look
      s.look = lk
      s.f.t = done
      s.l.t = lit
      clearTimeout(s.timer)
      if (done) { const c = s.c; s.timer = setTimeout(() => { c.t = 1; cur?.vai() }, 120) } else { s.c.x = s.c.t = 0; s.c.v = 0 }
      if (lk === 'now' && was !== 'now') { s.p.x = 0.7; s.p.v = 6 }
    }
    cur?.stop()
    const mm = motore([...springs.values()].flatMap((s) => [s.f, s.c, s.l, s.p]), render)
    cur = mm
    queueMicrotask(() => { render(); mm.vai() })
    return () => mm.stop()
  })
  $effect(() => () => springs.forEach((s) => clearTimeout(s.timer)))
</script>

{#if steps.items.length}
  <div class="sb" bind:this={root} data-testid="passi-prova" style:--n={steps.items.length}>
    {#each steps.items as s (s.key)}
      {@const lk = look(s)}
      <span class="seg {lk}" data-k={s.key}>
        {#if lk === 'failed'}<span class="mk">✕</span>
        {:else if lk === 'done'}<svg class="ck" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12.5l4.5 4.5L19 7.5" pathLength="1" /></svg>
        {:else if lk === 'now'}<svg class="sp" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3a9 9 0 0 1 9 9" /></svg>{/if}
        {#if names}<span class="lb">{#if lk === 'now'}{#each [...s.label] as ch, j}<span class="wv" style:animation-delay="{j * 50}ms">{ch}</span>{/each}{:else}{s.label}{/if}</span>{/if}
      </span>
    {/each}
  </div>
  {#if caption}<p class="why">{caption}</p>{:else if say}<p class="why" class:bad={l?.say === 'failed'}>{say}</p>{/if}
{/if}

<style>
  .sb { display: grid; grid-template-columns: repeat(var(--n), minmax(0, 1fr)); gap: 4px; margin-top: 8px; }
  .seg { --f: 0; --l: 0%; --c: 0; --p: 1; min-width: 0; min-height: 34px; box-sizing: border-box; display: flex; align-items: center; justify-content: center; gap: 4px; padding: 7px 4px; border-radius: 6px; font-size: 13px; line-height: 18px; white-space: nowrap; overflow: hidden; color: var(--tx2);
    background: linear-gradient(90deg, color-mix(in srgb, var(--good) 16%, var(--s1)) calc(var(--f) * 100%), var(--s2) calc(var(--f) * 100%));
    border: 1px solid color-mix(in srgb, color-mix(in srgb, var(--good) 45%, transparent) calc(var(--f) * 100%), color-mix(in srgb, var(--busy) var(--l), var(--line))); }
  .seg.done { color: var(--good); }
  .seg.failed { background: color-mix(in srgb, var(--red) 16%, var(--s1)); border-color: color-mix(in srgb, var(--red) 45%, transparent); color: var(--red); }
  .seg.now { color: var(--busy); }
  .lb { display: inline-block; min-width: 0; overflow: hidden; transform: scale(var(--p)); }
  .ck, .sp { flex: none; width: 12px; height: 12px; fill: none; stroke: currentColor; stroke-width: 2.4; stroke-linecap: round; stroke-linejoin: round; }
  .ck path { stroke-dasharray: 1; stroke-dashoffset: calc(1 - var(--c)); }
  .sp { animation: sb-turn 0.8s linear infinite; }
  @keyframes sb-turn { to { transform: rotate(360deg); } }
  .wv { animation: sb-wave 1.6s cubic-bezier(0.42, 0, 0.58, 1) infinite; }
  @keyframes sb-wave { 50% { opacity: 0.3; } }
  .why { margin: 6px 0 0; font-size: 13.5px; color: var(--tx2); }
  .why.bad { color: var(--red); }
  @media (prefers-reduced-motion: reduce) { .sp, .wv { animation: none; } }
</style>
