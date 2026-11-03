package hu.buildforge.diablo4

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Names and broad categories are discovery aids, not a verified seasonal stat database.
// Never display guessed affix ranges, power values, or an unverified patch label.
private data class CatalogEntry(val name: String, val type: String, val slot: String, val classes: String)
private val entries = listOf(
    CatalogEntry("Harlequin Crest", "Mythic Unique", "Helm", "All"),
    CatalogEntry("Ring of Starless Skies", "Mythic Unique", "Ring", "All"),
    CatalogEntry("The Grandfather", "Mythic Unique", "Two-Handed Sword", "Barbarian, Necromancer"),
    CatalogEntry("Tyrael's Might", "Mythic Unique", "Chest", "All"),
    CatalogEntry("Andariel's Visage", "Mythic Unique", "Helm", "All"),
    CatalogEntry("Doombringer", "Mythic Unique", "Sword", "Multiple"),
    CatalogEntry("Melted Heart of Selig", "Mythic Unique", "Amulet", "All"),
    CatalogEntry("Raiment of the Infinite", "Unique", "Chest", "Sorcerer"),
    CatalogEntry("Esu's Heirloom", "Unique", "Boots", "Sorcerer"),
    CatalogEntry("Tempest Roar", "Unique", "Helm", "Druid"),
    CatalogEntry("Vasily's Prayer", "Unique", "Helm", "Druid"),
    CatalogEntry("Razorplate", "Unique", "Chest", "All"),
    CatalogEntry("Penitent Greaves", "Unique", "Boots", "Multiple"),
    CatalogEntry("Tibault's Will", "Unique", "Pants", "Multiple"),
    CatalogEntry("Fists of Fate", "Unique", "Gloves", "All")
)

@Composable
fun NativeItemCatalog(onEquip: (String) -> Unit, modifier: Modifier = Modifier) {
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var selected by remember { mutableStateOf<CatalogEntry?>(null) }
    var affixText by remember { mutableStateOf("") }
    var comparison by remember { mutableStateOf("") }
    val filtered = entries.filter {
        (category == "All" || it.type == category) &&
            (it.name.contains(search, true) || it.slot.contains(search, true) || it.classes.contains(search, true))
    }
    Column(modifier.verticalScroll(rememberScrollState())) {
        Text("NATIVE ITEM CODEX", style = MaterialTheme.typography.titleLarge)
        Text("Preview catalog: ${entries.size} named entries. This is NOT the complete item database.", style = MaterialTheme.typography.bodySmall)
        Text("Stats and artwork are deliberately omitted until source, patch, and usage rights are verified.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(search, { search = it }, label = { Text("Search name, class or slot") }, modifier = Modifier.fillMaxWidth())
        Row {
            listOf("All", "Unique", "Mythic Unique").forEach { filter ->
                FilterChip(
                    selected = category == filter, onClick = { category = filter },
                    label = { Text(filter) }, modifier = Modifier.padding(end = 6.dp)
                )
            }
        }
        Text("${filtered.size} matching entries")
        filtered.forEach { entry ->
            ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(entry.name, style = MaterialTheme.typography.titleMedium)
                    Text("${entry.type} • ${entry.slot} • ${entry.classes}")
                    TextButton(onClick = { selected = entry; affixText = ""; comparison = "" }) {
                        Text("Inspect / equip")
                    }
                }
            }
        }
        selected?.let { entry ->
            HorizontalDivider()
            Text("ITEM DETAILS: ${entry.name}", style = MaterialTheme.typography.titleMedium)
            Text("Type: ${entry.type} | Slot: ${entry.slot}")
            Text("Verified numerical stats: unavailable. Do not use this preview for DPS calculations.")
            OutlinedTextField(affixText, { affixText = it }, label = { Text("Enter observed item stats (manual)") },
                minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(comparison, { comparison = it }, label = { Text("Second item stats (manual comparison)") },
                minLines = 2, modifier = Modifier.fillMaxWidth())
            if (comparison.isNotBlank()) Text("Compare the two entered stat lists manually; no numeric score is inferred.")
            Button(onClick = {
                onEquip("${entry.type} | ${entry.name}" +
                    if (affixText.isBlank()) "" else " — ${affixText.trim()}")
            }) { Text("Add to current build") }
        }
    }
}
