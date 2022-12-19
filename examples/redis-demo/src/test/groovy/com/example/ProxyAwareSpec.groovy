package com.example

import grails.core.support.proxy.ProxyHandler

trait ProxyAwareSpec {

    ProxyHandler proxyHandler

    Serializable getEntityId(Object obj) {
        Serializable identifier = proxyHandler.getIdentifier(obj)
        identifier ?: obj?.id
    }

}