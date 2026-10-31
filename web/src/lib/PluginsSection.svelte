<script lang="ts">
  import type { Snippet } from 'svelte'
  import type { PluginsPage, State } from './contract'
  import { account, accountsWithUpdates, pending, updateArg } from './plugins'
  import { pluginsRun } from './pluginsRun'
  import { t } from './t'
  import { locale } from './lang'
  // La sezione «Plugin e skill» (contratto 1.95, mockup approvato da Franz il 29/10 alle 10:39): per account i plugin da
  // aggiornare con il loro «Aggiorna», gli altri dietro «Mostra»; in fondo «Controlla ora» e «Aggiorna tutto» con conferma.
  // L'elenco si chiede al relay quando la sezione si apre; il riassunto nello stato basta per il titolo chiuso, che dice
  // sempre un numero o «nessun aggiornamento», come Sistema (app-home-riordino-29-10).
  let { st, load, run, head }: {
    /** La testata la disegna la home: è la stessa di tutte le voci (Franz 31/10 17:04). */
    head: Snippet<[boolean, () => void, string]>
    st: State
    load: () => Promise<PluginsPage | null>
    run: (op: 'plugin_check' | 'plugin_update' | 'plugin_update_all', arg: string | null) => Promise<{ ok: boolean; text: string } | null>
  } = $props()
  const KEY = 'home-plugins-open'
  let open = $state((() => { try { return localStorage.getItem(KEY) === '1' } catch { return false } })())
  // Comandi, stato ed esito stanno in pluginsRun: la seconda pressione non manda niente, la lista si rilegge all'esito.
  const runner = pluginsRun(load, run)
  let rs = $state(runner.get())
  runner.sub(v => { rs = v })
  const page = $derived(rs.page)
  const loading = $derived(rs.loading)
  const busy = $derived(rs.busy)
  const notes = $derived(rs.notes)
  let shown = $state<Record<string, boolean>>({})
  let asking = $state(false)
  // Il riassunto dello stato arriva con la push dopo: appena c'è l'elenco, contano i plugin dell'elenco.
  const count = $derived(page && (page.checked_at ?? 0) >= (st.plugins?.checked_at ?? 0) ? page.accounts.reduce((n, a) => n + a.plugins.filter(p => p.update === true).length, 0) : pending(st))
  const toUpdate = $derived(page && (page.checked_at ?? 0) >= (st.plugins?.checked_at ?? 0) ? page.accounts.filter(a => a.plugins.some(p => p.update === true)).map(a => a.account) : accountsWithUpdates(st))
  const refresh = () => runner.refresh()
  function toggle() {
    open = !open
    try { localStorage.setItem(KEY, open ? '1' : '0') } catch { /* resta per questa visita */ }
    if (open && !page) refresh()
  }
  $effect(() => { if (open && !page && !loading) refresh() })
  const act = runner.act
  function updateAll() { asking = false; runner.updateAll(toUpdate) }
  const hm = (s: number) => new Intl.DateTimeFormat(locale, { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date(s * 1000))
  const checked = $derived(page?.checked_at ?? st.plugins?.checked_at ?? null)
</script>

{#if count != null}
  <!-- Una riga di «Altro» nella home (stile «Studio e Diretta», 30/10): l'icona nel binario, il numero in vista, la freccina. -->
  {@render head(open, toggle, count ? t.pluginsToUpdate(count) : t.pluginsUpToDate)}
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
          {#if notes[a.account]}<p class="note" class:ko={!notes[a.account].ok}>{notes[a.account].text ?? t.pluginsNoAnswer}{#if notes[a.account].ok} · {t.pluginsNewSessions}{/if}</p>{/if}
        </div>
      {/each}
      {#if rs.all}
        <p class="note" role="status" data-testid="plugins_updating_all">{t.pluginsUpdatingAll}</p>
      {:else if asking}
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
</style>
