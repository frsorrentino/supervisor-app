import { describe, expect, it } from 'vitest'
import type { PluginsPage } from './contract'
import { pluginsRun } from './pluginsRun'

const page = (update: boolean) => ({ checked_at: 1, accounts: [{ account: 'personale', plugins: [{ id: 'a@b', name: 'a', update }], skills: [] }] }) as unknown as PluginsPage

function setup(answer: () => Promise<{ ok: boolean; text: string } | null>) {
  const ops: string[] = []
  let loads = 0
  const r = pluginsRun(async () => { loads++; return page(false) }, async (op) => { ops.push(op); return answer() })
  return { r, ops, loads: () => loads }
}

describe('Aggiorna tutto (Franz 31/10 15:31)', () => {
  it('dopo la conferma è in corso, e la seconda pressione non manda un altro comando', async () => {
    let done!: (v: { ok: boolean; text: string }) => void
    const { r, ops } = setup(() => new Promise(res => { done = res }))
    const going = r.updateAll(['personale'])
    expect(r.get().all).toBe(true)
    await r.updateAll(['personale'])
    expect(ops).toEqual(['plugin_update_all'])
    done({ ok: true, text: 'aggiornato' })
    await going
    expect(r.get().all).toBe(false)
  })
  it('all\'esito si vede il testo del relay e la lista si rilegge', async () => {
    const { r, loads } = setup(async () => ({ ok: true, text: 'a@b aggiornato' }))
    await r.updateAll(['personale'])
    expect(r.get().notes.personale).toEqual({ ok: true, text: 'a@b aggiornato' })
    expect(loads()).toBe(1)
    expect(r.get().page).toEqual(page(false))
  })
  it('senza risposta lo dice e rilegge lo stesso', async () => {
    const { r, loads } = setup(async () => null)
    await r.updateAll(['personale'])
    expect(r.get().notes.personale).toEqual({ ok: false, text: null })
    expect(loads()).toBe(1)
  })
})
