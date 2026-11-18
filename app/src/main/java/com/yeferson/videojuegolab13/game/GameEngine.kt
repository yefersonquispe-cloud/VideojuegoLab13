package com.yeferson.videojuegolab13.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

const val MAX_HULL = 3
const val SHIP_RADIUS = 14f

private const val MAX_STEP = 0.05f
private const val COUNTDOWN_STEP = 0.8f
private const val GO_TIME = 0.6f
private const val DESTROYED_TIME = 1.7f
private const val HIT_INVULNERABLE = 1.8f
private const val TRIPLE_TIME = 8f
private const val BULLET_SPEED = 640f
private const val PULSE_SPEED = 760f
private const val MAX_PARTICLES = 260
private const val MAX_TEXTS = 8

private const val COLOR_BONE = 0xFFF2E6C9.toInt()
private const val COLOR_MUSTARD = 0xFFF5B82E.toInt()
private const val COLOR_VERMILION = 0xFFF0452B.toInt()
private const val COLOR_TURQUOISE = 0xFF37C8B8.toInt()
private const val COLOR_ROCK = 0xFFB9A58A.toInt()
private const val COLOR_ROCK_SHADOW = 0xFF6F5B52.toInt()

class GameEngine(private val random: Random = Random.Default) {
    var worldWidth = 400f
        private set
    var worldHeight = 800f
        private set

    var phase by mutableStateOf(GamePhase.MENU)
        private set
    var countdown by mutableIntStateOf(3)
        private set
    var score by mutableIntStateOf(0)
        private set
    var bestScore by mutableIntStateOf(0)
        private set
    var isNewRecord by mutableStateOf(false)
        private set
    var hull by mutableIntStateOf(MAX_HULL)
        private set
    var secondsSurvived by mutableIntStateOf(0)
        private set
    var meteorsDestroyed by mutableIntStateOf(0)
        private set
    var pulseCharge by mutableFloatStateOf(0f)
        private set
    var hasShield by mutableStateOf(false)
        private set
    var activePower by mutableStateOf<PowerUpType?>(null)
        private set
    var powerFraction by mutableFloatStateOf(0f)
        private set

    var hitEvents by mutableIntStateOf(0)
        private set
    var shieldEvents by mutableIntStateOf(0)
        private set
    var pulseEvents by mutableIntStateOf(0)
        private set
    var pickupEvents by mutableIntStateOf(0)
        private set

    var frame by mutableLongStateOf(0L)
        private set

    val meteors = ArrayList<Meteor>()
    val bullets = ArrayList<Bullet>()
    val particles = ArrayList<Particle>()
    val waves = ArrayList<Shockwave>()
    val powerUps = ArrayList<PowerUp>()
    val texts = ArrayList<FloatingText>()

    var shipX = 200f
        private set
    var shipY = 500f
        private set
    var shipTilt = 0f
        private set
    var shipVisible = true
        private set
    var invulnerableTime = 0f
        private set

    var pulseRadius = -1f
        private set
    var pulseX = 0f
        private set
    var pulseY = 0f
        private set

    var clock = 0f
        private set
    var scroll = 0f
        private set
    var scrollSpeed = 24f
        private set

    private var targetX = 200f
    private var targetY = 500f
    private var elapsed = 0f
    private var countdownTime = 0f
    private var destroyedTime = 0f
    private var spawnTimer = 0f
    private var powerUpTimer = 0f
    private var fireTimer = 0f
    private var exhaustTimer = 0f
    private var powerTime = 0f
    private var powerDuration = 1f
    private var charge = 0f
    private val pendingMeteors = ArrayList<Meteor>()

    val shipDrawY: Float
        get() = if (phase == GamePhase.MENU) shipY + sin(clock * 1.6f) * 8f else shipY

    fun resize(width: Float, height: Float) {
        if (width == worldWidth && height == worldHeight) return
        worldWidth = width
        worldHeight = height
        if (phase == GamePhase.MENU) placeShipForMenu() else clampTarget()
    }

    fun loadBestScore(value: Int) {
        bestScore = value
    }

    fun startMission() {
        clearWorld()
        score = 0
        hull = MAX_HULL
        secondsSurvived = 0
        meteorsDestroyed = 0
        charge = 0f
        pulseCharge = 0f
        hasShield = false
        activePower = null
        powerFraction = 0f
        isNewRecord = false
        elapsed = 0f
        invulnerableTime = 0f
        shipVisible = true
        spawnTimer = 0.8f
        powerUpTimer = 8f
        fireTimer = 0f
        targetX = worldWidth / 2f
        targetY = worldHeight * 0.72f
        countdownTime = COUNTDOWN_STEP * 3 + GO_TIME
        countdown = 3
        phase = GamePhase.COUNTDOWN
    }

    fun pause() {
        if (phase == GamePhase.PLAYING) phase = GamePhase.PAUSED
    }

    fun resume() {
        if (phase == GamePhase.PAUSED) phase = GamePhase.PLAYING
    }

    fun backToMenu() {
        clearWorld()
        shipVisible = true
        hull = MAX_HULL
        activePower = null
        hasShield = false
        placeShipForMenu()
        phase = GamePhase.MENU
    }

    fun moveShipBy(dx: Float, dy: Float) {
        if (phase != GamePhase.PLAYING && phase != GamePhase.COUNTDOWN) return
        targetX += dx
        targetY += dy
        clampTarget()
    }

    fun firePulse(): Boolean {
        if (phase != GamePhase.PLAYING || charge < 1f) return false
        charge = 0f
        pulseCharge = 0f
        pulseRadius = 0f
        pulseX = shipX
        pulseY = shipY
        invulnerableTime = maxOf(invulnerableTime, 0.9f)
        pulseEvents++
        return true
    }

    fun step(deltaSeconds: Float) {
        if (phase == GamePhase.PAUSED) return
        val dt = deltaSeconds.coerceIn(0f, MAX_STEP)
        clock += dt
        updateScroll(dt)
        when (phase) {
            GamePhase.COUNTDOWN -> updateCountdown(dt)
            GamePhase.PLAYING -> updatePlaying(dt)
            GamePhase.DESTROYED -> updateDestroyed(dt)
            GamePhase.GAME_OVER -> {
                updateMeteors(dt * 0.4f)
                updateEffects(dt)
            }
            else -> Unit
        }
        frame++
    }

    private fun updateScroll(dt: Float) {
        val target = when (phase) {
            GamePhase.MENU -> 24f
            GamePhase.COUNTDOWN -> 520f
            GamePhase.PLAYING -> 110f
            else -> 40f
        }
        scrollSpeed += (target - scrollSpeed) * (1f - exp(-2.5f * dt))
        scroll += scrollSpeed * dt
    }

    private fun updateCountdown(dt: Float) {
        countdownTime -= dt
        updateShip(dt, 3.5f)
        emitExhaust(dt)
        updateEffects(dt)
        countdown = if (countdownTime > GO_TIME) {
            ceil((countdownTime - GO_TIME) / COUNTDOWN_STEP).toInt()
        } else {
            0
        }
        if (countdownTime <= 0f) {
            phase = GamePhase.PLAYING
        }
    }

    private fun updatePlaying(dt: Float) {
        elapsed += dt
        updateProgress()
        updateTimers(dt)
        updateShip(dt, 18f)
        emitExhaust(dt)
        updateFire(dt)
        updateSpawns(dt)
        updateBullets(dt)
        updateMeteors(dt)
        updatePowerUps(dt)
        updatePulse(dt)
        resolveBulletHits()
        resolveShipHits()
        meteors.addAll(pendingMeteors)
        pendingMeteors.clear()
        updateEffects(dt)
    }

    private fun updateDestroyed(dt: Float) {
        destroyedTime -= dt
        updateBullets(dt * 0.35f)
        updateMeteors(dt * 0.35f)
        updateEffects(dt * 0.6f)
        if (destroyedTime <= 0f) {
            if (score > bestScore) {
                bestScore = score
                isNewRecord = true
            }
            phase = GamePhase.GAME_OVER
        }
    }

    private fun updateProgress() {
        val seconds = elapsed.toInt()
        if (seconds > secondsSurvived) {
            secondsSurvived = seconds
            score += 5
        }
    }

    private fun updateTimers(dt: Float) {
        invulnerableTime = (invulnerableTime - dt).coerceAtLeast(0f)
        if (activePower != null) {
            powerTime -= dt
            val fraction = (powerTime / powerDuration).coerceIn(0f, 1f)
            if (abs(fraction - powerFraction) > 0.02f) powerFraction = fraction
            if (powerTime <= 0f) {
                activePower = null
                powerFraction = 0f
            }
        }
        addCharge(0.012f * dt)
    }

    private fun addCharge(amount: Float) {
        charge = (charge + amount).coerceAtMost(1f)
        if (charge == 1f || abs(charge - pulseCharge) > 0.01f) pulseCharge = charge
    }

    private fun updateShip(dt: Float, follow: Float) {
        val blend = 1f - exp(-follow * dt)
        val previousX = shipX
        shipX += (targetX - shipX) * blend
        shipY += (targetY - shipY) * blend
        val speed = if (dt > 0f) (shipX - previousX) / dt else 0f
        val targetTilt = (speed / 520f).coerceIn(-1f, 1f)
        shipTilt += (targetTilt - shipTilt) * (1f - exp(-12f * dt))
    }

    private fun emitExhaust(dt: Float) {
        exhaustTimer -= dt
        if (exhaustTimer > 0f || !shipVisible) return
        exhaustTimer = 0.035f
        val color = if (random.nextBoolean()) COLOR_MUSTARD else COLOR_VERMILION
        addParticle(
            shipX + randomBetween(-6f, 6f), shipY + 28f,
            randomBetween(-18f, 18f), randomBetween(120f, 220f),
            randomBetween(0.2f, 0.4f), randomBetween(2f, 4.5f), color
        )
    }

    private fun updateFire(dt: Float) {
        fireTimer -= dt
        if (fireTimer > 0f) return
        val triple = activePower == PowerUpType.TRIPLE
        fireTimer = if (triple) 0.17f else 0.22f
        bullets.add(Bullet(shipX, shipY - 28f, 0f, -BULLET_SPEED))
        if (triple) {
            bullets.add(Bullet(shipX - 12f, shipY - 16f, -110f, -BULLET_SPEED))
            bullets.add(Bullet(shipX + 12f, shipY - 16f, 110f, -BULLET_SPEED))
        }
    }

    private fun updateSpawns(dt: Float) {
        spawnTimer -= dt
        if (spawnTimer <= 0f) {
            spawnRock()
            val interval = (1.05f / (1f + elapsed / 40f)).coerceAtLeast(0.26f)
            spawnTimer = interval * randomBetween(0.8f, 1.2f)
        }

        powerUpTimer -= dt
        if (powerUpTimer <= 0f) {
            spawnPowerUp(randomBetween(40f, worldWidth - 40f), -24f)
            powerUpTimer = randomBetween(9f, 14f)
        }
    }

    private fun spawnRock() {
        val roll = random.nextFloat()
        val size = when {
            roll < 0.5f -> MeteorSize.SMALL
            roll < 0.85f -> MeteorSize.MEDIUM
            else -> MeteorSize.LARGE
        }
        val speedScale = (1f + elapsed / 90f).coerceAtMost(2.2f)
        val sizeScale = when (size) {
            MeteorSize.SMALL -> 1.15f
            MeteorSize.MEDIUM -> 1f
            MeteorSize.LARGE -> 0.72f
        }
        meteors.add(
            createMeteor(
                x = randomBetween(size.radius, worldWidth - size.radius),
                y = -size.radius - 10f,
                vx = randomBetween(-30f, 30f),
                vy = randomBetween(130f, 190f) * speedScale * sizeScale,
                size = size
            )
        )
    }

    private fun createMeteor(
        x: Float,
        y: Float,
        vx: Float,
        vy: Float,
        size: MeteorSize
    ): Meteor {
        return Meteor(
            x = x,
            y = y,
            vx = vx,
            vy = vy,
            size = size,
            shape = random.nextInt(METEOR_SHAPES),
            spin = randomBetween(-160f, 160f),
            color = COLOR_ROCK,
            shadow = COLOR_ROCK_SHADOW
        )
    }

    private fun spawnPowerUp(x: Float, y: Float) {
        val options = PowerUpType.entries
        powerUps.add(PowerUp(x, y, options[random.nextInt(options.size)]))
    }

    private fun updateBullets(dt: Float) {
        val iterator = bullets.iterator()
        while (iterator.hasNext()) {
            val bullet = iterator.next()
            bullet.x += bullet.vx * dt
            bullet.y += bullet.vy * dt
            if (bullet.y < -20f || bullet.x < -20f || bullet.x > worldWidth + 20f) iterator.remove()
        }
    }

    private fun updateMeteors(dt: Float) {
        val iterator = meteors.iterator()
        while (iterator.hasNext()) {
            val meteor = iterator.next()
            meteor.x += meteor.vx * dt
            meteor.y += meteor.vy * dt
            meteor.rotation += meteor.spin * dt
            meteor.hitFlash = (meteor.hitFlash - dt * 6f).coerceAtLeast(0f)
            val outside = meteor.y > worldHeight + meteor.radius + 20f ||
                meteor.x < -meteor.radius - 60f || meteor.x > worldWidth + meteor.radius + 60f
            if (outside) iterator.remove()
        }
    }

    private fun updatePowerUps(dt: Float) {
        val iterator = powerUps.iterator()
        while (iterator.hasNext()) {
            val powerUp = iterator.next()
            powerUp.age += dt
            powerUp.y += 120f * dt
            powerUp.x = powerUp.originX + sin(powerUp.age * 2.4f) * 22f
            if (hypot(powerUp.x - shipX, powerUp.y - shipY) < 34f) {
                collect(powerUp)
                iterator.remove()
            } else if (powerUp.y > worldHeight + 30f) {
                iterator.remove()
            }
        }
    }

    private fun collect(powerUp: PowerUp) {
        pickupEvents++
        waves.add(Shockwave(shipX, shipY, 70f, 0.4f, COLOR_TURQUOISE))
        when (powerUp.type) {
            PowerUpType.SHIELD -> {
                hasShield = true
                addText(shipX, shipY - 44f, "Escudo", COLOR_TURQUOISE)
            }
            PowerUpType.TRIPLE -> {
                startPower(PowerUpType.TRIPLE, TRIPLE_TIME)
                addText(shipX, shipY - 44f, "Disparo triple", COLOR_MUSTARD)
            }
        }
    }

    private fun startPower(type: PowerUpType, duration: Float) {
        activePower = type
        powerTime = duration
        powerDuration = duration
        powerFraction = 1f
    }

    private fun updatePulse(dt: Float) {
        if (pulseRadius < 0f) return
        pulseRadius += PULSE_SPEED * dt
        var index = meteors.lastIndex
        while (index >= 0) {
            val meteor = meteors[index]
            if (hypot(meteor.x - pulseX, meteor.y - pulseY) < pulseRadius + meteor.radius) {
                meteors.removeAt(index)
                destroyMeteor(meteor, split = false)
            }
            index--
        }
        if (pulseRadius > hypot(worldWidth, worldHeight)) pulseRadius = -1f
    }

    private fun resolveBulletHits() {
        var bulletIndex = bullets.lastIndex
        while (bulletIndex >= 0) {
            val bullet = bullets[bulletIndex]
            var meteorIndex = meteors.lastIndex
            while (meteorIndex >= 0) {
                val meteor = meteors[meteorIndex]
                if (hypot(bullet.x - meteor.x, bullet.y - meteor.y) < meteor.radius + 4f) {
                    bullets.removeAt(bulletIndex)
                    meteor.hp--
                    meteor.hitFlash = 1f
                    burst(bullet.x, bullet.y, 3, 110f, COLOR_MUSTARD)
                    if (meteor.hp <= 0) {
                        meteors.removeAt(meteorIndex)
                        destroyMeteor(meteor, split = true)
                    }
                    break
                }
                meteorIndex--
            }
            bulletIndex--
        }
    }

    private fun resolveShipHits() {
        if (invulnerableTime > 0f) return
        var index = meteors.lastIndex
        while (index >= 0) {
            val meteor = meteors[index]
            if (hypot(meteor.x - shipX, meteor.y - shipY) < meteor.radius * 0.86f + SHIP_RADIUS) {
                meteors.removeAt(index)
                burst(meteor.x, meteor.y, 14, 190f, meteor.color)
                damageShip()
                return
            }
            index--
        }
    }

    private fun damageShip() {
        if (hasShield) {
            hasShield = false
            invulnerableTime = 1f
            shieldEvents++
            waves.add(Shockwave(shipX, shipY, 90f, 0.5f, COLOR_TURQUOISE))
            return
        }
        hull--
        hitEvents++
        invulnerableTime = HIT_INVULNERABLE
        waves.add(Shockwave(shipX, shipY, 80f, 0.4f, COLOR_VERMILION))
        if (hull <= 0) destroyShip()
    }

    private fun destroyShip() {
        shipVisible = false
        activePower = null
        powerFraction = 0f
        pulseRadius = -1f
        destroyedTime = DESTROYED_TIME
        burst(shipX, shipY, 46, 300f, COLOR_MUSTARD)
        burst(shipX, shipY, 34, 220f, COLOR_VERMILION)
        burst(shipX, shipY, 22, 150f, COLOR_BONE)
        waves.add(Shockwave(shipX, shipY, 190f, 0.9f, COLOR_BONE))
        waves.add(Shockwave(shipX, shipY, 120f, 0.7f, COLOR_VERMILION))
        phase = GamePhase.DESTROYED
    }

    private fun destroyMeteor(meteor: Meteor, split: Boolean) {
        meteorsDestroyed++
        val gained = meteor.size.points
        score += gained
        addCharge(meteor.size.charge)
        addText(meteor.x, meteor.y, "+$gained", COLOR_BONE)
        burst(meteor.x, meteor.y, (meteor.radius * 0.7f).toInt() + 5, 170f, meteor.color)
        waves.add(Shockwave(meteor.x, meteor.y, meteor.radius * 2.2f, 0.35f, COLOR_BONE))

        if (split) splitMeteor(meteor)
        val dropChance = when (meteor.size) {
            MeteorSize.SMALL -> 0f
            MeteorSize.MEDIUM -> 0.08f
            MeteorSize.LARGE -> 0.2f
        }
        if (random.nextFloat() < dropChance) spawnPowerUp(meteor.x, meteor.y)
    }

    private fun splitMeteor(meteor: Meteor) {
        val smaller = when (meteor.size) {
            MeteorSize.LARGE -> MeteorSize.MEDIUM
            MeteorSize.MEDIUM -> MeteorSize.SMALL
            MeteorSize.SMALL -> return
        }
        for (side in intArrayOf(-1, 1)) {
            pendingMeteors.add(
                createMeteor(
                    x = meteor.x + side * smaller.radius * 0.7f,
                    y = meteor.y,
                    vx = meteor.vx + side * randomBetween(50f, 95f),
                    vy = meteor.vy * randomBetween(0.9f, 1.15f),
                    size = smaller
                )
            )
        }
    }

    private fun updateEffects(dt: Float) {
        val particleIterator = particles.iterator()
        while (particleIterator.hasNext()) {
            val particle = particleIterator.next()
            particle.life -= dt
            particle.x += particle.vx * dt
            particle.y += particle.vy * dt
            particle.vx *= 1f - 1.8f * dt
            particle.vy *= 1f - 1.8f * dt
            if (particle.life <= 0f) particleIterator.remove()
        }
        val waveIterator = waves.iterator()
        while (waveIterator.hasNext()) {
            val wave = waveIterator.next()
            wave.life -= dt
            if (wave.life <= 0f) waveIterator.remove()
        }
        val textIterator = texts.iterator()
        while (textIterator.hasNext()) {
            val text = textIterator.next()
            text.life -= dt
            text.y -= 42f * dt
            if (text.life <= 0f) textIterator.remove()
        }
    }

    private fun burst(x: Float, y: Float, count: Int, speed: Float, color: Int) {
        repeat(count) {
            val angle = randomBetween(0f, 6.2832f)
            val velocity = randomBetween(speed * 0.3f, speed)
            addParticle(
                x, y, cos(angle) * velocity, sin(angle) * velocity,
                randomBetween(0.3f, 0.8f), randomBetween(1.8f, 4.8f), color
            )
        }
    }

    private fun addParticle(
        x: Float,
        y: Float,
        vx: Float,
        vy: Float,
        life: Float,
        size: Float,
        color: Int
    ) {
        if (particles.size < MAX_PARTICLES) particles.add(Particle(x, y, vx, vy, life, size, color))
    }

    private fun addText(x: Float, y: Float, text: String, color: Int) {
        if (texts.size >= MAX_TEXTS) texts.removeAt(0)
        texts.add(FloatingText(x, y, text, color))
    }

    private fun clearWorld() {
        meteors.clear()
        pendingMeteors.clear()
        bullets.clear()
        particles.clear()
        waves.clear()
        powerUps.clear()
        texts.clear()
        pulseRadius = -1f
    }

    private fun placeShipForMenu() {
        shipX = worldWidth * 0.62f
        shipY = worldHeight * 0.5f
        shipTilt = 0f
        targetX = shipX
        targetY = shipY
    }

    private fun clampTarget() {
        targetX = targetX.coerceIn(26f, (worldWidth - 26f).coerceAtLeast(26f))
        targetY = targetY.coerceIn(worldHeight * 0.28f, (worldHeight - 70f).coerceAtLeast(worldHeight * 0.28f))
    }

    private fun randomBetween(min: Float, max: Float): Float =
        min + random.nextFloat() * (max - min)

    internal fun addMeteorForTest(meteor: Meteor) {
        meteors.add(meteor)
    }

    internal fun fillPulseForTest() {
        charge = 1f
        pulseCharge = 1f
    }

    internal fun giveShieldForTest() {
        hasShield = true
    }

    internal fun clearInvulnerabilityForTest() {
        invulnerableTime = 0f
    }

    companion object {
        const val METEOR_SHAPES = 6
    }
}
