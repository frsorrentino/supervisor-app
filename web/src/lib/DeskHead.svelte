<script lang="ts">
  import type { Head } from './kpi'
  import type { State } from './contract'
  import { personal } from './badge'
  import { resetLabel } from './header'
  import { shown } from './quotaLine'
  import { elapsedPct, paceText, quotaBar } from './utilizzo'
  import { t } from './t'
  // La testa di una colonna della plancia (pwa2, Franz 30/10 10:5x): la cifra del tabellone sopra il suo argomento, e il tocco
  // apre la sua pagina come i tre box della home (stessi data-testid). Si colora solo chi ti aspetta; i rossi restano rossi;
  // la quota delle 5 ore è neutra e diventa ambra da 75 % e rossa da 90 %, con la tacca di quanto è passato della finestra.
  type Kind = 'you' | 'work' | 'out' | 'tools'
  // `open`: le sessioni aperte; con lui la testa di «In corso» diventa quella delle Sessioni (app-sessioni-evidenti-06-11, J2).
  // `master` (app-sessioni-compatte-06-11): la colonna «Dalla master» conta solo quello che ha da fare Franz; i suggerimenti dopo «·».
  let { kind, h, st, now, sub = null, open = null, master = null, onOpen = () => {} }: { kind: Kind; h: Head; st: State; now: number; sub?: string | null; open?: number | null; master?: { todo: number; sug: number } | null; onOpen?: () => void } = $props()
  // quota-senza-cinque-ore: dove il dato manca, per quell'account, la settimana.
  const rings = $derived(shown(Object.entries(st.quota)).map(([name, r]) => [name, r, quotaBar(r.h5, r.w7, r.reset_h5, r.reset_w7, now, r.stale)] as const).filter(([, , b]) => b))
  // In testa la quota più alta fra gli account aggiornati: è quella che ferma il lavoro.
  const top = $derived(rings.filter(([, r]) => !r.stale).sort((a, b) => (b[2]?.pct ?? 0) - (a[2]?.pct ?? 0))[0] ?? null)
  const topPct = $derived(Math.round(top?.[2]?.pct ?? 0))
  const tone = (pct: number) => (pct >= 90 ? 'var(--red)' : pct >= 75 ? 'var(--wait)' : 'var(--tx2)')
  const testid: Record<Kind, string> = { you: 'home-box-aspetta', work: 'home-box-lavoro', out: 'home-box-esiti', tools: 'desk-head-tools' }
  // I nomi delle categorie della home (Franz 03/11 13:30, app-plancia-nomi-03-11): Tocca a te, In corso, Esiti, Risorse.
  const title: Record<Kind, string> = t.catName
</script>

<button class="chead" data-testid={testid[kind]} onclick={onOpen}>
  {#if kind === 'you' && master}<b class="n" class:wait={master.todo > 0} data-counter="you">{master.todo}</b>
  {:else if kind === 'you'}<b class="n" class:wait={h.waiting > 0} data-counter="you">{h.waiting}</b>
  <!-- Il numero grande conta solo le sessioni che ti aspettano; quelle al lavoro dopo «·» (app-sessioni-compatte-06-11). -->
  {:else if kind === 'work' && open != null}<b class="n" class:wait={h.waiting > 0}>{h.waiting}</b>
  {:else if kind === 'work'}<b class="n">{h.busy}</b>
  {:else if kind === 'out'}<b class="n">{h.outcomes ?? '–'}</b>
  {:else}<b class="n" style:color={top && topPct >= 75 ? tone(topPct) : null}>{top ? `${topPct}%` : '–'}</b>{/if}
  <span class="tt">
    <span class="t">{kind === 'work' && open != null ? t.deskSessionsTitle : title[kind]}</span>
    <span class="s">
      {#if kind === 'you' && master}{t.deskMasterSub(master.todo, master.sug)}
      {:else if kind === 'you'}{h.waiting ? t.kpi7Waiting(h.waiting, Math.max(1, Math.round((h.oldestWaitS ?? 0) / 60))) : t.kpi7NoneWaiting}
      {:else if kind === 'out'}{t.deskOutSub}{#if h.red}, <em>{t.kpi7Red(h.red)}</em>{/if}
      {:else if kind === 'tools'}{#if top?.[2]?.week}{paceText(top[2].delta, top[2].day) ?? t.deskToolsWeek}{:else}{t.deskToolsSub(top ? resetLabel(top[1].reset_h5, now) : null)}{/if}
      {:else if open != null}{t.deskSessionsSub(h.waiting, h.busy)}{:else}{t.kpi7Busy}{/if}
    </span>
    {#if kind === 'tools'}
      {#each rings as [name, r, b] (name)}
        {@const pct = Math.round(b?.pct ?? 0)}
        {@const tick = r.stale || !b ? null : elapsedPct(b.resetAt, b.windowS, now)}
        <span class="q">
          {#if rings.length > 1}
            <svg viewBox="0 0 14 14" width="11" height="11" role="img" aria-label={personal(name, r.kind) ? t.badgePersonal : t.badgeWork}>
              {#if personal(name, r.kind)}<circle cx="7" cy="7" r="5.5" fill="none" stroke="var(--tx2)" stroke-width="2" />{:else}<rect x="1" y="1" width="12" height="12" rx="3.2" fill="none" stroke="var(--tx2)" stroke-width="2" />{/if}
            </svg>
          {/if}
          <span class="meter"><i style="width:{pct}%;background:{r.stale ? 'var(--tx3)' : tone(pct)}"></i>{#if tick != null}<b style="left:{tick}%"></b>{/if}</span>
          {#if rings.length > 1}<span class="pct" style:color={r.stale ? 'var(--tx3)' : null} title={r.stale ? t.quotaLineStale : null}>{pct}%</span>{/if}
        </span>
      {/each}
    {/if}
  </span>
  <span class="ms chev" aria-hidden="true">chevron_right</span>
</button>

<style>
  .chead { position: sticky; top: 0; z-index: 2; display: grid; grid-template-columns: auto minmax(0, 1fr) auto; column-gap: 14px; align-items: center; width: 100%; min-height: 92px;
    padding: 14px 14px 14px 18px; background: var(--s2); border-bottom: 1px solid var(--line); text-align: left; color: var(--tx); cursor: pointer; }
  .chead:hover { background: var(--s3); }
  .chead:focus-visible { outline: 2px solid var(--tx); outline-offset: -2px; }
  .n { font: 700 42px/1 var(--display); letter-spacing: -0.01em; }
  .n.wait { color: var(--wait); }
  .tt { min-width: 0; display: flex; flex-direction: column; }
  .t { font: 700 19px/1.15 var(--display); }
  .s { font-size: 14px; line-height: 1.3; color: var(--tx2); margin-top: 3px; }
  .s em { font-style: normal; font-weight: 500; color: var(--red); }
  .chev { align-self: start; margin-top: 2px; color: var(--tx3); }
  .q { display: flex; align-items: center; gap: 8px; margin-top: 8px; }
  .q + .q { margin-top: 5px; }
  .meter { flex: 1; position: relative; height: 6px; border-radius: 3px; background: var(--s3); }
  .meter i { position: absolute; inset: 0 auto 0 0; border-radius: 3px; }
  /* Il segno del ritmo in giallo, come nei grafici di Utilizzo (app-grafici-colore-06-11). */
  .meter b { position: absolute; top: -3px; width: 2px; height: 12px; margin-left: -1px; border-radius: 1px; background: var(--q-now); }
  .pct { font: 400 12.5px var(--mono); color: var(--tx2); }
</style>
