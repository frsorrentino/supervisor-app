<script lang="ts">
  // Copia fedele all'originale (designbookmark.com/motion, Copy Button; app-animazioni-fedeli-26-11): al tocco l'icona copia si
  // rimpicciolisce e gira di -40°, la spunta entra da 0,4 con un piccolo rimbalzo, la parola sale da «Copia» a «Copiato» e il tasto
  // si allarga alla parola nuova; dopo COPY_HOLD_MS torna indietro. Molle dell'originale: rullo 260 20, icone 420 18, larghezza 300 24.
  import Icon from './Icon.svelte'
  import { COPY_HOLD_MS } from './animazioni'
  import { clamp, molla, motore } from './molla'
  import { t } from './t'

  let { text, words = false }: { text: string | (() => string); words?: boolean } = $props()

  const R = molla(0, 260, 20)
  const I = molla(0, 420, 18)
  const RW = molla(0, 300, 24)
  let on = $state(false)
  let iCopy = $state<HTMLElement>()
  let iOk = $state<HTMLElement>()
  let roll = $state<HTMLElement>()
  let col = $state<HTMLElement>()
  let a = $state<HTMLElement>()
  let b = $state<HTMLElement>()

  function render() {
    const i = clamp(I.x, -0.2, 1.2)
    const ic = clamp(i, 0, 1)
    if (iCopy) {
      iCopy.style.opacity = clamp(1 - i * 1.6, 0, 1).toFixed(3)
      iCopy.style.transform = `scale(${(1 - 0.5 * ic).toFixed(3)}) rotate(${(-40 * ic).toFixed(1)}deg)`
    }
    if (iOk) {
      iOk.style.opacity = clamp(i * 1.6 - 0.4, 0, 1).toFixed(3)
      iOk.style.transform = `scale(${(0.4 + 0.6 * i).toFixed(3)})`
    }
    if (roll && a && b) roll.style.width = (a.offsetWidth + (b.offsetWidth - a.offsetWidth) * RW.x).toFixed(2) + 'px'
    if (col) col.style.transform = `translateY(${(-20 * R.x).toFixed(2)}px)`
  }
  const m = motore([R, I, RW], render)
  let timer: ReturnType<typeof setTimeout> | undefined

  function copy() {
    void navigator.clipboard?.writeText(typeof text === 'function' ? text() : text)
    on = true
    R.t = 1; I.t = 1; RW.t = 1
    clearTimeout(timer)
    timer = setTimeout(() => { on = false; R.t = 0; I.t = 0; RW.t = 0; m.vai() }, COPY_HOLD_MS)
    m.vai()
  }
  $effect(() => { render() })
  $effect(() => () => { clearTimeout(timer); m.stop() })
</script>

{#snippet icons(size: number)}
  <span class="ic" style:width="{size}px" style:height="{size}px">
    <span class="i" bind:this={iCopy}><Icon name="copy" {size} /></span>
    <span class="i ok" bind:this={iOk} style:opacity="0"><svg viewBox="0 0 24 24" width={size} height={size} fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5" /></svg></span>
  </span>
{/snippet}

{#if words}
  <button class="cp" aria-label={on ? t.copiedWord : t.copy} onclick={copy} data-testid="copia">
    {@render icons(16)}
    <span class="roll" bind:this={roll} aria-hidden="true"><span class="w" bind:this={col}><b bind:this={a}>{t.copy}</b><b bind:this={b} class="okw">{t.copiedWord}</b></span></span>
  </button>
{:else}
  <button class="sm" aria-label={on ? t.copiedWord : t.copy} title={t.copy} onclick={copy} data-testid="copia">{@render icons(20)}</button>
{/if}

<style>
  .ic { position: relative; flex: none; display: block; }
  .i { position: absolute; inset: 0; display: grid; place-items: center; will-change: transform, opacity; }
  .ok, .okw { color: var(--good); }
  .sm { width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center; color: var(--text2); }
  .cp { flex: none; align-self: flex-start; display: inline-flex; align-items: center; gap: 6px; font-family: var(--display); font-weight: 600; font-size: 14px; line-height: 20px; padding: 4px 12px 4px 10px; border-radius: 999px; border: 1px solid var(--line); color: var(--tx); overflow: hidden; }
  .roll { position: relative; display: block; height: 20px; overflow: hidden; }
  .w { display: flex; flex-direction: column; align-items: flex-start; will-change: transform; }
  .w b { display: block; height: 20px; line-height: 20px; font-weight: 600; white-space: nowrap; }
  @media (hover: hover) and (pointer: fine) {
    .sm:hover { background: var(--surface); }
    .cp:hover { background: var(--s3); }
  }
</style>
