<script lang="ts">
  import TiAspetta from './TiAspetta.svelte'
  import CinqueMinuti from './CinqueMinuti.svelte'
  import DaProvare from './DaProvare.svelte'
  import type { CmdResult, DecisionItem, State, WaitItem, WaitOption } from './contract'
  import { decodeWait, decodeWaits, readPage } from './decideDecode'
  import { newCmd } from './transport'
  import type { Kind, Page } from './tiAspetta'
  import { t } from './t'
  // La scheda «Ti aspetta» collegata al contratto 1.130 (app-scheda-ti-aspetta-07-11): legge la voce con `attesa` e le altre con
  // `attese`. La carta in alto è quella che l'app ha già, trovata col wid: le decisioni del registro (ok, scelta, pausa, needs_you)
  // con CinqueMinuti, le verifiche con DaProvare; le domande nate in chat (Prossimi, AskUserQuestion) hanno i loro tasti dalle
  // `options`, con l'op di `answer`. «Apri nella trascrizione» salta alla voce `msg.id`.
  let { id, st, now, wide = false, run, onBack, onOpenWait, onJump, onSession, onApprove, onDone, onAsk, onAnswer, onPrompt, onAttentionDone, onScreen, onShot }: {
    id: string; st: State; now: number; wide?: boolean
    run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>
    onBack: () => void; onOpenWait: (id: string) => void
    onJump: (session: string, entry: string, sessionId: string | null) => void; onSession: (name: string) => void
    onApprove: (task: string, note: string, choice?: string) => void; onDone: (i: DecisionItem) => void; onAsk: (text: string) => void
    onAnswer: (session: string, n: number) => void; onPrompt: (session: string, text: string) => void
    onAttentionDone: (kind: string, ref: string) => void; onScreen: (s: string) => void; onShot: (f: File, note: string) => void
  } = $props()

  let item = $state<WaitItem | null>(null), others = $state<WaitItem[]>([]), failed = $state(false), gone = $state(false)
  $effect(() => {
    const want = id
    item = null; failed = false; gone = false
    void run(newCmd('attesa', null, want)).then(r => {
      if (want !== id) return
      if (r && !r.ok) { gone = true; return }
      const { page, failed: f } = readPage(r, decodeWait, null); item = page; failed = f
    })
    void run(newCmd('attese', null)).then(r => { const { page } = readPage(r, decodeWaits, null); if (page) others = page.items.filter(x => x.id !== want) })
  })
  const kindOf = (l: string): Kind => (l === 'choose' ? 'choose' : l === 'verify' ? 'verify' : l === 'reply' ? 'answer' : 'decide')
  const page = $derived<Page | null>(item && {
    key: item.id, kind: kindOf(item.label), session: item.session ?? null, since: item.at,
    source: item.msg ? { session: item.msg.session ?? item.session ?? '', at: item.msg.at ?? null, text: item.msg.text, ask: item.msg.key ?? null, entry: item.msg.id ?? null } : null,
    others: others.map(o => ({ key: o.id, kind: kindOf(o.label), title: o.card?.what || o.text, session: o.session ?? null, since: o.at })),
  })
  // La carta che l'app ha già: decisioni e verifiche col wid; le altre, i tasti di questa scheda.
  const inDecisions = $derived(!!item && item.type === 'carta' && item.kind !== 'prova')
  const inProve = $derived(!!item && item.kind === 'prova')
  let sent = $state<number | null>(null)
  function pickOption(o: WaitOption | null) {
    const w = item; const a = w?.answer
    if (!w || !a) return
    sent = o?.n ?? 0
    if (a.op === 'answer' && w.session && o) onAnswer(a.session ?? w.session, o.n)
    else if (a.op === 'prompt' && a.arg && o) { onPrompt(a.arg, o.value); if (a.done?.arg && a.done.kind) onAttentionDone(a.done.kind, a.done.arg) }
    else if (a.op === 'attention_done' && a.arg) onAttentionDone(a.kind ?? 'needs_you', a.arg)
    else if (a.op === 'approve' && a.arg) onApprove(a.arg, '', o?.value)
  }
</script>

{#if page && item}{@const w = item}
  <TiAspetta {page} {now} {wide} {onBack} onOther={onOpenWait} onJump={(s) => { if (s.entry) onJump(s.session, s.entry, w.msg?.session_id ?? null) }}>
    {#snippet card()}
      {#if inDecisions}
        <CinqueMinuti rows allOpen only={(g) => g.items.some(i => i.wid === w.id)} {st} {run} {onApprove} {onDone} {onAsk} />
      {:else if inProve}
        <DaProvare {run} only={(p) => p.wid === w.id} {onScreen} {onShot} />
      {:else}
        <div class="wc" data-testid="ti-aspetta-carta">
          <p class="k">{w.label_text}</p>
          <p class="q">{w.text}</p>
          {#if w.options?.length}
            <div class="opts">
              {#each w.options as o (o.n)}
                <button class="opt" class:rec={o.recommended} class:done={sent === o.n} disabled={sent != null} data-testid="ti-aspetta-scelta-{o.n}" onclick={() => pickOption(o)}>
                  <span class="num">{o.n}</span><span class="tx"><b>{o.label}</b>{#if o.recommended || o.description}<small>{[o.recommended ? t.tiAspettaRecommended : null, o.description].filter(Boolean).join(' · ')}</small>{/if}</span>
                </button>
              {/each}
            </div>
          {:else if w.answer}
            <button class="main" disabled={sent != null} onclick={() => pickOption(null)}>{t.tiAspettaDone}</button>
          {:else}
            <p class="note">{t.tiAspettaByMaster}</p>
          {/if}
          {#if w.session}<button class="own" onclick={() => onSession(w.session!)}>{t.tiAspettaOwnWords}</button>{/if}
        </div>
      {/if}
    {/snippet}
  </TiAspetta>
{:else}
  <section class="empty">
    <button class="back" aria-label={t.back} onclick={onBack}><svg viewBox="0 0 24 24" width="24" height="24"><path d="M15 5l-7 7 7 7" fill="none" stroke="var(--tx)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></button>
    <p>{gone ? t.tiAspettaGone : failed ? t.tiAspettaFailed : t.tiAspettaLoading}</p>
  </section>
{/if}

<style>
  .wc { display: flex; flex-direction: column; gap: 8px; padding: 14px; border-radius: 16px; background: var(--s1); border: 1px solid color-mix(in srgb, var(--wait) 45%, transparent); }
  .k { font: 600 12px var(--sans, inherit); letter-spacing: .08em; text-transform: uppercase; color: var(--tx3); }
  .q { font-size: 16px; line-height: 23px; white-space: pre-wrap; }
  .opts { display: flex; flex-direction: column; gap: 6px; }
  .opt { display: flex; gap: 10px; align-items: flex-start; padding: 10px 12px; border-radius: 12px; background: var(--s2); text-align: left; }
  .opt.rec { background: color-mix(in srgb, var(--wait) 14%, transparent); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--wait) 60%, transparent); }
  .opt.done { box-shadow: inset 0 0 0 1px var(--good); }
  .opt:disabled:not(.done) { opacity: .45; }
  .num { flex: none; width: 24px; height: 24px; border-radius: 50%; display: grid; place-items: center; font: 600 13px var(--mono); border: 1.5px solid var(--tx2); color: var(--tx2); }
  .opt.rec .num { background: var(--wait); border-color: var(--wait); color: #111; }
  .tx { display: flex; flex-direction: column; }
  .tx b { font: 700 15px var(--display); }
  .opt.rec .tx b { color: var(--wait); }
  .tx small { font-size: 13px; color: var(--tx2); }
  .main { align-self: flex-start; font: 600 15px var(--display); padding: 9px 16px; border-radius: 999px; color: var(--wait); background: color-mix(in srgb, var(--wait) 14%, transparent); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--wait) 60%, transparent); }
  .own { align-self: flex-start; font: 600 15px var(--display); padding: 9px 16px; border-radius: 999px; background: var(--s3); color: var(--tx); }
  .note { font-size: 14px; color: var(--tx2); }
  .empty { display: flex; flex-direction: column; gap: 12px; padding: 10px 16px; height: 100%; background: var(--bg); }
  .empty .back { width: 44px; height: 44px; display: grid; place-items: center; }
  .empty p { color: var(--tx2); }
</style>
