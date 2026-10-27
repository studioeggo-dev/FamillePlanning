package fr.familleplanning.app.data.models

import java.time.Instant

/**
 * Exception spécifique levée lors du décodage d'un document Firestore malformé.
 */
class InvalidDocumentException(message: String) : Exception(message)

/**
 * Adaptateurs de types stricts pour Cloud Firestore.
 * INTERDICTION : Ne jamais remplacer silencieusement une date obligatoire manquante par Instant.now().
 */
object FirestoreTypeAdapters {

    /**
     * Tente de parser un instant obligatoire depuis une liste de clés possibles.
     * @throws InvalidDocumentException si aucune valeur n'est présente ou si le format est invalide.
     */
    fun parseRequiredInstant(fieldName: String, map: Map<String, Any?>, vararg keys: String): Instant {
        for (key in keys) {
            val value = map[key] ?: continue
            val parsed = tryParseInstant(value)
            if (parsed != null) return parsed
        }
        throw InvalidDocumentException("Champ date obligatoire '$fieldName' absent ou invalide dans le document Firestore")
    }

    /**
     * Tente de parser un instant facultatif depuis une liste de clés possibles.
     * Retourne null si la date est absente ou illisible, sans lever d'exception.
     */
    fun parseOptionalInstant(map: Map<String, Any?>, vararg keys: String): Instant? {
        for (key in keys) {
            val value = map[key] ?: continue
            val parsed = tryParseInstant(value)
            if (parsed != null) return parsed
        }
        return null
    }

    private fun tryParseInstant(value: Any): Instant? {
        return when (value) {
            is com.google.firebase.Timestamp -> value.toDate().toInstant()
            is java.util.Date -> value.toInstant()
            is Long -> Instant.ofEpochMilli(value)
            is Number -> Instant.ofEpochMilli(value.toLong())
            is String -> try { Instant.parse(value) } catch (e: Exception) { null }
            else -> null
        }
    }
}

/**
 * Profil utilisateur dans Firestore: users/{uid}
 * Écriture canonique : currentFamilyId, displayName, email.
 */
data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val activeFamilyId: String? = null,
    val createdAt: Instant = Instant.now()
) {
    /**
     * Écriture canonique exacte conforme au schéma Web
     */
    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "email" to email,
            "displayName" to displayName,
            "photoUrl" to photoUrl,
            "currentFamilyId" to activeFamilyId,
            "createdAt" to com.google.firebase.Timestamp(createdAt.epochSecond, createdAt.nano)
        )
    }

    companion object {
        fun fromMap(uid: String, map: Map<String, Any?>): User {
            val email = map["email"] as? String ?: ""
            val name = map["displayName"] as? String ?: map["name"] as? String ?: ""
            val createdAt = FirestoreTypeAdapters.parseOptionalInstant(map, "createdAt") ?: Instant.now()

            return User(
                uid = uid,
                email = email,
                displayName = name,
                photoUrl = map["photoUrl"] as? String ?: map["avatarUrl"] as? String,
                activeFamilyId = (map["currentFamilyId"] as? String)
                    ?.takeIf { it.isNotBlank() }
                    ?: (map["activeFamilyId"] as? String)?.takeIf { it.isNotBlank() }
                    ?: (map["familyIds"] as? List<*>)?.filterIsInstance<String>()?.firstOrNull { it.isNotBlank() },
                createdAt = createdAt
            )
        }
    }
}

/**
 * Foyer familial dans Firestore: families/{familyId}
 * Écriture canonique : name, createdBy, timezone, createdAt.
 */
data class Family(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val timezone: String = "Europe/Paris",
    val createdAt: Instant = Instant.now()
) {
    /**
     * Écriture canonique Web sans clés doublons
     */
    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "createdBy" to ownerId,
            "timezone" to timezone,
            "createdAt" to com.google.firebase.Timestamp(createdAt.epochSecond, createdAt.nano)
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Family {
            val familyName = map["name"] as? String
                ?: throw InvalidDocumentException("Champ obligatoire 'name' introuvable pour le foyer $id")

            val owner = map["createdBy"] as? String ?: map["ownerId"] as? String ?: map["owner"] as? String ?: ""
            val tz = map["timezone"] as? String ?: map["timeZone"] as? String ?: "Europe/Paris"
            val createdAt = FirestoreTypeAdapters.parseOptionalInstant(map, "createdAt") ?: Instant.now()

            return Family(
                id = id,
                name = familyName,
                ownerId = owner,
                timezone = tz,
                createdAt = createdAt
            )
        }
    }
}

/**
 * Droit d'accès au foyer: families/{familyId}/access/{uid}
 * Rôles canoniques : "owner", "admin", "member", "viewer".
 */
data class FamilyAccess(
    val uid: String = "",
    val role: String = "member",
    val joinedAt: Instant = Instant.now()
) {
    val isOwnerOrAdmin: Boolean get() = role == "owner" || role == "admin"
    val isMemberOrHigher: Boolean get() = role == "owner" || role == "admin" || role == "member"

    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "role" to role,
            "joinedAt" to com.google.firebase.Timestamp(joinedAt.epochSecond, joinedAt.nano)
        )
    }

    companion object {
        fun fromMap(uid: String, map: Map<String, Any?>): FamilyAccess {
            val userRole = map["role"] as? String ?: "member"
            val joinedAt = FirestoreTypeAdapters.parseOptionalInstant(map, "joinedAt") ?: Instant.now()

            return FamilyAccess(
                uid = uid,
                role = userRole,
                joinedAt = joinedAt
            )
        }
    }
}

/**
 * Membre du foyer: families/{familyId}/members/{memberId}
 * Écriture canonique : name, avatarUrl, color, role, isChild, birthDate.
 */
data class FamilyMember(
    val id: String = "",
    val firstName: String = "",
    val avatarUrl: String? = null,
    val colorHex: String = "#FF6B6B",
    val roleName: String? = null,
    val isChild: Boolean = false,
    val birthDate: String? = null
) {
    /**
     * Écriture canonique exacte Web
     */
    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "name" to firstName,
            "avatarUrl" to avatarUrl,
            "color" to colorHex,
            "role" to roleName,
            "isChild" to isChild,
            "birthDate" to birthDate
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): FamilyMember {
            val name = map["name"] as? String ?: map["firstName"] as? String
                ?: throw InvalidDocumentException("Champ obligatoire 'name' introuvable pour le membre $id")

            val color = map["color"] as? String ?: map["colorHex"] as? String ?: "#FF6B6B"
            val role = map["role"] as? String ?: map["roleName"] as? String

            return FamilyMember(
                id = id,
                firstName = name,
                avatarUrl = map["avatarUrl"] as? String ?: map["photoUrl"] as? String,
                colorHex = color,
                roleName = role,
                isChild = map["isChild"] as? Boolean ?: (map["type"] == "child"),
                birthDate = map["birthDate"] as? String
            )
        }
    }
}

/**
 * Événement du planning familial: families/{familyId}/events/{eventId}
 * Écriture canonique Web : familyId, title, description, startAt, endAt, allDay, participantIds, category, color, location, createdBy, createdAt.
 */
data class CalendarEvent(
    val id: String = "",
    val familyId: String = "",
    val title: String = "",
    val description: String = "",
    val startInstant: Instant = Instant.now(),
    val endInstant: Instant = Instant.now(),
    val isAllDay: Boolean = false,
    val participantIds: List<String> = emptyList(),
    val category: String = "general",
    val colorHex: String = "#FF6B6B",
    val location: String? = null,
    val createdBy: String = "",
    val createdAt: Instant = Instant.now()
) {
    /**
     * Écriture canonique exacte conforme au schéma Web Firestore
     */
    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "familyId" to familyId,
            "title" to title,
            "description" to description,
            "startAt" to com.google.firebase.Timestamp(startInstant.epochSecond, startInstant.nano),
            "endAt" to com.google.firebase.Timestamp(endInstant.epochSecond, endInstant.nano),
            "allDay" to isAllDay,
            "participantIds" to participantIds,
            "category" to category,
            "color" to colorHex,
            "location" to location,
            "createdBy" to createdBy,
            "createdAt" to com.google.firebase.Timestamp(createdAt.epochSecond, createdAt.nano)
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): CalendarEvent {
            val eventTitle = map["title"] as? String ?: map["name"] as? String
                ?: throw InvalidDocumentException("Champ obligatoire 'title' introuvable pour l'événement $id")

            // Parsing strict des dates d'événement (lève InvalidDocumentException si manquante ou invalide)
            val startInstant = FirestoreTypeAdapters.parseRequiredInstant(
                fieldName = "startAt",
                map = map,
                keys = arrayOf("startAt", "startInstant", "startDate", "startTime")
            )

            val endInstant = FirestoreTypeAdapters.parseRequiredInstant(
                fieldName = "endAt",
                map = map,
                keys = arrayOf("endAt", "endInstant", "endDate", "endTime")
            )

            @Suppress("UNCHECKED_CAST")
            val participants = (map["participantIds"] ?: map["participants"] ?: map["assignedTo"]) as? List<String>
                ?: emptyList()

            val createdInstant = FirestoreTypeAdapters.parseOptionalInstant(map, "createdAt") ?: startInstant

            return CalendarEvent(
                id = id,
                familyId = map["familyId"] as? String ?: "",
                title = eventTitle,
                description = map["description"] as? String ?: "",
                startInstant = startInstant,
                endInstant = endInstant,
                isAllDay = map["allDay"] as? Boolean ?: map["isAllDay"] as? Boolean ?: false,
                participantIds = participants,
                category = map["category"] as? String ?: "general",
                colorHex = map["color"] as? String ?: map["colorHex"] as? String ?: "#FF6B6B",
                location = map["location"] as? String,
                createdBy = map["createdBy"] as? String ?: "",
                createdAt = createdInstant
            )
        }
    }
}

/**
 * Entrée de mémoire familiale: families/{familyId}/memory/{memoryId}
 * Écriture canonique Web : content, source, status, createdAt.
 */
data class FamilyMemoryEntry(
    val id: String = "",
    val rawText: String = "",
    val sourceType: String = "manual",
    val status: String = "pending",
    val createdAt: Instant = Instant.now()
) {
    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "content" to rawText,
            "source" to sourceType,
            "status" to status,
            "createdAt" to com.google.firebase.Timestamp(createdAt.epochSecond, createdAt.nano)
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): FamilyMemoryEntry {
            val contentText = map["content"] as? String ?: map["rawText"] as? String ?: map["text"] as? String
                ?: throw InvalidDocumentException("Champ obligatoire 'content' introuvable pour la mémoire $id")

            val createdAt = FirestoreTypeAdapters.parseOptionalInstant(map, "createdAt") ?: Instant.now()

            return FamilyMemoryEntry(
                id = id,
                rawText = contentText,
                sourceType = map["source"] as? String ?: map["sourceType"] as? String ?: "manual",
                status = map["status"] as? String ?: "pending",
                createdAt = createdAt
            )
        }
    }
}
