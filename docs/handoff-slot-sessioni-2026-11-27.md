# Sessions inside the board: how the slot and section order work (Explore, 27/11 ~22:00)

Folders: web web/src/lib/ · phone mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/ · rules core/src/main/kotlin/it/pixelbox/cmwatch/rules/

## 1. Slot
- Stored in relay `state.home_order` via op `home_order` (App.svelte:1719, MainActivity.kt:892): board entries `top:<key>`, plus `shut:`, `rev:8`.
  Old `pin:<key>` only read by `pinned()`, moved to the board by `top()` (homeCats.ts:141-145, HomeCats.kt:137-140).
- Up to BOARD_MAX = 4 (homeCats.ts:140, HomeCats.kt:136). `savedPin` = null (Home.svelte:221, SummaryList.kt:279); `slotKey(savedPin, weekAlarm)` /
  HomeOrder.slot (homeOrder.ts:53, HomeOrder.kt:65) = `usage` only with the quota alarm (Home.svelte:225-246, SummaryList.kt:283-288).
- Open/closed prefs: web localStorage `home-sections-3`, `home-sessioni-titolo` (Home.svelte:481-492); phone `home_sections_2`, `sessioni_titolo` (SummaryList.kt:333-344).
- Web: `<Kpi7 slot={slotBox}>` (Home.svelte:897) → `<div class="slot">` (Kpi7.svelte:141). `slotBox` (879-891): usage details, alarm row
  `slot-in-alto`, then every key before EDGE as `.hsec.onboard.inboard` → `secOf(k)` (875). Suggerimenti = `secSuggestions` (916-921).
- Phone: `Kpi7(joined=…, slot={ slotContent() })` (SummaryList.kt:804, KpiViews.kt:257) = usage details + alarm row (1141-1146). Pinned blocks are
  LazyColumn items `liveTop.forEach(::renderBoard)` (1206, 1166-1184) in `BoardCard(above, below)` (KpiViews.kt:298-305), pulled up 8 dp, drawn by `slotFor(k)` (1111-1140).
- Pin/unpin only by drag (mouse at once, touch long press; Home.svelte:304-312, SummaryList.kt:609-666): drop on the board `toTop` (Home.svelte:347,
  SummaryList.kt:658; full → «Tabellone pieno: 4 voci»); below the invisible edge (Home.svelte:1175-1176) `fromTop`. Riordina: «Metti in cima» / «Togli»
  (HomeOrder.svelte:58-69, 83; HomeOrderScreen.kt:123-124, 145, 174).

## 2. Sessions today
- Web below the board: header `sessHead()` (Home.svelte:956-968, `home-sessioni-testata`), line `sessionsLine` (700-703), `t.homeSessionsLine` (t.ts:204);
  `<div data-testid="home-sessioni">` (1181-1182) with `secSessions()` (930-955: filter pill, Ti aspettano, cards, «Chiudi le finite») and `secClosed()`.
- Phone: `renderSessions` (SummaryList.kt:886-928), header `CatLabel("sessions")` (903-905, tag `home-cat-sessions`), line at 895, list 909-926;
  called by `renderKey("sessions")` (1154) at 1212 after `top-edge`.
- Not a slot key: missing from PINNABLE (homeOrder.ts:44, HomeOrder.kt:50); `catOf('sessions')` null → `top()` drops `top:sessions`; boardTakes/inSlot/overSlot refuse it.
- Unused board-row code for Sessions since 08/11: web Home.svelte:937 (`up.includes('sessions')`, row `home-sez-sessioni`); phone SummaryList.kt:900
  (`"sessions" in liveTop` → BoardCard{SlotRow}) + bodyBelow, shutTop, openOnBoard (883, 425, 453).

## 3. Order
- decode skips sessions (no DEFAULT_CATS entry: homeCats.ts:11-24, 67-91; HomeCats.kt:12-26, 62-84). `flat(up, cats)` = board keys, EDGE, categories
  (homeCats.ts:148). «Fisso in alto» = no category: always drawn between the edge and the categories, no drag zone (Home.svelte:1177-1182, SummaryList.kt:1210-1212).

## 4. Tests
- Web: homeOrder.test.ts (51-94), homeCats.test.ts (91-107, 150-161, 230-247: `top:sessions` → []), tabelloneFiltro.test.ts, slotInfo.test.ts, demoAllineata.test.ts:85-93.
- Core: HomeOrderTest.kt (70-72, 90), HomeCatsTest.kt (19, 89, 111-118, 269), SlotInfoTest.kt, BoardFilterTest.kt.
- Phone goldens: HomeSezioniTest (aperta, chiusa, senzaSessioniVive), HomeCategoriesTest:67, :81 (`top:sessions`), SummaryListTest (109-121, inCima 295-312),
  HomeStartsClosedTest, SlotInfoScreenTest, CasellaSettimanaTest.

## 5. Plan
1. One rule both sides: `HomeCats.board(top, hasSessions)` / `board()` in homeCats.ts → `['sessions', ...top]`; sessions stays out of `top:` (relay unchanged);
   BOARD_MAX still = Franz's 4 pins (confirm with the master). Tests in HomeCatsTest.kt and homeCats.test.ts.
2. Web slotBox: after the usage details (885), before the alarm row and pinned rows, `<div class="hsec onboard inboard" data-testid="home-sessioni">` with
   secSessions() + secClosed() (`onboard` also folds the list while a pinned block is dragged, 1952).
3. Web cleanup: 937 `up.includes('sessions')` → `!part`; delete 1181-1182, sessHead, `.catlabel.sess` (1933); secDown returns early for sessions (306);
   openCat (452) still finds the moved testid.
4. Phone frame: before the kpi-7 item `val sessionsInBoard = parts == null && (model.rows.isNotEmpty() || justClosed != null)`; `joined = sessionsInBoard ||
   up.any { k -> k != pin }` (804); boardKeys (882) start with "sessions".
5. Phone renderSessions: at 900 use `parts == null`, `SlotRow(Modifier, …)` (no drag zone); remove the CatLabel branch (903-905); move renderKey("sessions")
   (1212) before 1206.
6. «Chiuse» on the phone: renderClosed (1154) is a plain V3Section → between two BoardCards it would glue; make it a board row (BoardCard { slotFor("closed") })
   or show it only under the open list; same on web.
7. Phone drag: shutTop(upNow) (629) → shutTop(listOf("sessions") + upNow).
8. Riordina (optional): fixed «Sessioni» first row in «In cima», no handle (HomeOrder.svelte:58, HomeOrderScreen.kt:134).
9. Comments (HomeCats.kt:20-23, homeCats.ts:18-21, Home.svelte:1177-1180, SummaryList.kt:1210), goldens, visual checks at 360 and 1280 (open cards inside `.slot`,
   drop area `inSlot` 271 covers the whole `.boardwrap`).
