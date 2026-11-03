import type { ResultGroup, ResultItem, ResultsPage, TranscriptFile } from './contract'

// Porta di Guardare.kt (app-da-guardare-02-11, contratto 1.106): i file consegnati dalle sessioni, riga per riga. Le parole
// le mette l'app.
export type Kind = 'video' | 'mockup' | 'image' | 'document' | 'other'
export const kindOf = (k: string | null | undefined): Kind => (k === 'video' || k === 'mockup' || k === 'image' || k === 'document' ? k : 'other')

export type Line = { title: string; version: number | null; session: string | null; kind: Kind; seconds: number | null; at: number; unseen: boolean }

/** Il titolo con la versione solo se ce n'è più d'una; durata in secondi interi per un video; `unseen` = punto ambra. */
export function line(i: ResultItem): Line {
  const k = kindOf(i.kind)
  return {
    title: i.title.trim() || i.path.split('/').pop() || i.path, version: i.versions > 1 ? i.version : null, session: i.session?.trim() || null,
    kind: k, seconds: k === 'video' && i.duration_s && i.duration_s > 0 ? Math.max(1, Math.round(i.duration_s)) : null, at: i.at, unseen: i.seen_at == null,
  }
}

/** L'immagine in piccolo: l'immagine stessa, o quella che accompagna un mockup; null per video e documenti. */
export function thumb(i: ResultItem): TranscriptFile | null {
  const k = kindOf(i.kind)
  if (k === 'image') return { path: i.path, mime: i.mime, size: i.size }
  if (k === 'mockup') return (i.files ?? []).find(f => f.mime?.startsWith('image/')) ?? (i.mime?.startsWith('image/') ? { path: i.path, mime: i.mime, size: i.size } : null)
  return null
}

/** Nella sessione, la riga «Risultati»: i suoi lavori e l'ultima versione consegnata. */
export function ofSession(page: ResultsPage | null | undefined, session: string): { groups: ResultGroup[]; latest: ResultItem | null } {
  const groups = (page?.groups ?? []).filter(g => g.session === session)
  // Solo un lavoro non visto fa da anteprima (Franz 03/11 19:54: «anche se visto tutto è rimasto Icona di Progress Bars»).
  const top = groups.filter(g => g.unseen).reduce<ResultGroup | null>((a, g) => (!a || g.at > a.at ? g : a), null)
  return { groups, latest: top?.items[0] ?? null }
}

export const older = (g: ResultGroup): ResultItem[] => g.items.slice(1)

/**
 * app-risultati-visti-03-11 (Franz 03/11 15:26: «dovrebbero rimanere solo se effettivamente non li ho visti»), porta di Guardare.splitSeen:
 * in elenco i lavori con l'ultima versione non vista; quelli visti, dal relay o qui (`local`), in fondo e chiusi.
 */
export function splitSeen(groups: ResultGroup[], local: Set<string>): { fresh: ResultGroup[]; seen: ResultGroup[] } {
  const isFresh = (g: ResultGroup) => !!g.items[0] && g.items[0].seen_at == null && !local.has(g.items[0].id)
  return { fresh: groups.filter(isFresh), seen: groups.filter(g => !isFresh(g)) }
}

/** Un file aperto altrove nell'app (la chat): i risultati non visti che lo consegnano, da segnare visti. */
export const seenByPath = (items: ResultItem[], path: string): string[] =>
  items.filter(i => i.seen_at == null && (i.path === path || (i.files ?? []).some(f => f.path === path))).map(i => i.id)

/** «36,7 s», «6 s», «1:05». */
export function pos(s: number): string {
  const t = Math.round(s * 10) / 10
  if (t >= 60) { const n = Math.floor(t); return `${Math.floor(n / 60)}:${String(n % 60).padStart(2, '0')}` }
  return Number.isInteger(t) ? `${t} s` : `${Math.floor(t)},${Math.round((t % 1) * 10)} s`
}

export const NOTE_MAX = 2000
export const note = (text: string): string | null => text.trim() ? text.trim().slice(0, NOTE_MAX) : null
