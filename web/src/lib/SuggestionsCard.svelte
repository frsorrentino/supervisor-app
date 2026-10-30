<script lang="ts">
  import type { Suggestion } from './contract'
  import { t } from './t'
  import { options } from './suggestions'

  // «Suggerimenti della master» (Franz 24/10 19:46, scheda A del mockup docs/mockup/2026-10-24-suggerimenti-home; contratto
  // 1.84): la stessa struttura delle altre schede, al massimo tre voci, ognuna col suo simbolo, «Accetta» tonale, «Parliamone»,
  // «Non ora» e «Mai». Accetta manda il prompt alla master: niente parte senza un tocco. Contratto 1.88: una scelta
  // (`choice`) ha un tasto tonale per opzione al posto di Accetta, come in «Per te».
  let { shown, total, at, onAnswer }: { shown: Suggestion[]; total: number; at: string; onAnswer: (s: Suggestion, action: 'accept' | 'later' | 'never' | 'discuss' | 'choose', choice?: string) => void } = $props()
  // «Altre risposte» (app-home-riordino-29-10): Parliamone, Non ora e Mai dietro un tocco; prima erano dodici tasti in tre voci.
  let more = $state(new Set<string>())
  const icon: Record<string, string> = {
    recurring: 'M17 2l4 4-4 4M3 11V9a3 3 0 0 1 3-3h15M7 22l-4-4 4-4M21 13v2a3 3 0 0 1-3 3H3',
    cleanup: 'M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14',
    deadline: 'M12 9v4l2 2M9 2h6M20 13a8 8 0 1 1-16 0 8 8 0 0 1 16 0z',
    quota: 'M21 12a9 9 0 1 1-9-9M21 3v6h-6',
    choice: 'M6 3v6a6 6 0 0 0 6 6h0a6 6 0 0 1 6 6M18 3v4M6 21v-4M14 7l4-4 4 4',
    followup: 'M15 4l5 5-5 5M20 9H9a5 5 0 0 0 0 10h3',
    unsaved: 'M5 3h11l3 3v13a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2zM8 3v5h7V3M8 14h8v7H8z',
    waiting: 'M6 2h12M6 22h12M7 2v4l5 6 5-6V2M7 22v-4l5-6 5 6v4',
    idea: 'M9 18h6M10 22h4M12 2a6 6 0 0 0-4 10.5c.8.8 1 1.5 1 2.5h6c0-1 .2-1.7 1-2.5A6 6 0 0 0 12 2z',
    proposal: 'M12 3v4M12 17v4M3 12h4M17 12h4M6 6l2.5 2.5M15.5 15.5L18 18M6 18l2.5-2.5M15.5 8.5L18 6',
    // Contratto 1.94: le note di claude-observe di un plugin, un foglietto con l'angolo piegato.
    observe: 'M15 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v10zM15 21v-4a2 2 0 0 1 2-2h4M7 8h10M7 12h6',
    // Un tipo che quest'app non conosce: una stella neutra, non più l'icona della quota (documento suggerimenti-coda, §8.8).
    other: 'M12 3l1.9 4.6L19 9l-4 3.4L16 18l-4-2.8L8 18l1-5.6L5 9l5.1-1.4z',
  }
</script>

<!-- Nello stile «Studio e Diretta» (30/10): fuori da «Ti aspettano», una lista a filo col binario; il tipo è l'icona grigia
     a sinistra e la parola sopra il testo; a destra «Altre risposte» (da cui si rifiuta) e «Accetta» tonale. -->
<div class="card sugg">
  <div class="top">
    <span class="rail"><span class="ms" aria-hidden="true">auto_awesome</span></span>
    <span class="nm">{t.sugTitle}</span>
    <span class="mono">{at} · {t.sugCount(shown.length, total)}</span>
  </div>
  {#each shown as s (s.id)}
    <div class="sug">
      <span class="rail sym"><svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d={icon[s.kind] ?? icon.other} /></svg></span>
      <span class="txt">
        <span class="k">{t.sugKind[s.kind] ?? t.sugKindOther}</span>
        <span class="body">{s.text}</span>
        <span class="btns">
          {#if more.has(s.id)}
            <button class="text" onclick={() => onAnswer(s, 'discuss')}>{t.sugDiscuss}</button>
            <button class="text" onclick={() => onAnswer(s, 'later')}>{t.sugLater}</button>
            <button class="text" onclick={() => onAnswer(s, 'never')}>{t.sugNever}</button>
          {:else}<button class="text" aria-expanded="false" onclick={() => (more = new Set([...more, s.id]))}>{t.sugMore}</button>{/if}
          {#each options(s) as o (o)}<button class="tonal" onclick={() => onAnswer(s, 'choose', o)}>{o}</button>{:else}<button class="tonal" onclick={() => onAnswer(s, 'accept')}>{t.sugAccept}</button>{/each}
        </span>
      </span>
    </div>
  {/each}
</div>

<style>
  .card { background: var(--s1); border: 1px solid var(--line); border-radius: 14px; display: flex; flex-direction: column; overflow: hidden; }
  .top { display: flex; align-items: center; gap: 8px; padding: 12px 12px 4px 0; }
  .rail { width: 52px; flex: none; display: flex; justify-content: center; color: var(--tx2); }
  .nm { flex: 1; min-width: 0; font: 600 17px/1.2 var(--display); }
  .mono { font: 400 12.5px var(--mono); color: var(--tx2); white-space: nowrap; }
  .sug { display: flex; gap: 8px; padding: 12px 12px 12px 0; border-top: 1px solid var(--line); }
  .sug:first-of-type { border-top: 0; }
  .sym { padding-top: 2px; }
  .txt { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; overflow-wrap: anywhere; }
  .k { font-size: 13px; color: var(--tx3); }
  .body { font-size: 15px; line-height: 1.4; color: var(--tx); }
  .btns { display: flex; flex-wrap: wrap; justify-content: flex-end; align-items: center; gap: 6px; margin-top: 8px; }
  .btns button { font: 600 15px var(--display); letter-spacing: .02em; white-space: nowrap; }
  .tonal { min-height: 40px; padding: 0 16px; border-radius: 20px; background: var(--s3); color: var(--tx); }
  .tonal:hover { background: color-mix(in srgb, var(--tx) 8%, var(--s3)); }
  .text { min-height: 40px; padding: 0 10px; border-radius: 20px; color: var(--tx2); }
  .text:hover { background: var(--s2); }
</style>
