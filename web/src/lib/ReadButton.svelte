<script lang="ts">
  import { speech, toggle } from './speech.svelte'
  import { t } from './t'
  // app-play-lettura-ovunque-27-11 (Franz 27/11 13:29): il ▶ della lettura, uno solo in tutta l'app, quello della chat: tondo da 36 px,
  // la stessa icona, «Leggi ad alta voce», ■ mentre legge. Una lettura alla volta: toggle ferma quella di prima. La pillola con velocità e
  // barra dei paragrafi è quella di sempre, in fondo alla pagina.
  let { text, source = null, testid = 'leggi' }: { text: string; source?: string | null; testid?: string } = $props()
  const reading = $derived(speech.text === text && !speech.paused)
</script>
<button class="play" data-testid={testid} aria-label={reading ? t.stopReading : t.readAloud} title={reading ? t.stopReading : t.readAloud} onclick={(e) => { e.stopPropagation(); toggle(text, source) }}>
  {#if reading}<svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><rect x="7" y="7" width="10" height="10" rx="1.5" fill="currentColor" /></svg>
  {:else}<svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path d="M8 5.5v13l10.5-6.5z" fill="currentColor" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" /></svg>{/if}
</button>
<style>
  .play { flex: none; width: 36px; height: 36px; padding: 0; border: 0; border-radius: 50%; display: grid; place-items: center; background: var(--high); color: var(--icon); cursor: pointer; }
  @media (hover: hover) and (pointer: fine) { .play:hover { filter: brightness(1.15); } }
</style>
