package cl.deepsource.horas.feature.auth.domain

/** [V6/ROLES] Permisos del prototipo local. En producción deben comprobarse también en el servidor. */
object RolePermissions {
    fun canManageAssignments(user: User?): Boolean = user?.role == UserRole.ADMIN
}
