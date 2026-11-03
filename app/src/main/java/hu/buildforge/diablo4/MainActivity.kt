package hu.buildforge.diablo4

import android.content.Context
import android.os.Bundle
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

private val classes = listOf("Barbarian", "Druid", "Necromancer", "Rogue", "Sorcerer", "Spiritborn", "Paladin")
private val slots = listOf("Weapon", "Helm", "Chest", "Gloves", "Pants", "Boots", "Amulet", "Rings")
private val tabs = listOf("Forge", "Advisor", "Codex", "Scanner", "Vault", "Items")
private val gold = Color(0xFFE0B76D)
private val dark = Color(0xFF111014)
private val surfaceDark = Color(0xFF242026)
private val rarityColors = mapOf(
    "Common" to Color(0xFFB9B9B9), "Magic" to Color(0xFF689DEA),
    "Rare" to Color(0xFFF4CD68), "Legendary" to Color(0xFFDE8C46),
    "Unique" to Color(0xFFD3A37C), "Mythic" to Color(0xFFB78CE5)
)
@Composable private fun ItemCard(name: String, rarity: String) {
    val tint = rarityColors[rarity] ?: gold
    Card(
        colors = CardDefaults.cardColors(containerColor = surfaceDark),
        border = BorderStroke(1.dp, tint),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
    ) {
        Row(Modifier.padding(12.dp)) {
            Box(
                Modifier.size(56.dp).background(tint.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) { Text("✦", color = tint, style = MaterialTheme.typography.headlineLarge) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(name.ifBlank { "Item preview" }, color = tint, fontWeight = FontWeight.Bold)
                Text(rarity, color = Color.LightGray)
            }
        }
    }
}

private data class BuildDraft(
    val name: String,
    val hero: String,
    val skill: String,
    val equipment: String,
    val paragon: String,
    val goal: String
) {
    fun toJson() = JSONObject().apply {
        put("name", name); put("hero", hero); put("skill", skill)
        put("equipment", equipment); put("paragon", paragon); put("goal", goal)
    }
}
private fun JSONObject.readDraft() = BuildDraft(
    optString("name"), optString("hero", "Sorcerer"), optString("skill"),
    optString("equipment"), optString("paragon"), optString("goal")
)
private fun loadBuilds(context: Context): List<BuildDraft> = try {
    val data = context.getSharedPreferences("buildforge", Context.MODE_PRIVATE)
        .getString("builds", "[]") ?: "[]"
    val array = JSONArray(data)
    (0 until array.length()).map { array.getJSONObject(it).readDraft() }
} catch (_: Exception) { emptyList() }
private fun saveBuilds(context: Context, builds: List<BuildDraft>) {
    val array = JSONArray()
    builds.forEach { array.put(it.toJson()) }
    context.getSharedPreferences("buildforge", Context.MODE_PRIVATE)
        .edit().putString("builds", array.toString()).apply()
}
private fun advice(hero: String, skill: String, gear: String, paragon: String, goal: String): List<String> {
    val result = mutableListOf<String>()
    if (skill.isBlank()) result.add("List your active and passive skills before optimizing.")
    if (gear.isBlank()) result.add("Record your equipped items and key aspects to identify gaps.")
    if (paragon.isBlank()) result.add("Add your Paragon boards and glyphs for a complete review.")
    if (goal.isBlank()) result.add("Choose a target activity such as Pit, bosses, or leveling.")
    if (skill.isNotBlank() && gear.isNotBlank())
        result.add("Check that your item affixes and aspects support the primary damage skill.")
    if (goal.contains("boss", ignoreCase = true))
        result.add("For bosses, review single-target damage, defenses and resource uptime.")
    if (goal.contains("pit", ignoreCase = true))
        result.add("For Pit progression, balance damage with resistances and survivability.")
    result.add("Class: $hero. These are general checklist suggestions, not verified seasonal rankings or calculated DPS.")
    return result
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContext = applicationContext
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = gold, background = dark, surface = surfaceDark)) {
                var tab by remember { mutableStateOf(0) }
                var hero by remember { mutableStateOf("Sorcerer") }
                var name by remember { mutableStateOf("") }
                var skill by remember { mutableStateOf("") }
                var gear by remember { mutableStateOf("") }
                var paragon by remember { mutableStateOf("") }
                var goal by remember { mutableStateOf("") }
                var selectedImage by remember { mutableStateOf<Uri?>(null) }
                var notice by remember { mutableStateOf("") }
                var itemName by remember { mutableStateOf("") }
                var itemRarity by remember { mutableStateOf("Legendary") }
                val saved = remember { mutableStateListOf<BuildDraft>().apply { addAll(loadBuilds(appContext)) } }
                val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
                    selectedImage = it
                }
                fun restore(d: BuildDraft) {
                    name = d.name; hero = d.hero; skill = d.skill
                    gear = d.equipment; paragon = d.paragon; goal = d.goal
                    tab = 0
                }
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text("✦ BUILDFORGE", color = gold, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
                    Text("SANCTUARY WORKSHOP • DIABLO IV", color = Color.LightGray)
                    Text("Reference patch: 3.2.4 • not live verified", color = gold, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                            Uri.parse("https://news.blizzard.com/en-us/article/24295395/diablo-iv-patch-notes")))
                    }) { Text("Official patch notes") }
                    Spacer(Modifier.height(10.dp))
                    ScrollableTabRow(selectedTabIndex = tab, edgePadding = 0.dp) {
                        tabs.forEachIndexed { index, label ->
                            Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (tab == 5) {
                        ItemCatalog(Modifier.weight(1f))
                    } else Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        when (tab) {
                            0 -> {
                                Text("HERO FORGE", color = gold, style = MaterialTheme.typography.titleLarge)
                                Text("Character class", style = MaterialTheme.typography.titleMedium)
                                classes.forEach { option ->
                                    Row {
                                        RadioButton(selected = hero == option, onClick = { hero = option })
                                        Text(option, Modifier.padding(top = 12.dp))
                                    }
                                }
                                OutlinedTextField(name, { name = it }, label = { Text("Build name") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(skill, { skill = it }, label = { Text("Skills and passives") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                                Text("ITEM WORKSHOP", color = gold, style = MaterialTheme.typography.titleMedium)
                                OutlinedTextField(itemName, { itemName = it }, label = { Text("Item name") }, modifier = Modifier.fillMaxWidth())
                                Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
                                    rarityColors.keys.forEach { rarity ->
                                        FilterChip(
                                            selected = itemRarity == rarity,
                                            onClick = { itemRarity = rarity },
                                            label = { Text(rarity) },
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                    }
                                }
                                ItemCard(itemName, itemRarity)
                                Button(onClick = {
                                    if (itemName.isNotBlank()) {
                                        gear += (if (gear.isBlank()) "" else "\n") + "$itemRarity | $itemName"
                                        itemName = ""
                                    }
                                }, enabled = itemName.isNotBlank()) { Text("Add to equipment") }
                                OutlinedTextField(gear, { gear = it }, label = { Text("Equipment, uniques, aspects and affixes") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                                gear.lines().filter { it.contains(" | ") }.forEach { line ->
                                    val parts = line.split(" | ", limit = 2)
                                    if (parts.size == 2) ItemCard(parts[1], parts[0])
                                }
                                Text("Rarity icons are illustrative. Official item images and validated stats require a licensed or permitted asset catalog.", style = MaterialTheme.typography.bodySmall)
                                Text("Suggested equipment slots: " + slots.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                                OutlinedTextField(paragon, { paragon = it }, label = { Text("Paragon boards and glyphs") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(goal, { goal = it }, label = { Text("Goal (Pit, bosses, leveling...)") }, modifier = Modifier.fillMaxWidth())
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = {
                                    val draft = BuildDraft(name.ifBlank { "$hero build" }, hero, skill, gear, paragon, goal)
                                    saved.add(draft)
                                    saveBuilds(appContext, saved)
                                    notice = "Build saved on this device."
                                }) { Text("Save build") }
                                if (notice.isNotBlank()) Text(notice)
                            }
                            1 -> {
                                Text("BUILD ADVISOR", color = gold, style = MaterialTheme.typography.titleLarge)
                                Text("Rule-based offline feedback. No AI service connected yet.")
                                advice(hero, skill, gear, paragon, goal).forEach { suggestion ->
                                    Spacer(Modifier.height(8.dp))
                                    Text("• $suggestion")
                                }
                            }
                            2 -> {
                                Text("SANCTUARY CODEX", color = gold, style = MaterialTheme.typography.titleLarge)
                                listOf(
                                    "Check the current season and patch before following a build.",
                                    "Review resistance, armor and defensive skills for your difficulty.",
                                    "Match skill synergies, aspects and uniques to your chosen activity.",
                                    "For boss encounters, learn attack patterns and prepare single-target damage.",
                                    "For farming, compare clear speed and survivability, not only peak damage."
                                ).forEach { Text("• $it", Modifier.padding(vertical = 7.dp)) }
                                Text("Guidance is general; current patch-specific data is not yet integrated.")
                            }
                            3 -> {
                                Text("ITEM SCANNER", color = gold, style = MaterialTheme.typography.titleLarge)
                                Text("Select a PS5 screenshot or item image for future analysis.")
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = { picker.launch("image/*") }) { Text("Choose image") }
                                Text(if (selectedImage == null) "No image selected" else "Image selected: " + selectedImage.toString().take(90))
                                Text("Automatic OCR and AI item recognition are not implemented in this version.")
                            }
                            4 -> {
                                Text("BUILD VAULT", color = gold, style = MaterialTheme.typography.titleLarge)
                                if (saved.isEmpty()) Text("No saved builds yet.")
                                saved.toList().forEachIndexed { index, draft ->
                                    ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text(draft.name, style = MaterialTheme.typography.titleMedium)
                                            Text(draft.hero)
                                            Row {
                                                TextButton(onClick = { restore(draft) }) { Text("Open") }
                                                TextButton(onClick = {
                                                    saved.removeAt(index)
                                                    saveBuilds(appContext, saved)
                                                }) { Text("Delete") }
                                            }
                                        }
                                    }
                                }
                                Text("Builds are stored locally on this device. No cloud sync yet.")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun ItemCatalog(modifier: Modifier = Modifier) {
    var source by remember { mutableStateOf("D4GG") }
    var loadError by remember { mutableStateOf(false) }
    val urls = mapOf(
        "D4GG" to "https://d4gg.gg/en/diablo4/database/",
        "D4Guides" to "https://d4guides.gg/en/database/items",
        "D4Life" to "https://diablo4.life/database/uniques"
    )
    Column(modifier) {
        Text("ITEM CODEX", color = gold, style = MaterialTheme.typography.titleLarge)
        Text("Live community catalogs • requires internet", style = MaterialTheme.typography.bodySmall)
        Text("Third-party information. Item values and patch coverage must be checked against Blizzard patch notes.", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
            urls.keys.forEach { key ->
                FilterChip(
                    selected = source == key,
                    onClick = { source = key; loadError = false },
                    label = { Text(key) },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }
        val url = urls[source] ?: return@Column
        if (loadError) {
            Text("The catalog could not load. Check your connection or open it in your browser.")
        }
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onReceivedError(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) loadError = true
                        }
                    }
                    loadUrl(url)
                }
            },
            update = { webView ->
                if (webView.url != url) webView.loadUrl(url)
            },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}
