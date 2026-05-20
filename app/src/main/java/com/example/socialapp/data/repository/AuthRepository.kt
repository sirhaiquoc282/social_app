package com.example.socialapp.data.repository

import com.example.socialapp.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val messaging: FirebaseMessaging
) {
    val currentUser: FirebaseUser? get() = auth.currentUser
    val isLoggedIn: Boolean get() = auth.currentUser != null

    /** Đăng nhập bằng Email/Password */
    suspend fun loginWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                updateFcmToken(it.uid)
                updateUserStatus(it.uid, "online")
            }
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Đăng ký bằng Email/Password */
    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                user.updateProfile(profileUpdates).await()
                upsertUser(user.uid, displayName, email)
                updateFcmToken(user.uid)
            }
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Đăng nhập bằng Google */
    suspend fun loginWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user?.let { user ->
                upsertUser(
                    uid = user.uid,
                    displayName = user.displayName ?: "Người dùng",
                    email = user.email ?: "",
                    avatarUrl = user.photoUrl?.toString() ?: ""
                )
                updateFcmToken(user.uid)
                updateUserStatus(user.uid, "online")
            }
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Đăng xuất */
    suspend fun logout() {
        currentUser?.uid?.let { uid ->
            updateUserStatus(uid, "offline")
        }
        auth.signOut()
    }

    /** Tạo hoặc cập nhật user document */
    suspend fun upsertUser(
        uid: String,
        displayName: String,
        email: String,
        avatarUrl: String = ""
    ) {
        val data = hashMapOf(
            "uid" to uid,
            "displayName" to displayName,
            "email" to email,
            "avatarUrl" to avatarUrl,
            "status" to "online"
        )
        firestore.collection("users").document(uid)
            .set(data, SetOptions.merge()).await()
    }

    /** Lấy thông tin user theo uid */
    suspend fun getUser(uid: String): User? {
        return try {
            firestore.collection("users").document(uid)
                .get().await().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /** Lắng nghe real-time user list (để tìm kiếm cuộc trò chuyện mới) */
    fun observeAllUsers(): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val users = snapshot?.toObjects(User::class.java) ?: emptyList()
                // Loại bỏ current user
                trySend(users.filter { it.uid != currentUser?.uid })
            }
        awaitClose { listener.remove() }
    }

    private suspend fun updateFcmToken(uid: String) {
        try {
            val token = messaging.token.await()
            firestore.collection("users").document(uid)
                .update("fcmToken", token).await()
        } catch (_: Exception) {}
    }

    private suspend fun updateUserStatus(uid: String, status: String) {
        try {
            firestore.collection("users").document(uid)
                .update("status", status).await()
        } catch (_: Exception) {}
    }
}

