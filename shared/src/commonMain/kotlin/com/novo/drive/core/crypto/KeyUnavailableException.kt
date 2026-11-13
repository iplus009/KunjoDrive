package com.novo.drive.core.crypto

class KeyUnavailableException(name: String) :
    IllegalStateException("Key '$name' cannot be unwrapped on this device")
