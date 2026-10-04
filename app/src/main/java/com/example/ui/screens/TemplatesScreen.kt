package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.QuickTemplate
import com.example.ui.components.getCategoryColor
import com.example.ui.components.getTemplateIcon
import com.example.ui.theme.EmeraldPrimary
import java.text.NumberFormat

@Composable
fun TemplatesScreen(
    templates: List<QuickTemplate>,
    currencyFormat: NumberFormat,
    onSaveTemplate: (QuickTemplate) -> Unit,
    onDeleteTemplate: (QuickTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var editingTemplate by remember { mutableStateOf<QuickTemplate?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    fun pinShortcutToHomeScreen(template: QuickTemplate) {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            val shortcutIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("shortcut_title", template.name)
                putExtra("shortcut_amount", template.defaultAmount.toString())
                putExtra("shortcut_category", template.category)
            }

            val pinShortcutInfo = ShortcutInfoCompat.Builder(context, "shortcut_${template.id}")
                .setShortLabel(template.name)
                .setLongLabel("Catat Kilat: ${template.name}")
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(shortcutIntent)
                .build()

            ShortcutManagerCompat.requestPinShortcut(context, pinShortcutInfo, null)
            Toast.makeText(context, "Shortcut ${template.name} ditambahkan ke Home Screen!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Launcher HP ini belum mendukung Pin Shortcut", Toast.LENGTH_SHORT).show()
        }
    }

    if (isCreatingNew || editingTemplate != null) {
        TemplateEditDialog(
            template = editingTemplate ?: QuickTemplate(name = "", category = "Makanan", defaultAmount = 25000.0),
            isNew = isCreatingNew,
            onDismiss = {
                isCreatingNew = false
                editingTemplate = null
            },
            onSave = { updated ->
                onSaveTemplate(updated)
                isCreatingNew = false
                editingTemplate = null
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Kelola Shortcut ⚡",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Atur tombol cepat & integrasikan ke OS",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { isCreatingNew = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.testTag("add_new_shortcut_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Info Banner about Android OS Shortcuts
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Tip OS Shortcuts: Tekan lama ikon aplikasi di Home Screen untuk mengakses pintasan kilat, atau sematkan shortcut langsung ke layar HP!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // List of Routine Templates
        items(templates, key = { it.id }) { template ->
            val catColor = getCategoryColor(template.category)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("template_card_${template.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(catColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getTemplateIcon(template.iconKey),
                                contentDescription = template.name,
                                tint = catColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = template.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = template.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = catColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = " • Standar: ${currencyFormat.format(template.defaultAmount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pin to Home Screen
                        IconButton(
                            onClick = { pinShortcutToHomeScreen(template) },
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("pin_shortcut_${template.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Pin ke Home Screen",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Edit
                        IconButton(
                            onClick = { editingTemplate = template },
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("edit_template_${template.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Delete
                        IconButton(
                            onClick = { onDeleteTemplate(template) },
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_template_${template.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun TemplateEditDialog(
    template: QuickTemplate,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (QuickTemplate) -> Unit
) {
    var name by remember { mutableStateOf(template.name) }
    var category by remember { mutableStateOf(template.category) }
    var amountStr by remember { mutableStateOf(if (template.defaultAmount > 0) template.defaultAmount.toLong().toString() else "25000") }
    var iconKey by remember { mutableStateOf(template.iconKey) }

    val categories = listOf("Makanan", "Transportasi", "Hiburan", "Belanja", "Tagihan", "Lainnya")
    val icons = listOf("food" to "Makan", "fuel" to "Bensin", "coffee" to "Kopi", "parking" to "Parkir", "groceries" to "Belanja", "electric" to "Listrik")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isNew) "Tambah Shortcut Baru" else "Edit Shortcut", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Pintasan (misal: Bensin)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("template_name_input")
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Nominal Standar (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("template_amount_input")
                )

                Text("Kategori:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Text("Ikon:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(icons) { (key, label) ->
                        val isSelected = iconKey == key
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { iconKey = key }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = getTemplateIcon(key),
                                    contentDescription = label,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        onSave(
                            template.copy(
                                name = name.trim(),
                                category = category,
                                defaultAmount = amount,
                                iconKey = iconKey
                            )
                        )
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
