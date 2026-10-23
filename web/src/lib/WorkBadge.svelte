<script lang="ts">
  import { PATHS } from './badge'
  import type { Kind } from './lavori'
  import type { Mark } from './recapAgenda'
  import { t } from './t'
  // Il segno di un lavoro (strada B, Franz 23/10 23:08): la stessa famiglia del badge delle sessioni (Badge.svelte): cerchio =
  // account personale, quadrato = agenzia; il glifo dice cosa chiede: mano = aspetta te, triangolo = può partire, sigillo = un ok.
  // `bare` = solo il glifo, nel colore del gruppo, dentro la scheda di una sessione.
  let { kind, shape = 'none', size = 22, bare = false }: { kind: Kind; shape?: Mark; size?: number; bare?: boolean } = $props()
  const GLYPH: Record<Kind, string[]> = {
    outside: PATHS.hand, claude: ['M7 4v16l13-8z'],
    ok: ['M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76z', 'm9 12 2 2 4-4'],
  }
  const TONE: Record<Kind, string> = { outside: 'var(--b-warn)', claude: 'var(--b-ring)', ok: 'var(--advice)' }
  const LABEL: Record<Kind, string> = { outside: t.lavoriOutside, claude: t.lavoriClaude, ok: t.lavoriOk }
  const square = $derived(shape === 'agency' || shape === 'desk')
</script>

{#if bare}
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={TONE[kind]} stroke-width="2" stroke-linecap="round" stroke-linejoin="round" role="img" aria-label={LABEL[kind]}>{#each GLYPH[kind] as d}<path {d} />{/each}</svg>
{:else}
  <svg width={size} height={size} viewBox="0 0 24 24" role="img" aria-label={LABEL[kind]}>
    {#if square}<rect width="24" height="24" rx="5.5" fill="var(--b-track)" />{:else}<circle cx="12" cy="12" r="12" fill="var(--b-track)" />{/if}
    <g transform="translate(4.8 4.8) scale(0.6)" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">{#each GLYPH[kind] as d}<path {d} />{/each}</g>
  </svg>
{/if}

<style>
  svg { flex: none; display: block; }
</style>
