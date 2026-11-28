<script lang="ts">
  import type { Session, State } from './contract'
  import { notes as notesOf, quotaLabel, resetLabel, tone, type Tone } from './header'
  import type { Glyph } from './permissionModes'
  import LongText from './LongText.svelte'
  import PermGlyph from './PermGlyph.svelte'
  import { t } from './t'

  // La scheda Dettagli della sessione (app-posizioni-sistema-27-11, gruppo 2): quello che stava nella riga dei dati della testata e
  // sotto di lei, coi tocchi di prima. Modello ed effort aprono il pannello Modello ed effort; la quota delle 5 ore porta a Utilizzo;
  // il contesto apre il suo foglio (handoff, compatta, finestra più grande); poi obiettivo, priorità e finestra, a capo. Le modifiche
  // hanno la loro scheda. Pannello e foglio restano nella testata (Header), una volta sola: qui `tune` e i due tocchi.
  let { s, st, tune, onTune, onCtx, onUsage }: {
    s: Session; st: State
    /** Modello ed effort come li mostra la testata, con la scelta appena fatta; il segno dei permessi; il puntino del consiglio. */
    tune: { label: string; glyph: Glyph; permTitle: string; tunable: boolean; dot: boolean } | null
    onTune: (anchor: HTMLElement) => void; onCtx: () => void; onUsage: () => void
  } = $props()
  const quota = $derived(st.quota[s.account])
  const reset = $derived(resetLabel(quota?.reset_h5, st.ts))
  const notes = $derived(notesOf(s, t.notes))
  // Il colore solo per lo stato (Franz 30/10 21:19), come nella testata: sotto le soglie l'anello è neutro.
  const ring: Record<Tone, string> = { neutral: 'var(--tx2)', warn: 'var(--b-warn)', alert: 'var(--b-alert)' }
  const arc = (pct: number) => `${Math.max(0, Math.min(100, pct)) * 0.4712} 100`
</script>

<div class="dt" data-rows data-testid="dettagli">
  {#if tune}
    <button class="dr" disabled={!tune.tunable} data-testid="dettagli-modello" onclick={(e) => onTune(e.currentTarget)}>
      <span class="dl">{t.detailsModel}</span>
      <span class="dv">
        <span class="lbl">{tune.label}</span>
        {#if tune.glyph !== 'none'}<span class="pm" title={tune.permTitle}><PermGlyph g={tune.glyph} /></span>{/if}
        {#if tune.tunable}<svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path d="M7 10l5 5 5-5z" fill="var(--text2)" /></svg>{/if}
        {#if tune.dot}<span class="dot" title={t.adviceDot} aria-label={t.adviceDot}></span>{/if}
      </span>
    </button>
  {/if}
  {#if quota?.h5 != null}
    <button class="dr" data-testid="dettagli-quota" title={reset ? t.quotaResetDesc(quota.h5, reset) : undefined} onclick={onUsage}>
      <span class="dl">{t.detailsQuota}</span>
      <span class="dv">
        <svg viewBox="0 0 20 20" width="16" height="16" aria-hidden="true"><circle cx="10" cy="10" r="7.5" class="trk" /><circle cx="10" cy="10" r="7.5" pathLength="47.12" stroke-dasharray={arc(quota.h5)} style="stroke:{quota.stale ? 'var(--wait)' : ring[tone(quota.h5)]}" class="arc" /></svg>
        <span class:stale={quota.stale}>{quotaLabel('full', t.quota5h(quota.h5), reset)}</span>
      </span>
    </button>
  {/if}
  {#if s.context != null}
    <button class="dr" data-testid="dettagli-contesto" disabled={!tune?.tunable} onclick={onCtx}>
      <span class="dl">{t.boxContext}</span>
      <span class="dv">
        <svg viewBox="0 0 20 20" width="16" height="16" aria-hidden="true"><circle cx="10" cy="10" r="7.5" class="trk" /><circle cx="10" cy="10" r="7.5" pathLength="47.12" stroke-dasharray={arc(s.context)} style="stroke:{ring[tone(s.context)]}" class="arc" /></svg>
        <span>{t.ctx(s.context)}</span>
      </span>
    </button>
  {/if}
  <!-- app-play-lettura-ovunque-27-11: le note vanno a capo; l'obiettivo lungo ha il ▶ accanto. -->
  {#each notes as n}<div class="dn"><LongText text={n} /></div>{/each}
</div>

<style>
  .dt { display: flex; flex-direction: column; border: 1px solid var(--line); border-radius: 14px; background: var(--s1); overflow: hidden; }
  .dr { display: flex; align-items: center; gap: 10px; width: 100%; min-height: 52px; padding: 8px 10px 8px 14px; border-top: 1px solid var(--line); text-align: left; color: var(--tx); }
  .dr:first-child { border-top: 0; }
  .dr:not(:disabled):hover { background: var(--s2); }
  .dr:disabled { cursor: default; }
  .dl { flex: 1; min-width: 0; font: 600 15px var(--display); white-space: nowrap; overflow: hidden; text-overflow: clip; }
  /* Il valore nella pillola della testata di prima: stessa forma, stesso testo. */
  .dv { position: relative; flex: none; display: flex; align-items: center; gap: 5px; height: 33px; padding: 0 10px; border-radius: 999px; background: var(--s2);
    font-size: 12.5px; font-weight: 500; white-space: nowrap; color: var(--text); }
  .dv svg { flex: none; }
  .lbl, .dv > span:not(.pm):not(.dot) { text-box: trim-both cap alphabetic; }
  .pm { display: inline-flex; }
  .stale { color: var(--wait); }
  .dot { position: absolute; top: 2px; right: 2px; width: 9px; height: 9px; border-radius: 50%; background: var(--advice); border: 2px solid var(--s1); }
  .trk, .arc { fill: none; stroke-width: 3; }
  .trk { stroke: var(--b-track); }
  .arc { stroke-linecap: round; transform: rotate(-90deg); transform-origin: center; }
  .dn { padding: 10px 14px; border-top: 1px solid var(--line); font-size: 12.5px; font-weight: 500; color: var(--b-label); letter-spacing: .03em; }
  .dn:first-child { border-top: 0; }
</style>
