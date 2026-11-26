import { t } from './t'

/**
 * app-per-te-trascina-nome-26-11 (Franz 26/11 22:12: «se faccio drag and drop della sezione per te, diventa decidi mentre trascino»):
 * la sezione `decisioni` della Home si chiama «Per te», con la mano, ovunque: nella Home, nell'anteprima che segue il dito e in
 * Riordina. Un nome solo, da qui.
 */
export const perTeSec = (): { icon: string; name: string } => ({ icon: 'svg:hand', name: t.forYou })
