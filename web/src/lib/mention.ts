import type { Session } from './contract'

// «@nome» nella chat della master (Franz, 14/10 21:55, voce 5), porta di Mention.kt: scrivendo «@» il campo propone le
// sessioni vive; il messaggio che comincia con «@nome » va a quella sessione invece che alla master.

export function suggestMention(draft: string, sessions: Session[], master: string): Session[] {
  if (!draft.startsWith('@') || /\s/.test(draft)) return []
  const q = draft.slice(1).toLowerCase()
  return sessions.filter(s => s.name !== master && s.state !== 'gone' && s.name.toLowerCase().startsWith(q))
}

export function parseMention(draft: string, sessions: Session[], master: string): [string, string] | null {
  if (!draft.startsWith('@')) return null
  const name = draft.slice(1).split(/\s/)[0]
  const text = draft.slice(1 + name.length).trim()
  if (!text || name === master) return null
  const s = sessions.find(x => x.name === name && x.state !== 'gone')
  return s ? [s.name, text] : null
}
