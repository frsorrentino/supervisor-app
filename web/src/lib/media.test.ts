import { describe, expect, it } from 'vitest'
import { autoPreview, canPreview, fileKey, firstLines, isVideo, previewKind } from './media'

describe('la chiave di un file della chat, come FileCache.key nell\'app', () => {
  const video = { path: '/home/franz/bozza.mp4', mime: 'video/mp4', size: 11_402 }
  it('lo stesso file tiene la sua chiave a ogni riapertura', () => {
    expect(fileKey('master', video, 'e1')).toBe(fileKey('master', { ...video }, 'e1'))
    // La misura c'è: una voce nuova con lo stesso file è ancora lo stesso file.
    expect(fileKey('master', video, 'e2')).toBe(fileKey('master', video, 'e1'))
  })
  it('lo stesso percorso con un\'altra misura è un altro file', () => {
    expect(fileKey('master', { ...video, size: 83_203 }, 'e1')).not.toBe(fileKey('master', video, 'e1'))
  })
  it('senza misura decide la voce', () => {
    const bare = { path: '/home/franz/bozza.mp4', mime: 'video/mp4', size: null }
    expect(fileKey('master', bare, 'e1')).toBe(fileKey('master', bare, 'e1'))
    expect(fileKey('master', bare, 'e2')).not.toBe(fileKey('master', bare, 'e1'))
    expect(fileKey('master', { path: bare.path }, 'e1')).toBe(fileKey('master', bare, 'e1'))
  })
  it('la sessione resta nella chiave', () => {
    expect(fileKey('supervisor-app', video, 'e1')).not.toBe(fileKey('master', video, 'e1'))
  })
})

describe('anteprime che arrivano da sole, come sul telefono', () => {
  it('immagini fino a 10 MB', () => {
    expect(autoPreview('image/png', 24_645)).toBe(true)
    expect(autoPreview('image/png', null)).toBe(true)
    expect(autoPreview('image/jpeg', 10_000_001)).toBe(false)
  })
  it('video fino a 25 MB, non col risparmio dati e non senza misura', () => {
    expect(autoPreview('video/mp4', 7_211_071)).toBe(true)
    expect(autoPreview('video/mp4', 26_214_400)).toBe(true)
    expect(autoPreview('video/mp4', 31_291_332)).toBe(false)
    expect(autoPreview('video/mp4', 7_211_071, false)).toBe(false)
    expect(autoPreview('video/mp4', null)).toBe(false)
  })
  // Franz, 15/10 22:20: «ogni file allegato mostri già l'anteprima».
  it('PDF fino a 10 MB e testi fino a 1 MB, non col risparmio dati', () => {
    expect(autoPreview('application/pdf', 212_000)).toBe(true)
    expect(autoPreview('application/pdf', 10_000_001)).toBe(false)
    expect(autoPreview('application/pdf', 212_000, false)).toBe(false)
    expect(autoPreview('text/markdown', 4_000)).toBe(true)
    expect(autoPreview(null, 4_000, true, '/w/notes/README.md')).toBe(true)
    expect(autoPreview('text/plain', 1_000_001)).toBe(false)
    expect(autoPreview('text/plain', 4_000, false)).toBe(false)
  })
  it('gli altri file hanno la scheda larga, senza scaricare', () => {
    expect(autoPreview('application/zip', 1_000)).toBe(false)
    expect(autoPreview(undefined, 1_000)).toBe(false)
    expect(previewKind('application/zip', 'a.zip')).toBe('other')
    expect(canPreview('application/zip')).toBe(false)
    expect(isVideo('video/webm')).toBe(true)
  })
  it('il tipo di anteprima', () => {
    expect(previewKind('image/png')).toBe('image')
    expect(previewKind('image/svg+xml')).toBe('text')
    expect(previewKind('application/pdf')).toBe('pdf')
    expect(previewKind('application/octet-stream', '/w/x/notes.md')).toBe('text')
    expect(previewKind('application/json')).toBe('text')
    expect(previewKind('video/mp4')).toBe('video')
  })
  it('le prime righe di un testo', () => {
    expect(firstLines('a\r\nb\n\n', 12)).toEqual({ lines: 'a\nb', more: false })
    expect(firstLines(Array.from({ length: 20 }, (_, i) => String(i)).join('\n'), 12)).toEqual({ lines: Array.from({ length: 12 }, (_, i) => String(i)).join('\n'), more: true })
  })
})
