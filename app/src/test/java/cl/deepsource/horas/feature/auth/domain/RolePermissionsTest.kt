package cl.deepsource.horas.feature.auth.domain

import org.junit.Assert.*
import org.junit.Test

class RolePermissionsTest {
    private fun user(role: UserRole) = User("id", "Usuario", "usuario@ejemplo.cl", role = role)
    private fun adminCanAssign() = assertTrue(RolePermissions.canManageAssignments(user(UserRole.ADMIN)))
    private fun regularUserCannotAssign() = assertFalse(RolePermissions.canManageAssignments(user(UserRole.NORMAL)))
    private fun anonymousCannotAssign() = assertFalse(RolePermissions.canManageAssignments(null))

    // Cada escenario reúne casos relacionados, sin quitar validaciones.
    @Test fun scenario1() {
        adminCanAssign()
        regularUserCannotAssign()
        anonymousCannotAssign()
    }
}
