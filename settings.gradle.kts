plugins {
    id("com.gradleup.nmcp.settings") version "1.6.2"
}

nmcpSettings {
    centralPortal {
        username = providers.environmentVariable("CENTRAL_USERNAME").orElse("").get()
        password = providers.environmentVariable("CENTRAL_PASSWORD").orElse("").get()
        publishingType = "USER_MANAGED"
    }
}

rootProject.name = "pebble-forge"

include("pebble-forge-core")
include("pebble-forge-spring")
include("pebble-forge-spring-boot-4-starter")
include("pebble-forge-demo")
