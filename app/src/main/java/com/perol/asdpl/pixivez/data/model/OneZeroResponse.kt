/*
 * MIT License
 *
 * Copyright (c) 2019 Perol_Notsfsssf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 */

package com.perol.asdpl.pixivez.data.model

import kotlinx.serialization.SerialName

data class OneZeroResponse(

    @SerialName("status") val status: Int,
    @SerialName("tC") val tC: Boolean,
    @SerialName("rD") val rD: Boolean,
    @SerialName("rA") val rA: Boolean,
    @SerialName("aD") val aD: Boolean,
    @SerialName("cD") val cD: Boolean,
    @SerialName("question") val question: List<Question>,
    @SerialName("answer") val answer: List<Answer>
)

data class Question(

    @SerialName("name") val name: String,
    @SerialName("type") val type: Int
)

data class Answer(

    @SerialName("name") val name: String,
    @SerialName("type") val type: Int,
    @SerialName("tTL") val tTL: Int,
    @SerialName("data") val data: String
)
