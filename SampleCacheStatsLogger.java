
@Slf4j
@Component
public class SampleCacheStatsLogger {

    private final CacheManager cacheManager;

    public SampleCacheStatsLogger(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Scheduled(fixedDelay = 10_000) // 10초마다 간단하게 체크, 운영시 체크용시간은 좀 크게 둘필요가 있음.
    public void logStats() {
        logOne(CacheConfig.HIT_DATA);
        logOne(CacheConfig.STALE_DATA);
    }

    private void logOne(String cacheName) {
        var cache = (org.springframework.cache.caffeine.CaffeineCache) cacheManager.getCache(cacheName);
        if (cache == null) return;

        var nativeCache = (com.github.benmanes.caffeine.cache.Cache<?, ?>) cache.getNativeCache();
        var stats = nativeCache.stats();

        log.info("[캐시정보 체크용 :{}] hitRate={}, hit={}, miss={}, evictions={}, loadSuccess={}, loadFail={}",
                cacheName,
                String.format("%.2f", stats.hitRate()),
                stats.hitCount(),
                stats.missCount(),
                stats.evictionCount(),
                stats.loadSuccessCount(),
                stats.loadFailureCount()
        );
    }
}