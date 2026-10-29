<script lang="ts">
  import { locale } from './lang'
  // Colonne per giorno (proposte 9 e 17): l'etichetta sopra, le parti dal basso, il giorno della settimana sotto.
  type Col = { date: string; parts: { f: number; color: string }[]; label: string; red?: number }
  let { cols, aria }: { cols: Col[]; aria: string } = $props()
  const H = 56
  const fmt = new Intl.DateTimeFormat(locale, { weekday: 'short', timeZone: 'UTC' })
  const wd = (d: string) => { const x = new Date(`${d}T12:00:00Z`); return isNaN(x.getTime()) ? d.slice(-2) : fmt.format(x).replace(/\.$/, '') }
</script>

<div class="days" role="img" aria-label={aria}>
  {#each cols as c (c.date)}
    <div class="col">
      <small class="v">{c.label}{#if c.red}<em> {c.red}</em>{/if}</small>
      <span class="stack">
        {#each c.parts.slice().reverse() as p, i (i)}{#if p.f > 0}<i style:height="{Math.max(2, p.f * H)}px" style:background={p.color}></i>{/if}{/each}
        {#if c.parts.every(p => p.f <= 0)}<i class="none"></i>{/if}
      </span>
      <small>{wd(c.date)}</small>
    </div>
  {/each}
</div>

<style>
  .days { display: flex; align-items: flex-end; gap: 4px; }
  .col { flex: 1; min-width: 0; display: flex; flex-direction: column; align-items: stretch; gap: 3px; }
  .stack { display: flex; flex-direction: column; justify-content: flex-end; height: 56px; }
  .stack i { display: block; border-radius: 2px; }
  .stack i.none { height: 2px; background: var(--b-track); }
  small { font: 10px/1.2 var(--mono); color: var(--text2); text-align: center; white-space: nowrap; overflow: hidden; }
  em { font-style: normal; color: var(--b-alert); }
</style>
