<script lang="ts">
  import TiAspetta from './TiAspetta.svelte'
  import CinqueMinuti from './CinqueMinuti.svelte'
  import DaProvare from './DaProvare.svelte'
  import type { CmdResult, DecisionItem, State, WaitAction, WaitItem } from './contract'
  import { decodeWait, decodeWaits, readPage } from './decideDecode'
  import { newCmd } from './transport'
  import { cmdOf, lookOf, type Kind, type Page } from './tiAspetta'
  import { t } from './t'
  // La scheda «Ti aspetta» collegata al contratto 1.130-1.131 (app-scheda-ti-aspetta-07-11): legge la voce con `attesa` e le altre con
  // `attese`. La carta in alto ha i tasti del relay (`actions`, ognuno col suo comando già pronto); un relay senza `actions` mostra la
  // carta che l'app ha già, trovata col wid (CinqueMinuti per le decisioni, DaProvare per le verifiche). «Apri nella trascrizione»
  // salta alla voce `msg.id`.
  let { id, st, now, wide = false, run, onBack, onOpenWait, onJump, onApprove, onDone, onAsk, onScreen, onShot }: {
    id: string; st: State; now: number; wide?: boolean
    run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>
    onBack: () => void; onOpenWait: (id: string) => void
    onJump: (session: string, entry: string, sessionId: string | null) => void
    onApprove: (task: string, note: string, choice?: string) => void; onDone: (i: DecisionItem) => void; onAsk: (text: string) => void
    onScreen: (s: string) => void; onShot: (f: File, note: string) => void
  } = $props()

  let item = $state<WaitItem | null>(null), others = $state<WaitItem[]>([]), failed = $state(false), gone = $state(false)
  $effect(() => {
    const want = id
    item = null; failed = false; gone = false; sent = null; writing = null; written = ''
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
    others: others.map(o => ({ key: o.id, kind: kindOf(o.label), title: o.title || o.card?.what || o.text, session: o.session ?? null, since: o.at })),
  })
  // Senza `actions` (un relay prima della 1.131): la carta che l'app ha già, trovata col wid.
  const legacy = $derived(!!item && item.actions == null)
  // Un tasto: manda il suo comando, o chiede prima il testo del campo `text_field`; dopo l'invio i tasti si spengono.
  let sent = $state<number | null>(null), writing = $state<number | null>(null), written = $state('')
  function go(i: number, a: WaitAction) {
    if (sent != null) return
    if (a.text_field && writing !== i) { writing = i; written = ''; return }
    sent = i; writing = null
    const c = cmdOf(a, a.text_field ? written : null)
    void run({ ...newCmd(c.op as Parameters<typeof newCmd>[0], c.session ?? null, c.arg ?? null), ...c } as ReturnType<typeof newCmd>)
  }
  const rows = (w: WaitItem) => [[t.tiAspettaWhat, w.card?.what], [t.tiAspettaWhere, w.card?.where], [t.tiAspettaUntested, w.card?.untested], [t.tiAspettaUndo, w.card?.undo_how || w.card?.undo]]
    .filter(([, v]) => !!v && v !== w.text) as [string, string][]
</script>

{#if page && item}{@const w = item}
  <TiAspetta {page} {now} {wide} {onBack} onOther={onOpenWait} onJump={(s) => { if (s.entry) onJump(s.session, s.entry, w.msg?.session_id ?? null) }}>
    {#snippet card()}
      {#if legacy && item.kind === 'prova'}
        <DaProvare {run} only={(p) => p.wid === w.id} {onScreen} {onShot} />
      {:else if legacy && w.type === 'carta'}
        <CinqueMinuti rows allOpen only={(g) => g.items.some(i => i.wid === w.id)} {st} {run} {onApprove} {onDone} {onAsk} />
      {:else}
        {@const acts = w.actions ?? []}
        <div class="wc" data-testid="ti-aspetta-carta">
          <p class="k">{w.label_text}{#if w.card?.deploy}<span class="tag">{t.tiAspettaProduction}</span>{/if}</p>
          {#if w.title && w.title !== w.text}<p class="ttl">{w.title}</p>{/if}
          <p class="q">{w.text}</p>
          {#if rows(w).length}<dl>{#each rows(w) as [k, v]}<dt>{k}</dt><dd>{v}</dd>{/each}</dl>{/if}
          {#if acts.some(a => lookOf(a) === 'option')}
            <div class="opts">
              {#each acts as a, i}{#if lookOf(a) === 'option'}{@const num = acts.slice(0, i + 1).filter(x => lookOf(x) === 'option').length}
                <button class="opt" class:rec={a.recommended} class:done={sent === i} disabled={sent != null} data-testid="ti-aspetta-scelta-{num}" onclick={() => go(i, a)}>
                  <span class="num">{num}</span><span class="tx"><b>{a.label}</b>{#if a.recommended}<small>{t.tiAspettaRecommended}</small>{/if}</span>
                </button>
              {/if}{/each}
            </div>
          {/if}
          {#if acts.some(a => lookOf(a) !== 'option')}
            <div class="pills">
              {#each acts as a, i}{#if lookOf(a) !== 'option'}
                <button class="pill" class:main={lookOf(a) === 'main'} class:done={sent === i} class:off={sent != null && sent !== i} disabled={sent != null} data-testid="ti-aspetta-tasto-{i}" onclick={() => go(i, a)}>{a.label}</button>
              {/if}{/each}
            </div>
          {/if}
          {#if writing != null}{@const a = acts[writing]}
            <textarea class="field" rows="2" placeholder={a?.label} bind:value={written} data-testid="ti-aspetta-testo"></textarea>
            <div class="pills">
              <button class="pill main" disabled={!written.trim() && a?.text_field !== 'text'} onclick={() => a && go(writing!, a)}>{t.tiAspettaSend}</button>
              <button class="pill" onclick={() => (writing = null)}>{t.cancel}</button>
            </div>
          {/if}
          {#if !acts.length}<p class="note">{t.tiAspettaByMaster}</p>{/if}
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
  .k .tag { margin-left: 8px; letter-spacing: .04em; color: var(--wait); border: 1px solid color-mix(in srgb, var(--wait) 55%, transparent); border-radius: 6px; padding: 1px 6px; }
  .ttl { font: 700 18px/22px var(--display); }
  dl { display: grid; grid-template-columns: 96px 1fr; gap: 3px 10px; font-size: 14px; line-height: 20px; }
  dt { color: var(--tx3); }
  .pills { display: flex; flex-wrap: wrap; gap: 8px; }
  .pill { font: 600 15px var(--display); padding: 9px 16px; border-radius: 999px; background: var(--s3); color: var(--tx); }
  .pill.main { color: var(--wait); background: color-mix(in srgb, var(--wait) 14%, transparent); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--wait) 60%, transparent); }
  .pill.done { box-shadow: inset 0 0 0 1px var(--good); }
  .pill.off, .pill:disabled:not(.done) { opacity: .45; }
  .field { width: 100%; resize: vertical; padding: 10px 12px; border-radius: 12px; background: var(--s2); color: var(--tx); border: 1px solid var(--line); font: inherit; }
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
