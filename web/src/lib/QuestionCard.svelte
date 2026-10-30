<script lang="ts">
  import type { Question } from './contract'
  import { allowAllVisible } from './questionRules'
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
</script>

<div class="qcard v3">
  <div class="qtext">
    <span class="gl"><StateGlyph kind={q.tier === 'high' ? 'red' : 'waiting'} /></span>
    <p><Md text={q.text} /></p>
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
  .qtext p { flex: 1; font-size: 16px; line-height: 1.5; letter-spacing: .03em; white-space: pre-wrap; overflow-wrap: anywhere; }
  .speak { color: var(--icon); width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center; flex: none; margin-top: -4px; }
  .speak:hover { background: var(--s3); }
  .more { display: flex; gap: 8px; }
  .outl { flex: 1; min-height: 44px; border: 1px solid color-mix(in srgb, var(--tx3) 50%, transparent); border-radius: 22px; color: var(--tx); font: 600 15px var(--display); }
  .outl:hover { background: var(--s3); }
</style>
