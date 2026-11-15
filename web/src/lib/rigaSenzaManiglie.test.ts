import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

// app-riga-senza-maniglie-15-11 (Franz 15/11 15:19: «dovremmo togliere anche maniglie e frecce quando la master è chiusa»): gli stessi
// controlli di RigaSenzaManiglieTest.kt. La riga in basso non ha più le due maniglie con la ⌃, né il loro gesto e le loro stringhe.
describe('riga_senza_maniglie', () => {
  const app = readFileSync(new URL('../App.svelte', import.meta.url), 'utf8')
  const strings = readFileSync(new URL('./t.ts', import.meta.url), 'utf8')
  const bar = app.slice(app.indexOf('{#snippet masterBar()}'), app.indexOf('{/snippet}', app.indexOf('{#snippet masterBar()}')))
  it('niente maniglie sul bordo della riga', () => {
    expect(app).not.toContain('bargrip')
    expect(bar).not.toContain('hgrip')
    expect(strings).not.toContain('masterBarGrip')
  })
  it('restano il tocco, il tiro in su e il tasto tondo', () => {
    expect(bar).toContain('onToggle=')
    expect(bar).toContain('onPlace={wide ? barDrag : undefined}')
    expect(bar).toContain('{@render masterKey()}')
  })
})
