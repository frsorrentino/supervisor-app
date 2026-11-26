import { describe, expect, it } from 'vitest'
import { avanza, ferma, molla } from './molla'

const corri = (k: number, c: number, sec: number) => {
  const s = molla(0, k, c)
  s.t = 1
  let max = 0
  for (let i = 0; i < Math.round(sec * 60); i++) { avanza([s], 1 / 60); max = Math.max(max, s.x) }
  return { s, max }
}

describe('molla, il motore dell’originale', () => {
  it('le icone del Copia (420 18) rimbalzano oltre la meta e poi si fermano', () => {
    const { s, max } = corri(420, 18, 2)
    expect(max).toBeGreaterThan(1.1)
    expect(ferma(s)).toBe(true)
  })
  it('il rullo della parola (260 20) arriva alla meta entro 1,5 s', () => {
    const { s } = corri(260, 20, 1.5)
    expect(Math.abs(s.x - 1)).toBeLessThan(0.01)
  })
  it('cambiare la meta tiene la velocità', () => {
    const s = molla(0, 300, 24)
    s.t = 1
    avanza([s], 1 / 60)
    const v = s.v
    s.t = 0
    expect(s.v).toBe(v)
  })
})
