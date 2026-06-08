package org.hyperskill.photoeditor

import android.graphics.drawable.BitmapDrawable
import org.hyperskill.photoeditor.internals.PhotoEditorUnitTest
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.RobolectricTestRunner

// version 2.0
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(RobolectricTestRunner::class)
class Stage4UnitTestB : PhotoEditorUnitTest<MainActivity>(MainActivity::class.java) {

    companion object {
        const val MESSAGE_NULL_AFTER_FILTERS = "Image was null after filters been applied"
        const val MESSAGE_WRONG_VALUES = "Wrong values after filters been applied."
        const val MARGIN_ERROR = 3
        const val CALCULATION_WAIT_TIME = 600L
    }

    @Test
    @Ignore
    fun test01_checkSliderContrast() {
        testActivity {
            slContrast
        }
    }

    @Test
    @Ignore
    fun test02_checkSliderContrastNotCrashingByDefault() {
        testActivity {
            ivPhoto
            slContrast.value += slContrast.stepSize
            slContrast.value -= slContrast.stepSize
            shadowLooper.runToEndOfTasks()
            Thread.sleep(CALCULATION_WAIT_TIME)
            shadowLooper.runToEndOfTasks()
            (ivPhoto.drawable as BitmapDrawable?)?.bitmap ?: AssertionError(MESSAGE_NULL_AFTER_FILTERS)
        }
    }

    @Test
    @Ignore
    fun test03_checkContrastOnlyWithHint() {
        testActivity {
            slBrightness
            slContrast
            ivPhoto

            val expected = Triple(129, 143, 158)

            slContrast.value -= slContrast.stepSize * 9

            shadowLooper.runToEndOfTasks()
            Thread.sleep(CALCULATION_WAIT_TIME)
            shadowLooper.runToEndOfTasks()

            val actualImage =
                (ivPhoto.drawable as BitmapDrawable?)?.bitmap ?: throw AssertionError(MESSAGE_NULL_AFTER_FILTERS)
            val actual = singleColor(actualImage, 90, 80)

            assertColorsValues(
                "$MESSAGE_WRONG_VALUES For x=90, y=80, avgBrightness=129, cAlpha=0.4782",
                expected,
                actual,
                MARGIN_ERROR
            )
        }
    }

    @Test
    fun test04_checkContrastOnlyWithoutHint() {
        testActivity {
            slBrightness
            slContrast
            ivPhoto

            val expected = Triple(131, 193, 255)

            slContrast.value += slContrast.stepSize * 9

            shadowLooper.runToEndOfTasks()
            Thread.sleep(CALCULATION_WAIT_TIME)
            shadowLooper.runToEndOfTasks()

            val actualImage =
                (ivPhoto.drawable as BitmapDrawable?)?.bitmap ?: throw AssertionError(MESSAGE_NULL_AFTER_FILTERS)
            val actual = singleColor(actualImage, 90, 80)

            assertColorsValues(
                "$MESSAGE_WRONG_VALUES For x=90, y=80",
                expected,
                actual,
                MARGIN_ERROR
            )
        }
    }

    @Test
    @Ignore
    fun test05_checkBrightnessBeforeContrastWithHint() {
        testActivity {
            slBrightness
            slContrast
            ivPhoto

            val expected = Triple(139, 155, 170)

            slBrightness.value += slBrightness.stepSize
            slContrast.value -= slContrast.stepSize * 9
            slContrast.value += slContrast.stepSize

            shadowLooper.runToEndOfTasks()
            Thread.sleep(CALCULATION_WAIT_TIME)
            shadowLooper.runToEndOfTasks()

            val actualImage = (ivPhoto.drawable as BitmapDrawable?)?.bitmap ?: throw AssertionError(
                MESSAGE_NULL_AFTER_FILTERS
            )
            val actual = singleColor(actualImage, 90, 80)
            assertColorsValues(
                "$MESSAGE_WRONG_VALUES For x=90, y=80, avgBrightness=139, cAlpha=0.5223",
                expected,
                actual,
                MARGIN_ERROR
            )
        }
    }

    @Test
    @Ignore
    fun test06_checkBrightnessBeforeContrastWithoutHint() {
        testActivity {
            slBrightness
            slContrast
            ivPhoto
            val expected = Triple(149, 177, 205)

            slBrightness.value += slBrightness.stepSize
            slBrightness.value += slBrightness.stepSize
            slContrast.value -= slContrast.stepSize

            shadowLooper.runToEndOfTasks()
            Thread.sleep(CALCULATION_WAIT_TIME)
            shadowLooper.runToEndOfTasks()

            val actualImage = (ivPhoto.drawable as BitmapDrawable?)?.bitmap ?: throw AssertionError(
                MESSAGE_NULL_AFTER_FILTERS
            )
            val actual = singleColor(actualImage, 90, 80)
            assertColorsValues("$MESSAGE_WRONG_VALUES For x=90, y=80", expected, actual, MARGIN_ERROR)
        }
    }

    @Test
    fun test07_checkBrightnessBeforeContrastWiderSample() {
        testActivity {
            slBrightness
            slContrast
            ivPhoto

            val sample = listOf(
                10 to 40,
                30 to 99,
                150 to 32,
                99 to 20,
                10 to 50,
                190 to 0,
                5 to 60,
                70 to 90,
                0 to 0,
                30 to 80
            )
            val expected = listOf(
                Triple(0, 118, 232),
                Triple(3, 253, 184),
                Triple(49, 100, 255),
                Triple(161, 72, 161),
                Triple(0, 141, 255),
                Triple(141, 26, 255),
                Triple(0, 164, 255),
                Triple(95, 232, 255),
                Triple(0, 26, 118),
                Triple(3, 210, 141),
            )

            slBrightness.value += slBrightness.stepSize
            slContrast.value += slContrast.stepSize * 9
            slContrast.value += slContrast.stepSize

            shadowLooper.runToEndOfTasks()
            Thread.sleep(CALCULATION_WAIT_TIME)
            shadowLooper.runToEndOfTasks()

            val actualImage =
                (ivPhoto.drawable as BitmapDrawable?)?.bitmap ?: throw AssertionError(MESSAGE_NULL_AFTER_FILTERS)

            for (i in sample.indices) {
                val point = sample[i]
                val actual = singleColor(actualImage, point.first, point.second)
                assertColorsValues(
                    "$MESSAGE_WRONG_VALUES For x=${point.first}, y=${point.second}",
                    expected[i], actual, MARGIN_ERROR
                )
            }
        }

    }

    @Test
    @Ignore
    fun test08_checkContrastBeforeBrightnessWithHint() {
        testActivity {
            slBrightness
            slContrast
            ivPhoto
            val expected = Triple(70, 138, 207)
            val wrongExpected = Triple(70, 100, 130)  //happens if brightness slider ignores contrast value

            slContrast.value += slContrast.stepSize * 9
            slContrast.value += slContrast.stepSize
            slBrightness.value -= slBrightness.stepSize * 6

            shadowLooper.runToEndOfTasks()
            Thread.sleep(CALCULATION_WAIT_TIME)
            shadowLooper.runToEndOfTasks()

            val actualImage =
                (ivPhoto.drawable as BitmapDrawable?)?.bitmap ?: throw AssertionError(MESSAGE_NULL_AFTER_FILTERS)
            val actual = singleColor(actualImage, 90, 80)
            val messageWrongOrder =
                if (actual == wrongExpected) " Order of slider events should not matter."
                else ""
            assertColorsValues(
                "$MESSAGE_WRONG_VALUES $messageWrongOrder For x=90, y=80, avgBrightness=70, cAlpha=2.2903",
                expected,
                actual,
                MARGIN_ERROR
            )
        }
    }

    @Test
    fun test09_checkContrastBeforeBrightnessWithoutHint() {
        testActivity {
            slBrightness
            slContrast
            ivPhoto
            val expected = Triple(141, 210, 255)
            val wrongExpected = Triple(140, 170, 200)  //happens if brightness slider ignores contrast value

            slContrast.value += slContrast.stepSize * 9
            slContrast.value += slContrast.stepSize
            slBrightness.value += slBrightness.stepSize

            shadowLooper.runToEndOfTasks()
            Thread.sleep(CALCULATION_WAIT_TIME)
            shadowLooper.runToEndOfTasks()

            val actualImage =
                (ivPhoto.drawable as BitmapDrawable?)?.bitmap ?: throw AssertionError(MESSAGE_NULL_AFTER_FILTERS)
            val actual = singleColor(actualImage, 90, 80)
            val messageWrongOrder =
                if (actual == wrongExpected) " Order of slider events should not matter."
                else ""
            assertColorsValues(
                "$MESSAGE_WRONG_VALUES $messageWrongOrder For x=90, y=80",
                expected,
                actual,
                MARGIN_ERROR
            )
        }
    }
}