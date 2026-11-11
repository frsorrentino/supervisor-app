import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { clearPreviews, loadPreview, previewKey, readThumb, shownOf, wantsPreview } from './thumbs'
import type { Cmd, CmdResult, ResultItem } from './contract'

// Le anteprime dei documenti (app-anteprime-verifica-11-11, contratto 1.136), dagli esempi del relay …1360-1363.
const root = JSON.parse(readFileSync(new URL('../../../contract/cmd-result-sample.json', import.meta.url), 'utf8')) as { cmd: Cmd[]; result: CmdResult[] }
const sample = (end: string) => root.result.find(r => r.id.endsWith(end))!
const item = (x: Partial<ResultItem>): ResultItem => ({ id: 'a', title: 'x', kind: 'document', path: '/x/a.pdf', at: 10, version: 1, versions: 1, ...x }) as ResultItem

describe('anteprime', () => {
  it('gli esempi del relay: immagine, righe, nessuna, id sconosciuto', () => {
    const img = readThumb(sample('1360').ok, sample('1360').text)!
    expect(img.kind).toBe('image'); expect(img.width).toBeLessThanOrEqual(240)
    expect(shownOf(img)).toMatchObject({ kind: 'image' })
    expect((shownOf(img) as { src: string }).src.startsWith('data:image/jpeg;base64,/9j/')).toBe(true)
    expect(shownOf(readThumb(true, sample('1361').text))).toMatchObject({ kind: 'lines' })
    expect((shownOf(readThumb(true, sample('1361').text)) as { text: string }).text.split('\n')[0]).toBe('# Verbale della riunione')
    expect(shownOf(readThumb(true, sample('1362').text))).toBeNull()
    expect(readThumb(sample('1363').ok, sample('1363').text)).toBeNull()
  })
  it('solo i file senza immagine loro la chiedono; una versione nuova è un\'anteprima nuova', () => {
    expect(wantsPreview(item({}))).toBe(true)
    expect(wantsPreview(item({ kind: 'image', mime: 'image/png', path: '/x/a.png' }))).toBe(false)
    expect(wantsPreview(item({ kind: 'video', path: '/x/a.mp4' }))).toBe(false)
    expect(previewKey(item({}))).not.toBe(previewKey(item({ version: 2, at: 20 })))
  })
  it('una lettura sola per versione, anche con più caselle insieme; poi la cache', async () => {
    clearPreviews()
    let n = 0
    const run = async (c: Cmd) => { n++; await new Promise(r => setTimeout(r, 2)); return { id: c.id, ok: true, text: sample('1361').text, at: 1 } as CmdResult }
    const i = item({ id: '9f2c1a7b3e4d5c61' })
    const [a, b] = await Promise.all([loadPreview(i, run), loadPreview(i, run)])
    expect(a).toEqual(b); expect(n).toBe(1)
    await loadPreview(i, run)
    expect(n).toBe(1)
    await loadPreview(item({ id: '9f2c1a7b3e4d5c61', version: 2 }), run)
    expect(n).toBe(2)
  })
})
