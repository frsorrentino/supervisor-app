#!/usr/bin/env python3
"""Le immagini di «Cosa sa fare» (Franz 01/11 22:13, passo 3): per ogni voce senza riquadro Prova, un'immagine di prova della CI, ridotta.
Uso, dopo aver aggiornato le immagini di prova: python3 docs/panoramica/gen_immagini.py. Scrive in mobile/src/main/assets/feature-images e
web/public/feature-images, e aggiunge `image` a features.json. Le immagini sono dei test: solo dati finti."""
import json, os
from PIL import Image

M = 'mobile/src/test/snapshots/images/it.pixelbox.cmwatch.mobile_%s.png'
W = 'wear/src/test/snapshots/images/it.pixelbox.cmwatch.wear_%s.png'
MAP = {
    'domande': M % 'SessionSheetTest_sheetQuestion', 'ok_da_dare': M % 'DecideScreenTest_ok_informato_card', 'notifiche': M % 'SettingsScreenTest_settingsPaired',
    'avvisi_altre': M % 'SessionSheetTest_sheetElsewhereWaiting', 'moduli': M % 'SummaryListTest_summaryWithElicitations', 'sessions': M % 'SummaryListTest_summaryOpen',
    'al_lavoro': M % 'HomeBoxScreenTest_busy412', 'chat': M % 'SessionSheetTest_sheetChat', 'modifiche': M % 'DiffViewsTest_changesFileOpen', 'prompt': M % 'SessionSheetTest_sheetChatNews',
    'lancia': M % 'LaunchSheetTest_launchSheet', 'prossimi': M % 'SessionSheetTest_sheetNextSteps', 'manda_alle': M % 'SessionSheetTest_sheetQuickPhrases', 'allegati': M % 'SessionSheetTest_masterPlusMenu',
    'notte_coda': M % 'LaunchSheetTest_nightSheetWithRecapCards', 'recap': M % 'LavoriScreenTest_lavoriFull', 'decisioni': M % 'SessionSheetTest_masterPlusMenu', 'usage': M % 'UtilizzoV3Test_weekOpen',
    'contesto': M % 'SessionSheetTest_sheetHighTier', 'modello': M % 'TunePanelTest_adviceDiffersMidWork', 'plugins': M % 'PluginsSectionTest_plugins_state412', 'cerca': M % 'SearchScreenTest_searchResults',
    'closed': M % 'SummaryListTest_summaryClosed', 'file': M % 'SessionSheetTest_sheetFilePreview', 'riordino': M % 'HomeAltroTest_reorderScreen', 'voce': M % 'ReadingPillTest_rateAndVoice',
    'dispositivi': M % 'SettingsScreenTest_settingsDevicesB', 'lingua': M % 'SettingsScreenTest_settingsDemo', 'aggiornamenti': M % 'SummaryListTest_summaryWithUpdate', 'plancia': M % 'TabletShellTest_tabletDesk',
    'orologio': W % 'ScreensSnapshotTest_card', 'live': W % 'LiveScreenSnapshotTest_liveQuestion', 'widget': M % 'WidgetConfigScreenTest_widgetConfigBoard', 'terminale': M % 'TerminalScreenTest_terminalText',
    'segui': M % 'ShellScreensTest_sessionMenuOpen', 'obiettivo': M % 'SessionSheetTest_sheetBusyWithGoal', 'controlla': M % 'ShellScreensTest_sessionMenuOpen', 'ferma': M % 'SessionSheetTest_sheetBusyStop',
    'segnalazione': M % 'ShareScreenTest_shareImage', 'ricorrenti': M % 'SessionSheetTest_masterRecurring', 'chiudi': M % 'SessionSheetTest_closeSplashClosing', 'quota_finita': M % 'SessionSheetTest_sheetQuotaWarning',
    'segnalibro': M % 'SessionSheetTest_sheetIdleWithOutcome', 'banco_prova': M % 'DecideScreenTest_banco_prova_installata', 'prima_raccolta': M % 'PrimaRaccoltaScreenTest_prima_raccolta_consenso412', 'cinque_minuti': M % 'DecideScreenTest_cinque_minuti_una',
    'ok_informato': M % 'DecideScreenTest_ok_informato_card', 'heads_up': M % 'DecideScreenTest_heads_up_nota', 'panoramica': M % 'HomeBoxScreenTest_panoramica_italiano412',
}
OUT = ['mobile/src/main/assets/feature-images', 'web/public/feature-images']
for d in OUT: os.makedirs(d, exist_ok=True)
total = 0
for key, src in MAP.items():
    im = Image.open(src).convert('RGB')
    # Via la parte vuota in fondo, sotto l'ultima riga disegnata; poi al massimo 412 di larghezza e 900 di altezza.
    bg = im.getpixel((im.width // 2, im.height - 2))
    h = im.height
    px = im.load()
    while h > 200 and all(px[x, h - 1] == bg for x in range(0, im.width, 7)): h -= 1
    im = im.crop((0, 0, im.width, min(im.height, h + 12)))
    im.thumbnail((412, 900))
    for d in OUT:
        p = f'{d}/{key}.webp'
        im.save(p, 'WEBP', quality=72, method=6)
    total += os.path.getsize(p)
F = 'mobile/src/main/assets/features.json'
c = json.load(open(F, encoding='utf8'))
for f in c['features']:
    if f['key'] in MAP: f['image'] = f['key'] + '.webp'
    else: f.pop('image', None)
json.dump(c, open(F, 'w', encoding='utf8'), ensure_ascii=False, indent=1)
print(len(MAP), 'immagini', total // 1024, 'KB')
