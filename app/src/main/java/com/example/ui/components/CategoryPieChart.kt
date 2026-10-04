package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.Expense
import com.example.ui.theme.BillsCategoryColor
import com.example.ui.theme.EntertainmentCategoryColor
import com.example.ui.theme.FoodCategoryColor
import com.example.ui.theme.HealthCategoryColor
import com.example.ui.theme.OtherCategoryColor
import com.example.ui.theme.ShoppingCategoryColor
import com.example.ui.theme.TransportCategoryColor
import java.text.NumberFormat
import java.util.Locale

fun getCategoryColor(category: String): Color {
    return when (category.lowercase(Locale.ROOT)) {
        "makanan", "kuliner", "food" -> FoodCategoryColor
        "transportasi", "bensin", "transport" -> TransportCategoryColor
        "hiburan", "kafe", "entertainment", "kopi" -> EntertainmentCategoryColor
        "belanja", "shopping", "mart" -> ShoppingCategoryColor
        "tagihan", "listrik", "bills" -> BillsCategoryColor
        "kesehatan", "obat", "health" -> HealthCategoryColor
        else -> OtherCategoryColor
    }
}

data class CategorySlice(
    val category: String,
    val totalAmount: Double,
    val percentage: Float,
    val color: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPieChart(
    expenses: List<Expense>,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    if (expenses.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Belum ada transaksi bulan ini",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val totalSpent = expenses.sumOf { it.amount }
    val grouped = expenses.groupBy { it.category }
    val slices = remember(expenses) {
        grouped.map { (cat, list) ->
            val sum = list.sumOf { it.amount }
            val pct = if (totalSpent > 0) (sum / totalSpent).toFloat() else 0f
            CategorySlice(cat, sum, pct, getCategoryColor(cat))
        }.sortedByDescending { it.totalAmount }
    }

    var selectedSlice by remember { mutableStateOf<CategorySlice?>(null) }
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Komposisi Pengeluaran per Kategori",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Donut Chart Canvas
        Box(
            modifier = Modifier.size(190.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(170.dp)) {
                val strokeWidth = 28.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = Offset(size.width / 2, size.height / 2)

                var currentStartAngle = -90f

                slices.forEach { slice ->
                    val sweepAngle = slice.percentage * 360f * animProgress.value
                    val isSelected = selectedSlice?.category == slice.category

                    drawArc(
                        color = slice.color,
                        startAngle = currentStartAngle,
                        sweepAngle = sweepAngle.coerceAtLeast(1f),
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(
                            width = if (isSelected) strokeWidth * 1.25f else strokeWidth,
                            cap = StrokeCap.Round
                        )
                    )
                    currentStartAngle += sweepAngle
                }
            }

            // Center Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                val active = selectedSlice ?: slices.firstOrNull()
                if (active != null) {
                    Text(
                        text = active.category,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = active.color
                    )
                    Text(
                        text = "${(active.percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currencyFormat.format(active.totalAmount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive Category Chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slices.forEach { slice ->
                val isSelected = selectedSlice?.category == slice.category
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) slice.color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                    tonalElevation = if (isSelected) 4.dp else 1.dp,
                    modifier = Modifier
                        .clickable {
                            selectedSlice = if (selectedSlice?.category == slice.category) null else slice
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(slice.color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = slice.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${(slice.percentage * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
