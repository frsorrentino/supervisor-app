<script lang="ts">
  import { HOLD_MS } from './animazioni'
  // Voce 6 di app-animazioni-otto-26-11 (Tieni premuto per la produzione): il tasto si riempie mentre il dito (o il tasto della
  // tastiera) resta giù, e l'ok parte solo quando è pieno, dopo HOLD_MS; lasciato prima torna vuoto e non succede niente. Il
  // riempimento è lineare perché è un tempo da leggere; con le animazioni ridotte non si vede, ma il tempo da tenere resta.
  let { label, onDone, cls = '' }: { label: string; onDone: () => void; cls?: string } = $props()
  let down = $state(false)
  let timer: ReturnType<typeof setTimeout> | undefined
  function start(e: Event) {
    if (down) return
    e.preventDefault()
    down = true
    timer = setTimeout(() => { down = false; onDone() }, HOLD_MS)
  }
  function stop() { clearTimeout(timer); down = false }
  $effect(() => () => clearTimeout(timer))
</script>

<button class="hold {cls}" class:down style:--hold="{HOLD_MS}ms" data-testid="tieni-premuto"
  onpointerdown={start} onpointerup={stop} onpointerleave={stop} onpointercancel={stop} oncontextmenu={(e) => e.preventDefault()}
  onkeydown={(e) => { if ((e.key === 'Enter' || e.key === ' ') && !e.repeat) start(e) }} onkeyup={stop} onblur={stop}>
  <span class="fill" aria-hidden="true"></span><span class="lab">{label}</span>
</button>

<style>
  .hold { position: relative; overflow: hidden; touch-action: none; user-select: none; -webkit-user-select: none; -webkit-touch-callout: none; }
  .fill { position: absolute; inset: 0; background: color-mix(in srgb, var(--red) 35%, transparent); clip-path: inset(0 100% 0 0); transition: clip-path 200ms ease-out; }
  .hold.down .fill { clip-path: inset(0 0 0 0); transition: clip-path var(--hold) linear; }
  .lab { position: relative; }
  @media (prefers-reduced-motion: reduce) { .fill { display: none; } }
</style>
