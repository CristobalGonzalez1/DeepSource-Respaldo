package cl.deepsource.horas.data.remote

import cl.deepsource.horas.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// Aquí Retrofit consulta nuestra API de proyectos y tareas; el emulador usa 10.0.2.2 para llegar al computador.
data class RemoteProject(val id: Long? = null, val name: String, val description: String = "", val clientId: String? = null)
data class RemoteTask(val id: Long? = null, val projectId: Long, val name: String, val status: String = "OPEN", val clientId: String? = null, val estimateMinutes: Int = 90)
data class RemoteHours(val id: Long? = null, val projectId: Long, val taskName: String, val durationMinutes: Long, val workedOn: String, val note: String = "", val clientId: String? = null)
data class Holiday(val title: String? = null, val date: String? = null, val type: String? = null)
data class HolidayResponse(val data: List<Holiday> = emptyList())

interface ProjectsApi {
 @GET("api/projects") suspend fun projects(): List<RemoteProject>
 @POST("api/projects") suspend fun addProject(@Body body: RemoteProject): RemoteProject
 @PUT("api/projects/{id}") suspend fun updateProject(@Path("id") id: Long, @Body body: RemoteProject): RemoteProject
 @DELETE("api/projects/{id}") suspend fun deleteProject(@Path("id") id: Long)
 @GET("api/tasks") suspend fun tasks(): List<RemoteTask>
 @POST("api/tasks") suspend fun addTask(@Body body: RemoteTask): RemoteTask
 @PUT("api/tasks/{id}") suspend fun updateTask(@Path("id") id: Long, @Body body: RemoteTask): RemoteTask
 @DELETE("api/tasks/{id}") suspend fun deleteTask(@Path("id") id: Long)
}
interface HoursApi {
 @GET("api/hours") suspend fun hours(): List<RemoteHours>
 @POST("api/hours") suspend fun addHours(@Body body: RemoteHours): RemoteHours
 @PUT("api/hours/{id}") suspend fun updateHours(@Path("id") id: Long, @Body body: RemoteHours): RemoteHours
 @DELETE("api/hours/{id}") suspend fun deleteHours(@Path("id") id: Long)
}
interface HolidayApi { @GET("feriados.json") suspend fun list(): HolidayResponse }
object RemoteApi {
 private fun retrofit(base: String) = Retrofit.Builder().baseUrl(base).addConverterFactory(GsonConverterFactory.create()).build()
 val projects: ProjectsApi by lazy { retrofit(BuildConfig.API_BASE_URL).create(ProjectsApi::class.java) }
 val hours: HoursApi by lazy { retrofit(BuildConfig.HOURS_BASE_URL).create(HoursApi::class.java) }
 // Public third-party API. Failure is isolated from own backend.
 val holidays: HolidayApi by lazy { retrofit("https://api.roison.cl/").create(HolidayApi::class.java) }
}
