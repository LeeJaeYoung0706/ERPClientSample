import dtos.SampleGetRequestSecondDto;

// 비즈니스 로직용
@Service
@Slf4j(topic = "ERROR_FILE_LOGGER")
public class SampleClientBusinessService {

    // Samplee API 요청용
    private final SampleClient client;
    // 외부 API 연동용 ObjectMapper
    private final ObjectMapper OT_OM;
    // 내부 용 ObjectMapper
    private final ObjectMapper IN_OM;
    // 내부 DB 핸들링용
    private final SampleClientTransactionService transactionService;

    private final SampleClientProperties properties;

    private final SampleClientModule module;

    private final Validator validator;

    public SampleClientBusinessService(SampleClient client, @Qualifier("externalObjectMapper") ObjectMapper OT_OM, ObjectMapper IN_OM, SampleClientTransactionService transactionService, SampleClientProperties properties, SampleClientModule module, Validator validator) {
        this.client = client;
        this.OT_OM = OT_OM;
        this.IN_OM = IN_OM;
        this.transactionService = transactionService;
        this.properties = properties;
        this.module = module;
        this.validator = validator;
    }


    /*============= 요청 하는 함수 ==================*/

    /*=========   관련 예제 ========*/

    /**
     * -
     * 샘플 조회 방식 멀티쓰레드 X
     * @param dto
     * @param endpoint
     * @return
     */
    public List<Map<String, Object>> sampleSearch(SampleGetRequestDto dto, SampleApiEndpoint endpoint) {
        return callByResultToList(dto, endpoint);
    }

    /**
     * MES 정보 검색 
     * -
     * @param paramMap 검색 조건
     * @return
     */
    public Response<?> sampleFindFunction(Map<String, Object> paramMap) {
        List<Map<String, Object>> list = transactionService.sampleFindFunction(paramMap);
        return Response.builder("ok", 200).data(list).total(TransUtils.transGetTotalByList(list)).build();
    }

    /**
     * === 멀티 쓰레드 방식으로 가져오기  정보 ===
     **/

    // sampleInteger2 확인 함수
    // -
    private void validateSampleInteger2(int sampleInteger2) {
        if (sampleInteger2 <= 0) {
            throw new CustomResponseException(" 조회 실패 : 오류 sampleInteger2 값 확인 필요 0보다 작습니다.", 400);
        }
        if (sampleInteger2 > 1000) {
            throw new CustomResponseException(" 조회 실패 : 오류 sampleInteger2 지정 범위는 1000을 초과할 수 없습니다. sampleInteger2=" + sampleInteger2, 400);
        }
    }

    // 필요 dto 생성
    // -
    private SampleGetRequestDto buildSampleType1Dto(int sampleInteger1, int sampleInteger2) {
        SampleGetRequestDto dto = SampleGetRequestDto.createSampleType1(sampleInteger1, sampleInteger2, properties.sampleKey());
        return dto;
    }

    // 병렬 처리 함수
    // -
    private int submitPagingTasks(CompletionService<PageResult> cs, int totalCount, SyncConfig cfg) {
        int taskCount = 0;

        for (int start = 0; start < totalCount; start += cfg.pageSize) {
            final int sampleInteger1 = start;
            final int sampleInteger2 = Math.min(cfg.pageSize, totalCount - sampleInteger1);

            validateSampleInteger2(sampleInteger2);

            final SampleGetRequestDto taskDto = buildSampleType1Dto(sampleInteger1, sampleInteger2);

            cs.submit(() -> {
                List<Map<String, Object>> list = callByResultToList(taskDto, SampleApiEndpoint.SAMPLE_QUERY_A);

                int saved = 0;
                int failed = 0;

                for (Map<String, Object> row : list) {
                    try {
                        // 여기에서 추가적인 함수처리하세요.
                        // 함수로 변경하여 쪼개면 좋아요.
                        saved++;
                    } catch (Exception e) {
                        failed++;
                        log.error("ERP 정보 동기화 실패 사유 : {}", e.getMessage(), e);
                        // 실패 사유 필요한 것 상태 값 처리
                    }
                }

                return PageResult.success(
                        sampleInteger1,
                        sampleInteger2,
                        list,
                        saved,
                        failed
                );
            });
            taskCount++;
        }
        return taskCount;
    }

    // 쓰레드 생성
    // -
    private ExecutorService newExecutor(SyncConfig cfg) {
        ThreadFactory tf = r -> {
            Thread t = new Thread(r);
            t.setName("erp-sync-" + t.getId());
            t.setDaemon(false);
            return t;
        };

        return new ThreadPoolExecutor(
                cfg.threadCount,
                cfg.threadCount,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(cfg.queueCapacity),
                tf,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    // 결과값 합치는 함수
    // -
    private FetchSummary collectResults(
            CompletionService<PageResult> cs,
            int taskCount,
            long startedAt,
            SyncConfig cfg
    ) {
        List<Map<String, Object>> all = new ArrayList<>();
        int successPages = 0;
        int failPages = 0;

        for (int i = 0; i < taskCount; i++) {
            Future<PageResult> fut = pollWithBatchTimeout(cs, startedAt, cfg);

            try {
                int totalSaved = 0;
                int totalFailed = 0;
                PageResult pr = fut.get();
                if (pr.ok) {
                    successPages++;
                    // save 실패 시 처리 용도 필요하면 추가 해서 사용
                    totalSaved += pr.savedCount;
                    totalFailed += pr.failedCount;
                    all.addAll(pr.data);
                } else {
                    failPages++;
                    handleFail(pr, cfg);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new CustomResponseException(" 조회 실패 : interrupted", 502);
            } catch (ExecutionException ee) {
                failPages++;
                Throwable cause = ee.getCause();
                String msg = (cause != null ? cause.getMessage() : ee.getMessage());
                if (cfg.failFast) {
                    throw new CustomResponseException(" 조회 실패 : " + msg, 502);
                }
            }
        }

        return new FetchSummary(all, successPages, failPages);
    }

    // 타임아웃 함수
    // -
    private Future<PageResult> pollWithBatchTimeout(CompletionService<PageResult> cs, long startedAt, SyncConfig cfg) {
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;
        long remainMs = cfg.batchTimeoutMs - elapsedMs;

        if (remainMs <= 0) {
            throw new CustomResponseException("조회 실패 : 배치 타임아웃(" + cfg.batchTimeoutMs + "ms) 초과", 504);
        }

        try {
            Future<PageResult> fut = cs.poll(remainMs, TimeUnit.MILLISECONDS);
            if (fut == null) {
                throw new CustomResponseException("조회 실패 : 배치 타임아웃(" + cfg.batchTimeoutMs + "ms) 초과", 504);
            }
            return fut;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new CustomResponseException("조회 실패 : interrupted", 502);
        }
    }

    // 실패 체크
    // -
    private void handleFail(PageResult pr, SyncConfig cfg) {
        if (cfg.failFast) {
            throw new CustomResponseException(
                    " 조회 실패 : validateSampleInteger1=" + pr.sampleInteger1 + ", validateSampleInteger2=" + pr.sampleInteger2 + ", cause=" + pr.errorMessage,
                    502
            );
        }
    }

    // 메세지 리턴 함수
    // -
    private String buildMessage(int totalCount, SyncConfig cfg, int taskCount, FetchSummary summary) {
        return " 조회 완료. total=" + totalCount
                + ", pageSize=" + cfg.pageSize
                + ", pages=" + taskCount
                + ", successPages=" + summary.successPages
                + ", failPages=" + summary.failPages
                + ", fetched=" + summary.all.size();
    }

    // 쓰레드 정리 함수
    // -
    private void shutdownGracefully(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    /**
     * 샘플타입1 멀티쓰레드 요청 조회
     * @return
     */
    public Response<?> findSampleType1ByMultiThread() {

        // 총 갯수 요청
        int totalCount = 0;
        try {
            totalCount = findSampleType2();
        } catch (CustomResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomResponseException("알수 없는 에러 발생 : " + e.getMessage(), 500);
        }

        if (totalCount <= 0) {
            return Response.builder("동기화할  정보가 존재하지 않습니다.", 200).build();
        }


        final SyncConfig cfg = SyncConfig.defaultConfig(totalCount);

        ExecutorService executor = newExecutor(cfg);
        CompletionService<PageResult> cs = new ExecutorCompletionService<>(executor);

        final long startedAt = System.nanoTime();

        try {
            int taskCount = submitPagingTasks(cs, totalCount, cfg);

            FetchSummary summary = collectResults(cs, taskCount, startedAt, cfg);

            String message = buildMessage(totalCount, cfg, taskCount, summary);

            return Response.builder(message, 200).data(summary.all).build();
        } finally {
            shutdownGracefully(executor);
        }
    }

    /**
     * findSampleType2
     * SampleType2 조회 결과 가져오기
     * @return
     */
    private int findSampleType2() {
        SampleGetRequestDto sampleType2Dto = SampleGetRequestDto.createSampleType2(properties.sampleKey());

        List<Map<String, Object>> maps = callByResultToList(sampleType2Dto, SampleApiEndpoint.SAMPLE_QUERY_A);

        if (maps.isEmpty() || maps.get(0).get("sampleKey") == null)
            throw new CustomResponseException("ERP 갯수 정보가 존재하지 않습니다.", 500);

        return (int) maps.get(0).get("sampleKey");
    }

    /** ====== **/

    /**
     *  sampleKey 확인용 함수
     * @return
     */
    public Map<String, Object> findSampleType2ReturnResponse() {
        return Map.of("sampleKey", findSampleType2());
    }

    /**
     *
     * sample 필수 조건으로 생성해야함으로 static 함수로 생성필요, 변환 필요.
     * @param dto
     * @param endpoint erp endpoint
     * @return
     */
    public List<Map<String, Object>> findSampleRequest(SampleGetRequestSecondValidationDto dto, SampleApiEndpoint endpoint) {
        SampleGetRequestSecondDto sampleGetRequestSecondDto = SampleGetRequestSecondDto.createSampleGetRequestSecondDto(dto);
        return callByResultToList(sampleGetRequestSecondDto, endpoint);
    }

    /**
     * Merge 조회
     * @param dto
     * @param endpoint
     * @return
     */
    public List<Map<String, Object>> findSampleMerge(SampleGetRequestDto dto, SampleApiEndpoint endpoint) {
        List<Map<String, Object>> erpOrderHeaderAll = sampleSearch(dto, endpoint);

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("dataListJson", buildDataListJsonParam(erpOrderHeaderAll));

        return transactionService.sampleFindFunction(paramMap);
    }

    // record 생성 
    private ResultCommand parseCommand(Map<String, Object> param) {
        LocalDate orderDate = module.requireLocalDate(param, "date");
        String sampleKey = module.requireString(param, "sampleKey");
        return new ResultCommand(orderDate, sampleKey);
    }

    /**
     * POST 요청 함수
     *
     * @param param -> 해당 매개변수는 Map으로 활용하는 Service 및 필요 정보 사용을위해 Map으로 받도록 처리
     * @return
     */
    public Response sendPostFunction(Map<String, Object> param) {

        // 실적 전송시 필수 적으로 필요한 작업 일시
        ResultCommand cmd = parseCommand(param);
        SampleGetRequestDto beforeRequestDto = SampleGetRequestDto.build(); // 요청 정보 만들고
        // 현재 해당 ERP 데이터 확인 요청용.
        List<Map<String, Object>> erpList1 = sampleSearch(beforeRequestDto, SampleApiEndpoint.SAMPLE_QUERY_A);
        SampleGetRequestSecondValidationDto secondDto = new SampleGetRequestSecondValidationDto(); // 요청 정보 만들고
        List<Map<String, Object>> erpList2 = findSampleRequest(secondDto, SampleApiEndpoint.SAMPLE_QUERY_B);
        List<Map<String, Object>> erpList3 = findSampleRequest(secondDto, SampleApiEndpoint.SAMPLE_QUERY_C);

        Map<String, Object> resultMap = sendProductionResult(erpList1, erpList2, erpList3);
        // 해당 정보 처리 후 MES DB 변경 할 경우
        // transactionService 호출 지점  transactionService. ~~

        return Response.builder("ERP에 성공적으로 반영되었습니다.", 200).build();
    }

    /**
     * ERP 전송
     *
     * @param erpList1
     * @param erpList2
     * @param erpList3
     * @return
     */
    private Map<String, Object> sendProductionResult(List<Map<String, Object>> erpList1, List<Map<String, Object>> erpList2, List<Map<String, Object>> erpList3) {
        // requestDto 생성하기 이걸로만 생성해야합니다. 아니면 validation 체크 로직을 따로 적용해야함.
        // SamplePostRequestDto.builder(validator).build();
        // 해당 결과 받기. Map<String, Object> resultMap = callByResult(인스턴스, SampleApiEndpoint.SAMPLE_COMMAND_CREATE);
        return null;
    }



    /*======= 요청 ============*/

    /**
     * 조회 result 가 List 일 때
     *
     * @param dto
     * @param endpoint
     * @return
     */
    private <T> List<Map<String, Object>> callByResultToList(T dto, SampleApiEndpoint endpoint) {
        Map<String, Object> apiResult = client.callErpApi(dto, endpoint);
        return getResultDataToList(apiResult, endpoint);
    }

    /**
     * 조회 result를 그대로 리턴할 때
     *
     * @param dto
     * @param endpoint
     * @return
     */
    private <T> Map<String, Object> callByResult(T dto, SampleApiEndpoint endpoint) {
        return client.callErpApi(dto, endpoint);
    }


    /*=========  private ========*/

    /**
     * 결과가 SUCCESS 일 때 resultData 추출 -> List<Map<String, Object>> 로 변환
     *
     * @param res      ERP 결과 데이터
     * @param endpoint 요청 endpoint
     * @return resultData List<Map<String,Object>> 로 변환하여 리턴
     */
    public List<Map<String, Object>> getResultDataToList(Map<String, Object> res, SampleApiEndpoint endpoint) {
        Object resultData = (res == null) ? null : res.get("resultData");

        if (!(resultData instanceof List<?> list))
            throw new CustomResponseException(module.stringFormatErrorReason("ERP 결과 데이터 변환 오류 결과 값이 부정확합니다.", endpoint.getErrorInfo()), 502);


        for (Object row : list) {
            if (!(row instanceof Map)) {
                throw new CustomResponseException(module.stringFormatErrorReason("ERP 결과 데이터 변환 오류 결과 리스트 내부 값이 부정확합니다.", endpoint.getErrorInfo()), 500);
            }
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> typed = (List<Map<String, Object>>) list;
        return typed;
    }

    /**
     * JSON 변환
     *
     * @param resultList JSON 형태의 리스트
     * @return String 으로 파싱된 리스트 dataListJson 추가
     */
    private String buildDataListJsonParam(List<Map<String, Object>> resultList) {
        if (resultList.isEmpty()) {
            throw new CustomResponseException("ERP 결과 데이터가 존재하지 않아 MES 데이터를 병합할 수 없습니다.", 500);
        }

        try {
            List<Map<String, Object>> copyList = new ArrayList<>(resultList);
            return OT_OM.writeValueAsString(copyList);
        } catch (JsonProcessingException e) {
            throw new CustomResponseException("ERP 결과 JSON 변환 실패", 500);
        }
    }


    /*=== records ====*/

    private record SyncConfig(int threadCount, int pageSize, int queueCapacity, long batchTimeoutMs, boolean failFast) {

        static SyncConfig defaultConfig(int totalCount) {
            int threads = 5;
            int page = 1000;
            int queue = threads * 4;
            long timeout = 3 * 60 * 1000L;

            return new SyncConfig(threads, page, queue, timeout, true);
        }
    }

    private record ResultCommand(LocalDate orderDate, String sampleKey) {
    }

    private record FetchSummary(List<Map<String, Object>> all, int successPages, int failPages) {
    }

    /**
     * @param data 데이터 미필요시 삭제 하세영
     */
    private record PageResult(boolean ok, int sampleInteger1, int sampleInteger2, int savedCount, int failedCount,
                              List<Map<String, Object>> data, String errorMessage) {

        static PageResult success(
                int sampleInteger1,
                int sampleInteger2,
                List<Map<String, Object>> data,
                int savedCount,
                int failedCount
        ) {
            return new PageResult(
                    true,
                    sampleInteger1,
                    sampleInteger2,
                    savedCount,
                    failedCount,
                    data,
                    null
            );
        }

        static PageResult fail(
                int sampleInteger1,
                int sampleInteger2,
                String errorMessage
        ) {
            return new PageResult(
                    false,
                    sampleInteger1,
                    sampleInteger2,
                    0,
                    0,
                    null,
                    errorMessage
            );
        }
    }


}
