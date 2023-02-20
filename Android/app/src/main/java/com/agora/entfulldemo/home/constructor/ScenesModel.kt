package com.agora.entfulldemo.home.constructor

import androidx.annotation.DrawableRes

/**
 * @author create by zhangwei03
 */
data class ScenesModel constructor(
    val clazzName: String,
    val name: String,
    @DrawableRes val background: Int,
    @DrawableRes val icon: Int,
    val active: Boolean = false,
)