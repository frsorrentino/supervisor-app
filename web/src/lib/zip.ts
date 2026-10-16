// «Salva tutti» nella web (Franz, 16/10 16:01): i browser bloccano più download di fila, quindi un solo zip. I file della
// chat sono già compressi o piccoli: lo zip li tiene come sono (metodo 0, «stored»), senza librerie.

const table = (() => {
  const t = new Uint32Array(256)
  for (let n = 0; n < 256; n++) {
    let c = n
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1
    t[n] = c >>> 0
  }
  return t
})()

export function crc32(b: Uint8Array): number {
  let c = 0xffffffff
  for (let i = 0; i < b.length; i++) c = table[(c ^ b[i]) & 0xff] ^ (c >>> 8)
  return (c ^ 0xffffffff) >>> 0
}

/** Nomi unici nello zip: lo stesso nome due volte diventa «nome (2).ext». */
export function uniqueNames(names: string[]): string[] {
  const seen = new Set<string>()
  return names.map(n => {
    let out = n
    for (let i = 2; seen.has(out); i++) {
      const dot = n.lastIndexOf('.')
      out = dot > 0 ? `${n.slice(0, dot)} (${i})${n.slice(dot)}` : `${n} (${i})`
    }
    seen.add(out)
    return out
  })
}

/** Lo zip dei file, nell'ordine dato. */
export function zip(files: { name: string; data: Uint8Array }[]): Uint8Array<ArrayBuffer> {
  const enc = new TextEncoder()
  const names = uniqueNames(files.map(f => f.name)).map(n => enc.encode(n))
  const size = files.reduce((s, f, i) => s + 30 + names[i].length + f.data.length + 46 + names[i].length, 0) + 22
  const out = new Uint8Array(size)
  const v = new DataView(out.buffer)
  let p = 0
  const central: number[] = []
  files.forEach((f, i) => {
    const crc = crc32(f.data)
    central.push(p)
    // Bit 11: nomi in UTF-8.
    v.setUint32(p, 0x04034b50, true); v.setUint16(p + 4, 20, true); v.setUint16(p + 6, 0x0800, true); v.setUint16(p + 8, 0, true)
    v.setUint32(p + 10, 0x00210000, true) // ora 00:00, data 01/01/1980
    v.setUint32(p + 14, crc, true); v.setUint32(p + 18, f.data.length, true); v.setUint32(p + 22, f.data.length, true)
    v.setUint16(p + 26, names[i].length, true); v.setUint16(p + 28, 0, true)
    out.set(names[i], p + 30); out.set(f.data, p + 30 + names[i].length)
    p += 30 + names[i].length + f.data.length
  })
  const dir = p
  files.forEach((f, i) => {
    v.setUint32(p, 0x02014b50, true); v.setUint16(p + 4, 20, true); v.setUint16(p + 6, 20, true); v.setUint16(p + 8, 0x0800, true)
    v.setUint16(p + 10, 0, true); v.setUint32(p + 12, 0x00210000, true)
    v.setUint32(p + 16, crc32(f.data), true); v.setUint32(p + 20, f.data.length, true); v.setUint32(p + 24, f.data.length, true)
    v.setUint16(p + 28, names[i].length, true)
    v.setUint32(p + 42, central[i], true)
    out.set(names[i], p + 46)
    p += 46 + names[i].length
  })
  v.setUint32(p, 0x06054b50, true); v.setUint16(p + 8, files.length, true); v.setUint16(p + 10, files.length, true)
  v.setUint32(p + 12, p - dir, true); v.setUint32(p + 16, dir, true)
  return out
}

/** Il nome dello zip: la sessione e il giorno, «storefront-2026-10-16.zip». */
export function zipName(session: string, at: Date): string {
  const d = `${at.getFullYear()}-${String(at.getMonth() + 1).padStart(2, '0')}-${String(at.getDate()).padStart(2, '0')}`
  return `${session.replace(/[\\/:*?"<>|\s]+/g, '-') || 'file'}-${d}.zip`
}
