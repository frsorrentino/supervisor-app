#!/usr/bin/env bash
# Il test di tools/golden-esito.sh (app-ci-golden-avviso-28-11) su risultati JUnit finti, con i messaggi veri di Paparazzi 2.0.0-alpha02:
# solo immagini cambiate → avviso ed esce con 0; un test fallito per altro → errore ed esce con 1; nessun test fallito → errore.
# Esce con 1 al primo controllo che non torna. Su win: PYTHON=python bash tools/golden-esito-test.sh
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
fail() { echo "NO: $1" >&2; exit 1; }
xml() { mkdir -p "$(dirname "$1")"; printf '<?xml version="1.0" encoding="UTF-8"?>\n<testsuite name="s" tests="3">%s</testsuite>\n' "$2" > "$1"; }
ok='<testcase classname="it.pixelbox.cmwatch.mobile.SummaryListTest" name="summaryWithUpdate"/>'
diff='<testcase classname="it.pixelbox.cmwatch.mobile.SessionSheetTest" name="sheetQuotaWarning"><failure message="java.lang.AssertionError: Images differ (by 0.412000%) - see details in file://x/delta.png" type="java.lang.AssertionError">java.lang.AssertionError: Images differ (by 0.412000%)</failure></testcase>'
size='<testcase classname="it.pixelbox.cmwatch.mobile.SummaryListTest" name="updateNewsOpen"><failure message="java.lang.AssertionError: Heights differ too much for updateNewsOpen: expected 640 but was 0" type="java.lang.AssertionError">trace</failure></testcase>'
real='<testcase classname="it.pixelbox.cmwatch.mobile.SessionSheetTest" name="sheetChat"><error message="java.lang.IllegalStateException: Vertically scrollable component was measured with an infinity maximum height" type="java.lang.IllegalStateException">trace</error></testcase>'

xml "$tmp/a/mobile/TEST-a.xml" "$ok$diff"
xml "$tmp/a/wear/TEST-b.xml" "$size"
out=$(bash tools/golden-esito.sh "$tmp/a/mobile" "$tmp/a/wear") || fail "solo immagini: è uscito con errore ($out)"
grep -q '^::warning title=Golden cambiati::2 immagini diverse dai golden (SessionSheetTest.sheetQuotaWarning, SummaryListTest.updateNewsOpen)' <<< "$out" \
  || fail "solo immagini: avviso sbagliato ($out)"

xml "$tmp/b/mobile/TEST-a.xml" "$ok$diff$real"
if out=$(bash tools/golden-esito.sh "$tmp/b/mobile"); then fail "un errore vero è passato ($out)"; fi
grep -q '^::error title=Golden::1 test falliti non per le immagini: SessionSheetTest.sheetChat' <<< "$out" || fail "errore vero: messaggio sbagliato ($out)"

xml "$tmp/c/mobile/TEST-a.xml" "$ok"
if out=$(bash tools/golden-esito.sh "$tmp/c/mobile"); then fail "senza test falliti è passato ($out)"; fi
grep -q '^::error title=Golden::la verifica è fallita senza test falliti' <<< "$out" || fail "senza test falliti: messaggio sbagliato ($out)"

echo "ok: golden-esito.sh separa le immagini cambiate dagli errori veri"
