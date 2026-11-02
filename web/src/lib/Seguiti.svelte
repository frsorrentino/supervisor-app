<script lang="ts">
  import type { CmdResult, RimastoItem, SeguitiPage, SeguitiSummary } from './contract'
  import { decodeSeguiti } from './decideDecode'
  import { age, canAccept, canAlign, changedPaths, clean, fired, ordered, whenText } from './seguiti'
  import { newCmd } from './transport'
  import { t } from './t'
  import { slide } from 'svelte/transition'
  import { leave } from './leave'
  import { reducedMotion } from './motion'
  // «Da seguire» (contratto 1.102), porta di SeguitiScreen: rimasto_indietro, stesso_difetto e promemoria in una pagina; quello che non si sa resta detto come non saputo.
  let { run, summary, onLavori, onMaster }: { run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>; summary: SeguitiSummary | null | undefined; onLavori: () => void; onMaster: () => void } = $props()
  let page = $state<SeguitiPage | null>(null)
  let failed = $state(false)
  let note = $state<string | null>(null)
  async function load() { const r = await run(newCmd('seguiti', null)); failed = !r?.ok; if (r?.ok) page = decodeSeguiti(r.text) }
  $effect(() => { void summary?.rimasto; void summary?.altrove; void summary?.promemoria; void load() })
  // Franz 02/11 15:38 (app-feedback-azioni-02-11): Accetta e Togli rispondono al tocco come in DaProvare (leave.ts): conferma sul
  // tasto, la scheda esce senza aspettare il PC, torna se il PC dice no; Allinea lascia la scheda e accende il tasto.
  let picked = $state<Record<string, string>>({}), gone = $state<Record<string, true>>({})
  const hooks = {
    pick: (ids: string[], v: string | null) => { const n = { ...picked }; for (const i of ids) { if (v) n[i] = v; else delete n[i] } picked = n },
    hide: (ids: string[], v: boolean) => { const n = { ...gone }; for (const i of ids) { if (v) n[i] = true; else delete n[i] } gone = n },
  }
  const outMs = () => (reducedMotion() ? 0 : 160)
  async function act(action: string, product: string | undefined, key: string) {
    const id = `${product ?? ''}${key}`
    if (picked[id]) return
    const send = async () => {
      const r = await run(newCmd('seguiti_azione', null, null, undefined, undefined, { action, product, key }))
      note = !r ? t.seguitiReadFailed : r.ok && action === 'allinea' ? t.seguitiTaskOpened(r.text) : r.text
      return !!r?.ok
    }
    if (action === 'allinea') { hooks.pick([id], action); const ok = await send(); if (!ok) hooks.pick([id], null); await load(); return }
    if (await leave([id], action, send, hooks)) void load()
  }
  const ageText = (i: RimastoItem) => {
    const a = age(i)
    if (a === 'aligned') return t.seguitiAligned
    if (a === 'unknown') return t.seguitiUnknown[i.why ?? ''] ?? t.seguitiUnknown.other
    const n = changedPaths(i)
    return n !== null ? t.seguitiBehindPaths(n) : i.why === 'check' ? t.seguitiBehindCheck : t.seguitiBehind
  }
  const alignedText = (i: RimastoItem) => { const x = [i.aligned_version, i.aligned_at ? whenText(i.aligned_at) : null].filter(Boolean).join(', '); return x ? t.seguitiLastAligned(x) : t.seguitiNeverAligned }
</script>

<div class="pr" data-testid="seguiti">
  <p class="m">{t.seguitiIntro}</p>
  {#if note}<p class="m" data-testid="seguiti-note">{note}</p>{/if}
  {#if failed}<p class="m">{t.seguitiReadFailed}</p>{/if}
  {#if !page}{#if !failed}<p class="m">{t.proveLoading}</p>{/if}
  {:else}
    <h3>{t.seguitiRimasto}</h3>
    {#if page.rimasto.length === 0}<p class="m">{t.seguitiRimastoEmpty}</p>{/if}
    {#each ordered(page.rimasto).filter(i => !gone[i.product + i.key]) as i (i.product + i.key)}
      <div class="c" data-testid={`rimasto-${i.key}`} out:slide={{ duration: outMs() }}>
        <b>{i.title || i.key}</b>
        <p class="m">{[i.product, i.where].filter(Boolean).join(' · ')}</p>
        <p>{ageText(i)}</p>
        <p class="m">{alignedText(i)}</p>
        {#if i.task}<p class="m">{t.seguitiTask(i.task)}</p>{/if}
        {#if canAccept(i) || canAlign(i)}<div class="ac"><span class="sp"></span>
          {#if canAccept(i)}<button class="out" class:done={picked[i.product + i.key] === 'ok'} disabled={!!picked[i.product + i.key]} onclick={() => act('ok', i.product, i.key)}>{#if picked[i.product + i.key] === 'ok'}✓ {/if}{t.seguitiAccept}</button>{/if}
          {#if canAlign(i)}<button class="tonal" class:done={picked[i.product + i.key] === 'allinea'} disabled={!!picked[i.product + i.key]} onclick={() => act('allinea', i.product, i.key)}>{#if picked[i.product + i.key] === 'allinea'}✓ {/if}{t.seguitiAlign}</button>{/if}
        </div>{/if}
      </div>
    {/each}
    <h3>{t.seguitiAltrove}</h3>
    {#if page.altrove.length === 0}<p class="m">{t.seguitiAltroveEmpty}</p>{/if}
    {#each page.altrove as r (r.id)}
      <div class="c" data-testid={`altrove-${r.id}`}>
        <b>{r.text}</b>
        <p class="m">{r.complete ? t.seguitiChecked(r.done, r.total) : t.seguitiPartial(r.done, r.total)}{r.at ? ` · ${whenText(r.at)}` : ''}</p>
        {#if r.running}<p class="m">{t.seguitiRunning}</p>{/if}
        {#if r.positive.length}<p class="m">{t.seguitiFoundIn}</p>{#each r.positive as x (x)}<p>{x}</p>{/each}{/if}
        {#if r.errors.length}<p class="m">{t.seguitiErrors}</p>{#each r.errors as x (x)}<p>{x}</p>{/each}{/if}
        {#if clean(r)}<p>{t.seguitiClean}</p>{/if}
        {#if r.task}<p>{t.seguitiCard(r.task)}</p><div class="ac"><span class="sp"></span><button class="text" onclick={onLavori}>{t.seguitiOpenLavori}</button></div>{/if}
      </div>
    {/each}
    <h3>{t.seguitiPromemoria}</h3>
    {#if page.promemoria.length === 0}<p class="m">{t.seguitiPromemoriaEmpty}</p>{/if}
    {#each page.promemoria.filter(p => !gone[p.id]) as p (p.id)}
      <div class="c" data-testid={`promemoria-${p.id}`} out:slide={{ duration: outMs() }}>
        <p>{p.text}</p>
        <p class="m">{p.at_time ? t.seguitiAtTime(whenText(p.at_time)) : t.seguitiUntil}</p>
        {#if p.expires_at}<p class="m">{t.seguitiExpires(whenText(p.expires_at))}</p>{/if}
        <div class="ac"><span class="sp"></span><button class="text" class:done={picked[p.id] === 'remind_remove'} disabled={!!picked[p.id]} onclick={() => act('remind_remove', undefined, p.id)}>{#if picked[p.id] === 'remind_remove'}✓ {/if}{t.seguitiRemove}</button></div>
      </div>
    {/each}
    <p class="m">{t.seguitiAddHint}</p>
    <div class="ac"><span class="sp"></span><button class="text" onclick={onMaster}>{t.seguitiWriteMaster}</button></div>
    {#if page.promemoria_scattati.length}
      <h3>{t.seguitiFiredTitle}</h3>
      {#each page.promemoria_scattati as p (p.id)}
        <div class="c"><p>{p.text}</p>{#if p.at}<p class="m">{fired(p) ? t.seguitiFired(whenText(p.at)) : t.seguitiExpired(whenText(p.at))}</p>{/if}</div>
      {/each}
    {/if}
  {/if}
</div>

<style>
  /* La conferma (voce 11), come in DaProvare. */
  .tonal, .out, .text { transition: background-color 150ms, color 150ms, transform var(--spring-bounce-t) var(--spring-bounce); }
  .tonal.done, .out.done { background: var(--good); color: var(--bg); border-color: transparent; transform: scale(1.06); }
  .text.done { color: var(--good); }
  .pr { padding: 0 12px 24px; max-width: 720px; display: flex; flex-direction: column; gap: 10px; }
  h3 { font-size: 16px; font-weight: 600; margin: 6px 4px 0; }
  .c { background: var(--low); border-radius: 20px; padding: 14px; display: flex; flex-direction: column; gap: 6px; }
  .c b { font-weight: 600; overflow-wrap: anywhere; }
  p { font-size: 14.5px; overflow-wrap: anywhere; }
  .m { color: var(--text2); }
  .ac { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; } .sp { flex: 1; }
  .tonal { background: var(--high); color: var(--text); border-radius: 999px; padding: 8px 14px; font-weight: 500; font-size: 14px; min-height: 44px; }
  .out { border: 1px solid var(--line); color: var(--text); border-radius: 999px; padding: 8px 14px; font-size: 14px; min-height: 44px; }
  .text { color: var(--icon); font-size: 14px; font-weight: 500; padding: 8px 4px; min-height: 44px; cursor: pointer; }
</style>
