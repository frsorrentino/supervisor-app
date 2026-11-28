package it.pixelbox.cmwatch.rules

import kotlin.math.abs

/**
 * Le molle senza rimbalzo della master: l'arrivo del suo foglio dopo il dito ([MasterSheet], app-posizioni-sistema-27-11 gruppo 4) e i
 * movimenti che la seguono (Fx.land). Con il gruppo 6 vanno il riconoscitore delle maniglie e la corsa breve fra le posizioni di
 * prima, che non usa più nessuno: il foglio scatta col punto proiettato (MasterSheet.snap).
 */
object MasterGestures {
    /**
     * Il cambio di lato che continua il gesto (app-lato-animazione-06-11): una molla smorzata senza rimbalzo, rigidità 300
     * (ω ≈ 17): da ferma arriva al 95 % in circa 270 ms, partendo dalla velocità del dito. Le stesse in gesture.ts.
     */
    const val SIDE_SPRING_DAMPING = 1f
    const val SIDE_SPRING_STIFFNESS = 300f
    /** La molla più rigida che si usa per un colpo veloce: ω = 100 al secondo, al 95 % in circa 50 ms. */
    const val LAND_MAX_STIFFNESS = 10000f

    /** La molla d'arrivo di un rilascio: la rigidità e la velocità di partenza da usare. */
    data class Land(val stiffness: Float, val velocity: Float)

    /**
     * Ogni rilascio della master arriva senza rimbalzo (app-altezza-senza-rimbalzo-06-11, Franz 06/11 10:59: «dopo il
     * ridimensionamento della master c'è ancora il rimbalzo»): la molla smorzata in modo critico x(t) = (x0 + (v + ω·x0)·t)·e^(−ωt)
     * passa l'arrivo e torna quando il dito va verso l'arrivo più veloce di ω·|x0|. Allora la molla si fa più rigida, ω = |v| / |x0|,
     * e parte con la velocità del dito senza superare l'arrivo; oltre LAND_MAX_STIFFNESS resta quella e la velocità si limita.
     * `from` è la distanza dall'arrivo (arrivo = 0), `velocity` nelle stesse unità al secondo. Le stesse in gesture.ts (landSpring).
     */
    fun land(from: Float, velocity: Float): Land {
        val w = kotlin.math.sqrt(SIDE_SPRING_STIFFNESS)
        if (from == 0f || from * velocity >= 0f || abs(velocity) <= w * abs(from)) return Land(SIDE_SPRING_STIFFNESS, velocity)
        val wMax = kotlin.math.sqrt(LAND_MAX_STIFFNESS)
        val need = abs(velocity) / abs(from)
        return if (need <= wMax) Land(need * need, velocity) else Land(LAND_MAX_STIFFNESS, -wMax * from)
    }

    /** La posizione al tempo `t` (s) della molla d'arrivo da `from` con `velocity`: per le prove. */
    fun landAt(from: Float, velocity: Float, t: Float): Float {
        val l = land(from, velocity)
        val w = kotlin.math.sqrt(l.stiffness)
        return (from + (l.velocity + w * from) * t) * kotlin.math.exp(-w * t)
    }
}
