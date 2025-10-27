package com.example.mobilebugsgame

import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.TextContent
import com.tickaroo.tikxml.annotation.Xml

@Xml(name = "Metall")
data class MetallResponse(
    @Attribute(name = "FromDate")
    val fromDate: String = "",

    @Attribute(name = "ToDate")
    val toDate: String = "",

    @Attribute(name = "name")
    val name: String = "",

    @Element(name = "Record")
    val records: List<MetallRecord>? = null
)

@Xml(name = "Record")
data class MetallRecord(
    @Attribute(name = "Date")
    val date: String? = null,

    @Attribute(name = "Code")
    val code: String? = null,

    @Element(name = "Buy")
    val buy: BuyElement? = null,

    @Element(name = "Sell")
    val sell: SellElement? = null
)

@Xml(name = "Buy")
data class BuyElement(
    @TextContent
    val value: String? = null
)

@Xml(name = "Sell")
data class SellElement(
    @TextContent
    val value: String? = null
)
