package hu.buildforge.diablo4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val classes = listOf("Barbarian", "Druid", "Necromancer", "Rogue", "Sorcerer", "Spiritborn", "Paladin")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                var selected by remember { mutableStateOf("Sorcerer") }
                var buildName by remember { mutableStateOf("") }
                val builds = remember { mutableStateListOf<String>() }
                Column(Modifier.fillMaxSize().padding(20.dp)) {
                    Text("BUILDFORGE AI", style = MaterialTheme.typography.headlineMedium)
                    Text("Diablo IV • Build planner prototype")
                    Spacer(Modifier.height(20.dp))
                    Text("Choose class")
                    LazyColumn(Modifier.weight(1f)) {
                        items(classes) { characterClass ->
                            Row {
                                RadioButton(selected == characterClass, onClick = { selected = characterClass })
                                Text(characterClass, Modifier.padding(top = 12.dp))
                            }
                        }
                    }
                    OutlinedTextField(value = buildName, onValueChange = { buildName = it }, label = { Text("Build name") })
                    Button(onClick = { builds.add("$selected: ${buildName.ifBlank { "New build" }}"); buildName = "" }) { Text("Save draft") }
                    Text("Drafts (current session): ${builds.size}")
                    builds.takeLast(3).forEach { Text(it) }
                }
            }
        }
    }
}
