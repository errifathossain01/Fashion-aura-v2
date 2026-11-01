package com.example.data.repository

import com.example.data.local.CatalogDao
import com.example.data.model.CatalogProductEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CatalogRepository(
    private val catalogDao: CatalogDao
) {
    val allProducts: Flow<List<CatalogProductEntity>> = catalogDao.getAllProducts()

    suspend fun getAllProductsDirect(): List<CatalogProductEntity> = withContext(Dispatchers.IO) {
        catalogDao.getAllProductsDirect()
    }

    suspend fun getProductById(id: String): CatalogProductEntity? = withContext(Dispatchers.IO) {
        catalogDao.getProductById(id)
    }

    suspend fun insertProduct(product: CatalogProductEntity) = withContext(Dispatchers.IO) {
        catalogDao.insertProduct(product)
    }

    suspend fun insertProducts(products: List<CatalogProductEntity>) = withContext(Dispatchers.IO) {
        catalogDao.insertProducts(products)
    }

    suspend fun updateProduct(product: CatalogProductEntity) = withContext(Dispatchers.IO) {
        catalogDao.updateProduct(product)
    }

    suspend fun deleteProduct(id: String) = withContext(Dispatchers.IO) {
        catalogDao.deleteProductById(id)
    }

    suspend fun restoreCatalog(products: List<CatalogProductEntity>) = withContext(Dispatchers.IO) {
        catalogDao.insertProducts(products)
    }
}
