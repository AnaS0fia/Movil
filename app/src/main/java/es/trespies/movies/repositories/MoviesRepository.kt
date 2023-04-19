package es.trespies.movies.repositories

import es.trespies.movies.api.MovieService
import es.trespies.movies.db.MovieDao
import es.trespies.movies.model.Movie
import es.trespies.movies.repositories.paging.LoadMoreState
import es.trespies.movies.repositories.paging.NextPageHandler
import es.trespies.movies.services.Configuration
import es.trespies.movies.services.CoroutineAppExecutors
import es.trespies.movies.util.Resource
import kotlinx.coroutines.flow.Flow

import javax.inject.Inject

class MoviesRepository @Inject constructor(
    private val coroutineAppExecutors: CoroutineAppExecutors,
    private val configuration: Configuration,
    private val movieService: MovieService,
    private val movieDao: MovieDao
) {

    val movies: Flow<Resource<List<Movie>>> = networkBoundResource(
        coroutineAppExecutors = coroutineAppExecutors,
        saveCallResult = { apiObject ->
                         apiObject.results?.mapNotNull {
                             Movie.build(it, configuration)
                         }?.let {
                             movieDao.insert(movies = it)
                         }
        },
        loadFromDb = { movieDao.loadMovies() },
        fetch = { movieService.popularMovies() }
    )

    private val nextPageHandler = NextPageHandler(
        coroutineAppExecutors = coroutineAppExecutors,
        saveCallResult = { apiObject ->
            apiObject.results?.mapNotNull {
                Movie.build(it, configuration)
            }?.let {
                movieDao.insert(movies = it)
            }
        },
        fetch = { numPage -> movieService.popularMovies(page = numPage) },
        processHasMorePages = {
            // TODO, control when there are no more pages
            true
        }
    )

    val loadMoreState: Flow<LoadMoreState> = nextPageHandler.asFlow()

    fun movie(movieId: String): Flow<Resource<Movie>> = networkBoundResource(
        coroutineAppExecutors = coroutineAppExecutors,
        saveCallResult = {
            Movie.build(it, configuration)?.let { movie ->
                movieDao.insert(listOf(movie))
            }
        },
        loadFromDb = { movieDao.loadMovie(movieId = movieId) },
        fetch = { movieService.movie(movieId = movieId) }
    )

    suspend fun queryNextPage() {
        nextPageHandler.queryNextPage()
    }

    fun resetNextPage() {
        nextPageHandler.reset()
    }

}