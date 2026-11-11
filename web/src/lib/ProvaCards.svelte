<script lang="ts">
  import { getContext } from 'svelte'
  import { provaRefs, type ProvaRead } from './provaLinks'
  import { t } from './t'
  import Check from './Check.svelte'
  // Le prove citate nelle chat come schede compatte (app-prova-scheda-in-chat-11-11, Franz 11/11 19:49: «vorrei che fossero proprio delle
  // schede come queste, ma compatte»), porta di ProvaCards.kt. La stessa famiglia di Verifica: titolo, cosa fare in una riga, Non va e Ok;
  // con l'esito già dato l'esito al posto dei tasti; il clic sulla scheda apre la prova come il link. I dati dall'op `prove` col numero,
  // una lettura per scheda con l'etag e poi in cache (App), riletta solo dopo un tocco. Senza il contesto (provini) niente schede.
  export type ProvaCardsHost = { read: (n: number) => ProvaRead | null | undefined; ask: (n: number) => void; open: (n: number) => void; ok: (n: number) => Promise<boolean>; ko: (n: number, why: string) => Promise<boolean> }
  let { text }: { text: string } = $props()
  const host = getContext<ProvaCardsHost | undefined>('prova-cards')
  const ns = $derived([...new Set(provaRefs(text).map(r => r.n))])
  $effect(() => { for (const n of ns) host?.ask(n) })
  let sent = $state<Record<number, 'ok' | 'ko'>>({})
  let asking = $state<number | null>(null), why = $state('')
  async function ok(n: number) { sent[n] = 'ok'; if (!(await host!.ok(n))) delete sent[n] }
  async function ko(n: number) { const body = why.trim(); if (!body) return; asking = null; why = ''; sent[n] = 'ko'; if (!(await host!.ko(n, body))) delete sent[n] }
</script>

{#if host}
  {#each ns as n (n)}
    {@const r = host.read(n)}
    {#if r == null || r.kind === 'found'}
      {@const p = r?.kind === 'found' ? r.prova : null}
      <div class="pc" role="button" tabindex="0" data-testid={`prova-scheda-${n}`} onclick={() => host.open(n)} onkeydown={(e) => { if (e.key === 'Enter') host.open(n) }}>
        <div class="top"><span class="dot {p ? (p.state === 'ok' ? 'good' : p.state === 'todo' ? 'wait' : 'red') : ''}"></span><b>{p ? (p.title ?? p.short ?? p.task) : t.provaTitle(n)}</b></div>
        {#if !p}<span class="m">{t.provaLoading}</span>
        {:else}
          <span class="m one">{[t.provaTitle(n), [p.short, p.task].find(x => x && x !== (p.title ?? p.short ?? p.task))].filter(Boolean).join(' · ')}</span>
          {#if p.fai}<span class="fai one">{p.fai}</span>{/if}
          {#if p.state === 'todo'}
            {#if asking === n}
              <!-- svelte-ignore a11y_click_events_have_key_events, a11y_no_static_element_interactions -->
              <div class="why" onclick={(e) => e.stopPropagation()}>
                <textarea rows="2" placeholder={t.proveKoHint} bind:value={why}></textarea>
                <div class="ac"><button class="text" onclick={() => { asking = null; why = '' }}>{t.cancel}</button><button class="tonal" disabled={!why.trim()} onclick={() => void ko(n)}>{t.proveKoSend}</button></div>
              </div>
            {:else}
              <div class="ac">
                <button class="out" class:fade={sent[n] === 'ok'} disabled={!!sent[n]} onclick={(e) => { e.stopPropagation(); asking = n }}>{t.proveKo}</button>
                <button class="tonal ck" class:done={sent[n] === 'ok'} class:fade={sent[n] === 'ko'} disabled={!!sent[n]} onclick={(e) => { e.stopPropagation(); void ok(n) }}><span class="lbl">{t.proveOk}</span>{#if sent[n] === 'ok'}<Check />{/if}</button>
              </div>
            {/if}
          {:else}
            <span class="res {p.state === 'ok' ? 'good' : 'red'}">{[p.state === 'ok' ? t.provaStateOk : p.state === 'red' ? t.provaStateRed : t.provaStateKo, p.text].filter(Boolean).join(': ')}</span>
          {/if}
        {/if}
      </div>
    {/if}
  {/each}
{/if}

<style>
  .pc { display: flex; flex-direction: column; gap: 4px; margin-top: 8px; padding: 10px 12px; border-radius: 14px; background: var(--s1); border: 1px solid var(--line); cursor: pointer; text-align: left; }
  .top { display: flex; align-items: center; gap: 8px; }
  .top b { font: 600 17px/21px var(--display); color: var(--tx); }
  .dot { flex: none; width: 8px; height: 8px; border-radius: 50%; background: var(--tx3); }
  .dot.wait { background: var(--wait); } .dot.good { background: var(--good); } .dot.red { background: var(--red); }
  .m { font-size: 13px; color: var(--tx2); }
  .fai { font-size: 15px; color: var(--tx); }
  /* Una riga sola, tagliata senza «…» (regole di forma, Franz 12/09): il testo intero sta nella prova aperta. */
  .one { white-space: nowrap; overflow: hidden; text-overflow: clip; }
  .res { font-size: 13px; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; text-overflow: clip; }
  .res.good { color: var(--good); } .res.red { color: var(--red); }
  .ac { display: flex; justify-content: flex-end; align-items: center; gap: 8px; margin-top: 4px; }
  .why { display: flex; flex-direction: column; gap: 6px; margin-top: 4px; cursor: default; }
  .why textarea { width: 100%; box-sizing: border-box; padding: 8px 10px; border-radius: 10px; border: 1px solid var(--line); background: var(--bg); color: var(--tx); font: inherit; resize: vertical; }
  .tonal { background: var(--high, var(--s3)); color: var(--tx); border-radius: 999px; padding: 8px 14px; font-size: 14px; white-space: nowrap; display: inline-flex; align-items: center; gap: 6px; }
  .tonal.done { background: color-mix(in srgb, var(--good) 22%, var(--s1)); color: var(--good); }
  .out { border: 1px solid var(--line); color: var(--tx); border-radius: 999px; padding: 8px 14px; font-size: 14px; white-space: nowrap; }
  .text { color: var(--tx2); padding: 8px 10px; font-size: 14px; }
  .fade { opacity: .35; }
  button:disabled { cursor: default; }
</style>
