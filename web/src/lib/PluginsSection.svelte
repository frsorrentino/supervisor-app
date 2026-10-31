<script lang="ts">
  import type { PluginsPage, State } from './contract'
  import { account, accountsWithUpdates, pending, updateArg } from './plugins'
  import { t } from './t'
  import { locale } from './lang'
  // La sezione «Plugin e skill» (contratto 1.95, mockup approvato da Franz il 29/10 alle 10:39): per account i plugin da
  // aggiornare con il loro «Aggiorna», gli altri dietro «Mostra»; in fondo «Controlla ora» e «Aggiorna tutto» con conferma.
  // L'elenco si chiede al relay quando la sezione si apre; il riassunto nello stato basta per il titolo chiuso, che dice
  // sempre un numero o «nessun aggiornamento», come Sistema (app-home-riordino-29-10).
  let { st, load, run, rowDrag = {} }: {
    /** La pressione lunga sulla riga sposta il blocco in home (Franz 31/10 14:27). */
    rowDrag?: Record<string, unknown>
    st: State
    load: () => Promise<PluginsPage | null>
    run: (op: 'plugin_check' | 'plugin_update' | 'plugin_update_all', arg: string | null) => Promise<{ ok: boolean; text: string } | null>
  } = $props()
  const KEY = 'home-plugins-open'
  let open = $state((() => { try { return localStorage.getItem(KEY) === '1' } catch { return false } })())
  let page = $state<PluginsPage | null>(null)
  let loading = $state(false)
  let shown = $state<Record<string, boolean>>({})
  let busy = $state<Record<string, boolean>>({})
  let notes = $state<Record<string, { ok: boolean; text: string }>>({})
  let asking = $state(false)
  const count = $derived(pending(st))
  async function refresh() { loading = true; page = await load(); loading = false }
  function toggle() {
    open = !open
    try { localStorage.setItem(KEY, open ? '1' : '0') } catch { /* resta per questa visita */ }
    if (open && !page) refresh()
  }
  $effect(() => { if (open && !page && !loading) refresh() })
  async function act(key: string, op: 'plugin_check' | 'plugin_update' | 'plugin_update_all', arg: string | null, note: string) {
    busy = { ...busy, [key]: true }
    const r = await run(op, arg)
    busy = { ...busy, [key]: false }
    if (r) notes = { ...notes, [note]: { ok: r.ok, text: r.text } }
    if (r?.ok && op !== 'plugin_check') refresh()
  }
  async function updateAll() {
    asking = false
    for (const a of accountsWithUpdates(st)) await act(`all:${a}`, 'plugin_update_all', a, a)
  }
  const hm = (s: number) => new Intl.DateTimeFormat(locale, { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date(s * 1000))
  const checked = $derived(page?.checked_at ?? st.plugins?.checked_at ?? null)
</script>

{#if count != null}
  <!-- Una riga di «Altro» nella home (stile «Studio e Diretta», 30/10): l'icona nel binario, il numero in vista, la freccina. -->
  <button class="prow" class:draggable={!!rowDrag.onpointerdown} {...rowDrag} aria-expanded={open} onclick={toggle}>
    <span class="wic"><span class="ms">extension</span></span><span class="pt">{t.homePlugins}</span>
    {#if !open}<span class="pv">{count ? t.pluginsToUpdate(count) : t.pluginsUpToDate}</span>{/if}
    <span class="ms tog">{open ? 'expand_less' : 'expand_more'}</span>
  </button>
  {#if open}
    <div class="plugins" data-kpi="plugins_state">
      {#if !page}<p class="dim">{loading ? t.pluginsLoading : t.pluginsNone}</p>{/if}
      {#each page?.accounts ?? [] as raw (raw.account)}
        {@const a = account(raw)}
        <div class="card pl">
          <div class="acct"><b>{a.account}</b><span class="meta">{t.pluginsMeta(raw.plugins.length, a.skills)}</span></div>
          {#each a.updates as p (p.id)}
            <div class="row">
              <span class="nm"><b>{p.name}</b><span class="v">{p.installed ?? '?'} → <span class="new">{p.latest}</span></span></span>
              {#if p.scope === 'user'}<button class="tonal mini" disabled={busy[p.id]} onclick={() => act(p.id, 'plugin_update', updateArg(a.account, p), a.account)}>{busy[p.id] ? t.pluginsUpdating : t.pluginsUpdate}</button>{/if}
            </div>
          {/each}
          {#if shown[a.account]}
            {#each a.rest as p (p.id)}
              <div class="row"><span class="nm"><b>{p.name}</b><span class="v">{p.installed ?? '?'}{#if p.update === false} · <span class="ok">{t.pluginsLatest}</span>{:else if p.update == null} · {t.pluginsUnknown}{/if}</span></span></div>
            {/each}
            {#if a.skillNames.length}<p class="skills">{t.pluginsSkills}: {a.skillNames.join(' · ')}</p>{/if}
          {/if}
          {#if a.rest.length || a.skills}
            <!-- Il numero sta già accanto al nome dell'account: qui solo il tasto (Franz, 29/10 20:32: «+ altri…» lo ripeteva). -->
            <div class="foot"><span></span><button class="text" onclick={() => (shown = { ...shown, [a.account]: !shown[a.account] })}>{shown[a.account] ? t.pluginsHide : t.pluginsShow}</button></div>
          {/if}
          {#if notes[a.account]}<p class="note" class:ko={!notes[a.account].ok}>{notes[a.account].text}{#if notes[a.account].ok} · {t.pluginsNewSessions}{/if}</p>{/if}
        </div>
      {/each}
      {#if asking}
        <div class="confirm" role="group" aria-label={t.pluginsUpdateAll(count ?? 0)}>
          <span>{t.pluginsConfirm(count ?? 0)}</span>
          <button class="keep" onclick={() => (asking = false)}>{t.pluginsKeep}</button>
          <button class="danger" onclick={updateAll}>{t.pluginsUpdateAll(count ?? 0)}</button>
        </div>
      {:else}
        <div class="bar">
          <span>{checked ? t.pluginsChecked(hm(checked)) : t.pluginsNeverChecked}</span>
          <button class="text" disabled={busy.check} onclick={() => act('check', 'plugin_check', null, '_check')}>{t.pluginsCheck}</button>
          {#if count}<button class="filled" onclick={() => (asking = true)}>{t.pluginsUpdateAll(count)}</button>{/if}
        </div>
      {/if}
      {#if notes._check}<p class="note" class:ko={!notes._check.ok}>{notes._check.text}</p>{/if}
    </div>
  {/if}
{/if}

<style>
  .prow { display: flex; align-items: center; gap: 8px; width: 100%; min-height: 52px; padding: 10px 12px 10px 0; border-top: 1px solid var(--line); text-align: left; color: var(--tx); }
  .prow:hover { background: var(--s2); }
  .prow .wic { width: 52px; flex: none; display: flex; justify-content: center; color: var(--tx2); }
  .prow .pt { flex: 1; min-width: 0; font: 600 17px/1.2 var(--display); }
  .prow .pv { font: 400 12.5px var(--mono); color: var(--tx2); white-space: nowrap; }
  .prow .tog { color: var(--tx3); }
  .plugins { display: flex; flex-direction: column; gap: 8px; padding: 2px 12px 12px 60px; }
  .dim { color: var(--text2); font-size: 14px; padding: 0 4px; }
  .card.pl { background: var(--s2); border-radius: 14px; padding: 12px 14px; display: flex; flex-direction: column; gap: 6px; }
  .acct { display: flex; align-items: baseline; gap: 8px; }
  .acct b { flex: 1; font-weight: 600; font-size: 16px; min-width: 0; overflow-wrap: anywhere; }
  .meta { font: 12px var(--mono); color: var(--text2); flex: none; }
  .row { display: flex; align-items: center; gap: 10px; padding: 6px 0; border-top: 1px solid var(--line); }
  .nm { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 1px; }
  .nm b { font-weight: 500; overflow-wrap: anywhere; }
  .v { font: 12px var(--mono); color: var(--text2); overflow-wrap: anywhere; }
  .v .new { color: var(--b-warn); } .v .ok { color: var(--good); }
  .tonal { background: var(--high); color: var(--text); border-radius: 999px; padding: 8px 14px; font: 500 14px inherit; white-space: nowrap; flex: none; }
  .filled { background: var(--primary); color: var(--on-primary); border-radius: 999px; padding: 9px 16px; font-weight: 500; white-space: nowrap; }
  .text { color: var(--icon); font-weight: 500; padding: 8px 6px; white-space: nowrap; }
  .foot, .bar { display: flex; align-items: center; gap: 8px; }
  .foot span, .bar span { flex: 1; min-width: 0; font: 12px var(--mono); color: var(--text2); }
  .bar { padding: 2px 4px 0; flex-wrap: wrap; }
  .skills { font-size: 13px; color: var(--text2); overflow-wrap: anywhere; }
  .note { font-size: 13.5px; background: color-mix(in srgb, var(--good) 12%, var(--low)); border-radius: 12px; padding: 8px 10px; overflow-wrap: anywhere; }
  .note.ko { background: color-mix(in srgb, var(--b-warn) 14%, var(--low)); }
  .confirm { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; font-size: 13.5px; color: var(--text2); padding: 0 4px; }
  .confirm span { flex: 1 1 100%; }
  .confirm button { border-radius: 999px; padding: 8px 14px; font-weight: 500; }
  .confirm .keep { color: var(--icon); }
  .confirm .danger { background: var(--primary); color: var(--on-primary); }
  .prow.draggable { user-select: none; -webkit-user-select: none; -webkit-touch-callout: none; touch-action: pan-y; }
</style>
