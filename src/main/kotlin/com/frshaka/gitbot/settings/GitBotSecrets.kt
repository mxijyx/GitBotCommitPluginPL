package com.frshaka.gitbot.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe

object GitBotSecrets {

    private const val SERVICE_NAME = "com.frshaka.gitbot.openrouter"

    // Nome de usuário fixo incluído tanto no get quanto no set para garantir
    // que a chave de lookup seja idêntica nos dois casos.
    // Sem isso, alguns backends (ex: Windows Credential Manager, KWallet)
    // não encontram a credencial gravada, fazendo a API key "sumir" ao reiniciar a IDE.
    private const val USER_NAME = "openrouter"

    private fun attributes() = CredentialAttributes(SERVICE_NAME, USER_NAME)

    /**
     * Retorna true se o PasswordSafe está operando apenas em memória,
     * ou seja, as credenciais NÃO são persistidas entre sessões da IDE.
     * Isso ocorre quando o keyring do sistema não está disponível.
     */
    fun isMemoryOnly(): Boolean = PasswordSafe.instance.isMemoryOnly

    fun getApiKey(): String? {
        return PasswordSafe.instance.get(attributes())?.getPasswordAsString()
    }

    fun setApiKey(apiKey: String) {
        PasswordSafe.instance.set(attributes(), Credentials(USER_NAME, apiKey))
    }
}