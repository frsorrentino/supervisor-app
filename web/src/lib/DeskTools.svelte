<script lang="ts">
  import type { Project, State } from './contract'
  import { hostNumbers, recentProjects } from './dash'
  import { since } from './durations'
  import { t } from './t'
  // La colonna «Numeri e strumenti» della plancia (pwa2, Franz 30/10), dopo Utilizzo, Sistema, Plugin e Chiuse della home:
  // lo stato del PC Windows in testo col pallino (le sue sessioni si aprono dal nome), le pubblicazioni di oggi, i ricorrenti
  // e il lancio rapido, l'unico posto con le pillole. Dati dal contratto 1.75 come nel cruscotto di prima (Dash.svelte).
  let { st, now, onPick, onTalk, onLaunch }: { st: State; now: number; onPick: (name: string) => void; onTalk: (text: string) => void; onLaunch: (p: Project) => void } = $props()
  const hosts = $derived(st.hosts_status ?? [])
  const recurring = $derived(st.recurring ?? [])
  const recent = $derived(recentProjects(st))
  let chosen = $state<Project | null>(null)
  // Le sezioni di Risorse si aprono e si chiudono col titolo come quelle di Tocca a te (web-pubblicazioni-voci-06-11), ricordate
  // in questo browser; le pubblicazioni di oggi sono passate in Esiti.
  const SHUT = 'cm.tools_shut'
  let shut = $state<Record<string, boolean>>((() => { try { return JSON.parse(localStorage.getItem(SHUT) ?? '{}') } catch { return {} } })())
  function toggle(k: string) { shut = { ...shut, [k]: !shut[k] }; try { localStorage.setItem(SHUT, JSON.stringify(shut)) } catch { /* resta per questa visita */ } }
</script>

{#snippet head(k: string, icon: string, title: string, n: number | null = null)}
  <button class="vsec vtog" aria-expanded={!shut[k]} onclick={() => toggle(k)}><span class="ic"><span class="ms" aria-hidden="true">{icon}</span></span>{title}{#if n}<small>{n}</small>{/if}<span class="ms tg" aria-hidden="true">{shut[k] ? 'expand_more' : 'expand_less'}</span></button>
{/snippet}

{@render head('win', 'computer', t.dashWin)}
{#if !shut.win}<div class="vlist">
  {#each hosts as h (h.name)}
    <div class="vrow">
      <span class="ic"><span class="ms" aria-hidden="true">computer</span></span>
      <span class="tx">
        <b>{h.name}</b>
        <small><i class="vdot" class:down={!h.up}></i>{h.up ? hostNumbers(h) : t.dashHostDown(h.error ?? '')}</small>
        {#if h.sessions.length}
          <small>{t.deskHostSessions} {#each h.sessions as n, i (n)}<button class="lnk" onclick={() => onPick(n)}>{n}</button>{i < h.sessions.length - 1 ? ', ' : ''}{/each}</small>
        {:else if h.up}<small>{t.dashHostNoSessions}</small>{/if}
      </span>
      {#if h.at}<span class="mono">{now - h.at < 60 ? t.dashHostNow : t.dashHostRead(since(h.at, now))}</span>{/if}
    </div>
  {:else}
    <div class="vrow"><span class="ic"></span><span class="tx"><small>{t.dashWinNone}</small></span></div>
  {/each}
</div>{/if}

{#if recurring.length}
  {@render head('recurring', 'repeat', t.recurring, recurring.length)}
  {#if !shut.recurring}<div class="vlist">
    {#each recurring as r (r.id)}
      <button class="vrow" onclick={() => onTalk(r.prompt)}><span class="ic"><span class="ms" aria-hidden="true">repeat</span></span><span class="tx"><b>{r.label}</b></span></button>
    {/each}
  </div>{/if}
{/if}

{#if recent.length}
  {@render head('launch', 'bolt', t.dashLaunch)}
  {#if !shut.launch}<div class="vlist">
    <div class="vchips">
      {#each recent as p (p.path)}
        <button class="vchip" class:on={chosen === p} aria-pressed={chosen === p} onclick={() => (chosen = chosen === p ? null : p)}>{p.name}</button>
      {/each}
    </div>
    {#if chosen}<div class="vgo"><button class="vbtn" onclick={() => { if (chosen) onLaunch(chosen); chosen = null }}>{t.dashLaunchGo(chosen.name)}</button></div>{/if}
  </div>{/if}
{/if}

<style>
  /* Il titolo di una sezione di Risorse è il tasto che la apre e la chiude, con le misure dei titoli (.vsec). */
  .vtog { width: 100%; text-align: left; cursor: pointer; padding: 0 13px 0 0; background: none; border: 0; }
  .vtog .tg { margin-left: auto; color: var(--tx3); font-size: 20px; }
</style>
