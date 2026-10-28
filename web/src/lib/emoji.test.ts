import { describe, expect, it } from 'vitest'
import { stripEmoji } from './emoji'

// Franz, 22/10 13:36: niente emoji nell'app e nelle notifiche; gli stessi casi di EmojiTest in Kotlin.
describe('filtro delle emoji', () => {
  it('i simboli di stato in testa a un titolo del relay se ne vanno', () => {
    expect(stripEmoji('✓ atlas-release-2-4: check green')).toBe('atlas-release-2-4: check green')
    expect(stripEmoji('✗ ledger-migrations-011: check red')).toBe('ledger-migrations-011: check red')
    expect(stripEmoji('⛔ social-example-it: limite cinque ore')).toBe('social-example-it: limite cinque ore')
    expect(stripEmoji('⚠ atlas-shop: turn failed — 529')).toBe('atlas-shop: turn failed — 529')
    expect(stripEmoji('❓ ledger-api')).toBe('ledger-api')
    expect(stripEmoji('⏸ orbit-docs in pausa')).toBe('orbit-docs in pausa')
  })
  it('pittogrammi, toni della pelle, bandiere e keycap se ne vanno e gli spazi si chiudono', () => {
    expect(stripEmoji('🚀 deploy fatto 🎉, controlla 👍🏽')).toBe('deploy fatto, controlla')
    expect(stripEmoji('🟢 nome')).toBe('nome')
    expect(stripEmoji('ok ✅ 3️⃣ file 🇮🇹')).toBe('ok 3 file')
    expect(stripEmoji('famiglia 👨‍👩‍👧')).toBe('famiglia')
    // ▶ ha la presentazione emoji e va via; ◐ ○ ● sono forme geometriche di testo e restano.
    expect(stripEmoji('▶ in corso · ◐ 2')).toBe('in corso · ◐ 2')
  })
  it('lettere, frecce e punteggiatura restano', () => {
    expect(stripEmoji('→ prossimo: apri la CI · 95 % · ↳ ok · © 2026 · ≥ 6 GB')).toBe('→ prossimo: apri la CI · 95 % · ↳ ok · © 2026 · ≥ 6 GB')
    expect(stripEmoji('perché «sì» — à é ì')).toBe('perché «sì» — à é ì')
    expect(stripEmoji('✓')).toBe('')
    expect(stripEmoji('✓ · 3 m')).toBe('3 m')
  })
})
