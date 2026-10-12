package com.example.game.model

import androidx.compose.ui.graphics.Color

object Catalog {
    val SKINS = listOf(
        Skin(
            id = "skin_classic",
            name = "Zamrud Klasik",
            colors = listOf(Color(0xFF00E676), Color(0xFF76FF03), Color(0xFF00C853)),
            pattern = PatternType.STRIPES,
            price = 0,
            isUnlocked = true,
            glowColor = Color(0x6600E676)
        ),
        Skin(
            id = "skin_rainbow",
            name = "Permen Pelangi",
            colors = listOf(
                Color(0xFFFF1744),
                Color(0xFFFF9100),
                Color(0xFFFFEA00),
                Color(0xFF00E676),
                Color(0xFF00B0FF),
                Color(0xFFD500F9)
            ),
            pattern = PatternType.RAINBOW,
            price = 150,
            isUnlocked = false,
            glowColor = Color(0x66FF9100)
        ),
        Skin(
            id = "skin_watermelon",
            name = "Semangka Manis",
            colors = listOf(Color(0xFF2E7D32), Color(0xFFFF1744), Color(0xFF1B5E20)),
            pattern = PatternType.STRIPES,
            price = 180,
            isUnlocked = false,
            glowColor = Color(0x66FF1744)
        ),
        Skin(
            id = "skin_bumblebee",
            name = "Lebah Madu",
            colors = listOf(Color(0xFFFFEA00), Color(0xFF212121)),
            pattern = PatternType.STRIPES,
            price = 200,
            isUnlocked = false,
            glowColor = Color(0x66FFEA00)
        ),
        Skin(
            id = "skin_bubblegum",
            name = "Permen Karet",
            colors = listOf(Color(0xFFFF4081), Color(0xFFFFFFFF), Color(0xFF80D8FF)),
            pattern = PatternType.CANDY_SWIRL,
            price = 220,
            isUnlocked = false,
            glowColor = Color(0x66FF4081)
        ),
        Skin(
            id = "skin_gold_king",
            name = "Raja Emas",
            colors = listOf(Color(0xFFFFD700), Color(0xFFFFAB00), Color(0xFF212121)),
            pattern = PatternType.STRIPES,
            price = 300,
            isUnlocked = false,
            glowColor = Color(0x88FFD700)
        ),
        Skin(
            id = "skin_tiger",
            name = "Harimau Juara",
            colors = listOf(Color(0xFFFF6D00), Color(0xFF212121), Color(0xFFFF9E80)),
            pattern = PatternType.STRIPES,
            price = 320,
            isUnlocked = false,
            glowColor = Color(0x66FF6D00)
        ),
        Skin(
            id = "skin_arctic",
            name = "Es Kutub",
            colors = listOf(Color(0xFFE0F7FA), Color(0xFF40C4FF), Color(0xFF0288D1)),
            pattern = PatternType.GRADIENT,
            price = 350,
            isUnlocked = false,
            glowColor = Color(0x8840C4FF)
        ),
        Skin(
            id = "skin_cyber_neon",
            name = "Cyber Neon",
            colors = listOf(Color(0xFF00E5FF), Color(0xFFFF007F), Color(0xFF7C4DFF)),
            pattern = PatternType.NEON_GLOW,
            price = 400,
            isUnlocked = false,
            glowColor = Color(0x9900E5FF)
        ),
        Skin(
            id = "skin_dragon_fire",
            name = "Naga Api",
            colors = listOf(Color(0xFFFF3D00), Color(0xFFFF9100), Color(0xFFD50000)),
            pattern = PatternType.DRAGON_SCALES,
            price = 450,
            isUnlocked = false,
            glowColor = Color(0x88FF3D00)
        ),
        Skin(
            id = "skin_galaxy",
            name = "Galaksi Kosmik",
            colors = listOf(Color(0xFF311B92), Color(0xFF651FFF), Color(0xFF00E5FF)),
            pattern = PatternType.GRADIENT,
            price = 500,
            isUnlocked = false,
            glowColor = Color(0x88651FFF)
        ),
        Skin(
            id = "skin_diamond",
            name = "Sultan Berlian",
            colors = listOf(Color(0xFFE1F5FE), Color(0xFF81D4FA), Color(0xFF0288D1)),
            pattern = PatternType.POLKA_DOTS,
            price = 600,
            isUnlocked = false,
            glowColor = Color(0x9981D4FA)
        ),
        Skin(
            id = "skin_neon_phantom",
            name = "Neon Phantom",
            colors = listOf(Color(0xFF7C4DFF), Color(0xFF00E5FF), Color(0xFF18FFFF)),
            pattern = PatternType.NEON_GLOW,
            price = 380,
            isUnlocked = false,
            glowColor = Color(0x9900E5FF)
        ),
        Skin(
            id = "skin_lava_magma",
            name = "Lava Pijar",
            colors = listOf(Color(0xFFFF3D00), Color(0xFFFFAB00), Color(0xFF3E2723)),
            pattern = PatternType.GRADIENT,
            price = 420,
            isUnlocked = false,
            glowColor = Color(0x99FF3D00)
        ),
        Skin(
            id = "skin_pastel_mint",
            name = "Pastel Imut",
            colors = listOf(Color(0xFF80CBC4), Color(0xFFF48FB1), Color(0xFFFFF59D)),
            pattern = PatternType.CANDY_SWIRL,
            price = 260,
            isUnlocked = false,
            glowColor = Color(0x99F48FB1)
        ),
        Skin(
            id = "skin_gold_emperor",
            name = "Kaisar Emas",
            colors = listOf(Color(0xFFFFD700), Color(0xFFFF1744), Color(0xFFFFEA00)),
            pattern = PatternType.DRAGON_SCALES,
            price = 700,
            isUnlocked = false,
            glowColor = Color(0xAAFFD700)
        )
    )

    val HATS = listOf(
        Hat("hat_none", "Tanpa Topi", HatType.NONE, 0, "DEFAULT", isUnlocked = true),
        Hat("hat_crown", "Mahkota Raja", HatType.CROWN, 200, "ROYAL", isUnlocked = false),
        Hat("hat_shades", "Kacamata Hitam", HatType.SUNGLASSES, 150, "COOL", isUnlocked = false),
        Hat("hat_party", "Topi Pesta", HatType.PARTY_HAT, 120, "PARTY", isUnlocked = false),
        Hat("hat_cap", "Topi Baseball", HatType.BASEBALL_CAP, 140, "SPORT", isUnlocked = false),
        Hat("hat_cat", "Telinga Kucing", HatType.CAT_EARS, 220, "KAWAII", isUnlocked = false),
        Hat("hat_viking", "Tanduk Viking", HatType.VIKING_HORNS, 250, "WARRIOR", isUnlocked = false),
        Hat("hat_halo", "Cincin Malaikat", HatType.ANGEL_HALO, 280, "MYTHIC", isUnlocked = false),
        Hat("hat_pirate", "Topi Bajak Laut", HatType.PIRATE_HAT, 300, "PIRATE", isUnlocked = false),
        Hat("hat_ninja", "Ikat Kepala Ninja", HatType.NINJA_HEADBAND, 260, "SHINOBI", isUnlocked = false),
        Hat("hat_wizard", "Topi Penyihir", HatType.WIZARD_HAT, 320, "MAGIC", isUnlocked = false),
        Hat("hat_devil", "Tanduk Iblis", HatType.DEVIL_HORNS, 270, "DARK", isUnlocked = false),
        Hat("hat_cowboy", "Topi Koboi", HatType.COWBOY_HAT, 210, "WESTERN", isUnlocked = false),
        Hat("hat_chef", "Topi Koki", HatType.CHEF_HAT, 190, "MASTER", isUnlocked = false)
    )

    val FACES = listOf(
        Face("face_happy", "Senyum Ceria", FaceType.HAPPY_SMILE, 0, "HAPPY", isUnlocked = true),
        Face("face_open", "Makan Lahap", FaceType.OPEN_MOUTH, 120, "HUNGRY", isUnlocked = false),
        Face("face_tongue", "Julur Lidah", FaceType.SILLY_TONGUE, 150, "PLAYFUL", isUnlocked = false),
        Face("face_cool", "Keren Mantap", FaceType.COOL_GRIN, 180, "SMUG", isUnlocked = false)
    )

    val ROOMS = listOf(
        GameRoom("room_asia_1", "Asia VIP Arena #1", "Jakarta ID", 18, 48, 50, "POPULER"),
        GameRoom("room_global", "Global Champions Hall", "Singapore SG", 24, 45, 50, "HOT"),
        GameRoom("room_cyber", "Cyber Neon City", "Tokyo JP", 42, 38, 50, "EVENT"),
        GameRoom("room_candy", "Candyland Battle Royale", "Sydney AU", 62, 34, 50, "BARU")
    )

    val DAILY_REWARDS = listOf(
        DailyReward(1, "Hari 1", 200, null, null, "+200 Koin Sambutan"),
        DailyReward(2, "Hari 2", 300, null, null, "+300 Koin Bonus"),
        DailyReward(3, "Hari 3", 400, "skin_neon_phantom", null, "+400 Koin & Skin Neon Phantom"),
        DailyReward(4, "Hari 4", 500, null, null, "+500 Koin Pejuang"),
        DailyReward(5, "Hari 5", 600, null, "hat_ninja", "+600 Koin & Ikat Kepala Ninja"),
        DailyReward(6, "Hari 6", 800, null, null, "+800 Koin Petarung"),
        DailyReward(7, "Hari 7", 1500, "skin_gold_emperor", "hat_crown", "+1500 Koin & Skin Kaisar Emas!")
    )
}
