/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baidu.location.LocationClient
import com.baidu.mapapi.SDKInitializer
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.MainActivity
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore

@Serializable
object WelcomeDestination

@Composable
fun WelcomeScreen(
    navToLoginDestination: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineContext = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text(
            "FaceScan 实验分支",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(
            modifier = Modifier
                .height(16.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                """本地清理实验分支基于 aquamarine5/ChaoxingSignFaker，保留上游署名并按 AGPL-3.0 提供源码。

本分支移除了打赏弹窗、友盟统计、Sentry 上报、远程排行榜与作者更新服务。

账号、照片和签到请求仍需要访问超星官方服务。地图与定位功能使用百度 SDK，开启相应功能前请了解其隐私说明：https://lbs.baidu.com/index.php?title=openprivacy

这是修改后的实验构建，不代表上游作者发布或支持。""".trimIndent()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedButton(onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                coroutineContext.launch {
                    context.chaoxingDataStore.updateData {
                        it.toBuilder().setAgreeTerms(true).build()
                    }
                }
                SDKInitializer.setAgreePrivacy(context, true)
                LocationClient.setAgreePrivacy(true)
                navToLoginDestination()
            }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "允许协议并进入应用",
                    fontSize = 16.sp
                )
            }
            Button(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                    context.startActivity(Intent().apply {
                        setClass(context, MainActivity::class.java)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra(MainActivity.INTENT_EXTRA_EXIT_FLAG, true)
                    })
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) { Text("退出应用", fontSize = 16.sp) }
        }

    }
}

@Preview
@Composable
fun WelcomeScreenPreview() {
    WelcomeScreen {}
}
