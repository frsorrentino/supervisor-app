# Notification center motion: sources (AOSP SystemUI + androidx Material 3)

Fetched 2026-11-26 directly from android.googlesource.com (`?format=TEXT`, base64) and raw.githubusercontent.com. All links are pinned to the commit that was the branch head at fetch time, with `#line` anchors:

- `frameworks/base` `main` @ `1cdfff555f4a21f71ccc978290e2e212e2f8b168`, abbreviated below as **FB**
  = `https://android.googlesource.com/platform/frameworks/base/+/1cdfff555f4a21f71ccc978290e2e212e2f8b168`
- **SUI** = FB + `/packages/SystemUI/src/com/android/systemui`
- **NOTIF** = SUI + `/statusbar/notification`
- **RES** = FB + `/packages/SystemUI/res/values`
- `frameworks/libs/systemui` `main` @ `89da99e2435efec403883849efac27a137ae56f3`, abbreviated as **INTERP**
  = `https://android.googlesource.com/platform/frameworks/libs/systemui/+/89da99e2435efec403883849efac27a137ae56f3/animationlib/src/com/android/app/animation/Interpolators.java`
- `frameworks/support` `androidx-main` @ `81738d05b7414cf58bee8a4232d5b852ac23210b`, abbreviated as **M3**
  = `https://android.googlesource.com/platform/frameworks/support/+/81738d05b7414cf58bee8a4232d5b852ac23210b/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3`

Each value below is quoted from the file, with the identifier and a link. Nothing was filled in from memory. The single item that could not be found is marked **NOT FOUND**.

---

## 0. Interpolators used throughout (bezier values)

Source: INTERP. `FAST_OUT_SLOW_IN` and the others are aliases of the LEGACY curves.

| Identifier | Definition | Link |
|---|---|---|
| `LEGACY` = `FAST_OUT_SLOW_IN` | `PathInterpolator(0.4f, 0f, 0.2f, 1f)` | INTERP#126, #152 |
| `LEGACY_ACCELERATE` = `FAST_OUT_LINEAR_IN` | `PathInterpolator(0.4f, 0f, 1f, 1f)` | INTERP#132, #153 |
| `LEGACY_DECELERATE` = `LINEAR_OUT_SLOW_IN` | `PathInterpolator(0f, 0f, 0.2f, 1f)` | INTERP#138, #154 |
| `FAST_OUT_SLOW_IN_REVERSE` | `PathInterpolator(0.8f, 0f, 0.6f, 1f)` | INTERP#160 |
| `ALPHA_IN` | `PathInterpolator(0.4f, 0f, 1f, 1f)` | INTERP#168 |
| `ALPHA_OUT` | `PathInterpolator(0f, 0f, 0.8f, 1f)` | INTERP#169 |
| `EMPHASIZED` | path `cubicTo(0.05,0, 0.133333,0.06, 0.166666,0.4)` then `cubicTo(0.208333,0.82, 0.25,1, 1,1)` ("same as fast_out_extra_slow_in") | INTERP#46, #320-327 |
| `EMPHASIZED_ACCELERATE` | `PathInterpolator(0.3f, 0f, 0.8f, 0.15f)` | INTERP#60 |
| `EMPHASIZED_DECELERATE` | `PathInterpolator(0.05f, 0.7f, 0.1f, 1f)` | INTERP#67 |
| `STANDARD` | `PathInterpolator(0.2f, 0f, 0f, 1f)` | INTERP#100 |
| `STANDARD_ACCELERATE` | `PathInterpolator(0.3f, 0f, 1f, 1f)` | INTERP#107 |
| `STANDARD_DECELERATE` | `PathInterpolator(0f, 0f, 0f, 1f)` | INTERP#114 |

Default interpolator: when no custom one is set, every stack property animation (Y, Z, alpha, height, clip) uses `Interpolators.FAST_OUT_SLOW_IN`. Sources: ViewState.java#470 (alpha), #554 (Z) and #619 (Y), at NOTIF/stack/ViewState.java; ExpandableViewState.java#264 (height) and #355 (clip), at NOTIF/stack/ExpandableViewState.java; PropertyAnimator.java#110, at NOTIF/PropertyAnimator.java.

Interruption: an animation that interrupts a running one takes **the longer of the new duration and the time the old one has left**. The code is `newDuration = Math.max(previousAnimator.getDuration() - previousAnimator.getCurrentPlayTime(), newDuration)` in `ViewState.cancelAnimatorAndGetNewDuration`, at NOTIF/stack/ViewState.java#747-758.

---

## A1. Swipe to dismiss

### SwipeHelper: SUI/SwipeHelper.java

| Value | Identifier | Line |
|---|---|---|
| **500 dp/s** fling threshold, multiplied by density | `SWIPE_ESCAPE_VELOCITY = 500f` | #67, used at #741-747 |
| **200 ms** dismiss duration when velocity == 0 | `DEFAULT_ESCAPE_ANIMATION_DURATION = 200` | #68, #424 |
| **400 ms** dismiss duration cap | `MAX_ESCAPE_ANIMATION_DURATION = 400` | #69, #417 |
| **4000 dp/s** velocity cap | `MAX_DISMISS_VELOCITY = 4000` | #70, #738 |
| **0.6** of the view width: alpha reaches 0 at this progress | `SWIPE_PROGRESS_FADE_END = 0.6f` | #72 |
| **0.6** of the width counts as "swiped far enough" to dismiss | `SWIPED_FAR_ENOUGH_SIZE_FRACTION = 0.6f` | #74, #753-756 |
| **0.3** of the size: maximum rubber-band travel for a row that cannot be dismissed | `MAX_SCROLL_SIZE_FRACTION = 0.3f` | #75, #687 |
| Snap-back **spring: stiffness 200, damping 0.75** | `new SpringConfig(SpringForce.STIFFNESS_LOW, SpringForce.DAMPING_RATIO_LOW_BOUNCY)` | #79-80 |
| Long press = system long-press timeout × **1.5** | `mLongPressTimeout = (long)(ViewConfiguration.getLongPressTimeout() * 1.5f)` | #148 |
| Falsing threshold **70 dp** | `R.dimen.swipe_helper_falsing_threshold` | #151; value in RES/dimens.xml#825 |

The spring constants are defined at FB/core/java/com/android/internal/dynamicanimation/animation/SpringForce.java: `STIFFNESS_LOW = 200f` (#47) and `DAMPING_RATIO_LOW_BOUNCY = 0.75f` (#68). The other constants in that file, for reference: `STIFFNESS_HIGH 10_000f` (#39), `STIFFNESS_MEDIUM 1500f` (#43), `STIFFNESS_VERY_LOW 50f` (#51), `DAMPING_RATIO_HIGH_BOUNCY 0.2f` (#57), `DAMPING_RATIO_MEDIUM_BOUNCY 0.5f` (#63), `DAMPING_RATIO_NO_BOUNCY 1f` (#74).

Snap back is `PhysicsAnimator.getInstance(row).spring(TRANSLATE_CONTENT, toPosition, startVelocity, mSnapBackSpringConfig)`, which keeps the finger's release velocity (SwipeHelper.java#545-555). PhysicsAnimator lives at FB/libs/WindowManager/Shell/shared/src/com/android/wm/shell/shared/animation/PhysicsAnimator.kt. I confirmed the path with HTTP 200 but did not read the file, because the spring values are passed in from SwipeHelper.

**Alpha while swiping** (SwipeHelper.java#218-236):
- `progress = min(max(0, |translation / viewSize|), 1)`
- `alpha = 1 - clamp(progress / SWIPE_PROGRESS_FADE_END)`. The row is fully transparent at 60% of its width.
- The alternative linear `max(1 - progress, 0)` applies only when `config_fadeDependingOnAmountSwiped` is true. It is **false** in RES/config.xml#402.

**Dismiss decision** (`isDismissGesture`, #759-764): the finger lifts, the gesture is not falsing, and either `swipedFastEnough()` (|v| > 500 dp/s in the same direction as the translation, #778-783) or `swipedFarEnough()` (|tx| > 0.6 × width) holds.

**Dismiss animation** (`dismissChild`, #398-447):
- Target = ±`getTotalTranslationLength`. In NSSL this is the row's `getMeasuredWidth()`, or `containerWidth - padding` when the row's own translationX is used (NotificationStackScrollLayout.java#6388-6396).
- Direction: left if the fling is left or tx < 0. With RTL and no velocity, it prefers left (#405-409).
- Duration: `min(400, |remaining| * 1000 / |v|)`, or 200 ms when v == 0 (#416-428).
- Curve:
  - Accelerated case (slow release, or Clear all): `Interpolators.FAST_OUT_LINEAR_IN` (0.4, 0, 1, 1) (#443-445).
  - Fast fling: `FlingAnimationUtils.applyDismissing(...)`, built with `maxLengthSeconds = 0.4` (#156-157).

**FlingAnimationUtils** (velocity-matched dismiss curve), at FB/libs/WindowManager/Shell/src/com/android/wm/shell/animation/FlingAnimationUtils.java:
- `LINEAR_OUT_FASTER_IN_X2 = 0.5f` (#39), `LINEAR_OUT_FASTER_IN_Y2_MIN = 0.4f` (#40), `LINEAR_OUT_FASTER_IN_Y2_MAX = 0.5f` (#41)
- `MIN_VELOCITY_DP_PER_SECOND = 250` (#42), `HIGH_VELOCITY_DP_PER_SECOND = 3000` (#43)
- `getDismissingProperties` (#298-327):
  - `maxLength = 0.4 s × sqrt(distance / maxDistance)`
  - `y2` goes linearly from 0.4 to 0.5 as velocity rises from 250 to 3000 dp/s (#334-339)
  - curve `PathInterpolator(0, 0, 0.5, y2)`; duration `(y2 / 0.5) × distance / v`
  - if that duration exceeds maxLength and v ≥ 250 dp/s: the duration is clamped and the curve blends velocity → linear-out-faster-in → `LINEAR_OUT_SLOW_IN`
  - otherwise: `FAST_OUT_LINEAR_IN` with maxLength

### NotificationSwipeHelper: NOTIF/stack/NotificationSwipeHelper.java

- `COVER_MENU_DELAY = 4000` ms (#56). With anti-falsing on, an exposed menu is covered (reset) again after 4 s (#466-469).
- `SWIPE_MENU_TIMING = 200` ms (#65). A non-dismissible ongoing row shows its menu when the gesture lasts ≥ 200 ms (#243-245).
- `getTotalTranslationLength` delegates to NSSL (#400-402).
- Menu/dismiss decision tree: `handleMenuRowSwipe` / `handleSwipeFromClosedState` / `handleSwipeFromOpenState`, at #212-290.

### Snooze/settings menu reveal: NOTIF/row/NotificationMenuRow.java

| Value | Identifier | Line |
|---|---|---|
| **0.25** of one menu-icon width before the menu shows (dismissible row) | `SWIPED_FAR_ENOUGH_MENU_FRACTION = 0.25f` | #61, #417-423 |
| **0.15** of one icon width for a non-dismissible row | `SWIPED_FAR_ENOUGH_MENU_UNCLEARABLE_FRACTION = 0.15f` | #62 |
| **0.2** of one icon width: swipe back past this and the row covers the menu again | `SWIPED_BACK_ENOUGH_TO_COVER_FRACTION = 0.2f` | #66, #426-428 |
| **200 ms** icon fade-in, curve `Interpolators.ALPHA_IN` (0.4, 0, 1, 1) | `ICON_ALPHA_ANIM_DURATION = 200` | #68, #631-632 |
| **60 ms** delay before the drag check | `SHOW_MENU_DELAY = 60` | #69 |
| Fade-in triggers when `|tx| ≥ 0.4 × menuWidth` and `|tx| < 0.4 × rowWidth` | literals `0.4f` / `0.4` in `CheckForDrag` | #577-588 |
| Swipe threshold used by the menu = rowWidth × `SWIPED_FAR_ENOUGH_SIZE_FRACTION` (0.6) | `getDismissThreshold()` | #749-751 |
| Menu icon size **64 dp**, padding **20 dp** | `notification_menu_icon_size`, `notification_menu_icon_padding` | RES/dimens.xml#360, #363 |

---

## A2. Expand/collapse of a row and of a group

### Durations: NOTIF/stack/StackStateAnimator.java#50-69

| Identifier | ms |
|---|---|
| `ANIMATION_DURATION_STANDARD` | **360** |
| `ANIMATION_DURATION_CORNER_RADIUS` | **200** |
| `ANIMATION_DURATION_WAKEUP` | 500 |
| `ANIMATION_DURATION_WAKEUP_SCRIM` | 667 |
| `ANIMATION_DURATION_GO_TO_FULL_SHADE` | **448** |
| `ANIMATION_DURATION_APPEAR_DISAPPEAR` | **464** |
| `ANIMATION_DURATION_SWIPE` | **200** |
| `ANIMATION_DURATION_DIMMED_ACTIVATED` | 220 |
| `ANIMATION_DURATION_CLOSE_REMOTE_INPUT` | 150 |
| `ANIMATION_DURATION_HEADS_UP_APPEAR` | **400** |
| `ANIMATION_DURATION_HEADS_UP_DISAPPEAR` | **400** |
| `ANIMATION_DURATION_HEADS_UP_CYCLING` | 400 |
| `ANIMATION_DURATION_PRIORITY_CHANGE` | 500 |
| `ANIMATION_DELAY_PER_ELEMENT_INTERRUPTING` | **80** |
| `ANIMATION_DELAY_PER_ELEMENT_MANUAL` | **32** |
| `ANIMATION_DELAY_PER_ELEMENT_GO_TO_FULL_SHADE` | **48** |
| `DELAY_EFFECT_MAX_INDEX_DIFFERENCE` | 2 (rows) |
| `MAX_STAGGER_COUNT` | 5 |

**`ANIMATION_DURATION_EXPAND_CLICKED`: NOT FOUND.** A grep over every downloaded SystemUI file (NSSL, StackStateAnimator, ExpandableNotificationRow, NotificationChildrenContainer, ViewState, ExpandableViewState, AnimationProperties, AnimationFilter, ActivatableNotificationView, ExpandableView, NotificationContentView, StackScrollAlgorithm, NotificationShelf) found no such identifier in `main`. Expansion is driven through the event table below.

**Event → duration** (`AnimationEvent.LENGTHS`, NOTIF/stack/NotificationStackScrollLayout.java#6666-6721). The longest duration among concurrent events wins (`combineLength`, StackStateAnimator.java#151):

| Event | Duration |
|---|---|
| ADD, REMOVE | APPEAR_DISAPPEAR 464 |
| REMOVE_SWIPED_OUT | STANDARD 360 |
| TOP_PADDING_CHANGED, CHANGE_POSITION, HIDE_SENSITIVE, **VIEW_RESIZE** (a row expanding/collapsing), **GROUP_EXPANSION_CHANGED**, HEADS_UP_OTHER, EVERYTHING | STANDARD 360 |
| ACTIVATED_CHILD, DIMMED | 220 |
| GO_TO_FULL_SHADE | 448 |
| HEADS_UP_APPEAR | 400 |
| HEADS_UP_DISAPPEAR(_CLICK) | 400 |
| HEADS_UP_CYCLING_IN/OUT | 400 |

The VIEW_RESIZE / GROUP_EXPANSION_CHANGED events are generated at NSSL#3482 and #3501. A priority change uses `ANIMATION_DURATION_PRIORITY_CHANGE` (NSSL#3558).

**Stagger of the other rows when one is added or removed** (`calculateChildAnimationDelay`, StackStateAnimator.java#241-296):
- ADD: `delay = (2 - clamp(|ownIndex - changingIndex| - 1, 0, 2)) × 80 ms`. Neighbours start up to 160 ms later; rows far away start immediately.
- REMOVE: `delay = clamp(|ownIndex - nextIndex| - 1, 0, 2) × 80 ms`.
- REMOVE_SWIPED_OUT: same formula with **32 ms** per element. The code falls through from the swiped-out case into the remove case (#264-266).

**Go to full shade** (StackStateAnimator.java#199-213, #298-311):
- Added rows start **200 dp** lower: `go_to_full_shade_appearing_translation`, RES/dimens.xml#913.
- Their duration is `464 + 50 + 100 × staggerCount^0.7` ms.
- Delay: `index^0.7 × 48 ms`, plus `staggerCount^0.7 × 48 × 0.25` for rows past the shelf.

### Row content swap (private ↔ public), ExpandableNotificationRow.java

`animateShowingPublic`, at NOTIF/row/ExpandableNotificationRow.java#3156-3191:
- overlap = duration / 10
- disappear = duration / 3 + overlap / 2
- appear = (duration - disappear) + overlap / 2, starting at delay + duration - appear
- both are alpha crossfades via `View.animate()`

### Group expand: NOTIF/stack/NotificationChildrenContainer.java

- Children ride the parent's AnimationProperties (`startAnimationToState`, #1067-1106), so a group expansion runs at STANDARD 360 ms with FAST_OUT_SLOW_IN.
- **How children translate** (`updateState`, #757-829):
  - Child Y is laid out cumulatively.
  - Between children the gap is `mChildPadding` (8 dp) when collapsed and `mDividerHeight` (0.5 dp) when expanded.
  - The first child gets an extra `mNotificationTopPadding + mDividerHeight` when expanded.
  - During a drag (`mUserLocked`) these gaps are interpolated with `expandFactor = getGroupExpandFraction()` (#1355-1362), which is `(actualHeight - collapsedHeight) / (expandedHeight - collapsedHeight)`, clamped to [0, 1].
- Child alpha: 1 for the visible ones (or `expandFactor` when the group shows as low priority). Children past the collapsed count fade in as `(actualHeight - childY) / childHeight` (#817-825).
- Z translation: 0 when collapsed. When the group is expanded and not animating, the children take the parent's Z (#800-814).
- **Dividers**: alpha `mDividerAlpha` when expanded, 0 when collapsed. During a drag the alpha is `interpolate(0, mDividerAlpha, min(childAlpha, expandFraction))`, and each divider sits at `childY - mDividerHeight` (#1083-1091).
- **Child alpha fade-in stagger** (`startChildAlphaAnimations`, #1240-1258): `ALPHA_FADE_IN` is **200 ms** (#81-88), alpha only, with **delay = i × 50 ms** for i < 5 (`NUMBER_OF_CHILDREN_WHEN_SYSTEM_EXPANDED`).
- **Header transformation**: only for low-priority groups during a drag. `mGroupHeaderWrapper.transformFrom(mMinimizedGroupHeaderWrapper, fraction)` is driven by the expand fraction, with no fixed duration (#1261-1271). Header alpha = `mHeaderVisibleAmount` (#872).

---

## A3. Grouping / stacking, heads-up, appear/disappear

### Children counts: NotificationChildrenContainer.java#77-80

- `NUMBER_OF_CHILDREN_WHEN_COLLAPSED = 2`
- `NUMBER_OF_CHILDREN_WHEN_SYSTEM_EXPANDED = 5`
- `NUMBER_OF_CHILDREN_WHEN_CHILDREN_EXPANDED = 8`

The selector is at #910-925. When there are more children than shown, an overflow "+N" (`mOverflowNumber`, from `HybridGroupManager.bindOverflowNumber`, #613) is placed and faded with `mGroupOverFlowState` (#831-855).

**No "pile" offset in AOSP.** A collapsed group in the phone shade does **not** draw a stacked deck. It shows the first 2 children as single-line hybrid views laid out vertically with `notification_children_padding` (8 dp) between them and `notification_children_collapsed_bottom_padding` (16 dp) below. The only "stack" metaphor is the shelf at the bottom of the shade (`notification_shelf_height` 48 dp, RES/dimens.xml#314). This is what the code shows; none of the files read contains any stacked-pile offset.

**Group dimens** (RES/dimens.xml):

| dimen | value | line |
|---|---|---|
| `notification_children_padding` | 8dp | #866 |
| `notification_children_container_divider_height` | 0.5dp | #777 |
| `notification_divider_alpha` | 1 (float) | #773 |
| `notification_children_container_margin_top` | 48dp | #783 |
| `notification_children_container_top_padding` | 8dp | #869 |
| `notification_children_collapsed_bottom_padding` | 16dp | #872 |

### Appear / disappear of a row: NOTIF/row/ActivatableNotificationView.java

- ADD and REMOVE both use **464 ms** (`ANIMATION_DURATION_APPEAR_DISAPPEAR`).
- `startAppearAnimation` (#377-415):
  - Appear curve: `FAST_OUT_SLOW_IN`. Disappear curve: `FAST_OUT_SLOW_IN_REVERSE` (0.8, 0, 0.6, 1). `LINEAR` when the `NotificationHeadsUpCycling` flag is on.
  - Duration is scaled by the remaining fraction: `duration × |fraction - target|`.
  - Translation `mAnimationTranslationY = translationDirection × actualHeight`. A normal add uses direction **-1**, i.e. the row comes from one full height above (#367-372). A heads-up add uses 0.
- The outline/clip rect grows from the top with the fraction; clipped from the BOTTOM by default (`updateAppearRect`, #527-555).
- Content alpha fades in only over **fraction 0.7 → 1.0** with `ALPHA_IN` (`ALPHA_APPEAR_START_FRACTION = .7f` #73, `ALPHA_APPEAR_END_FRACTION = 1` #78, #558-591).
- Remove direction is computed from where the next row is going, clamped to [-1, 1] (StackStateAnimator.java#401-430). Then `performRemoveAnimation(464, 0, direction, ...)` (#457).
- Background tint change: 360 ms `LINEAR` (#293-294).

### Heads-up: StackStateAnimator.java

- **Appear**: 400 ms. The Y translation starts at `-mStackTopMargin - heads_up_appear_y_above_screen`, or below the screen for bottom HUNs (#682-689). `heads_up_appear_y_above_screen` = **32 dp** (RES/dimens.xml#292). The curve for TRANSLATION_Y is `FAST_OUT_SLOW_IN` (#496-516).
- **Disappear**: 400 ms. `performRemoveAnimation(400, 0, 0.0f, true, ...)`, with TRANSLATION_Y curve `FAST_OUT_SLOW_IN_REVERSE` (#660-670).
- **Cycling**: 400 ms `LINEAR`, padding `heads_up_cycling_padding` **8 dp** (RES/dimens.xml#295).
- Max heads-up height 136 dp (`notification_max_heads_up_height`, RES/dimens.xml#282).

---

## A4. "Clear all": NotificationStackScrollLayout.clearNotifications

Source: NOTIF/stack/NotificationStackScrollLayout.java#5795-5837.

```java
// Decrease the delay for every row we animate to give the sense of
// accelerating the swipes
final int rowDelayDecrement = 5;
int currentDelay = 60;
int totalDelay = 0;
final int numItems = viewsToAnimateAway.size();
for (int i = numItems - 1; i >= 0; i--) {
    View view = viewsToAnimateAway.get(i);
    ...
    dismissViewAnimated(view, endRunnable, totalDelay, ANIMATION_DURATION_SWIPE);
    currentDelay = Math.max(30, currentDelay - rowDelayDecrement);
    totalDelay += currentDelay;
}
```

- Order: **from the last (bottom) row up to the first**. The first row's end callback dismisses everything in the backend.
- Delays: 0, 55, 105, 150, 190, 225, 255, 280, 300, … The step goes from 55 down by 5 per row to a floor of **30 ms**.
- Each row lasts **200 ms** (`ANIMATION_DURATION_SWIPE`).
- `dismissViewAnimated` (#2150-2165) calls `mSwipeHelper.dismissChild(child, 0 velocity, endRunnable, delay, useAccelerateInterpolator = true, duration, isClearAll = true)`. So the curve is **FAST_OUT_LINEAR_IN** (0.4, 0, 1, 1).
- Direction: **to the right** (+total translation length). In RTL it goes **left**, because velocity is 0 and `isDismissAll` (SwipeHelper.java#405-414).
- Section headers do not slide; they fade (`setContentVisible(false, true, ...)`, #2152-2155).
- During clear all, normal stack animations are off (`setClearAllInProgress(true)`, #5817) and the roundness manager is told (#4984).

---

## A5. Roundness while swiping

- Swipe: NOTIF/stack/NotificationRoundnessManager.java#70-104, `setViewsAffectedBySwipe`.
  - The row **before** the swiped one gets roundness top 0 / **bottom 1**.
  - The **swiped** row gets **top 1 / bottom 1**.
  - The row **after** gets **top 1** / bottom 0.
  - The previous targets are reset. Source type is `DISMISS_ANIMATION`.
- Animation: NOTIF/Roundable.kt#377-384, #402-404. `PropertyAnimator.setProperty(..., DURATION, animated)` with `DURATION = AnimationProperties().setDuration(StackStateAnimator.ANIMATION_DURATION_CORNER_RADIUS)` = **200 ms**. The curve falls back to the default `FAST_OUT_SLOW_IN` (PropertyAnimator.java#108-111).
- Radius: `radius = roundness × maxRadius`. When top + bottom exceed the clip height, they are scaled down proportionally (Roundable.kt#326-357).
- Dimens (RES/dimens.xml):
  - `notification_corner_radius` **28dp** (#320): corners without adjacent notifications, i.e. roundness 1
  - `notification_corner_radius_small` **4dp** (#329): corners with adjacent notifications
  - `notification_corner_animation_distance` 48dp (#326): distance over which corners animate near the shelf while scrolling
  - `notification_side_paddings` **16dp** (#288)
  - `notification_divider_height` **2dp** (#764)
  - `notification_section_divider_height` = side paddings, 16dp (#786)
  - `notification_min_interaction_height` 40dp (#317)

---

## B. androidx Compose Material 3 MotionScheme

- Scheme file: M3/MotionScheme.kt. `standard()` is at #123 and returns `StandardMotionSchemeImpl` (#136-196). `expressive()` is at #132 and returns `ExpressiveMotionSchemeImpl` (#200-260).
- Each spec is `spring<Any>(dampingRatio = <Tokens>.Spring<Name>Damping, stiffness = <Tokens>.Spring<Name>Stiffness)`.
- Token files are generated code, `// VERSION: 41.1.19`:
  - M3/tokens/ExpressiveMotionTokens.kt#21-34
  - M3/tokens/StandardMotionTokens.kt#21-34
- Key enum: M3/tokens/MotionSchemeKeyTokens.kt. Mapping key → spec at MotionScheme.kt#271-276.

| Spec | Expressive damping | Expressive stiffness | Standard damping | Standard stiffness |
|---|---|---|---|---|
| `defaultSpatialSpec` (`SpringDefaultSpatial*`) | **0.8** | **380** | **0.9** | **700** |
| `fastSpatialSpec` (`SpringFastSpatial*`) | **0.6** | **800** | **0.9** | **1400** |
| `slowSpatialSpec` (`SpringSlowSpatial*`) | **0.8** | **200** | **0.9** | **300** |
| `defaultEffectsSpec` (`SpringDefaultEffects*`) | **1.0** | **1600** | **1.0** | **1600** |
| `fastEffectsSpec` (`SpringFastEffects*`) | **1.0** | **3800** | **1.0** | **3800** |
| `slowEffectsSpec` (`SpringSlowEffects*`) | **1.0** | **800** | **1.0** | **800** |

The token names are exactly as they appear in the files, e.g. `ExpressiveMotionTokens.SpringFastSpatialDamping = 0.6f` and `StandardMotionTokens.SpringDefaultSpatialStiffness = 700.0f`. The effects specs are identical in the two schemes; only the spatial specs differ.

---

## C. Kokonut UI "Notification stack" (comparison only)

Source: https://github.com/kokonut-labs/kokonutui/blob/7e4bd770ca3f1ff4d93b9c1d0a61b11c3a51cbce/components/kokonutui/notification-stack.tsx (React/CSS, 720 lines).

Collapsed, it draws a real pile:
- `VISIBLE_BEHIND = 2` cards behind the front one, each peeking `PEEK = 10` px (#86-87)
- each card scaled `1 - depth × 0.05` from `origin-top` (#574)
- cards further back get opacity 0
- expanded, the cards are laid out with `GAP = 8` px (#88)
- layout changes animate transform/opacity and height over 300 ms `cubic-bezier(0.23, 1, 0.32, 1)` (#563, #587)

Swipe:
- dismiss at `DISMISS_DISTANCE = 100` px or `DISMISS_VELOCITY = 0.11` px/ms (#94-95)
- axis lock 6 px (#98)
- snap back with a critically damped spring, `SPRING_STIFFNESS = 520`, `SPRING_DAMPING = 2√520` (#101-102)
- dismiss flight at no less than `FLIGHT_MIN_SPEED = 1400` px/s with `FLIGHT_ACCEL = 4200` (#105-106)
- velocity sampled over the last 80 ms (#108)
- exit fade `EXIT_MS = 200` (#109)
- swipe opacity `max(1 - |x| / 240, 0.3)` (#123)

---

## Summary table: value → where → what it drives

| Value | Where | What it drives |
|---|---|---|
| 500 dp/s | SwipeHelper `SWIPE_ESCAPE_VELOCITY` #67 | fling speed that dismisses |
| 0.6 × width | SwipeHelper `SWIPED_FAR_ENOUGH_SIZE_FRACTION` #74 | drag distance that dismisses |
| 0.6 × width | SwipeHelper `SWIPE_PROGRESS_FADE_END` #72 | alpha reaches 0 while swiping |
| 0.3 × size | SwipeHelper `MAX_SCROLL_SIZE_FRACTION` #75 | rubber-band travel of a non-dismissible row |
| 200 / 400 ms | SwipeHelper `DEFAULT_/MAX_ESCAPE_ANIMATION_DURATION` #68-69 | dismiss fly-out (no velocity / cap) |
| 4000 dp/s | SwipeHelper `MAX_DISMISS_VELOCITY` #70 | velocity cap |
| spring 200 / 0.75 | SwipeHelper #79-80 + SpringForce #47, #68 | snap-back of a released row |
| (0, 0, 0.5, 0.4→0.5), 250-3000 dp/s | FlingAnimationUtils #39-43, #298-339 | velocity-matched dismiss curve |
| 0.4, 0, 1, 1 | Interpolators `FAST_OUT_LINEAR_IN` #132/#153 | slow dismiss and Clear all |
| 0.25 / 0.15 / 0.2 × icon | NotificationMenuRow #61-66 | menu reveal / cover thresholds |
| 200 ms ALPHA_IN | NotificationMenuRow #68, #631 | menu icon fade-in |
| 4000 ms | NotificationSwipeHelper `COVER_MENU_DELAY` #56 | auto-cover of an open menu |
| 360 ms | StackStateAnimator `ANIMATION_DURATION_STANDARD` #50 | expand/collapse, group expand, moves |
| 464 ms | `ANIMATION_DURATION_APPEAR_DISAPPEAR` #55 | add/remove of a row |
| 448 ms | `ANIMATION_DURATION_GO_TO_FULL_SHADE` #54 | lockscreen → full shade |
| 400 ms | `ANIMATION_DURATION_HEADS_UP_APPEAR/DISAPPEAR` #59-60 | heads-up in/out |
| 80 / 32 ms, max 2 rows | `ANIMATION_DELAY_PER_ELEMENT_*` #65-68 | neighbour stagger on add/remove/swipe |
| 0.4, 0, 0.2, 1 | `FAST_OUT_SLOW_IN` INTERP #126 | default for every stack property |
| 0.8, 0, 0.6, 1 | `FAST_OUT_SLOW_IN_REVERSE` INTERP #160 | row disappear, HUN disappear |
| fraction 0.7→1, ALPHA_IN | ActivatableNotificationView #73-78 | content fade during appear |
| -1 × height | ActivatableNotificationView #367-380 | appear translation of an added row |
| 32 dp | `heads_up_appear_y_above_screen` dimens #292 | HUN start above the screen |
| 2 / 5 / 8 | NotificationChildrenContainer #77-80 | children shown collapsed / system-expanded / expanded |
| 200 ms, +50 ms per child | NotificationChildrenContainer #81-88, #1255 | child alpha fade-in stagger |
| 8 dp → 0.5 dp | `notification_children_padding` / `..._divider_height` | gap between children, collapsed → expanded |
| 60 ms, −5 per row, floor 30 ms | NSSL `clearNotifications` #5819-5836 | Clear all stagger, bottom-up |
| 200 ms | `ANIMATION_DURATION_SWIPE` #56 | each row in Clear all |
| 200 ms | `ANIMATION_DURATION_CORNER_RADIUS` #51 / Roundable.kt #402 | corner roundness change on swipe |
| 28 dp / 4 dp | `notification_corner_radius(_small)` dimens #320/#329 | outer / adjacent corners |
| 16 dp | `notification_side_paddings` dimens #288 | side gutter, section gap |
| M3 expressive spatial 0.8/380, 0.6/800, 0.8/200 | ExpressiveMotionTokens #21-34 | default/fast/slow spatial springs |
| M3 standard spatial 0.9/700, 0.9/1400, 0.9/300 | StandardMotionTokens #21-34 | default/fast/slow spatial springs |
| M3 effects 1.0/1600, 1.0/3800, 1.0/800 | both token files | default/fast/slow effects springs (alpha, colour) |
