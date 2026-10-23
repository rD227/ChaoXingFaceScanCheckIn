/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import androidx.compose.runtime.Immutable
import com.google.mlkit.vision.barcode.common.Barcode
import android.net.Uri
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingOtherUserHelper

@Immutable
data class ChaoxingOtherUserSharedEntity(
    val phoneNumber: String,
    val encryptedPassword: String,
    val userName: String,
    val faceObjectIds: List<String> = emptyList(),
    val deviceCode: String? = null,
) {
    companion object {
        fun parseFromQRCode(qrcode: Barcode): ChaoxingOtherUserSharedEntity {
            return runCatching {
                val url = Uri.parse(qrcode.rawValue ?: throw ChaoxingOtherUserHelper.NotAvailableQRCodeException("二维码内容为空"))
                if (url.scheme != "facescan-clean" || url.host != "import")
                    throw ChaoxingOtherUserHelper.NotAvailableQRCodeException("不是本实验分支的账号导入码")
                val phoneNumber = url.getQueryParameter("phone")!!
                val password = url.getQueryParameter("pwd")!!
                val userName = url.getQueryParameter("name")!!
                val faceObjectIds = url.getQueryParameter("face")
                    ?.split(',')
                    ?.filter { it.isNotBlank() }
                    ?.distinct()
                    .orEmpty()
                val deviceCode = url.getQueryParameter("dc")?.takeIf { it.isNotEmpty() }
                ChaoxingOtherUserSharedEntity(
                    phoneNumber,
                    password,
                    userName,
                    faceObjectIds,
                    deviceCode,
                )
            }.getOrElse {
                throw ChaoxingOtherUserHelper.NotAvailableQRCodeException("此二维码不能作用于添加用户")
            }
        }
    }
}
