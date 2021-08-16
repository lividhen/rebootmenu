package github.ryuunoakaihitomi.powerpanel.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.drakeet.about.AbsAboutActivity
import com.drakeet.about.Category
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.topjohnwu.superuser.Shell
import github.ryuunoakaihitomi.powerpanel.BuildConfig
import github.ryuunoakaihitomi.powerpanel.R
import github.ryuunoakaihitomi.powerpanel.util.BlackMagic
import github.ryuunoakaihitomi.powerpanel.util.RC
import github.ryuunoakaihitomi.powerpanel.util.openUrlInBrowser
import org.apache.commons.io.IOUtils
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
    }

    override fun onCreateHeader(icon: ImageView, slogan: TextView, version: TextView) {
        ContextCompat.getDrawable(this, android.R.drawable.ic_lock_power_off)?.run {
            val d = DrawableCompat.wrap(this)
            DrawableCompat.setTint(d, RC.getColor(resources, R.color.colorIconBackground, null))
            icon.setImageDrawable(d)
        }
        icon.setOnLongClickListener {
            Toast.makeText(this, "とまれかくもあはれ\nほたるほたるおいで", Toast.LENGTH_LONG).show()
            openUrlInBrowser("https://www.nicovideo.jp/watch/sm15408719")
            true
        }
        slogan.visibility = View.GONE
        version.text = BuildConfig.BUILD_TIME
        version.setOnLongClickListener {
            recordLogcat()
            true
        }
    }

    override fun onItemsCreated(items: MutableList<Any>) {
        items.add(Category("Powered by…"))
        platformSupportList.all { items.add(it) }
        items.add(Category("implementation"))
        inflateData("_", items)
        items.add(Category("debugImplementation"))
        inflateData("debug", items)
    }

    private fun inflateData(endpoint: String, l: MutableList<Any>) = JsonParser.parseString(
        IOUtils.toString(assets.open("dependency_list/$endpoint.json"), StandardCharsets.UTF_8)
    ).asJsonArray.forEach { l.add(Gson().fromJson(it, L::class.java)) }

    //<editor-fold desc="抓取logcat">

    /*
     * 一般来说使用错误报告取得在发布后的调试信息，
     * 不过有时可用这个后门快速抓取logcat以便首先获得一部分可在设备上直接查看的蛛丝马迹。
     * （错误报告通常很大以至于在终端设备上加载起来极为卡顿，无法直接查看，只能导入至PC后才能查看）
     */

    private val maxLineCount = (1024..2048).random()

    private val ar = registerForActivityResult(object : ActivityResultContracts.CreateDocument() {
        override fun createIntent(context: Context, input: String): Intent {
            return super.createIntent(context, input).apply { type = "text/plain" }
        }
    }) {
        it?.runCatching {
            Shell.sh("logcat -t $maxLineCount").submit { r ->
                contentResolver.openOutputStream(this)?.apply {
                    write(r.out.joinToString(separator = System.lineSeparator()).toByteArray())
                    close()
                    finish()
                }
            }
        }
    }

    private fun recordLogcat() {
        Toast.makeText(application, "Recent $maxLineCount lines Logcat", Toast.LENGTH_LONG).show()
        ar.launch("logcat_${System.currentTimeMillis().toString(Character.MAX_RADIX).uppercase()}")
    }
    //</editor-fold>
}

private fun strOf(@StringRes id: Int) = BlackMagic.getGlobalApp().getString(id)