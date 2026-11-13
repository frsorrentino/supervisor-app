<script lang="ts">
  import { getContext } from 'svelte'
  import { provaRefs, type ProvaRead } from './provaLinks'
  import { t } from './t'
  import Voce from './Voce.svelte'
  // Le prove citate nelle chat come schede compatte (app-prova-scheda-in-chat-11-11, Franz 11/11 19:49: «vorrei che fossero proprio delle
  // schede come queste, ma compatte»), porta di ProvaCards.kt. La stessa famiglia di Verifica: titolo, cosa fare in una riga, Non va e Ok;
  // con l'esito già dato l'esito al posto dei tasti; il clic sulla scheda apre la prova come il link. I dati dall'op `prove` col numero,
  // una lettura per scheda con l'etag e poi in cache (App), riletta solo dopo un tocco. Senza il contesto (provini) niente schede.
  // Dal formato unico (app-formato-unico-12-11, variante A di Franz del 12/11 alle 07:29) la scheda è una voce: icona, «Prova #N» col
  // titolo, cosa fare su una riga e il tasto principale, Ok; «Non va» e il resto stanno nella prova aperta, a un tocco.
  export type ProvaCardsHost = { read: (n: number) => ProvaRead | null | undefined; ask: (n: number) => void; open: (n: number) => void; ok: (n: number) => Promise<boolean>; ko: (n: number, why: string) => Promise<boolean> }
  let { text }: { text: string } = $props()
  const host = getContext<ProvaCardsHost | undefined>('prova-cards')
  const ns = $derived([...new Set(provaRefs(text).map(r => r.n))])
  $effect(() => { for (const n of ns) host?.ask(n) })
  let sent = $state<Record<number, 'ok'>>({})
  async function ok(n: number) { sent[n] = 'ok'; if (!(await host!.ok(n))) delete sent[n] }
</script>

{#if host}
  {#each ns as n (n)}
    {@const r = host.read(n)}
    {#if r == null || r.kind === 'found'}
      {@const p = r?.kind === 'found' ? r.prova : null}
      {@const done = p && p.state !== 'todo' ? p.state : sent[n] ?? null}
      <Voce card group="prova" testid={`prova-scheda-${n}`} title={p ? `${t.provaTitle(n)} · ${p.title ?? p.short ?? p.task}` : t.provaTitle(n)}
        sub={!p ? t.provaLoading : done ? [done === 'ok' ? t.provaStateOk : done === 'red' ? t.provaStateRed : t.provaStateKo, p.text].filter(Boolean).join(': ') : [t.perTeGroup.prova, p.fai].filter(Boolean).join(' · ')}
        tone={done ? (done === 'ok' ? 'good' : 'red') : null} main={p && !done ? t.proveOk : null} onOpen={() => host.open(n)} onMain={() => void ok(n)} />
    {/if}
  {/each}
{/if}
