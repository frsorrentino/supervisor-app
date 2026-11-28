import type { State } from './contract'
import { MASTER } from './summary'

// app-allegati-carta-27-11 (Franz 27/11 20:47, su una carta del Banco prova: «questo campo di testo dovrebbe avere la possibilità di
// caricare allegati come nelle sessioni»): nel campo di risposta di una carta (Ti aspetta, Banco prova) il + del campo delle sessioni,
// con gli stessi limiti e la stessa anteprima. Gli allegati partono prima della risposta, in un `report` alla sessione del compito, così
// la sessione li ha già quando legge la risposta. Porta di AllegatiCarta.kt, coi casi di core/src/test/resources/allegati-carta-cases.json.

/** Quanti allegati per risposta: gli stessi del campo delle sessioni (Composer). */
export const MAX_ALLEGATI = 5

/** Un allegato scelto nel campo, con l'indirizzo della sua anteprima (solo le immagini). */
export type Allegato = { file: File; url: string | null }

/**
 * Carica gli allegati di una risposta e manda il `report` alla sessione scelta da [allegatiTarget] (`session` e `task` della carta,
 * `title` per il testo che li lega alla carta). Null se sono arrivati, altrimenti il motivo: la risposta non parte.
 */
export type CardFiles = (session: string | null | undefined, task: string | null | undefined, title: string, files: File[]) => Promise<string | null>

/** Le anteprime non servono più: si liberano. */
export function revoke(xs: Allegato[]) { for (const x of xs) if (x.url) URL.revokeObjectURL(x.url) }

/**
 * A quale sessione vanno gli allegati: quella della carta (`session` di una voce di `attese`, di una prova), poi quella del compito nel
 * registro (`commitments[].session` del `task`), la prima delle due che è viva. Senza nessuna viva la master: una sessione chiusa
 * rifiuta il `report` («no session»), e la master sa a chi passarli.
 */
export function allegatiTarget(session: string | null | undefined, task: string | null | undefined, st: Pick<State, 'sessions' | 'commitments'> | null): string {
  const live = new Set((st?.sessions ?? []).filter(s => s.state !== 'gone').map(s => s.name))
  const ofTask = task ? st?.commitments?.find(c => c.task === task)?.session ?? null : null
  return [session, ofTask].find((n): n is string => !!n && live.has(n)) ?? MASTER
}
