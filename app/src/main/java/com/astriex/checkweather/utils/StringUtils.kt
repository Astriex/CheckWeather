package com.astriex.checkweather.utils

import android.content.Context
import android.widget.Toast

class StringUtils {
    companion object {
        fun showMessage(context: Context, message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}