package github.ryuunoakaihitomi.powerpanel.ui.main

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.SpannedString
import androidx.annotation.StringRes
import androidx.core.graphics.ColorUtils
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.scale
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.dmoral.toasty.Toasty
import github.ryuunoakaihitomi.powerpanel.R
import github.ryuunoakaihitomi.powerpanel.desc.PowerInfo
import github.ryuunoakaihitomi.powerpanel.util.BlackMagic
import github.ryuunoakaihitomi.powerpanel.util.RC
import github.ryuunoakaihitomi.powerpanel.util.isRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import rikka.sui.Sui
import timber.log.Timber

// AndroidViewModel 因为魔法问题不可用了
class PowerViewModel : ViewModel() {

    private val app = BlackMagic.getGlobalApp()

    /* Root模式：分隔开受限模式 */
    val rootMode: LiveData<Boolean>
        get() = _rootMode
    private var _rootMode = MutableLiveData<Boolean>()

    /* 强制模式：分隔开特权模式 */
    private var forceMode = MutableLiveData<Boolean>()

    /* 标题：受限模式会提示用户 */
    val title: LiveData<CharSequence>
        get() = _title
    private var _title = MutableLiveData<CharSequence>()

    /* 观察对象，提供界面资源 */
    val infoArray: LiveData<Array<PowerInfo>>
        get() = _infoArray
    private var _infoArray = MutableLiveData<Array<PowerInfo>>()
    val shortcutInfoArray: LiveData<Array<PowerInfo>>
        get() = _shortcutInfoArray
    private var _shortcutInfoArray = MutableLiveData<Array<PowerInfo>>()

    /* 观察对象，执行回调 */
    val labelResId: LiveData<Int>
        get() = _labelResId
    private var _labelResId = MutableLiveData<Int>()

    // 用来显示对话框
    fun prepare() {
        viewModelScope.launch(Dispatchers.IO) {
            val hasPrivilege = if (Sui.isSui()) {
                // use Sui whenever available
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                // if Sui is not present, fallback to plain old su
                isRoot()
            }

            viewModelScope.launch {
                _rootMode.value = hasPrivilege
                forceMode.value = false
                changeInfo()
            }
        }
    }

    // 用来执行操作
    fun call(@StringRes labelResId: Int) {
        _labelResId.value = labelResId
    }

    fun reverseForceMode() {
        forceMode.value = getForceMode().not()
        if (forceMode.value == true) {
            Toasty.warning(app, R.string.toast_switch_to_force_mode).show()
        } else {
            Toasty.normal(app, R.string.toast_switch_to_privileged_mode).show()
        }
        changeInfo()
    }

    private fun changeInfo() {

        val lockScreen = provide(R.string.func_lock_screen)
        val showSysPowerDialog = provide(R.string.func_sys_pwr_menu)
        val reboot = provide(R.string.func_reboot)
        val shutdown = provide(R.string.func_shutdown)
        val recovery = provide(R.string.func_recovery)
        val bootloader = provide(R.string.func_bootloader)
        val softReboot = provide(R.string.func_soft_reboot)
        val restartSysUi = provide(R.string.func_restart_sys_ui)
        val safeMode = provide(R.string.func_safe_mode)
        val lockScreenPrivileged = provide(R.string.func_lock_screen_privileged)

        /* 这里定义了各个选项的顺序，这个顺序已经经过反复的试验，一般不需要更改 */

        val restrictedActions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                arrayOf(lockScreen, showSysPowerDialog)
            else
                arrayOf(lockScreen)

        var privilegedActions = arrayOf(
            reboot,
            shutdown,
            recovery,
            bootloader,
            softReboot,
            restartSysUi,
            safeMode,
            lockScreenPrivileged
        )

        val rawTitle = app.getString(R.string.app_name)
        if (rootMode.value == true) {
            _title.value = rawTitle

            /* 在Android 11中，为特权模式添加打开系统电源菜单的选项以访问设备控制器（使用Shizuku实现） */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED &&
                app.packageManager.hasSystemFeature(PackageManager.FEATURE_CONTROLS)
            ) {
                Timber.d("add showSysPowerDialog for device controls")
                val size = privilegedActions.size
                @Suppress("UNCHECKED_CAST")
                privilegedActions = privilegedActions.copyOf(size + 1) as Array<PowerInfo>
                privilegedActions[size] = showSysPowerDialog
            }

            _infoArray.value = privilegedActions
            // https://developer.android.google.cn/guide/topics/ui/shortcuts?hl=en#shortcut-limitations
            // Although you can publish up to five shortcuts (static and dynamic shortcuts combined) at a time for your app, most launchers can only display four.
            _shortcutInfoArray.value = privilegedActions.copyOfRange(0, 4)
        } else {
            val ssb = SpannableStringBuilder()
            val suffix = buildSpannedString {
                // 这里不简单使用[Color.GRAY]是为了确保在亮暗两种主题上的可视程度一致
                color(ColorUtils.blendARGB(Color.WHITE, Color.DKGRAY, 0.5f)) {
                    scale(0.6f) {
                        bold {
                            append(app.getString(R.string.title_dialog_restricted_mode))
                        }
                    }
                }
            }
            ssb.append(rawTitle, " ", suffix)
            _title.value = ssb
            _infoArray.value = restrictedActions
            _shortcutInfoArray.value = restrictedActions
        }
    }

    //<editor-fold desc="定义电源信息">
    private fun provide(@StringRes labelResId: Int): PowerInfo {
        val powerInfo = PowerInfo()
        return when (labelResId) {
            R.string.func_lock_screen -> {
                powerInfo.apply { iconResId = R.drawable.ic_baseline_lock_24 }
            }
            R.string.func_sys_pwr_menu -> {
                powerInfo.apply { iconResId = R.drawable.ic_baseline_menu_24 }
            }
            R.string.func_reboot -> {
                powerInfo.apply {
                    hasForceMode = true
                    iconResId = R.drawable.ic_baseline_settings_backup_restore_24
                }
            }
            R.string.func_shutdown -> {
                powerInfo.apply {
                    hasForceMode = true
                    iconResId = R.drawable.ic_baseline_mobile_off_24
                }
            }
            R.string.func_recovery -> {
                powerInfo.apply {
                    hasForceMode = true
                    iconResId = R.drawable.ic_baseline_restore_page_24
                }
            }
            R.string.func_bootloader -> {
                powerInfo.apply {
                    hasForceMode = true
                    iconResId = R.drawable.ic_baseline_system_update_24
                }
            }
            R.string.func_soft_reboot -> {
                powerInfo.apply { iconResId = R.drawable.ic_baseline_power_24 }
            }
            R.string.func_restart_sys_ui -> {
                powerInfo.apply { iconResId = R.drawable.ic_baseline_aspect_ratio_24 }
            }
            R.string.func_safe_mode -> {
                powerInfo.apply {
                    hasForceMode = true
                    iconResId = R.drawable.ic_baseline_android_24
                }
            }
            R.string.func_lock_screen_privileged -> {
                powerInfo.apply { iconResId = R.drawable.ic_baseline_lock_24 }
            }
            else -> powerInfo
        }.apply {
            label = colorForceLabel(app.getString(labelResId), this)
            this.labelResId = labelResId
        }
    }
    //</editor-fold>

    fun getForceMode() = forceMode.value ?: false

    fun isOnForceMode(info: PowerInfo) = getForceMode() and info.hasForceMode

    fun shouldConfirmAgain(item: PowerInfo) =
        rootMode.value == true && getForceMode().not() && listOf(
            R.string.func_lock_screen_privileged,
            // 打开系统电源菜单之后还有一步操作，所以无需确认
            R.string.func_sys_pwr_menu
        ).contains(item.labelResId).not()

    private fun colorForceLabel(label: String, info: PowerInfo): SpannedString {
        var forceLabel = SpannedString(label)
        val forceColor = RC.getColor(app.resources, R.color.colorForceModeItem, null)
        if (isOnForceMode(info)) {
            forceLabel = buildSpannedString { bold { color(forceColor) { append(label) } } }
        }
        return forceLabel
    }
}