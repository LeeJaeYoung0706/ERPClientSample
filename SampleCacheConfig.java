@Configuration
@EnableCaching
public class SampleCacheConfig {

    public static final String HIT_DATA = "hitData";
    public static final String STALE_DATA = "staleData";

    /**
     * 간단하게 백업데이터와 자주 변경하는 데이터로 구성
     * @return
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(    HIT_DATA,
                STALE_DATA);

//        manager.setCaffeine(
//                Caffeine.newBuilder()
//                        .maximumSize(3_000)
//                        .recordStats()
//        );

        manager.registerCustomCache(HIT_DATA,
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumWeight(200_000_000L) // 200MB
                        .weigher((Object k, Object v) -> WeightEstimators.listMapWeight(v))
                        .recordStats()
                        .build()
        );

        manager.registerCustomCache(STALE_DATA,
                Caffeine.newBuilder()
                        .expireAfterWrite(24, TimeUnit.HOURS)
                        .maximumWeight(300_000_000L) // 300MB
                        .weigher((Object k, Object v) -> WeightEstimators.listMapWeight(v))
                        .recordStats()
                        .build()
        );

        return manager;
    }

    private static final class WeightEstimators {
        private WeightEstimators() {}
        // 운영 시 조정 필요 한 정보들입니다.
        private static final int BASE = 1_000;
        private static final int PER_ROW = 200;
        private static final int PER_ENTRY = 80;
        private static final int PER_CHAR = 2;
        private static final int PER_NUMBER = 16;

        @SuppressWarnings("unchecked")
        public static int listMapWeight(Object value) {
            if (!(value instanceof List<?> list)) return BASE;

            long w = BASE;

            for (Object rowObj : list) {
                w += PER_ROW;
                if (!(rowObj instanceof Map<?, ?> row)) continue;

                w += (long) row.size() * PER_ENTRY;

                for (Map.Entry<?, ?> e : row.entrySet()) {
                    w += objWeight(e.getKey());
                    w += objWeight(e.getValue());
                }
            }

            return (int) Math.min(Integer.MAX_VALUE, w);
        }

        private static int objWeight(Object o) {
            if (o == null) return 0;
            if (o instanceof String s) return 40 + s.length() * PER_CHAR;
            if (o instanceof Number) return PER_NUMBER;
            if (o instanceof Boolean) return 8;
            return 64;
        }
    }
}
