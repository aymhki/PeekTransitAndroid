package com.aymanhki.peektransit.data.models

sealed class TransitError : Exception() {
    object InvalidURL : TransitError()
    data class NetworkError(val error: Throwable) : TransitError()
    object InvalidResponse : TransitError()
    object InvalidData : TransitError()
    object ServiceDown : TransitError()
    data class ParseError(override val message: String) : TransitError()
    data class BatchProcessingError(override val message: String) : TransitError()
    data class Custom(override val message: String) : TransitError()
    
    companion object {
        val maxRetriesExceeded = Custom("Maximum retry attempts exceeded")
    }
    
    override val message: String
        get() = when (this) {
            is InvalidURL -> "Invalid URL configuration"
            is NetworkError -> "Network error: ${error.message}"
            is InvalidResponse -> "Invalid response from server"
            is InvalidData -> "Invalid data received"
            is ServiceDown -> "Transit service is currently unavailable"
            is ParseError -> "Data parsing error: $message"
            is BatchProcessingError -> "Error processing stops: $message"
            is Custom -> message
        }
}