<script lang="ts">
  import { untrack } from 'svelte'
  import ItemList from './ItemList.svelte'
  import { prima } from './itemRows'
  import type { CmdResult, PrimaRaccoltaPage, PrimaRaccoltaSummary } from './contract'
  import { decodePrima, readPage } from './decideDecode'
  import { screen } from './decide'
  import { askConsent, canStart, emptySection, finished, firstRoundOffer } from './primaRaccolta'
  import { newCmd } from './transport'
  import { t } from './t'
  import LongText from './LongText.svelte'
  import LoadFailed from './LoadFailed.svelte'
  // «Prima raccolta» (contratto 1.100), porta di PrimaRaccoltaScreen: cosa si è trovato per ogni sezione; una vuota dice perché e quando si riempie.
  let { run, summary, onScreen, onFeatures }: { run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>; summary: PrimaRaccoltaSummary | null | undefined; onScreen: (s: string) => void; onFeatures: () => void } = $props()
  let page = $state<PrimaRaccoltaPage | null>(null)
  // Senza risposta in tempo la pagina lo dice e offre Riprova, invece di restare «In arrivo dal PC» (app-pwa-decisioni-03-11).
  let failed = $state(false)
  async function load() { failed = false; ({ page, failed } = readPage(await run(newCmd('prima_raccolta', null)), decodePrima, page)) }
  // La pagina segue la raccolta: ogni sezione finita cambia il riassunto nello stato.
  $effect(() => { void summary?.done; void summary?.status; untrack(() => void load()) })
  // Franz 02/11 15:38 (app-feedback-azioni-02-11): il tasto toccato si accende subito e resta acceso finché la scheda cambia.
  let busy = $state<string | null>(null)
  async function touch(action: string, value?: string) { if (busy) return; busy = `${action}${value ?? ''}`; try { await run(newCmd('prima_raccolta_azione', null, null, undefined, undefined, { action, value })); await load() } finally { busy = null } }
  const head = (p: PrimaRaccoltaPage) => p.status === 'running' ? t.primaRunning(p.done, p.total) : p.status === 'ask' ? t.primaAsk(p.done, p.total) : p.status === 'interrupted' ? t.primaInterrupted(p.done, p.total) : p.status === 'done' ? t.primaDone : t.primaNone
</script>

<div class="pr" data-testid="prima-raccolta">
  {#if !page}{#if failed}<LoadFailed onRetry={() => void load()} />{:else}<p class="m">{t.proveLoading}</p>{/if}
  {:else}
    <p>{head(page)}</p>
    {#if page.cost === 0}<p class="m">{t.primaFree}</p>{/if}
    {#if canStart(page)}<button class="tonal wide" class:done={busy === 'start'} disabled={!!busy} onclick={() => touch('start')}>{#if busy === 'start'}✓ {/if}{page.status === 'interrupted' ? t.primaResume : t.primaStart}</button>{/if}
    {#if askConsent(page)}
      <div class="c"><LongText text={page.ask_text} /><div class="ac"><span class="sp"></span><button class="out" class:done={busy === 'consentno'} disabled={!!busy} onclick={() => touch('consent', 'no')}>{#if busy === 'consentno'}✓ {/if}{t.primaNo}</button><button class="tonal" class:done={busy === 'consentsi'} disabled={!!busy} onclick={() => touch('consent', 'si')}>{#if busy === 'consentsi'}✓ {/if}{t.primaYes}</button></div></div>
    {/if}
    <!-- Le sezioni come righe (app-richiede-te-righe-02-11): il tocco apre il testo intero e «Vai». -->
    <ItemList rows={page.sections.map(prima)} testid="prima" name={(r) => t.primaSections[r.key] ?? r.key}>{#snippet card(r)}{@const s = page?.sections.find(x => x.key === r.key)}{#if s}
      <div class="in" data-testid={`prima-${s.key}`}>
        {#if emptySection(s)}{#if s.why}<p class="m">{s.why}</p>{/if}{#if s.when}<p class="m">{s.when}</p>{/if}{:else}<LongText text={s.text} />{/if}
        {#if screen(s.dove)}<div class="ac"><span class="sp"></span><button class="text" onclick={() => onScreen(screen(s.dove)!)}>{t.featuresGo}</button></div>{/if}
      </div>{/if}{/snippet}</ItemList>
    {#if firstRoundOffer(page)}
      <div class="c"><p>{firstRoundOffer(page)}</p><div class="ac"><span class="sp"></span><button class="tonal" class:done={busy === 'first_round'} disabled={!!busy} onclick={() => touch('first_round')}>{#if busy === 'first_round'}✓ {/if}{t.primaFirstRound}</button></div></div>
    {/if}
    {#if finished(page)}<div class="ac"><span class="sp"></span><button class="text" onclick={onFeatures}>{t.primaFeatures}</button></div>{/if}
  {/if}
</div>

<style>
  .in { display: flex; flex-direction: column; gap: 6px; }
  /* La conferma (voce 11), come in DaProvare. */
  .tonal, .out { transition: background-color 150ms, color 150ms, transform var(--spring-bounce-t) var(--spring-bounce); }
  .tonal.done, .out.done { background: var(--good); color: var(--bg); border-color: transparent; transform: scale(1.06); }
  .pr { padding: 0 12px 24px; max-width: 720px; display: flex; flex-direction: column; gap: 10px; }
  .c { background: var(--low); border-radius: 14px; padding: 14px; display: flex; flex-direction: column; gap: 6px; }
  .c b { font-weight: 600; }
  p { font-size: 14px; overflow-wrap: anywhere; }
  .m { color: var(--text2); }
  .ac { display: flex; align-items: center; gap: 8px; } .sp { flex: 1; }
  .tonal { background: var(--high); color: var(--text); border-radius: 999px; padding: 8px 14px; font-weight: 500; font-size: 14px; white-space: nowrap; }
  .wide { min-height: 44px; }
  .out { border: 1px solid var(--line); color: var(--text); border-radius: 999px; padding: 8px 14px; font-size: 14px; white-space: nowrap; }
  .text { color: var(--icon); font-size: 14px; font-weight: 500; padding: 8px 4px; cursor: pointer; }
</style>
