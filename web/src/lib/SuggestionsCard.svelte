<script lang="ts">
  import type { Suggestion } from './contract'
  import { t } from './t'

  // «Suggerimenti della master» (Franz 24/10 19:46, scheda A del mockup docs/mockup/2026-10-24-suggerimenti-home; contratto
  // 1.84): la stessa struttura delle altre schede, al massimo tre voci, ognuna col suo simbolo, «Accetta» tonale, «Parliamone»,
  // «Non ora» e «Mai». Accetta manda il prompt alla master: niente parte senza un tocco.
  let { shown, total, at, onAnswer }: { shown: Suggestion[]; total: number; at: string; onAnswer: (s: Suggestion, action: 'accept' | 'later' | 'never' | 'discuss') => void } = $props()
  const tone: Record<string, string> = { recurring: 'var(--b-ring)', cleanup: 'var(--good)', deadline: 'var(--b-warn)', quota: 'var(--b-week, #B9A6F5)', choice: 'var(--b-warn)', followup: 'var(--b-ring)', unsaved: 'var(--good)', waiting: 'var(--text2)', idea: 'var(--advice)', proposal: 'var(--advice)' }
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
    // Un tipo che quest'app non conosce: una stella neutra, non più l'icona della quota (documento suggerimenti-coda, §8.8).
    other: 'M12 3l1.9 4.6L19 9l-4 3.4L16 18l-4-2.8L8 18l1-5.6L5 9l5.1-1.4z',
  }
</script>

<div class="card sugg">
  <div class="top">
    <span class="badge" aria-hidden="true">✦</span>
    <span class="nm">{t.sugTitle}</span>
    <span class="mono">{at} · {t.sugCount(shown.length, total)}</span>
  </div>
  {#each shown as s (s.id)}
    <div class="sug" style="--s:{tone[s.kind] ?? 'var(--text2)'}">
      <span class="sym"><svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d={icon[s.kind] ?? icon.other} /></svg></span>
      <span class="txt">
        <span class="k">{t.sugKind[s.kind] ?? t.sugKindOther}</span>
        <span class="body">{s.text}</span>
        <span class="btns">
          <button class="tonal" onclick={() => onAnswer(s, 'accept')}>{t.sugAccept}</button>
          <button class="text" onclick={() => onAnswer(s, 'discuss')}>{t.sugDiscuss}</button>
          <button class="text" onclick={() => onAnswer(s, 'later')}>{t.sugLater}</button>
          <button class="text" onclick={() => onAnswer(s, 'never')}>{t.sugNever}</button>
        </span>
      </span>
    </div>
  {/each}
</div>

<style>
  .card { background: color-mix(in srgb, var(--advice) 9%, var(--low)); border-radius: 20px; padding: 12px 14px; display: flex; flex-direction: column; gap: 6px; }
  .top { display: flex; align-items: center; gap: 10px; }
  .badge { width: 24px; height: 24px; border-radius: 50%; display: grid; place-items: center; flex: none; background: var(--advice); color: #0b0d10; font-size: 13px; font-weight: 700; }
  .nm { flex: 1; min-width: 0; font-weight: 600; font-size: 16px; }
  .mono { font: 12px var(--mono); color: var(--text2); white-space: nowrap; }
  .sug { display: flex; gap: 10px; padding: 10px 0 2px; border-top: 1px solid color-mix(in srgb, var(--advice) 18%, transparent); }
  .sug:first-of-type { border-top: 0; }
  .sym { width: 30px; height: 30px; border-radius: 50%; flex: none; display: grid; place-items: center; background: color-mix(in srgb, var(--s) 20%, transparent); color: var(--s); }
  .txt { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; overflow-wrap: anywhere; }
  .k { font: 600 10.5px var(--mono); letter-spacing: .1em; text-transform: uppercase; color: var(--s); }
  .body { font-size: 14.5px; line-height: 1.4; }
  .btns { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 4px; }
  .btns button { border-radius: 999px; padding: 6px 12px; font-size: 13.5px; font-weight: 500; white-space: nowrap; }
  .tonal { background: var(--high); } .tonal:hover { filter: brightness(1.2); }
  .text { color: var(--text2); padding: 6px 8px; } .text:hover { background: var(--surface); }
</style>
