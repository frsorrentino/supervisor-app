<script lang="ts">
  import type { Session } from './contract'
  import { headsUpKey, visibleHeadsUp } from './decide'
  import { since } from './durations'
  import { t } from './t'
  // La nota di Claude Code (contratto 1.99, heads_up), nello stile di «Non provato». Nessun tasto mandato al terminale; «Già lo so» vale su questo dispositivo.
  let { s, now, onPropose }: { s: Session; now: number; onPropose: (text: string) => void } = $props()
  const KEY = 'cm.heads_up_seen'
  let seen = $state(new Set<string>((() => { try { return JSON.parse(localStorage.getItem(KEY) ?? '[]') as string[] } catch { return [] } })()))
  const n = $derived(visibleHeadsUp(s, seen))
  function known() { seen = new Set([...seen, headsUpKey(s.name, n!.text)]); try { localStorage.setItem(KEY, JSON.stringify([...seen])) } catch { /* resta per questa visita */ } }
</script>

{#if n}
  <div class="hu" data-testid="heads-up">
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="12" r="9" /><path d="M12 11v5M12 8h.01" /></svg>
    <div>
      <p>{n.text}</p><small>{t.queueAge(since(n.seen_at, now))}</small>
      <div class="ac"><button onclick={() => onPropose(t.headsUpPrompt(n.text))}>{t.headsUpPropose}</button><button class="q" onclick={known}>{t.headsUpKnown}</button></div>
    </div>
  </div>
{/if}

<style>
  .hu { display: flex; gap: 8px; color: var(--text2); padding: 6px 12px; }
  .hu svg { flex: none; margin-top: 3px; color: var(--tx3); }
  p { font-size: 14px; }
  small { font: 12px var(--mono); }
  .ac { display: flex; gap: 12px; margin-top: 4px; }
  button { color: var(--icon); font-weight: 500; font-size: 14px; padding: 4px 0; }
  button.q { color: var(--text2); }
</style>
