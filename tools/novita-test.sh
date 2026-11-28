#!/usr/bin/env bash
# Il test di tools/novita.sh sui commit veri (app-novita-cosa-cambia-28-11): b44c44f3 ha la riga `Novita:` staccata dal trailer e
# deve darla lo stesso, con un avviso; 3dda3e46 la ha nel trailer e la dà come prima, senza avvisi; un commit di soli golden non
# dà niente. Esce con 1 al primo controllo che non torna.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
warns=$(mktemp)
trap 'rm -f "$warns"' EXIT
fail() { echo "NO: $1" >&2; exit 1; }

out=$(bash tools/novita.sh b44c44f3^..b44c44f3 3>"$warns")
grep -Fxq "L'anello e il raggio della master corrono attorno a tutto il suo foglio; il campo torna una pillola semplice." <<< "$out" \
  || fail "b44c44f3: manca la sua novità (uscita: $out)"
grep -q '^::warning::commit b44c44f3' "$warns" || fail "b44c44f3: manca l'avviso della riga fuori dal trailer"

: > "$warns"
out=$(bash tools/novita.sh 3dda3e46^..3dda3e46 3>"$warns")
grep -Fxq "La risposta nuova arriva con l'esito per primo, fermo dove c'era «sta pensando», mentre la chat sale una volta sola" <<< "$out" \
  || fail "3dda3e46: manca la sua novità (uscita: $out)"
[ ! -s "$warns" ] || fail "3dda3e46: avviso inatteso: $(cat "$warns")"
[ "$(printf '%s\n' "$out" | grep -c .)" -eq 1 ] || fail "3dda3e46: righe doppie: $out"

out=$(bash tools/novita.sh c06855bd^..c06855bd 3>/dev/null)
[ -z "$out" ] || fail "c06855bd (soli golden): uscita inattesa: $out"

echo "ok: novita.sh legge trailer e righe staccate"
