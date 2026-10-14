package org.warifu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.warifu.ui.components.ServiceAvatar
import org.warifu.ui.theme.WarifuBackground
import org.warifu.ui.theme.WarifuBorder
import org.warifu.ui.theme.WarifuSurface
import org.warifu.ui.theme.WarifuSurfaceElevated
import org.warifu.ui.theme.WarifuTextSecondary

data class PredefinedService(
    val name: String,
    val category: String = "Popular"
)

private val POPULAR_SERVICES = listOf(
    PredefinedService("Google"),
    PredefinedService("GitHub"),
    PredefinedService("Microsoft"),
    PredefinedService("Discord"),
    PredefinedService("Binance"),
    PredefinedService("Solana")
)

private val OTHER_SERVICES = listOf(
    PredefinedService("Facebook", "Other"),
    PredefinedService("Instagram", "Other"),
    PredefinedService("Amazon", "Other"),
    PredefinedService("Twitter / X", "Other"),
    PredefinedService("Dropbox", "Other"),
    PredefinedService("Proton", "Other"),
    PredefinedService("Reddit", "Other"),
    PredefinedService("Twitch", "Other")
)

/**
 * Screen 4 from ui.png: Add Account Screen.
 * Dual top action cards (Scan QR & Manual Key) + popular preset services & search.
 */
@Composable
fun AddAccountScreen(
    onBack: () -> Unit,
    onScanQrClicked: () -> Unit,
    onEnterKeyManuallyClicked: (prefilledIssuer: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredPopular = remember(searchQuery) {
        if (searchQuery.isBlank()) POPULAR_SERVICES
        else POPULAR_SERVICES.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    val filteredOthers = remember(searchQuery) {
        if (searchQuery.isBlank()) OTHER_SERVICES
        else OTHER_SERVICES.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = WarifuBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Add account",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Dual Action Cards: Scan QR code & Enter key manually
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionChoiceCard(
                            title = "Scan QR code",
                            subtitle = "Most common",
                            icon = Icons.Outlined.QrCodeScanner,
                            onClick = onScanQrClicked,
                            modifier = Modifier.weight(1f)
                        )

                        ActionChoiceCard(
                            title = "Enter key manually",
                            subtitle = "For advanced users",
                            icon = Icons.Outlined.Key,
                            onClick = { onEnterKeyManuallyClicked(null) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Popular services section
                if (filteredPopular.isNotEmpty()) {
                    item {
                        Text(
                            text = "Popular services",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = WarifuTextSecondary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(filteredPopular) { service ->
                        ServiceRowItem(
                            service = service,
                            onClick = { onEnterKeyManuallyClicked(service.name) }
                        )
                    }
                }

                // Others section with search bar
                item {
                    Text(
                        text = "Others",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = WarifuTextSecondary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )

                    // Search input
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(WarifuSurface)
                            .border(1.dp, WarifuBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = WarifuTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 14.sp
                                ),
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search for a service...",
                                            color = WarifuTextSecondary,
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }
                }

                items(filteredOthers) { service ->
                    ServiceRowItem(
                        service = service,
                        onClick = { onEnterKeyManuallyClicked(service.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionChoiceCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(WarifuSurface)
            .border(1.dp, WarifuBorder, shape)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = WarifuTextSecondary
            )
        }
    }
}

@Composable
private fun ServiceRowItem(
    service: PredefinedService,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WarifuSurface)
            .border(1.dp, WarifuBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ServiceAvatar(
            issuer = service.name,
            accountName = "",
            size = 36.dp
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = service.name,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(18.dp)
        )
    }
}
