<script lang="ts">
  import { molla, motore, ridotto } from './molla'
  import { t } from './t'
  // Voce 6 del terzo giro, fedele all'originale (File drop; app-animazioni-terzo-c-26-11): il velo sopra la chat quando trascini dei
  // file. Entra con la zona che si accende (molla 300 26) e cresce del 2,5 %, il cerchio sale di 5 px e la freccia ondeggia (fino a
  // 3,5 px, 7 rad/s) finché sei sopra. Dice quanti file e per chi. Al rilascio i file volano nel campo (`volaFile` in molla.ts).
  // Moto ridotto: il velo compare in dissolvenza, senza oscillare.
  let { n, to }: { n: number; to: string } = $props()
  let zone = $state<HTMLElement>()
  const ZH = molla(0, 300, 26)
  const ZS = molla(1, 380, 16)
  let clock = 0
  let last = 0
  function render() {
    if (!zone) return
    const now = performance.now()
    clock += Math.min((now - (last || now)) / 1000, 1 / 30)
    last = now
    const h = ZH.x
    zone.style.setProperty('--h', h.toFixed(3))
    zone.style.transform = `scale(${(ZS.x * (1 + 0.025 * h)).toFixed(4)})`
    const core = zone.querySelector<HTMLElement>('.core')
    if (core) core.style.transform = `translateY(${(-5 * h).toFixed(2)}px)`
    const up = zone.querySelector<SVGElement>('.up')
    if (up) up.style.transform = `translateY(${(-3.5 * h * (0.5 + 0.5 * Math.sin(clock * 7))).toFixed(2)}px)`
  }
  // L'onda della freccia non si ferma mai da sola: il motore gira finché il velo c'è.
  const bob = molla(0, 1, 1)
  bob.t = 1e9
  const m = motore([ZH, ZS, bob], render)
  $effect(() => {
    if (ridotto()) { ZH.x = ZH.t = 1; queueMicrotask(render); return }
    ZH.t = 1
    queueMicrotask(() => { render(); m.vai() })
    return () => m.stop()
  })
</script>

<div class="dropzone" bind:this={zone} class:still={ridotto()} role="status">
  <span class="core" aria-hidden="true">
    <svg class="up" viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 19V5M6 11l6-6 6 6" /></svg>
  </span>
  <b>{t.dropHere}</b>
  <span class="sub">{t.dropFor(n, to)}</span>
</div>

<style>
  .dropzone { --h: 0; position: absolute; inset: 8px; z-index: 20; display: grid; place-content: center; justify-items: center; gap: 6px; border: 2px dashed color-mix(in srgb, var(--icon) 60%, transparent); border-radius: 24px; background: rgb(0 0 0 / .6); color: var(--icon); pointer-events: none; }
  .dropzone::after { content: ''; position: absolute; inset: 0; border-radius: inherit; background: rgb(255 255 255 / .09); opacity: var(--h); }
  .core { display: grid; place-items: center; width: 72px; height: 72px; border-radius: 50%; background: rgb(255 255 255 / .09); margin-bottom: 6px; }
  b { font-weight: 500; }
  .sub { font-size: 13.5px; color: var(--tx2); }
  .still { animation: dz-in 150ms ease both; }
  @keyframes dz-in { from { opacity: 0; } }
</style>
