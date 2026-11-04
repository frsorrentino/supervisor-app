// Feedback dal menù (app-feedback-menu-04-11, scelta A di Franz): «Suggerimento» o «Segnala un bug», un testo, i dati tecnici
// aggiunti da soli e, se la si accende, la foto della pagina. L'invio passa dal relay alle osservazioni di supervisor
// (supervisor-feedback-app-04-11): prima la bozza anonima, poi «Invia dal mio GitHub», «Invia anonimo» o «Non ora». Finché il
// relay non ha l'op `feedback` la bozza si prepara qui, con le stesse regole, e l'invio resta spento.
export type FeedbackKind = 'idea' | 'bug'
export type Tech = { version: string; commit: string; device: string; screen: string; lang: string }
export type Draft = { kind: FeedbackKind; title: string; body: string; photo: boolean }

/** Il dispositivo in chiaro dal browser: navigatore e versione, sistema, misura della finestra. */
export function deviceOf(ua: string, w: number, h: number): string {
  const br = /Edg\/(\d+)/.exec(ua) ? `Edge ${/Edg\/(\d+)/.exec(ua)![1]}`
    : /Firefox\/(\d+)/.exec(ua) ? `Firefox ${/Firefox\/(\d+)/.exec(ua)![1]}`
    : /Chrome\/(\d+)/.exec(ua) ? `Chrome ${/Chrome\/(\d+)/.exec(ua)![1]}`
    : /Version\/(\d+).*Safari/.exec(ua) ? `Safari ${/Version\/(\d+).*Safari/.exec(ua)![1]}` : 'browser'
  const os = /Android/.test(ua) ? 'Android' : /iPhone|iPad/.test(ua) ? 'iOS' : /Windows/.test(ua) ? 'Windows' : /CrOS/.test(ua) ? 'ChromeOS' : /Mac OS X/.test(ua) ? 'macOS' : /Linux/.test(ua) ? 'Linux' : ''
  return [br, os, `${w}×${h}`].filter(Boolean).join(' · ')
}

/**
 * Il testo reso anonimo prima di mostrarlo: indirizzi email, cartelle personali, chiavi e token lunghi, indirizzi IP. Le stesse
 * regole del relay; quello che resta è ciò che si vede nella bozza.
 */
export function anonymize(s: string): string {
  return s
    .replace(/[\w.+-]+@[\w-]+(\.[\w-]+)+/g, '<email>')
    .replace(/([A-Za-z]:\\Users\\|\/home\/|\/Users\/)[^\\/\s]+/g, '$1<utente>')
    .replace(/\b(gh[pousr]_[A-Za-z0-9]{20,}|sk-[A-Za-z0-9_-]{20,}|AIza[A-Za-z0-9_-]{30,})\b/g, '<chiave>')
    .replace(/\b[A-Fa-f0-9]{32,}\b/g, '<chiave>')
    .replace(/\b(\d{1,3}\.){3}\d{1,3}\b/g, '<ip>')
}

/** Il titolo: la prima riga del testo, al massimo 72 caratteri tagliati a una parola intera. */
export function titleOf(text: string): string {
  const first = text.trim().split('\n')[0].trim()
  if (first.length <= 72) return first
  const cut = first.slice(0, 72)
  return cut.slice(0, cut.lastIndexOf(' ') > 40 ? cut.lastIndexOf(' ') : 72)
}

/** La bozza anonima come andrà alle osservazioni: titolo, testo, dati tecnici e, se c'è, la foto allegata. */
export function draftOf(kind: FeedbackKind, text: string, tech: Tech, photo: boolean, labels: { kind: Record<FeedbackKind, string>; tech: string; photo: string }): Draft {
  const clean = anonymize(text.trim())
  const lines = [
    clean,
    '',
    `### ${labels.tech}`,
    `- app: ${tech.version} (${tech.commit})`,
    `- ${tech.device}`,
    `- ${tech.screen}`,
    `- ${tech.lang}`,
  ]
  if (photo) lines.push('', `_${labels.photo}_`)
  return { kind, title: `[${labels.kind[kind]}] ${titleOf(clean)}`, body: lines.join('\n'), photo }
}

/**
 * La foto della pagina: il browser chiede il permesso di riprendere la scheda (Chrome la propone per prima), si prende un
 * fotogramma e si chiude subito la ripresa. `hide` toglie il foglio di Feedback mentre si scatta, `show` lo rimette.
 * Senza permesso o senza supporto (telefono): null.
 */
export async function grabTab(hide: () => Promise<void>, show: () => void, maxW = 1280): Promise<string | null> {
  const md = navigator.mediaDevices as MediaDevices & { getDisplayMedia?: (o: object) => Promise<MediaStream> }
  if (!md?.getDisplayMedia) return null
  let stream: MediaStream | null = null
  try {
    stream = await md.getDisplayMedia({ video: { displaySurface: 'browser' }, audio: false, preferCurrentTab: true, selfBrowserSurface: 'include' })
    await hide()
    const v = document.createElement('video')
    v.muted = true; v.srcObject = stream
    await v.play()
    await new Promise(r => setTimeout(r, 250))
    const s = Math.min(1, maxW / (v.videoWidth || maxW))
    const c = document.createElement('canvas')
    c.width = Math.round(v.videoWidth * s); c.height = Math.round(v.videoHeight * s)
    c.getContext('2d')!.drawImage(v, 0, 0, c.width, c.height)
    return c.width ? c.toDataURL('image/jpeg', 0.82) : null
  } catch {
    return null
  } finally {
    stream?.getTracks().forEach(tr => tr.stop())
    show()
  }
}
