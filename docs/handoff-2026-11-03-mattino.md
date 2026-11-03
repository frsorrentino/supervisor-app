# Handoff supervisor-app, 03/11/2026 mattina

## 1. Stato
Notte in autonomia chiusa (mandato di Franz delle 00:43 via master, inbox 9cac01; cinque giri difetti chiesti dalla master). Ramo `tab-emoji/22-10`, tutto pushato. `docs/recap.md` modificato e i `docs/handoff-2026-11-02-*.md` non tracciati non sono di questa sessione: non committarli.

Tutti i compiti sono **in pausa per la prova di Franz** (`task pause … --waits person`): `app-attesa-dettagli-02-11`, `app-da-guardare-02-11`, `app-luci-02-11`, `app-bug-notte-03-11`. Nel registro restano «running» con la pausa a parte: è normale.

## 2. Uscito stanotte
| Compito | Pre-release (APK) | Web |
|---|---|---|
| app-attesa-dettagli-02-11 | `scheda-dettagli-2026-11-03` (e7232dd3) | 0.2.1749 |
| app-da-guardare-02-11 + app-luci-02-11 | `guardare-luci-2026-11-03` (482933c9) | 0.2.1755 |
| app-bug-notte-03-11, giro 1 | `difetti-notte-2026-11-03` (f1d8ca39) | 0.2.1755 |
| giro 2 | `difetti-notte-2-2026-11-03` (39800dd0) | 0.2.1763 |
| giro 3 | `difetti-notte-3-2026-11-03` (9d97d157) | 0.2.1766 |
| giro 4 | nessuna (solo CSS web) | 0.2.1768 |
| giro 5 | `difetti-notte-5-2026-11-03` (1831a36b) | 0.2.1768 |

L'APK più recente contiene tutto quello prima: basta installare l'ultima pre-release.

## 3. Cosa fanno
- **Scheda aperta di una sessione**: chi aspetta Franz mostra l'ultimo messaggio (esito e proposta) e tutte le voci di Prossimi come tasti sotto l'Ok; chi lavora dice cosa sta facendo; statistiche in fondo. Regola `SessionDetail` / `sessionDetail.ts`.
- **Da guardare** (contratto 1.106, fixture del relay copiate intere): voce in Esiti, Tutti i risultati, riga Risultati in cima alla chat, risultato aperto col lettore della chat, Visto, nota col secondo del video, «Riprova» se il file non arriva. Regola `Guardare` / `guardare.ts`.
- **Luci**: bordo acceso solo in alto a sinistra, spia ambra sottile e pillola, punto di oggi che brilla, sfondo illuminato su tablet e web largo (plancia a pannelli).
- **Difetti corretti** (ognuno col suo commit `app-bug-notte-03-11`): Prima raccolta in ambra; titolo e margini della riga Risultati; traduzioni inglesi mancanti; «…» nel testo di chiusura; Da guardare a 320 dp; tasti Heads up spezzati; Esito tipizzato nella demo; file che non arriva; aree di tocco (pillole, chip, righe, testate di sezione nel margine); pillola «Apri il file»; nome accessibile dei tasti della modalità live (TalkBack leggeva solo «pulsante»).

## 4. Note per Franz (non toccate: disegno o funzione nuova)
- Video in Da guardare: in elenco la tessera col ▶, non il fotogramma (servirebbe scaricare ogni video).
- La riga chiusa di Esiti non conta Da guardare.
- Testate di sezione: sul web l'area di tocco arriva a ~37 px dentro il margine; 48 pieni (web e V3Section del telefono, ~34 dp) chiedono spaziature nuove.
- Titoli tagliati a metà parola nella riga Risultati delle colonne strette e a 320 col testo grande.
- Colonne di chat strette della plancia (~225 px): i nomi dei file si spezzano (già prima).
- Nella demo italiana restano testi inglesi delle fixture; «ferma» ha due sensi (proposto «inattive»).
- Il pizzico a due dita non è mai stato provato su un dispositivo.
- Contrasto dei colori: non verificato (disegno).
- Telefono a 320 dp col testo grande: i riassunti a destra delle testate vanno a capo a frammenti e spezzano parole («6 / disposi / tivi» in Impostazioni, «1 ti / aspetta ·» in Sessioni, «1 / ×» in Lettura ad alta voce). Cosa nascondere quando non c'è posto è una scelta di disegno. Schermate: `mobile/src/test/snapshots/images/*giro5*_320Grande.png`.
- Per accessibilità e aree di tocco ci sono test solo per il file che non arriva (Paparazzi) e per l'Esito nella demo (vitest); aree di tocco, etichette e tasti Heads up sono verificati a mano.

## 5. Fatti verificati
- Test: core verdi salvo i 5 `ChatLogTest` (Windows); vitest 919/920 (`zip.test`, Windows); svelte-check solo i 2 errori esrap; CI verdi con golden accettate a ogni giro.
- Bundle web: main.js 963 kB (gzip 297) contro 939 kB (289) di ieri sera, +24,6 kB per le funzioni nuove.
- Quota settimana personale 46% alle 05:30 (reset gio 05/11 03:00).

## 6. Da fare al risveglio
- Prova di Franz delle pre-release e della PWA (Ctrl+R).
- Al via di Franz: `task done` dei compiti provati.
