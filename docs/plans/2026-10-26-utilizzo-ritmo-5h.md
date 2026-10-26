# Utilizzo: ritmo delle cinque ore (1A) e linea con la proiezione (1B)

Compito `app-utilizzo-ritmo-5h-25-10`. Mockup `docs/proposte/2026-10-25-kpi/1-utilizzo-v2.png` (genera_v2.py).

## Dove

Il pannello della quota 5h, uguale in Utilizzo e limiti (telefono), nel cruscotto e nella colonna del tablet:
`TabletQuotaPanel` (TabletShell.kt) e `QuotaPanel.svelte`. Al posto del grafico di oggi (media della finestra, soglie
80 e 100): le due barre di 1A, la riga avanti/indietro, il grafico di 1B, la frase finale. Testata e settimana restano.

## Regola nuova: `Rhythm5h` (core) e `rhythm5h.ts` (web), stessi casi nei test

- `elapsed`: quanto della finestra è passato, 0-100.
- `ahead` = quota − tempo, in punti (negativo: indietro).
- `rate`: ritmo dell'ultima ora (come dice il mockup), dai campioni della finestra: dal primo campione nell'ora prima
  dell'ultimo all'ultimo; con meno di 10 minuti fra i due, o senza salita, nessun ritmo.
- `fullAt`: l'ora del 100% a quel ritmo, solo se cade prima del reset; `idleMin` = minuti fra il 100% e il reset.
- `projected`: al reset, se il 100% non arriva prima.
- `QuotaHistory.pace` (media della finestra) non cambia: la usano gli altri.

## Testi (IT ed EN, strings.xml e t.ts)

«tempo», «quota», «quota avanti di N punti sul tempo» / «indietro» / «al passo col tempo», «A questo ritmo 100% alle
X, reset alle Y.», «A questo ritmo N% al reset delle Y.», sul grafico «100%», «reset X», «100% alle X», «N% ora»,
«fermi N min». Senza ritmo restano le frasi di oggi (dato vecchio, seconda lettura, ferma).

## Larghezza (Franz, 21:33)

Nessun testo esce da card e pagina: le frasi vanno a capo; le etichette del grafico si tengono dentro (ancorate al
bordo), e quelle che non ci stanno (fermi N min nella fascia stretta, 100% a sinistra sotto l'etichetta del 100%) non si
disegnano. Golden Paparazzi a 320, 360 e 412 dp dalla CI; web guardato a 320, 360 e 412 px.

## Uscita

Test, pre-release su tab-emoji/22-10, deploy della web app; l'APK lo installa la master.
