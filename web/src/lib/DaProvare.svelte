<script lang="ts">
  import { untrack } from 'svelte'
  import type { CmdResult, DecisionsPage, ProvePage } from './contract'
  import { decodeDecisions, decodeProve, readPage } from './decideDecode'
  import { allOk, cardKeys, proveDone, proveSent, screen, testCards, type TestCard } from './decide'
  import { newCmd } from './transport'
  import { t } from './t'
  import LongText from './LongText.svelte'
  import Check from './Check.svelte'
  import LoadFailed from './LoadFailed.svelte'
  import { slide } from 'svelte/transition'
  import { confirmMs, leave } from './leave'
  import { reducedMotion } from './motion'
  import ItemList from './ItemList.svelte'
  import { prove, proveKo, proveRed } from './itemRows'
  // «Da provare» (contratto 1.99, banco_prova), porta di DaProvareScreen. Sul web vale l'ultima versione online: nessuna prova aspetta un'installazione.
  let { run, onScreen, onShot }: { run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>; onScreen: (s: string) => void; onShot: (f: File, note: string) => void } = $props()
  let page = $state<ProvePage | null>(null)
  let ko = $state<ProvePage['groups'][number]['prove'][number] | null>(null)
  let text = $state(''), shot = $state<File | null>(null)
  // Senza risposta in tempo la pagina lo dice e offre Riprova, invece di restare «In arrivo dal PC» (app-pwa-decisioni-03-11).
  let failed = $state(false)
  async function load() { failed = false; ({ page, failed } = readPage(await run(newCmd('prove', null)), decodeProve, page)) }
  // Una lettura all'apertura: dentro l'effetto `load` leggeva la pagina e ripartiva a ogni risposta, una richiesta al secondo al
  // relay (04/11 18:19, i comandi del telefono in fila 8-15 s).
  $effect(() => { untrack(() => void load()) })
  // Contratto 1.107 (app-sezioni-accorpate-03-11, scelta A): sopra il banco, le prove che aspettano Franz fuse per progetto nella
  // pre-release più nuova. «Tutto va» manda `provato` per ogni gruppo, «Una non va» chiede quale e perché (`non_va`).
  let dec = $state<DecisionsPage | null>(null)
  async function loadDec() { ({ page: dec } = readPage(await run(newCmd('decisioni', null)), decodeDecisions, dec)) }
  $effect(() => { untrack(() => void loadDec()) })
  const cards = $derived(dec ? testCards(dec) : [])
  let doneKeys = $state<Record<string, true>>({}), sentKeys = $state<Record<string, true>>({}), more = $state<Record<string, true>>({})
  let koCard = $state<TestCard | null>(null), koWhich = $state<string | null>(null), koWhy = $state('')
  const decide = (key: string, action: 'provato' | 'non_va', reason?: string) => run(newCmd('decisione', null, key, undefined, undefined, reason ? { action, reason } : { action })).then(r => !!r?.ok)
  async function allGood(c: TestCard) {
    const ks = cardKeys(c); for (const k of ks) sentKeys[k] = true
    const [oks] = await Promise.all([Promise.all(ks.map(k => decide(k, 'provato'))), new Promise(r => setTimeout(r, confirmMs('ok')))])
    const ok = oks.every(Boolean)
    for (const k of ks) { if (ok) doneKeys[k] = true; else delete sentKeys[k] }
  }
  async function sendKoCard() { const k = koWhich!, why = koWhy.trim(); koCard = null; koWhy = ''; sentKeys[k] = true; if (await decide(k, 'non_va', why)) doneKeys[k] = true; else delete sentKeys[k] }
  const rowsOf = (c: TestCard) => c.rows.filter(r => !doneKeys[r.key])
  // Franz 02/11 15:38: Ok non blocca più e la scheda non sparisce di colpo (leave.ts): conferma sul tasto, uscita in altezza,
  // le altre salgono; la pagina si ricarica dopo, in silenzio. Se il PC dice no, la scheda torna.
  let picked = $state<Record<string, string>>({})
  // Le prove mandate restano via anche se la sezione si ridisegna (app-prova-ok-sparisce-04-11): partono da `proveSent`.
  let gone = $state<Record<string, true>>(Object.fromEntries([...proveSent].map(i => [i, true as const])))
  const hooks = {
    pick: (ids: string[], v: string | null) => { const n = { ...picked }; for (const i of ids) { if (v) n[i] = v; else delete n[i] } picked = n },
    hide: (ids: string[], v: boolean) => { const n = { ...gone }; for (const i of ids) { if (v) { n[i] = true; proveSent.add(i) } else { delete n[i]; proveSent.delete(i) } } gone = n },
  }
  // L'Ok torna indietro solo per un rifiuto vero (`proveDone`); il Non va resta com'era.
  const send = (arg: string, res: 'ok' | 'ko', body?: string) => run(newCmd('prova', null, arg, body, undefined, { result: res })).then(r => (res === 'ok' ? proveDone(r?.ok ?? null, r?.text) : !!r?.ok))
  // La lista si rilegge dopo ogni esito, anche un rifiuto.
  async function result(ids: string[], arg: string, res: 'ok' | 'ko', body?: string) { await leave(ids, res, () => send(arg, res, body), hooks); void load() }
  const shown = (g: ProvePage['groups'][number]) => g.prove.filter(p => !gone[String(p.id)])
  const outMs = () => (reducedMotion() ? 0 : 160)
  async function sendKo() { const p = ko!; const b = text.trim(), f = shot; ko = null; text = ''; shot = null; await result([String(p.id)], String(p.id), 'ko', b); if (f) onShot(f, t.proveKoShotNote(p.short ?? p.task)) }
</script>

<div class="dp" data-testid="da-provare">
  {#each cards.filter(c => rowsOf(c).length) as c (c.project)}
    {@const rows = rowsOf(c)}{@const busy = cardKeys(c).some(k => sentKeys[k])}
    <div class="c fuse" data-testid="prova-fusa" out:slide={{ duration: outMs() }}>
      <b>{t.proveCardTitle(c.release ?? (c.web ? `web ${c.web}` : c.project.split('/').pop() ?? ''))}</b>
      <p class="m">{t.proveCardContains(rows.length)}</p>
      {#each more[c.project] ? rows : rows.slice(0, 3) as r (r.key)}<div class="row"><LongText text={r.text} /></div>{/each}
      {#if !more[c.project] && rows.length > 3}<button class="text" onclick={() => (more[c.project] = true)}>{t.proveCardMore(rows.length - 3)}</button>{/if}
      <div class="ac"><button class="out" disabled={busy} onclick={() => { koCard = c; koWhich = rows.length === 1 ? rows[0].key : null }}>{t.proveCardKo}</button><span class="sp"></span><button class="tonal ck" class:done={busy} disabled={busy} onclick={() => allGood(c)}><span class="lbl">{t.proveCardAll}</span>{#if busy}<Check />{/if}</button></div>
    </div>
  {/each}
  {#if koCard}
    <div class="c ko">
      <b>{t.proveCardKoWhich}</b>
      {#each rowsOf(koCard) as r (r.key)}<label class="pick"><input type="radio" name="kowhich" checked={koWhich === r.key} onchange={() => (koWhich = r.key)} /> {r.text}</label>{/each}
      <textarea bind:value={koWhy} maxlength="300" rows="3" placeholder={t.proveKoHint}></textarea>
      <div class="ac"><span class="sp"></span><button class="text" onclick={() => (koCard = null)}>{t.cancel}</button><button class="tonal" disabled={!koWhich || !koWhy.trim()} onclick={sendKoCard}>{t.proveKoSend}</button></div>
    </div>
  {/if}
  {#if !page}{#if failed}<LoadFailed onRetry={() => void load()} />{:else}<p class="m">{t.proveLoading}</p>{/if}
  {:else if !page.groups.length && !page.ko.length && !page.red.length}<p class="m">{t.proveNone}</p>{/if}
  {#each (page?.groups ?? []).filter(g => shown(g).length) as g (g.build)}
    <div class="h" out:slide={{ duration: outMs() }}><span>{g.build}</span>{#if shown(g).length > 1}<button class="tonal ck" class:done={g.prove.every(p => picked[String(p.id)] === 'ok')} onclick={() => result(g.prove.map(p => String(p.id)), allOk(g), 'ok')}><span class="lbl">{t.proveAllOk}</span>{#if g.prove.every(p => picked[String(p.id)] === 'ok')}<Check />{/if}</button>{/if}</div>
    <!-- Le prove come righe (app-richiede-te-righe-02-11): il tocco apre la scheda con Non va e Ok. Sul web la versione c'è sempre. -->
    <ItemList rows={shown(g).map(p => prove(p, true))} testid={`prova-${g.build}`}>{#snippet card(r)}{@const p = g.prove.find(x => String(x.id) === r.key)}{#if p}{@const pk = picked[String(p.id)]}
      <div class="in"><LongText text={p.fai} /><p class="m">{t.proveExpected(p.atteso)}</p>
        <div class="ac">
          {#if screen(p.dove)}<button class="text" onclick={() => onScreen(screen(p.dove)!)}>{t.featuresGo}</button>{/if}
          <span class="sp"></span>
          <button class="out" class:fade={pk === 'ok'} disabled={!!pk} onclick={() => (ko = p)}>{t.proveKo}</button><button class="tonal ck" class:done={pk === 'ok'} class:fade={pk === 'ko'} disabled={!!pk} onclick={() => result([String(p.id)], String(p.id), 'ok')}><span class="lbl">{t.proveOk}</span>{#if pk === 'ok'}<Check />{/if}</button>
        </div>
      </div>{/if}{/snippet}</ItemList>
  {/each}
  {#if page?.ko.length}<div class="h"><span>{t.proveReported}</span></div><ItemList rows={page.ko.map(proveKo)} testid="prova-ko">{#snippet card(r)}<div class="m in"><LongText text={page?.ko.find(k => `ko-${k.id}` === r.key)?.text ?? ''} /></div>{/snippet}</ItemList>{/if}
  {#if page?.red.length}<div class="h"><span>{t.proveRed}</span></div><ItemList rows={page.red.map(proveRed)} testid="prova-rossa">{#snippet card(r)}<div class="r in"><LongText text={page?.red.find(x => `red-${x.task}` === r.key)?.reason ?? t.proveRedRunning} /></div>{/snippet}</ItemList>{/if}
  {#if page?.groups.length}<p class="m small">{t.proveWeb}</p>{/if}
  {#if ko}
    <div class="c ko">
      <b>{ko.short ?? ko.task}</b>
      <textarea bind:value={text} maxlength="2000" rows="3" placeholder={t.proveKoHint}></textarea>
      <label class="text">{shot ? shot.name : t.proveKoShot}<input type="file" accept="image/*" hidden onchange={(e) => { shot = (e.currentTarget as HTMLInputElement).files?.[0] ?? null }} /></label>
      <div class="ac"><span class="sp"></span><button class="text" onclick={() => (ko = null)}>{t.cancel}</button><button class="tonal" disabled={!text.trim()} onclick={sendKo}>{t.proveKoSend}</button></div>
    </div>
  {/if}
</div>

<style>
  .in { display: flex; flex-direction: column; gap: 6px; }
  .dp { padding: 0 12px 24px; max-width: 720px; display: flex; flex-direction: column; gap: 10px; }
  .h { display: flex; align-items: center; justify-content: space-between; padding: 8px 4px 0; font: 12.5px var(--mono); color: var(--text2); }
  .c { background: var(--low); border-radius: 20px; padding: 14px; display: flex; flex-direction: column; gap: 6px; }
  .c b { font-weight: 600; }
  .fuse .row { margin: 0 0 0 4px; } .fuse b { font-size: 17px; } .pick { display: flex; gap: 8px; align-items: center; padding: 4px 0; }
  p { font-size: 14px; }
  .m { color: var(--text2); } .r { color: var(--gone); } .small { font-size: 12.5px; }
  .ac { display: flex; align-items: center; gap: 8px; } .sp { flex: 1; }
  .tonal { background: var(--high); color: var(--text); border-radius: 999px; padding: 8px 14px; font-weight: 500; font-size: 14px; white-space: nowrap; }
  .out { border: 1px solid var(--line); color: var(--text); border-radius: 999px; padding: 8px 14px; font-size: 14px; white-space: nowrap; }
  /* La conferma (voce 11): il tasto scelto si accende e cresce un poco, gli altri sfumano; la scheda si schiarisce. */
  .tonal, .out { transition: background-color 150ms, color 150ms, opacity 150ms, transform var(--spring-bounce-t) var(--spring-bounce); }
  .fade { opacity: .35; }
  button:disabled { cursor: default; }
  .c { transition: background-color 150ms; }
  .c.picked { background: var(--high); }
  .out:active { transform: scale(.97); transition-duration: 100ms; }
  .text { color: var(--icon); font-size: 14px; font-weight: 500; padding: 8px 4px; cursor: pointer; }
  textarea { width: 100%; box-sizing: border-box; background: var(--bg); color: var(--text); border: 1px solid var(--line); border-radius: 12px; padding: 8px; font: inherit; }
</style>
