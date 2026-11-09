<script lang="ts">
  import { isLook, scale, yOf, type Monitor } from './monitors'
  import { t } from './t'
  // La sezione Monitoraggi nella home (app-monitoraggi-09-11, mockup A): una riga per monitoraggio come le righe delle sessioni,
  // pallino (verde, ambra se da guardare), nome e frase sotto, a destra il valore con la sparkline; in fondo «Nuovo monitoraggio».
  // Come MonitorsCard in Kotlin.
  let { items, onOpen, onNew }: { items: Monitor[]; onOpen: (id: string) => void; onNew: () => void } = $props()
  const spark = (vs: number[]) => {
    const s = scale(vs)
    return vs.map((v, i) => `${i ? 'L' : 'M'}${((i * 60) / Math.max(1, vs.length - 1) + 2).toFixed(1)} ${((1 - yOf(s, v)) * 16 + 2).toFixed(1)}`).join(' ')
  }
</script>

<div class="mlist">
  {#each items as m (m.id)}
    <button class="row" data-testid="monitor-{m.id}" onclick={() => onOpen(m.id)}>
      <span class="rail"><i class:look={isLook(m)}></i></span>
      <span class="tx"><b>{m.name}</b>{#if m.text}<small class:look={isLook(m)}>{m.text}</small>{/if}</span>
      <span class="val">
        {#if m.value}<b>{m.value}</b>{/if}
        {#if (m.spark?.length ?? 0) >= 2}{@const vs = m.spark!}{@const s = scale(vs)}
          <svg viewBox="0 0 64 20" width="64" height="20" aria-hidden="true"><path d={spark(vs)} /><circle cx="62" cy={(1 - yOf(s, vs[vs.length - 1])) * 16 + 2} r="2.4" /></svg>
        {/if}
      </span>
    </button>
  {/each}
  <button class="row new" data-testid="monitor-nuovo" onclick={onNew}>
    <span class="rail"><svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"><path d="M12 5v14M5 12h14" /></svg></span>
    <span class="nw"><b>{t.monitorsNew}</b><small> · {t.monitorsNewHint}</small></span>
  </button>
</div>

<style>
  .mlist { border-radius: 14px; background: var(--s1); overflow: hidden; }
  .row { display: flex; align-items: center; width: 100%; min-height: 60px; padding: 10px 12px 10px 0; text-align: left; border-top: 1px solid var(--line); }
  .row:first-child { border-top: 0; }
  .rail { width: 52px; flex: none; display: grid; place-items: center; }
  .rail i { width: 9px; height: 9px; border-radius: 50%; background: var(--good); }
  .rail i.look { background: var(--wait); }
  .rail svg path { fill: none; stroke: var(--tx2); stroke-width: 2; stroke-linecap: round; }
  .tx { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; margin-left: 8px; }
  .tx b { font: 600 17px/20px var(--display); color: var(--tx); white-space: nowrap; overflow: hidden; }
  .tx small { font-size: 14px; line-height: 19px; color: var(--tx2); }
  .tx small.look { color: var(--wait); }
  .val { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; margin-left: 10px; }
  .val b { font: 700 17px/20px var(--display); color: var(--tx); white-space: nowrap; }
  .val svg path { fill: none; stroke: var(--tx2); stroke-width: 1.5; stroke-linecap: round; stroke-linejoin: round; }
  .val svg circle { fill: var(--tx); }
  .new { min-height: 48px; }
  .nw { flex: 1; min-width: 0; margin-left: 8px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .nw b { font: 600 15px var(--display); color: var(--tx); }
  .nw small { font-size: 14px; color: var(--tx2); }
</style>
