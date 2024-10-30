package com.rudderstack.sampleapp.mainview

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rudderstack.sampleapp.ui.theme.RudderAndroidLibsTheme

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RudderAndroidLibsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    CreateButtonsTemplate(viewModel)
                }
            }
        }
    }

    @Composable
    fun CreateRowData(logData: LogData) {
        Text(color = Color.Blue, text = "${logData.time} - ${logData.log}")
    }

    @Composable
    fun ColumnScope.CreateLogcat(logCatList: List<LogData>) {
        LazyColumn(
            userScrollEnabled = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .weight(1f)
        ) {
            items(logCatList.size, null) { index ->
                CreateRowData(logData = logCatList[index])
            }
        }
    }

    //state hoisting?
    @Composable
    fun CreateRowOfApis(
        vararg names: AnalyticsState,
        weight: Float,
        viewModel: MainViewModel
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            names.forEach {
                Button(modifier = Modifier.weight(weight = weight, fill = true), onClick = {
                    viewModel.onMessageClicked(it)
                }) {
                    Text(text = it.eventName)
                }
            }
        }
    }

    @Composable
    fun CreateButtonsTemplate(viewModel: MainViewModel) {
        val state by viewModel.state.collectAsState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Rudderstack Analytics",
                modifier = Modifier
                    .padding(2.dp)
                    .align(Alignment.CenterHorizontally),
            )

            CreateRowOfApis(
                names = arrayOf(
                    AnalyticsState.TrackMessage,
                    AnalyticsState.ScreenMessage,
                    AnalyticsState.GroupMessage,
                    AnalyticsState.IdentifyMessage,
                    AnalyticsState.ForceFlush,
                ),
                weight = .3f,
                viewModel = viewModel
            )
            Spacer(modifier = Modifier.height(2.dp))
            CreateLogcat(state.logDataList)
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        CreateButtonsTemplate(MainViewModel(LocalContext.current.applicationContext as Application))
    }
}
