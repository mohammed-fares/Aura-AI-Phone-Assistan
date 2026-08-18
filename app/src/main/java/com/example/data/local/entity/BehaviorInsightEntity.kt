package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "behavior_insights")
data class BehaviorInsightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String, // "سلوك الاستخدام", "كفاءة الموارد", "تحليل الاتصالات", "أنماط الأوامر الصوتية"
    val title: String,
    val summary: String,
    val recommendation: String,
    val score: Int = 95, // 0-100 score/confidence
    val isApplied: Boolean = false
)
