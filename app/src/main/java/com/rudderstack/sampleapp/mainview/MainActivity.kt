package com.rudderstack.sampleapp.mainview

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rudderstack.android.sampleapp.R
import com.rudderstack.sampleapp.ui.theme.Black
import com.rudderstack.sampleapp.ui.theme.Blue
import com.rudderstack.sampleapp.ui.theme.RudderAndroidLibsTheme
import com.rudderstack.sampleapp.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RudderAndroidLibsTheme {
                val scrollBehavior =
                    TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = { AppTopBar(viewModel, scrollBehavior) }
                ) { innerPadding ->
                    ButtonsTemplate(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    @Composable
    fun AppTopBar(viewModel: MainViewModel, scrollBehavior: TopAppBarScrollBehavior) {
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = White,
                titleContentColor = Black,
            ),
            title = {
                Text(
                    text = getString(R.string.title_activity_main),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.onBackClicked() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Blue,
                    )
                }
            },
            scrollBehavior = scrollBehavior,
        )
    }

    @Composable
    fun ButtonsTemplate(modifier: Modifier, viewModel: MainViewModel) {
        val state by viewModel.state.collectAsState()

        Column(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(8.dp),
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
                listOf(
                    MainViewModelState.AnalyticsState.StartSession,
                    MainViewModelState.AnalyticsState.StartSessionWithCustomId
                ),
                listOf(
                    MainViewModelState.AnalyticsState.EndSession,
                    MainViewModelState.AnalyticsState.Reset
                )
            )

            buttonRows.forEach { row ->
                ButtonRow(names = row, viewModel = viewModel)
            }

            CreateLogcat(state.logDataList)
        }
    }

    @Composable
    fun ButtonRow(names: List<MainViewModelState.AnalyticsState>, viewModel: MainViewModel) {
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
                    eventName = it.eventName,
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
    fun CreateRowData(logData: MainViewModelState.LogData) {
        Text(color = Blue, text = "${logData.time} - ${logData.log}")
    }

    @Composable
    fun ColumnScope.CreateLogcat(logCatList: List<MainViewModelState.LogData>) {
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
}