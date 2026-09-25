package com.example.takedone

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform