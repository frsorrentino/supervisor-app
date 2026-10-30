<script lang="ts">
  import { encode } from 'uqr'
  import type { CmdResult } from './contract'
  import { left, offerOf, refusalOf, type Offer, type Refusal } from './addDevice'
  import { t } from './t'

  // Aggiungere un dispositivo dalla web app (Franz, 13/10 19:05), come AddDeviceSheet del telefono: il QR da inquadrare
  // col dispositivo nuovo, il link #pair= da condividere o copiare, il codice e quanto resta; scaduto, un altro invito.
  let { ask, origin }: { ask: () => Promise<CmdResult | null>; origin: string } = $props()

  type Ui = { k: 'asking' } | { k: 'offer'; o: Offer } | { k: 'refused'; r: Refusal } | { k: 'lost' }
  let dlg: HTMLDialogElement | undefined = $state()
  let ui = $state<Ui>({ k: 'asking' })
  let now = $state(Math.floor(Date.now() / 1000))
  let copied = $state(false)
  let timer: ReturnType<typeof setInterval> | undefined

  export function open() { dlg?.showModal(); request() }

  async function request() {
    ui = { k: 'asking' }
    copied = false
    const r = await ask().catch(() => null)
    const o = r?.ok ? offerOf(r.text, origin) : null
    ui = !r ? { k: 'lost' } : o ? { k: 'offer', o } : { k: 'refused', r: refusalOf(r.text) }
    clearInterval(timer)
    if (o) {
      now = Math.floor(Date.now() / 1000)
      timer = setInterval(() => { now = Math.floor(Date.now() / 1000); if (now >= o.exp) clearInterval(timer) }, 1000)
    }
  }
  function close() { clearInterval(timer); dlg?.close() }

  const canShare = typeof navigator !== 'undefined' && !!navigator.share
  async function share(link: string) {
    try { await navigator.share({ title: t.addShareTitle, url: link }) } catch { /* annullata */ }
  }
  async function copy(link: string) {
    try { await navigator.clipboard.writeText(link); copied = true } catch { copied = false }
  }

  // Il QR nero su bianco con il margine di quattro moduli che le fotocamere vogliono; correzione L come il relay.
  const qr = $derived(ui.k === 'offer' && ui.o.qr ? encode(ui.o.qr, { ecc: 'L', border: 4 }) : null)
  const path = $derived(qr ? qr.data.flatMap((row, y) => row.map((on, x) => (on ? `M${x} ${y}h1v1h-1z` : ''))).join('') : '')
  const expired = $derived(ui.k === 'offer' && now >= ui.o.exp)
</script>

<dialog bind:this={dlg} onclose={() => clearInterval(timer)} onclick={(e) => e.target === e.currentTarget && close()}>
  <h3>{t.addDevice}</h3>
  {#if ui.k === 'asking'}
    <div class="spin" aria-hidden="true"></div>
    <p>{t.addAsking}</p>
  {:else if ui.k === 'offer'}
    {@const o = ui.o}
    <p>{o.qr ? t.addHow : t.addHowCode}</p>
    {#if qr}
      <svg class="qr" class:dim={expired} viewBox="0 0 {qr.size} {qr.size}" role="img" aria-label={t.addQr} shape-rendering="crispEdges">
        <rect width={qr.size} height={qr.size} fill="#fff" /><path d={path} />
      </svg>
    {/if}
    <b class="code" class:dim={expired}>{t.addCode(o.code.match(/.{1,3}/g)?.join(' ') ?? o.code)}</b>
    <small class:late={expired}>{expired ? t.addExpired : t.addLeft(left(o.exp, now))}</small>
    {#if o.link && !expired}
      <div class="links">
        {#if canShare}<button class="tonal" onclick={() => share(o.link!)}>{t.addShare}</button>{/if}
        <button class="tonal" onclick={() => copy(o.link!)}>{copied ? t.addCopied : t.addCopy}</button>
      </div>
    {/if}
    <div class="btns">
      {#if expired}<button class="primary" onclick={request}>{t.addAgain}</button>
      {:else}<button class="primary" onclick={close}>{t.addDone}</button>{/if}
    </div>
  {:else}
    <p class="msg">{ui.k === 'lost' ? t.addLost : t.addRefused(ui.r.kind, ui.r.reason)}</p>
    <div class="btns">
      <button class="link" onclick={close}>{t.closeWord}</button>
      <button class="primary" onclick={request}>{t.addRetry}</button>
    </div>
  {/if}
</dialog>

<style>
  dialog { margin: auto; border: 0; color: var(--text); background: var(--surface); padding: 22px 24px 16px; width: min(420px, 92vw); border-radius: 28px; }
  dialog[open] { display: flex; flex-direction: column; align-items: center; gap: 14px; }
  dialog::backdrop { background: rgb(0 0 0 / .6); }
  h3 { font-size: 22px; font-weight: 500; margin: 0; align-self: stretch; }
  p { color: var(--text2); font-size: 15px; margin: 0; align-self: stretch; }
  p.msg { color: var(--text); }
  .qr { width: min(300px, 100%); aspect-ratio: 1; border-radius: 16px; }
  .qr path { fill: #000; }
  .qr.dim path { fill: var(--tx2); }
  .code { font-family: var(--mono); font-size: 22px; font-weight: 500; letter-spacing: 1px; }
  .code.dim { color: var(--text2); }
  small { font-size: 14px; color: var(--text2); }
  small.late { color: var(--wait); }
  .links { display: flex; gap: 8px; flex-wrap: wrap; justify-content: center; }
  .btns { display: flex; justify-content: flex-end; gap: 6px; align-self: stretch; }
  button { padding: 10px 18px; border-radius: 999px; font-weight: 500; font-size: 14px; }
  .primary { background: var(--primary); color: var(--on-primary); flex: 1; min-height: 48px; font-size: 15px; }
  .tonal { background: var(--high); color: var(--icon); }
  .link { color: var(--icon); }
  .link:hover, .tonal:hover { filter: brightness(1.15); }
  .spin { width: 40px; height: 40px; margin: 20px; border-radius: 50%; border: 4px solid var(--high); border-top-color: var(--icon); animation: r 1s linear infinite; }
  @keyframes r { to { transform: rotate(1turn) } }
</style>
