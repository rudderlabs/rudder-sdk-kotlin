package com.rudderstack.sampleapp.mainscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rudderstack.sampleapp.ui.theme.Shapes

/**
 * Main screen composable that serves as the root of the main screen UI.
 *
 * @param viewModel ViewModel handling the business logic
 * @param onNavigateToScreenOne Callback for navigation to Screen One
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToScreenOne: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.state) {
        if (state.state == MainViewModelState.AnalyticsState.NavigateToScreens) {
            onNavigateToScreenOne()
            viewModel.resetNavigationState()
        }
    }
    MainScreenContent(viewModel = viewModel)
}

/**
 * Content layout for the main screen containing all UI elements.
 *
 * @param viewModel ViewModel handling the business logic
 */
@Composable
fun MainScreenContent(
    viewModel: MainViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ButtonsTemplateOne(viewModel)
        Spacer(modifier = Modifier.height(16.dp))
        ButtonsTemplateTwo(viewModel)
        Spacer(modifier = Modifier.height(16.dp))
        AdvertisingIdSwitch(viewModel)
        Spacer(modifier = Modifier.height(16.dp))
        CreateLogcat(viewModel)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ButtonsTemplateOne(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Public API",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.align(Alignment.Start),
        )
        Spacer(modifier = Modifier.height(8.dp))
        val buttonRows = listOf(
            listOf(
                MainViewModelState.AnalyticsState.TrackMessage,
                MainViewModelState.AnalyticsState.ScreenMessage
            ),
            listOf(
                MainViewModelState.AnalyticsState.GroupMessage,
                MainViewModelState.AnalyticsState.IdentifyMessage
            ),
            listOf(
                MainViewModelState.AnalyticsState.AliasMessage,
                MainViewModelState.AnalyticsState.ForceFlush
            ),
        )
        buttonRows.forEach { row ->
            ButtonRow(names = row, viewModel = viewModel)
        }
    }
}

@Composable
fun ButtonsTemplateTwo(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Features",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.align(Alignment.Start),
        )
        Spacer(modifier = Modifier.height(8.dp))
        val buttonRows = listOf(
            listOf(
                MainViewModelState.AnalyticsState.StartSession,
                MainViewModelState.AnalyticsState.StartSessionWithCustomId
            ),
            listOf(
                MainViewModelState.AnalyticsState.EndSession,
                MainViewModelState.AnalyticsState.Reset
            ),
            listOf(
                MainViewModelState.AnalyticsState.Shutdown,
                MainViewModelState.AnalyticsState.NavigateToScreens
            )
        )
        buttonRows.forEach { row ->
            ButtonRow(names = row, viewModel = viewModel)
        }
    }
}

/**
 * Switch component for toggling advertising ID functionality.
 *
 * @param viewModel ViewModel handling the switch state
 */
@Composable
fun AdvertisingIdSwitch(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Enable Advertising ID",
            style = MaterialTheme.typography.titleSmall
        )
        Switch(
            checked = state.isAdvertisingIdEnabled,
            onCheckedChange = { viewModel.toggleAdvertisingIdPlugin(it) }
        )
    }
}


@Composable
fun ButtonRow(
    names: List<MainViewModelState.AnalyticsState>,
    viewModel: MainViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        names.forEach {
            ActionButton(
                modifier = Modifier.weight(0.5f),
                onClick = { viewModel.onMessageClicked(it) },
                eventName = it.eventName.toString(),
            )
        }
    }
}

@Composable
fun ActionButton(modifier: Modifier, onClick: () -> Unit, eventName: String) {
    Button(
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
    ) {
        Text(
            text = eventName,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White
        )
    }
}

@Composable
fun CreateLogcat(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val logText by viewModel.state.collectAsState()

        Text(
            text = "Payload Generated",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.align(Alignment.Start),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = logText.log,
            shape = Shapes.large,
            onValueChange = { /*no op*/ },
            modifier = Modifier
                .padding(bottom = 16.dp)
                .fillMaxWidth()
        )
    }
}
