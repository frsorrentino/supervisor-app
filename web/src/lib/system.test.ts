import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState } from './contract'
import { actionOf, consumersLine, isOpen, summaryLine } from './system'

const fixture = (f: string) => decodeState(readFileSync(new URL(`../../../contract/${f}`, import.meta.url), 'utf8'))

describe('sezione Sistema (contratto 1.90)', () => {
  it('system_advice: lo stato rosso porta livello, consumatori e quattro consigli', () => {
    const s = fixture('state-3-stale.json').system!
    expect(s.level).toBe('red')
    expect(s.advice.map(a => a.kind)).toEqual(['stuck_loop', 'orphan_proc', 'low_memory', 'stuck', 'orphan'])
    // Contratto 1.91: i bloccati sul ponte grafico restano in load1 ma escono dal carico per core.
    expect([s.stuck?.count, s.load1, s.load_per_core]).toEqual([48, 65.36, 2.17])
    expect(s.consumers[0]).toEqual({ group: 'claude', count: 5, cpu: 142.5, rss_mb: 2310 })
  })

  it('un relay precedente non porta system: niente sezione', () => {
    expect(fixture('state-2-idle.json').system ?? null).toBeNull()
    expect(fixture('state-1-question.json').system ?? null).toBeNull()
  })

  it('il tasto solo se il consiglio ha un\'azione e il relay ne conosce l\'op', () => {
    const st = fixture('state-3-stale.json')
    const [loop, kill, job] = st.system!.advice
    expect(actionOf(loop, st.ops)?.op).toBe('sys_unit_stop')
    expect(actionOf(kill, st.ops)).toEqual({ op: 'sys_kill', arg: '4410:912345', label: 'Close' })
    expect(actionOf(job, st.ops)).toBeNull()
    expect(actionOf(kill, ['close'])).toBeNull()
    expect(actionOf({ ...kill, action: { op: 'close', arg: 'a,b', label: 'Close' } }, ['close'])).toEqual({ op: 'close', arg: 'a,b', label: 'Close' })
  })

  it('aperta da sola in giallo e in rosso, compatta in verde; il tocco vale finché il livello non cambia', () => {
    expect(isOpen('green', null)).toBe(false)
    expect(isOpen('yellow', null)).toBe(true)
    expect(isOpen('red', null)).toBe(true)
    expect(isOpen('red', { level: 'red', open: false })).toBe(false)
    expect(isOpen('red', { level: 'yellow', open: false })).toBe(true)
    expect(isOpen('green', { level: 'green', open: true })).toBe(true)
  })

  it('la riga compatta dice carico per core e memoria libera, in italiano e in inglese', () => {
    const s = fixture('state-3-stale.json').system!
    expect(summaryLine(s, 'it')).toBe('carico 2,2 per core · 0,8 GB liberi su 6,5')
    expect(summaryLine(s, 'en')).toBe('load 2.2 per core · 0.8 GB free of 6.5')
  })

  it('chi consuma: i gruppi con CPU e memoria; vuota senza consumatori (stato tagliato)', () => {
    const s = fixture('state-3-stale.json').system!
    expect(consumersLine(s, 'it')).toBe('Claude ×5 143% 2,3 GB · altri ×118 96% 1,4 GB · chrome-bridge ×3 46% 1 GB · relay 3% 60 MB')
    expect(consumersLine({ ...s, consumers: [] }, 'it')).toBe('')
  })
})
