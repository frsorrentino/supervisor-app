#!/usr/bin/env bash
# Installa un APK via ADB Wi-Fi dal Chromebook: l'orologio (default) o il telefono.
#
# L'adb giusto è quello x86_64 dell'SDK (adb.x86_64) sotto qemu: il server va lanciato a mano con «server nodaemon»
# perché l'auto-spawn fallisce senza binfmt. L'adb di Debian (/usr/bin/adb, a cui punta il link platform-tools/adb)
# connette e installa, ma NON sa accoppiare: col pairing brucia il codice (21/10/2026, primo codice di Franz perso).
#
# Uso:
#   ./scripts/install-watch.sh pair <IP:PORTA_PAIRING> <CODICE>        # solo la prima volta per dispositivo
#   ./scripts/install-watch.sh devices                                 # chi è connesso, con modello
#   ./scripts/install-watch.sh <IP:PORTA> [file.apk]                   # orologio (default: wear debug locale)
#   ./scripts/install-watch.sh phone <IP:PORTA> <file.apk|TAG>         # telefono: un file, o il tag di una release GitHub
#
# Le porte le dà «avahi-browse -rpt _adb-tls-connect._tcp» (mai scansioni: spengono il debug wireless del dispositivo).
# Lo stesso applicationId sta su telefono e orologio: prima di installare si legge il modello e si rifiuta quello sbagliato.
set -euo pipefail

Q=/usr/bin/qemu-x86_64-static
A="$HOME/android-sdk/platform-tools/adb.x86_64"
REPO=frsorrentino/supervisor-app
HERE="$(cd "$(dirname "$0")/.." && pwd)"
adbq() { "$Q" "$A" "$@"; }

[ -x "$Q" ] || { echo "manca $Q (apt install qemu-user-static)"; exit 1; }
[ -x "$A" ] || { echo "manca $A: l'adb x86_64 dell'SDK (platform-tools), non il link a /usr/bin/adb"; exit 1; }

# Il server: una volta sola, in sottofondo; se gira ancora quello di Debian lo si chiude prima.
if ! adbq devices >/dev/null 2>&1 || ! pgrep -f "adb.x86_64 server nodaemon" >/dev/null; then
    adbq kill-server >/dev/null 2>&1 || true
    /usr/bin/adb kill-server >/dev/null 2>&1 || true
    nohup "$Q" "$A" server nodaemon >/tmp/adb-server.log 2>&1 &
    sleep 2
fi

model_of() { adbq -s "$1" shell getprop ro.product.model 2>/dev/null | tr -d '\r'; }

case "${1:-}" in
    pair)
        [ $# -eq 3 ] || { echo "uso: $0 pair IP:PORTA_PAIRING CODICE"; exit 1; }
        adbq pair "$2" "$3"
        echo "Pairing ok. Ora: $0 IP:PORTA_CONNESSIONE [file.apk]   oppure   $0 phone IP:PORTA_CONNESSIONE file.apk|TAG"
        exit 0 ;;
    devices)
        adbq devices -l | tail -n +2 | while read -r serial rest; do
            [ -n "$serial" ] && echo "$serial  $(model_of "$serial")  $rest"
        done
        exit 0 ;;
    phone)
        [ $# -ge 3 ] || { echo "uso: $0 phone IP:PORTA file.apk|TAG"; exit 1; }
        TARGET="$2"; WHAT="$3"
        if [ -f "$WHAT" ]; then
            APK="$WHAT"
        else
            # Un tag di release GitHub: mobile-release.apk scaricato in una cartella vuota, poi verificato col digest.
            DIR="${TMPDIR:-/tmp}/cmwatch-apk-$WHAT"; rm -rf "$DIR"; mkdir -p "$DIR"
            gh release download "$WHAT" -R "$REPO" -p mobile-release.apk -D "$DIR"
            APK="$DIR/mobile-release.apk"
            WANT=$(gh api "repos/$REPO/releases/tags/$WHAT" --jq '.assets[] | select(.name=="mobile-release.apk") | .digest' | sed 's/^sha256://')
            HAVE=$(sha256sum "$APK" | cut -d' ' -f1)
            [ -n "$WANT" ] && [ "$WANT" = "$HAVE" ] || { echo "digest diverso da quello della release ($WANT): non installo"; exit 1; }
            echo "APK della release $WHAT scaricato e verificato: $APK"
        fi
        adbq connect "$TARGET" >/dev/null
        M=$(model_of "$TARGET")
        case "$M" in
            "Pixel 11 Pro XL"|"Pixel Tablet"|strongbad) ;;
            *) echo "$TARGET è «$M»: l'APK del telefono va solo su Pixel 11 Pro XL, Pixel Tablet o strongbad (stesso pacchetto dell'orologio)"; exit 1 ;;
        esac
        adbq -s "$TARGET" install -r "$APK"
        echo "Installato $APK su $TARGET ($M)"
        adbq -s "$TARGET" shell dumpsys package it.pixelfarm.supervisor | grep -m1 lastUpdateTime | tr -d '\r'
        exit 0 ;;
    "")
        echo "uso: $0 pair IP:PORTA CODICE | devices | IP:PORTA [file.apk] | phone IP:PORTA file.apk|TAG"; exit 1 ;;
esac

# Orologio.
TARGET="$1"
APK="${2:-$HERE/wear/build/outputs/apk/debug/wear-debug.apk}"
[ -f "$APK" ] || { echo "APK non trovato: $APK (prima: ./gradlew :wear:assembleDebug, o l'artifact della CI)"; exit 1; }
adbq connect "$TARGET" >/dev/null
M=$(model_of "$TARGET")
case "$M" in
    *Watch*|*wisteria*) ;;
    *) echo "$TARGET è «$M», non l'orologio: per il telefono usa «$0 phone IP:PORTA file.apk»"; exit 1 ;;
esac
adbq -s "$TARGET" install -r "$APK"
echo "Installato $APK su $TARGET ($M)"
