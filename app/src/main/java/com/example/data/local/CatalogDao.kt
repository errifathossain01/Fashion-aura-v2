package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CatalogProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {

    @Query("SELECT * FROM catalog_products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<CatalogProductEntity>>

    @Query("SELECT * FROM catalog_products ORDER BY name ASC")
    suspend fun getAllProductsDirect(): List<CatalogProductEntity>

    @Query("SELECT * FROM catalog_products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): CatalogProductEntity?

    @Query("SELECT COUNT(*) FROM catalog_products")
    suspend fun getProductCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: CatalogProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<CatalogProductEntity>)

    @Update
    suspend fun updateProduct(product: CatalogProductEntity)

    @Query("DELETE FROM catalog_products WHERE id = :id")
    suspend fun deleteProductById(id: String)

    @Query("DELETE FROM catalog_products")
    suspend fun deleteAllProducts()
}
