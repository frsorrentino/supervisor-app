import type { BoardFilter } from './kpi'
// Il filtro acceso dal tabellone (app-tabellone-filtri-14-11): vale per la Home finché l'app è aperta, e non si ricorda fra una visita
// e l'altra. Sta qui e non in Home.svelte perché la Home si ricrea tornando da una sessione, e il filtro deve restare.
export const boardFilter = $state<{ on: BoardFilter | null }>({ on: null })
