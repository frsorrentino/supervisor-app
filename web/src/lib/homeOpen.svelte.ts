// La scheda di una sessione da aprire nella home (app-verifica-conto-elenco-12-11): sulla plancia larga la home è in più parti
// (Home.svelte con `part`), ognuna col suo stato; la riga «N prove nella sua scheda» sta in una parte e la scheda della sessione
// in un'altra. La richiesta passa da qui: `at` cambia a ogni tocco, così anche la stessa sessione toccata due volte si riapre.
export const homeOpen = $state<{ name: string | null; at: number }>({ name: null, at: 0 })

export function openSessionCard(name: string) {
  homeOpen.name = name
  homeOpen.at = Date.now()
}
