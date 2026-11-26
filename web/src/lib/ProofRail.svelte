<script lang="ts">
  import type { ProofSteps } from './contract'
  import { clamp, molla, motore, type Molla } from './molla'
  import { fill, line, look } from './passiProva'
  import { t } from './t'
  // Voce 5, fedele agli originali (designbookmark.com/motion Step progress e Kokonut Agent steps; app-animazioni-fedeli-26-11), come
  // ProofRail sul telefono. Il filo va alla tappa raggiunta con la molla 160 22; una tappa passata si riempie di verde (260 24) e la
  // spunta si disegna 120 ms dopo (140 20); la tappa che diventa quella in corso fa un pop da 0,7 (500 14, spinta 6). La tappa in corso
  // ha la rotella dell'Agent steps (un giro in 0,8 s) e il nome con l'onda (opacità fino a 0,3 in 1,6 s, 50 ms per lettera); una
  // fallita è rossa. Sotto, a parole, dove si è. Moto ridotto: niente movimento.
  // app-passi-centrati-26-11 (Franz 26/11 15:02: «troppo a destra, serve ben centrato»): `bleed` px a sinistra fuori dalla colonna del
  // testo (la colonna del glifo), così il filo è centrato nella riga intera e non sotto il nome.
  let { steps, bleed = 0 }: { steps: ProofSteps; bleed?: number } = $props()
  const items = $derived(steps.items)
  const l = $derived(line(steps))
  const say = $derived(!l ? '' : l.say === 'running' ? t.proofRunning(l.label) : l.say === 'failed' ? t.proofFailed(l.label) : l.say === 'todo' ? t.proofTodo(l.label) : t.proofAllDone)

  const L = molla(0, 160, 22)
  let fs: Molla[] = []
  let cs: Molla[] = []
  let ps: Molla[] = []
  const all: Molla[] = [L]
  let prev: string[] | null = null
  let root = $state<HTMLElement>()
  const timers: ReturnType<typeof setTimeout>[] = []

  function render() {
    if (!root) return
    const bar = root.querySelector<HTMLElement>('.rail i')
    if (bar) bar.style.width = (clamp(L.x, 0, 1) * 100).toFixed(2) + '%'
    root.querySelectorAll<HTMLElement>('.dot').forEach((d, k) => {
      const f = clamp(fs[k]?.x ?? 0, 0, 1)
      if (!d.closest('.failed')) {
        d.style.background = f > 0.01 ? `color-mix(in srgb, var(--good) ${(f * 100).toFixed(1)}%, var(--s1))` : ''
        d.style.borderColor = f > 0.01 ? `color-mix(in srgb, var(--good) ${(f * 100).toFixed(1)}%, transparent)` : ''
      }
      d.style.transform = `scale(${(ps[k]?.x ?? 1).toFixed(4)})`
      const c = d.querySelector<SVGPathElement>('.ck path')
      if (c) c.style.strokeDashoffset = (22 * (1 - clamp(cs[k]?.x ?? 0, 0, 1))).toFixed(2)
    })
  }
  const m = motore(all, render)

  $effect(() => {
    const looks = items.map(look)
    const first = prev == null || prev.length !== looks.length
    if (first) {
      fs = looks.map(x => molla(x === 'done' ? 1 : 0, 260, 24))
      cs = looks.map(x => molla(x === 'done' ? 1 : 0, 140, 20))
      ps = looks.map(() => molla(1, 500, 14))
      all.length = 1
      all.push(...fs, ...cs, ...ps)
      L.x = L.t = fill(items)
    } else {
      L.t = fill(items)
      looks.forEach((x, k) => {
        const done = x === 'done'
        fs[k].t = done ? 1 : 0
        if (done && cs[k].t < 1) timers.push(setTimeout(() => { cs[k].t = 1; m.vai() }, 120))
        if (!done) { cs[k].x = cs[k].t = 0; cs[k].v = 0 }
        if (x === 'now' && prev?.[k] !== 'now') { ps[k].x = 0.7; ps[k].v = 6; ps[k].t = 1 }
      })
    }
    prev = looks
    queueMicrotask(() => { render(); m.vai() })
  })
  $effect(() => () => { timers.forEach(clearTimeout); m.stop() })
</script>

{#if items.length}
  <div class="pr" bind:this={root} data-testid="passi-prova" style:--n={items.length} style:margin-left="{-bleed}px">
    <div class="steps">
      <div class="rail" aria-hidden="true"><i></i></div>
      {#each items as s (s.key)}
        <div class="st {look(s)}">
          <span class="dot" aria-hidden="true">
            {#if look(s) === 'failed'}<svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="#0B0B0B" stroke-width="3.2" stroke-linecap="round"><path d="M6 6l12 12M18 6 6 18" /></svg>
            {:else}<svg class="ck" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#0B0B0B" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7.5" /></svg>{/if}
          </span>
          <span class="lb">{#if look(s) === 'now'}{#each [...s.label] as ch, i (i)}<span class="wv" style:animation-delay="{i * 50}ms">{ch}</span>{/each}{:else}{s.label}{/if}</span>
        </div>
      {/each}
    </div>
    {#if say}<p class="say mono">{say}</p>{/if}
  </div>
{/if}

<style>
  .pr { margin-top: 8px; }
  .steps { position: relative; display: grid; grid-template-columns: repeat(var(--n), 1fr); }
  .rail { position: absolute; left: calc(50% / var(--n)); right: calc(50% / var(--n)); top: 10px; height: 2px; background: var(--s3); border-radius: 1px; }
  .rail i { position: absolute; left: 0; top: 0; bottom: 0; width: 0; background: var(--good); border-radius: 1px; }
  .st { position: relative; display: grid; justify-items: center; gap: 4px; font-size: 13px; color: var(--tx2); }
  .dot { width: 22px; height: 22px; border-radius: 50%; display: grid; place-items: center; background: var(--s1); border: 2px solid var(--tx3); box-sizing: border-box; will-change: transform; }
  .ck path { stroke-dasharray: 22; stroke-dashoffset: 22; }
  .done { color: var(--tx); }
  .failed .dot { background: var(--red); border-color: var(--red); }
  .failed { color: var(--red); }
  .now { color: var(--busy); }
  .now .dot { position: relative; border-color: color-mix(in srgb, var(--busy) 35%, transparent); }
  .now .dot::after { content: ''; position: absolute; inset: -2px; border-radius: 50%; border: 2px solid transparent; border-top-color: var(--busy); animation: pr-spin 0.8s linear infinite; }
  @keyframes pr-spin { to { transform: rotate(360deg); } }
  .wv { animation: pr-wave 1.6s ease-in-out infinite both; white-space: pre; }
  @keyframes pr-wave { 50% { opacity: 0.3; } }
  .lb { white-space: nowrap; }
  .say { margin: 4px 0 0; text-align: center; }
  @media (prefers-reduced-motion: reduce) { .now .dot::after { animation: none; border-color: var(--busy); } .wv { animation: none; } }
</style>
