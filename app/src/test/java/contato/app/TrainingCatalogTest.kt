package contato.app

import contato.app.domain.service.TrainingCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingCatalogTest {
    @Test
    fun todosOsItensSaoVibraveis() = assertTrue(TrainingCatalog.isValid())

    @Test
    fun tresModulosEmOrdem() = assertEquals(listOf(1, 2, 3), TrainingCatalog.modules.map { it.number })
}
