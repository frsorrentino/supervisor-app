<script lang="ts">
  import type { Approval } from './contract'
  import { choices } from './approvals'
  import { t } from './t'
  import HoldButton from './HoldButton.svelte'
  import { holdMs, holds, LEAVE_MS } from './tieniPremuto'
  import { HOLD_ICON } from './icons'
  import ReadButton from './ReadButton.svelte'
  // Franz 24/10 09:25: la finestra di conferma di un ok: titolo corto, la domanda una volta sola con il tasto ▶, le opzioni
  // con quella scelta evidenziata, la nota facoltativa, poi Annulla e la conferma.
  let { a, choice = null, onSpeak = null, onCancel, onConfirm }: {
    a: Approval; choice?: string | null; onSpeak?: ((text: string) => void) | null; onCancel: () => void; onConfirm: (note: string) => void
  } = $props()
  let note = $state('')
  const question = $derived([a.title, a.what, a.where ? t.approveWhere(a.where) : ''].filter(x => x && x.trim()).join('. '))
  const opts = $derived(choices(a))
  // app-tieni-premuto-28-11: l'ok che pubblica si tiene premuto 1,5 s (tieniPremuto.holds); ogni altro ok resta un tocco col finale del
  // cerchio verde, e la finestra si chiude a spunta disegnata.
  const hold = $derived(holds(a))
</script>

<h3 class="ah">{choice ? t.chooseShort(choice) : a.deploy ? t.approveProdTitle : t.approveShort}</h3>
<div class="aq">
  <p>{question}</p>
  <!-- app-play-lettura-ovunque-27-11: il ▶ di tutta l'app, «Leggi ad alta voce» e ■ mentre legge; c'è quando chi apre la finestra legge. -->
  {#if onSpeak}<ReadButton text={question} />{/if}
</div>
{#if opts.length}<div class="aopts">{#each opts as o (o)}<span class:on={o === choice}>{o}</span>{/each}</div>{/if}
<label class="alab">{t.approveNote}<input bind:value={note} placeholder="ok" /></label>
<div class="abtns" class:col={hold}>
  <button class="acancel" onclick={onCancel}>{t.cancel}</button>
  {#if hold}<HoldButton tone="amber" ms={holdMs('pubblica')} icon={HOLD_ICON.rocket} label={t.holdPublish} aria={`${t.holdPublish}: ${choice ?? t.approve}`} said={t.holdSaidPublish} testid="approva-pubblica" onDone={() => onConfirm(note)} />
  {:else}<HoldButton mode="tap" size="key" tone="amber" label={choice ?? t.approve} said={t.holdSaidOk} testid="approva" onDone={() => setTimeout(() => onConfirm(note), LEAVE_MS)} />{/if}
</div>

<style>
  .ah { margin: 0; font-size: 20px; font-weight: 600; }
  .aq { display: flex; align-items: flex-start; gap: 8px; margin-top: 10px; }
  .aq p { flex: 1; margin: 0; font-size: 15px; line-height: 1.45; color: var(--text); overflow-wrap: anywhere; }
  .aopts { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 12px; }
  .aopts span { white-space: nowrap; padding: 6px 12px; border-radius: 999px; background: var(--high); color: var(--text2); font-size: 14px; font-weight: 500; }
  .aopts span.on { background: var(--primary); color: var(--on-primary); }
  .alab { display: flex; flex-direction: column; gap: 6px; font-size: 12.5px; color: var(--text2); margin-top: 14px; }
  .alab input { padding: 10px 12px; border-radius: 12px; border: 1px solid var(--line); background: var(--low); color: var(--text); font-size: 15px; }
  .abtns { display: flex; justify-content: flex-end; gap: 8px; margin-top: 18px; }
  .acancel { padding: 10px 16px; border-radius: 999px; color: var(--text2); background: none; }
  /* Il tasto da tenere premuto prende la riga intera, sotto Annulla. */
  .abtns.col { flex-direction: column; align-items: stretch; }
  .abtns.col .acancel { align-self: flex-end; }
</style>
