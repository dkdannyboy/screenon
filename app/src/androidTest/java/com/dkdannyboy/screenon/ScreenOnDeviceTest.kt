package com.dkdannyboy.screenon

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenOnDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private val power = context.getSystemService(PowerManager::class.java)
    private var oldTimeout = "60000"
    private var oldStayAwake = "0"
    @Before fun launch() {
        TimeoutGuard(context).setEnabled(false)
        oldTimeout = device.executeShellCommand("settings get system screen_off_timeout").trim()
        oldStayAwake = device.executeShellCommand("settings get global stay_on_while_plugged_in").trim()
        device.executeShellCommand("settings put global stay_on_while_plugged_in 0")
        device.executeShellCommand("settings put system screen_off_timeout 15000")
        device.executeShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        device.wakeUp()
        device.executeShellCommand("cmd statusbar collapse")
        device.executeShellCommand("wm dismiss-keyguard")
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 5000))
        setEnabled(false)
    }
    @After fun cleanup() {
        allowTimeoutWrites(true)
        setEnabled(false)
        TimeoutGuard(context).restore()
        TimeoutGuard(context).setEnabled(false)
        device.executeShellCommand("settings put system screen_off_timeout $oldTimeout")
        device.executeShellCommand("settings put global stay_on_while_plugged_in $oldStayAwake")
        device.wakeUp()
        device.executeShellCommand("cmd statusbar collapse")
        device.executeShellCommand("wm dismiss-keyguard")
    }
    private fun setEnabled(enabled: Boolean) {
        instrumentation.runOnMainSync { AwakeController.setEnabled(context, enabled) }
        val deadline = System.currentTimeMillis() + 5000
        while (AwakeController.state.value.enabled != enabled && System.currentTimeMillis() < deadline) Thread.sleep(100)
        assertEquals(enabled, AwakeController.state.value.enabled)
        device.waitForIdle()
    }
    private fun activeLocks(): String = device.executeShellCommand("dumpsys power").substringAfter("Wake Locks: size=").substringBefore("Suspend Blockers:")

    @Test fun onKeepsOtherAppAwakePastSystemTimeoutAndOffReleases() {
        setEnabled(true)
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(device.wait(Until.hasObject(By.pkg("com.android.settings")), 5000))
        Thread.sleep(22000)
        assertTrue("Screen must remain interactive beyond the 15-second system timeout", power.isInteractive)
        val dump = activeLocks()
        assertTrue(dump.contains("ScreenOn:display"))
        assertEquals("15000", device.executeShellCommand("settings get system screen_off_timeout").trim())
        setEnabled(false)
        assertFalse(activeLocks().contains("ScreenOn:display"))
        Thread.sleep(22000)
        assertFalse("OFF must restore natural screen sleep", power.isInteractive)
    }
    @Test fun powerButtonStopsSessionWithoutWakingScreenAgain() {
        setEnabled(true)
        device.sleep()
        Thread.sleep(1200)
        assertFalse(power.isInteractive)
        assertFalse(AwakeController.state.value.enabled)
        assertFalse(activeLocks().contains("ScreenOn:display"))
        device.wakeUp()
        assertFalse(AwakeController.state.value.enabled)
    }
    @Test fun repeatedOnIsIdempotentAndOffReleasesOnlyLock() {
        setEnabled(true)
        val start = AwakeController.state.value.startedAt
        repeat(5) { setEnabled(true) }
        assertEquals(start, AwakeController.state.value.startedAt)
        val locks = activeLocks().lineSequence().count { it.contains("ScreenOn:display") }
        assertEquals(1, locks)
        setEnabled(false)
        setEnabled(false)
        assertFalse(activeLocks().contains("ScreenOn:display"))
    }
    @Test fun primaryPowerButtonActuallyToggles() {
        val toggle = device.wait(Until.findObject(By.descContains("화면 켜짐 유지")), 5000)
        assertNotNull(toggle)
        toggle.click()
        assertTrue(device.wait(Until.hasObject(By.text("화면 켜짐 유지 중")), 5000))
        device.findObject(By.descContains("화면 켜짐 유지")).click()
        assertTrue(device.wait(Until.hasObject(By.text("화면 켜짐 유지 꺼짐")), 5000))
    }
    @Test fun notificationOffActionStopsService() {
        setEnabled(true)
        device.openNotification()
        val off = device.wait(Until.findObject(By.text("끄기")), 5000)
        if (off == null) {
            device.findObjects(By.res("android", "expand_button")).forEach { it.click() }
        }
        val action = device.wait(Until.findObject(By.text("끄기")), 5000)
        assertNotNull("Notification must provide an OFF action", action)
        action.click()
        Thread.sleep(700)
        assertFalse(AwakeController.state.value.enabled)
        assertFalse(activeLocks().contains("ScreenOn:display"))
        device.pressBack()
    }
    private fun allowTimeoutWrites(allow: Boolean) {
        device.executeShellCommand("appops set ${context.packageName} WRITE_SETTINGS ${if (allow) "allow" else "deny"}")
    }
    @Test fun compatibilityAlonePreventsTimeoutAndRestoresSleep() {
        allowTimeoutWrites(true)
        val guard = TimeoutGuard(context)
        guard.setEnabled(true)
        guard.begin()
        assertEquals(TimeoutGuard.KEEP_TIMEOUT, guard.current())
        // No service/display wake lock: independently verify the new fallback.
        assertFalse(activeLocks().contains("ScreenOn:display"))
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(device.wait(Until.hasObject(By.pkg("com.android.settings")), 5000))
        Thread.sleep(22000)
        assertTrue(power.isInteractive)
        assertTrue(guard.restore())
        assertEquals(15000, guard.current())
        Thread.sleep(22000)
        assertFalse(power.isInteractive)
    }
    @Test fun compatibilityServiceRestoresAfterPowerButton() {
        allowTimeoutWrites(true)
        val guard = TimeoutGuard(context)
        guard.setEnabled(true)
        setEnabled(true)
        assertEquals(TimeoutGuard.KEEP_TIMEOUT, guard.current())
        setEnabled(true)
        device.sleep()
        Thread.sleep(1200)
        assertFalse(AwakeController.state.value.enabled)
        assertEquals(15000, guard.current())
        assertFalse(guard.pendingRestore())
    }
    @Test fun compatibilityStopsIfUserOrPolicyChangesTimeout() {
        allowTimeoutWrites(true)
        val guard = TimeoutGuard(context)
        guard.setEnabled(true)
        setEnabled(true)
        device.executeShellCommand("settings put system screen_off_timeout 30000")
        Thread.sleep(1200)
        assertFalse(AwakeController.state.value.enabled)
        assertEquals(30000, guard.current())
        assertFalse(guard.pendingRestore())
        assertNotNull(AwakeController.state.value.error)
    }
    @Test fun missingPermissionNeverClaimsOnOrChangesTimeout() {
        allowTimeoutWrites(false)
        val guard = TimeoutGuard(context)
        guard.setEnabled(true)
        instrumentation.runOnMainSync { AwakeController.setEnabled(context, true) }
        Thread.sleep(500)
        assertFalse(AwakeController.state.value.enabled)
        assertEquals(15000, guard.current())
        assertNotNull(AwakeController.state.value.error)
        assertFalse(guard.pendingRestore())
    }
    @Test fun restorationJournalSurvivesRecreationAndPermissionLoss() {
        allowTimeoutWrites(true)
        val guard = TimeoutGuard(context)
        guard.setEnabled(true)
        guard.begin()
        allowTimeoutWrites(false)
        assertFalse(TimeoutGuard(context).restore())
        assertTrue(guard.pendingRestore())
        allowTimeoutWrites(true)
        assertTrue(TimeoutGuard(context).restore())
        assertEquals(15000, guard.current())
        assertFalse(guard.pendingRestore())
    }
    @Test fun noOverlayOrNetworkPermissionDeclared() {
        val permissions = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty().toList()
        assertFalse(permissions.contains(Manifest.permission.SYSTEM_ALERT_WINDOW))
        assertTrue(permissions.contains(Manifest.permission.WRITE_SETTINGS))
        assertFalse(permissions.contains(Manifest.permission.INTERNET))
    }
}
