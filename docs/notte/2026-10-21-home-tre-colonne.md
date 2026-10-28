# Home su desktop senza sessioni: tre colonne (21/10/2026)

Esito: plancia vuota della web a tre colonne, pubblicata alle 13:17 su Firebase e sul relay locale (stessa build `index-hTr8VfFj.js`). Ramo `notte/home-tre-colonne`, pushato. Stato di win e pubblicazioni di oggi aspettano il contratto 1.75, chiesto alla master alle 13:06: i loro riquadri ci sono e dicono che i dati non arrivano ancora.

## Ramo

`notte/home-tre-colonne` parte da `notte/gantt` (71d86224). `notte/gantt` è il punto più recente: contiene già tutto `feature/tablet`. Dopo ho unito anche i golden che `notte/gantt` ha preso alle 13:17 (`c2dd62cb`).

## Cosa c'è

Senza colonne aperte né pagine, a destra della home:

1. **Cosa serve da te.**
   - «Ti aspettano»: le voci di `attention`, la più vecchia prima, con da quanto aspettano. Il tocco apre la sessione in colonna.
   - Poi il Recap: filtri, Azioni, «Aspetta te», «Fermo su altro», «Fatto e sospeso».
2. **Il lavoro.**
   - Il riquadro della notte, sempre presente quando c'è un rapporto: si chiede appena lo stato lo annuncia. Mentre lo mostra il cruscotto, esce dalla home.
   - «Può farlo Claude».
   - Sessioni per stato (al lavoro, aspettano, ferme) e le ultime 5 chiuse, che si aprono al tocco.
3. **Numeri e strumenti.**
   - I pannelli della quota, cioè 5 ore, settimana e eventi per ora. Erano già nel vecchio cruscotto.
   - PC Windows e Pubblicazioni di oggi: vuoti, con la frase «arrivano quando il relay li manda: richiesta fatta al PC (contratto 1.75)».
   - Ricorrenti: il tocco mette il testo nel campo della master.
   - Lancio rapido: i progetti usati di recente, senza quelli che hanno già una sessione viva. Il primo tocco sceglie il progetto, «Lancia …» lo avvia.

**Disposizione:** colonne larghe al massimo 440 px. Sono 3 da 1400 px di finestra, 2 da 1000 px, una sotto. Il cruscotto scorre tutto insieme, così a 2 colonne la terza va sotto senza tagli. Il telefono non cambia.

**Gantt nel riquadro della notte:** non c'è. La web non ha il Gantt (vedi il resoconto `2026-10-21-notte-gantt.md`), quindi per ora il riquadro è quello di oggi, come prevede la proposta.

## Codice

- `web/src/lib/dash.ts`: i conti.
  - Colonne secondo la larghezza.
  - «Ti aspettano».
  - Sessioni per stato.
  - Progetti recenti: `sessions[].project` è relativo, `projects[].path` è assoluto.
- `web/src/lib/dash.test.ts`: 4 test sulle fixture del contratto.
- `web/src/lib/Dash.svelte`: le tre colonne.
- `RecapPage.svelte`: prende `part` (`you`, `claude`, `all`), per dividere il Recap fra la colonna 1 e la colonna 2 senza duplicarlo.
- `App.svelte`: il vecchio cruscotto a due colonne (Recap e Utilizzo) è sostituito.

## Prove

- vitest su win: 602 su 603. Fallisce `zip.test.ts` per la codifica di `unzip` su Windows, come nel giro del Gantt: non tocca questo lavoro.
- `npm run check`: 5 errori, tutti in file che non ho toccato (`demo.ts`, `demoTransport.ts`, `pdfPreview.ts`). Ci sono anche con `npm ci` pulito; nessuno nei file nuovi o cambiati.
- `npm run build`: verde.
- Screenshot con chromium headless della demo a plancia vuota: 1440 px (3 colonne), 1200 (2), 900 (una) e 412 (telefono, invariato). Sono in `/tmp/claude-1000/-home-franz-Desktop-workspaces-personali-supervisor-app/7ffa5bf2-513e-40c7-b1a1-851ca169fde3/scratchpad/home3/`. Sono in inglese per la lingua del browser headless.
- Prima del deploy ho controllato la build: niente indirizzi email, niente chiavi.

## Contratto 1.75 (chiesto alla master, 13:06)

- `hosts_status`: `[{name, up, at, error, cpu, ram_used, ram_total, disk_free, disk_total, sessions}]`.
- `published_today`: `[{at, kind (release | push | deploy), project, text, url}]` dalla mezzanotte, al massimo 30.

Fixture proposte: win su e tre pubblicazioni in state-1; liste vuote in state-2; win giù in state-3. Quando il relay le scrive, questa sessione le copia e riempie i due riquadri.

## Da te

Ricarica la web app aperta (Ctrl+R): una finestra già aperta tiene la build di prima.

FINE
