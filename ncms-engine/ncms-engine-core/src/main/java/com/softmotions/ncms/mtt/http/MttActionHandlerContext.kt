package com.softmotions.ncms.mtt.http

import com.fasterxml.jackson.databind.node.ObjectNode
import com.softmotions.ncms.mtt.MttRule
import com.softmotions.ncms.mtt.MttRuleAction
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
interface MttActionHandlerContext : MutableMap<String, Any?> {

    val spec: ObjectNode

    val rule: MttRule

    val action: MttRuleAction

    fun execute(req: HttpServletRequest, resp: HttpServletResponse): Boolean

    fun findGroupActions(): List<MttActionHandlerContext>
}