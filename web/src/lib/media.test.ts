import { describe, expect, it } from 'vitest'
import { autoPreview, fileKey, isVideo } from './media'

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
  it('gli altri file aspettano il tocco', () => {
    expect(autoPreview('application/pdf', 1_000)).toBe(false)
    expect(autoPreview(undefined, 1_000)).toBe(false)
    expect(isVideo('video/webm')).toBe(true)
  })
})
