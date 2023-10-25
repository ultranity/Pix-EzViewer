package com.perol.asdpl.pixivez.data.model

import kotlinx.serialization.Serializable

@Serializable
class UserFollowDetail(
    val follow_detail: FollowDetail
)

@Serializable
class FollowDetail(
    val is_followed: Boolean = false,
    val restrict: String = Restrict.PUBLIC.value
)
