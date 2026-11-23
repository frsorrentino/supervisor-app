// app-per-te-ordine-23-11: i gruppi di Per te in Dalla master nascono chiusi; quelli aperti a mano restano aperti finché l'app è aperta,
// anche se la sezione si ridisegna. Una chiave per sessione ('' = senza sessione).
export const perTeOpen = $state<Record<string, boolean>>({})
export const togglePerTe = (key: string) => { perTeOpen[key] = !perTeOpen[key] }
