<script lang="ts">
  import type { AgendaPage, DonePage, Event, State } from './contract'
  import { done as doneOf, doneFromPages, forYou as forYouOf, inProgress as inProgressOf, ymd } from './lavori'
  import { since } from './durations'
  import { locale } from './lang'
  import StateGlyph from './StateGlyph.svelte'
  import { t } from './t'
  // Le due liste del registro nelle colonne della plancia (pwa2, Franz 30/10): «Compiti in corso» sotto le sessioni e «Fatto
  // oggi» sotto la notte. Solo le prime righe; il resto, con le viste e le prove, sta nella pagina Lavori.
  let { part, st, agenda, now, events, donePages, onLavori }: { part: 'progress' | 'done'; st: State; agenda: AgendaPage | null; now: number; events: Event[]; donePages: Record<string, DonePage>; onLavori: () => void } = $props()
  const MAX = 5
  const zone = Intl.DateTimeFormat().resolvedOptions().timeZone
  const day = $derived(ymd(now, zone))
  const ip = $derived(inProgressOf(st, agenda, now, zone, day))
  const lines = $derived([...ip.working, ...ip.you, ...ip.others])
  const fy = $derived(forYouOf(st, agenda, now, day))
  const later = $derived(fy.outside.length + fy.claude.length)
  const dn = $derived((Object.keys(donePages).length ? doneFromPages(donePages, zone) : doneOf(events, zone, day)).find(d => d.date === day)?.rows ?? [])
  // Chiuso (app-sessioni-evidenti-06-11, scelta J2): titolo e numero sotto le sessioni; il tocco lo apre sul posto.
  let tasksOpen = $state(false)
  const hhmm = (s: number) => new Intl.DateTimeFormat(locale, { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date(s * 1000))
</script>

{#if part === 'progress'}
  <button class="vsec vtog" aria-expanded={tasksOpen} onclick={() => (tasksOpen = !tasksOpen)}><span class="ic"><span class="ms" aria-hidden="true">checklist</span></span>{t.deskTasks}{#if ip.total}<small>{ip.total}</small>{/if}<span class="rt">{t.deskFromRegistry}</span><span class="ms tg" aria-hidden="true">{tasksOpen ? 'expand_less' : 'expand_more'}</span></button>
  {#if tasksOpen}<div class="vlist">
    {#each lines.slice(0, MAX) as l, i (i)}
      <button class="vrow" onclick={onLavori}>
        <span class="ic"><span class="ms" aria-hidden="true">event_list</span></span>
        <span class="tx"><b>{l.short}</b>{#if l.session || l.since}<small class="mono">{[l.session, l.since ? t.deskTaskSince(since(l.since, now)) : null].filter(Boolean).join('  ')}</small>{/if}</span>
      </button>
    {/each}
    <button class="vrow" onclick={onLavori}><span class="ic"></span><span class="tx"><b>{t.deskAllTasks}</b></span>{#if later}<span class="mono">{t.deskLater(later)}</span>{/if}<span class="ms" aria-hidden="true" style="color:var(--tx3)">chevron_right</span></button>
  </div>{/if}
{:else}
  <div class="vsec"><span class="ic"><span class="ms" aria-hidden="true">task_alt</span></span>{t.deskDoneToday}{#if dn.length}<small>{dn.length}</small>{/if}</div>
  <div class="vlist">
    {#each dn.slice(0, MAX) as d (d.task)}
      <button class="vrow" onclick={onLavori}>
        <span class="ic"><StateGlyph kind="done" /></span>
        <span class="tx"><b>{d.title}</b><small class="mono">{[d.session, hhmm(d.at)].filter(Boolean).join('  ')}</small></span>
      </button>
    {:else}
      <div class="vrow"><span class="ic"></span><span class="tx"><small>{t.lavoriDoneEmpty}</small></span></div>
    {/each}
    {#if dn.length > MAX}
      <button class="vrow" onclick={onLavori}><span class="ic"></span><span class="tx"><b>{t.deskAllTasks}</b></span><span class="mono">{dn.length}</span><span class="ms" aria-hidden="true" style="color:var(--tx3)">chevron_right</span></button>
    {/if}
  </div>
{/if}

<style>
  /* «Compiti in corso» chiuso: il titolo è il tasto che apre la lista (app-sessioni-evidenti-06-11, J2). */
  /* Come gli altri titoli (.vsec): senza il riempimento del tasto, che lo spostava di 6 px (web-tocca-apribile-06-11). */
  .vtog { width: 100%; text-align: left; cursor: pointer; padding: 0 13px 0 0; background: none; border: 0; }
  .vtog .tg { margin-left: 8px; color: var(--tx3); font-size: 20px; }
</style>
