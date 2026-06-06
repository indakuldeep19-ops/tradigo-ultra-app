package com.tradigo.ultra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradigo.ultra.ui.components.*
import com.tradigo.ultra.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingDashboard(
    onCallTrade: (String, Double) -> Unit = { _, _ -> },
    onPutTrade: (String, Double) -> Unit = { _, _ -> }
) {
    var selectedAsset by remember { mutableStateOf("BTC/USDT") }
    var livePrice by remember { mutableDoubleStateOf(66542.12) }

    val assets = listOf("BTC/USDT", "ETH/USDT", "SOL/USDT", "BNB/USDT", "XRP/USDT")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tradigo Ultra Pro",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundTopBar
                ),
                actions = {
                    // Notification bell
                    Text("🔔", fontSize = 20.sp, modifier = Modifier.padding(end = 16.dp))
                }
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Volatility Banner
            VolatilityHeatBanner(symbol = selectedAsset, price = livePrice)

            // Asset Selector
            AssetSelector(
                assets = assets,
                selected = selectedAsset,
                onSelect = { selectedAsset = it }
            )

            // Price Ticker
            PriceTickerCard(symbol = selectedAsset, price = livePrice)

            // Daily Streak
            DailyStreakCard()

            // AI Confidence
            var confidence by remember { mutableIntStateOf(0) }
            var signal by remember { mutableStateOf("WAIT") }
            
            ConfidenceGaugeCard(
                signal = signal,
                confidence = confidence,
                onGenerate = {
                    confidence = (55..96).random()
                    signal = if (confidence >= 75) listOf("CALL", "PUT").random() else "WAIT"
                }
            )

            // Candlestick Chart
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070913))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "OHLC | EMA Line | Pinch to Zoom",
                        color = CyanAccent,
                        fontSize = 11.sp
                    )
                    // InteractiveCandlestickChart would go here
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFF070913)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📊 Interactive Chart", color = TextSecondary)
                    }
                }
            }

            // Trade Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(
                    onClick = { onCallTrade(selectedAsset, livePrice) },
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreen
                    )
                ) {
                    Text(
                        "CALL ▲",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = { onPutTrade(selectedAsset, livePrice) },
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed
                    )
                ) {
                    Text(
                        "PUT ▼",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceTickerCard(symbol: String, price: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = symbol,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = "$${String.format("%,.2f", price)}",
                    color = SuccessGreen,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Surface(
                color = SuccessGreen.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "● LIVE",
                    color = SuccessGreen,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun AssetSelector(
    assets: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = assets.indexOf(selected).coerceAtLeast(0),
        containerColor = BackgroundSurface,
        contentColor = TextPrimary,
        edgePadding = 0.dp
    ) {
        assets.forEach { asset ->
            Tab(
                selected = asset == selected,
                onClick = { onSelect(asset) },
                text = {
                    Text(
                        text = asset,
                        fontSize = 13.sp,
                        fontWeight = if (asset == selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}
