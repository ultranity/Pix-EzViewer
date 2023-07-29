package com.perol.asdpl.pixivez.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param AD If true, it means that every record in the answer was verified with DNSSEC.
 * @param CD If true, the client asked to disable DNSSEC validation. In this case, Cloudflare will still fetch the DNSSEC-related records, but it will not attempt to validate the records.
 * @param RA If true, it means the Recursion Available bit was set. This is always set to true for Cloudflare DNS over HTTPS.
 * @param RD If true, it means the Recursive Desired bit was set. This is always set to true for Cloudflare DNS over HTTPS.
 * @param Status The Response Code of the DNS Query. These are defined here: https://www.iana.org/assignments/dns-parameters/dns-parameters.xhtml#dns-parameters-6.
 * @param TC If true, it means the truncated bit was set. This happens when the DNS answer is larger than a single UDP or TCP packet. TC will almost always be false with Cloudflare DNS over HTTPS because Cloudflare supports the maximum response size.
 */
@Serializable
data class DnsQueryResponse(
    val AD: Boolean = false,
    @SerialName("Answer")
    val answer: List<Answer> = listOf(),
    @SerialName("Authority")
    val authority: List<Authority> = listOf(),
    val CD: Boolean = false,
    @SerialName("Question")
    val question: List<Question> = listOf(),
    val RA: Boolean = false,
    val RD: Boolean = false,
    val Status: Int = 0,
    val TC: Boolean = false
) {

    /**
     * @param data The value of the DNS record for the given name and type. The data will be in text for standardized record types and in hex for unknown types.
     * @param name The record owner.
     * @param TTL The number of seconds the answer can be stored in cache before it is considered stale.
     * @param type The type of DNS record. These are defined here: https://www.iana.org/assignments/dns-parameters/dns-parameters.xhtml#dns-parameters-4.
     */
    @Serializable
    data class Answer(
        val data: String = "",
        val name: String = "",
        val TTL: Int = 0,
        val type: Int = 0
    )

    @Serializable
    data class Authority(
        val data: String = "",
        val name: String = "",
        val TTL: Int = 0,
        val type: Int = 0
    )

    /**
     * @param name The record name requested.
     * @param type The type of DNS record requested. These are defined here: https://www.iana.org/assignments/dns-parameters/dns-parameters.xhtml#dns-parameters-4.
     */
    @Serializable
    data class Question(
        val name: String = "",
        val type: Int = 0
    )
}
