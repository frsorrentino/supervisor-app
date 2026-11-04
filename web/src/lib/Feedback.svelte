<script lang="ts">
  import Check from './Check.svelte'
  import { confirmMs } from './leave'
  import { draftOf, type Draft, type FeedbackKind, type Tech } from './feedback'
  import { t } from './t'
  // Feedback dal menù (app-feedback-menu-04-11, scelta A): il tipo, il testo, i dati tecnici aggiunti da soli e la foto della
  // pagina, spenta di base e con l'anteprima; poi la bozza anonima con «Invia dal mio GitHub», «Invia anonimo» e «Non ora».
  // L'invio (contratto 1.121, `feedback_app`): `onSend` manda la bozza e la scelta, e dice il link di GitHub o l'errore. Null:
  // il relay non ha ancora l'op, la bozza si vede ma non parte.
  let { tech, onGrab, onSend, onDone }: {
    tech: Tech
    onGrab: (() => Promise<string | null>) | null
    onSend: ((d: Draft, photo: string | null, as: 'github' | 'anon' | 'later') => Promise<{ url: string | null } | string>) | null
    onDone: () => void
  } = $props()
  // Il testo non scritto resta per la prossima volta, solo in questo browser.
  const KEY = 'cm.feedback_text'
  const load = () => { try { return localStorage.getItem(KEY) ?? '' } catch { return '' } }
  let kind = $state<FeedbackKind>('idea')
  let text = $state(load())
  let photo = $state<string | null>(null)
  let photoFail = $state(false)
  let grabbing = $state(false)
  let draft = $state<Draft | null>(null)
  let sending = $state(false)
  // Mandato: la spunta si disegna sul tasto usato (D3, app-motion-loops-04-11), poi il foglio si chiude.
  let sentAs = $state<'github' | 'anon' | null>(null)
  // «Invia dal mio GitHub»: il link dell'issue già scritta, da aprire col proprio account.
  let github = $state<string | null>(null)
  let result = $state<string | null>(null)
  $effect(() => { const v = text; try { if (v) localStorage.setItem(KEY, v); else localStorage.removeItem(KEY) } catch { /* solo per questa visita */ } })
  const techLines = $derived([`app ${tech.version} (${tech.commit})`, tech.device, tech.screen, tech.lang])
  async function togglePhoto() {
    if (photo) { photo = null; return }
    photoFail = false; grabbing = true
    photo = await (onGrab?.() ?? Promise.resolve(null)).catch(() => null)
    grabbing = false
    photoFail = !photo
  }
  function prepare() {
    draft = draftOf(kind, text, tech, !!photo, { kind: { idea: t.feedbackIdea, bug: t.feedbackKindBug }, tech: t.feedbackTech, photo: t.feedbackPhotoAttached })
  }
  async function send(as: 'github' | 'anon') {
    if (!draft || !onSend) return
    sending = true
    const r = await onSend(draft, photo, as).catch((e) => String(e))
    sending = false
    if (typeof r === 'string') { result = t.feedbackSendFail(r); return }
    text = ''; sentAs = as
    // Su GitHub la persona apre l'issue e la manda lei: il foglio resta col link.
    if (as === 'github' && r.url) { github = r.url; result = t.feedbackGithubHint; return }
    result = t.feedbackAnonSent
    setTimeout(onDone, confirmMs('ok') + 300)
  }
  // «Non ora»: con il relay la bozza resta fra le osservazioni del PC.
  async function later() {
    if (draft && onSend && !sentAs) {
      const r = await onSend(draft, photo, 'later').catch((e) => String(e))
      if (typeof r !== 'string') { text = ''; result = t.feedbackKept; setTimeout(onDone, 900); return }
    }
    onDone()
  }
</script>

<div class="fb">
  {#if !draft}
    <div class="kinds" role="radiogroup">
      <button role="radio" aria-checked={kind === 'idea'} class:on={kind === 'idea'} onclick={() => (kind = 'idea')}>{t.feedbackIdea}</button>
      <button role="radio" aria-checked={kind === 'bug'} class:on={kind === 'bug'} onclick={() => (kind = 'bug')}>{t.feedbackBug}</button>
    </div>
    <textarea bind:value={text} rows="6" placeholder={kind === 'bug' ? t.feedbackPlaceholderBug : t.feedbackPlaceholderIdea}></textarea>
    <div class="card">
      <div class="row"><span class="tx"><b>{t.feedbackTech}</b><small>{t.feedbackTechSub}</small></span></div>
      <ul>{#each techLines as l}<li class="mono">{l}</li>{/each}</ul>
    </div>
    <!-- Dove il browser non sa riprendere la scheda (telefono) la foto non c'è. -->
    {#if onGrab}<div class="card">
      <button class="row" role="switch" aria-checked={!!photo} disabled={grabbing} onclick={togglePhoto}>
        <span class="tx"><b>{t.feedbackPhoto}</b><small class:warn={photoFail}>{photoFail ? t.feedbackPhotoFail : t.feedbackPhotoSub}</small></span>
        <span class="sw" aria-hidden="true"><span></span></span>
      </button>
      {#if photo}<img class="shot" src={photo} alt={t.feedbackPhotoAttached} />{/if}
    </div>{/if}
    <button class="fill" disabled={!text.trim()} onclick={prepare}>{t.feedbackPrepare}</button>
  {:else}
    <div class="card">
      <div class="row"><span class="tx"><b>{t.feedbackDraft}</b><small class="wrap">{t.feedbackDraftSub}</small></span></div>
      <h3>{draft.title}</h3>
      <pre>{draft.body}</pre>
      {#if photo}<img class="shot" src={photo} alt={t.feedbackPhotoAttached} />{/if}
    </div>
    {#if result}<p class="note">{result}</p>{:else if !onSend}<p class="note">{t.feedbackNotYet}</p>{/if}
    {#if github}<a class="fill gh" href={github} target="_blank" rel="noopener" onclick={() => setTimeout(onDone, 300)}>{t.feedbackGithubOpen}</a>{/if}
    <button class="fill ck" class:done={sentAs === 'github'} disabled={!onSend || sending || !!sentAs} onclick={() => send('github')}><span class="lbl">{t.feedbackSendGithub}</span>{#if sentAs === 'github'}<Check />{/if}</button>
    <div class="btns">
      <button class="tx2 ck" class:done={sentAs === 'anon'} disabled={!onSend || sending || !!sentAs} onclick={() => send('anon')}><span class="lbl">{t.feedbackSendAnon}</span>{#if sentAs === 'anon'}<Check />{/if}</button>
      <button class="tx2" onclick={() => (draft = null)}>{t.feedbackEdit}</button>
      <button class="tx2" disabled={sending || !!sentAs} onclick={later}>{t.feedbackLater}</button>
    </div>
  {/if}
</div>

<style>
  .fb { display: flex; flex-direction: column; gap: 10px; padding: 8px 12px 24px; max-width: 560px; }
  .kinds { display: flex; gap: 8px; }
  .kinds button { flex: 1; min-height: 44px; border-radius: 22px; background: var(--s2); color: var(--tx); font: 600 15px var(--display); }
  .kinds button.on { background: var(--s3); box-shadow: inset 0 0 0 2px var(--tx2); }
  textarea { min-height: 140px; padding: 12px 14px; border-radius: 14px; border: 1px solid var(--line); background: var(--s1); color: var(--tx); font: 16px/1.4 var(--body, inherit); resize: vertical; }
  .card { background: var(--s1); border-radius: 14px; overflow: hidden; }
  .row { display: flex; align-items: center; gap: 12px; width: 100%; min-height: 56px; padding: 8px 14px; text-align: left; }
  .tx { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
  .tx b { font: 600 16px var(--display); color: var(--tx); }
  .tx small { font-size: 13.5px; color: var(--tx2); }
  .tx small.warn { color: var(--wait); }
  ul { list-style: none; margin: 0; padding: 0 14px 12px; font-size: 13px; color: var(--tx2); }
  li { padding: 2px 0; }
  .sw { flex: none; position: relative; width: 52px; height: 32px; border-radius: 16px; background: var(--s3); transition: background .2s; }
  .sw span { position: absolute; top: 6px; left: 6px; width: 20px; height: 20px; border-radius: 50%; background: var(--tx); transition: transform .2s; }
  [aria-checked='true'] .sw { background: var(--busy); }
  [aria-checked='true'] .sw span { transform: translateX(20px); background: #fff; }
  .shot { display: block; width: calc(100% - 28px); margin: 0 14px 14px; border-radius: 10px; border: 1px solid var(--line); }
  h3 { margin: 0 14px 6px; font: 600 16px var(--display); color: var(--tx); }
  pre { margin: 0 14px 14px; white-space: pre-wrap; font: 13.5px/1.45 var(--mono); color: var(--tx2); }
  .note { margin: 0 4px; font-size: 14px; color: var(--tx2); }
  .fill { min-height: 48px; border-radius: 24px; background: var(--act); color: var(--on-act); font: 600 17px var(--display); }
  a.gh { display: flex; align-items: center; justify-content: center; text-decoration: none; }
  .fill:disabled, .tx2:disabled { opacity: .45; }
  .btns { display: flex; justify-content: center; gap: 4px; flex-wrap: wrap; }
  .tx2 { padding: 12px 14px; font-size: 15px; color: var(--tx); border-radius: 22px; }
  .tx2:hover:not(:disabled) { background: var(--s2); }
  @media (prefers-reduced-motion: reduce) { .sw, .sw span { transition: none; } }
</style>
