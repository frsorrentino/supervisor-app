<script lang="ts">
  import { locale } from './lang'
  import type { Session } from './contract'
  import { closeStateOf } from './masterService'
  import { t } from './t'
  import { getContext } from 'svelte'
  import { close as closeOk } from './vociAssenti'
  // La domanda prima di chiudere una sessione, uguale per il «Chiudi» della pulizia, «Chiudi la sessione» del menu e /exit
  // scritto nel campo (Franz, 06/10 10:57, mockup approvato 12:25): il nome nel titolo e nel tasto, lo stato, cosa succede.
  // Contratto 1.155 (app-chiudi-salva-19-11, Franz 19/11 15:16): con `canSave` due tasti, «Salva e chiudi» (il pieno) e
  // «Chiudi senza salvare»; senza, il «Chiudi <nome>» di prima.
  // app-chiudi-scheda-20-11 (Franz 20/11 19:08, bozza C senza tasto bianco): non più al centro ma la carta in fondo alla pagina,
  // dove sta il campo di scrittura, senza velo; prima di chiudere dice se è aperta anche in una finestra sul PC. Come CloseCard.kt.
  let { onConfirm, canSave = false }: { onConfirm: (name: string, save: boolean) => void; canSave?: boolean } = $props()
  let dlg: HTMLDialogElement | undefined = $state()
  let s = $state<Session | null>(null)
  // Contratto 1.161 (issue #4): prima di offrire Chiudi si legge `close_preview`; su una sessione in uso al PC o con un lavoro in
  // sottofondo restano il motivo e «Lascia aperta».
  const preview = getContext<((names: string[]) => Promise<{ sessions: { name: string; blocked_by?: string | null }[] } | null>) | undefined>('close-preview')
  let blocked = $state<string | null>(null)
  export function ask(session: Session) {
    s = session; blocked = null; dlg?.showModal()
    const name = session.name
    void preview?.([name]).then(p => { if (s?.name === name) blocked = p?.sessions.find(x => x.name === name)?.blocked_by ?? null })
  }
  const can = $derived(!!s && closeOk(s, blocked))
  const go = (save: boolean) => { const n = s!.name; dlg?.close(); onConfirm(n, save) }
  const hm = (at: number) => new Date(at * 1000).toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })
  const line = $derived.by(() => {
    if (!s) return ''
    const c = closeStateOf(s)
    if (c.kind === 'working') return t.closeWorking(s.name)
    if (c.kind === 'duplicate') return t.closeDuplicate(s.name, c.of ?? '')
    if (c.kind === 'finished') return c.at ? t.closeFinishedAt(s.name, hm(c.at)) : t.closeFinished(s.name)
    return t.closeStill(s.name)
  })
</script>

<dialog bind:this={dlg} onclick={(e) => e.target === e.currentTarget && dlg?.close()}>
  {#if s}
    <h3>{t.closeTitle(s.name)}</h3>
    <p>{line}</p>
    {#if !can}
      <div class="ck-attached" data-testid="close-blocked"><svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="12" rx="2" /><path d="M8 20h8M12 16v4" /></svg><span><b>{s.attached ? t.closeBlockedAttached : t.closeBlockedBackground}</b><small>{t.closeBlockedLine}</small></span></div>
      <div class="ck-keys"><button class="ck" onclick={() => dlg?.close()}>{t.closeKeep}</button></div>
    {:else}
    <ul>{#each canSave ? t.closeWhatSave : t.closeWhat as w}<li>{w}</li>{/each}</ul>
    <div class="ck-keys">
      {#if canSave}<button class="ck main" onclick={() => go(true)}>{t.closeSave}</button>{/if}
      <div class="ck-pair">
        <button class="ck" onclick={() => dlg?.close()}>{t.closeKeep}</button>
        <button class="ck red" onclick={() => go(false)}>{canSave ? t.closeNoSave : t.closeOk(s.name)}</button>
      </div>
    </div>
    {/if}
  {/if}
</dialog>

<style>
  /* In fondo, al posto del campo di scrittura, sopra la barra dei gesti; la pagina resta leggibile, senza velo. */
  dialog { margin: auto auto max(12px, env(safe-area-inset-bottom)); border: 1px solid var(--line); color: var(--text); background: var(--s2); padding: 18px 18px 14px; width: min(560px, calc(100vw - 24px)); max-width: none; border-radius: 20px; box-shadow: 0 -10px 28px rgb(0 0 0 / .55); }
  dialog::backdrop { background: transparent; }
  h3 { font-size: 21px; margin: 0 0 4px; }
  p { color: var(--text2); font-size: 14px; margin: 0 0 4px; }
  ul { margin: 10px 0 0; padding-left: 18px; color: var(--text2); font-size: 14px; }
  li { margin: 3px 0; }
  @media (prefers-reduced-motion: no-preference) { dialog[open] { animation: up 250ms cubic-bezier(.23, 1, .32, 1); } }
  @keyframes up { from { opacity: 0; translate: 0 16px; } }
</style>
