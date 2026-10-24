<script lang="ts">
  import type { Approval } from './contract'
  import { choices } from './approvals'
  import { t } from './t'
  // Franz 24/10 09:25: la finestra di conferma di un ok: titolo corto, la domanda una volta sola con il tasto ▶, le opzioni
  // con quella scelta evidenziata, la nota facoltativa, poi Annulla e la conferma.
  let { a, choice = null, onSpeak = null, onCancel, onConfirm }: {
    a: Approval; choice?: string | null; onSpeak?: ((text: string) => void) | null; onCancel: () => void; onConfirm: (note: string) => void
  } = $props()
  let note = $state('')
  const question = $derived([a.title, a.what, a.where ? t.approveWhere(a.where) : ''].filter(x => x && x.trim()).join('. '))
  const opts = $derived(choices(a))
</script>

<h3 class="ah">{choice ? t.chooseShort(choice) : a.deploy ? t.approveProdTitle : t.approveShort}</h3>
<div class="aq">
  <p>{question}</p>
  {#if onSpeak}<button class="play" aria-label={t.listen} title={t.listen} onclick={() => onSpeak?.(question)}><svg viewBox="0 0 24 24" width="20" height="20"><path d="M8 5.5v13l10.5-6.5z" fill="currentColor" /></svg></button>{/if}
</div>
{#if opts.length}<div class="aopts">{#each opts as o (o)}<span class:on={o === choice}>{o}</span>{/each}</div>{/if}
<label class="alab">{t.approveNote}<input bind:value={note} placeholder="ok" /></label>
<div class="abtns">
  <button class="acancel" onclick={onCancel}>{t.cancel}</button>
  <button class="aok" onclick={() => onConfirm(note)}>{choice ?? t.approve}</button>
</div>

<style>
  .ah { margin: 0; font-size: 20px; font-weight: 600; }
  .aq { display: flex; align-items: flex-start; gap: 8px; margin-top: 10px; }
  .aq p { flex: 1; margin: 0; font-size: 15px; line-height: 1.45; color: var(--text); overflow-wrap: anywhere; }
  .play { flex: none; width: 40px; height: 40px; border-radius: 50%; display: grid; place-items: center; color: var(--icon); background: var(--high); }
  .aopts { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 12px; }
  .aopts span { white-space: nowrap; padding: 6px 12px; border-radius: 999px; background: var(--high); color: var(--text2); font-size: 14px; font-weight: 500; }
  .aopts span.on { background: var(--primary); color: var(--on-primary); }
  .alab { display: flex; flex-direction: column; gap: 6px; font-size: 12px; color: var(--text2); margin-top: 14px; }
  .alab input { padding: 10px 12px; border-radius: 12px; border: 1px solid var(--line); background: var(--low); color: var(--text); font-size: 15px; }
  .abtns { display: flex; justify-content: flex-end; gap: 8px; margin-top: 18px; }
  .acancel { padding: 10px 16px; border-radius: 999px; color: var(--text2); background: none; }
  .aok { padding: 10px 20px; border-radius: 999px; background: var(--primary); color: var(--on-primary); font-weight: 500; }
</style>
