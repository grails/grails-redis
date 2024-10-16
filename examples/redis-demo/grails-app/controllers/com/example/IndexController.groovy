package com.example

class IndexController {

    BookCreateService bookCreateService

    def index() {
        render view: "/index", model: [book:bookCreateService.createOrGetBook()]
    }

}
