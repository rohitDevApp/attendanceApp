import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SaveDataWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // Retrieve input data
//        val eventType = inputData.getString("eventType") ?: return Result.failure()
        val latitude = inputData.getDouble("latitude", 0.0)
        val longitude = inputData.getDouble("longitude", 0.0)
//        val currentDate = inputData.getString("currentDate") ?: return Result.failure()
//        val currentTime = inputData.getString("currentTime") ?: return Result.failure()
        val fullAddress = inputData.getString("fullAddress") ?: return Result.failure()

        // API call logic
        val client = OkHttpClient()
        val url = "https://app.cheransoftwares.com/api/app/staff_attendance/clock_store"

        val jsonBody = """
            {
                "employee_id": "abide1234",
                "latitude": $latitude,
                "longitude": $longitude,
                "address": "$fullAddress"
            }
        """.trimIndent()

        val body = jsonBody.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()
            Log.d("ResponseDataFromManager",response.toString())
            if (response.isSuccessful) {
                Log.d("WorkManager", "Successfully sent data")
                Result.success()
            } else {
                Log.e("WorkManager", "API call failed: ${response.message}")
                Result.retry() // Retry on failure
            }
        } catch (e: Exception) {
            Log.e("WorkManager", "Error during API call", e)
            Result.retry()
        }
    }
}
