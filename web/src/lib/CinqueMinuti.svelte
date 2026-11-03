<script lang="ts">
  import type { CmdResult, DecisionGroup, DecisionItem, DecisionsPage, State } from './contract'
  import { decodeDecisions, readPage } from './decideDecode'
  import { buttons, canDrop, card as cardText, nextDecisions, viaApprove } from './decide'
  import { newCmd } from './transport'
  import { t } from './t'
  import LoadFailed from './LoadFailed.svelte'
  import { confirmMs } from './leave'
  import { pass } from './motion'
  import ItemList from './ItemList.svelte'
  import { decision } from './itemRows'
  // «Da decidere» (contratto 1.99, cinque_minuti), porta di CinqueMinutiScreen: quante vuoi, una alla volta, le più pesanti per prime. Niente timer.
  // In home (`rows`, app-richiede-te-righe-02-11): le decisioni come righe, le prime tre e «altre N»; il tocco apre la scheda coi tasti.
  let { run, onApprove, onDone, onAsk, rows = false }: { rows?: boolean; st: State; run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>; onApprove: (task: string, note: string, choice?: string) => void; onDone: (i: DecisionItem) => void; onAsk: (text: string) => void } = $props()
  let planOpen = $state(new Set<string>())
  let page = $state<DecisionsPage | null>(null)
  let howMany = $state<number | null>(null)
  let touched = $state(new Set<string>())
  let dropping = $state(false), reason = $state('')
  // Senza risposta in tempo la pagina lo dice e offre Riprova, invece di restare «In arrivo dal PC» (app-pwa-decisioni-03-11).
  let failed = $state(false)
  async function load() { failed = false; ({ page, failed } = readPage(await run(newCmd('decisioni', null)), decodeDecisions, page)) }
  $effect(() => { void load() })
  const g = $derived(page && howMany != null ? nextDecisions(page, touched, Math.max(0, howMany - touched.size))[0] ?? null : null)
  const next = (k: string) => { touched = new Set([...touched, k]) }
  // Franz 02/11 15:38 (app-feedback-azioni-02-11): il tasto scelto diventa la conferma e la scheda dopo entra con la sua
  // transizione, invece del cambio di colpo. Il comando parte subito; la scheda cambia dopo la conferma (leave.ts, confirmMs).
  let picked = $state<string | null>(null)
  function act(id: string, key: string, send: () => void) { if (picked) return; picked = id; send(); setTimeout(() => { next(key); picked = null }, confirmMs()) }
  async function decide(key: string, action: 'not_now' | 'drop', r?: string) { act(action, key, () => {}); await run(newCmd('decisione', null, key, undefined, undefined, action === 'drop' ? { action, reason: r } : { action, hours: 24 })) }
</script>

<div class="cm" data-testid="cinque-minuti">
  {#if !page}{#if failed}<LoadFailed onRetry={() => void load()} />{:else}<p class="m">{t.proveLoading}</p>{/if}
  {:else if !page.groups.length && !touched.size}<p class="m">{t.decisionsNone}</p>
  {:else if rows}
    {@const left = page.groups.filter(x => !touched.has(x.key))}
    {#if !left.length}<p>{t.decisionsDone}</p>{/if}
    <ItemList rows={left.map(decision)} testid="decisione">{#snippet card(r)}{@const x = left.find(y => y.key === r.key)}{#if x}<div class="in">{@render body(x, false)}</div>{/if}{/snippet}</ItemList>
  {:else if howMany == null}
    <div class="c"><p>{t.decisionsHead(page.n, page.here, page.tasks)}</p><p class="m">{t.decisionsHowMany}</p>
      <div class="ac">{#each [1, 3, 5].filter(n => n === 1 || n <= page!.groups.length) as n (n)}<button class="tonal" onclick={() => (howMany = n)}>{n}</button>{/each}<button class="tonal" onclick={() => (howMany = page!.groups.length)}>{t.decisionsAll}</button></div></div>
  {:else if !g}<p>{t.decisionsDone}</p>
  {:else}
    <span class="pos">{t.decisionsPosition(touched.size + 1, Math.min(howMany, page.groups.length))}</span>
    {#key g.key}
    <div class="c" in:pass>{@render body(g, true)}</div>
    {/key}
  {/if}
</div>

{#snippet body(g: DecisionGroup, head: boolean)}
  {@const cardOf = cardText(g)}
      <!-- Contratto 1.104 (Franz 02/11 08:04): la card si legge senza ricordare niente; mai lo stesso testo due volte. -->
      {#each cardOf.head as h, n (n)}{#if head}{#if n === 0}<b>{h}</b>{:else}<p class="m">{h}</p>{/if}{:else if h !== g.title.trim()}<p class="m">{h}</p>{/if}{/each}
      <!-- Nella riga aperta dove e quanto sblocca stanno già sulla riga. -->
      {#if head}<span class="pos" class:pc={g.where === 'pc'}>{g.where === 'pc' ? t.decisionsAtPc : t.decisionsHere}{g.weight > 1 ? ` · ${t.decisionsUnblocks(g.weight)}` : ''}</span>{/if}
      {#each g.items as i, n (n)}
        {@const x = cardOf.items[n]}
        {@const name = i.title ?? i.short ?? i.session ?? i.task ?? ''}
        {@const ref = i.task ?? i.session ?? ''}
        <hr />
        {#if x.title}<b class="i">{x.title}</b>{/if}
        {#if x.plan}{@const k = `${g.key}-${n}`}<p class="plan" class:folded={!planOpen.has(k)}>{x.plan}</p>{#if x.plan.length > 160}<button class="text" onclick={() => { const s = new Set(planOpen); if (s.has(k)) s.delete(k); else s.add(k); planOpen = s }}>{planOpen.has(k) ? t.permissionLess : t.permissionMore}</button>{/if}{/if}
        {#if x.reason}<p class="m">{x.reason}</p>{/if}
        {#if buttons(i).length}<div class="ac">{#each buttons(i) as btn, bi (bi)}
          {@const id = `${n}-${bi}`}{#if btn.kind === 'proceed'}<button class="tonal" class:done={picked === id} class:fade={!!picked && picked !== id} onclick={() => act(id, g.key, () => { if (viaApprove(i)) onApprove(i.task!, ''); else onAsk(t.decisionsProceedPrompt(name, ref)) })}>{#if picked === id}✓ {/if}{t.decisionsProceed}</button>
          {:else if btn.kind === 'discuss'}<button class="out" class:done={picked === id} class:fade={!!picked && picked !== id} onclick={() => act(id, g.key, () => onAsk(t.sugDiscussPrompt(t.decisionsDiscussAbout(name, ref))))}>{#if picked === id}✓ {/if}{t.decisionsDiscuss}</button>
          {:else if btn.kind === 'done'}<button class="tonal" class:done={picked === id} class:fade={!!picked && picked !== id} onclick={() => act(id, g.key, () => onDone(i))}>{#if picked === id}✓ {/if}{t.agendaDone}</button>
          {:else}<button class="tonal" class:done={picked === id} class:fade={!!picked && picked !== id} onclick={() => act(id, g.key, () => { if (viaApprove(i)) onApprove(i.task!, '', btn.label); else onAsk(`${t.decisionsChoicePrompt(name, ref)} ${btn.label}`) })}>{#if picked === id}✓ {/if}{btn.label}</button>{/if}
        {/each}</div>{/if}
      {/each}
      <hr />
      <div class="ac"><button class="text" class:done={picked === 'not_now'} onclick={() => decide(g.key, 'not_now')}>{#if picked === 'not_now'}✓ {/if}{t.decisionsNotNow}</button><button class="text red" onclick={() => (dropping = true)}>{t.decisionsDrop}</button></div>
      {#if dropping}
        <p>{t.decisionsDropTitle}</p>
        <input bind:value={reason} placeholder={t.decisionsDropReason} />
        <div class="ac"><button class="text" onclick={() => (dropping = false)}>{t.cancel}</button><button class="text red" disabled={!canDrop(reason)} onclick={() => { decide(g.key, 'drop', reason.trim()); dropping = false; reason = '' }}>{t.decisionsDrop}</button></div>
      {/if}
{/snippet}

<style>
  /* La conferma (voce 11), come in DaProvare: il tasto scelto si accende e cresce un poco, gli altri sfumano. */
  .tonal, .out, .text { transition: background-color 150ms, color 150ms, opacity 150ms, transform var(--spring-bounce-t) var(--spring-bounce); }
  .tonal.done, .out.done { background: var(--good); color: var(--bg); border-color: transparent; transform: scale(1.06); }
  .text.done { color: var(--good); }
  .fade { opacity: .35; }
  .cm { padding: 0 12px 24px; max-width: 720px; display: flex; flex-direction: column; gap: 10px; }
  .c { background: var(--low); border-radius: 20px; padding: 14px; display: flex; flex-direction: column; gap: 8px; }
  .in { display: flex; flex-direction: column; gap: 8px; }
  .c b { font-weight: 600; } .i { font-size: 14.5px; }
  p { font-size: 14.5px; } .m { color: var(--text2); }
  .pos { font: 12px var(--mono); color: var(--text2); } .pc { color: var(--wait); }
  hr { border: 0; border-top: 1px solid var(--line); width: 100%; margin: 0; }
  .ac { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
  .tonal { background: var(--high); color: var(--text); border-radius: 999px; padding: 8px 14px; font-weight: 500; font-size: 14px; }
  .text { color: var(--icon); font-weight: 500; font-size: 14px; padding: 8px 4px; } .red { color: var(--gone); }
  .plan { font-size: 14.5px; white-space: pre-wrap; overflow-wrap: anywhere; } .plan.folded { display: -webkit-box; -webkit-line-clamp: 4; line-clamp: 4; -webkit-box-orient: vertical; overflow: hidden; }
  .out { border: 1px solid var(--line); color: var(--text); border-radius: 999px; padding: 8px 14px; font-weight: 500; font-size: 14px; }
  input { width: 100%; box-sizing: border-box; background: var(--bg); color: var(--text); border: 1px solid var(--line); border-radius: 12px; padding: 8px; font: inherit; }
</style>
