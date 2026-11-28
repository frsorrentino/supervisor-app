import { untrack, type Snippet } from 'svelte'

// Il ⋮ della sessione resta in testata (app-posizioni-sistema-27-11, gruppo 2), accanto al nome come nel mockup della master. Al
// telefono la testata è quella della sessione (Header, la riga col tasto indietro); nella plancia è la testata della colonna
// (ColumnHead): la sessione le presta il suo ⋮ (`lend`), e la colonna lo prende (`claim`). Senza una colonna che lo prenda (la web
// installata con la barra del titolo, una conversazione chiusa aperta in sola lettura) la sessione lo tiene accanto alla riga di icone.
// Si chiamano dentro un $effect, e ne sono il ritorno: le letture qui non diventano dipendenze dell'effetto.
export const heads = $state<{ more: Record<string, Snippet>; claims: Record<string, number> }>({ more: {}, claims: {} })

/** La sessione `name` presta il suo ⋮; il ritorno lo riprende. */
export function lend(name: string, more: Snippet): () => void {
  untrack(() => { heads.more[name] = more })
  return () => untrack(() => { if (heads.more[name] === more) delete heads.more[name] })
}

/** Una testata di colonna prende il ⋮ della sessione `name`; il ritorno lo lascia. */
export function claim(name: string): () => void {
  untrack(() => { heads.claims[name] = (heads.claims[name] ?? 0) + 1 })
  return () => untrack(() => {
    const n = (heads.claims[name] ?? 1) - 1
    if (n > 0) heads.claims[name] = n
    else delete heads.claims[name]
  })
}
