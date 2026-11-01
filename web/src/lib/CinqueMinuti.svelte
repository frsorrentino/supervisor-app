<script lang="ts">
  import type { CmdResult, DecisionItem, DecisionsPage, State } from './contract'
  import { decodeDecisions } from './decideDecode'
  import { act, canDrop, nextDecisions } from './decide'
  import { newCmd } from './transport'
  import { t } from './t'
  // «Cinque minuti» (contratto 1.99, cinque_minuti), porta di CinqueMinutiScreen: quante vuoi, una alla volta, le più pesanti per prime. Niente timer.
  let { run, onApprove, onDone }: { st: State; run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>; onApprove: (task: string, note: string, choice?: string) => void; onDone: (i: DecisionItem) => void } = $props()
  let page = $state<DecisionsPage | null>(null)
  let howMany = $state<number | null>(null)
  let touched = $state(new Set<string>())
  let dropping = $state(false), reason = $state('')
  async function load() { const r = await run(newCmd('decisioni', null)); if (r?.ok) page = decodeDecisions(r.text) }
  $effect(() => { void load() })
  const g = $derived(page && howMany != null ? nextDecisions(page, touched, Math.max(0, howMany - touched.size))[0] ?? null : null)
  const next = (k: string) => { touched = new Set([...touched, k]) }
  async function decide(key: string, action: 'not_now' | 'drop', r?: string) { next(key); await run(newCmd('decisione', null, key, undefined, undefined, action === 'drop' ? { action, reason: r } : { action, hours: 24 })) }
</script>

<div class="cm" data-testid="cinque-minuti">
  {#if !page}<p class="m">{t.proveLoading}</p>
  {:else if !page.groups.length && !touched.size}<p class="m">{t.decisionsNone}</p>
  {:else if howMany == null}
    <div class="c"><p>{t.decisionsHead(page.n, page.here, page.tasks)}</p><p class="m">{t.decisionsHowMany}</p>
      <div class="ac">{#each [1, 3, 5].filter(n => n === 1 || n <= page!.groups.length) as n (n)}<button class="tonal" onclick={() => (howMany = n)}>{n}</button>{/each}<button class="tonal" onclick={() => (howMany = page!.groups.length)}>{t.decisionsAll}</button></div></div>
  {:else if !g}<p>{t.decisionsDone}</p>
  {:else}
    <span class="pos">{t.decisionsPosition(touched.size + 1, Math.min(howMany, page.groups.length))}</span>
    <div class="c">
      <b>{g.title}</b>
      <span class="pos" class:pc={g.where === 'pc'}>{g.where === 'pc' ? t.decisionsAtPc : t.decisionsHere}{g.weight > 1 ? ` · ${t.decisionsUnblocks(g.weight)}` : ''}</span>
      {#if g.note}<p class="m">{g.note}</p>{/if}
      {#each g.items as i, n (n)}
        <hr />
        <b class="i">{i.short ?? i.title ?? i.session ?? i.task}</b>
        {#if i.reason ?? i.what}<p class="m">{i.reason ?? i.what}</p>{/if}
        {#if act(i) === 'approve'}<div class="ac">{#if i.options?.length}{#each i.options as o (o)}<button class="tonal" onclick={() => { onApprove(i.task!, '', o); next(g.key) }}>{o}</button>{/each}{:else}<button class="tonal" onclick={() => { onApprove(i.task!, ''); next(g.key) }}>{t.approve}</button>{/if}</div>
        {:else if act(i) === 'done'}<div class="ac"><button class="tonal" onclick={() => { onDone(i); next(g.key) }}>{t.agendaDone}</button></div>{/if}
      {/each}
      <hr />
      <div class="ac"><button class="text" onclick={() => decide(g.key, 'not_now')}>{t.decisionsNotNow}</button><button class="text red" onclick={() => (dropping = true)}>{t.decisionsDrop}</button></div>
      {#if dropping}
        <p>{t.decisionsDropTitle}</p>
        <input bind:value={reason} placeholder={t.decisionsDropReason} />
        <div class="ac"><button class="text" onclick={() => (dropping = false)}>{t.cancel}</button><button class="text red" disabled={!canDrop(reason)} onclick={() => { decide(g.key, 'drop', reason.trim()); dropping = false; reason = '' }}>{t.decisionsDrop}</button></div>
      {/if}
    </div>
  {/if}
</div>

<style>
  .cm { padding: 0 12px 24px; max-width: 720px; display: flex; flex-direction: column; gap: 10px; }
  .c { background: var(--low); border-radius: 20px; padding: 14px; display: flex; flex-direction: column; gap: 8px; }
  .c b { font-weight: 600; } .i { font-size: 14.5px; }
  p { font-size: 14.5px; } .m { color: var(--text2); }
  .pos { font: 12px var(--mono); color: var(--text2); } .pc { color: var(--wait); }
  hr { border: 0; border-top: 1px solid var(--line); width: 100%; margin: 0; }
  .ac { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
  .tonal { background: var(--high); color: var(--text); border-radius: 999px; padding: 8px 14px; font-weight: 500; font-size: 14px; }
  .text { color: var(--icon); font-weight: 500; font-size: 14px; padding: 8px 4px; } .red { color: var(--gone); }
  input { width: 100%; box-sizing: border-box; background: var(--bg); color: var(--text); border: 1px solid var(--line); border-radius: 12px; padding: 8px; font: inherit; }
</style>
