<script lang="ts">
  import type { Head } from './kpi'
  import type { Box } from './HomeBox.svelte'
  import type { State } from './contract'
  import { personal } from './badge'
  import { resetLabel } from './header'
  import { shown } from './quotaLine'
  import { elapsedPct, paceText, quotaBar } from './utilizzo'
  import { t } from './t'
  // Proposta 7 (25/10): tre cifre in testa alla home. Ognuna apre la sua pagina, anche a zero (app-box-cliccabili-29-10).
  // Lo stile «Studio e Diretta» (terza versione, 30/10): il tabellone, con la quota delle 5 ore sotto le cifre al posto della
  // riga nella testata. Si colora solo chi ti aspetta, l'unico avviso della home; i rossi restano rossi. La barra della quota
  // è neutra e diventa ambra da 75 % e rossa da 90 %; la tacca bianca è quanto è passato della finestra delle 5 ore.
  // quota-senza-cinque-ore: dove il dato manca, per quell'account, la settimana col giorno e il ritmo.
  let { h, st = null, now = 0, onBox = () => {}, onQuota = () => {} }: { h: Head; st?: State | null; now?: number; onBox?: (b: Box) => void; onQuota?: () => void } = $props()
  const rings = $derived((st ? shown(Object.entries(st.quota)) : []).map(([name, r]) => [name, r, quotaBar(r.h5, r.w7, r.reset_h5, r.reset_w7, now)] as const).filter(([, r, b]) => b || r.stale))
  const tone = (pct: number) => (pct >= 90 ? 'var(--red)' : pct >= 75 ? 'var(--wait)' : 'var(--tx2)')
</script>

<div class="board" data-kpi="kpi-7">
  <button class="cell" data-testid="home-box-aspetta" onclick={() => onBox('aspetta')}><b class="n" class:wait={h.waiting > 0}>{h.waiting}</b><span class="l">{h.waiting ? t.kpi7Waiting(Math.max(1, Math.round((h.oldestWaitS ?? 0) / 60))) : t.kpi7NoneWaiting}</span><span class="ms chev" aria-hidden="true">chevron_right</span></button>
  <button class="cell" data-testid="home-box-lavoro" onclick={() => onBox('lavoro')}><b class="n">{h.busy}</b><span class="l">{t.kpi7Busy}</span><span class="ms chev" aria-hidden="true">chevron_right</span></button>
  <button class="cell" data-testid="home-box-esiti" onclick={() => onBox('esiti')}><b class="n">{h.outcomes ?? '–'}</b><span class="l">{#if h.outcomes == null}{t.kpi7OutcomesUnknown}{:else if h.red}{t.kpi7Outcomes}, <em>{t.kpi7Red(h.red)}</em>{:else}{t.kpi7Outcomes}{/if}</span><span class="ms chev" aria-hidden="true">chevron_right</span></button>
  {#if rings.length}
    <button class="quota" onclick={onQuota} aria-label={t.menuQuadro}>
      {#each rings as [name, r, b] (name)}
        {@const pct = b?.pct ?? 0}
        {@const stale = !!r.stale}
        {@const week = !!b?.week}
        {@const tick = stale || !b ? null : elapsedPct(b.resetAt, b.windowS, now)}
        {@const reset = rings.length === 1 && !stale && !week ? resetLabel(r.reset_h5, now) : null}
        {@const pace = week && !stale && b ? paceText(b.delta, b.day) : null}
        <span class="q" data-testid={week ? 'quota-senza-cinque-ore' : null}>
          {#if rings.length > 1}
            <svg viewBox="0 0 14 14" width="12" height="12" role="img" aria-label={personal(name, r.kind) ? t.badgePersonal : t.badgeWork}>
              {#if personal(name, r.kind)}<circle cx="7" cy="7" r="5.5" fill="none" stroke="var(--tx2)" stroke-width="2" />{:else}<rect x="1" y="1" width="12" height="12" rx="3.2" fill="none" stroke="var(--tx2)" stroke-width="2" />{/if}
            </svg>
          {/if}
          <strong>{week ? (rings.length > 1 ? t.quotaLineWeek : t.usageWeek) : rings.length > 1 ? t.quotaLineWindow : t.board5h}</strong>
          <span class="meter">{#if !stale}<i style="width:{pct}%;background:{tone(pct)}"></i>{/if}{#if tick != null}<b style="left:{tick}%"></b>{/if}</span>
          <span class="pct" style:color={pct >= 75 && !stale ? tone(pct) : null}>{stale ? t.quotaLineStale : `${pct}%`}</span>
          {#if reset}<span class="reset">{t.quotaResetsAt(reset)}</span>{/if}
          {#if pace}<span class="pace">{pace}</span>{/if}
        </span>
      {/each}
    </button>
  {/if}
</div>

<style>
  .board { flex: none; display: grid; grid-template-columns: 1.15fr 1fr 1.15fr; margin: 4px 0 2px; background: var(--s1); border: 1px solid var(--line); border-radius: 14px; overflow: hidden; }
  .cell { position: relative; padding: 12px 12px 13px; border-left: 1px solid var(--line); text-align: left; color: var(--tx); cursor: pointer; min-width: 0; }
  .cell:first-child { border-left: 0; }
  .cell:hover { background: var(--s2); }
  .cell:focus-visible, .quota:focus-visible { outline: 2px solid var(--tx); outline-offset: -2px; }
  .n { display: block; font: 700 42px/1 var(--display); letter-spacing: -0.01em; }
  .n.wait { color: var(--wait); }
  .l { display: block; font-size: 13.5px; line-height: 1.25; color: var(--tx2); margin-top: 6px; overflow-wrap: anywhere; }
  .l em { font-style: normal; font-weight: 500; color: var(--red); }
  .chev { position: absolute; top: 12px; right: 8px; color: var(--tx3); }
  .quota { grid-column: 1 / -1; display: flex; gap: 16px; padding: 10px 12px; border-top: 1px solid var(--line); text-align: left; cursor: pointer; min-width: 0; }
  .quota:hover { background: var(--s2); }
  .q { flex: 1; display: flex; flex-wrap: wrap; align-items: center; gap: 4px 10px; min-width: 0; font-size: 13.5px; color: var(--tx2); }
  .q svg { flex: none; }
  .q strong { font: 600 15px var(--display); color: var(--tx); white-space: nowrap; }
  .meter { flex: 1; position: relative; height: 6px; min-width: 40px; border-radius: 3px; background: var(--s3); }
  .meter i { position: absolute; inset: 0 auto 0 0; border-radius: 3px; }
  .meter b { position: absolute; top: -3px; width: 2px; height: 12px; margin-left: -1px; border-radius: 1px; background: var(--tx); }
  .pct { font: 400 12.5px var(--mono); color: var(--tx); font-variant-numeric: tabular-nums; white-space: nowrap; }
  .reset { white-space: nowrap; }
  .pace { flex: 1 0 100%; }
</style>
