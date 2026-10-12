package com.example.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*
import com.example.game.viewmodel.AppScreen
import com.example.game.viewmodel.GameViewModel
import kotlin.math.sin

@Composable
fun ShopScreen(viewModel: GameViewModel) {
    val coins by viewModel.coins.collectAsState()
    val selectedSkin by viewModel.selectedSkin.collectAsState()
    val selectedHat by viewModel.selectedHat.collectAsState()
    val selectedFace by viewModel.selectedFace.collectAsState()

    var activeTab by remember { mutableStateOf("SKINS") } // SKINS, HATS, FACES
    var previewSkin by remember { mutableStateOf(selectedSkin) }
    var previewHat by remember { mutableStateOf(selectedHat) }
    var previewFace by remember { mutableStateOf(selectedFace) }

    BackHandler {
        viewModel.setScreen(AppScreen.MAIN_MENU)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C101A))
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: Title, Back button, and Coins balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.setScreen(AppScreen.MAIN_MENU) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E283D))
                            .testTag("shop_back_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "LEMARI KOSTUM & SKIN",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD700)
                        )
                        Text(
                            text = "Kustomisasi tampilan cacing favoritmu",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // Coins balance & Claim Bonus Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { viewModel.addBonusCoins(300) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(36.dp).testTag("claim_bonus_coins")
                    ) {
                        Icon(imageVector = Icons.Default.CardGiftcard, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+300 Koin", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1E283D))
                            .border(1.5.dp, Color(0xFFFFD700), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$coins Koin",
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Content: Left Preview Card & Right Items Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // LEFT: Live Preview Card
                Card(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161E2E))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "PREVIEW CACING ANDA",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )

                        // Circular Canvas Preview
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0B101D))
                                .border(2.dp, Color(0x333F51B5), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            ShopPreviewCanvas(skin = previewSkin, hat = previewHat, face = previewFace)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = previewSkin.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (previewHat.type != HatType.NONE) {
                                Text(
                                    text = "+ ${previewHat.name}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFFD700)
                                )
                            }
                            Text(
                                text = "Wajah: ${previewFace.name}",
                                fontSize = 11.sp,
                                color = Color(0xFF00E676)
                            )
                        }

                        // Apply or Buy button based on active tab
                        when (activeTab) {
                            "SKINS" -> {
                                val isUnlocked = viewModel.isSkinUnlocked(previewSkin.id)
                                val isEquipped = selectedSkin.id == previewSkin.id

                                if (isEquipped) {
                                    Button(
                                        onClick = {},
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF00E676))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sedang Dipakai", color = Color.White)
                                    }
                                } else if (isUnlocked) {
                                    Button(
                                        onClick = { viewModel.selectSkin(previewSkin) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                        modifier = Modifier.fillMaxWidth().testTag("equip_skin_button")
                                    ) {
                                        Text("Pakai Skin Ini", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.buySkin(previewSkin) },
                                        enabled = coins >= previewSkin.price,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                        modifier = Modifier.fillMaxWidth().testTag("buy_skin_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Beli (${previewSkin.price})", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            "HATS" -> {
                                val isUnlocked = viewModel.isHatUnlocked(previewHat.id)
                                val isEquipped = selectedHat.id == previewHat.id

                                if (isEquipped) {
                                    Button(
                                        onClick = {},
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF00E676))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sedang Dipakai", color = Color.White)
                                    }
                                } else if (isUnlocked) {
                                    Button(
                                        onClick = { viewModel.selectHat(previewHat) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                        modifier = Modifier.fillMaxWidth().testTag("equip_hat_button")
                                    ) {
                                        Text("Pakai Topi Ini", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.buyHat(previewHat) },
                                        enabled = coins >= previewHat.price,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                        modifier = Modifier.fillMaxWidth().testTag("buy_hat_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Beli (${previewHat.price})", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            else -> {
                                val isUnlocked = viewModel.isFaceUnlocked(previewFace.id)
                                val isEquipped = selectedFace.id == previewFace.id

                                if (isEquipped) {
                                    Button(
                                        onClick = {},
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF00E676))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sedang Dipakai", color = Color.White)
                                    }
                                } else if (isUnlocked) {
                                    Button(
                                        onClick = { viewModel.selectFace(previewFace) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                        modifier = Modifier.fillMaxWidth().testTag("equip_face_button")
                                    ) {
                                        Text("Pakai Wajah Ini", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.buyFace(previewFace) },
                                        enabled = coins >= previewFace.price,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                        modifier = Modifier.fillMaxWidth().testTag("buy_face_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Beli (${previewFace.price})", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // RIGHT: Tabs + Grid of Items
                Column(modifier = Modifier.weight(1f)) {
                    // Modern Tab Selector with clean vector icons (No Emojis!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF161E2E))
                            .padding(4.dp)
                    ) {
                        TabButton(
                            title = "Skin Tubuh (${Catalog.SKINS.size})",
                            icon = Icons.Default.Palette,
                            isSelected = activeTab == "SKINS",
                            onClick = { activeTab = "SKINS" },
                            modifier = Modifier.weight(1f)
                        )
                        TabButton(
                            title = "Topi & Aksesoris (${Catalog.HATS.size})",
                            icon = Icons.Default.Diamond,
                            isSelected = activeTab == "HATS",
                            onClick = { activeTab = "HATS" },
                            modifier = Modifier.weight(1f)
                        )
                        TabButton(
                            title = "Ekspresi Wajah (${Catalog.FACES.size})",
                            icon = Icons.Default.Mood,
                            isSelected = activeTab == "FACES",
                            onClick = { activeTab = "FACES" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    when (activeTab) {
                        "SKINS" -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(Catalog.SKINS) { skin ->
                                    val isUnlocked = viewModel.isSkinUnlocked(skin.id)
                                    val isEquipped = selectedSkin.id == skin.id
                                    val isCurrentPreview = previewSkin.id == skin.id

                                    SkinItemCard(
                                        skin = skin,
                                        isUnlocked = isUnlocked,
                                        isEquipped = isEquipped,
                                        isSelected = isCurrentPreview,
                                        onClick = {
                                            previewSkin = skin
                                        }
                                    )
                                }
                            }
                        }
                        "HATS" -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(Catalog.HATS) { hat ->
                                    val isUnlocked = viewModel.isHatUnlocked(hat.id)
                                    val isEquipped = selectedHat.id == hat.id
                                    val isCurrentPreview = previewHat.id == hat.id

                                    HatItemCard(
                                        hat = hat,
                                        isUnlocked = isUnlocked,
                                        isEquipped = isEquipped,
                                        isSelected = isCurrentPreview,
                                        onClick = {
                                            previewHat = hat
                                        }
                                    )
                                }
                            }
                        }
                        else -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(Catalog.FACES) { face ->
                                    val isUnlocked = viewModel.isFaceUnlocked(face.id)
                                    val isEquipped = selectedFace.id == face.id
                                    val isCurrentPreview = previewFace.id == face.id

                                    FaceItemCard(
                                        face = face,
                                        isUnlocked = isUnlocked,
                                        isEquipped = isEquipped,
                                        isSelected = isCurrentPreview,
                                        onClick = {
                                            previewFace = face
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF651FFF) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SkinItemCard(
    skin: Skin,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF00E5FF) else if (isEquipped) Color(0xFF00E676) else Color(0x22FFFFFF),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E283D) else Color(0xFF141A29)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Color sample swatches
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                horizontalArrangement = Arrangement.Center
            ) {
                skin.colors.forEach { c ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(c)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = skin.name,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (isEquipped) {
                Text("Dipakai", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            } else if (isUnlocked) {
                Text("Tersedia", color = Color(0xFF00E5FF), fontSize = 11.sp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("${skin.price}", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HatItemCard(
    hat: Hat,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF00E5FF) else if (isEquipped) Color(0xFF00E676) else Color(0x22FFFFFF),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E283D) else Color(0xFF141A29)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Modern vector icon representation for each hat (NO EMOJIS)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222B40)),
                contentAlignment = Alignment.Center
            ) {
                val hatIcon = when (hat.type) {
                    HatType.CROWN -> Icons.Default.EmojiEvents
                    HatType.SUNGLASSES -> Icons.Default.Visibility
                    HatType.PARTY_HAT -> Icons.Default.Celebration
                    HatType.BASEBALL_CAP -> Icons.Default.SportsBaseball
                    HatType.CAT_EARS -> Icons.Default.Pets
                    HatType.VIKING_HORNS -> Icons.Default.Shield
                    HatType.ANGEL_HALO -> Icons.Default.Brightness7
                    HatType.PIRATE_HAT -> Icons.Default.Explore
                    HatType.NINJA_HEADBAND -> Icons.Default.SportsKabaddi
                    HatType.WIZARD_HAT -> Icons.Default.AutoAwesome
                    HatType.DEVIL_HORNS -> Icons.Default.Whatshot
                    HatType.COWBOY_HAT -> Icons.Default.Landscape
                    HatType.CHEF_HAT -> Icons.Default.Restaurant
                    HatType.NONE -> Icons.Default.Block
                }
                val iconTint = when (hat.type) {
                    HatType.CROWN -> Color(0xFFFFD700)
                    HatType.SUNGLASSES -> Color(0xFF00E5FF)
                    HatType.PARTY_HAT -> Color(0xFFFF4081)
                    HatType.CAT_EARS -> Color(0xFFFF80AB)
                    HatType.ANGEL_HALO -> Color(0xFFFFEB3B)
                    HatType.NINJA_HEADBAND -> Color(0xFFFF1744)
                    HatType.WIZARD_HAT -> Color(0xFF7C4DFF)
                    HatType.DEVIL_HORNS -> Color(0xFFFF3D00)
                    HatType.COWBOY_HAT -> Color(0xFFD7CCC8)
                    HatType.CHEF_HAT -> Color(0xFFFFFFFF)
                    HatType.NONE -> Color.Gray
                    else -> Color.White
                }
                Icon(
                    imageVector = hatIcon,
                    contentDescription = hat.name,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = hat.name,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (isEquipped) {
                Text("Dipakai", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            } else if (isUnlocked) {
                Text("Tersedia", color = Color(0xFF00E5FF), fontSize = 11.sp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("${hat.price}", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FaceItemCard(
    face: Face,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF00E5FF) else if (isEquipped) Color(0xFF00E676) else Color(0x22FFFFFF),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E283D) else Color(0xFF141A29)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Modern Canvas face expression preview (NO EMOJIS)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222B40)),
                contentAlignment = Alignment.Center
            ) {
                val faceIcon = when (face.type) {
                    FaceType.HAPPY_SMILE -> Icons.Default.SentimentVerySatisfied
                    FaceType.OPEN_MOUTH -> Icons.Default.Mood
                    FaceType.SILLY_TONGUE -> Icons.Default.SentimentSatisfiedAlt
                    FaceType.COOL_GRIN -> Icons.Default.TagFaces
                }
                Icon(
                    imageVector = faceIcon,
                    contentDescription = face.name,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(face.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))

            if (isEquipped) {
                Text("Dipakai", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            } else if (isUnlocked) {
                Text("Tersedia", color = Color(0xFF00E5FF), fontSize = 11.sp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("${face.price}", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ShopPreviewCanvas(skin: Skin, hat: Hat, face: Face) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val colors = skin.colors

        // S-curve preview segments
        val segCount = 10
        val segRadius = 18f
        for (i in segCount downTo 1) {
            val progress = i.toFloat() / segCount
            val x = center.x - (progress * 55f)
            val y = center.y + sin(progress * 3.14f * 1.5f) * 35f
            val color = colors[i % colors.size]

            drawCircle(color = Color(0xFF111111), radius = segRadius + 2f, center = Offset(x, y))
            drawCircle(color = color, radius = segRadius, center = Offset(x, y))
            drawCircle(color = Color.White.copy(alpha = 0.4f), radius = segRadius * 0.5f, center = Offset(x - 4f, y - 4f))
        }

        // Head
        val headRadius = segRadius * 1.35f
        drawCircle(color = Color(0xFF111111), radius = headRadius + 2.5f, center = center)
        drawCircle(color = colors.first(), radius = headRadius, center = center)
        drawCircle(color = Color.White.copy(alpha = 0.4f), radius = headRadius * 0.5f, center = center - Offset(headRadius * 0.25f, headRadius * 0.25f))

        // Big cartoon eyes
        val eyeRadius = headRadius * 0.42f
        val pupilRadius = eyeRadius * 0.52f
        val leftEye = center + Offset(-headRadius * 0.35f, -headRadius * 0.25f)
        val rightEye = center + Offset(headRadius * 0.35f, -headRadius * 0.25f)

        drawCircle(color = Color(0xFF121214), radius = eyeRadius + 1.5f, center = leftEye)
        drawCircle(color = Color.White, radius = eyeRadius, center = leftEye)
        drawCircle(color = Color(0xFF121214), radius = eyeRadius + 1.5f, center = rightEye)
        drawCircle(color = Color.White, radius = eyeRadius, center = rightEye)
        drawCircle(color = Color(0xFF121214), radius = pupilRadius, center = leftEye)
        drawCircle(color = Color(0xFF121214), radius = pupilRadius, center = rightEye)
        drawCircle(color = Color.White, radius = pupilRadius * 0.4f, center = leftEye - Offset(2f, 2f))
        drawCircle(color = Color.White, radius = pupilRadius * 0.4f, center = rightEye - Offset(2f, 2f))

        // Cheeks
        drawCircle(color = Color(0x66FF4081), radius = 5f, center = center + Offset(-headRadius * 0.5f, headRadius * 0.2f))
        drawCircle(color = Color(0x66FF4081), radius = 5f, center = center + Offset(headRadius * 0.5f, headRadius * 0.2f))

        // Mouth
        if (face.type == FaceType.OPEN_MOUTH) {
            drawCircle(color = Color(0xFF880E4F), radius = headRadius * 0.3f, center = center + Offset(0f, headRadius * 0.4f))
            drawCircle(color = Color(0xFFFF4081), radius = headRadius * 0.18f, center = center + Offset(0f, headRadius * 0.5f))
        } else {
            drawArc(
                color = Color(0xFF1E1E24),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = center - Offset(headRadius * 0.35f, -headRadius * 0.1f),
                size = androidx.compose.ui.geometry.Size(headRadius * 0.7f, headRadius * 0.5f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )
        }

        // Hat on preview
        val hatCenter = center - Offset(0f, headRadius * 0.95f)
        when (hat.type) {
            HatType.CROWN -> {
                drawCircle(color = Color(0xFFFFD700), radius = 10f, center = hatCenter)
            }
            HatType.SUNGLASSES -> {
                drawRoundRect(
                    color = Color(0xFF111111),
                    topLeft = center - Offset(headRadius * 0.45f, headRadius * 0.25f),
                    size = androidx.compose.ui.geometry.Size(headRadius * 0.9f, headRadius * 0.35f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                )
            }
            HatType.NINJA_HEADBAND -> {
                drawRoundRect(
                    color = Color(0xFFFF1744),
                    topLeft = center - Offset(headRadius * 0.55f, headRadius * 0.55f),
                    size = androidx.compose.ui.geometry.Size(headRadius * 1.1f, headRadius * 0.28f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                )
            }
            HatType.WIZARD_HAT -> {
                drawCircle(color = Color(0xFF311B92), radius = 11f, center = hatCenter)
                drawCircle(color = Color(0xFFFFD700), radius = 3.5f, center = hatCenter)
            }
            HatType.DEVIL_HORNS -> {
                drawCircle(color = Color(0xFFFF1744), radius = 5f, center = hatCenter - Offset(headRadius * 0.4f, 0f))
                drawCircle(color = Color(0xFFFF1744), radius = 5f, center = hatCenter + Offset(headRadius * 0.4f, 0f))
            }
            HatType.PARTY_HAT -> {
                drawCircle(color = Color(0xFFFF4081), radius = 9f, center = hatCenter)
            }
            HatType.CAT_EARS -> {
                drawCircle(color = Color(0xFF212121), radius = 6f, center = hatCenter - Offset(headRadius * 0.35f, 0f))
                drawCircle(color = Color(0xFF212121), radius = 6f, center = hatCenter + Offset(headRadius * 0.35f, 0f))
            }
            HatType.ANGEL_HALO -> {
                drawOval(
                    color = Color(0xFFFFD700),
                    topLeft = hatCenter - Offset(headRadius * 0.5f, headRadius * 0.15f),
                    size = androidx.compose.ui.geometry.Size(headRadius * 1.0f, headRadius * 0.35f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                )
            }
            else -> {}
        }
    }
}
