import { reducedMotion } from './motion'

// app-anteprime-webp-27-11: «riduci movimento» del sistema, letto anche quando cambia a pagina aperta. Le anteprime animate restano
// al primo fotogramma col ▶ finché è attivo.
export const movimento = $state({ ridotto: reducedMotion() })
if (typeof matchMedia === 'function') matchMedia('(prefers-reduced-motion: reduce)').addEventListener?.('change', e => { movimento.ridotto = e.matches })
