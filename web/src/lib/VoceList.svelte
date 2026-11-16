<script lang="ts">
  import type { CmdResult, WaitItem } from './contract'
  import type { newCmd } from './transport'
  import { voceOf } from './perTe'
  import { age } from './tiAspetta'
  import { answerWait } from './waitAct'
  import { t } from './t'
  import Voce from './Voce.svelte'
  // Le voci di `attese` in Dalla master, di tutte le sessioni (app-riall-1-dati-16-11, contratto 1.151; Franz 16/11: le voci per
  // intero, coi tasti): la stessa riga di «Per te», col nome della sessione davanti al contesto. Il tocco apre la scheda Ti aspetta, il
  // tasto principale risponde e la voce esce da ogni vista; un tasto che chiede un testo apre la scheda.
  let { run, items, now, testid, onOpenWait, onAnswered = () => {} }: {
    run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>; items: WaitItem[]; now: number; testid: string
    onOpenWait: (id: string) => void; onAnswered?: (w: WaitItem) => void
  } = $props()
  const vs = $derived(items.map(voceOf))
  const sub = (v: ReturnType<typeof voceOf>) => [v.wait?.session ?? null, t.perTeGroup[v.group], v.context].filter(Boolean).join(' · ')
  function main(v: ReturnType<typeof voceOf>) {
    const w = v.wait
    if (!w || !v.main?.direct) { onOpenWait(v.key); return }
    void answerWait(run, w, v.main.index).then(ok => { if (ok) onAnswered(w) })
  }
</script>

{#if vs.length}
  <div class="vl" data-testid={testid}>
    {#each vs as v (v.key)}
      <Voce group={v.group} title={v.title} sub={sub(v)} ago={v.group === 'aspetta' ? t.tiAspettaSince(age(now - v.since)) : age(now - v.since)} main={v.main?.label ?? null}
        testid="voce-{v.key}" onOpen={() => onOpenWait(v.key)} onMain={() => main(v)} />
    {/each}
  </div>
{/if}

<style>
  .vl { border-radius: 14px; background: var(--s1); border: 1px solid var(--line); overflow: hidden; }
  .vl > :global(.voce:first-child) { border-top: 0; }
</style>
