<script lang="ts">
  import type { Project, State } from './contract'
  import { hostNumbers, publishedRows, recentProjects } from './dash'
  import { since } from './durations'
  import { locale } from './lang'
  import { t } from './t'
  // La colonna «Numeri e strumenti» della plancia (pwa2, Franz 30/10), dopo Utilizzo, Sistema, Plugin e Chiuse della home:
  // lo stato del PC Windows in testo col pallino (le sue sessioni si aprono dal nome), le pubblicazioni di oggi, i ricorrenti
  // e il lancio rapido, l'unico posto con le pillole. Dati dal contratto 1.75 come nel cruscotto di prima (Dash.svelte).
  let { st, now, onPick, onTalk, onLaunch }: { st: State; now: number; onPick: (name: string) => void; onTalk: (text: string) => void; onLaunch: (p: Project) => void } = $props()
  const hosts = $derived(st.hosts_status ?? [])
  const published = $derived(publishedRows(st.published_today))
  const recurring = $derived(st.recurring ?? [])
  const recent = $derived(recentProjects(st))
  let chosen = $state<Project | null>(null)
  const hhmm = (s: number) => new Date(s * 1000).toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })
</script>

<div class="vsec"><span class="ic"><span class="ms" aria-hidden="true">computer</span></span>{t.dashWin}</div>
<div class="vlist">
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
</div>

<div class="vsec"><span class="ic"><span class="ms" aria-hidden="true">publish</span></span>{t.dashPublished}{#if published.length}<small>{published.length}</small>{/if}</div>
<div class="vlist">
  {#each published as p, i (i)}
    <svelte:element this={p.url ? 'a' : 'div'} class="vrow" href={p.url ?? undefined} target={p.url ? '_blank' : undefined} rel={p.url ? 'noopener' : undefined}>
      <span class="ic"><span class="ms" aria-hidden="true">north_east</span></span>
      <span class="tx"><b>{t.dashPubKind[p.kind] ?? p.kind}, {p.name}</b><small>{p.text}</small></span>
      <span class="mono">{hhmm(p.at)}</span>
    </svelte:element>
  {:else}
    <div class="vrow"><span class="ic"></span><span class="tx"><small>{t.dashPublishedNone}</small></span></div>
  {/each}
</div>

{#if recurring.length}
  <div class="vsec"><span class="ic"><span class="ms" aria-hidden="true">repeat</span></span>{t.recurring}</div>
  <div class="vlist">
    {#each recurring as r (r.id)}
      <button class="vrow" onclick={() => onTalk(r.prompt)}><span class="ic"><span class="ms" aria-hidden="true">repeat</span></span><span class="tx"><b>{r.label}</b></span></button>
    {/each}
  </div>
{/if}

{#if recent.length}
  <div class="vsec"><span class="ic"><span class="ms" aria-hidden="true">bolt</span></span>{t.dashLaunch}</div>
  <div class="vlist">
    <div class="vchips">
      {#each recent as p (p.path)}
        <button class="vchip" class:on={chosen === p} aria-pressed={chosen === p} onclick={() => (chosen = chosen === p ? null : p)}>{p.name}</button>
      {/each}
    </div>
    {#if chosen}<div class="vgo"><button class="vbtn" onclick={() => { if (chosen) onLaunch(chosen); chosen = null }}>{t.dashLaunchGo(chosen.name)}</button></div>{/if}
  </div>
{/if}
