package no.novari.flyt.webresourceserver.security.user.authorization

import com.github.benmanes.caffeine.cache.Ticker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import java.time.Duration
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class CachingUserAuthorizationClientTest {
    private val objectIdentifier = UUID.randomUUID()
    private val clock = AtomicLong()
    private val ticker = Ticker { clock.get() }
    private val properties =
        UserAuthorizationClientProperties.Cache(
            ttl = Duration.ofSeconds(15),
            maxSize = 10_000,
            staleIfError = Duration.ofMinutes(2),
        )

    @Test
    fun `caches positive and negative authorization results`() {
        val delegate = mock(UserAuthorizationClient::class.java)
        `when`(delegate.getAuthorizedSourceApplicationIds(objectIdentifier, setOf(1L, 2L)))
            .thenReturn(setOf(1L))
        val client = CachingUserAuthorizationClient(delegate, properties, ticker)

        assertThat(client.getAuthorizedSourceApplicationIds(objectIdentifier, setOf(1L, 2L))).containsExactly(1L)
        assertThat(client.getAuthorizedSourceApplicationIds(objectIdentifier, setOf(1L, 2L))).containsExactly(1L)

        verify(delegate).getAuthorizedSourceApplicationIds(objectIdentifier, setOf(1L, 2L))
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun `expired entries are refreshed when authorization service is available`() {
        val delegate = mock(UserAuthorizationClient::class.java)
        `when`(delegate.getAuthorizedSourceApplicationIds(objectIdentifier, setOf(1L)))
            .thenReturn(setOf(1L))
            .thenReturn(emptySet())
        val client = CachingUserAuthorizationClient(delegate, properties, ticker)

        assertThat(client.getAuthorizedSourceApplicationIds(objectIdentifier, setOf(1L))).containsExactly(1L)
        clock.addAndGet(Duration.ofSeconds(16).toNanos())

        assertThat(client.getAuthorizedSourceApplicationIds(objectIdentifier, setOf(1L))).isEmpty()

        verify(delegate, times(2)).getAuthorizedSourceApplicationIds(objectIdentifier, setOf(1L))
    }

    @Test
    fun `uses stale positive and negative entries when refresh fails within stale window`() {
        val delegate = mock(UserAuthorizationClient::class.java)
        `when`(delegate.getAuthorizedSourceApplicationIds(objectIdentifier, setOf(1L, 2L)))
            .thenReturn(setOf(1L))
            .thenThrow(UserAuthorizationClientException("unavailable", IllegalStateException()))
        val client = CachingUserAuthorizationClient(delegate, properties, ticker)

        assertThat(client.getAuthorizedSourceApplicationIds(objectIdentifier, setOf(1L, 2L))).containsExactly(1L)
        clock.addAndGet(Duration.ofSeconds(16).toNanos())

        assertThat(client.getAuthorizedSourceApplicationIds(objectIdentifier, setOf(1L, 2L))).containsExactly(1L)

        verify(delegate, times(2)).getAuthorizedSourceApplicationIds(objectIdentifier, setOf(1L, 2L))
    }

    @Test
    fun `fails when refresh fails after stale window has expired`() {
        val delegate = mock(UserAuthorizationClient::class.java)
        `when`(delegate.getAuthorizedSourceApplicationIds(objectIdentifier, setOf(1L)))
            .thenReturn(setOf(1L))
            .thenThrow(UserAuthorizationClientException("unavailable", IllegalStateException()))
        val client = CachingUserAuthorizationClient(delegate, properties, ticker)

        assertThat(client.getAuthorizedSourceApplicationIds(objectIdentifier, setOf(1L))).containsExactly(1L)
        val elapsedNanos =
            properties.ttl.toNanos() +
                properties.staleIfError.toNanos() +
                1
        clock.addAndGet(elapsedNanos)

        assertThatThrownBy {
            client.getAuthorizedSourceApplicationIds(objectIdentifier, setOf(1L))
        }.isInstanceOf(UserAuthorizationClientException::class.java)

        verify(delegate, times(2)).getAuthorizedSourceApplicationIds(objectIdentifier, setOf(1L))
    }
}
