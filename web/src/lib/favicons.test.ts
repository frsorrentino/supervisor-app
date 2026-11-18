import { describe, it, expect } from 'vitest'
import { wanted, faviconUrl, validHash, ringOf } from './favicons.svelte'
import { decodeState } from './contract'
import { demoFavicons } from './demo'
import stateRaw from '../../../contract/state-1-question.json?raw'

// app-favicon-siti-18-11 (contratto 1.153): la favicon del sito come avatar della sessione; porta di FaviconsTest.kt.
describe('favicon del sito', () => {
  const st = decodeState(stateRaw)
  it('la fixture del relay ha l\'op ma nessun sito', () => {
    expect(st.ops).toContain('favicon')
    expect(wanted(st)).toEqual([])
  })
  it('una lettura per impronta, solo con l\'op e solo impronte buone', () => {
    const s = { ...st, sessions: [{ ...st.sessions[0], favicon: '8c0173c7600b' }, { ...st.sessions[1], favicon: '8c0173c7600b' }, { ...st.sessions[2], favicon: '../x' }] }
    expect(wanted(s)).toEqual(['8c0173c7600b'])
    expect(wanted({ ...s, ops: s.ops!.filter(o => o !== 'favicon') })).toEqual([])
    expect(validHash('8C0173C7600B')).toBe(false)
  })
  it('la risposta della demo diventa un PNG; un\'impronta diversa, un errore o dati rotti no', () => {
    const [h, img] = Object.entries(demoFavicons.images)[0]
    const text = JSON.stringify(img)
    expect(faviconUrl(h, true, text)).toMatch(/^data:image\/png;base64,iVBOR/)
    expect(faviconUrl('000000000000', true, text)).toBeNull()
    expect(faviconUrl(h, false, text)).toBeNull()
    expect(faviconUrl(h, true, JSON.stringify({ ...img, mime: 'image/jpeg' }))).toBeNull()
    expect(faviconUrl(h, true, JSON.stringify({ ...img, data: 'R0lGODlhAQABAAAAACw=' }))).toBeNull()
    expect(faviconUrl(h, true, `favicon ${h} not found`)).toBeNull()
  })
  it('storefront e blog della demo hanno la loro favicon', () => {
    expect(Object.keys(demoFavicons.hashes).sort()).toEqual(['blog', 'storefront'])
    expect(ringOf('waiting')).toBe('var(--wait)')
  })
})
