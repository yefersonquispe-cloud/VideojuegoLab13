package com.yeferson.videojuegolab13

import com.yeferson.videojuegolab13.game.GameEngine
import com.yeferson.videojuegolab13.game.GamePhase
import com.yeferson.videojuegolab13.game.MAX_HULL
import com.yeferson.videojuegolab13.game.Meteor
import com.yeferson.videojuegolab13.game.MeteorKind
import com.yeferson.videojuegolab13.game.MeteorSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private lateinit var engine: GameEngine

    @Before
    fun prepararPartida() {
        engine = GameEngine(Random(7))
        engine.resize(400f, 800f)
        engine.startMission()
        avanzar(3.2f)
    }

    private fun avanzar(segundos: Float) {
        var restante = segundos
        while (restante > 0f) {
            engine.step(0.016f)
            restante -= 0.016f
        }
    }

    private fun meteoritoSobreLaNave(size: MeteorSize = MeteorSize.SMALL) = Meteor(
        x = engine.shipX,
        y = engine.shipY,
        vx = 0f,
        vy = 0f,
        size = size,
        kind = MeteorKind.ROCK,
        shape = 0,
        spin = 0f,
        color = 0,
        shadow = 0
    )

    private fun golpearNave() {
        engine.meteors.clear()
        engine.clearInvulnerabilityForTest()
        engine.addMeteorForTest(meteoritoSobreLaNave())
        engine.step(0.016f)
    }

    @Test
    fun laCuentaRegresivaIniciaLaPartida() {
        assertEquals(GamePhase.PLAYING, engine.phase)
        assertEquals(MAX_HULL, engine.hull)
    }

    @Test
    fun laNaveNoSaleDeLaPantalla() {
        engine.moveShipBy(-5000f, 5000f)
        engine.meteors.clear()
        avanzar(0.6f)
        assertTrue(engine.shipX >= 26f)
        assertTrue(engine.shipY <= 730f)
    }

    @Test
    fun unImpactoQuitaUnaVidaYDaInvulnerabilidad() {
        golpearNave()
        assertEquals(MAX_HULL - 1, engine.hull)
        assertEquals(1, engine.hitEvents)
        assertTrue(engine.invulnerableTime > 0f)
    }

    @Test
    fun elEscudoAbsorbeElImpacto() {
        engine.giveShieldForTest()
        golpearNave()
        assertEquals(MAX_HULL, engine.hull)
        assertFalse(engine.hasShield)
        assertEquals(1, engine.shieldEvents)
    }

    @Test
    fun sinVidasLaPartidaTermina() {
        repeat(MAX_HULL) { golpearNave() }
        assertEquals(GamePhase.DESTROYED, engine.phase)
        avanzar(2f)
        assertEquals(GamePhase.GAME_OVER, engine.phase)
    }

    @Test
    fun elPulsoNecesitaCargaCompleta() {
        assertFalse(engine.firePulse())
        engine.fillPulseForTest()
        assertTrue(engine.firePulse())
        assertEquals(0f, engine.pulseCharge)
    }

    @Test
    fun elPausadoDetieneLaSimulacion() {
        engine.pause()
        val fotograma = engine.frame
        avanzar(0.5f)
        assertEquals(GamePhase.PAUSED, engine.phase)
        assertEquals(fotograma, engine.frame)
        engine.resume()
        assertEquals(GamePhase.PLAYING, engine.phase)
    }
}
