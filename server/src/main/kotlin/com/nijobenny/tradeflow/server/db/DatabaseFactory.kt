package com.nijobenny.tradeflow.server.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Connects to PostgreSQL.
 *
 * - If `DATABASE_URL` is set (e.g. `jdbc:postgresql://localhost:5432/tradeflow`), it uses that server
 *   with `DB_USER` / `DB_PASSWORD`.
 * - Otherwise it starts a real embedded PostgreSQL that keeps its data in `~/.tradeflow/pgdata`,
 *   so the project runs with zero setup.
 */
object DatabaseFactory {
    private val log = LoggerFactory.getLogger("Database")

    lateinit var description: String
        private set

    private var embedded: EmbeddedPostgres? = null

    fun init(): Database {
        val externalUrl = System.getenv("DATABASE_URL")
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 8
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
        }
        if (externalUrl != null) {
            config.jdbcUrl = externalUrl
            config.username = System.getenv("DB_USER") ?: "postgres"
            config.password = System.getenv("DB_PASSWORD") ?: "postgres"
        } else {
            val dataDir = File(System.getProperty("user.home"), ".tradeflow/pgdata")
            dataDir.mkdirs()
            log.info("Starting embedded PostgreSQL (data in ${dataDir.absolutePath}) …")
            val pg = EmbeddedPostgres.builder()
                .setDataDirectory(dataDir)
                .setCleanDataDirectory(false)
                .setPort(System.getenv("DB_PORT")?.toIntOrNull() ?: 54329)
                .start()
            embedded = pg
            Runtime.getRuntime().addShutdownHook(Thread { runCatching { pg.close() } })
            config.jdbcUrl = pg.getJdbcUrl("postgres", "postgres")
            config.username = "postgres"
            config.password = "postgres"
        }

        val database = Database.connect(HikariDataSource(config))
        transaction(database) {
            SchemaUtils.create(*allTables)
        }
        val version = transaction(database) {
            exec("SELECT version()") { rs -> if (rs.next()) rs.getString(1) else null }
        } ?: "PostgreSQL"
        // "PostgreSQL 14.22 on x86_64-apple-darwin…" → "PostgreSQL 14.22"
        val short = version.split(' ').take(2).joinToString(" ")
        description = if (embedded != null) "$short (embedded)" else short
        return database
    }
}

/** Runs a blocking database transaction off the request thread. */
suspend fun <T> dbQuery(block: Transaction.() -> T): T = withContext(Dispatchers.IO) {
    transaction { block() }
}
