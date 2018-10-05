package io.plastique.deviations

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "deviation_metadata",
        foreignKeys = [
            ForeignKey(entity = DeviationEntity::class, parentColumns = ["id"], childColumns = ["deviation_id"], onDelete = ForeignKey.CASCADE)
        ])
data class DeviationMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "deviation_id")
    val deviationId: String,

    @ColumnInfo(name = "description")
    val description: String? = null
)
