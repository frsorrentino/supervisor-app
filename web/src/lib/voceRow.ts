// La riga di una voce, la stessa in «Per te» nella sessione e in Dalla master (app-riall-2-viste-16-11, direzione A scelta da Franz
// il 16/11): il contesto, da quanto aspetta, il consiglio di chi chiede, il tasto principale e che cosa fa. Dopo un tasto la voce esce
// subito da ogni vista (attese.svelte.ts); se il PC rifiuta, torna.
import type { CmdResult, WaitItem } from './contract'
import { newCmd } from './transport'
import { answeredLater } from './attese.svelte'
import { LEAVE_MS } from './tieniPremuto'
import { age, cmdOf } from './tiAspetta'
import { t } from './t'
import { lang } from './lang'
import { silencePending, silenceWhenShort } from './silence'
import type { Advice, Voce } from './perTe'

type Run = (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>

export const subOf = (v: Voce): string => [t.perTeGroup[v.group], v.result ? [t.watchKind[v.result.kind], v.result.version != null ? t.perTeVersion(v.result.version) : null].filter(Boolean).join(', ') : v.context].filter(Boolean).join(' · ')
export const agoOf = (v: Voce, now: number): string => (v.group === 'aspetta' ? t.tiAspettaSince(age(now - v.since)) : age(now - v.since))
/** app-silenzio-assenso-23-11: «si chiude da sola gio 26/11» se la voce ha una scadenza non ancora passata; altrimenti niente. */
export const silenceOf = (v: Voce, now: number): string | null => {
  const u = v.wait?.silence_until ?? null
  return u != null && silencePending(u, now) ? t.silenceRow(silenceWhenShort(u, lang === 'en')) : null
}
export const mainLabelOf = (v: Voce): string | null => (v.result ? t.watchSeen : v.main?.label ?? null)
/** «consigliata: approvare», «consigliata: 2 · sì»; senza consiglio, niente. */
export const adviceText = (a: Advice | null): string | null =>
  !a ? null : a.option != null ? t.perTeAdviceOption(a.option) : a.approve ? t.perTeAdviceApprove : t.perTeAdviceReject

/** Il tasto della riga: segna visto un risultato, risponde a una richiesta col suo tasto; un tasto che chiede un testo apre la scheda. */
export async function actVoce(run: Run, v: Voce, h: { open: (v: Voce) => void; reread: () => void; onAnswered: (w: WaitItem) => void }) {
  if (v.result) { await fire(run, v.key, newCmd('result_seen', null, v.result.id), h.reread); return }
  const w = v.wait, a = w && v.main ? w.actions?.[v.main.index] : null
  if (!w || !a || !v.main?.direct) { h.open(v); return }
  const c = cmdOf(a, null)
  // app-tieni-premuto-28-11: dopo un ok o un Va a un tocco la riga esce a spunta disegnata, non subito.
  if (await fire(run, v.key, { ...newCmd(c.op as Parameters<typeof newCmd>[0], c.session ?? null, c.arg ?? null), ...c } as ReturnType<typeof newCmd>, h.reread, v.main.tap ? LEAVE_MS : 0)) h.onAnswered(w)
}

async function fire(run: Run, key: string, c: ReturnType<typeof newCmd>, reread: () => void, wait = 0): Promise<boolean> {
  const out = answeredLater(key, wait)
  const r = await run(c)
  if (r?.ok) { reread(); return true }
  out.undo()
  return false
}
