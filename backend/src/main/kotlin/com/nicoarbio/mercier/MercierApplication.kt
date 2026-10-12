package com.nicoarbio.mercier

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MercierApplication

fun main(args: Array<String>) {
	runApplication<MercierApplication>(*args)
}
