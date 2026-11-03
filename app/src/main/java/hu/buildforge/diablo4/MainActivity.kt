package hu.buildforge.diablo4

import android.content.Context
import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
private val tabs = listOf("Planner", "Advisor", "Guide", "Image", "Saved")

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
            MaterialTheme(colorScheme = darkColorScheme()) {
                var tab by remember { mutableStateOf(0) }
                var hero by remember { mutableStateOf("Sorcerer") }
                var name by remember { mutableStateOf("") }
                var skill by remember { mutableStateOf("") }
                var gear by remember { mutableStateOf("") }
                var paragon by remember { mutableStateOf("") }
                var goal by remember { mutableStateOf("") }
                var selectedImage by remember { mutableStateOf<Uri?>(null) }
                var notice by remember { mutableStateOf("") }
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
                    Text("BUILDFORGE AI", style = MaterialTheme.typography.headlineMedium)
                    Text("Diablo IV companion • offline preview", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    ScrollableTabRow(selectedTabIndex = tab, edgePadding = 0.dp) {
                        tabs.forEachIndexed { index, label ->
                            Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        when (tab) {
                            0 -> {
                                Text("Character class", style = MaterialTheme.typography.titleMedium)
                                classes.forEach { option ->
                                    Row {
                                        RadioButton(selected = hero == option, onClick = { hero = option })
                                        Text(option, Modifier.padding(top = 12.dp))
                                    }
                                }
                                OutlinedTextField(name, { name = it }, label = { Text("Build name") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(skill, { skill = it }, label = { Text("Skills and passives") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(gear, { gear = it }, label = { Text("Equipment, uniques, aspects") }, minLines = 2, modifier = Modifier.fillMaxWidth())
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
                                Text("Build advisor", style = MaterialTheme.typography.titleLarge)
                                Text("Rule-based offline feedback. No AI service connected yet.")
                                advice(hero, skill, gear, paragon, goal).forEach { suggestion ->
                                    Spacer(Modifier.height(8.dp))
                                    Text("• $suggestion")
                                }
                            }
                            2 -> {
                                Text("Endgame checklist", style = MaterialTheme.typography.titleLarge)
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
                                Text("Screenshot workspace", style = MaterialTheme.typography.titleLarge)
                                Text("Select a PS5 screenshot or item image for future analysis.")
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = { picker.launch("image/*") }) { Text("Choose image") }
                                Text(if (selectedImage == null) "No image selected" else "Image selected: " + selectedImage.toString().take(90))
                                Text("Automatic OCR and AI item recognition are not implemented in this version.")
                            }
                            4 -> {
                                Text("Saved builds", style = MaterialTheme.typography.titleLarge)
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
