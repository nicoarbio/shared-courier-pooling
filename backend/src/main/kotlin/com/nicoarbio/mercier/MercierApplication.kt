package com.nicoarbio.mercier

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class MercierApplication

fun main(args: Array<String>) {
	runApplication<MercierApplication>(*args)
}
