package com.softmotions.ncms.rs

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kevinsawicki.http.HttpRequest
import com.github.kevinsawicki.http.HttpRequest.get
import com.softmotions.commons.JVMResources
import com.softmotions.ncms.WebBaseTest
import com.softmotions.web.security.XMLWSUserDatabase
import com.softmotions.web.security.tomcat.WSUserDatabaseRealm
import com.softmotions.weboot.testing.tomcat.TomcatRunner
import org.apache.commons.lang3.RandomStringUtils
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * @author Tyutyunkov Vyacheslav (tve@softmotions.com)
 */
@Test(groups = arrayOf("rs"))
class TestMttRulesRS : WebBaseTest() {

    @BeforeClass
    fun setup() {
        System.getProperty("liquibase.dropAll") ?: System.setProperty("liquibase.dropAll", "true")
        System.getProperty("ncmstest.ds") ?: System.setProperty("ncmstest.ds", "${System.getProperty("user.home")}/.ncms-test.ds")
        System.setProperty("WEBOOT_CFG_LOCATION", "com/softmotions/ncms/rs/cfg/test-ncms-rs-conf.xml")
        setupWeb()
        runner!!.start()
    }

    override fun configureTomcatRunner(b: TomcatRunner.Builder) {
        super.configureTomcatRunner(b)
        val wsdb = XMLWSUserDatabase("WSUserDatabase", "com/softmotions/ncms/rs/cfg/users.xml", false)
        JVMResources.set(wsdb.databaseName, wsdb);
        b.withRealm(WSUserDatabaseRealm(wsdb))
    }

    @AfterClass
    fun shutdown() {
        shutdownWeb()
    }

    override fun R(resource: String): String = super.R(resource = "/rs/adm/mtt/rules$resource")

    private val mapper = ObjectMapper()

    @Test(priority = 0)
    fun testRulesSelect() {
        with(auth(get(R("/select/count")))) {
            assertEquals(200, code())
            assertEquals("0", body())
        }

        with(auth(get(R("/select")))) {
            assertEquals(200, code())
            val body = body()
            assertNotNull(body)
            with(mapper.readTree(body)) {
                assertTrue(isArray)
                assertEquals(0, size())
            }
        }
    }

    @Test(dependsOnMethods = arrayOf("testRulesSelect"))
    fun testRuleCreate() {
        with(PUT("/rule/${RandomStringUtils.randomAlphanumeric(5)}")) {
            assertEquals(200, code())
            val body = body()
            assertNotNull(body)
            with(mapper.readTree(body)) {
                assertTrue(hasNonNull("id"))
            }
        }
        with(GET("/select/count")) {
            assertEquals(200, code())
            assertEquals("1", body())
        }
    }

    @Test(dependsOnMethods = arrayOf("testRuleCreate"))
    fun testRuleDelete() {
        with(GET("/select")) {
            assertEquals(200, code())
            val body = body()
            assertNotNull(body)
            with(mapper.readTree(body)) {
                assertTrue(isArray)
                forEach {
                    assertTrue(it.hasNonNull("id"))
                    with(DELETE("/rule/${it.path("id").asLong()}")) {
                        assertEquals(200, code())
                    }
                }
            }
        }

        with(GET("/select/count")) {
            assertEquals(200, code())
            assertEquals("0", body())
        }
    }

    @Test(dependsOnMethods = arrayOf("testRuleCreate", "testRuleDelete"))
    fun testRuleSearch() {
        val rname = RandomStringUtils.randomAlphanumeric(8);
        with(PUT("/rule/$rname")) {
            assertEquals(200, code())
            with(mapper.readTree(body())) {
                assertTrue(hasNonNull("id"))
                val rid = path("id").asLong()

                assertEquals("1", GET("/select/count").body())
                assertEquals("0", GET("/select/count?stext=A$rname").body())
                assertEquals("1", GET("/select/count?stext=$rname").body())
                with(GET("/select?stext=$rname")) {
                    assertEquals(200, code())
                    with(mapper.readTree(body())) {
                        assertTrue(isArray)
                        assertEquals(1, size())
                        with(get(0)) {
                            assertEquals(rid, path("id").asLong())
                            assertEquals(rname, path("name").asText())
                        }
                    }
                }
                assertEquals(200, DELETE("/rule/$rid").code())
            }
        }
    }

    @Test(priority = 1000, dependsOnMethods = arrayOf("testRuleDelete"))
    fun testFiltersSelect() {
        with(PUT("/rule/${RandomStringUtils.randomAlphanumeric(6)}")) {
            assertEquals(200, code())
            val cbody = body()
            assertNotNull(cbody)
            with(mapper.readTree(cbody)) {
                assertTrue(isObject)
                assertTrue(hasNonNull("id"))

                val rid = path("id").asLong()
                with(auth(HttpRequest.get(R("/rule/$rid/filters/select/count")))) {
                    assertEquals(200, code())
                    assertEquals("0", body())
                }

                with(auth(HttpRequest.get(R("/rule/$rid/filters/select")))) {
                    assertEquals(200, code())
                    val body = body();
                    assertNotNull(body)
                    with(mapper.readTree(body)) {
                        assertTrue(isArray)
                        assertEquals(0, size())
                    }
                }

                with(DELETE("/rule/$rid")) {
                    assertEquals(200, code())
                }
            }
        }
    }
}