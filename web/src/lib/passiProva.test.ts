import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import type { ProofSteps } from './contract'
import { fill, line, look, reach } from './passiProva'

// Le tappe della prova su un filo (app-animazioni-passi-26-11, contratto 1.162), dalla fixture del relay: gli stessi casi di
// PassiProvaTest in Kotlin.
const fx = JSON.parse(readFileSync(new URL('../../../contract/steps-in-verifica.json', import.meta.url), 'utf8')) as { sessions: { name: string; steps?: ProofSteps }[] }
const app = fx.sessions.find(s => s.name === 'supervisor-app')!.steps!
describe('passi della prova', () => {
  it('la fixture: tappe e null', () => {
    expect(app.task).toBe('app-animazioni-otto-26-11')
    expect(app.items.map(s => s.label)).toEqual(['Test', 'CI', 'Pre-release', 'Prova'])
    expect(app.items.map(look)).toEqual(['done', 'done', 'now', 'todo'])
    expect(fx.sessions.find(s => s.name === 'gamma')!.steps ?? null).toBeNull()
  })
  it('il filo arriva alla tappa di adesso', () => {
    expect(reach(app.items)).toBe(2)
    expect(fill(app.items)).toBeCloseTo(2 / 3)
    expect(fill(app.items.map(s => ({ ...s, state: 'ok' })))).toBe(1)
    expect(fill(app.items.map(s => ({ ...s, state: 'todo' })))).toBe(0)
  })
  it('la riga dice dove si è', () => {
    expect(line(app)).toEqual({ say: 'running', label: 'Pre-release' })
    expect(line({ ...app, items: app.items.map((s, i) => (i === 1 ? { ...s, state: 'ko' } : s)) })).toEqual({ say: 'failed', label: 'CI' })
    expect(line({ ...app, items: app.items.map(s => (s.key === 'proof' ? s : { ...s, state: 'ok' })) })).toEqual({ say: 'todo', label: 'Prova' })
    expect(line({ ...app, items: app.items.map(s => ({ ...s, state: 'ok' })) })).toEqual({ say: 'all_done', label: '' })
    expect(line(null)).toBeNull()
    expect(line({ task: 't', items: [] })).toBeNull()
  })
})
