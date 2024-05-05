package agdesigns.elevatefitness.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import agdesigns.elevatefitness.ui.BottomNavigationGraph
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination<BottomNavigationGraph>
@Composable
fun Statistics(
    destinationsNavigator: DestinationsNavigator
) {
    Column(modifier = Modifier/*.verticalScroll(rememberScrollState())*/.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Coming soon!", style = MaterialTheme.typography.headlineSmall)
    }
}