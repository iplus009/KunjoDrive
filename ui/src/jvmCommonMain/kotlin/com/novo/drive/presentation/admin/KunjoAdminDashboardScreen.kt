package com.novo.drive.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novo.drive.core.network.CountryCount
import com.novo.drive.core.network.SupabaseRestClient
import com.novo.drive.core.network.UserCountSummary
import com.novo.drive.presentation.components.glass.liquidGlass
import com.novo.drive.presentation.components.glass.liquidGlassBackground
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KunjoAdminDashboardScreen(
    onBack: () -> Unit
) {
    var counts by remember { mutableStateOf(UserCountSummary(0, 0, 0, isUnavailable = true)) }
    var topCountries by remember { mutableStateOf<List<CountryCount>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val isBn = com.novo.drive.presentation.common.isBengaliUi()

    val accentCyan = Color(0xFF00E5FF)
    val luxuryGold = Color(0xFFFFD54F)
    val electricPurple = Color(0xFFBB86FC)

    fun refresh() {
        scope.launch {
            isLoading = true
            counts = SupabaseRestClient.fetchUserCounts()
            topCountries = SupabaseRestClient.fetchTopCountries()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = luxuryGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin Insights & Telemetry",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refresh() }, enabled = !isLoading) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = luxuryGold,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = accentCyan)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = if (isDark) Color(0xFF090C12) else Color(0xFFF1F5F9)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = if (isBn) "লাইভ অ্যাকাউন্ট ট্র্যাকিং (Live Accounts)" else "Live Account Tracking",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Big Metric Card (Total Unique Users)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlassBackground(shape = RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.padding(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(accentCyan.copy(alpha = 0.18f))
                            .border(1.dp, accentCyan.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = accentCyan, modifier = Modifier.size(28.dp))
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    Column {
                        Text(
                            text = if (isBn) "মোট ইউনিক অ্যাকাউন্ট" else "Total Unique Accounts",
                            fontSize = 13.sp,
                            color = if (isDark) Color.LightGray else Color(0xFF64748B)
                        )
                        Text(
                            text = if (counts.isUnavailable) {
                                if (isBn) "সার্ভার অসংযুক্ত" else "Server Unavailable"
                            } else {
                                if (isBn) "${counts.totalUsers} জন" else "${counts.totalUsers} users"
                            },
                            fontSize = if (counts.isUnavailable) 20.sp else 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isDark) Color.White else Color.Black
                        )
                        Text(
                            text = if (counts.isUnavailable) {
                                if (isBn) {
                                    "⚠️ Supabase কনফিগার বা ইন্টারনেট সংযোগ যাচাই করুন"
                                } else {
                                    "⚠️ Check Supabase configuration or internet connection"
                                }
                            } else {
                                if (isBn) {
                                    "✅ লাইভ সার্ভার থেকে প্রাপ্ত (No duplicate devices)"
                                } else {
                                    "✅ Synced from live server (No duplicate devices)"
                                }
                            },
                            fontSize = 11.sp,
                            color = if (counts.isUnavailable) Color(0xFFFFB300) else Color(0xFF00E676)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Split Cards: Main vs Sub accounts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Main Accounts
                TelemetryCard(
                    modifier = Modifier.weight(1f),
                    title = if (isBn) "👑 মেইন অ্যাকাউন্ট" else "👑 Main Accounts",
                    subtitle = if (isBn) "ক্লাউড ড্রাইভ হোস্ট" else "Cloud Drive Hosts",
                    count = counts.mainUsers,
                    accentColor = luxuryGold,
                    icon = Icons.Default.Person,
                    isDark = isDark
                )

                // Sub Accounts
                TelemetryCard(
                    modifier = Modifier.weight(1f),
                    title = if (isBn) "👨‍👩‍👧 সাব অ্যাকাউন্ট" else "👨‍👩‍👧 Sub Accounts",
                    subtitle = if (isBn) "ফ্যামিলি মেম্বার" else "Family Members",
                    count = counts.subUsers,
                    accentColor = electricPurple,
                    icon = Icons.Default.FamilyRestroom,
                    isDark = isDark
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Geolocation Section
            Text(
                text = if (isBn) "জিও-লোকেশন ও টেলিমেট্রি (Live Geolocation)" else "Live Geolocation & Telemetry",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(shape = RoundedCornerShape(20.dp))
                    .liquidGlassBackground(shape = RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = accentCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isBn) "টপ কান্ট্রি ও শহর" else "Top Countries & Cities", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (counts.isUnavailable || topCountries.isEmpty()) {
                        LocationRow(
                            if (counts.isUnavailable) {
                                if (isBn) "ক্লাউড টেলিমেট্রি অনুপলব্ধ (Offline)" else "Cloud telemetry unavailable (Offline)"
                            } else {
                                if (isBn) "কোনো জিও-লোকেশন ডেটা নেই" else "No geolocation data yet"
                            },
                            "N/A",
                            isDark
                        )
                    } else {
                        val denominator = topCountries.sumOf { it.count }.coerceAtLeast(1)
                        topCountries.forEach { entry ->
                            val pct = (entry.count * 100) / denominator
                            LocationRow("${entry.country} (${entry.count})", "$pct%", isDark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    count: Int,
    accentColor: Color,
    icon: ImageVector,
    isDark: Boolean
) {
    val isBn = com.novo.drive.presentation.common.isBengaliUi()
    Card(
        modifier = modifier
            .liquidGlass(shape = RoundedCornerShape(18.dp))
            .liquidGlassBackground(shape = RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accentColor)
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isBn) "$count জন" else "$count users",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = if (isDark) Color.White else Color.Black
            )
            Text(text = subtitle, fontSize = 11.sp, color = if (isDark) Color.Gray else Color(0xFF64748B))
        }
    }
}

@Composable
private fun LocationRow(name: String, percentage: String, isDark: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = name, fontSize = 13.sp, color = if (isDark) Color.LightGray else Color(0xFF334155))
        Text(text = percentage, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
    }
}
