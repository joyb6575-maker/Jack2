package com.example

import com.example.data.action.ActionPlanner
import com.example.model.ActionStatus
import com.example.model.PhoneAction
import com.example.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JackAssistantTest {

    private lateinit var planner: ActionPlanner

    @Before
    fun setUp() {
        planner = ActionPlanner()
    }

    @Test
    fun testParseSettingsIntent() {
        val action = planner.parseBengaliIntent("ওয়াইফাই সেটিংস খোলো")
        assertNotNull(action)
        assertTrue(action is PhoneAction.OpenSettings)
        assertEquals("wifi", (action as PhoneAction.OpenSettings).settingType)
    }

    @Test
    fun testParseTimerIntent() {
        val action = planner.parseBengaliIntent("১০ মিনিট টাইমার সেট করো")
        assertNotNull(action)
        assertTrue(action is PhoneAction.SetTimer)
        assertEquals(10, (action as PhoneAction.SetTimer).minutes)
    }

    @Test
    fun testParseAppIntent() {
        val action = planner.parseBengaliIntent("ইউটিউব খোলো")
        assertNotNull(action)
        assertTrue(action is PhoneAction.OpenApp)
        assertEquals("YouTube", (action as PhoneAction.OpenApp).appName)
    }

    @Test
    fun testParseDialIntent() {
        val action = planner.parseBengaliIntent("কল করো +8801700000000")
        assertNotNull(action)
        assertTrue(action is PhoneAction.DialNumber)
        assertEquals("+8801700000000", (action as PhoneAction.DialNumber).phoneNumber)
    }

    @Test
    fun testParseMemoryIntent() {
        val action = planner.parseBengaliIntent("মনে রাখো আমার প্রিয় রঙ নীল")
        assertNotNull(action)
        assertTrue(action is PhoneAction.SaveMemory)
        assertEquals("আমার প্রিয় রঙ নীল", (action as PhoneAction.SaveMemory).value)
    }

    @Test
    fun testRiskClassificationLowRisk() {
        val action = PhoneAction.OpenApp(appName = "Camera")
        val plan = planner.planAction(action)

        assertEquals(RiskLevel.LOW, plan.riskLevel)
        assertFalse(plan.requiresConfirmation)
        assertEquals(ActionStatus.CONFIRMED, plan.status)
    }

    @Test
    fun testRiskClassificationHighRisk() {
        val action = PhoneAction.DialNumber(phoneNumber = "01711223344", contactName = "রহিম")
        val plan = planner.planAction(action)

        assertEquals(RiskLevel.HIGH, plan.riskLevel)
        assertTrue(plan.requiresConfirmation)
        assertEquals(ActionStatus.PENDING, plan.status)
    }
}
