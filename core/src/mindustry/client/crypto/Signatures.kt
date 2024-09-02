package mindustry.client.crypto

import mindustry.client.*
import mindustry.client.communication.*
import java.math.*
import java.security.*
import java.security.cert.*
import java.time.*
import java.util.concurrent.atomic.*
import kotlin.math.*

class Signatures(private val store: KeyStorage, private val ntp: AtomicReference<Clock>) {
    companion object {
        private val signature by lazy { Signature.getInstance("ed448", "BC") }
        const val SIGNATURE_LENGTH = 114
        const val SIGNATURE_EXPIRY_SECONDS = 10

        fun rawVerify(original: ByteArray, signatureBytes: ByteArray, publicKey: PublicKey): Boolean {
            return synchronized(signature) {
                try {
                    signature.initVerify(publicKey)
                    signature.update(original)
                    signature.verify(signatureBytes)
                } catch (e: Exception) {
                    false
                }
            }
        }

        fun rawSign(byteArray: ByteArray, key: PrivateKey): ByteArray {
            return synchronized(signature) {
                signature.initSign(key)
                signature.update(byteArray)
                signature.sign()
            }
        }
    }

    fun sign(inp: ByteArray): ByteArray? = store.key()?.run { rawSign(inp, this) }

    fun verify(original: ByteArray, signature: ByteArray, certSN: ByteArray) = Main.keyStorage.findTrusted(BigInteger(certSN))?.run { (if (rawVerify(original, signature, this.publicKey)) VerifyResult.VALID else VerifyResult.INVALID) to this } ?: (VerifyResult.UNKNOWN_CERT to null)

    fun signatureTransmission(original: ByteArray, commsId: Int, messageId: Short): SignatureTransmission? {
        val cert = store.cert() ?: return null
        val key = store.key() ?: return null
        val time = ntp.get().instant().toEpochMilli()
        val baseTransmission = SignatureTransmission(ByteArray(SIGNATURE_LENGTH), cert.serialNumber, time, commsId, messageId)
        val signature = rawSign(baseTransmission.toSignable(original), key)
        return SignatureTransmission(signature, cert.serialNumber, time, commsId, messageId)
    }

    enum class VerifyResult {
        INVALID, VALID, UNKNOWN_CERT
    }

    fun verifySignatureTransmission(original: ByteArray, transmission: SignatureTransmission): Pair<VerifyResult, X509Certificate?> {
        val foundCert = store.findTrusted(transmission.sn) ?: return Pair(VerifyResult.UNKNOWN_CERT, null)
        if (foundCert == Main.keyStorage.cert()) return Pair(VerifyResult.UNKNOWN_CERT, null)

        // the time is synchronized to NTP on both sides so this is fine
        if (abs(transmission.time - ntp.get().instant().toEpochMilli()) > SIGNATURE_EXPIRY_SECONDS * 1000) return Pair(VerifyResult.INVALID, foundCert)
        val signedValue = transmission.toSignable(original)
        val valid = rawVerify(signedValue, transmission.signature, foundCert.publicKey)
        return Pair(if (valid) VerifyResult.VALID else VerifyResult.INVALID, foundCert)
    }
}
