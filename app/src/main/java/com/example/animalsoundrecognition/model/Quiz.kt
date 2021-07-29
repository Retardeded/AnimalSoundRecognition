package com.example.animalsoundrecognition.model


class Quiz(title:String, text:String, options:List<String>, answer:List<Int>) {
    private val id: Int? = null
    private val title: String? = title
    private val text: String? = text
    private val options: List<String>? = options
    private val answer: List<Int>? = answer
    override fun toString(): String {
        return "Quiz(id=$id, title=$title, text=$text, options=$options, answer=$answer)"
    }
}