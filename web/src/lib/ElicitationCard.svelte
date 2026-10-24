<script lang="ts">
  import type { ElicitField, Elicitation } from './contract'
  import { initial, problem, ready, type Problem, type Values } from './elicit'
  import { t } from './t'
  import { untrack } from 'svelte'

  // Contratto 1.83 (lavoro mcp-elicitation-lavoro-24-10, approvato da Franz), come ElicitationCard.kt: un server MCP chiede
  // qualcosa a metà di uno strumento. In cima alla home, perché dopo `expires_at` (120 s) la domanda torna al terminale: il
  // server, la sessione e il tempo che resta, la domanda, il modulo e «Rifiuta» / «Invia». Il modo url ha il link e «Apri»:
  // si risponde al terminale. Niente si riempie da solo: solo il `default` che il server stesso propone.
  let { e, now, onAnswer }: { e: Elicitation; now: number; onAnswer: (e: Elicitation, action: 'accept' | 'decline', values: Values) => void } = $props()
  // I valori di partenza una volta sola: la scheda è legata all'id della domanda (`#each … (e.id)` in Home).
  let values = $state<Values>(untrack(() => Object.fromEntries((e.fields ?? []).map(f => [f.key, initial(f)]))))
  const left = $derived(Math.max(0, e.expires_at - now))
  const clock = $derived(`${Math.floor(left / 60)}:${String(left % 60).padStart(2, '0')}`)
  const can = $derived(ready(e, values))
  const opts = (f: ElicitField) => (f.kind === 'boolean' ? [{ value: 'true', label: t.elicitYes }, { value: 'false', label: t.elicitNo }] : f.options ?? [])
  const say = (p: Problem) => p.kind === 'required' ? t.elicitRequired : p.kind === 'not_number' ? t.elicitNotNumber : p.kind === 'not_whole' ? t.elicitNotWhole
    : p.kind === 'not_option' ? t.elicitNotOption : p.kind === 'length' ? t.elicitLength(p.min, p.max) : t.elicitRange(p.min, p.max)
  const input = (f: ElicitField) => f.kind === 'integer' || f.kind === 'number' ? 'decimal' : f.format === 'email' ? 'email' : f.format === 'uri' ? 'url' : 'text'
</script>

<div class="card elicit" role="group" aria-label={e.server}>
  <div class="head">
    <svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><path fill="var(--wait)" d="M20.5 11H19V7a2 2 0 0 0-2-2h-4V3.5a2.5 2.5 0 0 0-5 0V5H4a2 2 0 0 0-2 2v3.8h1.5a2.7 2.7 0 0 1 0 5.4H2V20a2 2 0 0 0 2 2h3.8v-1.5a2.7 2.7 0 0 1 5.4 0V22H17a2 2 0 0 0 2-2v-4h1.5a2.5 2.5 0 0 0 0-5z" /></svg>
    <span class="col"><b>{e.server}</b><small class="mono">{t.elicitMeta(e.session, clock)}</small></span>
  </div>
  <p class="q">{e.message}</p>
  {#if e.mode === 'url'}
    {#if e.url}<small class="mono link">{e.url}</small>{/if}
    <p class="note">{t.elicitLinkNote}</p>
    {#if e.url}<a class="btn filled" href={e.url} target="_blank" rel="noopener noreferrer">{t.elicitOpen}</a>{/if}
  {:else}
    {#each e.fields ?? [] as f (f.key)}
      {@const title = f.required ? `${f.title} *` : f.title}
      {#if f.kind === 'choice' || f.kind === 'boolean'}
        <div class="field">
          <span class="lab">{title}</span>
          {#if f.description}<small>{f.description}</small>{/if}
          <div class="opts">
            <!-- Un secondo tocco sull'opzione scelta la toglie, per un campo facoltativo. -->
            {#each opts(f) as o (o.value)}
              <button class="btn" class:filled={values[f.key] === o.value} aria-pressed={values[f.key] === o.value}
                onclick={() => (values[f.key] = values[f.key] === o.value && !f.required ? null : o.value)}>{o.label}</button>
            {/each}
          </div>
        </div>
      {:else}
        {@const p = values[f.key]?.trim() ? problem(f, values[f.key]) : null}
        <label class="field">
          <span class="lab">{title}</span>
          <input class:bad={p} inputmode={input(f)} bind:value={values[f.key]} />
          {#if p}<small class="err">{say(p)}</small>{:else if f.description}<small>{f.description}</small>{/if}
        </label>
      {/if}
    {/each}
    <div class="acts">
      <button class="btn" onclick={() => onAnswer(e, 'decline', {})}>{t.elicitDecline}</button>
      <button class="btn filled" disabled={!can} onclick={() => onAnswer(e, 'accept', { ...values })}>{t.elicitSend}</button>
    </div>
  {/if}
</div>

<style>
  .elicit { display: flex; flex-direction: column; gap: 10px; padding: 14px; border-radius: 20px; background: var(--low); }
  .head { display: flex; align-items: center; gap: 10px; }
  .col { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 1px; }
  .col b { font-size: 16px; font-weight: 600; }
  .mono { font: 12px/1.4 var(--mono); color: var(--text2); }
  .link { overflow-wrap: anywhere; }
  .q { font-size: 16px; }
  .note { font-size: 14px; color: var(--text2); }
  .field { display: flex; flex-direction: column; gap: 6px; }
  .lab { font-size: 14px; font-weight: 500; }
  .field small { font-size: 12px; color: var(--text2); }
  .field small.err { color: var(--gone); }
  .opts, .acts { display: flex; flex-wrap: wrap; gap: 6px; }
  .acts { justify-content: flex-end; gap: 8px; }
  input { font: inherit; color: var(--text); background: transparent; border: 1px solid var(--line); border-radius: 12px; padding: 12px 14px; outline: none; }
  input:focus { border-color: var(--primary); }
  input.bad { border-color: var(--gone); }
  .btn { display: inline-flex; align-items: center; height: 40px; padding: 0 16px; border-radius: 999px; background: var(--high); color: var(--text); font-weight: 500; text-decoration: none; }
  .btn.filled { background: var(--primary); color: var(--on-primary); }
  .btn:disabled { opacity: .5; cursor: default; }
  a.btn { align-self: flex-start; }
</style>
