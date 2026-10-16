import { describe, expect, it, vi } from 'vitest'
import { dictate, dictError, dictLang, joinDictation, speechCtor, type Rec, type RecEvent } from './dictation'

// Una SpeechRecognition finta: il test fa «parlare» con say() e chiude con end().
class FakeRec implements Rec {
  static last: FakeRec | null = null
  lang = ''; interimResults = false; continuous = true
  onresult: ((e: RecEvent) => void) | null = null
  onerror: ((e: { error: string }) => void) | null = null
  onend: (() => void) | null = null
  started = false; stopped = false; aborted = false
  constructor() { FakeRec.last = this }
  start() { this.started = true }
  stop() { this.stopped = true; this.onend?.() }
  abort() { this.aborted = true; this.onerror?.({ error: 'aborted' }); this.onend?.() }
  say(...parts: [string, boolean][]) { this.onresult?.({ results: parts.map(([t, f]) => ({ isFinal: f, 0: { transcript: t } })) }) }
  end() { this.onend?.() }
  fail(error: string) { this.onerror?.({ error }); this.onend?.() }
}

describe('dettatura', () => {
  it('senza API il tasto non compare; con SpeechRecognition o webkitSpeechRecognition sì', () => {
    expect(speechCtor({})).toBeNull()
    expect(speechCtor(undefined)).toBeNull()
    expect(speechCtor({ SpeechRecognition: FakeRec })).toBe(FakeRec)
    expect(speechCtor({ webkitSpeechRecognition: FakeRec })).toBe(FakeRec)
  })

  it('nella lingua dell\'app, con i risultati intermedi, fino alla fine del parlato', () => {
    dictate(FakeRec, dictLang('it'), '', { text: () => {}, end: () => {} })
    expect(FakeRec.last).toMatchObject({ lang: 'it-IT', interimResults: true, continuous: false, started: true })
    expect(dictLang('en')).toBe('en-US')
  })

  it('il testo intermedio e poi il finale compaiono nel campo, dopo quello già scritto', () => {
    const text = vi.fn()
    dictate(FakeRec, 'it-IT', 'Controlla', { text, end: () => {} })
    const rec = FakeRec.last!
    rec.say(['i test', false])
    expect(text).toHaveBeenLastCalledWith('Controlla i test')
    rec.say(['i test del checkout', true], [' e poi', false])
    expect(text).toHaveBeenLastCalledWith('Controlla i test del checkout e poi')
    rec.say(['i test del checkout', true], [' e poi pubblica', true])
    expect(text).toHaveBeenLastCalledWith('Controlla i test del checkout e poi pubblica')
  })

  it('non invia mai: alla fine resta solo il testo nel campo, senza errore', () => {
    const on = { text: vi.fn(), end: vi.fn() }
    const before = Object.keys(on)
    dictate(FakeRec, 'it-IT', '', on)
    FakeRec.last!.say(['manda il report', true])
    FakeRec.last!.end()
    expect(on.text).toHaveBeenLastCalledWith('manda il report')
    expect(on.end).toHaveBeenCalledExactlyOnceWith(null)
    // Il controllo della dettatura ha solo il campo e la fine: nessuna strada verso l'invio.
    expect(Object.keys(on)).toEqual(before)
  })

  it('il secondo tocco ferma e tiene il testo; l\'invio stacca i risultati in ritardo', () => {
    const text = vi.fn(), end = vi.fn()
    const d = dictate(FakeRec, 'it-IT', '', { text, end })
    FakeRec.last!.say(['ciao', false])
    d.stop()
    expect(FakeRec.last!.stopped).toBe(true)
    expect(end).toHaveBeenCalledExactlyOnceWith(null)

    const t2 = vi.fn(), e2 = vi.fn()
    const d2 = dictate(FakeRec, 'it-IT', '', { text: t2, end: e2 })
    const rec = FakeRec.last!
    d2.cancel()
    rec.say(['in ritardo', true])
    expect(t2).not.toHaveBeenCalled()
    expect(rec.aborted).toBe(true)
    expect(e2).toHaveBeenCalledExactlyOnceWith(null)
  })

  it('permesso negato: la fine porta l\'errore, il campo resta com\'era', () => {
    const text = vi.fn(), end = vi.fn()
    dictate(FakeRec, 'it-IT', 'già scritto', { text, end })
    FakeRec.last!.fail('not-allowed')
    expect(end).toHaveBeenCalledExactlyOnceWith('denied')
    expect(text).not.toHaveBeenCalled()
  })

  it('gli errori: permesso, nessun parlato, il resto; aborted è il nostro stop', () => {
    expect(dictError('service-not-allowed')).toBe('denied')
    expect(dictError('no-speech')).toBe('no_speech')
    expect(dictError('network')).toBe('failed')
    expect(dictError('aborted')).toBeNull()
  })

  it('lo spazio fra scritto e dettato solo se serve', () => {
    expect(joinDictation('', ' ciao ')).toBe('ciao')
    expect(joinDictation('ciao', 'mondo')).toBe('ciao mondo')
    expect(joinDictation('ciao\n', 'mondo')).toBe('ciao\nmondo')
    expect(joinDictation('ciao', '  ')).toBe('ciao')
  })
})
