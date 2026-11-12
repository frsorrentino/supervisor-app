<script lang="ts">
  import type { WaitAction, WaitFile, WaitItem } from './contract'
  import AttesaImmagine from './AttesaImmagine.svelte'
  import AttesaFiles from './AttesaFiles.svelte'
  import Icon from './Icon.svelte'
  import ImageViewer from './ImageViewer.svelte'
  import { imageRatios, media } from './mediaCache.svelte'
  import { choiceOf, frameRatio, outcomeOf, REDO_MAX, redoReady, roundsOf, sizeText, waitFileKey, type Round } from './scelta'
  import { locale } from './lang'
  import { t } from './t'
  // La scheda di scelta aperta (app-scheda-scelta-12-11, descrizione approvata da Franz il 12/11 alle 15:10): la descrizione vera,
  // le opzioni una accanto all'altra, ognuna con la sua immagine, la riga di pro e contro e il suo tasto; le immagini si ingrandiscono
  // al tocco e nel visore si passa dall'una all'altra; in fondo «Nessuna, rifare» col motivo, obbligatorio. Un giro nuovo entra nella
  // stessa scheda e i precedenti restano sotto, col motivo per cui sono stati scartati. Dall'archivio (dopo la risposta) non ha tasti
  // e dice com'è finita. Una sola griglia: stretta, le immagini stanno affiancate in alto e sotto ogni opzione ha la sua riga intera,
  // così il testo non finisce in colonne strette; larga (misurata sulla scheda, non sulla finestra), ogni opzione è una colonna.
  // Le regole (lettere, tasto di ogni opzione, esito, giri) stanno in scelta.ts, coi casi condivisi con Kotlin.
  let { item, rows = [], sent, onSend, onFile }: {
    item: WaitItem; rows?: [string, string][]; sent: number | null
    onSend: (i: number, written: string | null) => void
    onFile: (f: WaitFile, act: 'preview' | 'open') => void
  } = $props()
  const acts = $derived(item.actions ?? [])
  const c = $derived(choiceOf(item))
  const out = $derived(outcomeOf(item))
  const rounds = $derived(roundsOf(item))
  const n = $derived(c.options.length)
  const hasPics = $derived(c.options.some(o => o.images.length > 0))
  const ar = $derived(frameRatio(c.options.flatMap(o => o.images).map(f => imageRatios[waitFileKey(f)])))
  const ask = (f: WaitFile) => onFile(f, 'preview')

  // Il campo di un tasto che vuole un testo: il motivo di «Nessuna, rifare» (obbligatorio, fino a 300 caratteri) o le parole di un
  // altro tasto (per `text` possono mancare, come nella carta di prima). Mentre si scrive il tasto pieno è «Manda»: quello
  // dell'opzione consigliata torna tonale, così di pieno ce n'è sempre uno solo.
  let writing = $state<number | null>(null), written = $state('')
  const wa = $derived<WaitAction | null>(writing != null ? acts[writing] ?? null : null)
  const isRedo = $derived(writing != null && writing === c.redo)
  const ready = $derived(isRedo ? redoReady(written) : !!written.trim() || wa?.text_field === 'text')
  const left = $derived(REDO_MAX - written.trim().length)
  function tap(i: number) {
    if (sent != null) return
    const a = acts[i]
    if (a?.text_field) { writing = i; written = ''; return }
    onSend(i, null)
  }
  function send() { if (writing == null || !ready || sent != null) return; const i = writing; writing = null; onSend(i, written) }
  const focus = (node: HTMLTextAreaElement) => { node.focus({ preventScroll: true }); node.scrollIntoView({ block: 'nearest' }) }

  // Il visore: tutte le immagini già arrivate di questo giro (o di un giro precedente), nell'ordine delle opzioni.
  type Shot = { key: string; name: string }
  const shotsOf = (options: { letter: string; name: string; images: WaitFile[] }[], prefix = ''): Shot[] =>
    options.flatMap(o => o.images.map(f => ({ key: waitFileKey(f), name: [prefix, [o.letter, o.name].filter(Boolean).join(', '), o.images.length > 1 ? f.name : ''].filter(Boolean).join(' · ') })))
  let zoom = $state<{ items: { src: string; name: string }[]; start: number } | null>(null)
  function enlarge(shots: Shot[], key: string) {
    const ready = shots.filter(s => media[s.key])
    const start = ready.findIndex(s => s.key === key)
    if (start >= 0) zoom = { items: ready.map(s => ({ src: media[s.key], name: s.name })), start }
  }
  const when = (at: number) => {
    const d = new Date(at * 1000), hm = d.toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })
    return d.toDateString() === new Date().toDateString() ? t.sceltaToday(hm) : t.sceltaOn(`${d.getDate()}/${d.getMonth() + 1}`, hm)
  }
  const roundWhy = (r: Round) => (r.state === 'redo' ? t.sceltaRoundRedo : r.state === 'replaced' ? t.sceltaRoundReplaced : t.sceltaRoundClosed)
  const docIcon = (f: WaitFile) => (f.mime === 'application/pdf' ? 'pdf' : 'file')
</script>

<article class="sc" class:c2={n === 2} class:c3={n === 3} class:c4={n >= 4} data-testid="scelta-aperta" style:--n={n} style:--cols={n >= 4 ? 2 : Math.max(1, n)} style:--ar={ar}>
  <header>
    <h2 class="ttl">{item.title || item.text}</h2>
    {#if out.state !== 'open'}
      <p class="st {out.state}" data-testid="scelta-esito">
        {#if out.state === 'chosen' || out.state === 'approved'}<svg viewBox="0 0 20 20" width="18" height="18" aria-hidden="true"><path d="M4.5 10.5 8.5 14.5 15.5 6" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" /></svg>{/if}
        <span>{out.state === 'chosen' ? t.sceltaChosen(out.letter, out.name) : out.state === 'approved' ? t.sceltaApproved : out.state === 'redo' ? t.sceltaRedo : t.sceltaClosed}</span>
      </p>
      {#if out.reason}<p class="why">{out.reason}</p>{/if}
    {:else if rounds.length || item.card?.deploy}
      <p class="st">{#if rounds.length}<span>{t.sceltaRoundNow(rounds.length + 1)}</span>{/if}{#if item.card?.deploy}<span class="prod">{t.sceltaProduction}</span>{/if}</p>
    {/if}
  </header>
  {#if item.text && item.text !== (item.title || item.text)}<p class="q">{item.text}</p>{/if}
  {#if item.description && item.description !== item.text}<p class="desc">{item.description}</p>{/if}
  {#if rows.length}<dl>{#each rows as [k, v]}<dt>{k}</dt><dd>{v}</dd>{/each}</dl>{/if}

  <div class="opts" class:nopics={!hasPics}>
    {#each c.options as o (o.n)}
      {@const chosen = out.state === 'chosen' && out.letter === o.letter}
      {@const what = [o.letter, o.name].filter(Boolean).join(', ')}
      <div class="o" class:dim={out.state === 'chosen' && !chosen}>
        {#if o.images.length}
          <div class="pics">
            <AttesaImmagine file={o.images[0]} letter={o.letter} label={what} picked={chosen} onAsk={ask} onOpen={() => enlarge(shotsOf(c.options), waitFileKey(o.images[0]))} />
            {#if o.images.length > 1}<button class="more" aria-label={t.sceltaMoreImages(o.images.length - 1)} onclick={() => enlarge(shotsOf(c.options), waitFileKey(o.images[1]))}>+{o.images.length - 1}</button>{/if}
          </div>
        {:else if hasPics}
          <!-- Un'opzione senza immagine fra altre che ce l'hanno: stretta non ha cornice, larga tiene il posto nella sua colonna. -->
          <div class="pics none" aria-hidden="true"></div>
        {/if}
        <div class="row" data-testid="scelta-opzione-{o.letter}">
          <span class="lt" aria-hidden="true">{o.letter}</span>
          <p class="nm">{#if o.name}<b>{o.name}</b>{/if}{#if o.recommended}<span class="rec">{t.tiAspettaRecommended}</span>{/if}</p>
          {#if o.action != null}
            <button class="go" class:filled={o.recommended && writing == null} class:done={sent === o.action} disabled={sent != null} aria-label={t.sceltaPickAria(o.letter, o.name)} data-testid="scelta-scegli-{o.letter}" onclick={() => tap(o.action!)}>{t.sceltaPick(o.letter)}</button>
          {:else if chosen}
            <span class="got"><svg viewBox="0 0 20 20" width="18" height="18" aria-hidden="true"><path d="M4.5 10.5 8.5 14.5 15.5 6" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" /></svg>{t.sceltaPicked}</span>
          {/if}
          {#if o.line}<p class="ln">{o.line}</p>{/if}
          {#if o.docs.length}
            <div class="docs">{#each o.docs as f (f.path)}<button class="doc" onclick={() => onFile(f, 'open')}><Icon name={docIcon(f)} size={18} /><span class="dn">{f.name}</span>{#if f.size != null}<span class="ds">{sizeText(f.size)}</span>{/if}</button>{/each}</div>
          {/if}
        </div>
      </div>
    {/each}
  </div>

  {#if c.extra.length}<AttesaFiles files={c.extra} {onFile} />{/if}

  {#if writing != null && wa}
    <form class="form" onsubmit={(e) => { e.preventDefault(); send() }}>
      <label for="scelta-motivo">{isRedo ? t.sceltaReasonLabel : wa.label}</label>
      {#if isRedo}<p class="hint">{t.sceltaReasonHint}</p>{/if}
      <textarea id="scelta-motivo" rows="3" maxlength={isRedo ? REDO_MAX : undefined} bind:value={written} use:focus enterkeyhint="send" data-testid="scelta-motivo"></textarea>
      <div class="ac">
        {#if isRedo && left <= 60}<span class="left" aria-live="polite">{t.sceltaLeft(Math.max(0, left))}</span>{/if}
        <span class="sp"></span>
        <button type="button" class="txt" onclick={() => (writing = null)}>{t.cancel}</button>
        <button type="submit" class="go filled" disabled={!ready || sent != null} data-testid="scelta-manda">{t.tiAspettaSend}</button>
      </div>
    </form>
  {:else if c.redo != null || c.rest.length}
    <div class="foot">
      {#if c.redo != null}<button class="out" disabled={sent != null} data-testid="scelta-rifare" onclick={() => tap(c.redo!)}>{acts[c.redo].label}</button>{/if}
      {#each c.rest as i (i)}<button class="txt" class:done={sent === i} disabled={sent != null} data-testid="scelta-tasto-{i}" onclick={() => tap(i)}>{acts[i].label}</button>{/each}
    </div>
  {/if}

  {#if rounds.length}
    <section class="rounds" aria-label={t.sceltaRounds}>
      <h3>{t.sceltaRounds}</h3>
      {#each rounds as r (r.id)}
        {@const shots = shotsOf(r.options, t.sceltaRound(r.n))}
        <div class="rd" data-testid="scelta-giro-{r.n}">
          <p class="rh"><b>{t.sceltaRound(r.n)}</b><small>{when(r.answeredAt ?? r.at)}</small></p>
          <p class="rw"><span>{roundWhy(r)}</span>{#if r.reason}{': '}{r.reason}{/if}</p>
          {#if r.description || r.text}<p class="rt">{r.description || r.text}</p>{/if}
          {#if r.options.some(o => o.images.length)}
            <div class="rth">{#each r.options as o (o.n)}{#if o.images.length}<div class="rf" style:--ar={frameRatio([imageRatios[waitFileKey(o.images[0])]])}><AttesaImmagine small file={o.images[0]} letter={o.letter} label={[t.sceltaRound(r.n), o.letter, o.name].filter(Boolean).join(', ')} onAsk={ask} onOpen={() => enlarge(shots, waitFileKey(o.images[0]))} /></div>{/if}{/each}</div>
          {/if}
          <p class="ro">{#each r.options as o, i (o.n)}{#if i}{' · '}{/if}<b>{o.letter}</b>{#if o.name}{' '}{o.name}{/if}{/each}</p>
        </div>
      {/each}
    </section>
  {/if}
</article>

{#if zoom}<ImageViewer items={zoom.items} start={zoom.start} onClose={() => (zoom = null)} />{/if}

<style>
  .sc { container-type: inline-size; display: flex; flex-direction: column; gap: 14px; color: var(--tx); }
  /* Con tre o quattro opzioni le cornici sono più basse, così i tasti restano più vicini. */
  .sc.c3, .sc.c4 { --cap: 34dvh; }
  header { display: flex; flex-direction: column; gap: 6px; }
  .ttl { font: 700 22px/26px var(--display); letter-spacing: .005em; text-wrap: balance; overflow-wrap: anywhere; }
  /* Lo stato a parole: ambra finché aspetta, verde quando è scelta o approvata, grigio quando è chiusa. */
  .st { display: flex; flex-wrap: wrap; align-items: center; gap: 4px 8px; font: 600 16px/22px var(--display); letter-spacing: .01em; color: var(--tx2); }
  .st.redo, .st .prod { color: var(--wait); }
  .st.chosen, .st.approved { color: var(--good); }
  .why { font-size: 15.5px; line-height: 23px; white-space: pre-wrap; overflow-wrap: anywhere; }
  .q { font-size: 17px; line-height: 24px; white-space: pre-wrap; overflow-wrap: anywhere; max-width: 70ch; }
  .desc { font-size: 15.5px; line-height: 23px; color: var(--tx2); white-space: pre-wrap; overflow-wrap: anywhere; max-width: 70ch; }
  dl { display: grid; grid-template-columns: 96px minmax(0, 1fr); gap: 3px 10px; font-size: 14px; line-height: 20px; }
  dt { color: var(--tx3); }
  dd { overflow-wrap: anywhere; }

  /* Stretta: le cornici affiancate in alto (due o tre per riga; quattro in due righe), poi una riga intera per opzione. */
  .opts { display: grid; grid-template-columns: repeat(var(--cols), minmax(0, 1fr)); gap: 0 10px; }
  .o { display: contents; }
  .pics { order: 0; position: relative; margin-bottom: 10px; }
  .row { order: 1; grid-column: 1 / -1; display: grid; grid-template-columns: 26px minmax(0, 1fr) auto; column-gap: 10px; align-items: center; padding: 10px 0; border-top: 1px solid var(--line); }
  .nopics .o:first-child .row { border-top: 0; padding-top: 0; }
  .row .lt { font: 700 20px/24px var(--display); color: var(--tx2); align-self: start; padding-top: 10px; }
  .nm { display: flex; flex-wrap: wrap; align-items: baseline; gap: 2px 10px; min-height: 44px; align-content: center; }
  .nm b { font: 600 17px/22px var(--display); overflow-wrap: anywhere; }
  .rec { font: 500 14px/20px var(--sans); color: var(--wait); }
  .ln { grid-column: 2 / -1; font-size: 15px; line-height: 21px; color: var(--tx2); overflow-wrap: anywhere; padding-bottom: 2px; }
  .docs { grid-column: 2 / -1; display: flex; flex-direction: column; }
  .doc { display: flex; align-items: center; gap: 8px; min-height: 40px; text-align: left; color: var(--tx); touch-action: manipulation; }
  .doc .dn { min-width: 0; overflow-wrap: anywhere; font-size: 15px; text-decoration: underline; text-decoration-color: var(--tx3); text-underline-offset: 3px; }
  .doc .ds { flex: none; font: 12.5px var(--mono); color: var(--tx2); }
  .dim .row .nm b, .dim .row .lt { color: var(--tx2); }
  .got { display: inline-flex; align-items: center; gap: 6px; font: 600 15px var(--display); color: var(--good); }
  .more { position: absolute; right: 6px; bottom: 6px; min-width: 32px; height: 28px; padding: 0 8px; border-radius: 8px; background: color-mix(in srgb, var(--s1) 90%, transparent); color: var(--tx); font: 600 13px var(--display); touch-action: manipulation; }
  .pics.none { display: none; }

  /* I tasti: uno solo pieno, quello dell'opzione consigliata; gli altri tonali; «Nessuna, rifare» col filo; il resto a parole. */
  .go { min-height: 44px; padding: 0 18px; border-radius: 999px; background: var(--s3); color: var(--tx); font: 600 15px var(--display); letter-spacing: .01em; white-space: nowrap; touch-action: manipulation; user-select: none; -webkit-user-select: none; transition: transform 120ms ease-out, opacity 160ms ease-out; }
  .go.filled { background: var(--act); color: var(--on-act); }
  .go:active:not(:disabled), .out:active:not(:disabled) { transform: scale(.97); }
  .go:disabled { opacity: .45; cursor: default; }
  .go.done:disabled { opacity: 1; box-shadow: inset 0 0 0 2px var(--good); }
  .foot { display: flex; flex-wrap: wrap; align-items: center; gap: 4px 6px; padding-top: 12px; border-top: 1px solid var(--line); }
  .out { min-height: 44px; padding: 0 16px; border-radius: 999px; box-shadow: inset 0 0 0 1px var(--tx3); color: var(--tx); font: 600 15px var(--display); letter-spacing: .01em; touch-action: manipulation; user-select: none; -webkit-user-select: none; transition: transform 120ms ease-out; }
  .txt { min-height: 44px; padding: 0 12px; border-radius: 999px; color: var(--tx2); font: 600 15px var(--display); letter-spacing: .01em; touch-action: manipulation; user-select: none; -webkit-user-select: none; }
  .out:disabled, .txt:disabled { opacity: .45; cursor: default; }
  .txt.done:disabled { opacity: 1; color: var(--good); }
  .go:focus-visible, .out:focus-visible, .txt:focus-visible, .doc:focus-visible, .more:focus-visible { outline: 2px solid var(--tx); outline-offset: 2px; }

  .form { display: flex; flex-direction: column; gap: 8px; padding-top: 12px; border-top: 1px solid var(--line); transition: opacity 180ms ease-out, transform 180ms cubic-bezier(0.23, 1, 0.32, 1); }
  @starting-style { .form { opacity: 0; transform: translateY(6px); } }
  .form label { font: 600 17px/22px var(--display); }
  .hint { font-size: 14px; line-height: 20px; color: var(--tx2); }
  /* 16 px: sotto, iOS ingrandisce la pagina quando il campo prende il fuoco. */
  textarea { width: 100%; min-height: 92px; max-height: 40dvh; field-sizing: content; resize: vertical; padding: 10px 12px; border-radius: 12px; background: color-mix(in srgb, var(--tx) 5%, var(--s1)); color: var(--tx); border: 1px solid var(--line); font: inherit; font-size: 16px; line-height: 22px; caret-color: var(--wait); }
  textarea:focus-visible { outline: 2px solid var(--tx); outline-offset: 1px; }
  @media (pointer: coarse) { textarea { resize: none; } }
  .ac { display: flex; align-items: center; gap: 6px; }
  .sp { flex: 1; }
  .left { font: 12.5px var(--mono); color: var(--tx2); }

  .rounds { display: flex; flex-direction: column; margin-top: 10px; }
  .rounds h3 { font: 700 18px/22px var(--display); padding-bottom: 8px; }
  .rd { display: flex; flex-direction: column; gap: 6px; padding: 12px 0; border-top: 1px solid var(--line); }
  .rh { display: flex; align-items: baseline; gap: 10px; }
  .rh b { font: 600 16px/20px var(--display); }
  .rh small { font: 12.5px var(--mono); color: var(--tx2); }
  .rw { font-size: 15px; line-height: 21px; overflow-wrap: anywhere; }
  .rw span { color: var(--tx2); }
  .rt { font-size: 14px; line-height: 20px; color: var(--tx2); overflow-wrap: anywhere; }
  /* Le miniature dei giri precedenti: tutte alte uguali, larghe quanto vuole la loro immagine. */
  .rth { display: flex; flex-wrap: wrap; gap: 8px; }
  .rf { width: calc(112px * var(--ar, 1.3333)); }
  .ro { font-size: 14px; line-height: 20px; color: var(--tx2); overflow-wrap: anywhere; }
  .ro b { font: 600 14px var(--display); color: var(--tx); }

  @media (hover: hover) and (pointer: fine) {
    .go:hover:not(:disabled) { background: color-mix(in srgb, var(--tx) 12%, var(--s3)); }
    .go.filled:hover:not(:disabled) { background: color-mix(in srgb, var(--act) 88%, var(--bg)); }
    .out:hover:not(:disabled) { box-shadow: inset 0 0 0 1px var(--tx); }
    .txt:hover:not(:disabled) { color: var(--tx); }
    .doc:hover .dn { text-decoration-color: var(--tx); }
  }

  /* Larga: ogni opzione è una colonna (cornice, nome, riga di pro e contro, tasto in fondo); i tasti restano allineati. La soglia
     cresce col numero delle opzioni, così una colonna non scende mai sotto i 240 px circa. */
  @container (min-width: 560px) {
    .sc.c2 .opts { grid-template-columns: repeat(2, minmax(0, 1fr)); grid-template-rows: auto 1fr; gap: 0 20px; }
    .sc.c2 .o { display: grid; grid-row: 1 / span 2; grid-template-rows: subgrid; }
    .sc.c2 .row { grid-column: auto; border-top: 0; padding: 4px 0 0; grid-template-columns: 26px minmax(0, 1fr); grid-template-rows: auto auto auto 1fr; align-items: start; }
    .sc.c2 .pics.none { display: block; }
    .sc.c2 .row .lt { padding-top: 0; }
    .sc.c2 .nm { min-height: 0; }
    .sc.c2 .go, .sc.c2 .got { grid-column: 1 / -1; grid-row: 4; align-self: end; margin-top: 14px; }
    .sc.c2 .go { width: 100%; }
  }
  @container (min-width: 780px) {
    .sc.c3 .opts { grid-template-columns: repeat(3, minmax(0, 1fr)); grid-template-rows: auto 1fr; gap: 0 20px; }
    .sc.c3 .o { display: grid; grid-row: 1 / span 2; grid-template-rows: subgrid; }
    .sc.c3 .row { grid-column: auto; border-top: 0; padding: 4px 0 0; grid-template-columns: 26px minmax(0, 1fr); grid-template-rows: auto auto auto 1fr; align-items: start; }
    .sc.c3 .pics.none { display: block; }
    .sc.c3 .row .lt { padding-top: 0; }
    .sc.c3 .nm { min-height: 0; }
    .sc.c3 .go, .sc.c3 .got { grid-column: 1 / -1; grid-row: 4; align-self: end; margin-top: 14px; }
    .sc.c3 .go { width: 100%; }
  }
  @container (min-width: 980px) {
    .sc.c4 .opts { grid-template-columns: repeat(4, minmax(0, 1fr)); grid-template-rows: auto 1fr; gap: 0 16px; }
    .sc.c4 .o { display: grid; grid-row: 1 / span 2; grid-template-rows: subgrid; }
    .sc.c4 .row { grid-column: auto; border-top: 0; padding: 4px 0 0; grid-template-columns: 26px minmax(0, 1fr); grid-template-rows: auto auto auto 1fr; align-items: start; }
    .sc.c4 .pics.none { display: block; }
    .sc.c4 .row .lt { padding-top: 0; }
    .sc.c4 .nm { min-height: 0; }
    .sc.c4 .go, .sc.c4 .got { grid-column: 1 / -1; grid-row: 4; align-self: end; margin-top: 14px; }
    .sc.c4 .go { width: 100%; }
  }
  @media (prefers-reduced-motion: reduce) { .go, .out, .form { transition: none; } }
</style>
