# Supervisor su Play: grafica e scheda (21/10/2026)

## Scheda dello store

Testi in `listing.json`, en-US (predefinita) e it-IT, già in Console dal 13/10. Limiti controllati il 21/10:

| | limite | en-US | it-IT |
|---|---|---|---|
| Titolo «Supervisor» | 30 | 10 | 10 |
| Descrizione breve | 80 | 78 | 77 |
| Descrizione completa | 4000 | 1036 | 1068 |

Categoria: Produttività. Contatti nella scheda: oggi info@francescosorrentino.com e
https://supervisor.francescosorrentino.com; da mettere quelli di Pixelfarm (vedi `da-fare-franz.md`, passo 1).

La descrizione completa nomina «the author» e «l'autore» per dire chi non può leggere i dati: dopo il trasferimento
resta vera, ma si può scrivere «neither Google nor Pixelfarm» / «né Google né Pixelfarm». Decisione di Franz.

## Materiali

| Materiale | Richiesto da Play | File | Stato |
|---|---|---|---|
| Icona | 512×512 PNG | `icon-512.png` | in Console dal 13/10 |
| Grafica in primo piano | 1024×500 | `feature-graphic-1024x500.png` | in Console dal 13/10 |
| Screenshot telefono | 2-8, lato lungo ≤ 2× il corto | `screenshot/phone-store/{en,it}/s1…s4` (1080×2160) | in Console (en) |
| Screenshot tablet 7" e 10" | facoltativi; 16:9 o 9:16, lati 320-3840 px (7") e 1080-7680 px (10") | `screenshot/tablet-store/{en,it}/t1…t3` (2880×1620, 16:9) | **nuovi, da caricare** |
| Screenshot orologio | almeno 1, quadrati, ≥ 384 px | `screenshot/watch-store/*` (456×456) | in Console dal 13/10 |

### Screenshot tablet (nuovi, 21/10)

Generati con Paparazzi dalle stesse schermate dei golden del tablet, a risoluzione piena:

```
./gradlew :mobile:testDebugUnitTest --tests '*TabletShellTest*' -Pstore.shots=true -Pstore.locale=it   # poi en
```

Le immagini escono in `mobile/build/store-shots/tablet-<lingua>/`, a 2880×1620: la Console accetta solo 16:9 per
i tablet (lo dice la scheda in Console; il primo giro era a 16:10). Scelte tre:

1. `t1-tabletDesk` — home a sinistra, due conversazioni affiancate, una domanda che aspetta.
2. `t2-tabletDeskHomeRightDetails` — home a destra con i dettagli della sessione.
3. `t3-tabletDeskChanges` — la vista «Modifiche» di una sessione accanto all'altra.

`tabletDeskLargeFont` resta fuori: cambia la configurazione dentro il test e torna a 16:10.

Da guardare prima di caricarli: la domanda di esempio della fixture contiene «DEPLOY_TOKEN=dpl_9f8e7d6c5b4a» (inventato,
ma sulla scheda sembra un segreto in chiaro). Se Franz preferisce, si cambia il testo della domanda solo per gli
screenshot.
