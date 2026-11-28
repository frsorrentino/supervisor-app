#!/usr/bin/env bash
# Le Novità di un salto di commit, per la sezione «## Novità» della pre-release (app-changelog-aggiornamento-26-11) e la scheda
# dell'aggiornamento (core Novita.kt). Uso, con gli stessi argomenti di git log: tools/novita.sh <range> 3>&1 > novita.txt
#
# app-novita-cosa-cambia-28-11 (Franz 28/11 15:48, «novità ancora non funziona»): prima si leggevano solo i trailer `Novita:`.
# Una riga vuota fra `Novita:` e `Co-Authored-By:` toglie la riga dal trailer, e le Novità di una notte intera sono sparite in
# silenzio. Ora si prendono i trailer e, per tolleranza, anche le righe `Novita:` del corpo; per ognuna fuori dal trailer un
# avviso `::warning::` su fd 3 (il log del passo, se il chiamante lo apre) o su stderr. Una riga per novità, la più nuova prima,
# senza doppioni, al massimo 12.
set -euo pipefail
warn() { if { true >&3; } 2>/dev/null; then printf '%s\n' "$1" >&3; else printf '%s\n' "$1" >&2; fi }
for c in $(git log --no-merges --format=%H "$@"); do
  trailers=$(git log -1 --format='%(trailers:key=Novita,valueonly,separator=%x0A)' "$c" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')
  [ -n "$trailers" ] && printf '%s\n' "$trailers"
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    if ! printf '%s\n' "$trailers" | grep -Fxq -- "$line"; then
      warn "::warning::commit ${c:0:8}: la riga «Novita: $line» non è un trailer (una riga vuota prima di Co-Authored-By?): presa lo stesso"
      printf '%s\n' "$line"
    fi
  done < <(git log -1 --format=%B "$c" | sed -n -e 's/^Novita:[[:space:]]*//p' | sed -e 's/[[:space:]]*$//')
done | sed -e '/^$/d' | awk '!seen[$0]++' | head -12
