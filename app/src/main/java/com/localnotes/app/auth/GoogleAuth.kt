package com.localnotes.app.auth

import android.app.Activity
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.localnotes.app.data.UserAccount
import org.json.JSONObject

/**
 * 用 Credential Manager 走「Sign in with Google」的 ID Token 流程。
 *
 * 需要的是一个「Web 应用」类型的 OAuth 客户端 ID（serverClientId），
 * 不是 Android 客户端 ID —— 这也是 Credential Manager 相比旧版 GoogleSignIn
 * 最省事的地方：不需要在 Google 控制台登记包名 + SHA-1。
 */
object GoogleAuth {

    suspend fun signIn(activity: Activity, serverClientId: String): UserAccount {
        require(serverClientId.isNotBlank()) { "未配置 GOOGLE_WEB_CLIENT_ID" }

        val credentialManager = CredentialManager.create(activity)

        val googleIdOption = GetGoogleIdOption.Builder()
            // false：允许用户选择账号，而不是只列出「已授权过本应用」的账号
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = credentialManager.getCredential(
            context = activity,
            request = request
        )

        val credential = response.credential
        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw IllegalStateException("返回的凭证类型不支持，请改用 Google 账户登录")
        }

        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val payload = JwtPayload.parse(googleCredential.idToken)

        val email = payload.optString("email").takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("无法从 Google 账户读取邮箱")
        val subject = payload.optString("sub").takeIf { it.isNotBlank() } ?: email

        return UserAccount(
            id = subject,
            email = email,
            displayName = payload.optString("name").takeIf { it.isNotBlank() }
                ?: googleCredential.displayName,
            photoUrl = payload.optString("picture").takeIf { it.isNotBlank() }
                ?: googleCredential.profilePictureUri?.toString()
        )
    }
}

/**
 * 仅解析 JWT 的 payload 段用于展示用户信息。
 *
 * ⚠️ 没有做签名校验 —— 纯本地应用不需要，但如果以后要把这个 idToken 发给后端，
 * 必须由后端用 Google 的公钥（JWKS）验签，不能相信客户端解析结果。
 */
internal object JwtPayload {

    fun parse(idToken: String): JSONObject {
        val parts = idToken.split(".")
        if (parts.size < 2) throw IllegalStateException("ID Token 格式不正确")
        val decoded = Base64.decode(
            parts[1],
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
        return JSONObject(String(decoded, Charsets.UTF_8))
    }
}
