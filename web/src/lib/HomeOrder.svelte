<script lang="ts">
  import { BOARD_MAX, DEFAULT_CATS, decode, encode, fromTop, moveCat, moveSection, toTop, toggle, top as topOf, type Cat, type CatId, type HomeKey } from './homeCats'
  import { move, pinned, type HomeSection } from './homeOrder'
  import { t } from './t'
  import CatIcon from './CatIcon.svelte'
  // «Riordina la home» per categorie (Franz 02/11 16:44, app-home-categorie-02-11; HomeOrderScreen.kt nell'app; mockup
  // home-categorie-telefono.png, riquadro 4): si trascinano le categorie e, dentro ognuna, le sue sezioni, dalla maniglia; con la
  // tastiera la maniglia sposta con le frecce. L'interruttore decide se in home la categoria è in vista o chiusa. Una sezione resta
  // sempre nella sua categoria. «Salva» è l'unico tasto pieno.
  let { saved, onSave }: { saved?: string[]; onSave: (arg: string | null) => void } = $props()
  let cats = $state<Cat[]>(decode(saved))
  // Lo spazio in cima (app-home-in-cima-02-11): la prima card, con «Togli dalla cima»; ogni sezione ha «Metti in cima».
  let top = $state<HomeKey[]>(topOf(saved))
  const putTop = (k: HomeKey) => { const r = toTop(cats, top, k); top = r.top; cats = r.cats }
  const dropTop = (k: HomeKey) => { const r = fromTop(cats, top, k); top = r.top; cats = r.cats }
  const SEC: Record<string, { icon: string; name: string }> = {
    seguiti: { icon: 'event_list', name: t.homeSecFollow }, prove: { icon: 'checklist', name: t.homeSecVerify }, decisioni: { icon: 'timer', name: t.homeSecDecide },
    sessions: { icon: 'play_circle', name: t.homeSessions }, suggestions: { icon: 'auto_awesome', name: t.sugTitle },
    notte: { icon: 'bedtime', name: t.homeNight }, lavori: { icon: 'checklist', name: t.lavoriProgress }, esiti: { icon: 'sports_score', name: t.homeOutcomes }, usage: { icon: 'data_usage', name: t.homeUsage }, system: { icon: 'memory', name: t.homeSystem },
    // Le tre sezioni nuove (app-da-guardare-02-11, app-rendiconti-2-02-11): hanno già il loro posto; icone disegnate, non sono nel font.
    da_guardare: { icon: 'svg:eye', name: t.homeSecWatch }, monitors: { icon: 'svg:chart', name: t.monitorsTitle }, rendiconti: { icon: 'svg:doc', name: t.homeSecReports }, pagella: { icon: 'svg:medal', name: t.homeSecScorecard },
  }

  // Il trascinamento dalla maniglia: la voce segue il puntatore e scavalca la vicina quando ne passa la metà, come nella home.
  let held = $state<string | null>(null)
  let dy = $state(0)
  let startY = 0
  function down(e: PointerEvent, key: string) {
    if (e.button !== 0) return
    held = key; startY = e.clientY; dy = 0
    ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
  }
  function drag(e: PointerEvent, key: string, keys: string[], move: (from: number, to: number) => void) {
    if (held !== key) return
    dy = e.clientY - startY
    const i = keys.indexOf(key)
    const h = (k?: string) => (k ? document.querySelector(`[data-ro="${k}"]`)?.getBoundingClientRect().height ?? 0 : 0)
    const next = h(keys[i + 1]), prev = h(keys[i - 1])
    if (next && dy > next / 2) { move(i, i + 1); startY += next; dy -= next }
    else if (prev && dy < -prev / 2) { move(i, i - 1); startY -= prev; dy += prev }
  }
  const up = () => { held = null; dy = 0 }
  function keys(e: KeyboardEvent, i: number, n: number, move: (from: number, to: number) => void) {
    if (e.key === 'ArrowUp' && i > 0) { e.preventDefault(); move(i, i - 1) }
    if (e.key === 'ArrowDown' && i < n - 1) { e.preventDefault(); move(i, i + 1) }
  }
  const shift = (k: string) => (held === k ? `translateY(${dy}px)` : undefined)
</script>

{#snippet handle(key: string, name: string, i: number, list: string[], move: (from: number, to: number) => void)}
  <button class="hd" aria-label={t.reorderMoveCat(name)} onpointerdown={(e) => down(e, key)} onpointermove={(e) => drag(e, key, list, move)} onpointerup={up} onpointercancel={up} onkeydown={(e) => keys(e, i, list.length, move)}>
    <CatIcon name="drag" />
  </button>
{/snippet}

<div class="v3 ro" data-testid="riordino-sezioni">
  <div class="card" data-testid="riordino-cima">
    <div class="head"><b class="toph">{t.reorderTop}</b></div>
    {#if !top.length}<p class="empty">{t.reorderTopEmpty}</p>{/if}
    {#each top as k, ti (k)}
      <div class="row" class:held={held === `top-${k}`} data-ro="top-{k}" style:transform={shift(`top-${k}`)}>
        {@render handle(`top-${k}`, SEC[k].name, ti, top.map(x => `top-${x}`), (from, to) => (top = move(top, from, to)))}
        {#if SEC[k].icon.startsWith('svg:')}<span class="own"><CatIcon name={SEC[k].icon.slice(4)} /></span>{:else}<span class="ms own" aria-hidden="true">{SEC[k].icon}</span>{/if}
        <span class="tx">{SEC[k].name}</span>
        <button class="pin" aria-label={t.reorderTopOut(SEC[k].name)} data-testid="riordino-togli-{k}" onclick={() => dropTop(k)}><CatIcon name="tobottom" /></button>
      </div>
    {/each}
  </div>
  {#each cats as c, ci (c.id)}
    <div class="card" class:held={held === c.id} data-ro={c.id} style:transform={shift(c.id)} data-testid="riordino-cat-{c.id}">
      <div class="head">
        {@render handle(c.id, t.catName[c.id], ci, cats.map(x => x.id), (from, to) => (cats = moveCat(cats, from, to)))}
        <b>{t.catName[c.id]}</b>
        <span class="state">{c.open ? t.reorderInView : t.reorderCatClosed}</span>
        <button class="sw" role="switch" aria-checked={c.open} aria-label={t.catName[c.id]} data-testid="riordino-interruttore-{c.id}" onclick={() => (cats = toggle(cats, c.id as CatId))}><span></span></button>
      </div>
      {#each c.sections as k, si (k)}
        <div class="row" class:held={held === k} data-ro={k} style:transform={shift(k)}>
          {@render handle(k, SEC[k].name, si, c.sections, (from, to) => (cats = moveSection(cats, c.id, from, to)))}
          {#if SEC[k].icon.startsWith('svg:')}<span class="own"><CatIcon name={SEC[k].icon.slice(4)} /></span>{:else}<span class="ms own" aria-hidden="true">{SEC[k].icon}</span>{/if}
          <span class="tx">{SEC[k].name}</span>
          {#if top.length < BOARD_MAX}<button class="pin" aria-label={t.reorderTopIn(SEC[k].name)} data-testid="riordino-in-cima-{k}" onclick={() => putTop(k)}><CatIcon name="totop" /></button>{/if}
        </div>
      {/each}
    </div>
  {/each}
  <p class="sub">{t.reorderFoot}</p>
  <button class="save" onclick={() => onSave(encode(cats, null, top))}>{t.reorderSave}</button>
  <button class="reset" onclick={() => { cats = DEFAULT_CATS; top = [] }}>{t.reorderReset}</button>
</div>

<style>
  .ro { display: flex; flex-direction: column; gap: 10px; padding: 8px 12px 24px; max-width: 560px; min-height: 100%; }
  .card { background: var(--s1); border-radius: 14px; overflow: hidden; }
  .card.held, .row.held { position: relative; z-index: 2; box-shadow: 0 8px 24px rgb(0 0 0 / .45); }
  .row.held { background: var(--s2); }
  .head { display: flex; align-items: center; gap: 4px; min-height: 56px; padding: 0 12px 0 4px; }
  .head b { flex: 1; font: 700 18px/22px var(--display); color: var(--tx); }
  .state { font-size: 14px; color: var(--tx2); margin-right: 8px; }
  .row { display: flex; align-items: center; gap: 4px; min-height: 52px; margin: 0 12px; padding: 0 0 0 0; border-top: 1px solid var(--line); background: var(--s1); }
  .tx { flex: 1; font: 600 17px/20px var(--display); color: var(--tx); }
  .own { display: grid; place-items: center; width: 34px; color: var(--tx2); }
  .hd { flex: none; width: 40px; height: 40px; display: grid; place-items: center; color: var(--tx3); cursor: grab; touch-action: none; }
  .sw { flex: none; position: relative; width: 52px; height: 32px; border-radius: 16px; background: var(--s3); transition: background .2s; }
  .sw span { position: absolute; top: 6px; left: 6px; width: 20px; height: 20px; border-radius: 50%; background: var(--tx); transition: transform .2s; }
  .sw[aria-checked='true'] { background: var(--busy); }
  .sw[aria-checked='true'] span { transform: translateX(20px); background: #fff; }
  .toph { padding-left: 12px; }
  .empty { margin: 0 16px 14px; font-size: 14px; line-height: 19px; color: var(--tx2); }
  .pin { flex: none; width: 44px; height: 44px; display: grid; place-items: center; color: var(--tx2); }
  .sub { margin: 4px 4px 0; font-size: 14px; color: var(--tx2); }
  .save { margin: 8px 0 0; min-height: 48px; border-radius: 24px; background: var(--act); color: var(--on-act); font: 600 17px var(--display); }
  .reset { align-self: center; padding: 12px; font-size: 14px; color: var(--tx); }
  @media (prefers-reduced-motion: reduce) { .sw, .sw span { transition: none; } }
</style>
