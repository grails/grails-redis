package com.example

import grails.gorm.transactions.Transactional

@Transactional
class BookCreateService {
    Book createOrGetBook() {
        Book b = Book.findOrCreateByTitle('some title')
        b.save(flush:true)
    }

}
