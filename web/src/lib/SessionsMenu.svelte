<script lang="ts">
  import type { Session, State } from './contract'
  import { of as menuOf } from './sessionsMenu'
  import { since } from './durations'
  import { stop } from './strongAlert'
  import { t } from './t'
  import Badge from './Badge.svelte'
  import StateGlyph from './StateGlyph.svelte'
  import { glyphOf } from './rail'
  import { personal } from './badge'

  // Il menu delle sessioni in alto (SessionMenu di AppShell.kt): il nome della sessione aperta con ▾ apre «Vai a», con la
  // home in testa, le sessioni vive nei gruppi e le chiuse contate, ognuna con «Riapri».
  let { st, s, now, onGo, onReopen = null }: {
    st: State; s: Session; now: number; onGo: (name: string | null) => void; onReopen?: ((name: string) => void) | null
  } = $props()
  const model = $derived(menuOf(st.sessions))
  let dlg: HTMLDialogElement | undefined = $state()
  let closedDlg: HTMLDialogElement | undefined = $state()
  const tone: Record<string, string> = { waiting: 'var(--wait)', working: 'var(--busy)', still: 'var(--tx2)' }
  function status(x: Session, g: string) {
    if (g === 'waiting') return t.menuRowWaiting(since(x.question?.asked_at ?? stop(x)?.at ?? x.since, now))
    if (g === 'working') return t.menuRowWorking(since(x.turn_started ?? x.since, now))
    return t.menuRowStill(since(x.since, now))
  }
  const go = (n: string | null) => { dlg?.close(); onGo(n) }
  const closed = $derived(st.sessions.filter(x => x.state === 'gone'))
  const backdrop = (e: MouseEvent) => { if (e.target === e.currentTarget) (e.currentTarget as HTMLDialogElement).close() }
</script>

<button class="who" onclick={() => dlg?.showModal()} aria-haspopup="dialog">
  <Badge {s} size={22} /><b>{s.name}</b>
  <svg viewBox="0 0 24 24" width="20" height="20"><path d="M7 10l5 5 5-5z" fill="var(--text2)" /></svg>
</button>

<dialog bind:this={dlg} class="panel" data-testid="vai-a-v3" onclick={backdrop} aria-label={t.menuGoto}>
  <div class="ph">{t.menuGoto}</div>
  <button class="row" onclick={() => go(null)}>
    <span class="home"><svg viewBox="0 0 24 24" width="22" height="22"><path d="M4 11l8-7 8 7v9h-5v-6H9v6H4z" fill="none" stroke="var(--tx2)" stroke-width="1.8" stroke-linejoin="round" /></svg></span>
    <span class="col"><b>{t.summaryTitle}</b><small>{t.menuHomeSub}</small></span>
  </button>
  {#each model.groups as [g, list] (g)}
    <div class="gh" style:color={tone[g]}>{(t.summary[g] ?? t.summary.still)(list.length).toUpperCase()}</div>
    {#each list as x (x.name)}
      <button class="row" class:on={x.name === s.name} onclick={() => go(x.name)}>
        <span class="glyph"><StateGlyph kind={glyphOf(x)} ctx={x.context} square={!personal(x.account, x.account_kind)} /></span>
        <span class="col"><b>{x.name}</b><small style:color={g === 'waiting' ? 'var(--wait)' : null}>{status(x, g)}</small></span>
        {#if x.context != null}<span class="ctx mono">{t.ctx(x.context)}</span>{/if}
        {#if x.name === s.name}<svg viewBox="0 0 24 24" width="18" height="18"><path d="M5 12l5 5 9-10" fill="none" stroke="var(--icon)" stroke-width="2.4" stroke-linecap="round" /></svg>{/if}
      </button>
    {/each}
  {/each}
  {#if model.closed}
    <hr />
    <button class="row" onclick={() => { dlg?.close(); closedDlg?.showModal() }}>
      <span class="glyph"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--tx2)" stroke-width="1.8" stroke-linejoin="round"><path d="M4 5h16v4H4zM5 9h14v10H5zM10 13h4" /></svg></span>
      <span class="col"><span class="dim">{t.closedCat(model.closed)}</span></span>
      <svg viewBox="0 0 24 24" width="20" height="20"><path d="M10 7l5 5-5 5" fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round" /></svg>
    </button>
  {/if}
</dialog>

<dialog bind:this={closedDlg} class="sheet" onclick={backdrop}>
  <h2>{t.closedCat(closed.length)}</h2>
  {#each closed as c (c.name)}
    <div class="crow"><Badge s={c} size={24} /><b>{c.name}</b>
      {#if onReopen}<button class="re" onclick={() => { closedDlg?.close(); onReopen(c.name) }}>{t.reopen}</button>{/if}</div>
  {/each}
</dialog>

<style>
  .who { flex: 1; min-width: 0; display: flex; align-items: center; gap: 8px; padding: 8px 10px; border-radius: 999px; text-align: left; }
  .who b { font-size: 17px; font-weight: 600; white-space: nowrap; overflow: hidden; min-width: 0; }
  .who:hover { background: var(--surface); }
  dialog.panel { position: fixed; inset: 52px auto auto 8px; margin: 0; border: 1px solid var(--line); color: var(--text); background: var(--surface); padding: 8px 0; width: min(380px, calc(100vw - 16px)); max-height: calc(100dvh - 68px); border-radius: 28px; box-shadow: 0 12px 40px rgb(0 0 0 / .5); }
  dialog.panel::backdrop { background: rgb(0 0 0 / .55); }
  .ph { padding: 6px 20px 8px; font-size: 14px; color: var(--text2); }
  .row { width: 100%; display: flex; align-items: center; gap: 14px; padding: 9px 20px; text-align: left; }
  .row:hover, .row.on { background: color-mix(in srgb, var(--primary) 10%, transparent); }
  .home { width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center; background: color-mix(in srgb, var(--advice) 25%, transparent); flex: none; }
  .col { flex: 1; min-width: 0; display: flex; flex-direction: column; }
  .col b { font-size: 16px; font-weight: 500; white-space: nowrap; overflow: hidden; }
  .col small { font-size: 13px; color: var(--text2); white-space: nowrap; overflow: hidden; }
  .dim { color: var(--text2); font-size: 16px; }
  .ctx { font-size: 12px; color: var(--text2); }
  .gh { padding: 10px 20px 2px; font: 500 12px/1.4 var(--mono); letter-spacing: .08em; }
  hr { border: 0; height: 1px; background: var(--line); margin: 4px 0; }
  dialog.sheet { margin: auto auto 0; border: 0; color: var(--text); background: var(--surface); padding: 20px 0 16px; width: min(600px, 100vw); max-height: 80vh; border-radius: 28px 28px 0 0; }
  dialog.sheet::backdrop { background: rgb(0 0 0 / .55); }
  dialog.sheet h2 { font-size: 20px; font-weight: 600; padding: 0 20px 8px; }
  .crow { display: flex; align-items: center; gap: 12px; padding: 10px 20px; }
  .crow b { flex: 1; font-weight: 500; }
  .re { padding: 9px 16px; border-radius: 999px; background: var(--high); color: var(--primary); font-size: 14px; font-weight: 500; }
  /* «Vai a» nella forma della v3 (app-menu-v3-30-10): glifi a contorno come in home, icone grigie nel binario, nessun cerchio. */
  dialog.panel { padding: 0; }
  .ph { padding: 14px 16px; border-bottom: 1px solid var(--line); color: var(--tx2); }
  .row { gap: 8px; min-height: 58px; padding: 9px 12px 9px 0; border-top: 1px solid var(--line); }
  .ph + .row { border-top: 0; }
  .row:hover, .row.on { background: var(--s2); }
  .home, .glyph { width: 52px; height: auto; flex: none; display: flex; justify-content: center; background: none; border-radius: 0; }
  .col b { font: 600 17px/1.2 var(--display); white-space: normal; overflow-wrap: anywhere; }
  .col small { font-size: 14px; white-space: normal; }
  .dim { font: 600 17px var(--display); color: var(--tx); }
  .ctx { font: 400 12.5px var(--mono); color: var(--tx2); }
  .gh { padding: 8px 0 2px 60px; font: 400 14px var(--sans); letter-spacing: 0; border-top: 1px solid var(--line); }
  .gh + .row { border-top: 0; }
  hr { display: none; }
  dialog.sheet { background: var(--s1); }
  .re { background: var(--s3); color: var(--tx); font: 600 15px var(--display); }
</style>
