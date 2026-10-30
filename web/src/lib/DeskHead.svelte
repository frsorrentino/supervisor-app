<script lang="ts">
  import type { Head } from './kpi'
  import type { State } from './contract'
  import { personal } from './badge'
  import { resetLabel } from './header'
  import { shown } from './quotaLine'
  import { t } from './t'
  // La testa di una colonna della plancia (pwa2, Franz 30/10 10:5x): la cifra del tabellone sopra il suo argomento, e il tocco
  // apre la sua pagina come i tre box della home (stessi data-testid). Si colora solo chi ti aspetta; i rossi restano rossi;
  // la quota delle 5 ore è neutra e diventa ambra da 75 % e rossa da 90 %, con la tacca di quanto è passato della finestra.
  type Kind = 'you' | 'work' | 'out' | 'tools'
  let { kind, h, st, now, sub = null, onOpen = () => {} }: { kind: Kind; h: Head; st: State; now: number; sub?: string | null; onOpen?: () => void } = $props()
  const rings = $derived(shown(Object.entries(st.quota)))
  // In testa la quota più alta fra gli account aggiornati: è quella che ferma il lavoro.
  const top = $derived(rings.filter(([, r]) => !r.stale).sort((a, b) => (b[1].h5 ?? 0) - (a[1].h5 ?? 0))[0] ?? null)
  const pctOf = (x: number | null | undefined) => Math.min(100, Math.max(0, Math.round(x ?? 0)))
  const tone = (pct: number) => (pct >= 90 ? 'var(--red)' : pct >= 75 ? 'var(--wait)' : 'var(--tx2)')
  const WINDOW = 5 * 3600
  const elapsed = (reset?: number | null) => (reset != null && reset > now ? Math.max(0, Math.min(100, (1 - (reset - now) / WINDOW) * 100)) : null)
  const testid: Record<Kind, string> = { you: 'home-box-aspetta', work: 'home-box-lavoro', out: 'home-box-esiti', tools: 'desk-head-tools' }
  const title: Record<Kind, string> = { you: t.deskYou, work: t.deskWork, out: t.deskOut, tools: t.deskTools }
</script>

<button class="chead" data-testid={testid[kind]} onclick={onOpen}>
  {#if kind === 'you'}<b class="n" class:wait={h.waiting > 0}>{h.waiting}</b>
  {:else if kind === 'work'}<b class="n">{h.busy}</b>
  {:else if kind === 'out'}<b class="n">{h.outcomes ?? '–'}</b>
  {:else}<b class="n" style:color={top && pctOf(top[1].h5) >= 75 ? tone(pctOf(top[1].h5)) : null}>{top ? `${pctOf(top[1].h5)}%` : '–'}</b>{/if}
  <span class="tt">
    <span class="t">{title[kind]}</span>
    <span class="s">
      {#if kind === 'you'}{h.waiting ? t.kpi7Waiting(Math.max(1, Math.round((h.oldestWaitS ?? 0) / 60))) : t.kpi7NoneWaiting}
      {:else if kind === 'out'}{t.deskOutSub}{#if h.red}, <em>{t.kpi7Red(h.red)}</em>{/if}
      {:else if kind === 'tools'}{t.deskToolsSub(top ? resetLabel(top[1].reset_h5, now) : null)}
      {:else}{sub}{/if}
    </span>
    {#if kind === 'tools'}
      {#each rings as [name, r] (name)}
        {@const pct = pctOf(r.h5)}
        {@const tick = r.stale ? null : elapsed(r.reset_h5)}
        <span class="q">
          {#if rings.length > 1}
            <svg viewBox="0 0 14 14" width="11" height="11" role="img" aria-label={personal(name, r.kind) ? t.badgePersonal : t.badgeWork}>
              {#if personal(name, r.kind)}<circle cx="7" cy="7" r="5.5" fill="none" stroke="var(--tx2)" stroke-width="2" />{:else}<rect x="1" y="1" width="12" height="12" rx="3.2" fill="none" stroke="var(--tx2)" stroke-width="2" />{/if}
            </svg>
          {/if}
          <span class="meter">{#if !r.stale}<i style="width:{pct}%;background:{tone(pct)}"></i>{/if}{#if tick != null}<b style="left:{tick}%"></b>{/if}</span>
          {#if rings.length > 1}<span class="pct">{r.stale ? t.quotaLineStale : `${pct}%`}</span>{/if}
        </span>
      {/each}
    {/if}
  </span>
  <span class="ms chev" aria-hidden="true">chevron_right</span>
</button>

<style>
  .chead { position: sticky; top: 0; z-index: 2; display: grid; grid-template-columns: auto minmax(0, 1fr) auto; column-gap: 14px; align-items: center; width: 100%; min-height: 92px;
    padding: 14px 14px 14px 18px; background: var(--s1); border-bottom: 1px solid var(--line); text-align: left; color: var(--tx); cursor: pointer; }
  .chead:hover { background: var(--s2); }
  .chead:focus-visible { outline: 2px solid var(--tx); outline-offset: -2px; }
  .n { font: 700 42px/1 var(--display); letter-spacing: -0.01em; }
  .n.wait { color: var(--wait); }
  .tt { min-width: 0; display: flex; flex-direction: column; }
  .t { font: 700 19px/1.15 var(--display); }
  .s { font-size: 13.5px; line-height: 1.3; color: var(--tx2); margin-top: 3px; }
  .s em { font-style: normal; font-weight: 500; color: var(--red); }
  .chev { align-self: start; margin-top: 2px; color: var(--tx3); }
  .q { display: flex; align-items: center; gap: 8px; margin-top: 8px; }
  .q + .q { margin-top: 5px; }
  .meter { flex: 1; position: relative; height: 6px; border-radius: 3px; background: var(--s3); }
  .meter i { position: absolute; inset: 0 auto 0 0; border-radius: 3px; }
  .meter b { position: absolute; top: -3px; width: 2px; height: 12px; margin-left: -1px; border-radius: 1px; background: var(--tx); }
  .pct { font: 400 12px var(--mono); color: var(--tx2); }
</style>
