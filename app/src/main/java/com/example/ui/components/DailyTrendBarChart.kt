package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.Expense
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DaySpend(
    val dateLabel: String,
    val dayName: String,
    val amount: Double,
    val dayTimestamp: Long
)

@Composable
fun DailyTrendBarChart(
    expenses: List<Expense>,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    val dayFormatter = SimpleDateFormat("dd/MM", Locale.getDefault())
    val dayNameFormatter = SimpleDateFormat("EEE", Locale("id", "ID"))

    // Generate last 7 days buckets
    val dailyList = remember(expenses) {
        val calendar = Calendar.getInstance()
        val list = mutableListOf<DaySpend>()
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val dayEnd = cal.timeInMillis

            val sumForDay = expenses
                .filter { it.timestamp in dayStart..dayEnd }
                .sumOf { it.amount }

            list.add(
                DaySpend(
                    dateLabel = dayFormatter.format(Date(dayStart)),
                    dayName = dayNameFormatter.format(Date(dayStart)),
                    amount = sumForDay,
                    dayTimestamp = dayStart
                )
            )
        }
        list
    }

    val maxAmount = remember(dailyList) {
        (dailyList.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(10000.0)
    }

    var selectedDay by remember { mutableStateOf<DaySpend?>(null) }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(expenses) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tren Pengeluaran 7 Hari",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (selectedDay != null) {
                Text(
                    text = "${selectedDay?.dayName}: ${currencyFormat.format(selectedDay?.amount ?: 0.0)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            } else {
                Text(
                    text = "Ketuk bar untuk detail",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(dailyList) {
                        detectTapGestures { offset ->
                            val slotWidth = size.width / dailyList.size
                            val index = (offset.x / slotWidth).toInt().coerceIn(0, dailyList.size - 1)
                            selectedDay = dailyList[index]
                        }
                    }
            ) {
                val barCount = dailyList.size
                val slotWidth = size.width / barCount
                val barWidth = slotWidth * 0.55f
                val chartHeight = size.height - 35.dp.toPx()

                // Baseline line
                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = Offset(0f, chartHeight),
                    end = Offset(size.width, chartHeight),
                    strokeWidth = 1.dp.toPx()
                )

                dailyList.forEachIndexed { i, day ->
                    val barHeight = ((day.amount / maxAmount) * chartHeight * animProgress.value).toFloat()
                    val xPos = i * slotWidth + (slotWidth - barWidth) / 2
                    val yPos = chartHeight - barHeight

                    val isSelected = selectedDay?.dayTimestamp == day.dayTimestamp
                    val isPeak = day.amount == maxAmount && day.amount > 0

                    val barBrush = Brush.verticalGradient(
                        colors = if (isSelected) {
                            listOf(Color(0xFF00CEC9), EmeraldPrimary)
                        } else if (isPeak) {
                            listOf(Color(0xFFFF7675), Color(0xFFD63031))
                        } else {
                            listOf(EmeraldLight, EmeraldPrimary)
                        }
                    )

                    // Draw bar
                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(xPos, yPos.coerceAtLeast(0f)),
                        size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx())),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }
            }

            // Day labels under bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dailyList.forEach { day ->
                    val isSelected = selectedDay?.dayTimestamp == day.dayTimestamp
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = day.dayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = day.dateLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
