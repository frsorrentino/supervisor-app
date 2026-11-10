<script lang="ts">
  import type { ProvaRead } from './provaLinks'
  import { t } from './t'
  // La scheda di una prova (app-prova-link-10-11, contratto 1.134): si apre da «prova #N» in un messaggio o da `#/prova/<n>`. Il numero e il
  // compito; lo stato con le parole della persona o il motivo e l'ora; dove, cosa fare e cosa vedere. Ancora da provare, il tasto porta a
  // Verifica. `read` null: sta arrivando; un relay di prima del contratto 1.134 lo dice in una riga. Come ProvaPage in Kotlin.
  let { n, read, wide = false, onBack, onVerify }: { n: number; read: ProvaRead | null; wide?: boolean; onBack: () => void; onVerify: () => void } = $props()
  const p = $derived(read?.kind === 'found' ? read.prova : null)
  const dm = (at: number) => { const d = new Date(at * 1000), z = (k: number) => String(k).padStart(2, '0'); return `${z(d.getDate())}/${z(d.getMonth() + 1)} ${z(d.getHours())}:${z(d.getMinutes())}` }
  const word = (s: string) => (s === 'ok' ? t.provaStateOk : s === 'ko' ? t.provaStateKo : s === 'red' ? t.provaStateRed : t.provaStateTodo)
</script>

<section class="pv" class:wide data-testid="prova-{n}">
  <header>
    {#if !wide}<button class="ic" aria-label={t.back} onclick={onBack}><svg viewBox="0 0 24 24" width="24" height="24"><path d="M15 5l-7 7 7 7" fill="none" stroke="var(--tx)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></button>{/if}
    <span class="tt"><b>{t.provaTitle(n)}</b>{#if p}<small>{[p.short ?? (p.task || null), p.session, p.build || null].filter(Boolean).join(' · ')}</small>{/if}</span>
    {#if wide}<button class="ic" aria-label={t.closeWord} onclick={onBack}><svg viewBox="0 0 24 24" width="20" height="20"><path d="M6 6l12 12M18 6L6 18" fill="none" stroke="var(--tx2)" stroke-width="2" stroke-linecap="round" /></svg></button>{/if}
  </header>
  <div class="body">
    {#if !read}<p class="wait" data-testid="prova-in-arrivo"><span class="spin" aria-hidden="true"></span>{t.provaLoading}</p>
    {:else if read.kind === 'unsupported'}<p class="note" data-testid="prova-non-supportata">{t.provaUnsupported}</p>
    {:else if read.kind === 'refused'}<p class="err" data-testid="prova-rifiutata">{read.message}</p>
    {:else}{@const q = read.prova}
      <div class="state s-{q.state}" data-testid="prova-stato-{q.state}"><p class="lb"><i></i>{word(q.state)}</p>{#if q.text}<p>{q.text}</p>{/if}{#if q.at}<small>{t.provaAt(dm(q.at))}</small>{/if}</div>
      {#if q.title}<p class="title">{q.title}</p>{/if}
      {#each [[t.provaWhere, q.dove], [t.provaDo, q.fai], [t.provaExpected, q.atteso]] as [label, value]}{#if value}<div class="f"><small>{label}</small><p>{value}</p></div>{/if}{/each}
    {/if}
  </div>
  {#if p?.state === 'todo'}<footer><button class="btn main" data-testid="prova-verifica" onclick={onVerify}>{t.provaOpenVerify}</button></footer>{/if}
</section>

<style>
  .pv { display: flex; flex-direction: column; height: 100%; background: var(--bg); color: var(--tx); }
  .wide { background: transparent; }
  header { display: flex; align-items: center; gap: 6px; padding: 10px 6px 6px; }
  .wide header { padding-left: 18px; }
  .ic { width: 44px; height: 44px; display: grid; place-items: center; border-radius: 50%; flex: none; }
  .tt { flex: 1; min-width: 0; display: flex; flex-direction: column; }
  .tt b { font: 700 22px/26px var(--display); }
  .tt small { font-size: 14px; line-height: 19px; color: var(--tx2); }
  .body { flex: 1; overflow-y: auto; padding: 8px 14px 16px; display: flex; flex-direction: column; gap: 12px; }
  .state { padding: 14px; border-radius: 14px; background: var(--s1); border: 1px solid var(--line); display: flex; flex-direction: column; gap: 6px; font-size: 15.5px; line-height: 21px; }
  .state small { font-size: 14px; color: var(--tx2); }
  .lb { display: flex; align-items: center; gap: 8px; font: 600 15px var(--display); letter-spacing: .04em; text-transform: uppercase; color: var(--wait); }
  .lb i { width: 9px; height: 9px; border-radius: 50%; background: currentColor; }
  .s-ok .lb { color: var(--good); }
  .s-ko .lb, .s-red .lb { color: var(--red); }
  .title { font-size: 15.5px; line-height: 21px; color: var(--tx2); }
  .f small { display: block; font-size: 12px; letter-spacing: .1em; text-transform: uppercase; color: var(--tx3); }
  .f p { font-size: 15.5px; line-height: 21px; }
  .wait { display: flex; align-items: center; gap: 10px; color: var(--tx2); }
  .spin { width: 14px; height: 14px; border-radius: 50%; border: 2px solid var(--tx3); border-top-color: var(--tx2); animation: pvspin .9s linear infinite; }
  @keyframes pvspin { to { transform: rotate(360deg); } }
  @media (prefers-reduced-motion: reduce) { .spin { animation: none; } }
  .note { color: var(--tx2); }
  .err { color: var(--red); }
  footer { display: flex; gap: 10px; padding: 12px 14px; }
  .btn { flex: 1; min-height: 48px; padding: 8px 14px; border-radius: 999px; font: 600 16px var(--display); }
  .btn.main { color: var(--wait); background: color-mix(in srgb, var(--wait) 14%, transparent); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--wait) 60%, transparent); }
</style>
