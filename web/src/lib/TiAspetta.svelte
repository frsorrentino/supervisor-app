<script lang="ts">
  import type { Snippet } from 'svelte'
  import { age, marks, type Kind, type Page, type Source } from './tiAspetta'
  import { t } from './t'
  // La scheda «Ti aspetta» (app-scheda-ti-aspetta-07-11, mockup approvato da Franz il 07/11 alle 21:04): la testata con la sessione
  // e da quanto aspetta; la carta coi tasti (`card`, quella di Decidi, Scegli, Verifica o Rispondi); «Da dove viene» col pezzo di
  // trascrizione, la frase della domanda accesa e «Apri nella trascrizione»; le altre che aspettano. `wide`: la carta e «Da dove
  // viene» affiancate e la × al posto della freccia indietro. «Ti aspettano anche» parte chiusa e si apre col tocco, anche quando da
  // qui si apre un'altra scheda (app-ti-aspetta-lista-chiusa-08-11). Come TiAspettaPage in Kotlin.
  // `stack` (app-scheda-scelta-12-11): una scelta con le immagini tiene tutta la larghezza anche sulla plancia, e «Da dove viene» le
  // sta sotto. `title` e `sub`: una scheda che non aspetta più dice com'è finita e quando, al posto di «Ti aspetta» e «da 12 min».
  let { page, now, wide = false, stack = false, title = null, sub = null, card, onBack, onJump, onOther }: {
    page: Page; now: number; wide?: boolean; stack?: boolean; title?: string | null; sub?: string | null; card: Snippet
    onBack: () => void; onJump: (s: Source) => void; onOther: (key: string) => void
  } = $props()
  const label: Record<Kind, string> = { decide: t.tiAspettaKind.decide, choose: t.tiAspettaKind.choose, verify: t.tiAspettaKind.verify, answer: t.tiAspettaKind.answer }
  let open = $state(false), openKey = ''
  $effect.pre(() => { if (page.key !== openKey) { openKey = page.key; open = false } })
  const hm = (at: number) => new Date(at * 1000).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' })
</script>

<section class="ta" class:wide class:stack data-testid="ti-aspetta-{page.key}">
  <header>
    {#if !wide}<button class="ic" aria-label={t.back} onclick={onBack}><svg viewBox="0 0 24 24" width="24" height="24"><path d="M15 5l-7 7 7 7" fill="none" stroke="var(--tx)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></button>{/if}
    <span class="tt"><b>{title ?? t.tiAspettaTitle}</b><small>{#if sub != null}{[page.session, sub].filter(Boolean).join(' · ')}{:else}{#if page.session}{page.session} · {/if}<em>{t.tiAspettaSince(age(now - page.since))}</em>{/if}</small></span>
    {#if wide}<button class="ic" aria-label={t.closeWord} onclick={onBack}><svg viewBox="0 0 24 24" width="20" height="20"><path d="M6 6l12 12M18 6L6 18" fill="none" stroke="var(--tx2)" stroke-width="2" stroke-linecap="round" /></svg></button>{/if}
  </header>
  <div class="body">
    <div class="top">
      <div class="slot">{@render card()}</div>
      {#if page.source}{@const src = page.source}
        <div class="src">
          <p class="lbl">{t.tiAspettaSource}</p>
          <div class="box">
            <small>{src.at ? t.tiAspettaReplyAt(src.session, hm(src.at)) : src.session}</small>
            <blockquote data-testid="ti-aspetta-pezzo">{#each marks(src.text, src.ask) as s}{#if s.on}<mark>{s.text}</mark>{:else}{s.text}{/if}{/each}</blockquote>
            {#if src.entry}<button class="jump" data-testid="ti-aspetta-salto" onclick={() => onJump(src)}>{t.tiAspettaJump}</button>{/if}
          </div>
        </div>
      {/if}
    </div>
    {#if page.others.length}
      <button class="lbl tog" class:open aria-expanded={open} aria-label="{t.tiAspettaOthers} {page.others.length}, {open ? t.tiAspettaOthersClose : t.tiAspettaOthersOpen}" data-testid="ti-aspetta-altre" onclick={() => (open = !open)}>
        <span>{t.tiAspettaOthers} <i>{page.others.length}</i></span>
        <svg viewBox="0 0 24 24" width="22" height="22"><path d="M6 9l6 6 6-6" fill="none" stroke="var(--tx2)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
      </button>
      {#if open}
      <div class="others">
        {#each page.others as o (o.key)}
          <button class="row" data-testid="ti-aspetta-altra-{o.key}" onclick={() => onOther(o.key)}>
            <span class="d"></span>
            <span class="x"><span>{o.title}</span><small>{[label[o.kind], o.session, age(now - o.since)].filter(Boolean).join(' · ')}</small></span>
            <svg viewBox="0 0 24 24" width="20" height="20"><path d="M9 6l6 6-6 6" fill="none" stroke="var(--tx3)" stroke-width="2" stroke-linecap="round" /></svg>
          </button>
        {/each}
      </div>
      {/if}
    {/if}
  </div>
</section>

<style>
  .ta { display: flex; flex-direction: column; height: 100%; background: var(--bg); color: var(--tx); }
  header { display: flex; align-items: center; gap: 6px; padding: 10px 6px; }
  .wide header { padding-left: 18px; border-bottom: 1px solid var(--line); }
  .ic { width: 44px; height: 44px; display: grid; place-items: center; border-radius: 50%; }
  .tt { flex: 1; min-width: 0; display: flex; flex-direction: column; }
  .tt b { font: 700 22px/1.1 var(--display); }
  .tt small { font: 13px var(--mono); color: var(--tx2); margin-top: 3px; }
  .tt em { font-style: normal; color: var(--wait); }
  .body { flex: 1; overflow-y: auto; padding: 12px 14px 24px; display: flex; flex-direction: column; gap: 14px; }
  .top { display: flex; flex-direction: column; gap: 14px; }
  .wide .top { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; align-items: start; }
  .wide.stack .top { grid-template-columns: minmax(0, 1fr); gap: 22px; }
  .wide.stack .body { padding: 18px 22px 28px; }
  .wide.stack .src { max-width: 720px; }
  .src { display: flex; flex-direction: column; gap: 8px; }
  .lbl { font: 600 12px var(--sans, inherit); letter-spacing: .1em; text-transform: uppercase; color: var(--tx3); margin: 4px 2px 0; }
  .lbl i { font-style: normal; color: var(--wait); margin-left: 6px; }
  .tog { display: flex; align-items: center; justify-content: space-between; min-height: 44px; margin: 0; padding: 0 6px 0 2px; text-align: left; cursor: pointer; }
  .tog svg { transition: transform .2s; }
  .tog.open svg { transform: rotate(180deg); }
  .box { border: 1px solid var(--s3); border-radius: 10px; padding: 10px 12px 12px; display: flex; flex-direction: column; gap: 8px; }
  .box small { font: 12.5px var(--mono); color: var(--tx2); }
  blockquote { border-left: 1px solid var(--tx3); padding: 2px 0 2px 12px; font-size: 15px; line-height: 22px; color: var(--tx2); white-space: pre-wrap; }
  mark { background: color-mix(in srgb, var(--wait) 18%, transparent); color: var(--tx); border-radius: 3px; padding: 0 2px; }
  .jump { align-self: flex-start; font: 600 14.5px var(--display); padding: 8px 14px; border-radius: 999px; background: var(--s3); color: var(--tx); }
  .others { border-radius: 14px; background: var(--s1); overflow: hidden; }
  .row { display: flex; align-items: center; gap: 10px; width: 100%; min-height: 52px; padding: 10px 12px; text-align: left; border-top: 1px solid var(--line); }
  .row:first-child { border-top: 0; }
  .d { width: 8px; height: 8px; border-radius: 50%; background: var(--wait); flex: none; }
  .x { flex: 1; min-width: 0; display: flex; flex-direction: column; font-size: 15px; }
  .x small { font: 12.5px var(--mono); color: var(--tx2); }
</style>
