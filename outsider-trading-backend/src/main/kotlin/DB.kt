package io.otrade

import io.otrade.data.Company
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnTransformer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

object DB {

    fun setup() {
        Database.connect("jdbc:sqlite:database.db", driver = "org.sqlite.JDBC")

        transaction {
            SchemaUtils.create(Companies)
            if (Companies.selectAll().count() == 0L) {
                Companies.insert {
                    it[id] = "AAPL"
                    it[name] = "Apple"
                    it[description] = "This is a description of Apple."
                    it[values] = listOf(Random.nextInt(30, 60).toFloat())
                }
                Companies.insert {
                    it[id] = "GOOGL"
                    it[name] = "Google"
                    it[description] = "This is a description of Google."
                    it[values] = listOf(Random.nextInt(30, 60).toFloat())
                }

                Companies.insert {
                    it[id] = "TSLA"
                    it[name] = "Tesla"
                    it[description] = "This is a description of Tesla."
                    it[values] = listOf(Random.nextInt(30, 60).toFloat())
                }
            }
        }
    }

    fun loadCompanies() = transaction {
        Companies.selectAll().map { c ->
            val values = c[Companies.values].toMutableList()
            if (values.isEmpty()) {
                values.add(Random.nextInt(30, 60).toFloat())
            }
            if (values.size < 50) {
                val first = values.first()
                values.addAll(0, List(50 - values.size) { first })
            }
            Company(c[Companies.id], c[Companies.name], c[Companies.description], Company.StockPerformance(values.last(), values))
        }
    }

    fun saveCompanies(companies: List<Company>) = transaction {
        Companies.deleteAll()
        Companies.batchInsert(companies) {
            this[Companies.id] = it.id
            this[Companies.name] = it.name
            this[Companies.description] = it.description
            this[Companies.values] = it.performance.toList()
        }
    }

    object Companies : Table() {
        val id: Column<String> = varchar("id", 50).uniqueIndex()
        val name: Column<String> = varchar("name", 100)
        val description: Column<String> = text("description")
        val values: Column<List<Float>> = text("values").transform(ListToString)

        override val primaryKey = PrimaryKey(id)
    }

    private object ListToString : ColumnTransformer<String, List<Float>> {
        override fun unwrap(value: List<Float>) = value.joinToString(";") { it.toString() }
        override fun wrap(value: String): List<Float> = value.split(";").map { it.toFloat() }
    }

}
