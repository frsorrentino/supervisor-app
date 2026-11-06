// Il contatore dei fotogrammi nascosto (app-fluidita-06-11): il riassunto dell'ultimo gesto, per misurare sul Chromebook vero
// invece che sulla demo. Qui solo i conti, senza browser.

/** Un long-animation-frame ridotto a quello che serve: durata, da quando il browser impagina, e gli script più lunghi. */
export type Loaf = { at: number; d: number; layoutFrom: number | null; scripts: { name: string; d: number }[] }
export type Cause = { kind: 'script'; name: string } | { kind: 'layout' } | { kind: 'paint' }
export type Summary = { frames: number; max: number; over33: number; loaf: { d: number; cause: Cause }[] }

/**
 * La causa principale di un fotogramma lungo: lo script più lungo se pesa almeno metà del fotogramma; se no stile e
 * impaginazione se pesano almeno un terzo; se no il disegno (quello che resta dopo, che il browser non scompone).
 */
export function cause(l: Loaf): Cause {
  const top = [...l.scripts].sort((a, b) => b.d - a.d)[0]
  if (top && top.d >= l.d / 2) return { kind: 'script', name: top.name }
  const layout = l.layoutFrom == null ? 0 : l.d - (l.layoutFrom - l.at)
  if (layout >= l.d / 3) return { kind: 'layout' }
  return { kind: 'paint' }
}

/** I fotogrammi del gesto (intervalli fra un disegno e l'altro, ms) e i suoi long-animation-frame, i tre più lunghi. */
export function summarize(frames: number[], loaf: Loaf[]): Summary {
  return {
    frames: frames.length,
    max: Math.round(Math.max(0, ...frames)),
    over33: frames.filter(f => f > 34).length,
    loaf: [...loaf].sort((a, b) => b.d - a.d).slice(0, 3).map(l => ({ d: Math.round(l.d), cause: cause(l) })),
  }
}
