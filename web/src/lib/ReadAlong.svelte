<script lang="ts">
  import { untrack } from 'svelte'
  import { clamp, molla, motore, ridotto } from './molla'
  import { words } from './animazioni'
  // Voce 2 del primo giro, fedele all'originale (Read along), come ReadAlongText del telefono (app-web-parola-lettura-27-11, Franz 27/11
  // 11:50): nel paragrafo letto le parole già dette in chiaro e una pillola dietro quella che la voce sta dicendo. La pillola ha due bordi
  // con due molle, quello davanti 520 40 e quello dietro 210 26, così si allunga mentre va; l'altezza segue con la 600 48 e la comparsa con
  // la 300 30. A capo non scivola: si riapre dalla parola nuova. Margine di 0,16 em ai lati, alta 1,3 em, angoli di 0,34 em. Moto ridotto:
  // al posto.
  let { text, word }: { text: string; word: number } = $props()
  const ws = $derived(words(text))
  const k = $derived(ws.length ? Math.min(Math.max(0, word), ws.length - 1) : -1)
  // Il testo a pezzi: gli spazi restano testo, ogni parola (fine compresa, come in `words`) nel suo span.
  const parts = $derived.by(() => {
    const out: { w: number; s: string }[] = []
    let i = 0
    ws.forEach(([a, b], j) => { if (a > i) out.push({ w: -1, s: text.slice(i, a) }); out.push({ w: j, s: text.slice(a, b + 1) }); i = b + 1 })
    if (i < text.length) out.push({ w: -1, s: text.slice(i) })
    return out
  })
  let root = $state<HTMLElement>()
  let pill = $state<HTMLElement>()
  const L = molla(0, 210, 26), R = molla(0, 520, 40), Y = molla(0, 600, 48), O = molla(0, 300, 30)
  let lineTop = NaN
  function render() {
    if (!pill) return
    pill.style.transform = `translate(${L.x.toFixed(2)}px, ${Y.x.toFixed(2)}px)`
    pill.style.width = Math.max(0, R.x - L.x).toFixed(2) + 'px'
    pill.style.opacity = clamp(O.x, 0, 1).toFixed(3)
  }
  const m = motore([L, R, Y, O], render)
  function place(j: number) {
    if (!root || !pill) return
    const el = j < 0 ? null : root.querySelectorAll<HTMLElement>('.w')[j]
    if (!el) { O.t = 0; m.vai(); return }
    const em = parseFloat(getComputedStyle(root).fontSize) || 16
    const l = el.offsetLeft - 0.16 * em
    const r = el.offsetLeft + el.offsetWidth + 0.16 * em
    const top = el.offsetTop + (el.offsetHeight - 1.3 * em) / 2
    const forward = l >= L.t
    if (ridotto() || Number.isNaN(lineTop) || Math.abs(top - lineTop) > 2 || O.x < 0.05) { L.x = L.t = R.x = R.t = l; L.v = R.v = 0; Y.x = Y.t = top; Y.v = 0 }
    lineTop = top
    pill.style.height = (1.3 * em).toFixed(2) + 'px'
    pill.style.borderRadius = (0.34 * em).toFixed(2) + 'px'
    // Il bordo davanti va con la 520 40, quello dietro lo segue con la 210 26: la pillola si allunga mentre va.
    L.k = forward ? 210 : 520; L.c = forward ? 26 : 40
    R.k = forward ? 520 : 210; R.c = forward ? 40 : 26
    L.t = l; R.t = r; Y.t = top; O.t = 1
    m.vai()
  }
  $effect(() => { const j = k; void parts; untrack(() => queueMicrotask(() => place(j))) })
  $effect(() => () => m.stop())
</script>

<span class="ra" bind:this={root}><span class="pill" bind:this={pill} aria-hidden="true"></span>{#each parts as p, i (i)}{#if p.w < 0}{p.s}{:else}<span class="w" class:said={p.w <= k}>{p.s}</span>{/if}{/each}</span>

<style>
  .ra { position: relative; display: block; }
  .pill { position: absolute; left: 0; top: 0; width: 0; opacity: 0; background: color-mix(in srgb, var(--busy) 30%, transparent); pointer-events: none; will-change: transform, width; }
  .w { position: relative; color: var(--tx2); }
  .w.said { color: var(--tx); }
</style>
