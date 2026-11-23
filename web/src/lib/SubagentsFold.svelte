<script lang="ts">
  import type { Subagents } from './contract'
  import { lang } from './lang'
  import { fold as foldIn } from './motion'
  import { duration, more, rows, summary, tokens } from './subagents'
  import { t } from './t'
  // app-sottoagenti-23-11 (variante A del mockup, Franz 23/11 13:26 «procedi con la consigliata»): i sottoagenti di una sessione come
  // righe sotto la sessione. La sezione nasce chiusa e si apre col toggle delle altre sezioni (la freccia che gira); chiusa dice solo
  // «2 al lavoro, 5 finiti». Aperta: chi lavora in cima col pallino blu, i finiti con la spunta, cosa fa, tipo e modello, da quanto o
  // quanto ha lavorato; «Tutti e N» chiede al PC l'elenco intero (op `subagents`, fino a 30). Contratto 1.158.
  let { subs, now, onAll = null }: { subs: Subagents; now: number; onAll?: (() => Promise<Subagents | null>) | null } = $props()
  let open = $state(false)
  let full = $state<Subagents | null>(null)
  let asking = $state(false)
  let failed = $state(false)
  const shown = $derived(full ?? subs)
  const sum = $derived(summary(subs))
  const list = $derived(rows(shown, now))
  const all = $derived(full ? null : more(subs))
  const en = lang === 'en'
  async function askAll() {
    if (!onAll || asking) return
    asking = true; failed = false
    const r = await onAll().catch(() => null)
    asking = false
    if (r) full = r; else failed = true
  }
  const meta = (r: ReturnType<typeof rows>[number]) => [r.type, r.model, r.bg ? t.subagentsBg : null, r.phase, r.tokens ? t.subagentsTokens(tokens(r.tokens[0], en), tokens(r.tokens[1], en)) : null].filter(Boolean).join(' · ')
</script>

<div class="subs" data-testid="sottoagenti">
  <button class="tg" aria-expanded={open} data-testid="sottoagenti-toggle" onclick={() => (open = !open)}>
    <span class="sum"><span class="lb">{t.subagentsTitle}</span> · {#if sum.running}<b>{t.subagentsSum(sum.running, 0)}</b>{sum.done ? ', ' : ''}{/if}{t.subagentsSum(0, sum.done)}</span>
    <svg class="turn" class:up={open} viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"><path d="M6 9l6 6 6-6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
  </button>
  {#if open}<div class="fb" in:foldIn out:foldIn={{ ms: 150 }}><div class="foldin">
    {#each list as r (r.id)}
      <div class="sr" data-testid="sottoagente">
        {#if r.running}<span class="dot" aria-hidden="true"></span>
        {:else}<svg class="ok" viewBox="0 0 24 24" width="16" height="16" aria-hidden="true"><path d="m5 12.5 4.5 4.5L19 7.5" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" /></svg>{/if}
        <span class="b"><span class="d">{r.title}</span><span class="k">{meta(r)}</span></span>
        {#if r.seconds != null}<span class="tm">{duration(r.seconds)}</span>{/if}
      </div>
    {/each}
    {#if all && onAll}
      <button class="all" disabled={asking} data-testid="sottoagenti-tutti" onclick={askAll}>{asking ? t.subagentsLoading : failed ? t.subagentsFailed : t.subagentsAll(all)}</button>
    {:else if full}
      <button class="all" onclick={() => (full = null)}>{t.subagentsFewer}</button>
    {/if}
  </div></div>{/if}
</div>

<style>
  .subs { margin: 0 0 8px 40px; border-top: 1px solid var(--line); }
  .tg { display: flex; align-items: center; gap: 8px; width: 100%; min-height: 44px; text-align: left; color: var(--text2); cursor: pointer; }
  .sum { flex: 1; min-width: 0; font-size: 13.5px; line-height: 1.35; }
  .sum .lb { color: var(--text); font-weight: 500; }
  .sum b { color: var(--busy); font-weight: 600; }
  .turn { flex: none; color: var(--text2); transition: transform 200ms cubic-bezier(0.23, 1, 0.32, 1); }
  .turn.up { transform: rotate(180deg); }
  @media (prefers-reduced-motion: reduce) { .turn { transition: none; } }
  .fb { display: grid; grid-template-columns: minmax(0, 1fr); grid-template-rows: 1fr; }
  /* Il pallino, la spunta e la durata stanno sulla prima riga del titolo (14px × 1,3), come sul telefono. */
  .foldin { min-height: 0; display: flex; flex-direction: column; }
  .sr { display: grid; grid-template-columns: 16px minmax(0, 1fr) auto; gap: 8px; align-items: start; padding: 5px 0; }
  .dot { width: 8px; height: 8px; border-radius: 50%; background: var(--busy); box-shadow: 0 0 0 3px color-mix(in srgb, var(--busy) 22%, transparent); justify-self: center; margin-top: 5px; }
  .ok { color: var(--idle); margin-top: 1px; }
  .b { min-width: 0; display: flex; flex-direction: column; }
  .d { font-size: 14px; line-height: 1.3; color: var(--text); overflow-wrap: anywhere; }
  .k { font: 12px/1.35 var(--mono); color: var(--text2); margin-top: 1px; overflow-wrap: anywhere; }
  .tm { font: 12px/18px var(--mono); color: var(--text2); white-space: nowrap; }
  .all { align-self: flex-start; min-height: 40px; padding: 0 2px; font-size: 13.5px; font-weight: 500; color: var(--busy); }
  .all:disabled { color: var(--text2); }
</style>
