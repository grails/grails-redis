package com.example

trait ProxyAwareSpec {


    Serializable getEntityId(Object obj) {
        Serializable identifier = proxyHandler.unwrapIfProxy(obj)?.ident()
        identifier ?: obj?.id
    }

}