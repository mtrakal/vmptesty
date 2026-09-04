package cz.mtrakal.vmptesty

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform