package com.back.global.file

import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

@Component
class LocalFileStorage(
    @Value("\${custom.upload.path}") private val uploadPath: String
) : FileStorage {

    companion object {
        private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
        private const val MAX_FILE_SIZE = 5 * 1024 * 1024L
    }

    override fun store(file: MultipartFile, subDirectory: String): String {
        if (file.isEmpty) throw ServiceException(ErrorCode.FILE_EMPTY)
        if (file.size > MAX_FILE_SIZE) throw ServiceException(ErrorCode.FILE_TOO_LARGE)

        val ext = file.originalFilename
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?.takeIf { it in ALLOWED_EXTENSIONS }
            ?: throw ServiceException(ErrorCode.FILE_INVALID_TYPE)

        val filename = "${UUID.randomUUID()}.$ext"
        val targetDir = Path.of(uploadPath, subDirectory)
        Files.createDirectories(targetDir)
        file.transferTo(targetDir.resolve(filename))

        return "$subDirectory/$filename"
    }

    override fun delete(storedPath: String) {
        Files.deleteIfExists(Path.of(uploadPath, storedPath))
    }
}