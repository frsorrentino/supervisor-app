<script lang="ts">
  import { locale } from './lang'
  import type { CaseView, StudioModel, StudioStep } from './studio'
  import { t } from './t'
  // La pagina Studio (spec del 18/10, approvata da Franz): scadenziario in cima, poi le pratiche (in evidenza quelle con
  // una scadenza in avviso, una sessione o un esito recente, le altre richiuse), i Prossimi e il recap delle pratiche.
  // Quota, ricorrenti e segnalazioni restano sulla home. Il dock della master sta sopra, in HomePane.
  let { model, error, loading, old, onStep, onSession, onLaunch, onSpeak }: {
    model: StudioModel | null; error: string | null; loading: boolean; old: boolean
    onStep: (s: StudioStep) => void; onSession: (name: string) => void; onLaunch: (c: CaseView) => void; onSpeak: (text: string) => void
  } = $props()
  let restOpen = $state(false)
  let opened = $state<string | null>(null)
  /** Sopra i 120 caratteri il ▶ per ascoltare (tts.min_chars). */
  const TTS_MIN = 120
  const dm = (iso: string) => `${iso.slice(8, 10)}/${iso.slice(5, 7)}`
  const when = (at: number) => {
    const d = new Date(at * 1000)
    return d.toDateString() === new Date().toDateString() ? d.toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' }) : d.toLocaleDateString(locale, { day: '2-digit', month: '2-digit' })
  }
  function tap(c: CaseView) {
    if (c.session) onSession(c.session.name)
    else opened = opened === c.id ? null : c.id
  }
</script>

{#snippet row(c: CaseView)}
  <button class="case" aria-expanded={c.session ? undefined : opened === c.id} onclick={() => tap(c)}>
    <span class="name">{c.name}</span>
    <small><span class:live={!!c.session}>{c.session ? `${t.studioLive} · ${c.session.name}` : t.studioIdle}</span>{c.next ? ` · ${dm(c.next.date)} ${c.next.summary}` : ''}</small>
    {#if c.last}<small>{when(c.last.at)} · {c.last.short}</small>{/if}
  </button>
  {#if !c.session && opened === c.id}<div class="launch"><button class="key" onclick={() => onLaunch(c)}>{t.studioLaunch}</button></div>{/if}
{/snippet}

<div class="studio">
  {#if old}<p class="note">{t.studioOld}</p>{/if}
  {#if error}<p class="note">{error}</p>{:else if !model && loading}<p class="note">{t.recapLoading}</p>{/if}
  {#if model}
    <h2 class="gh" style="--t:var(--b-warn)"><span>{t.studioDeadlines.toUpperCase()}</span><i></i></h2>
    {#each model.deadlines as d (d.uid)}
      <div class="dl" class:due={d.due}><b>{dm(d.date)}</b><span>{t.studioWhen(d.days_left)} · {d.summary}{d.caseName ? ` · ${d.caseName}` : ''}</span></div>
    {:else}<p class="note">{t.studioNone}</p>{/each}

    <h2 class="gh" style="--t:var(--text)"><span>{t.studioCases.toUpperCase()}</span><i></i></h2>
    {#each model.open as c (c.id)}{@render row(c)}{/each}
    {#if model.rest.length}
      <button class="fold" aria-expanded={restOpen} onclick={() => (restOpen = !restOpen)}>
        <span>{t.studioMore(model.rest.length)}</span>
        <svg viewBox="0 0 24 24" width="20" height="20"><path d={restOpen ? 'M6 15l6-6 6 6' : 'M6 9l6 6 6-6'} fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" /></svg>
      </button>
      {#if restOpen}{#each model.rest as c (c.id)}{@render row(c)}{/each}{/if}
    {/if}

    {#if model.steps.length}
      <h2 class="gh" style="--t:var(--icon)"><span>{t.studioNext.toUpperCase()}</span><i></i></h2>
      <div class="chips">{#each model.steps as s (s.send)}<button class="act" onclick={() => onStep(s)}>{s.label}</button>{/each}</div>
    {/if}

    {#if model.recap.length}
      <h2 class="gh" style="--t:var(--text2)"><span>{t.studioRecap.toUpperCase()}</span><i></i></h2>
      {#each model.recap as o (`${o.project}\n${o.at}`)}
        <div class="out">
          <span class="col"><small>{when(o.at)} · {o.names.join(' · ')}</small><span>{o.short}</span></span>
          {#if o.short.length >= TTS_MIN}
            <button class="ib" aria-label={t.dockListen} onclick={() => onSpeak(o.short)}><svg viewBox="0 0 24 24" width="20" height="20"><path d="M8 5.5v13l10.5-6.5z" fill="currentColor" /></svg></button>
          {/if}
        </div>
      {/each}
    {/if}
  {/if}
</div>

<style>
  /* Scorre da sé, come la lista della home: sotto c'è il dock della master. */
  .studio { height: 100%; overflow-y: auto; padding: 0 16px 24px; display: flex; flex-direction: column; gap: 8px; }
  .gh { display: flex; align-items: center; gap: 8px; margin: 12px 4px 0; font: 500 12px/1.4 var(--mono); letter-spacing: .08em; color: var(--t); }
  .gh i { flex: 1; height: 1px; background: color-mix(in srgb, var(--t) 25%, transparent); }
  .note { color: var(--text2); font-size: 14px; padding: 8px 4px; }
  .dl { display: flex; gap: 12px; align-items: baseline; padding: 6px 4px; font-size: 15px; }
  .dl b { flex: none; font: 600 14px var(--mono); color: var(--text2); }
  .dl.due b { color: var(--b-warn); }
  .case { display: flex; flex-direction: column; gap: 2px; width: 100%; padding: 12px 14px; border-radius: 16px; background: var(--low); color: var(--text); text-align: left; }
  .case .name { font-size: 16px; font-weight: 600; }
  .case small { font-size: 13.5px; color: var(--text2); }
  .case .live { color: var(--good); }
  .launch { display: flex; justify-content: flex-end; padding: 0 4px; }
  .key { border-radius: 999px; padding: 10px 20px; background: var(--primary); color: var(--on-primary); font-size: 15px; }
  .fold { display: flex; justify-content: space-between; align-items: center; margin-top: 6px; padding: 12px 4px; border-top: 1px solid var(--line); color: var(--text2); font-size: 15px; text-align: left; }
  .chips { display: flex; flex-wrap: wrap; gap: 8px; }
  .act { border-radius: 999px; padding: 10px 16px; background: var(--low); color: var(--text); font-size: 15px; text-align: left; }
  .out { display: flex; align-items: center; gap: 8px; padding: 6px 4px; }
  .out .col { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; font-size: 15px; }
  .out small { font-size: 13px; color: var(--text2); }
  .ib { flex: none; width: 44px; height: 44px; border-radius: 22px; display: grid; place-items: center; color: var(--text); }
  .ib:hover { background: var(--surface); }
</style>
