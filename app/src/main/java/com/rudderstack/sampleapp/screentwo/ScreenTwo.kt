package com.rudderstack.sampleapp.screentwo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rudderstack.sampleapp.mainscreen.ActionButton
import com.rudderstack.sampleapp.mainscreen.CreateLogcat
import com.rudderstack.sampleapp.mainscreen.MainViewModel
import com.rudderstack.sampleapp.screenone.Title

@Composable
fun ScreenTwo(
    viewModel: MainViewModel,
    onNavigateToScreenOne: () -> Unit
) {
    ScreenTwoContent(
        viewModel = viewModel,
        onNavigateToScreenOne = onNavigateToScreenOne
    )
}

@Composable
fun ScreenTwoContent(
    viewModel: MainViewModel,
    onNavigateToScreenOne: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(8.dp))
        Title(text = "Screen Two")
        Spacer(modifier = Modifier.height(8.dp))
        NavigationButton(onNavigateToScreenOne = onNavigateToScreenOne)
        Spacer(modifier = Modifier.height(8.dp))
        CreateLogcat(viewModel)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun NavigationButton(onNavigateToScreenOne: () -> Unit) {
    ActionButton(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        onClick = onNavigateToScreenOne,
        eventName = "Navigate to Screen One"
    )
}