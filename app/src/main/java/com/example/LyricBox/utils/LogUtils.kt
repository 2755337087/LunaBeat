package com.example.LyricBox.utils

import android.util.Log

object LogUtils {
    
    private const val TAG = "LyricBox"
    private const val DEBUG = true
    
    fun v(tag: String = TAG, message: String, throwable: Throwable? = null) {
        if (DEBUG) {
            if (throwable != null) {
                Log.v(tag, message, throwable)
            } else {
                Log.v(tag, message)
            }
        }
    }
    
    fun d(tag: String = TAG, message: String, throwable: Throwable? = null) {
        if (DEBUG) {
            if (throwable != null) {
                Log.d(tag, message, throwable)
            } else {
                Log.d(tag, message)
            }
        }
    }
    
    fun i(tag: String = TAG, message: String, throwable: Throwable? = null) {
        if (DEBUG) {
            if (throwable != null) {
                Log.i(tag, message, throwable)
            } else {
                Log.i(tag, message)
            }
        }
    }
    
    fun w(tag: String = TAG, message: String, throwable: Throwable? = null) {
        if (DEBUG) {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        }
    }
    
    fun e(tag: String = TAG, message: String, throwable: Throwable? = null) {
        if (DEBUG) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }
    
    fun wtf(tag: String = TAG, message: String, throwable: Throwable? = null) {
        if (DEBUG) {
            if (throwable != null) {
                Log.wtf(tag, message, throwable)
            } else {
                Log.wtf(tag, message)
            }
        }
    }
}
