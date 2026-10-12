package com.example.game.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.engine.GameWorld
import com.example.game.engine.SoundManager
import com.example.game.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.min

enum class AppScreen {
    MAIN_MENU,
    PLAYING,
    SHOP,
    LEADERBOARD,
    SETTINGS
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("nana_tayantuh_prefs", Context.MODE_PRIVATE)

    val soundManager = SoundManager(application)

    // Screen State
    private val _currentScreen = MutableStateFlow(AppScreen.MAIN_MENU)
    val currentScreen = _currentScreen.asStateFlow()

    // Player Profile State
    private val _playerName = MutableStateFlow(prefs.getString("player_name", "Nana") ?: "Nana")
    val playerName = _playerName.asStateFlow()

    private val _coins = MutableStateFlow(prefs.getInt("coins", 500)) // Give generous initial coins for testing shop!
    val coins = _coins.asStateFlow()

    private val _highScore = MutableStateFlow(prefs.getInt("high_score", 1200))
    val highScore = _highScore.asStateFlow()

    private val _totalKills = MutableStateFlow(prefs.getInt("total_kills", 8))
    val totalKills = _totalKills.asStateFlow()

    private val _totalGames = MutableStateFlow(prefs.getInt("total_games", 3))
    val totalGames = _totalGames.asStateFlow()

    // Unlocked skins, hats, and faces
    private val unlockedSkinIds = HashSet<String>().apply {
        addAll(prefs.getStringSet("unlocked_skins", setOf("skin_classic")) ?: setOf("skin_classic"))
    }
    private val unlockedHatIds = HashSet<String>().apply {
        addAll(prefs.getStringSet("unlocked_hats", setOf("hat_none")) ?: setOf("hat_none"))
    }
    private val unlockedFaceIds = HashSet<String>().apply {
        addAll(prefs.getStringSet("unlocked_faces", setOf("face_happy")) ?: setOf("face_happy"))
    }

    // Selected Skin, Hat, Face
    private val _selectedSkin = MutableStateFlow(
        Catalog.SKINS.find { it.id == prefs.getString("selected_skin", "skin_classic") } ?: Catalog.SKINS[0]
    )
    val selectedSkin = _selectedSkin.asStateFlow()

    private val _selectedHat = MutableStateFlow(
        Catalog.HATS.find { it.id == prefs.getString("selected_hat", "hat_none") } ?: Catalog.HATS[0]
    )
    val selectedHat = _selectedHat.asStateFlow()

    private val _selectedFace = MutableStateFlow(
        Catalog.FACES.find { it.id == prefs.getString("selected_face", "face_happy") } ?: Catalog.FACES[0]
    )
    val selectedFace = _selectedFace.asStateFlow()

    // Selected Room
    private val _selectedRoom = MutableStateFlow(Catalog.ROOMS[0])
    val selectedRoom = _selectedRoom.asStateFlow()

    // Sound settings
    private val _soundEnabled = MutableStateFlow(prefs.getBoolean("sound_enabled", true))
    val soundEnabled = _soundEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(prefs.getBoolean("vibration_enabled", true))
    val vibrationEnabled = _vibrationEnabled.asStateFlow()

    // Game World Engine
    val gameWorld: GameWorld = GameWorld(
        soundManager = soundManager,
        playerName = _playerName.value,
        selectedSkin = _selectedSkin.value,
        selectedHat = _selectedHat.value,
        selectedFace = _selectedFace.value
    )

    init {
        soundManager.isSoundEnabled = _soundEnabled.value
        soundManager.isVibrationEnabled = _vibrationEnabled.value
    }

    fun setPlayerName(name: String) {
        val trimmed = name.take(16)
        _playerName.value = trimmed
        gameWorld.playerName = trimmed
        gameWorld.playerWorm.name = trimmed
        prefs.edit().putString("player_name", trimmed).apply()
    }

    fun setScreen(screen: AppScreen) {
        soundManager.playClick()
        _currentScreen.value = screen
    }

    fun selectRoom(room: GameRoom) {
        soundManager.playClick()
        _selectedRoom.value = room
    }

    fun addBonusCoins(amount: Int = 300) {
        soundManager.playPotion()
        _coins.value += amount
        prefs.edit().putInt("coins", _coins.value).apply()
    }

    fun startGame() {
        soundManager.playClick()
        _totalGames.value += 1
        prefs.edit().putInt("total_games", _totalGames.value).apply()

        gameWorld.playerName = _playerName.value
        gameWorld.selectedSkin = _selectedSkin.value
        gameWorld.selectedHat = _selectedHat.value
        gameWorld.selectedFace = _selectedFace.value
        gameWorld.resetGame()
        _currentScreen.value = AppScreen.PLAYING
    }

    fun onGameOver() {
        val earnedCoins = gameWorld.coinsCollected
        _coins.value += earnedCoins
        prefs.edit().putInt("coins", _coins.value).apply()

        val kills = gameWorld.sessionKills
        _totalKills.value += kills
        prefs.edit().putInt("total_kills", _totalKills.value).apply()

        val finalScore = gameWorld.playerWorm.score
        if (finalScore > _highScore.value) {
            _highScore.value = finalScore
            prefs.edit().putInt("high_score", finalScore).apply()
        }
    }

    fun replayGame() {
        soundManager.playClick()
        gameWorld.playerName = _playerName.value
        gameWorld.selectedSkin = _selectedSkin.value
        gameWorld.selectedHat = _selectedHat.value
        gameWorld.selectedFace = _selectedFace.value
        gameWorld.resetGame()
    }

    // Revive with Coins (50 coins)
    fun payRevive(cost: Int = 50): Boolean {
        if (_coins.value >= cost) {
            _coins.value -= cost
            prefs.edit().putInt("coins", _coins.value).apply()
            return true
        }
        return false
    }

    // 7-Day Login Rewards
    private val _loginDay = MutableStateFlow(prefs.getInt("login_day", 1))
    val loginDay = _loginDay.asStateFlow()

    private val _claimedDays = MutableStateFlow<Set<Int>>(
        (prefs.getStringSet("claimed_days_set", emptySet()) ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet()
    )
    val claimedDays = _claimedDays.asStateFlow()

    fun isDayClaimed(day: Int): Boolean = _claimedDays.value.contains(day)

    fun claimDailyReward(reward: DailyReward): Boolean {
        if (_claimedDays.value.contains(reward.day)) return false

        _coins.value += reward.coins

        reward.skinId?.let { sId ->
            unlockedSkinIds.add(sId)
            prefs.edit().putStringSet("unlocked_skins", unlockedSkinIds).apply()
        }

        reward.hatId?.let { hId ->
            unlockedHatIds.add(hId)
            prefs.edit().putStringSet("unlocked_hats", unlockedHatIds).apply()
        }

        val nextSet = _claimedDays.value.toMutableSet().apply { add(reward.day) }
        _claimedDays.value = nextSet

        if (_loginDay.value < 7 && reward.day >= _loginDay.value) {
            _loginDay.value = min(7, _loginDay.value + 1)
            prefs.edit().putInt("login_day", _loginDay.value).apply()
        }

        prefs.edit()
            .putInt("coins", _coins.value)
            .putStringSet("claimed_days_set", nextSet.map { it.toString() }.toSet())
            .apply()

        soundManager.playPotion()
        return true
    }

    fun togglePause() {
        soundManager.playClick()
        gameWorld.isPaused = !gameWorld.isPaused
    }

    fun toggleSound() {
        val newState = !_soundEnabled.value
        _soundEnabled.value = newState
        soundManager.isSoundEnabled = newState
        prefs.edit().putBoolean("sound_enabled", newState).apply()
        soundManager.playClick()
    }

    fun toggleVibration() {
        val newState = !_vibrationEnabled.value
        _vibrationEnabled.value = newState
        soundManager.isVibrationEnabled = newState
        prefs.edit().putBoolean("vibration_enabled", newState).apply()
        soundManager.playClick()
    }

    fun isSkinUnlocked(skinId: String): Boolean = unlockedSkinIds.contains(skinId)
    fun isHatUnlocked(hatId: String): Boolean = unlockedHatIds.contains(hatId)
    fun isFaceUnlocked(faceId: String): Boolean = unlockedFaceIds.contains(faceId)

    fun selectSkin(skin: Skin) {
        soundManager.playClick()
        if (isSkinUnlocked(skin.id)) {
            _selectedSkin.value = skin
            gameWorld.selectedSkin = skin
            gameWorld.playerWorm.skin = skin
            prefs.edit().putString("selected_skin", skin.id).apply()
        }
    }

    fun buySkin(skin: Skin): Boolean {
        if (_coins.value >= skin.price && !isSkinUnlocked(skin.id)) {
            _coins.value -= skin.price
            unlockedSkinIds.add(skin.id)
            prefs.edit()
                .putInt("coins", _coins.value)
                .putStringSet("unlocked_skins", unlockedSkinIds)
                .apply()
            selectSkin(skin)
            soundManager.playPotion()
            return true
        }
        return false
    }

    fun selectHat(hat: Hat) {
        soundManager.playClick()
        if (isHatUnlocked(hat.id)) {
            _selectedHat.value = hat
            gameWorld.selectedHat = hat
            gameWorld.playerWorm.hat = hat
            prefs.edit().putString("selected_hat", hat.id).apply()
        }
    }

    fun buyHat(hat: Hat): Boolean {
        if (_coins.value >= hat.price && !isHatUnlocked(hat.id)) {
            _coins.value -= hat.price
            unlockedHatIds.add(hat.id)
            prefs.edit()
                .putInt("coins", _coins.value)
                .putStringSet("unlocked_hats", unlockedHatIds)
                .apply()
            selectHat(hat)
            soundManager.playPotion()
            return true
        }
        return false
    }

    fun selectFace(face: Face) {
        soundManager.playClick()
        if (isFaceUnlocked(face.id)) {
            _selectedFace.value = face
            gameWorld.selectedFace = face
            gameWorld.playerWorm.face = face
            prefs.edit().putString("selected_face", face.id).apply()
        }
    }

    fun buyFace(face: Face): Boolean {
        if (_coins.value >= face.price && !isFaceUnlocked(face.id)) {
            _coins.value -= face.price
            unlockedFaceIds.add(face.id)
            prefs.edit()
                .putInt("coins", _coins.value)
                .putStringSet("unlocked_faces", unlockedFaceIds)
                .apply()
            selectFace(face)
            soundManager.playPotion()
            return true
        }
        return false
    }

    // Global Leaderboard mock records
    fun getGlobalLeaderboard(tab: String): List<LeaderboardEntry> {
        val myName = _playerName.value
        val myScore = _highScore.value
        val myKills = _totalKills.value

        val baseList = when (tab) {
            "HARI_INI" -> listOf(
                LeaderboardEntry(1, "SultanCacing", 12540, 24, "ID"),
                LeaderboardEntry(2, "BudiDragon", 9820, 18, "ID"),
                LeaderboardEntry(3, "ViperKing", 8940, 15, "SG"),
                LeaderboardEntry(4, "TokyoDrift99", 7630, 12, "JP"),
                LeaderboardEntry(5, "GarudaJuara", 6850, 11, "ID"),
                LeaderboardEntry(6, "SnakeMaster", 5420, 9, "MY"),
                LeaderboardEntry(7, "MegaWorm", 4910, 8, "US")
            )
            "MINGGUAN" -> listOf(
                LeaderboardEntry(1, "RajaCacingID", 38400, 72, "ID"),
                LeaderboardEntry(2, "ApexPredator", 31200, 65, "JP"),
                LeaderboardEntry(3, "AuraSlayer", 28900, 58, "ID"),
                LeaderboardEntry(4, "SpeedDemon", 24300, 47, "BR"),
                LeaderboardEntry(5, "NasiGorengPro", 21500, 42, "ID"),
                LeaderboardEntry(6, "BlitzStorm", 18900, 39, "SG")
            )
            else -> listOf( // ALL TIME
                LeaderboardEntry(1, "NanaMaster", 88900, 192, "ID"),
                LeaderboardEntry(2, "LegendWorm", 74200, 168, "JP"),
                LeaderboardEntry(3, "KingOfSlither", 69400, 145, "US"),
                LeaderboardEntry(4, "GarudaMythic", 58300, 120, "ID"),
                LeaderboardEntry(5, "DragonLord", 49600, 108, "SG"),
                LeaderboardEntry(6, "PhantomSnake", 43200, 95, "MY")
            )
        }

        val mutable = ArrayList(baseList)
        // Insert player based on score
        val playerEntry = LeaderboardEntry(0, myName, myScore, myKills, "ID", isPlayer = true)
        mutable.add(playerEntry)
        mutable.sortByDescending { it.score }

        return mutable.mapIndexed { index, entry ->
            entry.copy(rank = index + 1)
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
