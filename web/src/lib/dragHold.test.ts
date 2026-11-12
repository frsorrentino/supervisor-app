import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DRAG_IDLE_MS, dragHold } from './dragHold'

// app-web-trascina-riquadro-12-11: il riquadro «Rilascia per allegare» non resta acceso sulla sessione solo attraversata.
describe('dragHold', () => {
  beforeEach(() => { vi.useFakeTimers() })
  afterEach(() => { vi.useRealTimers() })
  const make = () => { let on = false; const h = dragHold(v => (on = v)); return { h, on: () => on } }

  it('acceso coi dragover, spento quando smettono anche senza dragleave dalla colonna', () => {
    const { h, on } = make()
    h.over()
    expect(on()).toBe(true)
    vi.advanceTimersByTime(DRAG_IDLE_MS - 1)
    expect(on()).toBe(true)
    vi.advanceTimersByTime(1)
    expect(on()).toBe(false)
  })

  it('i dragover di continuo lo tengono acceso', () => {
    const { h, on } = make()
    for (let i = 0; i < 10; i++) { h.over(); vi.advanceTimersByTime(500) }
    expect(on()).toBe(true)
  })

  it('uscito davvero dalla colonna si spegne subito; passato a un elemento interno no', () => {
    const { h, on } = make()
    h.over(); h.leave(false)
    expect(on()).toBe(true)
    h.leave(true)
    expect(on()).toBe(false)
  })

  it('il rilascio in un\'altra sessione lo spegne subito, e il timer non lo riaccende', () => {
    const { h, on } = make()
    h.over(); h.off()
    expect(on()).toBe(false)
    vi.advanceTimersByTime(DRAG_IDLE_MS * 2)
    expect(on()).toBe(false)
  })
})
