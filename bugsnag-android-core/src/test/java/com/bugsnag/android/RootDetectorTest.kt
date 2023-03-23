package com.bugsnag.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files

@RunWith(MockitoJUnitRunner::class)
class RootDetectorTest {

    private val rootDetector = RootDetector(logger = NoopLogger)

    @Mock
    lateinit var processBuilder: ProcessBuilder

    @Mock
    lateinit var process: Process

    @Before
    fun setUp() {
        `when`(processBuilder.start()).thenReturn(process)
    }

    /**
     * IOExceptions thrown when starting the process are handled appropriately
     */
    @Test
    fun checkSuProcessStartException() {
        `when`(processBuilder.start()).thenThrow(IOException())
        assertFalse(rootDetector.checkSuExists(processBuilder))
    }

    /**
     * The method returns false if 'which su' returns an empty string
     */
    @Test
    fun checkSuNotFound() {
        val emptyStream = ByteArrayInputStream("".toByteArray())
        `when`(process.inputStream).thenReturn(emptyStream)
        assertFalse(rootDetector.checkSuExists(processBuilder))
        verify(processBuilder, times(1)).command(listOf("which", "su"))
        verify(process, times(1)).destroy()
    }

    /**
     * The method returns false if 'which su' returns a blank string
     */
    @Test
    fun checkSuNotFoundBlank() {
        val emptyStream = ByteArrayInputStream("\n  \n".toByteArray())
        `when`(process.inputStream).thenReturn(emptyStream)
        assertFalse(rootDetector.checkSuExists(processBuilder))
        verify(processBuilder, times(1)).command(listOf("which", "su"))
        verify(process, times(1)).destroy()
    }

    /**
     * The method returns true if 'which su' returns a non-empty string
     */
    @Test
    fun checkSuFound() {
        val resultStream = ByteArrayInputStream("/system/bin/su".toByteArray())
        `when`(process.inputStream).thenReturn(resultStream)
        assertTrue(rootDetector.checkSuExists(processBuilder))
    }

    /**
     * Verifies that 'test-keys' triggers root detection.
     */
    @Test
    fun checkBuildTagsRooted() {
        val info = DeviceBuildInfo(null, null, null, null, null, null, "test-keys", null, null)
        assertTrue(RootDetector(info, logger = NoopLogger).checkBuildTags())
    }

    /**
     * Verifies that 'release-keys' does not trigger root detection
     */
    @Test
    fun checkBuildTagsNotRooted() {
        val info = DeviceBuildInfo(null, null, null, null, null, null, "release-keys", null, null)
        assertFalse(RootDetector(info, logger = NoopLogger).checkBuildTags())
    }

    /**
     * Verifies that a non-existent file does not trigger root detection
     */
    @Test
    fun checkRootBinaryRooted() {
        assertFalse(
            RootDetector(
                rootBinaryLocations = listOf("/foo"),
                logger = NoopLogger
            ).checkRootBinaries()
        )
    }

    /**
     * Verifies that an existing root binary triggers root detection
     */
    @Test
    fun checkRootBinaryNotRooted() {
        val tmpFile = Files.createTempFile("evilrootbinary", ".apk")
        val path = tmpFile.toFile().absolutePath
        assertTrue(
            RootDetector(
                rootBinaryLocations = listOf(path),
                logger = NoopLogger
            ).checkRootBinaries()
        )
    }

    /**
     * Verifies that a missing file does not throw an exception
     */
    @Test
    fun checkBuildPropsIOException() {
        val tmpFile = File("/foo")
        assertFalse(RootDetector(buildProps = tmpFile, logger = NoopLogger).checkBuildProps())
    }

    /**
     * Verifies that an empty file does not trigger root detection
     */
    @Test
    fun checkBuildPropsEmptyFile() {
        val tmpFile = Files.createTempFile("empty", ".prop").toFile()
        assertFalse(RootDetector(buildProps = tmpFile, logger = NoopLogger).checkBuildProps())
    }

    /**
     * Verifies that regular build properties do not trigger root detection
     */
    @Test
    fun checkBuildPropsNonRooted() {
        val tmpFile = Files.createTempFile("regular", ".prop").toFile()
        tmpFile.writeText(SAMPLE_BUILD_PROPS)
        assertFalse(RootDetector(buildProps = tmpFile, logger = NoopLogger).checkBuildProps())
    }

    /**
     * Verifies that regular build properties do not trigger root detection
     */
    @Test
    fun checkSafeBuildProps() {
        val tmpFile = Files.createTempFile("rooted", ".prop").toFile()
        tmpFile.writeText(SAMPLE_BUILD_PROPS)
        tmpFile.appendText("ro.secure=[1]")
        tmpFile.appendText("ro.debuggable=[0]")
        assertFalse(RootDetector(buildProps = tmpFile, logger = NoopLogger).checkBuildProps())
    }

    /**
     * Verifies that commented out build properties do not trigger root detection
     */
    @Test
    fun checkCommentedOutBuildProps() {
        val tmpFile = Files.createTempFile("rooted", ".prop").toFile()
        tmpFile.writeText(SAMPLE_BUILD_PROPS)
        tmpFile.appendText("#ro.secure=[1]")
        tmpFile.appendText("#  ro.debuggable=[0]")
        assertFalse(RootDetector(buildProps = tmpFile, logger = NoopLogger).checkBuildProps())
    }

    /**
     * Verifies that the ro.debuggable build property triggers root detection
     */
    @Test
    fun checkBuildPropsRootedDebuggable() {
        val tmpFile = Files.createTempFile("rooted", ".prop").toFile()
        tmpFile.writeText(SAMPLE_BUILD_PROPS)
        tmpFile.appendText("\nro.debuggable=[1]\n")
        assertTrue(RootDetector(buildProps = tmpFile, logger = NoopLogger).checkBuildProps())
    }

    /**
     * Verifies that the ro.secure build property triggers root detection
     */
    @Test
    fun checkBuildPropsRootedSecure() {
        val tmpFile = Files.createTempFile("rooted", ".prop").toFile()
        tmpFile.writeText(SAMPLE_BUILD_PROPS)
        tmpFile.appendText("\nro.secure=[0]\n")
        assertTrue(RootDetector(buildProps = tmpFile, logger = NoopLogger).checkBuildProps())
    }

    companion object {
        // write a sample /system/build.prop taken from an API 21 emulator
        val SAMPLE_BUILD_PROPS =
            """
            # begin build properties
            # autogenerated by buildinfo.sh
            ro.build.id=LSY66K
            ro.build.display.id=sdk_google_phone_x86-eng 5.0.2 LSY66K 6695550 test-keys
            ro.build.version.incremental=6695550
            ro.build.version.sdk=21
            ro.build.version.codename=REL
            ro.build.version.all_codenames=REL
            ro.build.version.release=5.0.2
            ro.build.date=Tue Jul 21 02:33:27 UTC 2020
            ro.build.date.utc=1595298807
            ro.build.type=eng
            ro.build.user=android-build
            ro.build.host=abfarm-01195
            ro.build.tags=test-keys
            ro.product.model=Android SDK built for x86
            ro.product.brand=generic_x86
            ro.product.name=sdk_google_phone_x86
            ro.product.device=generic_x86
            ro.product.board=
            # ro.product.cpu.abi and ro.product.cpu.abi2 are obsolete,
            # use ro.product.cpu.abilist instead.
            ro.product.cpu.abi=x86
            ro.product.cpu.abilist=x86
            ro.product.cpu.abilist32=x86
            ro.product.cpu.abilist64=
            ro.product.manufacturer=unknown
            ro.product.locale.language=en
            ro.product.locale.region=US
            ro.wifi.channels=
            ro.board.platform=
            # ro.build.product is obsolete; use ro.product.device
            ro.build.product=generic_x86
            # Do not try to parse description, fingerprint, or thumbprint
            ro.build.description=sdk_google_phone_x86-eng 5.0.2 LSY66K 6695550 test-keys
            ro.build.fingerprint=generic_x86/sdk_google_phone_x86/generic_x86:5.0.2/LSY66K/6695550:eng/test-keys
            ro.build.characteristics=default
            # end build properties
            #
            # from build/target/board/generic_x86/system.prop
            #
            #
            # system.prop for generic sdk
            #

            rild.libpath=/system/lib/libreference-ril.so
            rild.libargs=-d /dev/ttyS0

            #
            # ADDITIONAL_BUILD_PROPERTIES
            #
            ro.config.notification_sound=OnTheHunt.ogg
            ro.config.alarm_alert=Alarm_Classic.ogg
            persist.sys.dalvik.vm.lib.2=libart.so
            dalvik.vm.isa.x86.features=default
            ro.kernel.android.checkjni=1
            xmpp.auto-presence=true
            ro.config.nocheckin=yes
            net.bt.name=Android
            dalvik.vm.stack-trace-file=/data/anr/traces.txt
            """.trimIndent()
    }
}
