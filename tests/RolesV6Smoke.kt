import cl.deepsource.horas.feature.auth.domain.RolePermissions
import cl.deepsource.horas.feature.auth.domain.User
import cl.deepsource.horas.feature.auth.domain.UserRole

fun main() {
    val normal = User("demo", "Usuario Demo", "demo@deepsource.cl", role = UserRole.NORMAL)
    val admin = User("admin", "Administrador", "admin@deepsource.cl", role = UserRole.ADMIN)
    check(!RolePermissions.canManageAssignments(null))
    check(!RolePermissions.canManageAssignments(normal))
    check(RolePermissions.canManageAssignments(admin))
    println("PASS: null y normal sin acceso; ADMIN con acceso a gestión")
}
