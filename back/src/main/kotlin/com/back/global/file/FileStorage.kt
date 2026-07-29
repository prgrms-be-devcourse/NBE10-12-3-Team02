package com.back.global.file

import org.springframework.web.multipart.MultipartFile

interface FileStorage {
    fun store(file: MultipartFile, subDirectory: String): String
    fun delete(storedPath: String)
}
