// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\magic\MagicExceptions.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model.magic


sealed class MagicException(message: String) : Exception(message)

class ResourceExhaustedException(msg: String) : MagicException(msg)
class PreparationViolationException(msg: String) : MagicException(msg)
class InvalidSourceException(msg: String) : MagicException(msg)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\magic\MagicExceptions.kt