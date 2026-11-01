<script lang="ts">
  import type { CmdResult, ProvePage } from './contract'
  import { decodeProve } from './decideDecode'
  import { allOk, screen } from './decide'
  import { newCmd } from './transport'
  import { t } from './t'
  // «Da provare» (contratto 1.99, banco_prova), porta di DaProvareScreen. Sul web vale l'ultima versione online: nessuna prova aspetta un'installazione.
  let { run, onScreen, onShot }: { run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>; onScreen: (s: string) => void; onShot: (f: File, note: string) => void } = $props()
  let page = $state<ProvePage | null>(null)
  let ko = $state<ProvePage['groups'][number]['prove'][number] | null>(null)
  let text = $state(''), shot = $state<File | null>(null)
  async function load() { const r = await run(newCmd('prove', null)); if (r?.ok) page = decodeProve(r.text) }
  $effect(() => { void load() })
  async function result(arg: string, res: 'ok' | 'ko', body?: string) { await run(newCmd('prova', null, arg, body, undefined, { result: res })); await load() }
  async function sendKo() { const p = ko!; const b = text.trim(), f = shot; ko = null; text = ''; shot = null; await result(String(p.id), 'ko', b); if (f) onShot(f, t.proveKoShotNote(p.short ?? p.task)) }
</script>

<div class="dp" data-testid="da-provare">
  {#if !page}<p class="m">{t.proveLoading}</p>
  {:else if !page.groups.length && !page.ko.length && !page.red.length}<p class="m">{t.proveNone}</p>{/if}
  {#each page?.groups ?? [] as g (g.build)}
    <div class="h"><span>{g.build}</span>{#if g.prove.length > 1}<button class="tonal" onclick={() => result(allOk(g), 'ok')}>{t.proveAllOk}</button>{/if}</div>
    {#each g.prove as p (p.id)}
      <div class="c">
        <b>{p.short ?? p.title ?? p.task}</b><p>{p.fai}</p><p class="m">{t.proveExpected(p.atteso)}</p>
        <div class="ac">
          {#if screen(p.dove)}<button class="text" onclick={() => onScreen(screen(p.dove)!)}>{t.featuresGo}</button>{/if}
          <span class="sp"></span>
          <button class="out" onclick={() => (ko = p)}>{t.proveKo}</button><button class="tonal" onclick={() => result(String(p.id), 'ok')}>{t.proveOk}</button>
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
  .text { color: var(--icon); font-size: 14px; font-weight: 500; padding: 8px 4px; cursor: pointer; }
  textarea { width: 100%; box-sizing: border-box; background: var(--bg); color: var(--text); border: 1px solid var(--line); border-radius: 12px; padding: 8px; font: inherit; }
</style>
