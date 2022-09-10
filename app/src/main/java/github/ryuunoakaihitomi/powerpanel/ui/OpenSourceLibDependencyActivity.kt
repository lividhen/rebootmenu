package github.ryuunoakaihitomi.powerpanel.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.system.Os
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.annotation.StringRes
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.doOnTextChanged
import com.drakeet.about.AbsAboutActivity
import com.drakeet.about.Category
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.topjohnwu.superuser.Shell
import es.dmoral.toasty.Toasty
import github.ryuunoakaihitomi.poweract.Callback
import github.ryuunoakaihitomi.poweract.PowerActX
import github.ryuunoakaihitomi.powerpanel.BuildConfig
import github.ryuunoakaihitomi.powerpanel.R
import github.ryuunoakaihitomi.powerpanel.util.BlackMagic
import github.ryuunoakaihitomi.powerpanel.util.CC
import github.ryuunoakaihitomi.powerpanel.util.RC
import github.ryuunoakaihitomi.powerpanel.util.emptyAccessibilityDelegate
import github.ryuunoakaihitomi.powerpanel.util.isCrackDroidEnv
import github.ryuunoakaihitomi.powerpanel.util.isRoot
import github.ryuunoakaihitomi.powerpanel.util.isWatch
import github.ryuunoakaihitomi.powerpanel.util.openUrlInBrowser
import github.ryuunoakaihitomi.powerpanel.util.uiLog
import org.apache.commons.io.IOUtils
import timber.log.Timber
import java.io.IOException
import java.nio.charset.StandardCharsets
import com.drakeet.about.License as L

class OpenSourceLibDependencyActivity : AbsAboutActivity() {

    private val platformSupportList = listOf(
        /**
        本应用程序作为在Android平台及其相关支持组件下开发的产物，
        受到AOSP协议的约束。（目前将Jetpack也算入AOSP中）
         */
        L(
            "Android Open Source Project", "Google LLC",
            strOf(R.string.url_aosp_license), strOf(R.string.url_aosp_home)
        ),

        L(
            "CyanogenMod Platform SDK",
            "CyanogenMod",
            com.drakeet.about.License.APACHE_2,
            "https://github.com/CyanogenMod/cm_platform_sdk"
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        if (isWatch()) {
            uiLog("Denied on wear")
            finish()
        }
    }

    override fun onCreateHeader(icon: ImageView, slogan: TextView, version: TextView) {
        CC.getDrawable(this, android.R.drawable.ic_lock_power_off)?.run {
            val d = DrawableCompat.wrap(this)
            DrawableCompat.setTint(d, RC.getColor(resources, R.color.colorIconBackground, null))
            icon.setImageDrawable(d)
        }
        icon.setOnClickListener {
            // 不被推荐使用但偶尔需要的功能，因此
            // - 不计入统计
            // - 不在PowerExecution中抽象，直接调用
            // - 不记录在帮助文档中
            if (!isRoot()) return@setOnClickListener
            val editor = EditText(this).apply {
                hint = getText(R.string.hint_edittext_custom_reboot)
                setSingleLine()
                // 保证hint不为monospace，防止长度超出dialog
                doOnTextChanged { text, _, _, _ ->
                    typeface = if (text.isNullOrEmpty()) Typeface.DEFAULT else Typeface.MONOSPACE
                }
            }
            val isS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            AlertDialog.Builder(this)
                .setTitle(R.string.title_dialog_custom_reboot)
                .setIcon(android.R.drawable.stat_sys_warning)
                .setView(editor)
                .setPositiveButton(R.string.func_reboot) { _, _ ->
                    PowerActX.customReboot(editor.text.toString(), object : Callback {
                        override fun done() {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                finishAndRemoveTask()
                            } else {
                                finish()
                            }
                        }

                        override fun failed() {
                            Timber.i("Failed CR: ${editor.text}")
                        }
                    })
                }
                .show().run {
                    window?.run {
                        decorView.emptyAccessibilityDelegate()
                        @SuppressLint("NewApi") // 假阳性
                        if (isS) setHideOverlayWindows(true)
                    }
                    // 没有再确认选项，所以做成红色按钮
                    getButton(DialogInterface.BUTTON_POSITIVE).run {
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(Color.WHITE)
                        setBackgroundColor(Color.RED)
                    }
                }

            if (isS) {
                Toast.makeText(this, R.string.toast_custom_reboot, Toast.LENGTH_LONG).show()
            } else {
                Toasty.warning(this, R.string.toast_custom_reboot, Toasty.LENGTH_LONG).show()
            }
        }
        icon.setOnLongClickListener {
            Toast.makeText(this, "F.S.I.E.M.I.N", Toast.LENGTH_LONG).show()
            openUrlInBrowser("https://www.gnu.org/philosophy/free-software-even-more-important.html")
            true
        }
        slogan.visibility = View.GONE
        version.text = BuildConfig.BUILD_TIME
        // 可能把DocumentUI阉割了
        if (!isCrackDroidEnv) version.setOnLongClickListener {
            recordLogcat()
            true
        }
    }

    override fun onItemsCreated(items: MutableList<Any>) {
        items.add(Category("Platform Support"))
        platformSupportList.all { items.add(it) }
        inflateJsonData(items, "implementation")
        inflateJsonData(items, "debugImplementation", "debug")
        // 给个位置给Firebase
        inflateJsonData(items, "non-free", "free")
        inflateJsonData(items, "Gradle plugin", "plugin")
    }

    private fun inflateJsonData(l: MutableList<Any>, title: String, ep: String = title) = try {
        val `is` = assets.open("dependency_list/$ep.json")
        l.add(Category(title))
        JsonParser.parseString(IOUtils.toString(`is`, StandardCharsets.UTF_8))
            .asJsonArray.forEach { l.add(Gson().fromJson(it, L::class.java)) }
    } catch (e: IOException) {
        Timber.w(e.toString())
    }

    //<editor-fold desc="抓取logcat">

    /*
     * 一般来说使用错误报告取得在发布后的调试信息，
     * 不过有时可用这个后门快速抓取logcat以便首先获得一部分可在设备上直接查看的蛛丝马迹。
     * （错误报告通常很大以至于在终端设备上加载起来极为卡顿，无法直接查看，只能导入至PC后才能查看）
     */

    private val maxLineCount = 2048

    private val ar = registerForActivityResult(CreateDocument("text/plain")) {
        it?.runCatching {
            val command =
                "logcat -t $maxLineCount${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) " --pid=${Os.getpid()}" else ""}"
            Shell.cmd(command).submit { r ->
                contentResolver.openOutputStream(this)?.apply {
                    write(r.out.joinToString(separator = System.lineSeparator()).toByteArray())
                    close()
                    finish()
                }
            }
        }
    }

    private fun recordLogcat() {
        uiLog("Recent $maxLineCount lines Logcat…")
        ar.launch("logcat_${System.currentTimeMillis().toString(Character.MAX_RADIX).uppercase()}")
    }
    //</editor-fold>
}

// 在Activity初始化前可用
private fun strOf(@StringRes id: Int) = BlackMagic.getGlobalApp().getString(id)