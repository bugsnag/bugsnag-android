package com.bugsnag.android

class Error(
    var errorClass: String,
    var errorMessage: String,
    var stackframe: List<Stackframe>
)
