package com.borzsoft.smsgateway.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.borzsoft.smsgateway.db.dao.SessionDao
import com.borzsoft.smsgateway.db.dao.SmsLogDao
import com.borzsoft.smsgateway.db.entity.Session
import com.borzsoft.smsgateway.db.entity.SmsLog
import com.borzsoft.smsgateway.security.TokenManager
import com.borzsoft.smsgateway.service.GatewayService
import com.borzsoft.smsgateway.utils.DateUtils
import com.borzsoft.smsgateway.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GatewayViewModel @Inject constructor(
    private val smsLogDao: SmsLogDao,
    private val sessionDao: SessionDao
) : ViewModel() {

    val smsLogs: LiveData<List<SmsLog>> = smsLogDao.observeAll()
    val activeSessions: LiveData<List<Session>> = sessionDao.observeActive()
    val activeSessionCount: LiveData<Int> = sessionDao.observeActiveCount()

    private val _currentToken = MutableLiveData<String?>()
    val currentToken: LiveData<String?> = _currentToken

    private val _currentQrData = MutableLiveData<String?>()
    val currentQrData: LiveData<String?> = _currentQrData

    val isRunning: Boolean get() = GatewayService.isRunning
    val gatewayUrl: String get() = "http://${NetworkUtils.getLocalIp()}:${GatewayService.PORT}"

    fun generateNewSession() {
        viewModelScope.launch {
            val token = TokenManager.generate()
            val ip = NetworkUtils.getLocalIp()
            val expiresAt = TokenManager.expiryStr(10)
            val session = Session(
                token = token,
                clientIp = "*",
                createdAt = DateUtils.now(),
                expiresAt = expiresAt
            )
            sessionDao.insert(session)
            _currentToken.postValue(token)
            _currentQrData.postValue(
                """{"token":"$token","ip":"$ip","port":${GatewayService.PORT},"expires":"$expiresAt"}"""
            )
        }
    }

    fun revokeAllSessions() {
        viewModelScope.launch {
            sessionDao.revokeAll()
            _currentToken.postValue(null)
            _currentQrData.postValue(null)
        }
    }

    fun revokeSession(token: String) {
        viewModelScope.launch { sessionDao.revoke(token) }
    }

    fun clearLogs() {
        viewModelScope.launch { smsLogDao.clearAll() }
    }
}
