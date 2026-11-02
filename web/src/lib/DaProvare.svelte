<script lang="ts">
  import type { CmdResult, ProvePage } from './contract'
  import { decodeProve } from './decideDecode'
  import { allOk, screen } from './decide'
  import { newCmd } from './transport'
  import { t } from './t'
  import { flip } from 'svelte/animate'
  import { slide } from 'svelte/transition'
  import { leave } from './leave'
  import { bounceEase, bounceMs, reducedMotion } from './motion'
  // «Da provare» (contratto 1.99, banco_prova), porta di DaProvareScreen. Sul web vale l'ultima versione online: nessuna prova aspetta un'installazione.
  let { run, onScreen, onShot }: { run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>; onScreen: (s: string) => void; onShot: (f: File, note: string) => void } = $props()
  let page = $state<ProvePage | null>(null)
  let ko = $state<ProvePage['groups'][number]['prove'][number] | null>(null)
  let text = $state(''), shot = $state<File | null>(null)
  async function load() { const r = await run(newCmd('prove', null)); if (r?.ok) page = decodeProve(r.text) }
  $effect(() => { void load() })
  // Franz 02/11 15:38: Ok non blocca più e la scheda non sparisce di colpo (leave.ts): conferma sul tasto, uscita in altezza,
  // le altre salgono; la pagina si ricarica dopo, in silenzio. Se il PC dice no, la scheda torna.
  let picked = $state<Record<string, string>>({})
  let gone = $state<Record<string, true>>({})
  const hooks = {
    pick: (ids: string[], v: string | null) => { const n = { ...picked }; for (const i of ids) { if (v) n[i] = v; else delete n[i] } picked = n },
    hide: (ids: string[], v: boolean) => { const n = { ...gone }; for (const i of ids) { if (v) n[i] = true; else delete n[i] } gone = n },
  }
  const send = (arg: string, res: 'ok' | 'ko', body?: string) => run(newCmd('prova', null, arg, body, undefined, { result: res })).then(r => !!r?.ok)
  async function result(ids: string[], arg: string, res: 'ok' | 'ko', body?: string) { if (await leave(ids, res, () => send(arg, res, body), hooks)) void load() }
  const shown = (g: ProvePage['groups'][number]) => g.prove.filter(p => !gone[String(p.id)])
  const outMs = () => (reducedMotion() ? 0 : 160)
  async function sendKo() { const p = ko!; const b = text.trim(), f = shot; ko = null; text = ''; shot = null; await result([String(p.id)], String(p.id), 'ko', b); if (f) onShot(f, t.proveKoShotNote(p.short ?? p.task)) }
</script>

<div class="dp" data-testid="da-provare">
  {#if !page}<p class="m">{t.proveLoading}</p>
  {:else if !page.groups.length && !page.ko.length && !page.red.length}<p class="m">{t.proveNone}</p>{/if}
  {#each (page?.groups ?? []).filter(g => shown(g).length) as g (g.build)}
    <div class="h" out:slide={{ duration: outMs() }}><span>{g.build}</span>{#if shown(g).length > 1}<button class="tonal" class:done={g.prove.every(p => picked[String(p.id)] === 'ok')} onclick={() => result(g.prove.map(p => String(p.id)), allOk(g), 'ok')}>{#if g.prove.every(p => picked[String(p.id)] === 'ok')}✓ {/if}{t.proveAllOk}</button>{/if}</div>
    {#each shown(g) as p (p.id)}
      {@const pk = picked[String(p.id)]}
      <div class="c" class:picked={!!pk} out:slide={{ duration: outMs() }} animate:flip={{ duration: bounceMs(), easing: bounceEase }}>
        <b>{p.short ?? p.title ?? p.task}</b><p>{p.fai}</p><p class="m">{t.proveExpected(p.atteso)}</p>
        <div class="ac">
          {#if screen(p.dove)}<button class="text" onclick={() => onScreen(screen(p.dove)!)}>{t.featuresGo}</button>{/if}
          <span class="sp"></span>
          <button class="out" class:fade={pk === 'ok'} disabled={!!pk} onclick={() => (ko = p)}>{t.proveKo}</button><button class="tonal" class:done={pk === 'ok'} class:fade={pk === 'ko'} disabled={!!pk} onclick={() => result([String(p.id)], String(p.id), 'ok')}>{#if pk === 'ok'}✓ {/if}{t.proveOk}</button>
        </div>
      </div>
    {/each}
  {/each}
  {#if page?.ko.length}<div class="h"><span>{t.proveReported}</span></div>{#each page.ko as k (k.id)}<div class="c"><b>{k.short ?? k.task}</b><p class="m">{k.text}</p></div>{/each}{/if}
  {#if page?.red.length}<div class="h"><span>{t.proveRed}</span></div>{#each page.red as r (r.task)}<div class="c"><b>{r.short ?? r.task}</b><p class="r">{r.reason ?? t.proveRedRunning}</p></div>{/each}{/if}
  {#if page?.groups.length}<p class="m small">{t.proveWeb}</p>{/if}
  {#if ko}
    <div class="c ko">
      <b>{ko.short ?? ko.task}</b>
      <textarea bind:value={text} maxlength="2000" rows="3" placeholder={t.proveKoHint}></textarea>
      <label class="text">{shot ? shot.name : t.proveKoShot}<input type="file" accept="image/*" hidden onchange={(e) => { shot = (e.currentTarget as HTMLInputElement).files?.[0] ?? null }} /></label>
      <div class="ac"><span class="sp"></span><button class="text" onclick={() => (ko = null)}>{t.cancel}</button><button class="tonal" disabled={!text.trim()} onclick={sendKo}>{t.proveKoSend}</button></div>
    </div>
  {/if}
</div>

<style>
  .dp { padding: 0 12px 24px; max-width: 720px; display: flex; flex-direction: column; gap: 10px; }
  .h { display: flex; align-items: center; justify-content: space-between; padding: 8px 4px 0; font: 12px var(--mono); color: var(--text2); }
  .c { background: var(--low); border-radius: 20px; padding: 14px; display: flex; flex-direction: column; gap: 6px; }
  .c b { font-weight: 600; }
  p { font-size: 14.5px; }
  .m { color: var(--text2); } .r { color: var(--gone); } .small { font-size: 13px; }
  .ac { display: flex; align-items: center; gap: 8px; } .sp { flex: 1; }
  .tonal { background: var(--high); color: var(--text); border-radius: 999px; padding: 8px 14px; font-weight: 500; font-size: 14px; white-space: nowrap; }
  .out { border: 1px solid var(--line); color: var(--text); border-radius: 999px; padding: 8px 14px; font-size: 14px; white-space: nowrap; }
  /* La conferma (voce 11): il tasto scelto si accende e cresce un poco, gli altri sfumano; la scheda si schiarisce. */
  .tonal, .out { transition: background-color 150ms, color 150ms, opacity 150ms, transform var(--spring-bounce-t) var(--spring-bounce); }
  .done { background: var(--good); color: var(--bg); transform: scale(1.06); }
  .fade { opacity: .35; }
  button:disabled { cursor: default; }
  .c { transition: background-color 150ms; }
  .c.picked { background: var(--high); }
  .out:active { transform: scale(.97); transition-duration: 100ms; }
  .text { color: var(--icon); font-size: 14px; font-weight: 500; padding: 8px 4px; cursor: pointer; }
  textarea { width: 100%; box-sizing: border-box; background: var(--bg); color: var(--text); border: 1px solid var(--line); border-radius: 12px; padding: 8px; font: inherit; }
</style>
