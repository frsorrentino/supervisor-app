<script lang="ts">
  import type { Group } from './perTe'
  import { MENU_ICON } from './sessionMenu'
  // Una voce del formato unico (app-formato-unico-12-11, variante A, tavola 2 del mockup): l'icona del tipo, il titolo, una riga di
  // contesto con da quanto aspetta, e a destra il tasto principale; il tocco sulla voce apre la sua scheda. Titolo e contesto stanno
  // ognuno su una riga, tagliati senza «…»: il testo intero è nella scheda. `hot`: la voce che aspetta una risposta, in ambra.
  // `tone`: l'esito di una prova già chiusa, al posto del tasto.
  let { group, title, sub, ago = null, main = null, tone = null, card = false, testid = undefined, onOpen, onMain = () => {} }: {
    group: Group; title: string; sub: string; ago?: string | null; main?: string | null; tone?: 'good' | 'red' | null
    /** Da sola in una chat, fuori da un blocco: col suo bordo. */
    card?: boolean; testid?: string; onOpen: () => void; onMain?: () => void
  } = $props()
  const hot = $derived(group === 'aspetta')
  const key = (e: KeyboardEvent) => { if (e.target === e.currentTarget && (e.key === 'Enter' || e.key === ' ')) { e.preventDefault(); onOpen() } }
</script>

<div class="voce" class:hot class:card role="button" tabindex="0" data-testid={testid} onclick={onOpen} onkeydown={key}>
  <span class="ic">
    <svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      {#if group === 'aspetta'}<path d="M18 11V6a2 2 0 0 0-4 0M14 10V4a2 2 0 0 0-4 0v2M10 10.5V6a2 2 0 0 0-4 0v8" /><path d="M18 8a2 2 0 1 1 4 0v6a8 8 0 0 1-8 8h-2c-2.8 0-4.5-.86-5.99-2.34l-3.6-3.6a2 2 0 0 1 2.83-2.82L7 15" />
      {:else if group === 'decidi'}<path d="m14 13-8.4 8.4a2 2 0 0 1-2.8-2.8L11.2 10.2M16 16l6-6M8 8l6-6M9 7l8 8M21 11l-8-8" />
      {:else if group === 'prova'}<path d={MENU_ICON.prove} />
      {:else}<path d="m3 6.5 1.8 1.8L8 5M3 16.5l1.8 1.8L8 15M12 7h9M12 17h9" />{/if}
    </svg>
  </span>
  <span class="tx"><span class="ti">{title}</span><span class="cx" class:good={tone === 'good'} class:red={tone === 'red'}>{sub}{#if ago}{sub ? ' · ' : ''}<span class="ago">{ago}</span>{/if}</span></span>
  {#if main}
    <button class="mini" onclick={(e) => { e.stopPropagation(); onMain() }}>{main}</button>
  {:else}
    <svg class="chev" viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"><path d="M9 6l6 6-6 6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
  {/if}
</div>

<style>
  .voce { display: grid; grid-template-columns: 32px minmax(0, 1fr) auto; column-gap: 10px; align-items: center; min-height: 52px; padding: 8px 12px; border-top: 1px solid var(--line); cursor: pointer; text-align: left; color: var(--tx); }
  .voce.card { margin-top: 8px; border: 1px solid var(--line); border-radius: 14px; background: var(--s1); }
  .voce.hot { background: color-mix(in srgb, var(--wait) 8%, transparent); }
  .ic { width: 32px; height: 32px; border-radius: 10px; display: grid; place-items: center; background: var(--s3); color: var(--tx2); }
  .hot .ic { background: color-mix(in srgb, var(--wait) 14%, var(--s1)); color: var(--wait); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--wait) 40%, transparent); }
  .tx { min-width: 0; display: flex; flex-direction: column; }
  .ti { font: 600 16.5px/21px var(--display); white-space: nowrap; overflow: hidden; text-overflow: clip; }
  .cx { font-size: 13px; line-height: 18px; color: var(--tx2); white-space: nowrap; overflow: hidden; text-overflow: clip; }
  .cx.good { color: var(--good); } .cx.red { color: var(--red); }
  .hot .ago { color: var(--wait); }
  .mini { font: 600 14.5px var(--display); min-height: 36px; padding: 6px 14px; border-radius: 999px; background: var(--s3); color: var(--tx); white-space: nowrap; }
  .hot .mini { background: color-mix(in srgb, var(--wait) 12%, var(--s1)); color: var(--wait); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--wait) 45%, transparent); }
  .mini:hover { filter: brightness(1.12); }
  .chev { color: var(--tx3); }
  .voce:focus-visible { outline: 2px solid var(--wait); outline-offset: -2px; }
</style>
