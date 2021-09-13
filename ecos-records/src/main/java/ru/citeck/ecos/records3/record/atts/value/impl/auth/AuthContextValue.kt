package ru.citeck.ecos.records3.record.atts.value.impl.auth

import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.data.AuthData
import ru.citeck.ecos.records3.record.atts.value.AttValue

/**
 * Cache is not allowed here, because this value shared between threads
 */
class AuthContextValue : AttValue {

    override fun getAtt(name: String): Any? {
        when (name) {
            "full" -> AuthAttValue(AuthContext.getCurrentFullAuth())
            "runAs" -> AuthAttValue(AuthContext.getCurrentRunAsAuth())
        }
        return AuthAttValue(AuthContext.getCurrentFullAuth()).getAtt(name)
    }

    override fun has(name: String): Boolean {
        return AuthAttValue(AuthContext.getCurrentFullAuth()).has(name)
    }

    class AuthAttValue(val auth: AuthData) : AttValue {

        override fun getAtt(name: String?): Any? {
            return when (name) {
                "user" -> auth.getUser()
                "authorities" -> auth.getAuthorities()
                "userWithAuthorities" -> getUserWithAuthorities()
                else -> null
            }
        }

        override fun has(name: String): Boolean {
            return auth.getUser() == name || auth.getAuthorities().contains(name)
        }

        private fun getUserWithAuthorities(): List<String> {
            val user = auth.getUser()
            val authorities = auth.getAuthorities()
            if (user.isBlank()) {
                return authorities
            }
            if (authorities.isEmpty()) {
                return listOf(user)
            }
            val res = ArrayList<String>()
            res.add(user)
            res.addAll(authorities)
            return res
        }
    }
}
