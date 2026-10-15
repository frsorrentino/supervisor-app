import { describe, expect, it } from 'vitest'
import { of, stable, step } from './swipePages'

// Gli stessi casi di SwipePagesTest in Kotlin.
describe('SwipePages', () => {
  it('la home è sempre la prima pagina', () => expect(of(['a', 'b'], null)).toEqual([null, 'a', 'b']))
  it('senza sessioni solo la home', () => expect(of([], null)).toEqual([null]))
  it('la master o una chiusa sono una pagina solo se aperte', () => {
    expect(of(['a'], 'master')).toEqual([null, 'a', 'master'])
    expect(of(['a'], 'x')).toEqual([null, 'a', 'x'])
  })
  const pp = [null, 'team-supervisor-app', 'motion-graphic-video']
  it('dopo un riordino la striscia torna alla sessione aperta', () =>
    expect(step(2, 2, false, pp, 'team-supervisor-app')).toEqual({ type: 'scrollTo', page: 1 }))
  it('uno scorrimento su un\'altra sessione la apre', () =>
    expect(step(1, 2, false, pp, 'team-supervisor-app')).toEqual({ type: 'open', name: 'motion-graphic-video' }))
  it('uno scorrimento sulla prima pagina torna alla home', () =>
    expect(step(1, 0, false, pp, 'team-supervisor-app')).toEqual({ type: 'open', name: null }))
  it('una sessione aperta da fuori: la striscia ci va', () => {
    expect(step(1, 1, false, pp, 'motion-graphic-video')).toEqual({ type: 'scrollTo', page: 2 })
    expect(step(null, 1, false, pp, 'motion-graphic-video')).toEqual({ type: 'scrollTo', page: 2 })
  })
  it('mentre scorre niente', () => expect(step(1, 1, true, pp, 'motion-graphic-video')).toEqual({ type: 'none' }))
  it('ferma sulla sessione aperta niente; aperta sparita niente', () => {
    expect(step(1, 1, false, pp, 'team-supervisor-app')).toEqual({ type: 'none' })
    expect(step(1, 1, false, pp, 'zeta')).toEqual({ type: 'none' })
  })
  it('durante il gesto l\'ordine resta, le nuove in fondo, le sparite escono', () => {
    const prev = [null, 'a', 'b', 'c']
    expect(stable(prev, [null, 'b', 'a', 'c'])).toEqual([null, 'a', 'b', 'c'])
    expect(stable(prev, [null, 'd', 'c', 'a'])).toEqual([null, 'a', 'c', 'd'])
    expect(stable([], [null, 'b', 'a'])).toEqual([null, 'b', 'a'])
  })
})
