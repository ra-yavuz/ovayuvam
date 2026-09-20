package tr.ovayuva.ovayuvam.backup

import org.json.JSONArray
import org.json.JSONObject
import tr.ovayuva.ovayuvam.domain.GeoPosition
import tr.ovayuva.ovayuvam.domain.RevealCell
import tr.ovayuva.ovayuvam.domain.VisitedCell
import tr.ovayuva.ovayuvam.location.GoalPin
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class WorldBackup(
    val visitedCells: List<VisitedCell>,
    val revealCells: List<RevealCell>,
    val goal: GoalPin?,
    val exportedMs: Long = System.currentTimeMillis(),
)

object WorldBackupCodec {
    const val MinPassphraseLength = 8
    private const val Format = "ovayuvam.encrypted-world.v1"
    private const val PlainFormat = "ovayuvam.world.v1"
    private const val Iterations = 210_000
    private const val KeyBits = 256
    private const val GcmBits = 128
    private const val SaltBytes = 16
    private const val IvBytes = 12
    private val random = SecureRandom()

    fun encrypt(backup: WorldBackup, passphrase: String): ByteArray {
        require(passphrase.length >= MinPassphraseLength) { "Passphrase must be at least $MinPassphraseLength characters." }
        val salt = ByteArray(SaltBytes).also(random::nextBytes)
        val iv = ByteArray(IvBytes).also(random::nextBytes)
        val key = keyFor(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GcmBits, iv))
        val ciphertext = cipher.doFinal(toPlainJson(backup).toByteArray(Charsets.UTF_8))
        return JSONObject()
            .put("format", Format)
            .put("cipher", "AES-256-GCM")
            .put("kdf", "PBKDF2WithHmacSHA256")
            .put("iterations", Iterations)
            .put("salt", b64(salt))
            .put("iv", b64(iv))
            .put("payload", b64(ciphertext))
            .toString(2)
            .toByteArray(Charsets.UTF_8)
    }

    fun decrypt(bytes: ByteArray, passphrase: String): WorldBackup {
        require(passphrase.length >= MinPassphraseLength) { "Passphrase must be at least $MinPassphraseLength characters." }
        val envelope = JSONObject(bytes.toString(Charsets.UTF_8))
        require(envelope.getString("format") == Format) { "Unsupported backup file." }
        require(envelope.getString("cipher") == "AES-256-GCM") { "Unsupported backup encryption." }
        val iterations = envelope.getInt("iterations")
        require(iterations == Iterations) { "Unsupported backup key settings." }
        val salt = unb64(envelope.getString("salt"))
        val iv = unb64(envelope.getString("iv"))
        val payload = unb64(envelope.getString("payload"))
        val key = keyFor(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GcmBits, iv))
        val plain = cipher.doFinal(payload).toString(Charsets.UTF_8)
        return fromPlainJson(plain)
    }

    private fun keyFor(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, Iterations, KeyBits)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    private fun toPlainJson(backup: WorldBackup): String {
        val visited = JSONArray()
        backup.visitedCells.forEach { cell ->
            visited.put(
                JSONObject()
                    .put("x", cell.x)
                    .put("y", cell.y)
                    .put("firstSeenMs", cell.firstSeenMs)
                    .put("lastSeenMs", cell.lastSeenMs)
                    .put("samples", cell.samples),
            )
        }
        val reveal = JSONArray()
        backup.revealCells.forEach { cell ->
            reveal.put(
                JSONObject()
                    .put("x", cell.x)
                    .put("y", cell.y)
                    .put("kind", cell.kind.id)
                    .put("firstSeenMs", cell.firstSeenMs)
                    .put("lastSeenMs", cell.lastSeenMs)
                    .put("samples", cell.samples),
            )
        }
        return JSONObject()
            .put("format", PlainFormat)
            .put("exportedMs", backup.exportedMs)
            .put("visitedCells", visited)
            .put("revealCells", reveal)
            .put(
                "goal",
                backup.goal?.let { goal ->
                    JSONObject()
                        .put("latitude", goal.position.latitude)
                        .put("longitude", goal.position.longitude)
                        .put("createdMs", goal.createdMs)
                } ?: JSONObject.NULL,
            )
            .toString()
    }

    private fun fromPlainJson(json: String): WorldBackup {
        val root = JSONObject(json)
        require(root.getString("format") == PlainFormat) { "Unsupported backup payload." }
        val visited = root.getJSONArray("visitedCells").mapVisited { item ->
            VisitedCell(
                x = item.getInt("x"),
                y = item.getInt("y"),
                firstSeenMs = item.getLong("firstSeenMs"),
                lastSeenMs = item.getLong("lastSeenMs"),
                samples = item.getInt("samples"),
            )
        }
        val reveal = root.getJSONArray("revealCells").mapReveal { item ->
            RevealCell(
                x = item.getInt("x"),
                y = item.getInt("y"),
                kind = RevealCell.Kind.fromId(item.getInt("kind")),
                firstSeenMs = item.getLong("firstSeenMs"),
                lastSeenMs = item.getLong("lastSeenMs"),
                samples = item.getInt("samples"),
            )
        }
        val goal = if (root.isNull("goal")) {
            null
        } else {
            val item = root.getJSONObject("goal")
            GoalPin(
                position = GeoPosition(
                    latitude = item.getDouble("latitude"),
                    longitude = item.getDouble("longitude"),
                ),
                createdMs = item.getLong("createdMs"),
            )
        }
        return WorldBackup(
            visitedCells = visited,
            revealCells = reveal,
            goal = goal,
            exportedMs = root.getLong("exportedMs"),
        )
    }

    private fun JSONArray.mapVisited(block: (JSONObject) -> VisitedCell): List<VisitedCell> =
        List(length()) { index -> block(getJSONObject(index)) }

    private fun JSONArray.mapReveal(block: (JSONObject) -> RevealCell): List<RevealCell> =
        List(length()) { index -> block(getJSONObject(index)) }

    private fun b64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun unb64(value: String): ByteArray = Base64.getDecoder().decode(value)
}
