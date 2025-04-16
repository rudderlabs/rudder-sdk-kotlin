package com.rudderstack.sampleapp.screenone

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rudderstack.sampleapp.mainscreen.ActionButton
import com.rudderstack.sampleapp.mainscreen.CreateLogcat
import com.rudderstack.sampleapp.mainscreen.MainViewModel

@Composable
fun ScreenOne(
    viewModel: MainViewModel,
    onNavigateToScreenTwo: () -> Unit
) {
    ScreenOneContent(
        viewModel = viewModel,
        navigateToScreenTwo = onNavigateToScreenTwo
    )
}

@Composable
fun ScreenOneContent(
    viewModel: MainViewModel,
    navigateToScreenTwo: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(8.dp))
        Title(text = "Screen One")
        Spacer(modifier = Modifier.height(8.dp))
        NavigationButton(onNavigateToScreenTwo = navigateToScreenTwo)
        Spacer(modifier = Modifier.height(8.dp))
        CreateLogcat(viewModel)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun Title(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium
    )
}

@Composable
private fun NavigationButton(onNavigateToScreenTwo: () -> Unit) {
    ActionButton(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        onClick = onNavigateToScreenTwo,
        eventName = "Navigate to Screen Two"
    )
}
