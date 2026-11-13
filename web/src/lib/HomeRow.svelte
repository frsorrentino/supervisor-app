<script lang="ts">
  import { age } from './summary'
  import type { Voice } from './homeRow'
  import { t } from './t'
  // La Home in riga (app-home-riga-c1-13-11, variante C1): in basso, mentre la master occupa la colonna. Solo le sessioni che ti
  // aspettano fuori schermo e le novità, una voce per cosa: il tocco su una voce apre la sua sessione; il resto della riga, la
  // maniglia o la riga tirata su riaprono la Home. Vuota resta la maniglia, alta 28 px.
  // `tight` (telefono): chi ti aspetta col nome e da quanto, le novità contate in una voce sola.
  let { voices, now, tight = false, onOpen, onHome }: { voices: Voice[]; now: number; tight?: boolean; onOpen: (v: Voice) => void; onHome: () => void } = $props()
  const waits = $derived(voices.filter(v => v.kind === 'wait'))
  const news = $derived(voices.filter(v => v.kind === 'news'))
  // La riga tirata su: oltre 24 px verso l'alto riapre la Home.
  let from: number | null = null
  const down = (e: PointerEvent) => { from = e.clientY }
  const move = (e: PointerEvent) => { if (from != null && from - e.clientY > 24) { from = null; onHome() } }
  const up = () => { from = null }
</script>

<!-- svelte-ignore a11y_click_events_have_key_events, a11y_no_static_element_interactions -->
<div class="hrow" class:empty={!voices.length} class:tight data-testid="home-riga" onclick={onHome} onpointerdown={down} onpointermove={move} onpointerup={up} onpointercancel={up}>
  <button class="hand" aria-label={t.homeRowOpen} title={t.homeRowOpen}><i></i></button>
  {#if voices.length}
    <span class="hn" aria-hidden="true"><svg viewBox="0 0 24 24" width="22" height="22"><path d="M4 11l8-7 8 7v8a1 1 0 0 1-1 1h-4v-6h-6v6H5a1 1 0 0 1-1-1z" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round" /></svg></span>
    <div class="vs">
      {#each waits as v (v.key)}
        <button class="voce" data-testid="home-riga-voce" title={v.text ?? v.session} onclick={(e) => { e.stopPropagation(); onOpen(v) }}>
          <i></i><b>{v.session}</b>{#if v.text && !tight}<span class="w">{v.text}</span>{/if}{#if v.at != null}<span class="a">{age(v.at, now)}</span>{/if}
        </button>
      {/each}
      {#if tight && news.length}
        <button class="voce new" data-testid="home-riga-novita" onclick={(e) => { e.stopPropagation(); onOpen(news[0]) }}>
          <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="2" /><path d="M8 12.5l2.8 2.8L16 9.5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
          <b>{news.length}</b><span class="tagn">{t.homeRowNew}</span>
        </button>
      {:else}
        {#each news as v (v.key)}
          <button class="voce new" data-testid="home-riga-novita" title={v.text ?? v.session} onclick={(e) => { e.stopPropagation(); onOpen(v) }}>
            <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="2" /><path d="M8 12.5l2.8 2.8L16 9.5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
            <b>{v.session}</b>{#if v.text}<span class="w">{v.text}</span>{/if}<span class="tagn">{t.homeRowNew}</span>
          </button>
        {/each}
      {/if}
    </div>
    {#if !tight}<span class="up" aria-hidden="true"><svg viewBox="0 0 24 24" width="22" height="22"><path d="M7 14l5-5 5 5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></span>{/if}
  {/if}
</div>

<style>
  .hrow { position: relative; flex: none; height: 64px; display: flex; align-items: center; gap: 10px; padding: 6px 14px 0; border-top: 1px solid var(--line); background: var(--bg); cursor: pointer; touch-action: none; container-type: inline-size; }
  .hand { position: absolute; top: 0; left: 50%; width: 88px; height: 14px; margin-left: -44px; display: grid; place-items: center; }
  .hand i { width: 44px; height: 4px; border-radius: 2px; background: var(--tx3); }
  .hand:focus-visible { outline: 2px solid var(--tx); outline-offset: 2px; border-radius: 6px; }
  .hn { display: flex; align-items: center; color: var(--tx2); flex: none; }
  /* Le voci su una riga: quelle che non ci stanno escono a destra, senza puntini. */
  .vs { flex: 1; min-width: 0; display: flex; align-items: center; gap: 10px; overflow: hidden; }
  .up { width: 40px; height: 40px; border-radius: 50%; background: var(--s3); display: grid; place-items: center; flex: none; color: var(--tx); }
  .voce { flex: 0 1 auto; display: flex; align-items: center; gap: 8px; min-width: 0; padding: 7px 12px 7px 10px; border-radius: 999px; border: 1px solid var(--line); background: var(--s1); font-size: 14px; color: var(--tx2); white-space: nowrap; }
  .voce:hover { border-color: var(--tx3); }
  .voce i { width: 9px; height: 9px; border-radius: 50%; background: var(--wait); flex: none; }
  .voce b { flex: none; font: 600 15.5px/20px var(--display); color: var(--tx); }
  .voce .w { min-width: 0; overflow: hidden; }
  .voce .a { flex: none; color: var(--wait); font-variant-numeric: tabular-nums; }
  .voce.new { border-color: color-mix(in srgb, var(--good) 45%, var(--line)); }
  .voce.new svg { flex: none; color: var(--good); }
  .voce.new .tagn { flex: none; font: 500 10.5px/16px var(--mono); letter-spacing: .12em; text-transform: uppercase; color: var(--good); }
  /* Vuota: solo la maniglia. */
  .hrow.empty { height: 28px; padding: 0; }
  .hrow.empty .hand { top: 0; height: 28px; }
  .hrow.tight { gap: 8px; padding: 6px 10px 0; }
  /* In una riga stretta (tablet) restano il nome e da quanto. */
  @container (max-width: 900px) { .voce .w { display: none; } }
</style>
