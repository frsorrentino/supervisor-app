import { execFileSync } from 'node:child_process'
import { mkdtempSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'
import { crc32, uniqueNames, zip, zipName } from './zip'

const enc = new TextEncoder()

describe('Salva tutti: lo zip dei file', () => {
  it('crc32 come lo standard', () => {
    expect(crc32(enc.encode('123456789'))).toBe(0xcbf43926)
    expect(crc32(new Uint8Array())).toBe(0)
  })
  it('lo stesso nome due volte non si sovrascrive', () => {
    expect(uniqueNames(['a.png', 'a.png', 'b', 'b', 'a.png'])).toEqual(['a.png', 'a (2).png', 'b', 'b (2)', 'a (3).png'])
  })
  it('il nome dice sessione e giorno', () => {
    expect(zipName('storefront', new Date(2026, 9, 16, 16, 5))).toBe('storefront-2026-10-16.zip')
    expect(zipName('a b/c', new Date(2026, 0, 2))).toBe('a-b-c-2026-01-02.zip')
  })
  it('unzip lo apre e trova i file com\'erano', () => {
    const z = zip([{ name: 'nota è.md', data: enc.encode('# ciao\n') }, { name: 'dati.bin', data: new Uint8Array([0, 1, 2, 255]) }, { name: 'nota è.md', data: enc.encode('due') }])
    const dir = mkdtempSync(join(tmpdir(), 'zip-'))
    const f = join(dir, 'x.zip')
    writeFileSync(f, z)
    const list = execFileSync(process.platform === 'win32' ? 'python' : 'python3', ['-I', '-c', 'import sys,zipfile;z=zipfile.ZipFile(sys.argv[1]);assert z.testzip() is None;print("|".join(n+":"+z.read(n).hex() for n in z.namelist()))', f]).toString().trim()
    expect(list).toBe(`nota è.md:${Buffer.from('# ciao\n').toString('hex')}|dati.bin:000102ff|nota è (2).md:${Buffer.from('due').toString('hex')}`)
  })
})
