package io.plastique.users.profile

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import io.plastique.users.UserEntity

@Entity(tableName = "user_profiles",
        foreignKeys = [
            ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["user_id"])
        ])
data class UserProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "profile_url")
    val url: String,

    @ColumnInfo(name = "real_name")
    val realName: String?,

    @ColumnInfo(name = "bio")
    val bio: String?,

    @ColumnInfo(name = "is_watching")
    val isWatching: Boolean
)