dataSource {
    driverClassName = "org.h2.Driver"
    username = "sa"
    password = ""
    dbCreate = 'create-drop'
}

hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = true
    cache.provider_class = 'net.sf.ehcache.hibernate.EhCacheProvider'
    flush.mode='auto'
}