<script lang="ts">
  import TiAspetta from './TiAspetta.svelte'
  import CinqueMinuti from './CinqueMinuti.svelte'
  import DaProvare from './DaProvare.svelte'
  import SceltaAperta from './SceltaAperta.svelte'
  import AttesaFiles from './AttesaFiles.svelte'
  import type { CmdResult, DecisionItem, State, WaitAction, WaitFile, WaitItem } from './contract'
  import { isChoice, outcomeOf } from './scelta'
  import { locale } from './lang'
  import { decodeWait, decodeWaits, readPage } from './decideDecode'
  import { newCmd } from './transport'
  import { adviceLine, adviceOf, cmdOf, lookOf, sentWhat, waitLoad, waitShown, type Kind, type Page } from './tiAspetta'
  import { markAnswered, waits } from './attese.svelte'
  import { t } from './t'
  // La scheda «Ti aspetta» collegata al contratto 1.130-1.131 (app-scheda-ti-aspetta-07-11): legge la voce con `attesa` e le altre con
  // `attese`. La carta in alto ha i tasti del relay (`actions`, ognuno col suo comando già pronto); un relay senza `actions` mostra la
  // carta che l'app ha già, trovata col wid (CinqueMinuti per le decisioni, DaProvare per le verifiche). «Apri nella trascrizione»
  // salta alla voce `msg.id`.
  // Dopo una risposta (un tasto, Approva o Chiudi senza ok, l'ok di una verifica) la scheda si chiude con onBack e torna alla pagina
  // da cui era stata aperta (app-ti-aspetta-lista-chiusa-08-11).
  // Contratto 1.137 (app-scheda-scelta-12-11): una scelta ha la sua scheda (SceltaAperta: le opzioni con immagini, righe di pro e
  // contro e tasti, «Nessuna, rifare», i giri precedenti); ogni altra richiesta mostra i suoi file sotto il testo (le immagini di cosa
  // guardare di una verifica). La voce si legge con `archive`: dopo la risposta la scheda resta, senza tasti, con com'è finita.
  let { id, st, now, wide = false, run, onBack, onHome, onSession = () => {}, onSent, onOpenWait, onJump, onApprove, onDone, onAsk, onScreen, onShot, onFile = () => {}, onAnswered = () => {} }: {
    id: string; st: State; now: number; wide?: boolean
    run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>
    onBack: () => void; onOpenWait: (id: string) => void
    /** app-attesa-dopo-scelta-17-11: la via per la home, sempre; e dopo una risposta, cosa è partito e a chi (afterAnswer). */
    onHome: () => void; onSession?: (session: string) => void; onSent: (w: WaitItem, what: string, to: string | null) => void
    onJump: (session: string, entry: string, sessionId: string | null) => void
    onApprove: (task: string, note: string, choice?: string) => void; onDone: (i: DecisionItem) => void; onAsk: (text: string) => void
    onScreen: (s: string) => void; onShot: (f: File, note: string) => void
    /** Un file della richiesta: l'anteprima (la chiede la cornice appena è in pagina) o l'apertura. */
    onFile?: (f: WaitFile, act: 'preview' | 'open') => void
    /** Una risposta partita da questa scheda ha avuto il suo esito dal PC: le schede compatte delle chat rileggono la loro. */
    onAnswered?: (w: WaitItem) => void
  } = $props()

  // app-attesa-caricamento-17-11 (17/11 11:23: il comando `attesa` 8,8 s in fila e 4,1 s in esecuzione, 13 s di sola «La leggo dal PC»):
  // la scheda parte subito dalla voce che l'app ha già nell'elenco delle attese (titolo, sessione, tipo, tasti) e chiede al PC solo il
  // resto (da dove viene, i file, com'è finita); un segno piccolo finché arriva. Oltre 10 s dice che il PC è lento, con Riprova e
  // Apri la sessione. Una lettura fallita ha Riprova. Stessa forma per una voce aperta da link e da elenco.
  let full = $state<WaitItem | null>(null), fetched = $state<WaitItem[] | null>(null), failed = $state(false), gone = $state(false), slow = $state(false), tries = $state(0)
  const item = $derived(waitShown(id, full, waits.items, gone))
  // La carta mostrata può avere un id diverso da quello chiesto (un giro dopo, o `replaced_from`): fuori dalle altre anche per card_id.
  const others = $derived((fetched ?? waits.items).filter(x => x.id !== id && x.id !== item?.id && !(item?.card_id && x.card_id === item.card_id)))
  const mark = $derived(waitLoad(full != null, failed, gone, slow))
  $effect(() => {
    const want = id, n = tries
    full = null; failed = false; gone = false; slow = false; sent = null; writing = null; written = ''
    const timer = setTimeout(() => { if (want === id && n === tries && full == null) slow = true }, 10_000)
    void run({ ...newCmd('attesa', null, want), archive: true }).then(r => {
      if (want !== id || n !== tries) return
      clearTimeout(timer); slow = false
      if (r && !r.ok) { gone = true; return }
      const { page, failed: f } = readPage(r, decodeWait, null); full = page; failed = f
    })
    void run(newCmd('attese', null)).then(r => { const { page } = readPage(r, decodeWaits, null); if (page) fetched = page.items })
    return () => clearTimeout(timer)
  })
  const retry = () => { tries += 1 }
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
    send(i, a.text_field ? written : null)
  }
  /** Manda il comando del tasto `i`, col testo scritto nel suo campo, e chiude la scheda. */
  function send(i: number, text: string | null) {
    const w = item, a = w?.actions?.[i]
    if (sent != null || !w || !a) return
    sent = i; writing = null
    const c = cmdOf(a, a.text_field ? text : null)
    // La voce esce subito da ogni vista (app-riall-1-dati-16-11); se il PC rifiuta, torna.
    markAnswered(w.id)
    void run({ ...newCmd(c.op as Parameters<typeof newCmd>[0], c.session ?? null, c.arg ?? null), ...c } as ReturnType<typeof newCmd>).then(r => { if (!r?.ok) markAnswered(w.id, false); onAnswered(w) })
    onSent(w, sentWhat(a.label, a.text_field ? text : null), typeof c.session === 'string' ? c.session : null)
  }
  // Una scheda che non aspetta più (dall'archivio): la testata dice com'è finita e quando, non da quanto aspetta.
  const out = $derived(item ? outcomeOf(item) : null)
  const choice = $derived(!!item && !legacy && isChoice(item))
  const answeredAt = $derived.by(() => {
    if (!out || out.state === 'open' || !out.at) return null
    const d = new Date(out.at * 1000), hm = d.toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })
    return [t.sceltaAnswered(d.toDateString() === new Date().toDateString() ? t.sceltaToday(hm) : t.sceltaOn(`${d.getDate()}/${d.getMonth() + 1}`, hm)), out.by].filter(Boolean).join(' · ')
  })
  const rows = (w: WaitItem) => [[t.tiAspettaWhat, w.card?.what], [t.tiAspettaWhere, w.card?.where], [t.tiAspettaUntested, w.card?.untested], [t.tiAspettaUndo, w.card?.undo_how || w.card?.undo]]
    .filter(([, v]) => !!v && v !== w.text) as [string, string][]
</script>

{#if page && item}{@const w = item}
  {#if mark}
    <!-- Il segno piccolo dei dettagli in arrivo; oltre 10 s il PC lento, con Riprova e la sessione. -->
    <div class="load" class:slow={mark !== 'details'} role="status" data-testid="ti-aspetta-dettagli">
      {#if mark === 'details'}<span class="spin" aria-hidden="true"></span>{t.tiAspettaDetails}{:else}{mark === 'failed' ? t.tiAspettaFailed : t.tiAspettaSlow}
        <button class="lk" onclick={retry}>{t.tiAspettaRetry}</button>{#if w.session}<button class="lk" onclick={() => onSession(w.session!)}>{t.tiAspettaOpenSession}</button>{/if}{/if}
    </div>
  {/if}
  <TiAspetta {page} {now} {wide} {onBack} {onHome} stack={choice} title={out && out.state !== 'open' ? t.sceltaDone[out.state] : null} sub={out && out.state !== 'open' ? answeredAt ?? '' : null} onOther={onOpenWait} onJump={(s) => { if (s.entry) onJump(s.session, s.entry, w.msg?.session_id ?? null) }}>
    {#snippet card()}
      {#if legacy && item.kind === 'prova'}
        <DaProvare {run} only={(p) => p.wid === w.id} {onScreen} {onShot} />
      {:else if legacy && w.type === 'carta'}
        <CinqueMinuti rows allOpen only={(g) => g.items.some(i => i.wid === w.id)} {st} {run} onApprove={(task, note, choice) => { onApprove(task, note, choice); onSent(w, sentWhat(choice || t.approve, note), null) }} onDone={(i) => { onDone(i); onSent(w, t.tiAspettaDone, null) }} {onAsk} />
      {:else if choice}
        <SceltaAperta item={w} rows={rows(w)} {sent} onSend={send} {onFile} />
      {:else}
        {@const acts = w.actions ?? []}
        <div class="wc" data-testid="ti-aspetta-carta">
          <p class="k">{w.label_text}{#if w.card?.deploy}<span class="tag">{t.tiAspettaProduction}</span>{/if}</p>
          {#if w.title && w.title !== w.text}<p class="ttl">{w.title}</p>{/if}
          <p class="q">{w.text}</p>
          <!-- Contratto 1.149: il consiglio di chi chiede, con la motivazione, sotto la domanda. -->
          {#if adviceOf(w.advice)}<p class="adv" data-testid="ti-aspetta-consiglio">{adviceLine(adviceOf(w.advice)!, t.tiAspettaAdvice, t.tiAspettaAdviceYes, t.tiAspettaAdviceNo)}</p>{/if}
          {#if rows(w).length}<dl>{#each rows(w) as [k, v]}<dt>{k}</dt><dd>{v}</dd>{/each}</dl>{/if}
          {#if w.files?.length}<AttesaFiles files={w.files} {onFile} />{/if}
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
          {#if !acts.length}<p class="note">{out && out.state !== 'open' ? [out.state === 'chosen' ? t.sceltaChosen(out.letter, out.name) : t.sceltaDone[out.state], out.reason].filter(Boolean).join(': ') : t.tiAspettaByMaster}</p>{/if}
        </div>
      {/if}
    {/snippet}
  </TiAspetta>
{:else}
  <section class="empty">
    <button class="back" aria-label={t.back} onclick={onBack}><svg viewBox="0 0 24 24" width="24" height="24"><path d="M15 5l-7 7 7 7" fill="none" stroke="var(--tx)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></button>
    <p>{#if !gone && !failed && !slow}<span class="spin" aria-hidden="true"></span>{/if}{gone ? t.tiAspettaGone : failed ? t.tiAspettaFailed : slow ? t.tiAspettaSlow : t.tiAspettaLoading}</p>
    <div class="btns">
      {#if failed || slow}<button class="home" onclick={retry}>{t.tiAspettaRetry}</button>{/if}
      <button class="home" data-testid="ti-aspetta-home" onclick={onHome}>{t.tiAspettaHome}</button>
    </div>
  </section>
{/if}

<style>
  .wc { display: flex; flex-direction: column; gap: 8px; padding: 14px; border-radius: 16px; background: var(--s1); border: 1px solid color-mix(in srgb, var(--wait) 45%, transparent); }
  .k { font: 600 12px var(--sans, inherit); letter-spacing: .08em; text-transform: uppercase; color: var(--tx3); }
  .q { font-size: 16px; line-height: 23px; white-space: pre-wrap; }
  .adv { font-size: 15px; line-height: 22px; color: var(--wait); overflow-wrap: anywhere; }
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
  .empty p { color: var(--tx2); display: flex; align-items: center; gap: 8px; }
  .empty .btns { display: flex; gap: 8px; flex-wrap: wrap; }
  .load { position: fixed; left: 50%; bottom: calc(16px + env(safe-area-inset-bottom)); transform: translateX(-50%); z-index: 5; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; max-width: calc(100% - 32px);
    padding: 7px 12px; border-radius: 999px; background: var(--s3); color: var(--tx2); font-size: 13.5px; }
  .load.slow { color: var(--tx); border-radius: 14px; }
  .lk { font: 600 13.5px var(--display); color: var(--tx); text-decoration: underline; }
  .spin { width: 12px; height: 12px; border-radius: 50%; border: 2px solid var(--tx3); border-top-color: transparent; animation: sp .9s linear infinite; flex: none; }
  @keyframes sp { to { transform: rotate(360deg) } }
  .empty .home { align-self: flex-start; font: 600 14.5px var(--display); padding: 8px 14px; border-radius: 999px; background: var(--s3); color: var(--tx); }
</style>
