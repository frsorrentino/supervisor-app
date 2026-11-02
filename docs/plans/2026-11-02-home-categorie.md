# Home per categorie: piano di attuazione

> **Per chi esegue:** sotto-skill superpowers:executing-plans (nativo) o superpowers:subagent-driven-development. Passi con `- [ ]`.

**Obiettivo:** telefono e PWA mostrano le stesse 4 categorie con le stesse sezioni nello stesso ordine; sul telefono le categorie si aprono e si chiudono, quelle chiuse hanno un segnale sulla riga; Riordina sposta categorie e sezioni e decide quali categorie sono in vista.

**Architettura:** un modello unico mirrorato (`core/.../rules/HomeCats.kt` e `web/src/lib/homeCats.ts`, stessi vettori di test) sostituisce l'ordine piatto di `HomeOrder`/`homeOrder.ts`. Si salva nello stesso `state.home_order`, senza toccare il relay. Le UI (telefono `SummaryList.kt` e `HomeOrderScreen.kt`, web `Home.svelte`, `HomeOrder.svelte` e la plancia di `App.svelte`) leggono solo il modello.

**Stack:** Kotlin + Compose (Paparazzi), Svelte 5 + vitest.

**Spec:** compito `app-home-categorie-02-11` (`supervisor task show`), mockup approvati in `/home/franz/Desktop/workspaces/.claude/proposte/2026-11-02-home-categorie/home-categorie-{telefono,pwa}.{png,html}`.

## Vincoli globali

- Categorie e sezioni, in quest'ordine predefinito: **Richiede te** (Da decidere, Da provare, Suggerimenti) · **Attività e code** (Sessioni, Da seguire, Lavori con la Notte dentro) · **Esiti** (Esiti di oggi, Da guardare, Rendiconti) · **Numeri e strumenti** (Utilizzo, Pagella, Sistema con dentro Plugin e skill).
- Telefono, predefinito: Richiede te e Attività aperte; Esiti e Numeri chiuse, una riga con a destra il segnale, in ambra quando c'è qualcosa. Nessuna sezione cambia posto da sola.
- I tre riquadri in alto (ti aspetta, al lavoro, esiti oggi) aprono la loro categoria.
- Una sezione resta sempre nella sua categoria. Ordine e categorie valgono anche per le colonne della PWA.
- PWA larga: una colonna per categoria come la plancia di oggi.
- Da guardare, Rendiconti e Pagella: qui solo la chiave e il posto; il contenuto è di app-da-guardare-02-11 e app-rendiconti-2-02-11. Finché non esistono non si disegnano.
- Relay invariato: `home_order` ≤ 16 chiavi, ognuna ≤ 32 caratteri (contratto 1.97).
- Testi in `strings.xml` e `t.ts`, mai cablati; un solo bottone pieno per schermata; mai «…».

## Scelte mie, da confermare (il mockup non le dice)

1. **Prima raccolta** va in Richiede te, prima di Da decidere: chiede un tuo tocco (Avvia, consenso). Sparisce come oggi quando è finita.
2. **Chiuse** (sessioni chiuse) entra in Sessioni, in fondo: la riga del mockup dice già «1 al lavoro · 1 finita».
3. **Lo slot del tabellone** resta com'è: il mockup non lo mostra, ma toglierlo sarebbe un cambio non chiesto.
4. «Ti aspettano» e le richieste di login restano fisse sopra le categorie, come oggi.

## Formato salvato

`home_order` = le chiavi di sezione nell'ordine voluto, poi `shut:<id,id>` con le categorie chiuse, poi l'eventuale `pin:<chiave>` di oggi. Massimo 13 + 1 + 1 = 15 chiavi.

- Id categorie: `you`, `work`, `out`, `tools` (gli stessi `part` della plancia).
- Chiavi di sezione: `prima_raccolta`, `decisioni`, `prove`, `suggestions` · `sessions`, `seguiti`, `lavori` · `esiti`, `da_guardare`, `rendiconti` · `usage`, `pagella`, `system`.
- L'ordine delle categorie è quello della loro prima sezione nella lista.
- Lettura di un ordine vecchio: `altro`, `plugins`, `closed` si ignorano; ogni sezione va nella sua categoria con l'ordine relativo salvato; le mancanti si accodano nella loro categoria; senza `shut:` vale il predefinito (`out,tools` chiuse).
- Il predefinito si manda come `null` (arg vuoto), come oggi.

## Punti critici da provare

1. Un ordine salvato dalla versione di ieri (piatto, con `closed`, `plugins`, `pin:usage`) si apre senza perdere il pin e senza sezioni duplicate o perse.
2. Una categoria con tutte le sezioni vuote (es. Esiti senza esiti oggi): la riga chiusa resta con il segnale «niente oggi», non sparisce. Nessuno spostamento.
3. Trascinare una sezione oltre il bordo della sua categoria non la porta nell'altra.
4. Tocco su un riquadro con la categoria già aperta: scorre fino a lei, non la chiude.
5. Testo grande (fontScale 1.3) e 320 dp: nome categoria e segnale su una riga ciascuno, segnale a capo intero se non ci sta, mai tagliato.

---

### Compito 1: il modello `HomeCats`

**File:**
- Crea: `core/src/main/kotlin/it/pixelbox/cmwatch/rules/HomeCats.kt`, `web/src/lib/homeCats.ts`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/rules/HomeCatsTest.kt`, `web/src/lib/homeCats.test.ts` (stessi casi, stessi valori)

**Interfacce prodotte:**
- `data class Cat(val id: String, val open: Boolean, val sections: List<String>)` / `type Cat = { id: CatId; open: boolean; sections: HomeKey[] }`
- `HomeCats.DEFAULT: List<Cat>`; `decode(saved: List<String>?): List<Cat>`; `encode(cats: List<Cat>, pin: String?): List<String>?` (null = predefinito); `moveCat(cats, from, to)`; `moveSection(cats, catId, from, to)` (solo dentro la categoria); `toggle(cats, catId)`; `catOf(key): String`; `pin(saved)` riusa `HomeOrder.pinned`.
- TS: stessi nomi in camelCase, `encode` restituisce la stringa JSON dell'arg o null (come `orderArg`).

- [ ] Test: `decode(null) == DEFAULT`; `encode(DEFAULT, null) == null`; giro andata e ritorno di un ordine con `work` prima di `you`, `you` chiusa e `pin:usage`; ordine vecchio `["sessions","closed","usage","prove","plugins","pin:usage"]` → `work` prima, poi `tools`, poi `you`, poi `out`; `you` con `prove` prima di `prima_raccolta`, `decisioni`, `suggestions`; `pin(saved) == "usage"`; nessuna `closed`/`plugins`; chiavi ≤ 15 e ognuna ≤ 32 caratteri per ogni ordine possibile del predefinito; `moveSection` con `to` fuori dalla categoria → invariato.
- [ ] Rossi, poi implementazione, poi verdi (`:core:test --tests '*HomeCatsTest*'` su win; `npx vitest run src/lib/homeCats.test.ts`).
- [ ] Commit `feat(core,web): the home model by categories … (app-home-categorie-02-11)`.

### Compito 2: telefono, la home per categorie

**File:** `mobile/.../ui/SummaryList.kt` (render per categoria; il trascinamento sul posto resta, limitato alla categoria), `mobile/src/main/res/values/strings.xml` (`cat_you` «Richiede te», `cat_work` «Attività e code», `cat_out` «Esiti», `cat_tools` «Numeri e strumenti»), `mobile/src/test/.../SummaryListTest.kt`.

**Consuma:** `HomeCats.decode`, `encode`, `toggle`, `moveSection`.

- Categoria aperta: titolo maiuscolo piccolo (come «RICHIEDE TE» nel mockup), poi le sue sezioni come oggi.
- Categoria chiusa: una riga (icona, nome, a destra il segnale, freccia). Si apre sul posto in una card con le sue sezioni (mockup 2). Aprire e chiudere in home non salva: lo stato salvato si cambia solo in Riordina.
- Segnale: Esiti = «N oggi», più «N fallito» in rosso, più «N da guardare»; Numeri = «N% · carico X»; ambra (fondo e testo) quando c'è un fallito, qualcosa da guardare, un allarme della settimana o carico alto (la soglia già usata da Sistema).
- Riquadri: «ti aspetta» → apre `you` e ci scorre; «al lavoro» → `work`; «esiti oggi» → `out`.
- Sistema contiene Plugin e skill; Sessioni contiene le chiuse in fondo; Lavori contiene la Notte (già oggi: verificare).

- [ ] Paparazzi: `homeByCategories` (predefinito, dati di `state-1-question`), `homeClosedCategorySignal` (ambra, un fallito), `homeCategoryOpenedInPlace`, `homeCategoriesFont13At320`. Golden dalla CI (`record=true`), confrontati a occhio col mockup prima del commit.
- [ ] Test di comportamento (motore di frame Paparazzi, come ChatScrollTest): tocco su «esiti oggi» apre Esiti; trascinare Da provare sotto Sessioni la lascia in Richiede te.
- [ ] Commit.

### Compito 3: telefono, Riordina per categorie

**File:** `mobile/.../ui/HomeOrderScreen.kt`, strings (`reorder_in_view` «in vista», `reorder_closed` «chiusa», `reorder_foot` «Ordine e categorie valgono anche per le colonne della PWA.»), test Paparazzi `HomeOrderScreenTest`.

- Una card per categoria: maniglia, nome, «in vista»/«chiusa», interruttore; sotto le sezioni con maniglia e icona. Trascinamento con la pressione lunga sulla maniglia; frecce su/giù accessibili con TalkBack come oggi. «Salva» unico tasto pieno; «Predefinito» testuale.
- [ ] Golden `reorderByCategories`; test: interruttore di Esiti → `encode` contiene `shut:` senza `out`.
- [ ] Commit.

### Compito 4: PWA stretta, home e Riordina

**File:** `web/src/lib/Home.svelte`, `web/src/lib/HomeOrder.svelte`, `web/src/lib/t.ts`, test vitest esistenti della home (`Home*.test.ts`) aggiornati.

Stesso comportamento dei compiti 2 e 3, con gli stessi testi. Il trascinamento con il puntatore resta, limitato alla categoria.

- [ ] vitest: ordine dei `data-sec-head` = `decode(st.home_order)` appiattito sulle categorie aperte; riga chiusa con `data-testid="cat-out"` e segnale; clic sul riquadro esiti apre `out`.
- [ ] Prova nel browser (chrome-bridge, `?demo`), screenshot a 412 px confrontato col mockup. localStorage rimesso com'era.
- [ ] Commit.

### Compito 5: PWA larga, una colonna per categoria

**File:** `web/src/App.svelte` (plancia), `web/src/lib/Home.svelte` (`part`).

- Le colonne seguono l'ordine delle categorie salvato, tutte e quattro, anche quelle chiuse sul telefono. Testata di colonna: nome maiuscolo piccolo, numero grande, etichetta (1 ti aspetta, 1 al lavoro, 102 esiti oggi, 45% settimana); sotto le sezioni nell'ordine salvato.
- `part` usa l'ordine dentro la categoria invece di `DEFAULT_ORDER`. Le sezioni di oggi assegnate ad altre colonne (es. `prove` e `decisioni` → you, `seguiti` e `prima_raccolta` → work) seguono le categorie nuove; Prima raccolta segue la scelta 1.
- Schermo stretto: le colonne che non stanno scendono sotto l'ultima, come oggi.
- [ ] vitest: con `home_order` che mette `work` prima, la prima colonna è Attività e code. Screenshot a 1280 × 720 contro `home-categorie-pwa.png`.
- [ ] Commit.

### Compito 6: uscita

- [ ] Golden dalla CI (`gh workflow run "Build Android APKs" --ref tab-emoji/22-10 -f record=true`), confronto con PIL, commit.
- [ ] Pre-release `home-categorie-2026-11-0X` con l'APK del run verde; web build su win e deploy; avviso a Franz con Ctrl+R.
- [ ] Prova di Franz nel registro: home predefinita, riquadri, Riordina, PWA larga.
