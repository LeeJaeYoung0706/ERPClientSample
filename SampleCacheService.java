/**
 * 자주 변하지 않는 ERP 정보 캐싱처리하는 용도의 예제
 */
public class SampleCacheService {
    private final CacheManager cacheManager;

    public ErpCacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    private String key(CacheKeyProvider dto, ErpApiEndpoint endpoint) {
        return endpoint.name()
                + ":" + dto.getClass().getSimpleName()
                + ":" + dto.cacheKey();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getFromCache(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) return null;
        return cache.get(key, List.class);
    }

    private void putToCache(String cacheName, String key, List<Map<String, Object>> value) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) cache.put(key, value);
    }

    /**
     * forceApi=false: hot 캐시 우선, miss면 ERP 호출
     * forceApi=true : 무조건 ERP 호출, 성공 시 캐시 갱신, 실패 시 stale fallback
     */
    public List<Map<String, Object>> getOrRefreshWithFallback(
            CacheKeyProvider dto,
            ErpApiEndpoint endpoint,
            boolean forceApi,
            Supplier<List<Map<String, Object>>> loader
    ) {
        final String k = key(dto, endpoint);

        if (!forceApi) {
            List<Map<String, Object>> hot = getFromCache(CacheConfig.HIT_DATA, k);
            if (hot != null) {
                log.info("[HIT 캐시 정보] cache={}, key={}", CacheConfig.HIT_DATA, k);
                return hot;
            }
        }

        try {
            log.info("[ERP API 호출 시도] forceApi={}, endpoint={}, key={}", forceApi, endpoint.name(), dto.cacheKey());
            List<Map<String, Object>> fresh = loader.get();

            putToCache(CacheConfig.HIT_DATA, k, fresh);
            putToCache(CacheConfig.STALE_DATA, k, fresh);

            log.info("[CACHE 업데이트 됨] key={} -> hot+stale updated", k);
            return fresh;

        } catch (Exception e) {
            // 실패 시 stale fallback
            List<Map<String, Object>> stale = getFromCache(CacheConfig.STALE_DATA, k);
            if (stale != null) {
                log.warn("[ERP 동기화 실패 백업 데이터] endpoint={}, key={}, msg={}",
                        endpoint.name(), dto.cacheKey(), e.getMessage());
                return stale;
            }

            // stale도 없으면 그대로 실패
            log.error("[ERP 동기화 실패 백업 데이터 미존재] endpoint={}, key={}, msg={}",
                    endpoint.name(), dto.cacheKey(), e.getMessage(), e);
            throw new CustomResponseException("ERP 동기화 실패 및 백업 데이터 미존재", 500);
        }
    }

    public void evictAll() {
        Cache hot = cacheManager.getCache(CacheConfig.HIT_DATA);
        Cache stale = cacheManager.getCache(CacheConfig.STALE_DATA);
        if (hot != null) hot.clear();
        if (stale != null) stale.clear();
        log.info("[CACHE EVICT 캐시초기화] HIT_DATA, STALE_DATA");
    }
}
