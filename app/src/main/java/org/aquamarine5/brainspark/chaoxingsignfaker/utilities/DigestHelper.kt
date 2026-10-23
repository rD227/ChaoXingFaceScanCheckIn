package org.aquamarine5.brainspark.chaoxingsignfaker.utilities

import java.security.MessageDigest

fun md5Hex(value: String): String = MessageDigest.getInstance("MD5")
    .digest(value.toByteArray(Charsets.UTF_8))
    .joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
