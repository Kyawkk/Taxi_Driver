package com.codetest.taxidriver.model

import com.google.firebase.firestore.GeoPoint

data class Customer(
    val name: String,
    val phone: String,
    val latLog: GeoPoint,
    val customerPhoto: String
): java.io.Serializable
