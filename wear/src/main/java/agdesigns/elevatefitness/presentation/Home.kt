package agdesigns.elevatefitness.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@Destination<RootGraph>(start = true)
@Composable
fun Home(
    viewModel: HomeViewModel = hiltViewModel()
){
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        TimeText()
        if (viewModel.state.value.exerciseName.isEmpty()){
            Text(text = "Please start a workout on your phone to begin")
        } else {
            Column {
                Text(text = viewModel.state.value.exerciseName)
                Text(text = viewModel.state.value.reps.toString())
                Text(text = viewModel.state.value.weight.toString())
                Text(text = viewModel.state.value.rest.toString())
                Text(text = viewModel.state.value.note)
            }
        }
    }
}