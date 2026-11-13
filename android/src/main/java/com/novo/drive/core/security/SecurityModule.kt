package com.novo.drive.core.security

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val securityModule = module {
    singleOf(::AppLockManager)
    singleOf(::VaultSecurityManager)
}
