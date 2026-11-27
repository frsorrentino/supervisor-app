<script lang="ts">
  import type { Session } from './contract'
  import { headsUpKey, visibleHeadsUp } from './decide'
  import { since } from './durations'
  import { t } from './t'
  import { huSeen, markKnown } from './headsUpSeen.svelte'
  import LongText from './LongText.svelte'
  // La nota di Claude Code (contratto 1.99, heads_up), nello stile di «Non provato». Nessun tasto mandato al terminale; «Già lo so» vale su questo dispositivo.
  let { s, now, onPropose }: { s: Session; now: number; onPropose: (text: string) => void } = $props()
  const n = $derived(visibleHeadsUp(s, huSeen.set))
  const known = () => markKnown(headsUpKey(s.name, n!.text))
</script>

{#if n}
  <div class="hu" data-testid="heads-up">
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="12" r="9" /><path d="M12 11v5M12 8h.01" /></svg>
    <!-- app-play-lettura-ovunque-27-11: la nota col ▶ accanto, se va oltre una riga. -->
    <div class="bd">
      <div class="tx"><LongText text={n.text} /></div><small>{t.queueAge(since(n.seen_at, now))}</small>
      <div class="ac"><button onclick={() => onPropose(t.headsUpPrompt(n.text))}>{t.headsUpPropose}</button><button class="q" onclick={known}>{t.headsUpKnown}</button></div>
    </div>
  </div>
{/if}

<style>
  .hu { display: flex; gap: 8px; color: var(--text2); padding: 6px 12px; }
  .hu svg { flex: none; margin-top: 3px; color: var(--tx3); }
  .bd { flex: 1; min-width: 0; }
  .tx { font-size: 14px; }
  small { font: 12.5px var(--mono); }
  /* I tasti restano interi e vanno a capo insieme (a 320 px in inglese «I know» si spezzava in due righe). */
  .ac { display: flex; flex-wrap: wrap; gap: 4px 12px; margin-top: 4px; }
  .ac button { white-space: nowrap; }
  button { color: var(--icon); font-weight: 500; font-size: 14px; padding: 4px 0; }
  button.q { color: var(--text2); }
</style>
