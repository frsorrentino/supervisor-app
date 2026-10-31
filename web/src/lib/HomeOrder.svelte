<script lang="ts">
  import { DEFAULT_ORDER, move, normalize, orderArg, type HomeSection } from './homeOrder'
  import { t } from './t'
  // «Riordina la home» (Franz 30/10 17:22, compito app-riordino-sezioni-30-10; HomeOrderScreen.kt nell'app), nello stile «Studio e
  // Diretta»: le sezioni chiuse su righe singole, con le frecce su e giù: il trascinamento sta nella home, con la pressione lunga
  // sul titolo, che con un lettore di schermo non si può. Tabellone e «Ti aspettano» restano fissi in cima. «Salva» è l'unico tasto pieno.
  let { saved, onSave }: { saved?: string[]; onSave: (arg: string | null) => void } = $props()
  let order = $state<HomeSection[]>(normalize(saved))
  const SEC: Record<HomeSection, { icon: string; name: string }> = {
    sessions: { icon: 'play_circle', name: t.homeSessions }, suggestions: { icon: 'auto_awesome', name: t.sugTitle },
    lavori: { icon: 'checklist', name: t.lavoriTitle }, esiti: { icon: 'sports_score', name: t.homeOutcomes }, altro: { icon: 'data_usage', name: t.reorderTools },
  }
</script>

<div class="v3 ro" data-testid="riordino-sezioni">
  <p class="sub">{t.reorderSub}</p>
  <div class="vlist fixed">
    <div class="vrow"><span class="ic"><svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="var(--tx3)" stroke-width="1.8" stroke-linejoin="round" aria-hidden="true"><path d="M3 4h18v16H3zM9.5 4v11M15 4v11M3 15h18" /></svg></span><span class="tx"><b>{t.reorderBoard}</b></span></div>
    <div class="vrow"><span class="ic"><span class="ms" aria-hidden="true">pan_tool</span></span><span class="tx"><b>{t.homeWaiting}</b></span></div>
  </div>
  <div class="vlist">
    {#each order as k, i (k)}
      <div class="vrow">
        <span class="ic"></span>
        <span class="ms own" aria-hidden="true">{SEC[k].icon}</span>
        <span class="tx"><b>{SEC[k].name}</b></span>
        <button class="ar" disabled={i === 0} aria-label={t.reorderUp(SEC[k].name)} onclick={() => (order = move(order, i, i - 1))}><span class="ms" aria-hidden="true">expand_less</span></button>
        <button class="ar" disabled={i === order.length - 1} aria-label={t.reorderDown(SEC[k].name)} onclick={() => (order = move(order, i, i + 1))}><span class="ms" aria-hidden="true">expand_more</span></button>
      </div>
    {/each}
  </div>
  <button class="save" onclick={() => onSave(orderArg(order))}>{t.reorderSave}</button>
  <button class="reset" onclick={() => (order = [...DEFAULT_ORDER])}>{t.reorderReset}</button>
</div>

<style>
  .ro { display: flex; flex-direction: column; gap: 8px; padding: 8px 0 24px; max-width: 560px; min-height: 100%; }
  .sub { margin: 0 16px 4px; font-size: 14px; color: var(--tx2); }
  .fixed b { color: var(--tx2); }
  .fixed .ms { color: var(--tx3); }
  .own { color: var(--tx2); }
  .ar { flex: none; width: 40px; height: 40px; display: grid; place-items: center; border-radius: 20px; color: var(--tx); }
  .ar:disabled { color: var(--s3); }
  .save { margin: 8px 12px 0; min-height: 48px; border-radius: 24px; background: var(--act); color: var(--on-act); font: 600 17px var(--display); }
  .reset { align-self: center; padding: 12px; font-size: 14px; color: var(--tx); }
</style>
