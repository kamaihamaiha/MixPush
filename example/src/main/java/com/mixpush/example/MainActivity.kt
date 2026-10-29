package com.mixpush.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.mixpush.core.GetRegisterIdCallback
import com.mixpush.core.MixPushPlatform
import com.mixpush.core.MixPushClient
import com.mixpush.vivo.VivoPushProvider
import java.util.*


class MainActivity : AppCompatActivity() {
    var notificationMixPushPlatform: MixPushPlatform? = null
    var passThroughMixPushPlatform: MixPushPlatform? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!NotificationManagerUtils.isPermissionOpen(this)) {
            NotificationManagerUtils.openPermissionSetting(this)
        }
        findViewById<View>(R.id.copy_reg_id).setOnClickListener {
            if (notificationMixPushPlatform != null) {
                copy(notificationMixPushPlatform!!.regId!!)
            } else {
                Toast.makeText(this, "注册失败", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<View>(R.id.copy_pass_through_reg_id).setOnClickListener {
            if (passThroughMixPushPlatform != null) {
                copy(passThroughMixPushPlatform!!.regId!!)
            } else {
                Toast.makeText(this, "注册失败", Toast.LENGTH_SHORT).show()
            }
        }
        MixPushClient.getInstance().getRegisterId(this, object : GetRegisterIdCallback() {
            override fun callback(platformMix: MixPushPlatform?) {
                notificationMixPushPlatform = platformMix
                updateRegId()
            }
        })
        findViewById<TextView>(R.id.log).text = Date().toString()
        VivoPushProvider.setSubscribeResultListener { operateResult, operations ->
            appendVivoTestLog("用户操作：$operateResult，渠道结果：$operations")
        }
        findViewById<View>(R.id.test_vivo_subscribe).setOnClickListener {
            if (notificationMixPushPlatform?.platformName != VivoPushProvider.VIVO) {
                appendVivoTestLog("当前通知推送不是 vivo，或尚未获取 regId")
                return@setOnClickListener
            }
            val channelIds = listOf("vpush_channel_4j") // 当前 example 使用每日英语听力的 vivo 应用配置
            VivoPushProvider.checkCanShowSubscribeDialog(applicationContext, channelIds) { code, message, statuses ->
                appendVivoTestLog("预检：code=$code，message=$message，渠道状态=$statuses")
                if (code == 0) {
                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        VivoPushProvider.showSubscribeDialog(this, channelIds) { showCode, showMessage, displays ->
                            appendVivoTestLog("弹窗拉起：code=$showCode，message=$showMessage，渠道状态=$displays")
                        }
                    }
                }
            }
        }

        updateRegId()
        onRequirePermissions()
    }

    private fun onRequirePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.READ_PHONE_STATE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 101
            )
        }
    }

    override fun onDestroy() {
        VivoPushProvider.setSubscribeResultListener(null)
        super.onDestroy()
    }

    private fun appendVivoTestLog(message: String) {
        Log.i("VivoSubscribeTest", message)
        runOnUiThread { findViewById<TextView>(R.id.log).append("\n$message") }
    }

    private fun copy(data: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(null, data)
        clipboard.setPrimaryClip(clipData)
        Toast.makeText(this, "已经复制到粘贴板", Toast.LENGTH_SHORT).show()
    }

    fun updateRegId() {
        runOnUiThread {
            if (notificationMixPushPlatform != null) {
                findViewById<TextView>(R.id.text_reg_id).text = notificationMixPushPlatform.toString()
            }
            if (passThroughMixPushPlatform != null) {
                findViewById<TextView>(R.id.text_pass_through_reg_id).text = passThroughMixPushPlatform.toString()
            }
        }
    }
}
