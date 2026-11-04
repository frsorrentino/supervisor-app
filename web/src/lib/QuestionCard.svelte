<script lang="ts">
  import type { Question } from './contract'
  import { allowAllVisible, FOLD_LINES, permissionCard } from './questionRules'
  import { question } from './speechRules'
  import { speech, toggle } from './speech.svelte'
  import { t } from './t'
  import Md from './Md.svelte'
  import Options from './Options.svelte'
  import StateGlyph from './StateGlyph.svelte'
  // La domanda in fondo alla chat (QuestionCard dell'app) nella forma delle schede di «Ti aspettano» (Franz 30/10 21:19): fondo
  // neutro, il colore solo nel glifo di stato, rosso col rischio alto; il testo con ▶, le opzioni con la consigliata a bordo
  // chiaro e numero pieno, sotto «Parliamone» e, per i permessi, «Consenti tutto».
  let { q, source, onAnswer, onChat, onAllowAll }: { q: Question; source: string; onAnswer: (n: number) => void; onChat: () => void; onAllowAll: () => void } = $props()
  // Si legge la domanda con le opzioni numerate.
  const spoken = $derived(question(q))
  const reading = $derived(speech.text === spoken)
  // Contratto 1.101: cosa si approva; un testo lungo si piega con il tasto per aprirlo, mai tagliato senza poterlo aprire.
  const card = $derived(permissionCard(q))
  let open = $state(false)
</script>

<div class="qcard v3">
  <div class="qtext">
    <span class="gl"><StateGlyph kind={q.tier === 'high' ? 'red' : 'waiting'} /></span>
    {#if card}
      <div class="perm">
        <span class="label">{t.permissionAsks}</span>
        <p class:folded={card.folds && !open} style:--fold={FOLD_LINES}><Md text={card.main} /></p>
        {#if open && card.more}<pre>{card.more}</pre>{/if}
        {#if card.folds}<button class="fold" aria-expanded={open} onclick={() => (open = !open)}>{open ? t.permissionLess : t.permissionMore}</button>{/if}
        {#if card.question}<span class="label">{card.question}</span>{/if}
      </div>
    {:else}
      <p><Md text={q.text} /></p>
    {/if}
    <button class="speak" aria-label={reading ? t.stopReading : t.readAloud} onclick={() => toggle(spoken, source)}>
      {#if reading}<svg viewBox="0 0 24 24" width="22" height="22"><rect x="7" y="7" width="10" height="10" rx="1.5" fill="currentColor" /></svg>
      {:else}<svg viewBox="0 0 24 24" width="22" height="22"><path d="M8 5.5v13l10.5-6.5z" fill="currentColor" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" /></svg>{/if}
    </button>
  </div>
  <Options {q} {onAnswer} />
  <div class="more">
    <button class="outl" onclick={onChat}>{t.chatAboutThis}</button>
    {#if allowAllVisible(q)}<button class="outl" onclick={onAllowAll}>{t.allowAll}</button>{/if}
  </div>
</div>

<style>
  .qcard { background: var(--s2); border-radius: 14px; padding: 12px; display: flex; flex-direction: column; gap: 10px; max-width: 760px; }
  .qtext { display: flex; gap: 10px; align-items: flex-start; }
  .gl { flex: none; display: flex; padding-top: 3px; }
  .qtext p { flex: 1; font-size: 15.5px; line-height: 1.5; letter-spacing: .03em; white-space: pre-wrap; overflow-wrap: anywhere; }
  .perm { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px; align-items: flex-start; }
  .perm p { flex: none; align-self: stretch; }
  .perm p.folded { max-height: calc(var(--fold) * 1.5em); overflow: hidden; }
  .perm .label { font-size: 14px; color: var(--tx2); overflow-wrap: anywhere; }
  .perm pre { align-self: stretch; margin: 0; font: 12.5px/1.45 var(--mono, monospace); color: var(--tx2); white-space: pre-wrap; overflow-wrap: anywhere; }
  .fold { min-height: 44px; color: var(--icon); font: 600 15px var(--display); }
  .speak { color: var(--icon); width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center; flex: none; margin-top: -4px; }
  .speak:hover { background: var(--s3); }
  .more { display: flex; gap: 8px; }
  .outl { flex: 1; min-height: 44px; border: 1px solid color-mix(in srgb, var(--tx3) 50%, transparent); border-radius: 22px; color: var(--tx); font: 600 15px var(--display); }
  .outl:hover { background: var(--s3); }
</style>
