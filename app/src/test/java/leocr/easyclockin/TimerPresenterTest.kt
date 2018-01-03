package leocr.easyclockin

import junit.framework.Assert.*
import org.joda.time.DateTime
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class TimerPresenterTest {

    private lateinit var timer: TimerPresenter

    @Before
    fun setUp() {
        timer = TimerPresenter()
        timer.outTime = DateTime.parse("2017-12-23T21:03:00Z")
        timer.timeToBack = DateTime.parse("2017-12-23T22:03:00Z")
    }

    @Test
    fun countTime() {
        timer.countTime { now ->
            assertNotEquals("", now)
            assertNotEquals("Almoço?", now)
        }
    }

    @Test
    fun isInTimeSuccess() {
        var result = timer.isInTime(DateTime.parse("2017-12-23T22:01:00Z"))
        val outTime = DateTime.parse("2017-12-23T21:03:00Z")
        val timeToBack = DateTime.parse("2017-12-23T22:03:00Z")
        var expected = TimerPresenter.TimeData(true, outTime, timeToBack)
        assertEquals(expected, result)

        result = timer.isInTime(DateTime.parse("2017-12-23T21:03:00Z"))
        expected = TimerPresenter.TimeData(true, outTime, timeToBack)
        assertEquals(expected, result)

        result = timer.isInTime(DateTime.parse("2017-12-23T21:02:59Z"))
        expected = TimerPresenter.TimeData(false, outTime, timeToBack)
        assertEquals(expected, result)

        result = timer.isInTime(DateTime.parse("2017-12-23T22:03:00Z"))
        expected = TimerPresenter.TimeData(false, outTime, timeToBack)
        assertEquals(expected, result)

        result = timer.isInTime(DateTime.parse("2017-12-23T22:03:01Z"))
        expected = TimerPresenter.TimeData(false, outTime, timeToBack)
        assertEquals(expected, result)
    }
}