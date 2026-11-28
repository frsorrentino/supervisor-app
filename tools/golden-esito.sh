#!/usr/bin/env bash
# L'esito della verifica dei golden (app-ci-golden-avviso-28-11, Franz 28/11 17:04: ogni modifica visiva dava una run rossa e una mail
# «Run failed»). Si chiama dopo `verifyPaparazzi…` fallito, con le cartelle dei risultati JUnit (default: quelle di wear e mobile).
# Se tutti i test falliti sono differenze d'immagine di Paparazzi, cioè «Images differ (by …%)» o «… differ too much for …» (in
# Paparazzi 2.0.0-alpha02 anche un golden che manca finisce lì), scrive un ::warning:: col numero ed esce con 0. Se uno è fallito per
# altro, o non si trova nessun test fallito, scrive un ::error:: ed esce con 1: compilazione e test veri restano bloccanti.
set -euo pipefail
PYTHONIOENCODING=utf-8 "${PYTHON:-python3}" - "$@" <<'PY'
import glob, re, sys
import xml.etree.ElementTree as ET
roots = sys.argv[1:] or ['wear/build/test-results', 'mobile/build/test-results']
files = sorted(f for r in roots for f in glob.glob(r + '/**/TEST-*.xml', recursive=True))
image = re.compile(r'Images differ \(by |differ too much for ')
changed, other = [], []
for f in files:
    for tc in ET.parse(f).getroot().iter('testcase'):
        bad = tc.find('failure')
        if bad is None:
            bad = tc.find('error')
        if bad is None:
            continue
        name = tc.get('classname', '').rsplit('.', 1)[-1] + '.' + tc.get('name', '')
        (changed if image.search((bad.get('message') or '') + '\n' + (bad.text or '')) else other).append(name)
if other:
    print('::error title=Golden::%d test falliti non per le immagini: %s' % (len(other), ', '.join(other[:12])))
    sys.exit(1)
if not changed:
    print('::error title=Golden::la verifica è fallita senza test falliti nei risultati (%d file letti): errore di Gradle' % len(files))
    sys.exit(1)
more = ' e altre %d' % (len(changed) - 6) if len(changed) > 6 else ''
print('::warning title=Golden cambiati::%d immagini diverse dai golden (%s%s); la run resta verde. Le nuove sono nell\'artifact '
      'paparazzi-snapshots e sul ramo goldens/<sha>, le differenze nell\'artifact golden-differenze.' % (len(changed), ', '.join(changed[:6]), more))
PY
