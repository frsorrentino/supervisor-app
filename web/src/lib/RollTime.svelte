<script lang="ts">
  import { untrack } from 'svelte'
  import { ridotto } from './molla'
  // app-messaggio-master-effetto-27-11, opzione E (Franz 27/11 15:48, mockup 04-subito.html), come RollTime in Animazioni.kt: l'ora del
  // riquadro della master che scorre quando arriva una risposta, da 04:05 a 04:08. Scorre solo la cifra che cambia: la vecchia sale via e la
  // nuova entra da sotto con la molla di casa (620 ms). Senza `roll`, con le animazioni ridotte o con un'altra lunghezza, cambia e basta.
  let { value, roll = false }: { value: string; roll?: boolean } = $props()
  let shown = $state(untrack(() => value))
  let from = $state<string | null>(null)
  $effect(() => {
    const v = value
    untrack(() => {
      if (v === shown) return
      if (!roll || ridotto() || v.length !== shown.length) { shown = v; from = null; return }
      from = shown
      shown = v
    })
  })
  function scorre(node: HTMLElement) {
    const bounce = getComputedStyle(document.documentElement).getPropertyValue('--spring-bounce').trim() || 'ease-out'
    const a = node.animate([{ transform: 'translateY(0)' }, { transform: 'translateY(-50%)' }], { duration: 620, easing: bounce, fill: 'forwards' })
    a.onfinish = () => { from = null }
    return { destroy: () => a.cancel() }
  }
</script>
{#if from == null}<span class="rt">{shown}</span>
{:else}<span class="rt">{#each shown.split('') as ch, i (i)}{#if from[i] === ch}{ch}{:else}<span class="dg"><span class="col" use:scorre><i>{from[i]}</i><i>{ch}</i></span></span>{/if}{/each}</span>{/if}
<style>
  .rt { display: inline-flex; white-space: pre; }
  .dg { display: inline-block; height: 1lh; overflow: hidden; }
  .col { display: flex; flex-direction: column; }
  .col i { font-style: normal; height: 1lh; }
</style>
