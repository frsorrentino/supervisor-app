// Il file non c'è più sul PC (app-file-mancante-24-11, Franz 24/11 21:08: «cos'è questo missing or unreadable che mi appare spesso?»):
// citato nella conversazione e poi spostato o cancellato. Il relay lo dice con «missing or unreadable». Non torna da solo: la web lo
// segna non disponibile, non lo richiede e lo dice in italiano col nome, solo al tocco. FileRefusal.gone sul telefono.
export const fileGone = (reason: string | null | undefined): boolean => !!reason && /missing or unreadable|no such file/i.test(reason)

/** Il nome che si legge nell'avviso: l'ultima parte del percorso. */
export const fileName = (path: string): string => path.replace(/\/+$/, '').split('/').pop() || path

/** I file spariti, per sessione e percorso: il tocco dice l'avviso senza chiedere di nuovo il file al PC. */
export class GoneFiles {
  private gone = new Set<string>()
  private k = (session: string | null, path: string) => `${session ?? ''}\n${path}`
  has(session: string | null, path: string): boolean { return this.gone.has(this.k(session, path)) }
  add(session: string | null, path: string): void { this.gone.add(this.k(session, path)) }
}
