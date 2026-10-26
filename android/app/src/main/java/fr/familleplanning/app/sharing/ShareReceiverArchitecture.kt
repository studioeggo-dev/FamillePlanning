package fr.familleplanning.app.sharing

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import java.time.Instant

/**
 * =======================================================================
 * ARCHITECTURE DU FUTUR RÉCEPTEUR DE PARTAGE ANDROID (Prévu Phase 3C)
 * =======================================================================
 *
 * Cette classe documente et pose les bases architecturales de l'interception
 * du menu de partage natif d'Android (Android Sharesheet).
 *
 * CONTRATS DE PARTAGE PRÉVUS :
 * 1. Intent.ACTION_SEND :
 *    - Texte simple (ex: SMS transféré, email, message WhatsApp) via Intent.EXTRA_TEXT.
 *    - Fichier unique (capture d'écran, photo d'affiche, PDF) via Intent.EXTRA_STREAM et Uri.
 *
 * 2. Intent.ACTION_SEND_MULTIPLE :
 *    - Fichiers multiples (plusieurs photos de documents scolaires) via Intent.EXTRA_STREAM.
 *
 * 3. ContentResolver & Sécurité :
 *    - Résolution sécurisée via context.contentResolver.openInputStream(uri).
 *    - Gestion stricte des autorisations temporaires (FLAG_GRANT_READ_URI_PERMISSION).
 *
 * 4. Découplage strict (Ne déclenche aucun LLM/Gemini en Phase 3A) :
 *    - Cette couche produit uniquement un objet [SharedIncomingPayload].
 *    - L'analyse et l'extraction d'événement seront déclenchées dans les phases ultérieures.
 */

sealed class SharedIncomingPayload {
    data class TextPayload(
        val text: String,
        val receivedAt: Instant = Instant.now()
    ) : SharedIncomingPayload()

    data class MediaPayload(
        val uri: Uri,
        val mimeType: String,
        val receivedAt: Instant = Instant.now()
    ) : SharedIncomingPayload()

    data class MultipleMediaPayload(
        val uris: List<Uri>,
        val mimeTypes: List<String>,
        val receivedAt: Instant = Instant.now()
    ) : SharedIncomingPayload()
}

/**
 * Contrat d'architecture pour le futur analyseur de partage.
 * Interface purement déclarative pour la Phase 3A.
 */
interface ISharePayloadExtractor {
    fun extractFromIntent(intent: Intent, contentResolver: ContentResolver): SharedIncomingPayload?
    fun isSupportedMimeType(mimeType: String?): Boolean
}

/**
 * Types MIME ciblés pour FamillePlanning :
 * - text/plain (SMS, courriels, notes)
 * - image/* (PNG, JPEG, HEIC : affiches de rentrée, flyers périscolaires, convocations)
 * - application/pdf (bulletins d'inscription, devis club sportif)
 */
object SupportedMimeTypes {
    const val TEXT_PLAIN = "text/plain"
    const val IMAGE_ANY = "image/*"
    const val APPLICATION_PDF = "application/pdf"

    val ALL_SUPPORTED = listOf(TEXT_PLAIN, IMAGE_ANY, APPLICATION_PDF)
}
